package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.effectHandlers.helpers.StatusBar;
import dev.auto.trims.managers.TrimManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

public class AbsorptionListener extends OptimizedHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.VEX;
    private final Map<UUID, Integer> countdown = new HashMap<>();

    public AbsorptionListener(Main instance) {
        super(defaultPattern);
        this.instance = instance;
        TrimManager.handlers.add(this);

        setActivationFunction(uuid -> getTrimCount(uuid) > 0);
        setHideCooldown(20 * 3);
        setBossBarConsumer((uuid, statusBar) -> {
            float s = statusBar.getProgress();
            int percent = Math.round(s * 100f);
            statusBar.setTitle("Charging " + percent + "%");
            statusBar.setColor(net.kyori.adventure.bossbar.BossBar.Color.YELLOW);
        });

        Bukkit.getScheduler().runTaskLater(instance, new TickTask(this), 5);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount > 0) {
            AttributeInstance maxAbs = player.getAttribute(Attribute.MAX_ABSORPTION);
            if (maxAbs != null) {
                double desired = Math.min(40.0, instanceCount * 10.0);
                if (maxAbs.getBaseValue() != desired) maxAbs.setBaseValue(desired);
            }
        } else {
            countdown.remove(id);
            AttributeInstance maxAbs = player.getAttribute(Attribute.MAX_ABSORPTION);
            if (maxAbs != null && maxAbs.getBaseValue() != 0.0) maxAbs.setBaseValue(0.0);
        }
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        super.onArmorChange(event);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        countdown.remove(id);

        StatusBar bar = getStatusBar(id);
        if (bar != null) {
            bar.hide();
        }
    }

    static class TickTask implements Runnable {
        private final AbsorptionListener h;
        public TickTask(AbsorptionListener h) { this.h = h; }
        @Override
        public void run() {
            final int PERIOD = 20 * 15;

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID uid = p.getUniqueId();
                if (!(p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) continue;
                int instances = h.getTrimCount(uid);

                if (instances > 0) {
                    double cap = instances * 10.0;
                    double maxHealth = p.getMaxHealth();
                    double currentTotal = p.getHealth() + p.getAbsorptionAmount();
                    double totalMax = maxHealth + cap;
                    boolean allowCharge = currentTotal < totalMax;

                    StatusBar bar = h.getStatusBar(uid);

                    if (allowCharge) {
                        int time = h.countdown.getOrDefault(uid, 0) + 5;
                        if (time >= PERIOD) {
                            h.countdown.put(uid, 0);
                            double cur = p.getAbsorptionAmount();
                            if (cur < cap) {
                                p.setAbsorptionAmount(Math.min(cap, cur + 10.0));
                                Location loc = p.getLocation();

                                Color color = Color.fromRGB(0xFBD716);
                                Particle.DustOptions options = new Particle.DustOptions(color, 1.25f);
                                loc.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.3, 0.3, 0.3, 1.5, options);
                                p.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
                            }

                            if (bar != null) {
                                bar.setTitle("Charging 0%");
                                bar.setProgress(0f);
                            }
                        }

                        else {
                            h.countdown.put(uid, time);
                            if (bar != null) {
                                float progress = Math.min(1f, (float) time / (float) PERIOD);
                                int percent = Math.min(100, Math.max(0, Math.round(progress * 100f)));
                                bar.setTitle("Charging " + percent + "%");
                                bar.setProgress(progress);
                            }
                        }
                    }
                    else {
                        h.countdown.put(uid, 0);
                        if (bar != null) {
                            bar.setTitle("Charging 0%");
                            bar.setProgress(0f);
                        }
                    }
                }
                else {
                    h.countdown.remove(uid);
                }
            }
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), this, 5);
        }
    }
}
