package dev.auto.trims.managers;

import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.PlayerArmorSlots;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrimManager {
    private static final Map<UUID, PlayerArmorSlots> SLOTS = new ConcurrentHashMap<>();
    public static final List<IBaseEffectHandler> handlers = new ArrayList<>();

    public static PlayerArmorSlots getSlots(UUID id) {
        return SLOTS.computeIfAbsent(id, k -> new PlayerArmorSlots());
    }

    public static TrimPattern deserializeTrim(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        var m = item.getItemMeta();
        if (!(m instanceof ArmorMeta am)) return null;
        if (!am.hasTrim()) return null;
        ArmorTrim tr = am.getTrim();
        return (tr == null) ? null : tr.getPattern();
    }

    public static void setSlots(UUID id, PlayerArmorSlots s) {
        SLOTS.put(id, s);
    }

    public static void clear(UUID id) {
        SLOTS.remove(id);
    }

    public static void buildSlots(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;
        PlayerInventory inv = p.getInventory();
        var s = getSlots(id);
        s.setBoots(deserializeTrim(inv.getBoots()));
        s.setLeggings(deserializeTrim(inv.getLeggings()));
        s.setChestplate(deserializeTrim(inv.getChestplate()));
        s.setHelmet(deserializeTrim(inv.getHelmet()));
    }

    public static BukkitTask start() {
        Runnable r = new EffectUpdateTask(handlers);
        return Bukkit.getScheduler().runTaskTimer(Main.getInstance(), r, 1, 1);
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
        if (handlers.isEmpty()) return;
        if (plugin.getServer().getOnlinePlayers().isEmpty()) return;

        long t0 = System.nanoTime();
        EffectManager.beginTick();

        for (IBaseEffectHandler h : handlers) {
            try {
                h.onTick();
                h.Tick();
            } catch (Throwable ex) {
                plugin.getLogger().warning("Error " + h.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        EffectManager.endTick();
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        plugin.getLogger().fine("Handled " + handlers.size() + " in " + ms + "ms");
    }
}
