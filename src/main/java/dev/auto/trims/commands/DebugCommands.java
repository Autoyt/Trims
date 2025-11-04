package dev.auto.trims.commands;

import dev.auto.trims.Main;
import dev.auto.trims.crafting.CraftUtils;
import dev.auto.trims.effectHandlers.PlayerArmorSlots;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.particles.GhostStepFX;
import dev.auto.trims.world.BorderLand;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
                new BorderLand(p.getUniqueId());
                p.sendMessage("Running!");
            }

            case "new-objective" -> {
                if (!(sender instanceof Player p)) return;
                Location loc = p.getLocation();

                WorldObjective obj = new WorldObjective(Structure.ANCIENT_CITY, loc, loc, loc);
                BorderLand land = new BorderLand(p.getUniqueId());
                land.createWayPoint(obj, args.length > 1 ? Integer.parseInt(args[1]) : 0);
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

            default -> sender.sendMessage("Unknown subcommand. Try: nether, set, rc, heal, explode, spawn-ghost-block, armor-profile");
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
