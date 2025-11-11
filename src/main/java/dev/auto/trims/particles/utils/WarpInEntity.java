package dev.auto.trims.particles.utils;

import dev.auto.trims.Main;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class WarpInEntity {

    @Getter
    private final BlockDisplay display;
    private BukkitTask animationTask;

    public WarpInEntity(Location location) {
        Main.getInstance().getLogger().info("Warping in warp entity at " + location);
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location has no world");
        }

        Vector3f targetScale = new Vector3f(2f, 0.10f, 2f);
        int durationTicks = 15;

        this.display = world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(Material.BLACK_CONCRETE.createBlockData());
            entity.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.01f, targetScale.y, 0.01f),
                    new Quaternionf()
            ));
            entity.setPersistent(false);
            entity.setViewRange(64f);
            entity.setBrightness(new BlockDisplay.Brightness(15, 15));
        });

        this.animationTask = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!display.isValid()) {
                    cancel();
                    return;
                }

                tick++;
                float t = Math.min(tick / (float) durationTicks, 1f);

                // pick the easing you want:
                // float eased = easeInOutBack(t);
                float eased = easeOutBack(t);     // nice ease-out "for stop" style

                Vector3f scale = new Vector3f(
                        targetScale.x * eased,
                        targetScale.y,
                        targetScale.z * eased
                );

                Transformation current = display.getTransformation();

                display.setTransformation(new Transformation(
                        new Vector3f(current.getTranslation()),
                        new Quaternionf(),
                        scale,
                        new Quaternionf()
                ));

                if (t >= 1f) {
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // === Back easing variants ===

    // Ease-IN (starts slow, overshoots into motion)
    private float easeInBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return c3 * x * x * x - c1 * x * x;
    }

    // Ease-OUT (fast at start, overshoots and settles at end)
    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float t = x - 1f;
        return 1f + c3 * t * t * t + c1 * t * t;
    }

    // Your original ease-in-out-back (in both directions)
    private float easeInOutBack(float x) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;

        if (x < 0.5f) {
            return (float) ((Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2.0);
        } else {
            return (float) ((Math.pow(2 * x - 2, 2) * ((c2 + 1) * (2 * x - 2) + c2) + 2) / 2.0);
        }
    }

    public void destroy() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
}
