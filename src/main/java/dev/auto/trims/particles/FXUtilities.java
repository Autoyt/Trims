package dev.auto.trims.particles;

import dev.auto.trims.Main;
import dev.auto.trims.particles.utils.CircleFX;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FXUtilities {
    public static void lv4Activation(Player p) {
        final World w = p.getWorld();
        final Location base = p.getLocation().clone();
        final double radius = 1.0, height = 4.0, dy = 0.06, dTheta = Math.PI / 14;

        new BukkitRunnable() {
            double y = 0, t = 0;

            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    if (!p.isOnline() || y > height) {
                        cancel();
                        return;
                    }
                    double x1 = radius * Math.cos(t), z1 = radius * Math.sin(t);
                    double x2 = radius * Math.cos(t + Math.PI), z2 = radius * Math.sin(t + Math.PI);
                    Location l1 = base.clone().add(x1, y, z1);
                    Location l2 = base.clone().add(x2, y, z2);
                    w.spawnParticle(Particle.END_ROD, l1, 4, 0, 0.06, 0, 0);
                    w.spawnParticle(Particle.END_ROD, l2, 4, 0, 0.06, 0, 0);
                    t += Math.PI / 14;
                    y += 0.06;
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L,1L);
    }



    public static CircleFX ConduitFX(Player p) {
        Color color = Color.fromARGB(204, 15, 166, 236);
        Particle.DustOptions options = new Particle.DustOptions(color, 0.75f);
        return new CircleFX(p).setParticle(Particle.DUST).setRadius(4).setPoints(144).setDustOptions(options).run();
    }

    public static CircleFX HeroOfTheVillagerFX(Player p) {
        Color color = Color.fromARGB(204, 15, 166, 45);
        Particle.DustOptions options = new Particle.DustOptions(color, 0.75f);
        return new CircleFX(p).setParticle(Particle.DUST).setRadius(4).setPoints(144).setDustOptions(options).run();
    }

    public static CircleFX AbsorptionFX(Player p) {
        Color color = Color.fromARGB(0xFFD6392E);
        Particle.DustOptions options = new Particle.DustOptions(color, 2f);
        return new CircleFX(p).setParticle(Particle.DUST).setRadius(12).setPoints(122).setDustOptions(options).run();
    }

    public static int speedToARGB(Vector vel, double slow, double fast, int alpha) {
        double t = (vel.length() - slow) / Math.max(1e-9, fast - slow);
        t = Math.max(0.0, Math.min(1.0, t));
        t = t * t * (3 - 2 * t);
        double r, g, b;
        if (t < 0.5) {
        double u = t * 2.0;
        r = 255.0 * u; g = 255.0; b = 0.0;
        } else {
        double u = (t - 0.5) * 2.0;
        r = 255.0; g = 255.0 * (1.0 - u); b = 0.0;
        }
        int R = (int)Math.round(r), G = (int)Math.round(g), B = (int)Math.round(b);
        return ((alpha & 0xFF) << 24) | (R << 16) | (G << 8) | B;
    }

      private static double smoothstep(double a, double b, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - a)/(b - a)));
        return t * t * (3 - 2 * t);
      }
}