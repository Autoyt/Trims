package dev.auto.trims.commands;

import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.PlayerArmorSlots;
import dev.auto.trims.effectHandlers.TrimManager;
import dev.auto.trims.particles.GhostStepFX;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

            case "rc" -> {
                Main.getInstance().reloadConfig();
                Player p = (Player) sender;
                p.sendMessage("Reloaded config!");
            }

            case "heal" -> {
                Player p = (Player) sender;
                p.setHealth(20);
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
        if (args.length == 1) return List.of("spawn-ghost-block", "armor-profile", "nether", "explode");

        if (args.length == 2 && args[0].equalsIgnoreCase("min-fall-velocity"))
            return List.of("-0.08", "-0.10", "-0.06");

        if (args.length == 2 && args[0].equalsIgnoreCase("armor-profile")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return List.of();
    }
}
