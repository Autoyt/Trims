package listeners;

import effecthandler.TrimManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListeners implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        TrimManager.clear(e.getPlayer().getUniqueId());

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            TrimManager.stop();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!TrimManager.running) {
            TrimManager.start();
        }

        TrimManager.buildSlots(event.getPlayer().getUniqueId());
    }
}
