package dev.auto.trims.particles;

import dev.auto.trims.Main;
import dev.auto.trims.particles.utils.ColorUtils;
import dev.auto.trims.world.WorldManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.joml.Matrix4f;

import java.util.UUID;

public class OutputRift {
    private final Location origin;
    private BukkitTask ambientTask;
    private BukkitTask spinTask;
    private BukkitTask wayPointTask;
    public static NamespacedKey outputRiftKey = new NamespacedKey(Main.getInstance(), "output_rift");
    private static final UUID riftId = UUID.randomUUID();

    private ItemDisplay display;
    private final Particle.DustTransition ambientDustOptions = new Particle.DustTransition(
            Color.fromRGB(ColorUtils.hexToRgbInt("#570670")),
            Color.fromRGB(ColorUtils.hexToRgbInt("#2e2d2e")),
            0.8f
    );

    private final Team selectedTeam;

    public OutputRift(Location origin) {
        this.origin = origin.clone().getBlock().getLocation().add(0.5, 3, 0.5);
        this.origin.setRotation(0, 0);

        Block block = this.origin.getBlock();
        block.setType(Material.LIGHT, false);
        Light light = (Light) block.getBlockData();
        light.setLevel(15);
        block.setBlockData(light);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamId = UUID.randomUUID().toString();
        Team team = scoreboard.getTeam(teamId);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamId);
            team.color(NamedTextColor.LIGHT_PURPLE);
        }
        this.selectedTeam = team;

        spawnAmbientParticles();
        spawnDisplayEntity();
        setupWaypoint();
    }

    private void spawnAmbientParticles() {
        ambientTask = new BukkitRunnable() {
            @Override
            public void run() {

                World world = origin.getWorld();
                if (world == null) return;
                world.spawnParticle(
                        Particle.DUST_COLOR_TRANSITION,
                        origin,
                        10,
                        3.5, 3.5, 3.5,
                        5,
                        ambientDustOptions
                );
            }
        }.runTaskTimer(Main.getInstance(), 0, 1);
    }

    private void spawnDisplayEntity() {
        World world = origin.getWorld();
        if (world == null) return;

        int animationDuration = 20 * 3;

        Matrix4f endMatrix = new Matrix4f()
                .scale(1.6f)
                .rotateY((float) Math.toRadians(180));

        world.spawn(origin, Interaction.class, i -> {
            i.setInteractionWidth(1.2f);
            i.setInteractionHeight(1.2f);
            i.setResponsive(true);

            var pdc = i.getPersistentDataContainer();
            pdc.set(outputRiftKey, PersistentDataType.STRING, riftId.toString());
        });

        world.spawn(this.origin, ItemDisplay.class, entity -> {
            this.display = entity;

            ItemStack displayStack = new ItemStack(Material.ENDER_EYE);
            ItemMeta meta = displayStack.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            displayStack.setItemMeta(meta);
            entity.setItemStack(displayStack);

            entity.setViewRange(256f);
            entity.setBrightness(new Display.Brightness(15, 15));
            selectedTeam.addEntity(entity);
            entity.setGlowing(true);

            // intro animation
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!entity.isValid()) return;

                    entity.setTransformationMatrix(new Matrix4f().scale(0.0f));
                    entity.setInterpolationDelay(0);
                    entity.setInterpolationDuration(animationDuration);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!entity.isValid()) return;
                            entity.setTransformationMatrix(endMatrix);

                            startSpinTask();
                        }
                    }.runTaskLater(Main.getInstance(), 1L);
                }
            }.runTaskLater(Main.getInstance(), 1L);
        });
    }

    private void startSpinTask() {
        final float speed = (float) Math.toRadians(30);

        this.spinTask = new BukkitRunnable() {
            float angle = 0f;

            @Override
            public void run() {
                if (display == null || !display.isValid()) {
                    cancel();
                    return;
                }

                angle += speed;
                if (angle > Math.PI * 2) {
                    angle -= (float) (Math.PI * 2);
                }

                Matrix4f spinMatrix = new Matrix4f()
                        .scale(1.6f)
                        .rotateY(angle);

                display.setInterpolationDelay(0);
                display.setInterpolationDuration(4);
                display.setTransformationMatrix(spinMatrix);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L);
    }

    private void setupWaypoint() {
        UUID id = UUID.randomUUID();
        ArmorStand stand = origin.getWorld().spawn(origin, ArmorStand.class);
        stand.setPersistent(true);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setCanMove(false);
        stand.addScoreboardTag(id.toString());
        AttributeInstance waypointRange = stand.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (waypointRange != null) waypointRange.setBaseValue(600000);

        wayPointTask = new BukkitRunnable() {
            @Override
            public void run() {
                boolean commandStatus = Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "waypoint modify @e[type=armor_stand,tag=%id%,limit=1] color hex e612e6".replace("%id%", id.toString()));
                if (!commandStatus) {
                    Main.getInstance().getLogger().warning("Failed to set waypoint color for output rift.");
                    this.cancel();
                }
            }

        }.runTaskTimer(Main.getInstance(), 1, 20);
    }

    public void stop() {
        if (ambientTask != null) {
            ambientTask.cancel();
        }
        if (spinTask != null) {
            spinTask.cancel();
        }
        if (display != null && display.isValid()) {
            display.remove();
        }

        wayPointTask.cancel();

        origin.getBlock().setType(Material.AIR);
        selectedTeam.unregister();
    }
}
