package dev.auto.trims.listeners;

import dev.auto.trims.effectHandlers.TrimManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class GameListeners implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        TrimManager.clear(uuid);

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            TrimManager.stop();
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
    }
}
