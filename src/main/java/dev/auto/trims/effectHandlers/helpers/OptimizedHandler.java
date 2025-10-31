package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
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

public class OptimizedHandler implements Listener {
    private final Map<UUID, Integer> instancesOfTrim = new HashMap<>();
    private final TrimPattern defaultPattern;

    public OptimizedHandler(TrimPattern pattern) {
        this.defaultPattern = pattern;
    }

    protected int getTrimCount(UUID uuid) {
        return instancesOfTrim.computeIfAbsent(uuid, i -> updateTrimCache(uuid));
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
}
