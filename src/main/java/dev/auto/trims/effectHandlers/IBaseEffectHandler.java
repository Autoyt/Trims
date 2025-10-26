package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import dev.auto.trims.particles.FXUtilities;

import java.util.UUID;

public interface IBaseEffectHandler {
    void onTick();

    default void handleEquip(PlayerArmorChangeEvent event, TrimPattern defaultPattern) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final ItemStack oldItem = event.getOldItem();
        final ItemStack newItem = event.getNewItem();

        // Rebuild from the player's inventory ONCE to get the AFTER state of the change
        TrimManager.buildSlots(uuid);
        final PlayerArmorSlots slots = TrimManager.getSlots(uuid);
        final int afterCount = slots.instancesOfTrim(defaultPattern);

        // Derive whether the old/new items had this specific trim pattern
        boolean oldHas = false;
        if (oldItem != null && !oldItem.getType().isAir()) {
            if (oldItem.getItemMeta() instanceof ArmorMeta om && om.hasTrim()) {
                ArmorTrim t = om.getTrim();
                oldHas = t != null && defaultPattern.equals(t.getPattern());
            }
        }
        boolean newHas = false;
        if (newItem != null && !newItem.getType().isAir()) {
            if (newItem.getItemMeta() instanceof ArmorMeta nm && nm.hasTrim()) {
                ArmorTrim t = nm.getTrim();
                newHas = t != null && defaultPattern.equals(t.getPattern());
            }
        }

        // Compute the BEFORE count using the event delta: before = after - (new?1:0) + (old?1:0)
        final int beforeCount = afterCount - (newHas ? 1 : 0) + (oldHas ? 1 : 0);

        // Decide sounds/FX based on crossing the threshold
        Location soundEmitter = player.getLocation().add(0, 1, 0);
        if (afterCount >= 4 && beforeCount < 4) {
            soundEmitter.getWorld().playSound(soundEmitter, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);
            soundEmitter.getWorld().playSound(soundEmitter, Sound.ITEM_GOAT_HORN_SOUND_4, 1f, 1f);
            FXUtilities.lv4Activation(player);
        }

        if (afterCount < beforeCount) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        }

        if (beforeCount >= 4 && afterCount < 4) {
            soundEmitter.getWorld().playSound(soundEmitter, Sound.ENTITY_WITHER_DEATH, 1f, 1f);
        }

        // Generic feedback sound on any equip change
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
    }
}
