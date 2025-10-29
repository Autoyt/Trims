package dev.auto.trims.effectHandlers;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TrimManager {
    private static final Main instance = Main.getInstance();
    private static final Map<UUID, PlayerArmorSlots> SLOTS = new ConcurrentHashMap<>();
    private static final Map<UUID, PotionEffectType> EFFECTS = new ConcurrentHashMap<>();
    // Coordinator state: what effects handlers requested this tick, and what we applied previously
    private static final Map<UUID, Map<PotionEffectType, PotionEffect>> DESIRED = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<PotionEffectType, PotionEffect>> APPLIED = new ConcurrentHashMap<>();

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

    public static void setEffect(UUID uuid, PotionEffectType type) {
        // Compatibility method (kept for legacy callers). Not used by current handlers.
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) {
            // Still record for later if needed
            EFFECTS.put(uuid, type);
            return;
        }

        PotionEffectType old = EFFECTS.get(uuid);
        if (old != null && old != type) {
            // Remove the previously tracked effect, not the new one
            p.removePotionEffect(old);
        }
        EFFECTS.put(uuid, type);
    }

    public static void clearEffect(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        PotionEffectType tracked = EFFECTS.remove(uuid);
        if (p != null && tracked != null) {
            p.removePotionEffect(tracked);
        }
    }

    /** Remove all effects previously applied by the coordinator for this player and clear state. */
    public static void clearAllEffects(UUID uuid) {
        Map<PotionEffectType, PotionEffect> applied = APPLIED.remove(uuid);
        if (applied == null) return;
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        for (PotionEffectType t : applied.keySet()) {
            p.removePotionEffect(t);
        }
    }

    /** Start the repeating ticker (if not already started). */
    public static void beginTick() {
        // Start of a reconciliation cycle: clear desired effects
        DESIRED.clear();
    }

    public static void wantEffect(UUID uuid, PotionEffect effect) {
        if (uuid == null || effect == null) return;
        DESIRED.computeIfAbsent(uuid, k -> new HashMap<>()).put(effect.getType(), effect);
    }

    public static void endTick() {
        // Reconcile desired vs applied and add/remove as needed
        Set<UUID> ids = new HashSet<>();
        ids.addAll(APPLIED.keySet());
        ids.addAll(DESIRED.keySet());

        for (UUID id : ids) {
            Player p = Bukkit.getPlayer(id);
            Map<PotionEffectType, PotionEffect> desired = DESIRED.getOrDefault(id, Collections.emptyMap());
            Map<PotionEffectType, PotionEffect> applied = APPLIED.computeIfAbsent(id, k -> new HashMap<>());

            if (p != null) {
                // Apply or refresh desired effects
                for (Map.Entry<PotionEffectType, PotionEffect> e : desired.entrySet()) {
                    PotionEffectType type = e.getKey();
                    PotionEffect eff = e.getValue();
                    PotionEffect prev = applied.get(type);

                    boolean needsApply = prev == null
                            || prev.getAmplifier() != eff.getAmplifier()
                            || prev.isAmbient() != eff.isAmbient()
                            || prev.hasParticles() != eff.hasParticles();
                    // Always refresh duration to keep it topped up
                    // Important: Bukkit will not replace a stronger effect with a weaker one unless forced.
                    // We only force if we previously applied a stronger amplifier and now want to downgrade.
                    boolean force = prev != null && eff.getAmplifier() < prev.getAmplifier();
                    if (needsApply || true) {
                        p.addPotionEffect(eff, force);
                        applied.put(type, eff);
                    }
                }

                // Remove effects we previously applied but are no longer desired
                Iterator<Map.Entry<PotionEffectType, PotionEffect>> it = applied.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<PotionEffectType, PotionEffect> ap = it.next();
                    if (!desired.containsKey(ap.getKey())) {
                        p.removePotionEffect(ap.getKey());
                        it.remove();
                    }
                }
            } else {
                // Player offline: just sync maps, removals will occur next time they are online
                APPLIED.put(id, new HashMap<>(desired));
            }

            // If nothing desired and nothing applied, clean up maps
            if (desired.isEmpty() && APPLIED.getOrDefault(id, Collections.emptyMap()).isEmpty()) {
                APPLIED.remove(id);
            }
        }

        // Done for this tick
        DESIRED.clear();
    }

    public static void start() {
        running = true; // enable work right away
        if (ticker == null || ticker.isCancelled()) {
            ticker = Bukkit.getScheduler().runTaskTimer(
                instance,
                new EffectUpdateTask(handlers),
                20L,
                20L
            );
        }
    }

    public static void pause() {
        running = false;
    }

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
        TrimManager.beginTick();
        for (IBaseEffectHandler h : handlers) {
            try {
                h.onTick();
                h.Tick();
            } catch (Throwable ex) {
                plugin.getLogger().warning("Error while updating effect " +
                        h.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
        TrimManager.endTick();
        final long ms = (System.nanoTime() - t0) / 1_000_000L;
        plugin.getLogger().fine("Handled " + handlers.size() + " effects in " + ms + "ms");
    }
}
