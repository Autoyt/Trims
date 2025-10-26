package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.customEvents.BossBarChangeValueEvent;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import java.util.*;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.bossbar.BossBar.Color;

public class SpeedHandler implements Listener, IBaseEffectHandler {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.EYE;
    @Getter
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Float> dashEnergy = new HashMap<>();

    public SpeedHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);

        Bukkit.getScheduler().runTaskLater(instance, new EnergyRechargeTask(this), 1);
    }

    public Float getDashEnergy(UUID id) {
        return dashEnergy.getOrDefault(id, 0.0f);
    }

    private void showBossBar(UUID id) {
        if (!lv4Players.contains(id)) return;
        Player player = instance.getServer().getPlayer(id);
        if (player == null) {
            bossBars.remove(id);
            return;
        }

        Float energy = dashEnergy.computeIfAbsent(id, k -> 0.0f);

        if (!bossBars.containsKey(id) && player.isOnline()) {
            BossBar bar = BossBar.bossBar(Component.text("Dash | Charging..."), energy, Color.YELLOW, Overlay.PROGRESS);
            bossBars.put(id, bar);
        }

        BossBar bar = bossBars.get(id);
        player.showBossBar(bar);
    }


    @Override
    public void onTick() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            PlayerArmorSlots slots = TrimManager.getSlots(id);
            int instanceCount = slots.instancesOfTrim(this.defaultPattern);


            // Maintain LV4 membership used by the dash
            if (instanceCount >= 4) {
                lv4Players.add(id);
                showBossBar(id);
            }
            else {
                lv4Players.remove(id);
                BossBar bar = bossBars.get(player.getUniqueId());
                if (bar != null) {
                    player.hideBossBar(bar);
                }
                bossBars.remove(player.getUniqueId());
            }

            // Arm/disarm double-jump based on lv4 membership and grounded state (authoritative fallback once per tick)
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                if (lv4Players.contains(id)) {
                    // simple grounded check
                    if (player.isOnGround() || player.getLocation().subtract(0, 0.1, 0).getBlock().getType() != Material.AIR) {
                        if (!player.getAllowFlight()) player.setAllowFlight(true);
                        player.setFlying(false);
                    }
                } else {
                    if (player.getAllowFlight()) player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }

            if (instanceCount > 0) {
                int amplifier = Math.min(instanceCount, 4) - 1;
                PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);
                if (current == null || current.getDuration() <= 1200 || current.getAmplifier() != amplifier) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2400, amplifier, false, false));
                }
            } else {
                // Remove SPEED when no EYE-trim pieces are worn
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }

    private void handleRearm(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (!lv4Players.contains(p.getUniqueId())) {
            p.setAllowFlight(false);
            return;
        }

        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        // simple grounded check
        if (p.isOnGround() || p.getLocation().subtract(0, 0.1, 0).getBlock().getType() != Material.AIR) {
            if (!p.getAllowFlight()) p.setAllowFlight(true);
            p.setFlying(false);
        }
    }

    private void handleBBIncrease(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player p = event.getPlayer();
        if (!lv4Players.contains(p.getUniqueId())) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        if (p.isGliding() || p.isFlying() || p.isInsideVehicle()) return;
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to == null || to.getWorld() != from.getWorld()) return;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (dist == 0.0 || dist > 6.0) return;

        float value = (float) (dist * p.getVelocity().length() / 30);
        Bukkit.getPluginManager().callEvent(new BossBarChangeValueEvent(p.getUniqueId(), value));
    }

    @EventHandler
    public void handleBossBarValueChange(BossBarChangeValueEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!lv4Players.contains(player.getUniqueId())) return;

        UUID id = player.getUniqueId();
        float delta = event.getValue();

        float cur = dashEnergy.getOrDefault(id, 0.0f);
        float next = Math.max(0.0f, Math.min(1.0f, cur + delta));
        dashEnergy.put(id, next);

        BossBar bar = bossBars.get(id);
        if (bar == null) {
            showBossBar(id);
            bar = bossBars.get(id);
            if (bar == null) return;
        }

        boolean crossedHalf = cur < 0.5f && next >= 0.5f;
        boolean crossedFull = cur < 1.0f && next >= 1.0f;

        if (crossedFull) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        } else if (crossedHalf) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.6f);
        }

        if (next >= 1.0f) {
            bar.name(Component.text("Dash | Ready!"));
        } else if (next >= 0.5f) {
            bar.name(Component.text("Dash | Half Ready"));
        } else {
            bar.name(Component.text("Dash | Charging..."));
        }

        if (next >= 0.00f && next <= 0.10f) {
            bar.color(Color.WHITE);
        } else if (next > 0.10f && next <= 0.25f) {
            bar.color(Color.RED);
        } else if (next > 0.25f && next <= 0.65f) {
            bar.color(Color.YELLOW);
        } else if (next > 0.65f && next <= 0.90f) {
            bar.color(Color.GREEN);
        } else {
            bar.color(Color.BLUE);
        }

        bar.progress(next);
    }


    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        // Provide sounds/FX + rebuild slots
        handleEquip(event, defaultPattern);

        // Immediately update LV4 membership and bossbar/flight state so UI feels responsive
        final Player player = event.getPlayer();
        final UUID id = player.getUniqueId();

        // Slots were rebuilt in handleEquip, but calling again is safe if event order changes later
        TrimManager.buildSlots(id);
        final PlayerArmorSlots slots = TrimManager.getSlots(id);
        final int count = slots.instancesOfTrim(this.defaultPattern);

        if (count >= 4) {
            lv4Players.add(id);
            showBossBar(id);
            // Allow double jump to arm while grounded
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                if (player.isOnGround() && !player.getAllowFlight()) player.setAllowFlight(true);
            }
        } else {
            if (lv4Players.remove(id)) {
                BossBar bar = bossBars.get(id);
                if (bar != null) player.hideBossBar(bar);
                bossBars.remove(id);
            }
            // Ensure flight is disabled when losing LV4
            if (player.getAllowFlight()) player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        BossBar bar = bossBars.get(id);
        if (bar != null) {
            event.getPlayer().hideBossBar(bar);
        }
        lv4Players.remove(id);
        bossBars.remove(id);
        dashEnergy.remove(id);
    }

    // TODO add some checks for damaged armor.

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        handleRearm(event);
        handleBBIncrease(event);

    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (!lv4Players.contains(p.getUniqueId())) return;

        // Always cancel the vanilla flight toggle for double-jump behavior
        event.setCancelled(true);

        // Current charge in [0..1]
        float energy = dashEnergy.getOrDefault(p.getUniqueId(), 0.0f);
        float s = Math.max(0.0f, Math.min(1.0f, energy));

        // Perform dash regardless of charge; scale strength by current charge
        p.setAllowFlight(false); // disarm until re-armed when grounded

        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Vector vel = p.getVelocity();

        double horizMult = 1.3 * s;      // full charge = 1.3x boost
        double yBoost = 0.10 * s;     // full charge = +0.10 Y
        Vector boost = dir.multiply(horizMult);

        Vector newVel = vel.add(boost);
        newVel.setY(Math.max(vel.getY(), vel.getY() + yBoost));

        // Cap only when there is some strength to avoid slowing the player at 0 charge
        if (s > 0.0f) {
            double xz = Math.hypot(newVel.getX(), newVel.getZ());
            double maxXZ = 2.8 * s; // full charge cap = 2.8
            if (xz > maxXZ) {
                double scale = maxXZ / xz;
                newVel.setX(newVel.getX() * scale);
                newVel.setZ(newVel.getZ() * scale);
            }
        }

        // Consume exactly the used charge (proportional)
        if (s > 0.0f) {
            Bukkit.getPluginManager().callEvent(new BossBarChangeValueEvent(p.getUniqueId(), -s));
        }

        p.setVelocity(newVel);
        // Whoosh sound scales with charge
        float vol = 0.25f + 0.75f * s; // 0.25..1.0
        float pitch = 1.0f + 0.4f * s; // 1.0..1.4
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, vol, pitch);
        p.setFallDistance(0f);
    }
}

class EnergyRechargeTask implements Runnable {
    private final Main instance = Main.getInstance();
    private final SpeedHandler handler;

    EnergyRechargeTask(SpeedHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        if (!handler.getLv4Players().isEmpty()) {
            for (UUID id : handler.getLv4Players()) {
                Player player = instance.getServer().getPlayer(id);
                if (player == null || !player.isOnline()) continue;

                final Float energy = handler.getDashEnergy(id);
                if (energy >= 1.0f) continue;

                Float addIncrement = 1.0f / 100f;
                Bukkit.getPluginManager().callEvent(new BossBarChangeValueEvent(id, addIncrement));
            }
        }
        Bukkit.getScheduler().runTaskLater(instance, new EnergyRechargeTask(handler), 1);
    }
}
