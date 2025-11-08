package dev.auto.trims.commands;

import dev.auto.trims.Main;
import dev.auto.trims.crafting.CraftUtils;
import dev.auto.trims.effectHandlers.PlayerArmorSlots;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.particles.GhostStepFX;
import dev.auto.trims.world.BorderLandWorld;
import dev.auto.trims.world.WorldGenerator;
import dev.auto.trims.world.WorldManager;
import dev.auto.trims.world.WorldObjective;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class DebugCommands implements BasicCommand {

    private final Main plugin;

    public DebugCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        var sender = source.getSender();

        // Permission check (also backed by permission() method)
        if (!sender.hasPermission("trims.debug")) {
            sender.sendMessage("You don't have permission to use this command!");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /trimsdebug <subcommand>");
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {

            case "nether" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return;
                }
                World nether = Bukkit.getWorld("world_nether");
                if (nether == null) {
                    sender.sendMessage("world_nether not found.");
                    return;
                }
                Location loc = nether.getSpawnLocation();
                p.teleport(loc);
            }

            case "world" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return;
                }

                // List worlds or teleport to a given world
                if (args.length == 1) {
                    List<String> names = Bukkit.getWorlds().stream()
                            .map(World::getName)
                            .toList();
                    p.sendMessage("Available worlds: " + String.join(", ", names));
                    p.sendMessage("Usage: /trimsdebug world <name>");
                    return;
                }

                String query = args[1];
                World target = Bukkit.getWorld(query);

                if (target == null) {
                    // Try case-insensitive and partial prefix match as a convenience
                    String qLower = query.toLowerCase(Locale.ROOT);
                    for (World w : Bukkit.getWorlds()) {
                        if (w.getName().equalsIgnoreCase(query)) { target = w; break; }
                    }
                    if (target == null) {
                        for (World w : Bukkit.getWorlds()) {
                            if (w.getName().toLowerCase(Locale.ROOT).startsWith(qLower)) { target = w; break; }
                        }
                    }
                }

                if (target == null) {
                    p.sendMessage("World not found. Use /trimsdebug world to list worlds.");
                    return;
                }

                Location dest = target.getSpawnLocation();
                if (p.teleport(dest)) {
                    p.sendMessage("Teleported to world '" + target.getName() + "'.");
                } else {
                    p.sendMessage("Teleport failed.");
                }
            }

            case "set" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("Usage: /trimsdebug set <pattern> <material>");
                    return;
                }

                NamespacedKey patKey = NamespacedKey.fromString(args[1].toLowerCase(Locale.ROOT));
                if (patKey == null) patKey = NamespacedKey.minecraft(args[1].toLowerCase(Locale.ROOT));

                NamespacedKey matKey = NamespacedKey.fromString(args[2].toLowerCase(Locale.ROOT));
                if (matKey == null) matKey = NamespacedKey.minecraft(args[2].toLowerCase(Locale.ROOT));

                TrimPattern pattern = Registry.TRIM_PATTERN.get(patKey);
                TrimMaterial material = Registry.TRIM_MATERIAL.get(matKey);

                if (pattern == null) {
                    sender.sendMessage("Unknown pattern: " + patKey + ". Try coast, dune, rib, wayfinder…");
                    return;
                }
                if (material == null) {
                    sender.sendMessage("Unknown material: " + matKey + ". Try iron, gold, diamond, netherite, quartz…");
                    return;
                }

                CraftUtils.giveTrimmedNetheriteSet(p, pattern, material);
                sender.sendMessage("Gave Prot IV netherite set trimmed: " +
                        pattern.getKey() + " + " + material.getKey());
            }

            case "rc" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return;
                }
                Main.getInstance().reloadConfig();
                p.sendMessage("Reloaded config!");
            }

            case "heal" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return;
                }
                AttributeInstance maxHealth = p.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    p.setHealth(maxHealth.getValue());
                }
                p.setFoodLevel(20);
            }

            case "explode" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return;
                }
                Location loc = p.getLocation();
                float power = 1f;
                if (args.length > 1) {
                    try {
                        power = Float.parseFloat(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage("Invalid power value. Use a number like 2.0");
                        return;
                    }
                }
                loc.getWorld().createExplosion(loc, power);
            }

            case "spawn-ghost-block" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return;
                }
                GhostStepFX effect = new GhostStepFX();
                effect.run(plugin, p);
            }

            case "new-world" -> {
                if (!(sender instanceof Player p)) return;

                Set<UUID> players = new HashSet<>();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    players.add(onlinePlayer.getUniqueId());
                }

                WorldGenerator generator = new WorldGenerator();
                UUID worldId = generator.getWorldID();

                p.sendMessage("World is generating; you will be teleported when it finishes loading.");

                generator.whenDone().thenRun(() -> {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        long time = generator.getEndTime();
                        p.sendMessage("Generated in: %time%".replace("%time%", time + "ms"));

                        var bd = new BorderLandWorld(worldId, players, Structure.DESERT_PYRAMID);
                        World world = Bukkit.getWorld(worldId.toString());
                        if (world == null) {
                            p.sendMessage("World not found!");
                            return;
                        }

                        WorldObjective objective = bd.worldObjectives.get(Structure.DESERT_PYRAMID);
                        if (objective == null) {
                            p.sendMessage("Objective for DESERT_PYRAMID not found!");
                            return;
                        }
                        Location loc = objective.spawn();
                        if (loc == null) {
                            p.sendMessage("Spawn location not available for the selected objective!");
                            return;
                        }
                        p.teleport(loc);
                    });
                });
            }

            case "unload-world" -> {
                if (!(sender instanceof Player p)) return;
                World world = p.getWorld();

                BorderLandWorld bdl = WorldManager.getBorderWorld(world);
                if (bdl == null) {
                    sender.sendMessage("This world is not a BorderLand world or is not registered.");
                    return;
                }
                bdl.unload();
                p.sendMessage("Unloaded!");
            }

            case "new-objective" -> {
                if (!(sender instanceof Player p)) return;
                Location loc = p.getLocation();

                WorldObjective obj = new WorldObjective(WorldManager.getIdFromStructure(Structure.ANCIENT_CITY), loc, loc, loc);
                Set<UUID> onlinePlayerIds = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getUniqueId)
                        .collect(Collectors.toSet());

                if (args.length < 2) {
                    sender.sendMessage("Usage: /trimsdebug new-objective <structure> [index]");
                    return;
                }

                NamespacedKey key = NamespacedKey.fromString(args[1]);
                if (key == null) {
                    sender.sendMessage("Unknown structure name: " + args[1]);
                    return;
                }

                Structure name = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE).get(key);
                if (name == null) {
                    sender.sendMessage("Unknown structure name: " + args[1]);
                    return;
                }

                BorderLandWorld land = new BorderLandWorld(
                        p.getWorld().getUID(),
                        onlinePlayerIds,
                        name
                );

                int index = 0;
                if (args.length >= 3) {
                    try {
                        index = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage("Invalid index. Use 0 or 1.");
                        return;
                    }
                }
                land.createWayPoint(obj, index);
            }

            case "armor-profile" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /trimsdebug armor-profile <player>");
                    return;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found!");
                    return;
                }

                PlayerArmorSlots slots = TrimManager.getSlots(target.getUniqueId());
                sender.sendMessage(slots.toString());
            }

            default -> sender.sendMessage("Unknown subcommand. Try: world, nether, set, rc, heal, explode, spawn-ghost-block, armor-profile");
        }
    }

    @Override
    public @Nullable String permission() {
        // This ties into Paper’s command permission system.
        return "trims.debug";
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        var sender = source.getSender();
        if (!sender.hasPermission("trims.debug")) {
            return List.of();
        }

        // No args yet: suggest root subcommands
        if (args.length == 0) {
            return List.of(
                    "spawn-ghost-block",
                    "armor-profile",
                    "world",
                    "nether",
                    "explode",
                    "heal",
                    "rc",
                    "set",
                    "new-world"
            );
        }

        String first = args[0].toLowerCase(Locale.ROOT);

        // First argument: filter subcommands by what they've typed
        if (args.length == 1) {
            String q = first;
            return List.of(
                    "spawn-ghost-block",
                    "armor-profile",
                    "world",
                    "nether",
                    "explode",
                    "heal",
                    "rc",
                    "set"
            ).stream()
             .map(s -> s.toLowerCase(Locale.ROOT))
             .filter(s -> s.startsWith(q))
             .toList();
        }

        // Second arg suggestions

        // old example, still works if you ever add this subcommand back
        if (args.length == 2 && first.equals("min-fall-velocity")) {
            return List.of("-0.08", "-0.10", "-0.06");
        }

        if (args.length == 2 && first.equals("armor-profile")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }

        // /trimsdebug world <name>
        if (args.length == 2 && first.equals("world")) {
            String q = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(q))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        if (args.length == 2 && first.equalsIgnoreCase("new-world")) {
            Registry<Structure> registry = Registry.STRUCTURE;

            List<String> all = new ArrayList<>();
            if (registry != null) {
                for (Structure s : registry) {
                    all.add(registry.getKeyOrThrow(s).toString());
                }
            }

            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[1], all, completions);
            return completions;
        }

        // /trimsdebug set <pattern> <material>
        if (first.equals("set")) {
            if (args.length == 2) {
                String q = args[1].toLowerCase(Locale.ROOT);
                return RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.TRIM_PATTERN).stream()
                        .map(p -> p.getKey().getKey()) // "coast", "dune", etc
                        .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(q))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
            }

            if (args.length == 3) {
                String q = args[2].toLowerCase(Locale.ROOT);
                return RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.TRIM_MATERIAL).stream()
                        .map(m -> m.getKey().getKey()) // "iron", "gold", etc
                        .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(q))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
            }
        }

        return List.of();
    }

}
