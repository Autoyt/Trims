package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.particles.FXUtilities;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.UUID;

public interface IBaseEffectHandler {
    /** @deprecated Use {@link #Tick()} instead then override {@link #onlinePlayerTick(Player)} */
    @Deprecated(since = "Beta 1")
    default void onTick() {}

    default void Tick() {
        for (Player player : Main.getInstance().getServer().getOnlinePlayers()) {
            onlinePlayerTick(player);
        }
    }

    default void onlinePlayerTick(Player player) {}

    /** @apiNote Helpers **/
    default int getTrimCount(UUID uuid, TrimPattern pattern) {
        PlayerArmorSlots slots = TrimManager.getSlots(uuid);
        return slots.instancesOfTrim(pattern);
    }
    default void handleEquip(PlayerArmorChangeEvent event, TrimPattern defaultPattern) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final ItemStack oldItem = event.getOldItem();
        final ItemStack newItem = event.getNewItem();

        TrimPattern oldPattern = null, newPattern = null;
        if (oldItem != null && !oldItem.getType().isAir() && oldItem.getItemMeta() instanceof ArmorMeta om && om.hasTrim()) {
            ArmorTrim t = om.getTrim();
            if (t != null) oldPattern = t.getPattern();
        }
        if (newItem != null && !newItem.getType().isAir() && newItem.getItemMeta() instanceof ArmorMeta nm && nm.hasTrim()) {
            ArmorTrim t = nm.getTrim();
            if (t != null) newPattern = t.getPattern();
        }

        boolean typeSame = (oldItem == null ? null : oldItem.getType()) == (newItem == null ? null : newItem.getType());
        boolean trimSame = java.util.Objects.equals(oldPattern, newPattern);
        if (typeSame && trimSame) return;

        TrimManager.buildSlots(uuid);
        final PlayerArmorSlots slots = TrimManager.getSlots(uuid);
        final int afterCount = slots.instancesOfTrim(defaultPattern);

        boolean oldHas = defaultPattern.equals(oldPattern);
        boolean newHas = defaultPattern.equals(newPattern);
        final int beforeCount = afterCount - (newHas ? 1 : 0) + (oldHas ? 1 : 0);

        Location soundEmitter = player.getLocation().add(0, 1, 0);
        if (afterCount >= 4 && beforeCount < 4) {
            soundEmitter.getWorld().playSound(soundEmitter, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
            FXUtilities.lv4Activation(player);
        }
        if (beforeCount >= 4 && afterCount < 4) {
            soundEmitter.getWorld().playSound(soundEmitter, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        }
    }
}
