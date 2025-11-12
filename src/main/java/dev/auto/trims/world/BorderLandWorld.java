package dev.auto.trims.world;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.auto.trims.Main;
import dev.auto.trims.customEvents.BorderLandsOnLoadEvent;
import dev.auto.trims.effectHandlers.helpers.StatusBar;
import dev.auto.trims.particles.OutputRift;
import dev.auto.trims.particles.utils.WarpInEntity;
import dev.auto.trims.utils.FileUtils;
import dev.auto.trims.utils.LocationUtils;
import dev.auto.trims.utils.WaypointUtils;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StructureSearchResult;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BorderLandWorld {
    public final Map<Structure, WorldObjective> worldObjectives = new HashMap<>();
    @Getter
    public final Set<UUID> players;
    public final Set<UUID> frozenPlayers = new HashSet<>();
    public final Map<UUID, StatusBar> statuses = new HashMap<>();
    public final Set<UUID> edgers = new HashSet<>();
    @Setter
    private Player leader;
    private ArmorStand waypoint;
    private final UUID worldID;
    private World world;

    private final Structure structure;

    private BukkitTask waypointTask;
    private BukkitTask countdownTask;
    private BukkitTask localBorderTask;

    private final int ticksUntilMeltdown;
    private double edgerMultiplier = 1;
    private final ConfigurationSection config;

    private OutputRift outputRift;

    @Setter
    private int elapsedTicks = 0;

    public static final List<Structure> structures = List.of(
            Structure.DESERT_PYRAMID,
            Structure.JUNGLE_PYRAMID,
            Structure.ANCIENT_CITY,
            Structure.TRIAL_CHAMBERS,
            Structure.TRAIL_RUINS,
            Structure.STRONGHOLD,
            Structure.MANSION,
            Structure.MONUMENT,
            Structure.PILLAGER_OUTPOST,
            Structure.SHIPWRECK
    );

    public BorderLandWorld(Set<UUID> players, Structure structure) {
        this.players = players;
        this.structure = structure;

        File configFile = new File(Main.getInstance().getDataFolder(), "data/generated-worlds.yml");
        YamlConfiguration configData = YamlConfiguration.loadConfiguration(configFile);

        List<String> possibleWorldIds = configData.getStringList("generated-worlds");
        if (possibleWorldIds.isEmpty()) throw new IllegalStateException("No generated worlds found");

        String worldId = possibleWorldIds.getFirst();
        this.worldID = UUID.fromString(worldId);

        possibleWorldIds.remove(worldId);
        configData.set("generated-worlds", possibleWorldIds);
        try {
            configData.save(configFile);
        }
        catch (IOException e) {
            Main.getInstance().getLogger().warning("Failed to save generated worlds config");
        }

        World world = Bukkit.getWorld(worldID.toString());
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(worldID.toString()));
        }
        this.world = world;

        config = Main.getInstance().getConfig().getConfigurationSection("trims.structure-options.%id%".replace("%id%", WorldManager.getIdFromStructure(structure).toString()));
        if (config == null) throw new IllegalStateException("Structure config not found");

        ticksUntilMeltdown = config.getInt("time-to-meltdown");

        load();
    }

    public void load() {
        if (world == null) throw new IllegalStateException("Instance not found");
        WorldManager.addWorld(world, this);

        // World configuration
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);

        File data = new File(Main.getInstance().getDataFolder(), "data/%world%/world-objectives.yml".replace("%world%", worldID.toString()));
        YamlConfiguration config = YamlConfiguration.loadConfiguration(data);
        for (String key : config.getKeys(false)) {
            WorldObjective objective = config.getSerializable(key, WorldObjective.class);
            if (objective == null) {
                Main.getInstance().getLogger().warning(
                        "Failed to deserialize world objective for world " + worldID + ": " + key
                );
                continue;
            }

            Structure struc = WorldManager.getStructureFromId(objective.type());
            if (struc == null) {
                Main.getInstance().getLogger().warning(
                        "Unknown structure id " + objective.type() + " for key " + key
                );
                continue;
            }

            worldObjectives.put(struc, objective);
            if (struc != structure) continue;

            outputRift = new OutputRift(objective.exit());
        }

        // Later
        for (UUID id : players) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;

            AttributeInstance waypointRange = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);
            if (waypointRange != null) waypointRange.setBaseValue(600000);

            StatusBar bar = new StatusBar(id);
            bar.setConsumer((uuid, statusBar) -> {
                if (elapsedTicks >= ticksUntilMeltdown) {
                    statusBar.setColor(BossBar.Color.RED);
                    statusBar.setTitle("Meltdown in progress");
                    return;
                }

                statusBar.setColor(BossBar.Color.GREEN);

                double remainingTicks = Math.max(0, ticksUntilMeltdown - elapsedTicks);
                double adjustedTicks = remainingTicks / edgerMultiplier;

                int totalSeconds = (int) Math.ceil(adjustedTicks / 20.0);
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;

                String formatted = String.format("%d:%02d", minutes, seconds);
                statusBar.setTitle("%time% to meltdown".replace("%time%", formatted));
            });

            bar.setShouldHide(false);
            float initialProgress = Math.min(1f, Math.max(0f, elapsedTicks / (float) ticksUntilMeltdown));
            bar.setProgress(initialProgress);

            statuses.put(id, bar);
        }

        // Waypoint task
        waypointTask = createWaypointTask();
        countdownTask = createCountdownTask();
        localBorderTask = createLocalBorderTask();

        Bukkit.getPluginManager().callEvent(new BorderLandsOnLoadEvent(worldID));
    }

    public void unload() {
        if (waypointTask != null) {
            waypointTask.cancel();
            waypointTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        outputRift.stop();

        for (UUID sid : new HashSet<>(statuses.keySet())) {
            StatusBar bar = statuses.remove(sid);
            if (bar != null) bar.hide();
        }

        World overworld = Bukkit.getWorld("world");
        if (overworld == null) {
            List<World> worlds = Bukkit.getWorlds();
            if (!worlds.isEmpty()) {
                overworld = worlds.get(0);
            }
        }

        World instanceWorld = this.world != null ? this.world : Bukkit.getWorld(worldID.toString());
        if (instanceWorld != null && overworld != null) {
            Location safeSpawn = overworld.getSpawnLocation().clone().add(0, 1, 0);
            for (Player p : new ArrayList<>(instanceWorld.getPlayers())) {
                try {
                    p.teleport(safeSpawn);
                } catch (Exception ignored) {
                }
            }
        }

        for (UUID id : new HashSet<>(players)) {
            players.remove(id);
        }

        if (waypoint != null) {
            try { waypoint.remove(); } catch (Exception ignored) {}
            waypoint = null;
        }

        // Unload world if still loaded
        if (instanceWorld != null) {
            try {
                Bukkit.unloadWorld(instanceWorld, false);
            } catch (Exception ignored) {
            }
        }

        // Delete world folder and objectives file
        try {
            Path worldFolder = Bukkit.getWorldContainer().toPath().resolve(worldID.toString());
            FileUtils.deleteFolder(worldFolder);
        } catch (Exception ignored) {
        }
        FileUtils.deleteObjectivesFile(worldID);

        if (instanceWorld != null) {
            WorldManager.removeWorld(instanceWorld);
        }
        this.world = null;
    }

    public void ritual(Player player, List<ItemStack> items) {
        Location base = player.getLocation().clone();
        player.teleport(base.add(0, 1, 0));
        World world = base.getWorld();
        if (world == null || items.isEmpty()) return;

        int centerX = base.getBlockX();
        int centerZ = base.getBlockZ();
        int floorY = base.getBlockY() - 1;

        Location center = new Location(world, centerX, floorY, centerZ).getBlock().getLocation();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int radius = 2;

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                world.getBlockAt(x, cy, z).setType(Material.SMOOTH_QUARTZ);

                for (int y = cy + 1; y <= cy + 4; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }

        int count = items.size();
        int interval = Math.max(1, 80 / count);

        Location dropLoc = center.clone().add(2.5, 0.5, 0.5);
        frozenPlayers.add(player.getUniqueId());

        for (int i = 0; i < count; i++) {
            ItemStack stack = items.get(i).clone();
            int delay = i * interval;

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Item dropped = world.dropItem(dropLoc.clone().add(0, 1, 0), stack);
                dropped.setPickupDelay(Integer.MAX_VALUE);
                dropped.setVelocity(new Vector(0, 0, 0));
                dropped.setGravity(false);

                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Location target = dropped.getLocation();
                    Location orient = player.getLocation().clone();
                    Vector toTarget = target.toVector().subtract(orient.toVector());
                    orient.setDirection(toTarget);
                    player.teleport(orient);
                }, 1);
            }, delay);
        }

        // lava at center, one block above floor
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            int lavaX = dropLoc.getBlockX();
            int lavaZ = dropLoc.getBlockZ();

            Block block = world.getBlockAt(lavaX, cy + 1, lavaZ);
            block.setType(Material.LAVA);
            world.strikeLightning(block.getLocation());

            frozenPlayers.remove(player.getUniqueId());
            executePlayer(player);

        }, 80);
    }

    private void executePlayer(Player player) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 0);
            player.addPotionEffect(blindness);
            player.getInventory().clear();
            player.setHealth(0);
            StatusBar bar = statuses.get(player.getUniqueId());
            if (bar != null) bar.hide();
        }, 30);
    }

    private BukkitTask createCountdownTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (players.isEmpty()) return;

                final int before = elapsedTicks;
                final int increment = (int) (5 * edgerMultiplier);
                elapsedTicks += increment;

                if (elapsedTicks > ticksUntilMeltdown) elapsedTicks = ticksUntilMeltdown;

                for (UUID id : players) {
                    Player player = Bukkit.getPlayer(id);
                    if (player == null) continue;
                    if (player.getWorld() != world) continue;

                    StatusBar bar = statuses.get(id);
                    if (bar == null) continue;
                    float progress = Math.min(1f, Math.max(0f, elapsedTicks / (float) ticksUntilMeltdown));
                    bar.setProgress(progress);
                }

                if (elapsedTicks >= ticksUntilMeltdown && before < ticksUntilMeltdown) {
                    this.cancel();
                    for (UUID id : players) {
                        Player player = Bukkit.getPlayer(id);
                        if (player == null) continue;
                        if (player.getWorld() != world) continue;

                        List<ItemStack> items = Arrays.stream(player.getInventory().getContents())
                            .filter(Objects::nonNull)
                            .filter(stack -> !stack.getType().isAir())
                            .map(ItemStack::clone)
                            .toList();

                        if (items.isEmpty()) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have nothing to offer... Unfortunate"));
                            executePlayer(player);
                            return;
                        }

                        player.getInventory().clear();

                        ritual(player, items);
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            unload();
                            Main.getInstance().getLogger().info("Unloaded world " + worldID.toString() + " after meltdown");
                        }
                    }.runTaskLater(Main.getInstance(), 20 * 10);
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 5);
    }

    private BukkitTask createWaypointTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID id : players) {
                    Player player = Bukkit.getPlayer(id);
                    if (player == null) continue;

                    if (player == leader) {
                        WaypointUtils.SetWaypoint(player, "#a11818");
                        continue;
                    }

                    WaypointUtils.SetWaypoint(player, "#9c5021");
                }
            }
        }.runTaskLater(Main.getInstance(), 20);
    }

    private BukkitTask createLocalBorderTask() {
        return new BukkitRunnable() {
            final double followDistance = config.getDouble("follow-distance");
            final double penaltyMultiplier = config.getDouble("follow-penalty-multiplier");
            final double worldHeight = world.getMaxHeight();

            @Override
            public void run() {
                if (leader == null || !leader.isOnline() || leader.getWorld() != world) return;

                Location location = leader.getLocation();
                Collection<Player> nearbyPlayers = location.getWorld()
                        .getNearbyPlayers(location, followDistance, worldHeight, followDistance);

                for (UUID id : players) {
                    Player player = Bukkit.getPlayer(id);
                    if (player == null || player == leader || player.getWorld() != world) continue;

                    boolean isOut = !nearbyPlayers.contains(player);

                    if (isOut && !edgers.contains(id)) {
                        edgers.add(id);
                        player.sendMessage(MiniMessage.miniMessage()
                                .deserialize("<gray>Don't stay out too long, the world is unstable"));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                    else if (!isOut && edgers.contains(id)) {
                        edgers.remove(id);
                    }
                }

                int totalPlayers = players.size();
                if (totalPlayers <= 1) {
                    edgerMultiplier = 1;
                    return;
                }

                double ratio = (double) edgers.size() / totalPlayers;
                edgerMultiplier = Math.min(1 + (ratio * totalPlayers * penaltyMultiplier), 5);
            }

        }.runTaskTimer(Main.getInstance(), 0, 10);
    }


    public void addPlayer(Player player) {
        if (worldObjectives.isEmpty()) throw new IllegalStateException("World Objectives are empty");
        if (!players.contains(player.getUniqueId())) throw new IllegalStateException("Player is not in world");

        WorldObjective objective = worldObjectives.get(structure);
        if (objective == null)
            throw new IllegalStateException("World objective for structure " + structure + " is null");

        int spawnVariation = Main.getInstance().getConfig().getInt("trims.player-options.spawn-variation");
        if (spawnVariation <= 0) throw new IllegalStateException("Spawn variation is not supported");

        Location spawn = objective.spawn().clone().add(0, 70, 0);
        Location dropPoint = LocationUtils.randomInCircle(spawn, spawnVariation);

        PotionEffect sf = new PotionEffect(PotionEffectType.SLOW_FALLING, 200 * 20, 1, false, false);
        player.addPotionEffect(sf);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isDead() || !player.isOnline()) {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    cancel();
                    return;
                }

                if (!player.isOnGround()) return;
                player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                cancel();
            }
        }.runTaskTimer(Main.getInstance(), 20, 5);

        if (!dropPoint.isChunkLoaded()) {
            dropPoint.getChunk().load();
        }

        WarpInEntity warpInEntity = new WarpInEntity(dropPoint.clone().add(-1f, 3, -1f));
        new BukkitRunnable() {
            @Override
            public void run() {
                warpInEntity.destroy();
            }

        }.runTaskLater(Main.getInstance(), 20 * 10);

        player.teleport(dropPoint);

        if (player.isOp()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green><click:run_command:" + LocationUtils.locationToCommand(objective.spawn(), player) + ">Click</click> to teleport to spawn"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red><click:run_command:" + LocationUtils.locationToCommand(objective.objective(), player) + ">Click</click> to teleport to objective"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<light_purple><click:run_command:" + LocationUtils.locationToCommand(objective.exit(), player) + ">Click</click> to teleport to exit"));
        }
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        WaypointUtils.removeWaypoint(player);
        StatusBar bar = statuses.remove(player.getUniqueId());
        if (bar != null) bar.hide();

        if (!player.isDead()) {
            Location spawn = player.getRespawnLocation();
            if (spawn == null || spawn.getWorld() == world) {
                spawn = Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation();
            }
            player.teleport(spawn);
        }

        if (players.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!players.isEmpty()) {
                        cancel();
                        return;
                    }
                    unload();
                }
            }.runTaskLater(Main.getInstance(), 20 * 5);
        }
    }

    public static UUID createWorld() {
        UUID worldID = UUID.randomUUID();

        if (Bukkit.getWorld(worldID) != null) {
            throw new IllegalStateException("World already exists");
        }

        WorldCreator creator = new WorldCreator(worldID.toString());
        creator.environment(World.Environment.NORMAL);
        creator.seed(UUID.randomUUID().getLeastSignificantBits());

        ConfigurationSection config = Main.getInstance().getConfig().getConfigurationSection("trims.world-options");
        assert config != null;
        ConfigurationSection spawnToObjective = config.getConfigurationSection("s-o");
        assert spawnToObjective != null;
        ConfigurationSection objectiveToExtraction = config.getConfigurationSection("o-e");
        assert objectiveToExtraction != null;

        double spawnMin = spawnToObjective.getDouble("min");
        double spawnMax = spawnToObjective.getDouble("max");
        double exitMin = objectiveToExtraction.getDouble("min");
        double exitMax = objectiveToExtraction.getDouble("max");

        World world = creator.createWorld();
        assert world != null;

        Location origin = new Location(world, 0, world.getMaxHeight(), 0);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Map<Structure, WorldObjective> worldObjectives = new HashMap<>();

        for (Structure structure : structures) {
            StructureSearchResult result = world.locateNearestStructure(origin, structure, 20000, false);
            if (result == null) {
                Main.getInstance().getLogger().warning("Cant find structure " + structure);
                continue;
            }

            Location resultLoc = result.getLocation();
            int baseX = resultLoc.getBlockX();
            int baseZ = resultLoc.getBlockZ();

            Location objective = null;
            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                var block = world.getBlockAt(baseX, y, baseZ);
                if (!block.getType().isAir()) {
                    objective = new Location(world, baseX + 0.5, y + 1, baseZ + 0.5);
                    break;
                }
            }
            if (objective == null) {
                objective = world.getSpawnLocation(); // fallback
            }

            double angleSpawn = rnd.nextDouble(0, Math.PI * 2);
            double radiusSpawn = rnd.nextDouble(spawnMin, spawnMax);

            double spawnX = objective.getX() + Math.cos(angleSpawn) * radiusSpawn;
            double spawnZ = objective.getZ() + Math.sin(angleSpawn) * radiusSpawn;

            Location spawn = null;
            int spawnBX = (int) Math.floor(spawnX);
            int spawnBZ = (int) Math.floor(spawnZ);

            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                var block = world.getBlockAt(spawnBX, y, spawnBZ);
                if (!block.getType().isAir()) {
                    spawn = new Location(world, spawnBX + 0.5, y + 1, spawnBZ + 0.5);
                    break;
                }
            }

            if (spawn == null) {
                spawn = objective.clone().add(0, 5, 0);
            }

            double angleExit = rnd.nextDouble(0, Math.PI * 2);
            double radiusExit = rnd.nextDouble(exitMin, exitMax);

            double exitX = objective.getX() + Math.cos(angleExit) * radiusExit;
            double exitZ = objective.getZ() + Math.sin(angleExit) * radiusExit;

            Location exit = null;
            int exitBX = (int) Math.floor(exitX);
            int exitBZ = (int) Math.floor(exitZ);

            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                var block = world.getBlockAt(exitBX, y, exitBZ);
                if (!block.getType().isAir()) {
                    exit = new Location(world, exitBX + 0.5, y + 1, exitBZ + 0.5);
                    break;
                }
            }
            if (exit == null) {
                exit = objective.clone().add(0, 5, 0);
            }

            WorldObjective worldObjective = new WorldObjective(
                    WorldManager.getIdFromStructure(structure),
                    spawn,
                    objective,
                    exit
            );

            worldObjectives.put(structure, worldObjective);
        }

        File datafile = new File(Main.getInstance().getDataFolder(), "data/%world%/world-objectives.yml".replace("%world%", worldID.toString()));
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(datafile);

        for (Map.Entry<Structure, WorldObjective> entry : worldObjectives.entrySet()) {
            Structure struc = entry.getKey();
            WorldObjective objective = entry.getValue();

            String idKey = WorldManager.getIdFromStructure(struc).toString();
            dataConfig.set(idKey, objective);
        }

        try {
            dataConfig.save(datafile);
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Failed to save world data for world %worldid%: ".replace("%worldid%", worldID.toString()) + e.getMessage());
        }

        return worldID;
    }
}
