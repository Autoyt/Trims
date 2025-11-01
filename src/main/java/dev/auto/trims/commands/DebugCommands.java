package dev.auto.trims.commands;

import dev.auto.trims.Main;
import dev.auto.trims.crafting.CraftUtils;
import dev.auto.trims.effectHandlers.PlayerArmorSlots;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.particles.GhostStepFX;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class DebugCommands implements TabExecutor {
    private final Main plugin;

    public DebugCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("trims.debug")) {
            sender.sendMessage("You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " min-fall-velocity <number>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "nether" -> {
                Player p = (Player) sender;
                World nether = Bukkit.getWorld("world_nether");
                assert nether != null;
                Location loc = nether.getSpawnLocation();
                p.teleport(loc);
            }

            case "set" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("Usage: /gear set <pattern> <material>");
                    return true;
                }

                // TrimPattern/TrimMaterial are registry entries (not enums)
                NamespacedKey patKey = NamespacedKey.fromString(args[1].toLowerCase(Locale.ROOT));
                if (patKey == null) patKey = NamespacedKey.minecraft(args[1].toLowerCase(Locale.ROOT));
                NamespacedKey matKey = NamespacedKey.fromString(args[2].toLowerCase(Locale.ROOT));
                if (matKey == null) matKey = NamespacedKey.minecraft(args[2].toLowerCase(Locale.ROOT));

                TrimPattern pattern = Registry.TRIM_PATTERN.get(patKey);
                TrimMaterial material = Registry.TRIM_MATERIAL.get(matKey);

                if (pattern == null) {
                    sender.sendMessage("Unknown pattern: " + patKey + ". Try vanilla like coast, dune, rib, wayfinder…");
                    return true;
                }
                if (material == null) {
                    sender.sendMessage("Unknown material: " + matKey + ". Try iron, gold, diamond, netherite, quartz…");
                    return true;
                }

                CraftUtils.giveTrimmedNetheriteSet(p, pattern, material);
                sender.sendMessage("Gave Prot IV netherite set trimmed: " +
                        RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN) + " + " + RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL));
                return true;
            }


            case "rc" -> {
                Main.getInstance().reloadConfig();
                Player p = (Player) sender;
                p.sendMessage("Reloaded config!");
            }

            case "heal" -> {
                Player p = (Player) sender;
                AttributeInstance maxHealth = p.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    p.setHealth(maxHealth.getValue());
                }
                p.setFoodLevel(20);
            }

            case "explode" -> {
                Player p = (Player) sender;
                Location loc = p.getLocation();
                float power = args.length > 1 ? Float.parseFloat(args[1]) : 1f;
                loc.getWorld().createExplosion(loc, power);
            }

            case "spawn-ghost-block" -> {
                GhostStepFX effect = new GhostStepFX();
                effect.run(plugin, (Player) sender);
            }
            case "armor-profile" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /" + label + " armor-profile <player>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found!");
                    return true;
                }

                PlayerArmorSlots slots = TrimManager.getSlots(target.getUniqueId());
                sender.sendMessage(slots.toString());
            }
            default -> sender.sendMessage("Unknown subcommand. Try: min-fall-velocity");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("trims.debug")) return List.of();
        if (args.length == 1) return List.of("spawn-ghost-block", "armor-profile", "nether", "explode", "heal", "rc", "set");

        if (args.length == 2 && args[0].equalsIgnoreCase("min-fall-velocity"))
            return List.of("-0.08", "-0.10", "-0.06");

        if (args.length == 2 && args[0].equalsIgnoreCase("armor-profile")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        if (args[0].equalsIgnoreCase("set")) {
        if (args.length == 2) {
            String q = args[1].toLowerCase(Locale.ROOT);
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).stream()
                    .map(p -> p.getKey().getKey()) // or .toString() for namespaced
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(q))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        if (args.length == 3) {
            String q = args[2].toLowerCase(Locale.ROOT);
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).stream()
                    .map(m -> m.getKey().getKey()) // or .toString() for namespaced
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(q))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
    }

        return List.of();
    }
}
