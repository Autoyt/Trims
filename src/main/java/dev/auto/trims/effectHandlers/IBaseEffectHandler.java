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
        final EquipmentSlot slot = event.getSlot();
        final ItemStack newItem = event.getNewItem();
        final int intialInstanceCount = TrimManager.getSlots(uuid).instancesOfTrim(defaultPattern);


        // Determine what the pattern should be for this slot (single compute)
        TrimPattern patternToSet = null;
        if (newItem != null && !newItem.getType().isAir()) {
            if (newItem.getItemMeta() instanceof ArmorMeta meta && meta.hasTrim()) {
                ArmorTrim trim = meta.getTrim();
                // Use equals on values, not reference
                if (trim != null && defaultPattern.equals(trim.getPattern())) {
                    patternToSet = trim.getPattern();
                }
            }
        }

        // Update the slot with the determined value (single write)
        PlayerArmorSlots slots = TrimManager.getSlots(uuid);
        switch (slot) {
            case HEAD -> slots.setHelmet(patternToSet);
            case CHEST -> slots.setChestplate(patternToSet);
            case LEGS -> slots.setLeggings(patternToSet);
            case FEET -> slots.setBoots(patternToSet);
        }

        // Optional refresh to rebuild from inventory once after setting
        TrimManager.buildSlots(uuid);
        int currentInstanceCount = slots.instancesOfTrim(defaultPattern);

        Location soundEmitter = player.getLocation().add(0, 1, 0);
        if (currentInstanceCount >= 4 && currentInstanceCount > intialInstanceCount) {
            soundEmitter.getWorld().playSound(soundEmitter, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);
            soundEmitter.getWorld().playSound(soundEmitter, Sound.ITEM_GOAT_HORN_SOUND_4, 1f, 1f);
            FXUtilities.lv4Activation(player);
        }

        if (currentInstanceCount < intialInstanceCount) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        }

        if (intialInstanceCount >= 4 && currentInstanceCount < intialInstanceCount) {
            soundEmitter.getWorld().playSound(soundEmitter, Sound.ENTITY_WITHER_DEATH, 1f, 1f);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
    }
}
