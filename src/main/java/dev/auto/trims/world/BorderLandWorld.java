package dev.auto.trims.world;

import dev.auto.trims.Main;
import dev.auto.trims.customEvents.BorderLandsOnLoadEvent;
import dev.auto.trims.effectHandlers.helpers.StatusBar;
import dev.auto.trims.utils.WaypointUtils;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StructureSearchResult;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BorderLandWorld {
    private static int readMeltdownTicks() {
        int value = Main.getInstance().getConfig().getInt("trims.world-options.duration.meltdown", 6000);
        if (value <= 0) {
            Main.getInstance().getLogger().warning("Invalid 'trims.world-options.duration.meltdown' (" + value + ") — falling back to 6000 ticks (5 minutes)");
            return 6000;
        }
        return value;
    }
    public final Map<Structure, WorldObjective> worldObjectives = new HashMap<>();
    @Getter
    public final Set<UUID> players;
    public final Map<UUID, StatusBar> statuses = new HashMap<>();
    @Setter
    private Player leader;
    private ArmorStand waypoint;
    private final UUID worldID;
    private World world;
    private final Structure structure;
    private BukkitTask waypointTask;
    private BukkitTask countdownTask;
    private final int ticksUntilMeltdown = readMeltdownTicks();

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

    public BorderLandWorld(UUID worldID, Set<UUID> players, Structure structure) {
        this.worldID = worldID;
        this.players = players;
        this.structure = structure;

        this.world = Bukkit.getWorld(worldID.toString());
        if (this.world == null) {
            Main.getInstance().getLogger().warning("World not found for world ID " + worldID);
        }

        // ticksUntilMeltdown is validated via readMeltdownTicks() with a safe fallback
        load();
    }

    public void load() {
        if (world == null) throw new IllegalStateException("Instance not found");
        WorldManager.addWorld(world, this);


        /** Point load **/
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

            createWayPoint(objective, 1);
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
                int remainingTicks = Math.max(0, ticksUntilMeltdown - elapsedTicks);
                int minutes = (int) Math.ceil(remainingTicks / 1200.0);

                statusBar.setTitle("%min% min to meltdown".replace("%min%", String.valueOf(minutes)));
            });

            bar.setShouldHide(false);
            float initialProgress = Math.min(1f, Math.max(0f, elapsedTicks / (float) ticksUntilMeltdown));
            bar.setProgress(initialProgress);

            statuses.put(id, bar);
        }

        // Waypoint task
        waypointTask = createWaypointTask();
        countdownTask = createCountdownTask();

        Bukkit.getPluginManager().callEvent(new BorderLandsOnLoadEvent(worldID));
    }

    public void unload() {
        for (UUID id : new HashSet<>(players)) {
            players.remove(id);
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;

            Location spawn = player.getRespawnLocation();
            if (spawn == null || spawn.getWorld() == world) spawn = Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation();
            player.teleport(spawn);

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>We likely encountered an error for the world: %worldid% sending you home safely. Report to admins!"
                            .replace("%worldid%", worldID.toString())
            ));

            WaypointUtils.RemoveWaypoint(player);
        }

        if (waypointTask != null) waypointTask.cancel();
        if (countdownTask != null) countdownTask.cancel();

        for (UUID sid : new HashSet<>(statuses.keySet())) {
            StatusBar bar = statuses.remove(sid);
            if (bar != null) bar.hide();
        }
        WorldManager.removeWorld(world);
    }

    private BukkitTask createCountdownTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (players.isEmpty()) return;

                final int before = elapsedTicks;
                elapsedTicks += 5;
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
                    // todo add meltdown logic
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

    public void warpPlayer(Player player) {
        if (worldObjectives.isEmpty()) throw new IllegalStateException("World Objectives are empty");

        

    }

    public void createWayPoint(WorldObjective objective, Integer index) {
        if (index > 1 || index < 0) throw new IllegalArgumentException("Index must be between 0 and 1");

        Location loc = switch (index) {
            case 0 -> objective.objective();
            case 1 -> objective.exit();
            default -> throw new IllegalArgumentException("Index must be between 0 and 1");
        };

        UUID id = UUID.randomUUID();
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setCanMove(false);
        stand.addScoreboardTag(id.toString());
        AttributeInstance waypointRange = stand.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (waypointRange != null) waypointRange.setBaseValue(600000);

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "waypoint modify @e[type=armor_stand,tag=%id%,limit=1] color hex 43E866".replace("%id%", id.toString()));
        }, 5);

        if (waypoint != null) waypoint.remove();
        waypoint = stand;
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
