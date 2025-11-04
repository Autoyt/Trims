package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.effectHandlers.helpers.StatusBar;
import dev.auto.trims.managers.EffectManager;
import dev.auto.trims.managers.TrimManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class StrengthHandler extends OptimizedHandler implements Listener, IBaseEffectHandler {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.WARD;

    // state
    @Getter
    private final Set<UUID> lv4Players = new HashSet<>();

    // ctor / registration
    public StrengthHandler(Main instance) {
        super(defaultPattern);
        this.instance = instance;
        TrimManager.handlers.add(this);

        setActivationFunction(uuid -> getTrimCount(uuid) >= 4);

        setHideCooldown(20 * 3);

        setBossBarConsumer((uuid, statusBar) -> {
            float status = statusBar.getProgress();

            // Dynamic title based on charge
            statusBar.setTitle(status >= 1f ? "Ready!" : "Charging...");

            // Color coding by thresholds
            if (status <= 0.10f) statusBar.setColor(net.kyori.adventure.bossbar.BossBar.Color.WHITE);
            else if (status <= 0.25f) statusBar.setColor(net.kyori.adventure.bossbar.BossBar.Color.RED);
            else if (status <= 0.65f) statusBar.setColor(net.kyori.adventure.bossbar.BossBar.Color.YELLOW);
            else if (status <= 0.90f) statusBar.setColor(net.kyori.adventure.bossbar.BossBar.Color.GREEN);
            else statusBar.setColor(net.kyori.adventure.bossbar.BossBar.Color.BLUE);
        });

        Bukkit.getScheduler().runTaskLater(instance, new ChargeTask(this), 1);
    }

    // tick
    @Override
    public void onlinePlayerTick(Player p) {
        UUID id = p.getUniqueId();
        int instanceCount = getTrimCount(id);

        // Maintain the set of players eligible for level 4 ability
        if (instanceCount >= 4) {
            lv4Players.add(id);
        } else {
            lv4Players.remove(id);
        }

        // Apply baseline strength based on instances
        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 2;
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.STRENGTH, 3600, amplifier, false, false));
        }
    }

    // events
    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        super.onArmorChange(e);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lv4Players.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (!lv4Players.contains(id)) return;

        StatusBar bar = getStatusBar(id);
        if (bar == null) return;

        float s = bar.getProgress();
        if (s < 1f) return;

        e.setDamage(e.getDamage() + 15);

        // Reset charge and reflect immediately on the bar
        bar.setTitle("Charging...");
        bar.setProgress(0f);

        p.playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f);
        org.bukkit.Color color = org.bukkit.Color.fromARGB(0xFFD6392E);
        Particle.DustOptions options = new Particle.DustOptions(color, 2f);
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 10, 0.3, 0.3, 0.3, 1.5, options);
    }

    // task
    static class ChargeTask implements Runnable {
        private final StrengthHandler h;

        public ChargeTask(StrengthHandler h) {
            this.h = h;
        }

        @Override
        public void run() {
            for (UUID id : h.lv4Players) {
                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.isOnline()) continue;

                StatusBar bar = h.getStatusBar(id);
                if (bar == null) continue;

                float prev = bar.getProgress();
                if (prev < 1f) {
                    float increment = 1f / (45f * 20f);
                    float next = Math.min(1f, prev + increment);
                    bar.setTitle(next >= 1f ? "Ready!" : "Charging...");
                    bar.setProgress(next);

                    // Threshold sounds
                    if (prev < 1f && next == 1f) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                    } else if (prev < 0.5f && next >= 0.5f) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.6f);
                    }
                }
            }
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), this, 1);
        }
    }
}
