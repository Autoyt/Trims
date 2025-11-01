package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.managers.EffectManager;
import dev.auto.trims.managers.TrimManager;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
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
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Float> charge = new HashMap<>();

    // ctor / registration
    public StrengthHandler(Main instance) {
        super(defaultPattern);
        this.instance = instance;
        TrimManager.handlers.add(this);

        Bukkit.getScheduler().runTaskLater(instance, new ChargeTask(this), 1);
    }

    // charge accessors
    private float getCharge(UUID id) {
        return charge.getOrDefault(id, 0f);
    }

    private void setCharge(UUID id, float v) {
        charge.put(id, Math.max(0f, Math.min(1f, v)));
    }

    // bossbar
    private void showBossBar(Player p) {
        UUID id = p.getUniqueId();
        bossBars.computeIfAbsent(id, k -> BossBar.bossBar(Component.text("Charging..."), getCharge(id), Color.YELLOW, Overlay.PROGRESS));
        p.showBossBar(bossBars.get(id));
    }

    private void hideBossBar(Player p) {
        UUID id = p.getUniqueId();
        BossBar bar = bossBars.remove(id);
        if (bar != null) p.hideBossBar(bar);
        charge.remove(id);
    }

    private void updateBar(Player p, float prev, float next) {
        UUID id = p.getUniqueId();
        BossBar bar = bossBars.get(id);
        if (bar == null) return;

        bar.name(next >= 1f ? Component.text("Ready!") : Component.text("Charging..."));

        if (next <= 0.10f) bar.color(Color.WHITE);
        else if (next <= 0.25f) bar.color(Color.RED);
        else if (next <= 0.65f) bar.color(Color.YELLOW);
        else if (next <= 0.90f) bar.color(Color.GREEN);
        else bar.color(Color.BLUE);

        bar.progress(next);

        if (prev < 1f && next == 1f) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        else if (prev < 0.5f && next >= 0.5f) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.6f);
    }

    // tick
    @Override
    public void onlinePlayerTick(Player p) {
        UUID id = p.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount >= 4) {
            if (lv4Players.add(id)) showBossBar(p);
        }
        else {
            if (lv4Players.remove(id)) hideBossBar(p);
        }

        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 1;
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
        hideBossBar(e.getPlayer());
        lv4Players.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (!lv4Players.contains(id)) return;

        float s = getCharge(id);
        if (s < 1f) return;

        e.setDamage(e.getDamage() + 15);

        setCharge(id, 0f);
        updateBar(p, s, 0f);

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

                float prev = h.getCharge(id);
                if (prev < 1f) {
                    float next = Math.min(1f, prev + (1f / 100f));
                    h.setCharge(id, next);
                    h.updateBar(p, prev, next);
                }
            }
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), this, 1);
        }
    }
}
