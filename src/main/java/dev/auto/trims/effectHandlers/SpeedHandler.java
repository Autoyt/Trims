package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.util.Vector;

import java.util.*;

public class SpeedHandler implements Listener, IBaseEffectHandler {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.EYE;
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Map<UUID, Long> lastDash = new HashMap<>();

    public SpeedHandler(Main instance) {
        this.instance = instance;
        // Ensure this handler participates in the ticker
        TrimManager.handlers.add(this);
    }

    // TODO Add boss bar with 3 segments for different charge levels.

    @Override
    public void onTick() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            PlayerArmorSlots slots = TrimManager.getSlots(id);
            int instanceCount = slots.instancesOfTrim(this.defaultPattern);

            if (instanceCount >= 4) {
                lv4Players.add(id);
            }
            else {
                lv4Players.remove(id);
            }
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        lv4Players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onGroundRearm(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        // simple grounded check
        if (p.isOnGround() || p.getLocation().subtract(0, 0.1, 0).getBlock().getType() != Material.AIR) {
            if (!p.getAllowFlight()) p.setAllowFlight(true);
            p.setFlying(false);
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        event.setCancelled(true);
        p.setAllowFlight(false);

        long now = System.currentTimeMillis();
        long last = lastDash.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < 3000) return;
        lastDash.put(p.getUniqueId(), now);

        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Vector vel = p.getVelocity();

        Vector boost = dir.multiply(1.2);
        double yBoost = 0.20;

        Vector newVel = vel.add(boost);
        newVel.setY(Math.max(vel.getY(), vel.getY() + yBoost));

        double xz = Math.hypot(newVel.getX(), newVel.getZ());
        double maxXZ = 2.8;
        if (xz > maxXZ) {
            double s = maxXZ / xz;
            newVel.setX(newVel.getX() * s);
            newVel.setZ(newVel.getZ() * s);
        }

        p.setVelocity(newVel);
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.4f);
        p.setFallDistance(0f);
    }
}
