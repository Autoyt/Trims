package dev.auto.trims.world;

import dev.auto.trims.Main;
import dev.auto.trims.crafting.CraftUtils;
import dev.auto.trims.crafting.RelayAppleListener;
import dev.auto.trims.crafting.RiftCraftListener;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
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
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

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

    private static final int riftCooldown = Main.getInstance().getConfig().getInt("trims.player-options.rift-cooldown");


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

    public static BorderLandWorld getBorderWorld(World world) {
        return worlds.get(world);
    }

    // On actual load
    public void onWorldChangeEvent(PlayerChangedWorldEvent event) {
        // Add some locator bar logic here. World load resets stuff
        // TODO fix particles
        // TODO use ints for structure types, preload
    }

    // On visual load
    public void onWorldLoadEvent(PlayerClientLoadedWorldEvent event) {}

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPlace(PlayerInteractEvent event) {
        handleRiftToken(event);
        handleRelayApple(event);
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
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_EYE) return;

        var meta = item.getItemMeta();
        if (meta == null) return;

        var itemPdc = meta.getPersistentDataContainer();
        Integer id = itemPdc.get(RiftCraftListener.riftKey, PersistentDataType.INTEGER);
        if (id == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        var playerPdc = player.getPersistentDataContainer();

        long cooldownMs = riftCooldown * 50L;

        Long storedCooldown = playerPdc.get(RiftCraftListener.riftKey, PersistentDataType.LONG);
        if (storedCooldown != null) {
            long now = System.currentTimeMillis();
            long elapsedMs = now - storedCooldown;
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
        playerPdc.set(RiftCraftListener.riftKey, PersistentDataType.LONG, now);

        item.setAmount(0);

        Structure structure = getStructureFromId(id);
        if (structure == null) return;

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<red>Rift for " + CraftUtils.getPrettyStructureName(structure)
        ));
    }

    private void handleRelayApple(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

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
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You can't use relay apples in this world!"));

                messageCooldown.add(player.getUniqueId());
                new BukkitRunnable() {
                @Override
                public void run() {
                    messageCooldown.remove(player.getUniqueId());
                }
                }.runTaskLater(Main.getInstance(), 40);

                return;
            }

            return;
        }

        int amount = Math.max(item.getAmount() - 1, 0);
        item.setAmount(amount);

        // TODO relay logic
    }
}
