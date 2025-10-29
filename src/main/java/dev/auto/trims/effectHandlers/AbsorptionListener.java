package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.particles.FXUtilities;
import dev.auto.trims.particles.utils.CircleFX;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

public class AbsorptionListener implements IBaseEffectHandler, Listener, Runnable {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.VEX;
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Map<UUID, CircleFX> fx = new HashMap<>();
    private final Map<UUID, Integer> nearbyPlayerCount = new HashMap<>();

    public AbsorptionListener(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);

        instance.getServer().getScheduler().runTaskTimer(instance, this, 1, 1);
        // TODO add glowing red, adjust particles color
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount >= 4) {
            if (!fx.containsKey(id)) {
                fx.put(id, FXUtilities.AbsorptionFX(player));
            }
            lv4Players.add(id);
        }
        else {
            CircleFX circleFX = fx.remove(id);
            if (circleFX != null) {
                circleFX.cancel();
            }
            lv4Players.remove(id);
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lv4Players.remove(id);
    }

    @Override
    public void run() {
        nearbyPlayerCount.clear();

        for (UUID id : lv4Players) {
            Player player = instance.getServer().getPlayer(id);
            if (player == null) continue;

            Location loc = player.getLocation();

            Collection<Player> nearby = loc.getWorld().getNearbyPlayers(
                    loc,
                    12,
                    p -> !p.equals(player)
            );

            nearbyPlayerCount.put(id, nearby.size());
        }
    }
}
