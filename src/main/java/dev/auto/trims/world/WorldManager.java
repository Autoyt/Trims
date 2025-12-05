package dev.auto.trims.world;

import dev.auto.trims.Main;
import dev.auto.trims.crafting.CraftUtils;
import dev.auto.trims.crafting.RelayAppleListener;
import dev.auto.trims.crafting.RiftCraftListener;
import dev.auto.trims.customEvents.NewBorderlandGenerationEvent;
import dev.auto.trims.particles.InputRift;
import dev.auto.trims.particles.utils.ColorUtils;
import dev.auto.trims.utils.FileUtils;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorldManager implements Listener {
    private static final List<Structure> structures = List.of(
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

    public static Structure getStructureFromId(int id) {
        return structures.get(id);
    }

    public static Integer getIdFromStructure(Structure structure) {
        return structures.indexOf(structure);
    }

    private static final Map<World, BorderLandWorld> worlds = new HashMap<>();
    private static final Set<UUID> globalPlayers = new HashSet<>();
    private static final Set<UUID> messageCooldown = new HashSet<>();
    private static UUID loadingWorld;

    private static final int riftCooldown = Main.getInstance().getConfig().getInt("trims.player-options.rift-cooldown");

    private static final ConfigurationSection generationOptions = Objects.requireNonNull(Main.getInstance().getConfig().getConfigurationSection("trims.world-options.generation"));
    private static final int intialGenerationCooldown = generationOptions.getInt("start-intial-generation-ticks");
    private static final int emptyGenerationCooldown = generationOptions.getInt("start-after-empty-ticks");
    private static final int maxGeneratedWorlds = generationOptions.getInt("max-generated-worlds");

    public static void addWorld(World world, BorderLandWorld borderLandWorld) {
        worlds.put(world, borderLandWorld);
        globalPlayers.addAll(borderLandWorld.getPlayers());
    }

    public static void removeWorld(World world) {
        BorderLandWorld blw = worlds.get(world);
        if (blw != null) {
            try {
                globalPlayers.removeAll(new HashSet<>(blw.getPlayers()));
            } catch (Exception ignored) {
            }
        }
        worlds.remove(world);
    }

    public WorldManager() {
        File file = new File(Main.getInstance().getDataFolder(), "data/generated-worlds.yml");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!(config.getStringList("generated-worlds").size() >= maxGeneratedWorlds)) {
            new BukkitRunnable() {
            @Override
            public void run() {
                if (loadingWorld == null) {
                    if (!Bukkit.getOnlinePlayers().isEmpty()) return;

                    WorldGenerator generator = new WorldGenerator();
                    loadingWorld = generator.getWorldID();
                }
            }
        }.runTaskLater(Main.getInstance(), intialGenerationCooldown);
        }

    }

    public static void cleanupWorlds() {
        File serverContainer = Bukkit.getWorldContainer();
        File[] worldFolders = serverContainer.listFiles(file ->
                file.isDirectory() && new File(file, "level.dat").exists()
        );

        if (worldFolders == null) {
            throw new IllegalStateException("Failed to list world folders");
        }

        File file = new File(Main.getInstance().getDataFolder(), "data/generated-worlds.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> worlds = config.getStringList("generated-worlds");
        Set<String> valid = new HashSet<>(worlds);

        Set<String> existingNames = new HashSet<>();

        for (File folder : worldFolders) {
            String name = folder.getName();
            existingNames.add(name);

            if (name.equals("world") || name.equals("world_nether") || name.equals("world_the_end")) {
                continue;
            }

            if (!valid.contains(name)) {
                World loaded = Bukkit.getWorld(name);
                if (loaded != null) {
                    Bukkit.unloadWorld(loaded, false);
                }

                FileUtils.deleteFolder(folder.toPath());

                File dataDir = new File(Main.getInstance().getDataFolder(), "data/" + name);
                FileUtils.deleteFolder(dataDir.toPath());
            }
        }

        worlds.removeIf(w -> !existingNames.contains(w));
        config.set("generated-worlds", worlds);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BorderLandWorld getBorderWorld(World world) {
        return worlds.get(world);
    }

    @EventHandler
    public void onWorldChangeEvent(PlayerChangedWorldEvent event) {
        // Add some locator bar logic here. World load resets stuff
        // TODO fix particles
        // TODO use ints for structure types, preload
    }

    @EventHandler
    public void onWorldLoadEvent(PlayerClientLoadedWorldEvent event) {}

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPlace(PlayerInteractEvent event) {
        handleRiftToken(event);
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        var cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL && cause != PlayerTeleportEvent.TeleportCause.END_PORTAL) return;
        if (!worlds.containsKey(event.getFrom().getWorld())) return;

        // Blocks nether portal
        event.setCancelled(true);
        event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Trying to leave? I don't think so"));
    }

    @EventHandler
    public void onEmptyServer(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!Bukkit.getOnlinePlayers().isEmpty()) return;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!Bukkit.getOnlinePlayers().isEmpty()) return;
                    Main.getInstance().getLogger().info("Server is empty, generating a new world...");

                    WorldGenerator generator = new WorldGenerator();
                    loadingWorld = generator.getWorldID();
                }

            }.runTaskLater(Main.getInstance(), emptyGenerationCooldown);
        }, 1);
    }

    @EventHandler
    public void onWorldLoad(PlayerJoinEvent event) {
        if (loadingWorld == null) return;
        event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<green>Generating world, expect lag for a short period."));
    }

    @EventHandler
    public void onGenerationEvent(NewBorderlandGenerationEvent event) {
        UUID worldID = event.getWorldID();
        String name = event.getWorldName();

        File file = new File(Main.getInstance().getDataFolder(), "data/generated-worlds.yml");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<String> worlds = config.getStringList("generated-worlds");

        if (!worlds.contains(name)) {
            worlds.add(name);
            config.set("generated-worlds", worlds);
            try {
                config.save(file);
            } catch (IOException e) {
                Main.getInstance().getLogger().warning("Failed to save generated world list: " + e.getMessage());
            }
        }

        if (!Bukkit.getOnlinePlayers().isEmpty() || worlds.size() >= maxGeneratedWorlds) {
            Main.getInstance().getLogger().info("Server is not empty or world list is full, not generating a new world");
            loadingWorld = null;
            return;
        }

        loadingWorld = new WorldGenerator().getWorldID();
        Main.getInstance().getLogger().info("Generating %worldid%".replace("%worldid%", loadingWorld.toString()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        BorderLandWorld bdDeath = WorldManager.getBorderWorld(player.getWorld());
        if (bdDeath == null) return;

        Location respawn = player.getRespawnLocation();
        Location target;

        if (respawn != null) {
            World respawnWorld = respawn.getWorld();
            BorderLandWorld bdRespawn = WorldManager.getBorderWorld(respawnWorld);

            if (bdRespawn != null) {
                target = respawn;
            } else {
                bdDeath.removePlayer(player);
                target = respawn;
                if (target.getWorld() == null) {
                    World main = Bukkit.getWorld("world");
                    if (main == null) main = Bukkit.getWorlds().get(0);
                    target = main.getSpawnLocation();
                }
            }
        } else {
            bdDeath.removePlayer(player);
            World main = Bukkit.getWorld("world");
            if (main == null) main = Bukkit.getWorlds().get(0);
            target = main.getSpawnLocation();
        }

        // TODO custom death message
        player.setRespawnLocation(target, true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!globalPlayers.contains(event.getPlayer().getUniqueId())) return;

        final UUID id = event.getPlayer().getUniqueId();
        for (BorderLandWorld bd : worlds.values()) {
            if (bd.frozenPlayers.contains(id)) {
                var to = event.getTo();
                var from = event.getFrom();
                if (to == null) return;
                // Lock position while allowing yaw/pitch updates to pass through
                to.setX(from.getX());
                to.setY(from.getY());
                to.setZ(from.getZ());
                event.setTo(to);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!worlds.containsKey(event.getClickedBlock().getWorld())) return;

        Block block = event.getClickedBlock();
        World world = event.getClickedBlock().getWorld();
        Player player = event.getPlayer();
        if (block.getWorld() != world) return;

        if (block.getType() == Material.ENDER_CHEST) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            event.setCancelled(true);

            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You can't open an ender chest in this world!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }

        if (block.getType() == Material.SHULKER_BOX) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            event.setCancelled(true);

            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You can't open an Shulker box in this world!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @EventHandler
    public void onExitClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) return;
        if (!globalPlayers.contains(event.getPlayer().getUniqueId())) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        var bd = getBorderWorld(world);
        bd.removePlayer(player);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!worlds.containsKey(event.getPlayer().getWorld())) return;

        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>You can't open an ender chest in this world!"));
            event.setCancelled(true);
        }

        if (event.getInventory().getType() == InventoryType.SHULKER_BOX) {
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>You can't open an Shulker box in this world!"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!worlds.containsKey(event.getEntity().getWorld())) return;
        if (!(event.getEntity() instanceof Player damaged)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        var players = worlds.get(damaged.getWorld()).getPlayers();
        if (players.isEmpty()) return;

        if (!players.contains(damaged.getUniqueId())) return;
        if (!players.contains(attacker.getUniqueId())) return;

        attacker.playSound(attacker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        damaged.getWorld().spawnParticle(Particle.ASH, damaged.getLocation(), 10);
        event.setCancelled(true);
    }

    private void handleRiftToken(PlayerInteractEvent event) {
        // Only handle right-clicks with the main hand
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_EYE) return;

        var meta = item.getItemMeta();
        if (meta == null) return;

        var itemPdc = meta.getPersistentDataContainer();
        Integer id = itemPdc.get(RiftCraftListener.riftKey, PersistentDataType.INTEGER);
        if (id == null) return;

        event.setCancelled(true);

        if (worlds.containsKey(event.getPlayer().getWorld())) return;

        Player player = event.getPlayer();
        var playerPdc = player.getPersistentDataContainer();

        long cooldownMs = riftCooldown * 50L;

        Long storedCooldown = playerPdc.get(RiftCraftListener.riftPlayerCooldownKey, PersistentDataType.LONG);
        if (storedCooldown != null) {
            long nowMs = System.currentTimeMillis();
            long elapsedMs = nowMs - storedCooldown;
            long remainingMs = cooldownMs - elapsedMs;

            if (remainingMs > 0) {
                double cooldownHours = cooldownMs / 3_600_000d;
                double remainingHours = remainingMs / 3_600_000d;

                if (messageCooldown.contains(player.getUniqueId())) return;

                String message = String.format(
                        "<red>You may only rift once per %.1f hrs - Remaining time: %.1f hrs",
                        cooldownHours,
                        remainingHours
                );

                messageCooldown.add(player.getUniqueId());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        messageCooldown.remove(player.getUniqueId());
                    }
                }.runTaskLater(Main.getInstance(), 40);

                player.sendMessage(MiniMessage.miniMessage().deserialize(message));
                return;
            }
        }

        long now = System.currentTimeMillis();
        playerPdc.set(RiftCraftListener.riftPlayerCooldownKey, PersistentDataType.LONG, now);

        // Consume exactly one item from the main hand
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.isSimilar(item)) {
            int amount = main.getAmount();
            if (amount > 0) {
                main.setAmount(amount - 1);
            }
        }

        Structure structure = getStructureFromId(id);
        if (structure == null) return;

        Block base = event.getClickedBlock();
        if (base == null) {
            base = player.getLocation().getBlock();
        }

        BlockFace face = event.getBlockFace();
        Block intendedBlock = base.getRelative(face);

        var rift = new InputRift(intendedBlock.getLocation(), structure);
        rift.setPlacer(player);
    }

    @EventHandler
    public void onRelayAppleConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENCHANTED_GOLDEN_APPLE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        var itemPdc = meta.getPersistentDataContainer();
        Boolean relay = itemPdc.get(RelayAppleListener.relayKey, PersistentDataType.BOOLEAN);
        if (relay == null || !relay) return;

        Player player = event.getPlayer();
        if (!worlds.containsKey(player.getWorld())) {
            event.setCancelled(true);

            if (!messageCooldown.contains(player.getUniqueId())) {
                player.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<red>You can't use relay apples in this world!"));

                messageCooldown.add(player.getUniqueId());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        messageCooldown.remove(player.getUniqueId());
                    }
                }.runTaskLater(Main.getInstance(), 40);
            }
            return;
        }

        var bd = worlds.get(player.getWorld());

        if (!bd.getPlayers().contains(player.getUniqueId())) {
            event.setCancelled(true);  // no effects, no item eaten
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red>You dont need that"));
            return;
        }

        Particle.DustOptions dustOptions = new Particle.DustTransition(
                Color.fromRGB(ColorUtils.hexToRgbInt("#edaf02")),
                Color.fromRGB(ColorUtils.hexToRgbInt("#262626")),
                1f
        );

        player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_3, 1f, 1f);
        player.getWorld().spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                player.getLocation(),
                70,
                2, 2, 2,
                2,
                dustOptions
        );
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("<green>Sending you home!"));

        new BukkitRunnable() {
            @Override
            public void run() {
                bd.removePlayer(player);
            }
        }.runTaskLater(Main.getInstance(), 30);
    }
}