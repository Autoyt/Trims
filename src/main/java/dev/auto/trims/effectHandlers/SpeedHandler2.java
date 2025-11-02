package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.heavyEvents.MovementListener;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.effectHandlers.helpers.StatusBar;
import dev.auto.trims.managers.EffectManager;
import dev.auto.trims.managers.TrimManager;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedHandler2 extends OptimizedHandler implements IBaseEffectHandler, MovementListener, Listener, Runnable {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.EYE;

    // Charge: 0..165. +5 every 5t (~8.25s full) + movement gain.
    private static final int MAX_CHARGE = 165;
    private static final int RECHARGE_STEP = 10;

    private final Map<UUID, Integer> charge = new HashMap<>();
    private final Map<UUID, StatusBar> bars = new HashMap<>();

    public SpeedHandler2(Main instance) {
        super(defaultPattern);
        this.instance = instance;

        Bukkit.getPluginManager().registerEvents(this, instance);
        TrimManager.handlers.add(this);

        setActivationFunction(uuid -> getTrimCount(uuid) >= 4);
        setHideCooldown(20 * 3);

         Bukkit.getScheduler().runTaskTimer(instance, this, 5L, 5L);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int count = getTrimCount(id);

        if (count > 0) {
            int amp = Math.min(count, 4) - 1;
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.SPEED, 3600, amp, false, false));
        }
    }

    @Override
    public void run() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            int count = getTrimCount(id);
            boolean creativeOrSpec = player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;

            if (count >= 4) {
                StatusBar bar = bars.get(id);
                if (bar == null) {
                    bar = new StatusBar(id);
                    bar.setHideTicks(20 * 3);
                    bars.put(id, bar);
                }

                int c = charge.getOrDefault(id, 0);
                if (c < MAX_CHARGE) {
                    c = Math.min(MAX_CHARGE, c + RECHARGE_STEP);
                    charge.put(id, c);
                }

                float s = c / (float) MAX_CHARGE;
                int percent = Math.round(s * 100f);
                if (s >= 1.0f - 0.000001f) bar.setTitle("Dash | Ready!");
                else bar.setTitle("Charging " + percent + "%");

                if (s <= 0.10f) bar.setColor(BossBar.Color.WHITE);
                else if (s <= 0.25f) bar.setColor(BossBar.Color.RED);
                else if (s <= 0.65f) bar.setColor(BossBar.Color.YELLOW);
                else if (s <= 0.90f) bar.setColor(BossBar.Color.GREEN);
                else bar.setColor(BossBar.Color.BLUE);

                bar.setProgress(s);

                // flight arm on ground
                if (!creativeOrSpec) {
                    boolean grounded = player.isOnGround() || player.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid();
                    if (grounded) {
                        if (!player.getAllowFlight()) player.setAllowFlight(true);
                        player.setFlying(false);
                    }
                }
            } else {
                StatusBar bar = bars.remove(id);
                if (bar != null) bar.hide();

                if (!creativeOrSpec) {
                    if (player.getAllowFlight()) player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        super.onArmorChange(event);
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        UUID id = p.getUniqueId();
        if (getTrimCount(id) < 4) return;

        event.setCancelled(true);

        int c = charge.getOrDefault(id, 0);
        if (c <= 0) { p.setAllowFlight(false); return; }

        float s = c / (float) MAX_CHARGE;

        p.setAllowFlight(false);

        // exact velocity formula
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Vector vel = p.getVelocity();

        double horizMult = 1.25 * s;
        double yBoost = 0.15  * s;
        Vector boost = dir.multiply(horizMult);

        Vector newVel = vel.add(boost);
        newVel.setY(Math.max(vel.getY(), vel.getY() + yBoost));

        if (s > 0.0f) {
            double xz = Math.hypot(newVel.getX(), newVel.getZ());
            double maxXZ = 2.8 * s;
            if (xz > maxXZ) {
                double scale = maxXZ / xz;
                newVel.setX(newVel.getX() * scale);
                newVel.setZ(newVel.getZ() * scale);
            }
        }

        charge.put(id, 0);
        StatusBar bar = bars.get(id);
        if (bar != null) {
            bar.setTitle("Charging 0%");
            bar.setColor(BossBar.Color.WHITE);
            bar.setProgress(0f);
        }

        p.setVelocity(newVel);
        float vol = 0.25f + 0.75f * s;
        float pitch = 1.0f + 0.4f * s;
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, vol, pitch);
    }

    @Override
    public void onMovement(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        UUID id = p.getUniqueId();
        if (getTrimCount(id) < 4) return;

        boolean grounded = p.isOnGround() || p.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid();
        if (grounded) {
            if (!p.getAllowFlight()) p.setAllowFlight(true);
            p.setFlying(false);
        }

        if (!event.hasChangedBlock()) return;
        if (p.isGliding() || p.isFlying() || p.isInsideVehicle()) return;

        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || from == null || to.getWorld() != from.getWorld()) return;

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (dist == 0.0 || dist > 6.0) return;

        float fGain = (float) (dist * p.getVelocity().length() / 50.0);
        if (fGain <= 0f) return;

        int add = (int) Math.ceil(fGain * MAX_CHARGE);
        int c = Math.min(MAX_CHARGE, charge.getOrDefault(id, 0) + add);
        charge.put(id, c);

        StatusBar bar = bars.get(id);
        if (bar == null) {
            bar = new StatusBar(id);
            bar.setHideTicks(20 * 3);
            bars.put(id, bar);
        }
        float s = c / (float) MAX_CHARGE;
        int percent = Math.round(s * 100f);
        if (s >= 1.0f - 0.000001f) bar.setTitle("Dash | Ready!");
        else if (s >= 0.5f) bar.setTitle("Dash | Half Ready");
        else bar.setTitle("Charging " + percent + "%");

        if (s <= 0.10f) bar.setColor(BossBar.Color.WHITE);
        else if (s <= 0.25f) bar.setColor(BossBar.Color.RED);
        else if (s <= 0.65f) bar.setColor(BossBar.Color.YELLOW);
        else if (s <= 0.90f) bar.setColor(BossBar.Color.GREEN);
        else bar.setColor(BossBar.Color.BLUE);

        bar.setProgress(s);
    }
}
