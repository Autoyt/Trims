package dev.auto.trims.listeners;

import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.effectHandlers.heavyEvents.MovementListener;
import dev.auto.trims.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;
import java.util.UUID;

public class GameListeners implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Clear cached trim slots for this player
        TrimManager.clear(uuid);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;

        for (IBaseEffectHandler h : TrimManager.handlers) {
            if (h instanceof MovementListener m) {
                m.onMovement(event);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (WorldManager.getBorderWorld(player.getWorld()) == null) {
            player.teleport(Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation());
            player.sendMessage("Sending you to spawn. Invalid world");
        }

        // The repeating tick task is started from Main.onEnable(). Just build the player's slots.
        TrimManager.buildSlots(uuid);
        // Not proud of this shit, but damn it I dont want to add join listener logic to all sounds smh..
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::stopAllSounds, 2);

        if (player.getName().equals("themithrandir")) {
            AttributeInstance attr = player.getAttribute(Attribute.SCALE);
            if (attr == null) return;

            attr.setBaseValue(0.9);
        }
    }
}
