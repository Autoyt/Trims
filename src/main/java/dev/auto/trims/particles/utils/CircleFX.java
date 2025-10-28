package dev.auto.trims.particles.utils;

import dev.auto.trims.Main;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class CircleFX {
    private final Player player;
    private BukkitTask task;
    private int radius;
    private int points;
    private Particle particle;
    private Particle.DustOptions dustOptions;
    private final Map<Integer, Double> lastY = new HashMap<>();
    private final double lerpFactor = 0.25;
    private final double maxStep = 0.35;

    public CircleFX(Player player) {
        this.player = player;
    }

    public CircleFX run() {
        Main instance = Main.getInstance();
        if (task != null) return this;

        task = new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }

                final Location base = player.getLocation();
                final World w = base.getWorld();
                final double cx = base.getX(), cz = base.getZ();
                final double startY = base.getY() + 2.5;
                final double epsilon = 0.02;

                for (int i = 0; i < points; i++) {
                    double a = (2 * Math.PI * i) / points;
                    double x = cx + radius * Math.cos(a);
                    double z = cz + radius * Math.sin(a);

                    RayTraceResult hit = w.rayTraceBlocks(
                        new Location(w, x, startY, z),
                        new Vector(0, -1, 0),
                        8.0,
                        FluidCollisionMode.NEVER,
                        true
                    );

                    double targetY;
                    if (hit != null) {
                        targetY = hit.getHitPosition().getY() + epsilon;
                    } else {
                        int bx = Location.locToBlock(x), bz = Location.locToBlock(z);
                        targetY = w.getHighestBlockYAt(bx, bz) + 1.0 + epsilon;
                    }

                    double prevY = lastY.getOrDefault(i, targetY);
                    double lerped = prevY + (targetY - prevY) * lerpFactor;

                    double delta = lerped - prevY;
                    if (delta >  maxStep) lerped = prevY + maxStep;
                    if (delta < -maxStep) lerped = prevY - maxStep;

                    if (particle == Particle.DUST && dustOptions != null) {
                        w.spawnParticle(particle, x, lerped, z, 1, 0, 0, 0, dustOptions);
                    } else {
                        w.spawnParticle(particle, x, lerped, z, 1, 0, 0, 0, 0);
                    }

                    lastY.put(i, lerped);
                }
            }
        }.runTaskTimer(instance, 0L, 1L);

        return this;
    }

    public CircleFX setRadius(int radius) {
        this.radius = radius;
        return this;
    }

    public CircleFX setPoints(int points) {
        this.points = points;
        return this;
    }

    public CircleFX setParticle(Particle particle) {
        this.particle = particle;
        return this;
    }

    public CircleFX setDustOptions(Particle.DustOptions dustOptions) {
        this.dustOptions = dustOptions;
        return this;
    }

    public void cancel() {
        if (task != null) task.cancel();
        task = null;
    }
}
