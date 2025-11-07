package dev.auto.trims.commands;

import dev.auto.trims.Main;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayEntityDebugCommand implements BasicCommand {
    private static float easeInOutBack(float x) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;

        if (x < 0.5f) {
            return (float) ((Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2.0);
        } else {
            return (float) ((Math.pow(2 * x - 2, 2) * ((c2 + 1) * (2 * x - 2) + c2) + 2) / 2.0);
        }
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        var sender = source.getSender();
        if (!(sender instanceof Player player)) return;

        if (!sender.hasPermission("trims.debug")) {
            sender.sendMessage("You don't have permission to use this command!");
            return;
        }

        Location playerLoc = player.getLocation().clone().add(1, 1.5, 0);
        playerLoc.setYaw(0f);
        playerLoc.setPitch(0f);

        BlockDisplay display = playerLoc.getWorld().spawn(playerLoc, BlockDisplay.class, entity -> {
            entity.setBlock(Material.BLACK_CONCRETE.createBlockData());

            Vector3f targetScale = new Vector3f(1.5f, 0.10f, 1.5f); // final size
            int durationTicks = 15; // 0.75s * 20 tps

            // start: almost zero X/Z, thin Y
            entity.setTransformation(new Transformation(
                    new Vector3f(),                       // translation
                    new Quaternionf(),                   // left rotation (identity)
                    new Vector3f(0.01f, targetScale.y, 0.01f), // tiny slab
                    new Quaternionf()                    // right rotation (identity)
            ));

            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (!entity.isValid()) {
                        cancel();
                        return;
                    }

                    tick++;
                    float t = Math.min(tick / (float) durationTicks, 1f); // 0 → 1
                    float eased = easeInOutBack(t);

                    // X/Z scale up with curve, Y stays thin/constant
                    Vector3f scale = new Vector3f(
                            targetScale.x * eased,
                            targetScale.y,
                            targetScale.z * eased
                    );

                    Transformation current = entity.getTransformation();

                    entity.setTransformation(new Transformation(
                            new Vector3f(current.getTranslation()), // keep position
                            new Quaternionf(),                      // no rotation
                            scale,
                            new Quaternionf()                       // no rotation
                    ));

                    if (t >= 1f) {
                        cancel();
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 1L);
        });

        player.sendMessage("Spawned display entity at " + playerLoc);
    }

    @Override
    public String permission() {
        return "trims.debug";
    }
}
