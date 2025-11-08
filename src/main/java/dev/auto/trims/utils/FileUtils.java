package dev.auto.trims.utils;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

public class FileUtils {
    public static void deleteFolder(Path folder) {
        if (!Files.exists(folder)) return;

        try {
            Files.walk(folder)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
            Main.getInstance().getLogger().warning("Failed to delete folder %folder%: ".replace("%folder%", folder.toString()) + e.getMessage());
        }
    }

    public static void deleteObjectivesFile(UUID worldId) {
        Path folder = Main.getInstance().getDataFolder().toPath().resolve("data").resolve(worldId.toString());
        deleteFolder(folder);
    }
}
