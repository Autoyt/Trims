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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;


public class HeroOfTheVillagerHandler implements IBaseEffectHandler, Listener, Runnable {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.SENTRY;
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Set<UUID> nearbyPlayers = new HashSet<>();
    private final Map<UUID, CircleFX> fx = new HashMap<>();

    public HeroOfTheVillagerHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount >= 4) {
            if (!fx.containsKey(id)) {
                fx.put(id, FXUtilities.ConduitFX(player));
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

        if (instanceCount > 0) {
            TrimManager.wantEffect(id, new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 3600, 0, false, false));
        }
    }

    @Override
    public void run() {
        nearbyPlayers.clear();

        for (UUID id : lv4Players) {
            Player player = instance.getServer().getPlayer(id);
            if (player == null) continue;

            Location loc = player.getLocation();

            Collection<Player> nearby = loc.getWorld().getNearbyPlayers(
                    loc,
                    4,
                    p -> !p.equals(player)
            );

            for (Player p : nearby) {
                nearbyPlayers.add(p.getUniqueId());
            }

        }

        for (UUID id : nearbyPlayers) {
            Player player = instance.getServer().getPlayer(id);
            if (player == null) continue;

            handleNearby(player);
        }
    }

    public void handleNearby(Player player) {
        PotionEffect active = player.getPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        if (active == null || active.getDuration() < 40) {
            PotionEffect nearbyEffect = new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 3600, 0, false, false);
            player.addPotionEffect(nearbyEffect);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        CircleFX circleFX = fx.remove(uuid);
        if (circleFX != null) {
            circleFX.cancel();
        }

        lv4Players.remove(uuid);
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }
}

