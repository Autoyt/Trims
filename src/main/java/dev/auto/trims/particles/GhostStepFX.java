package dev.auto.trims.particles;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class GhostStepFX {
    private BlockDisplay display;
    private Player target;
    private BukkitTask task;

    public void run(final Plugin plugin, final Player player) {
        this.target = player;

        Location center = player.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation().add(0.5, 0.5, 0.5);
        display = (BlockDisplay) center.getWorld().spawnEntity(center, EntityType.BLOCK_DISPLAY);

        BlockData data = Bukkit.createBlockData(Material.SLIME_BLOCK);
        display.setBlock(data);

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!update()) {
                if (task != null) task.cancel();
            }
        }, 0L, 5L);
    }


    private boolean update() {
        if (display == null || display.isDead() || target == null || !target.isOnline()) {
            remove();
            return false;
        }

        Location targetFeet = target.getLocation().clone().subtract(0, 1, 0);
        Location dispLoc = display.getLocation();

        if (targetFeet.getWorld() != dispLoc.getWorld()) {
            remove();
            return false;
        }

        if (targetFeet.distanceSquared(dispLoc) >= 15.0) {
            remove();
            return false;
        }

        return true;
    }

    private void remove() {
        if (display != null && !display.isDead()) {
            display.remove();
        }
        display = null;
    }
}
