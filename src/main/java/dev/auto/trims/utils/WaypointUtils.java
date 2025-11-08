package dev.auto.trims.utils;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;

public class WaypointUtils {
    public static Boolean SetWaypoint(Player player, String hex) {
        if (hex.length() != 7) throw new IllegalArgumentException("Hex must be 7 characters long.");
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "waypoint modify %id% color hex %hex%"
                    .replace("%id%", player.getName())
                    .replace("%hex%", hex.replace("#", "")));
            return true;
        } catch (CommandException error) {
            Main.getInstance().getLogger().warning("Failed to set waypoint for " + player.getName() + ": " + error.getMessage());
            return false;
        }
    }

    public static Boolean removeWaypoint(Player player) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "waypoint modify %id% color reset"
                    .replace("%id%", player.getName()));
            return true;
        } catch (CommandException error) {
            Main.getInstance().getLogger().warning("Failed to remove waypoint for " + player.getName() + ": " + error.getMessage());
            return false;
        }
    }
}
