package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.customEvents.BossBarChangeValueEvent;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

public class StrengthHandler implements Listener, IBaseEffectHandler {
    private final Main instance;
    private final TrimPattern pattern = TrimPattern.WARD;

    @Getter
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Float> charge = new HashMap<>();

    public StrengthHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);

        // Begin continuous recharge loop
        Bukkit.getScheduler().runTaskLater(instance, new ChargeTask(this), 1);
    }

    private float getCharge(UUID id) {
        return charge.getOrDefault(id, 0f);
    }

    private void setCharge(UUID id, float v) {
        charge.put(id, Math.max(0f, Math.min(1f, v)));
    }

    private void showBar(Player p) {
        UUID id = p.getUniqueId();
        bars.computeIfAbsent(id, k ->
            BossBar.bossBar(Component.text("Charging Strike..."), getCharge(id), Color.YELLOW, Overlay.PROGRESS)
        );
        p.showBossBar(bars.get(id));
    }

    private void hideBar(Player p) {
        UUID id = p.getUniqueId();
        BossBar bar = bars.remove(id);
        if (bar != null) p.hideBossBar(bar);
        charge.remove(id);
    }

    private void updateBar(Player p, float prev, float next) {
        UUID id = p.getUniqueId();
        BossBar bar = bars.get(id);
        if (bar == null) return;

        // Names
        if (next >= 1f) bar.name(Component.text("Strike Ready!"));
        else bar.name(Component.text("Charging..."));

        // Colors
        if (next <= 0.10f) bar.color(Color.WHITE);
        else if (next <= 0.25f) bar.color(Color.RED);
        else if (next <= 0.65f) bar.color(Color.YELLOW);
        else if (next <= 0.90f) bar.color(Color.GREEN);
        else bar.color(Color.BLUE);

        bar.progress(next);

        // Sound triggers
        if (prev < 1f && next == 1f)
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        else if (prev < 0.5f && next >= 0.5f)
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.6f);
    }

    @Override
    public void onTick() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            int count = getTrimCount(id, pattern);

            if (count >= 4) {
                if (activePlayers.add(id)) {
                    showBar(player);
                }
            } else {
                if (activePlayers.remove(id)) {
                    hideBar(player);
                }
            }
        }
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();

        TrimManager.buildSlots(id);
        int count = TrimManager.getSlots(id).instancesOfTrim(pattern);

        if (count >= 4) {
            if (activePlayers.add(id)) showBar(p);
        } else {
            if (activePlayers.remove(id)) hideBar(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hideBar(event.getPlayer());
        activePlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (!activePlayers.contains(id)) return;

        float s = getCharge(id);
        if (s < 1f) return;

        // Apply STRIKE
        event.setDamage(event.getDamage() * 3.0);

        // Reset
        float prev = s;
        setCharge(id, 0f);
        updateBar(p, prev, 0f);

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1f);
    }

    static class ChargeTask implements Runnable {
        private final StrengthHandler handler;
        public ChargeTask(StrengthHandler handler) { this.handler = handler; }

        @Override
        public void run() {
            for (UUID id : handler.activePlayers) {
                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.isOnline()) continue;

                float prev = handler.getCharge(id);
                if (prev < 1f) {
                    float next = Math.min(1f, prev + (1f / 100f));
                    handler.setCharge(id, next);
                    handler.updateBar(p, prev, next);
                }
            }
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), this, 1);
        }
    }
}
