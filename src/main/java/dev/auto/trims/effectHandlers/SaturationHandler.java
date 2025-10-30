package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SaturationHandler implements IBaseEffectHandler, Listener, Runnable {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.SNOUT;
    private final Map<UUID, Integer> cooldown = new HashMap<>();

    public SaturationHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);

        instance.getServer().getScheduler().runTaskTimer(instance, this, 1, 1);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            int instanceCount = getTrimCount(id, defaultPattern);
            if (!(instanceCount > 0)) continue;

            final int period = 20 * 5 * instanceCount;

            int ticks = cooldown.getOrDefault(id, 0);
            cooldown.put(id, ticks + 1);

            if (ticks >= period) {
                player.setSaturation(20.0f);
                player.setFoodLevel(20);

                Location loc = player.getLocation();
                Color color = Color.fromRGB(0xFF0F26);
                Particle.DustOptions options = new Particle.DustOptions(color, 1.25f);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.3, 0.3, 0.3, 1.5, options);

                player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.3f);
                cooldown.put(id, 0);
            }
        }
    }

    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }
}
