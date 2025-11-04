package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.managers.EffectManager;
import dev.auto.trims.managers.TrimManager;
import lombok.Getter;
import dev.auto.trims.effectHandlers.helpers.StatusBar;
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

public class ResistanceHandler extends OptimizedHandler implements Listener, IBaseEffectHandler {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.SILENCE;

    @Getter
    private final Set<UUID> lv4Players = new HashSet<>();

    public ResistanceHandler(Main instance) {
        super(defaultPattern);
        this.instance = instance;
        TrimManager.handlers.add(this);

        setActivationFunction(uuid -> getTrimCount(uuid) >= 4);
        setHideCooldown(20 * 3);

        setBossBarConsumer((uuid, statusBar) -> {
            float status = statusBar.getProgress();
            statusBar.setTitle(status >= 1f ? "Ready!" : "Charging...");
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
        if (instanceCount >= 4) {
            lv4Players.add(id);
        } else {
            lv4Players.remove(id);
        }

        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 2;
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.RESISTANCE, 3600, amplifier, false, false));
        }
    }

    // events
    @EventHandler
    @Override
    public void onArmorChange(PlayerArmorChangeEvent e) {
        super.onArmorChange(e);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lv4Players.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (!lv4Players.contains(id)) return;

        StatusBar bar = getStatusBar(id);
        if (bar == null) return;

        float s = bar.getProgress();
        if (s < 1f) return;

        event.setDamage(0);

        bar.setTitle("Charging...");
        bar.setProgress(0f);

        p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f);
        org.bukkit.Color color = org.bukkit.Color.fromARGB(0x20D620);
        Particle.DustOptions options = new Particle.DustOptions(color, 2f);
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 10, 0.3, 0.3, 0.3, 1.5, options);
    }

    // task
    static class ChargeTask implements Runnable {
        private final ResistanceHandler h;

        public ChargeTask(ResistanceHandler h) {
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
