package dev.auto.trims.effectHandlers.helpers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.PlayerArmorSlots;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.particles.FXUtilities;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class OptimizedHandler implements Listener, Runnable {
    private final Map<UUID, Integer> instancesOfTrim = new ConcurrentHashMap<>();
    private final Map<UUID, StatusBar> bossBars = new HashMap<>();
    private final TrimPattern defaultPattern;
    private Function<UUID, Boolean> statusBarActivation;
    private BiConsumer<UUID, StatusBar> statusBarConsumer;
    private int hideTicks;

    public OptimizedHandler(TrimPattern pattern) {
        this.defaultPattern = pattern;

        Main.getInstance().getServer().getScheduler().runTaskTimer(Main.getInstance(), this, 1, 5);
    }

    protected void setActivationFunction(Function<UUID, Boolean> function) {
        this.statusBarActivation = function;
    }

    protected void setBossBarConsumer(BiConsumer<UUID, StatusBar> consumer) {
        this.statusBarConsumer = consumer;
    }

    protected void setHideCooldown(int ticks) {
        this.hideTicks = ticks;
    }

    protected StatusBar getStatusBar(UUID uuid) {
        return bossBars.get(uuid);
    }

    protected int getTrimCount(UUID uuid) {
        return instancesOfTrim.computeIfAbsent(uuid, this::computeTrimCount);
    }

    private int computeTrimCount(UUID id) {
        PlayerArmorSlots s = TrimManager.getSlots(id);
        return s.instancesOfTrim(defaultPattern);
    }

    private int updateTrimCache(UUID id) {
        PlayerArmorSlots slots = TrimManager.getSlots(id);
        int count = slots.instancesOfTrim(defaultPattern);

        instancesOfTrim.put(id, count);
        return count;
    }

    @EventHandler
    protected void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        bossBars.remove(id);
        instancesOfTrim.remove(id);
    }

    @EventHandler(ignoreCancelled = true)
    protected void onArmorChange(PlayerArmorChangeEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final ItemStack oldItem = event.getOldItem();
        final ItemStack newItem = event.getNewItem();

        TrimPattern oldPattern = null, newPattern = null;
        if (oldItem != null && !oldItem.getType().isAir()) {
            var meta = oldItem.getItemMeta();
            if (meta instanceof ArmorMeta am && am.hasTrim() && am.getTrim() != null) {
                oldPattern = am.getTrim().getPattern();
            }
        }
        if (newItem != null && !newItem.getType().isAir()) {
            var meta = newItem.getItemMeta();
            if (meta instanceof ArmorMeta am && am.hasTrim() && am.getTrim() != null) {
                newPattern = am.getTrim().getPattern();
            }
        }

        boolean typeSame = java.util.Objects.equals(
            oldItem == null ? null : oldItem.getType(),
            newItem == null ? null : newItem.getType()
        );
        boolean trimSame = java.util.Objects.equals(oldPattern, newPattern);
        if (typeSame && trimSame) return;

        TrimManager.buildSlots(uuid);
        final PlayerArmorSlots slots = TrimManager.getSlots(uuid);
        final int afterCount = slots.instancesOfTrim(defaultPattern);

        boolean oldHas = defaultPattern.equals(oldPattern);
        boolean newHas = defaultPattern.equals(newPattern);
        final int derivedBefore = afterCount - (newHas ? 1 : 0) + (oldHas ? 1 : 0);
        final int beforeCount = instancesOfTrim.getOrDefault(uuid, derivedBefore);

        Location at = player.getLocation().add(0, 1, 0);
        if (afterCount >= 4 && beforeCount < 4) {
            at.getWorld().playSound(at, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
            FXUtilities.lv4Activation(player);
        } else if (beforeCount >= 4 && afterCount < 4) {
            at.getWorld().playSound(at, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        }

        instancesOfTrim.put(uuid, afterCount);
    }

    @Override
    public void run() {
        if (statusBarActivation == null || statusBarConsumer == null) return;
        if (instancesOfTrim.isEmpty()) return;

        for (UUID id : instancesOfTrim.keySet()) {
            boolean passed = statusBarActivation.apply(id);

            if (passed) {
                if (bossBars.containsKey(id)) continue;
                StatusBar bar = new StatusBar(id);
                bar.setConsumer(statusBarConsumer);
                bar.setHideTicks(hideTicks);

                bossBars.put(id, bar);
            }

            if (bossBars.containsKey(id) && !passed) {
                bossBars.get(id).hide();
            }
        }
    }
}
