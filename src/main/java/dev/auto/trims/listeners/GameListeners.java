package dev.auto.trims.listeners;

import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.TrimManager;
import dev.auto.trims.effectHandlers.heavyEvents.MovementListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameListeners implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        TrimManager.clear(uuid);
        TrimManager.clearAllEffects(uuid);
        TrimManager.clearEffect(uuid);

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            TrimManager.stop();
        }
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

        if (!TrimManager.running) {
            TrimManager.start();
        }

        TrimManager.buildSlots(uuid);
        // Not proud of this shit, but damn it I dont want to add join listener logic to all sounds smh..
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::stopAllSounds, 2);
    }
}
