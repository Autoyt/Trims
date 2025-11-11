package dev.auto.trims.utils;

import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtils {
    public static Location randomInCircle(Location center, double radius) {
        World world = center.getWorld();
        double angle = Math.random() * 2 * Math.PI;
        double r = Math.sqrt(Math.random()) * radius;

        double dx = Math.cos(angle) * r;
        double dz = Math.sin(angle) * r;

        Location base = center.getWorld().getBlockAt(center).getLocation().add(0.5, 0, 0.5);
        return base.add(dx, 0, dz);
    }

}
