package dev.auto.trims.world;

import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.Structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static Map<World, BorderLandWorld> worlds = new HashMap<>();
    public static void addWorld(World world, BorderLandWorld borderLandWorld) {
        worlds.put(world, borderLandWorld);
    }

    public static void removeWorld(World world) {
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
}
