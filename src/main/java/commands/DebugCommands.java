package commands;

import dev.auto.trims.Main;
import listeners.GhostStepListener;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import particles.GhostStepFX;

import java.util.List;

public class DebugCommands implements TabExecutor {
    private final Main plugin;
    private final GhostStepListener ghost;

    public DebugCommands(Main plugin, GhostStepListener ghost) {
        this.plugin = plugin;
        this.ghost = ghost;
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
            case "min-fall-velocity" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /" + label + " min-fall-velocity <number>");
                    return true;
                }
                try {
                    double v = Double.parseDouble(args[1]);
                    ghost.MIN_FALLING_VEL = v;
                    sender.sendMessage("Set min falling velocity to " + v);
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Not a valid number: " + args[1]);
                }
            }
            case "toggle-dj" -> {
                ghost.TOGGLE = !ghost.TOGGLE;
            }
            case "spawn-ghost-block" -> {
                GhostStepFX effect = new GhostStepFX();
                effect.run(plugin, (Player) sender);
            }
            default -> sender.sendMessage("Unknown subcommand. Try: min-fall-velocity");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("trims.debug")) return List.of();
        if (args.length == 1) return List.of("min-fall-velocity", "spawn-ghost-block", "toggle-dj");
        if (args.length == 2 && args[0].equalsIgnoreCase("min-fall-velocity"))
            return List.of("-0.08", "-0.10", "-0.06");
        return List.of();
    }
}
