package effectHandlers;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TrimManager {
    private static final Main instance = Main.getInstance();
    private static final Map<UUID, PlayerArmorSlots> SLOTS = new ConcurrentHashMap<>();

    public static volatile boolean running = false;

    private static BukkitTask ticker;
    public static final List<IBaseEffectHandler> handlers = new ArrayList<>();
    
    public static PlayerArmorSlots getSlots(UUID uuid) {
        return SLOTS.computeIfAbsent(uuid, id -> new PlayerArmorSlots());
    }

    public static void setSlots(UUID uuid, PlayerArmorSlots slots) {
        SLOTS.put(uuid, slots);
    }

    public static void clear(UUID uuid) {
        SLOTS.remove(uuid);
    }

    public static void buildSlots(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        PlayerInventory inv = player.getInventory();

        PlayerArmorSlots slots = TrimManager.getSlots(uuid);
        slots.setBoots(deserializeTrim(inv.getBoots()));
        slots.setLeggings(deserializeTrim(inv.getLeggings()));
        slots.setChestplate(deserializeTrim(inv.getChestplate()));
        slots.setHelmet(deserializeTrim(inv.getHelmet()));
    }

    /** Start the repeating ticker (if not already started). */
    public static void start() {
        running = true; // enable work right away
        if (ticker == null || ticker.isCancelled()) {
            ticker = Bukkit.getScheduler().runTaskTimer(
                instance,
                new EffectUpdateTask(handlers),
                20L, // initial delay
                20L  // period
            );
        }
    }

    /** Stop doing work (ticker stays scheduled but will early-return). */
    public static void pause() {
        running = false;
    }

    /** Fully shutdown and release the scheduler task. */
    public static void stop() {
        running = false;
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    public static TrimPattern deserializeTrim(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        var meta = item.getItemMeta();
        if (!(meta instanceof ArmorMeta am)) return null;
        if (!am.hasTrim()) return null;
        ArmorTrim trim = am.getTrim();
        if (trim == null) return null;
        return trim.getPattern();
    }
}

class EffectUpdateTask implements Runnable {
    private final List<IBaseEffectHandler> handlers;
    private final Main plugin = Main.getInstance();

    public EffectUpdateTask(List<IBaseEffectHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void run() {
        // Only do work if the toggle is on
        if (!TrimManager.running) return;

        final long t0 = System.nanoTime();
        for (IBaseEffectHandler h : handlers) {
            try {
                h.onTick();
            } catch (Throwable ex) {
                plugin.getLogger().warning("Error while updating effect " +
                        h.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
        final long ms = (System.nanoTime() - t0) / 1_000_000L;
        plugin.getLogger().fine("Handled " + handlers.size() + " effects in " + ms + "ms");
    }
}
