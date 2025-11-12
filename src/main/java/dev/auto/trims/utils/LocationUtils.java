package dev.auto.trims.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

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

    public static String getLocationString(Location loc) {
        return loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    public static String locationToCommand(Location location, Player player) {
        return "/tp " + player.getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

}
