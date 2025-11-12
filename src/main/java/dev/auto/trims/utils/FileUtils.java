package dev.auto.trims.utils;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

public class FileUtils {
    public static void deleteFolder(Path folder) {
        if (!Files.exists(folder)) return;

        try (var walk = Files.walk(folder)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            Main.getInstance().getLogger().warning(
                                    "Failed to delete " + path + ": " + e.getMessage()
                            );
                        }
                    });
        } catch (IOException e) {
            Main.getInstance().getLogger().warning(
                    "Failed to walk folder " + folder + ": " + e.getMessage()
            );
        }
    }

    /**
     * Attempts to delete folder now; returns true if the folder no longer exists after the attempt.
     */
    public static boolean tryDeleteFolder(Path folder) {
        deleteFolder(folder);
        return !Files.exists(folder);
    }

    /**
     * Tries to delete a folder with a few retries on the main thread using Bukkit scheduler.
     * This avoids blocking the server thread with sleeps and helps on Windows where files may be briefly locked.
     */
    public static void deleteFolderWithRetries(Path folder, int attempts, long delayTicks) {
        if (attempts <= 1) {
            deleteFolder(folder);
            return;
        }

        for (int i = 0; i < attempts; i++) {
            final boolean last = (i == attempts - 1);
            long delay = (i == 0) ? 0 : delayTicks * i;
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                boolean deleted = tryDeleteFolder(folder);
                if (deleted) {
                    return;
                }
                if (last) {
                    Main.getInstance().getLogger().warning("Failed to delete folder after retries: " + folder);
                }
            }, delay);
        }
    }

    public static void deleteObjectivesFile(UUID worldId) {
        Path folder = Main.getInstance().getDataFolder().toPath().resolve("data").resolve(worldId.toString());
        // Use retries to be resilient against brief file locks
        deleteFolderWithRetries(folder, 5, 20);
    }
}
