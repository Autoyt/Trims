package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.particles.FXUtilities;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class TeleportHandler implements IBaseEffectHandler, Listener, Runnable {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.SPIRE;
    private final Set<UUID> sneakingPlayers = new HashSet<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Integer> cooldown = new HashMap<>();
    private final Set<UUID> ready = new HashSet<>();

    public TeleportHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);

        instance.getServer().getScheduler().runTaskTimer(instance, this, 1, 1);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount > 0) {
            // Ensure bossbar is visible when the player has the trim
            showBossBar(id);
        } else {
            // Hide bossbar when no trims, but keep state maps intact until player quits
            BossBar bar = bossBars.get(id);
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
    }

    private void showBossBar(UUID id) {
        int instances = getTrimCount(id, defaultPattern);
        if (instances < 0) return;

        Player player = instance.getServer().getPlayer(id);
        if (player == null) {
            bossBars.remove(id);
            return;
        }

        if (!bossBars.containsKey(id) && player.isOnline()) {
            BossBar bar = BossBar.bossBar(Component.text("Charging 0%"), 0, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
            bossBars.put(id, bar);
        }

        BossBar bar = bossBars.get(id);
        player.showBossBar(bar);
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
        // After slots are rebuilt by handleEquip, adjust cooldown and bossbar without clearing state
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount > 0) {
            // Ensure bossbar exists and is visible
            showBossBar(id);
            // Set/adjust cooldown to appropriate value for the new period
            int period = Math.max(2, 40 - (instanceCount - 1) * 10) * 20;
            if (!cooldown.containsKey(id)) {
                // Initialize as ready (no cooldown) if none present
                cooldown.put(id, 0);
            } else {
                int remaining = cooldown.getOrDefault(id, 0);
                // Clamp remaining cooldown to not exceed the new period
                if (remaining > period) {
                    cooldown.put(id, period);
                }
            }
        } else {
            // No trims: hide bossbar but keep maps intact until player leaves
            BossBar bar = bossBars.get(id);
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (event.isSneaking()) {
            sneakingPlayers.add(id);
        } else {
            sneakingPlayers.remove(id);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);
        if (instanceCount <= 0) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead() || arrow.isOnGround() || arrow.isInBlock()) {
                    cancel();
                    return;
                }

                Location loc = arrow.getLocation();
                Vector vel = arrow.getVelocity();

                int argb = FXUtilities.speedToARGB(vel, 0.2, 3.0, 255);
                int rgb  = argb & 0x00FFFFFF;
                var options = new Particle.DustTransition(
                    Color.fromRGB(rgb),
                    Color.fromRGB(0xFFFFFF),
                    0.65f
                );

                loc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 12, 0.2, 0.2, 0.2, 0.01, options);
            }
        }.runTaskTimer(instance, 1, 1);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);
        if (!(instanceCount > 0)) return;

        int cooldownTicks = cooldown.getOrDefault(id, 0);
        Entity target = event.getHitEntity();

        if (target != null) {
            cooldownTicks = Math.max(0, cooldownTicks - 20);
            cooldown.put(id, cooldownTicks);
        }

        if (cooldownTicks > 0) return;

        int cooldownValue = Math.max(2, 40 - (instanceCount - 1) * 10) * 20;

        if (target instanceof Player targetPlayer) {
            if (!sneakingPlayers.contains(id)) return;
            if (targetPlayer.getUniqueId().equals(id)) return;
            if (!(targetPlayer.getGameMode() == GameMode.SURVIVAL)) return;

            Location loc = targetPlayer.getLocation();
            targetPlayer.playSound(loc, Sound.ENTITY_SHULKER_BULLET_HIT, 1f, 1f);

            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 3 * 20, 1));
            Vector velocity = targetPlayer.getVelocity().clone();
            velocity.setY(1);
            targetPlayer.setVelocity(velocity);

            cooldown.put(id, cooldownValue);
        }
        else if (sneakingPlayers.contains(id)) {
            Location loc = arrow.getLocation();
            loc.setY(loc.getY() + 1);
            player.teleport(loc);

            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            cooldown.put(id, cooldownValue);
        }

    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        sneakingPlayers.remove(id);

        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            event.getPlayer().hideBossBar(bar);
        }

        cooldown.remove(id);
        ready.remove(id);
    }

    @Override
    public void run() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            int instanceCount = getTrimCount(id, defaultPattern);
            if (instanceCount <= 0) continue;

            int period = Math.max(2, 40 - (instanceCount - 1) * 10) * 20;
            int ticks = cooldown.getOrDefault(id, 0);

            if (ticks > 0) ticks--;
            cooldown.put(id, ticks);

            BossBar bar = bossBars.get(id);
            if (bar == null) continue;
            float prog = 1f - (ticks / (float) period);
            bar.progress(Math.max(0f, Math.min(1f, prog)));

            if (ticks > 0) {
                bar.name(Component.text(String.format("Cooldown: %.1fs", ticks / 20f)));
                ready.remove(id);
            }
            else {
                if (!ready.contains(id)) {
                    bar.name(Component.text("Ready!"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                    ready.add(id);
                }
            }
        }
    }
}
