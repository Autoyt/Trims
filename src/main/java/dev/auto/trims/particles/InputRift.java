package dev.auto.trims.particles;

import dev.auto.trims.Main;
import dev.auto.trims.particles.utils.ColorUtils;
import dev.auto.trims.world.BorderLandWorld;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class InputRift {
    private static final float REGISTRY_DISTANCE = 15f;

    private final Location origin;
    private BorderLandWorld bd;
    private final Structure structure;
    @Setter
    Player placer;

    private BukkitTask fingerTask;
    private BukkitTask ambientTask;
    private BukkitTask spinTask;
    private ItemDisplay display;

    private final int totalTicks = 20 * 15;
    private int ticksSinceSpawn = 0;

    private final Team selectedTeam;

    private final Particle.DustTransition fingerDustOptions = new Particle.DustTransition(
            Color.fromRGB(ColorUtils.hexToRgbInt("#22de0d")),
            Color.fromRGB(ColorUtils.hexToRgbInt("#de6b0d")),
            1.10f
    );

    private final Particle.DustTransition ambientDustOptions = new Particle.DustTransition(
            Color.fromRGB(ColorUtils.hexToRgbInt("#2f6364")),
            Color.fromRGB(ColorUtils.hexToRgbInt("#89c359")),
            0.8f
    );

    private final Particle.DustOptions teleportDustOptions = new Particle.DustOptions(
            Color.fromRGB(ColorUtils.hexToRgbInt("#531378")),
            1.0f
    );

    public InputRift(Location origin, Structure structure) {
        this.origin = origin.clone().getBlock().getLocation().add(0.5, 4, 0.5);
        this.origin.setRotation(0, 0);

        this.structure = structure;

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
            team.color(NamedTextColor.DARK_GREEN);
        }
        this.selectedTeam = team;

        spawnDisplayEntity();
        spawnAmbientParticles();

        new BukkitRunnable() {
            @Override
            public void run() {
                origin.getWorld().playSound(origin, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f);
            }

        }.runTaskLater(Main.getInstance(), 1);
    }

    private void spawnDisplayEntity() {
        World world = origin.getWorld();
        if (world == null) return;

        int animationDuration = 20 * 3;

        Matrix4f endMatrix = new Matrix4f()
                .scale(1.6f)
                .rotateY((float) Math.toRadians(180));

        world.spawn(this.origin, ItemDisplay.class, entity -> {
            this.display = entity;

            ItemStack displayStack = new ItemStack(Material.ENDER_EYE);
            ItemMeta meta = displayStack.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            displayStack.setItemMeta(meta);
            entity.setItemStack(displayStack);

            entity.setPersistent(false);
            entity.setViewRange(64f);
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

                            startFingerTask(animationDuration);
                            startSpinTask(); // <-- start continuous spinning
                        }
                    }.runTaskLater(Main.getInstance(), 1L);
                }
            }.runTaskLater(Main.getInstance(), 1L);
        });
    }

    private void tick() {
        World world = origin.getWorld();

        if (ticksSinceSpawn > 20 * 10 && ticksSinceSpawn % 20 == 0 && ticksSinceSpawn < totalTicks) {
            world.playSound(origin, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
        }

        if (ticksSinceSpawn >= totalTicks) {
            fingerTask.cancel();
            ambientTask.cancel();

            Collection<Player> nearbyPlayers = world.getNearbyPlayers(
                origin,
                REGISTRY_DISTANCE,
                p -> true
            );


            Set<UUID> uuids = nearbyPlayers.stream()
                    .map(Player::getUniqueId)
                    .collect(Collectors.toSet());

            try {
                bd = new BorderLandWorld(uuids, structure);
                bd.setLeader(placer);
            }
            catch (IllegalStateException e) {
                placer.sendMessage(MiniMessage.miniMessage().deserialize("<red>Error Encountered: " + e.getMessage()));
                this.stop();
                return;
            }

            for (Player p : nearbyPlayers) {
                p.setGlowing(false);
                try { selectedTeam.removeEntity(p); } catch (IllegalStateException ignored) {}

                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 10, 0.3, 0.3, 0.3, 1.5, teleportDustOptions);

                bd.addPlayer(p);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    stop();
                    cancel();
                }

            }.runTaskLater(Main.getInstance(), 20);
            return;
        }

    }

     private void startSpinTask() {
        final float speed = (float) Math.toRadians(6 * 5);

        this.spinTask = new BukkitRunnable() {
            float angle = (float) Math.toRadians(180 * 5);

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
                display.setInterpolationDuration(5);
                display.setTransformationMatrix(spinMatrix);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L);
    }

    private void spawnAmbientParticles() {
        ambientTask = new BukkitRunnable() {
            @Override
            public void run() {
                ticksSinceSpawn++;
                if (!(ticksSinceSpawn > totalTicks)) {
                    tick();
                }

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

    private void startFingerTask(int animationDuration) {
        this.fingerTask = new BukkitRunnable() {
            @Override
            public void run() {
                drawFingers();
            }
        }.runTaskTimer(Main.getInstance(), animationDuration + 2L, 5L);
    }

    private void drawFingers() {
        if (display == null || !display.isValid()) return;
        World world = display.getWorld();
        if (world == null) return;

        Location drawOrigin = display.getLocation().clone().add(0, -0.5f, 0);

        Collection<Player> nearbyPlayers = world.getNearbyPlayers(
                origin,
                REGISTRY_DISTANCE,
                p -> true
        );

        for (Player p : nearbyPlayers) {
            Location toLoc = p.getLocation().clone().add(0, 1, 0);
            selectedTeam.addEntity(p);
            p.setGlowing(true);
            drawFinger(drawOrigin, toLoc);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayers.contains(p)) continue;
            if (selectedTeam.hasEntity(p)) {
                p.setGlowing(false);
                try { selectedTeam.removeEntity(p); } catch (IllegalStateException ignored) {}
            }
        }
    }

    private void drawFinger(Location from, Location to) {
        World w = from.getWorld();
        if (w == null) return;

        new BukkitRunnable() {
            double d = 0;
            final double step = 0.35;
            final Vector dir = to.clone().subtract(from).toVector().normalize();
            final double len = from.distance(to);

            @Override
            public void run() {
                if (len <= 0.01) {
                    cancel();
                    return;
                }

                d = Math.min(d + step, len);
                Location spot = from.clone().add(dir.clone().multiply(d));

                w.spawnParticle(
                        Particle.DUST_COLOR_TRANSITION,
                        spot,
                        1,
                        0, 0, 0,
                        0,
                        fingerDustOptions
                );

                if (d >= len) {
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    public void stop() {
        if (fingerTask != null) {
            fingerTask.cancel();
        }
        if (ambientTask != null) {
            ambientTask.cancel();
        }
        if (spinTask != null) {
            spinTask.cancel();
        }
        if (display != null && display.isValid()) {
            display.remove();
        }

        origin.getBlock().setType(Material.AIR);

        // Clear glowing and team membership for players safely
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (selectedTeam.hasEntity(p)) {
                p.setGlowing(false);
                try { selectedTeam.removeEntity(p); } catch (IllegalStateException ignored) {}
            }
        }
        // Remove display entity from team as well
        if (display != null && display.isValid()) {
            try { selectedTeam.removeEntity(display); } catch (IllegalStateException ignored) {}
        }

        // Finally, unregister the team
        try { selectedTeam.unregister(); } catch (IllegalStateException ignored) {}
    }
}
