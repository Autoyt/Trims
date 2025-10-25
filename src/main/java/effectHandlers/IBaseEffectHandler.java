package effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.UUID;

public interface IBaseEffectHandler {
    void onTick();

    default void handleEquip(PlayerArmorChangeEvent event, TrimPattern defaultPattern) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final EquipmentSlot slot = event.getSlot();
        ItemStack item = event.getNewItem();

        // Determine what the pattern should be for this slot
        TrimPattern patternToSet = null;

        if (item != null && !item.getType().isAir()) {
            if (item.getItemMeta() instanceof ArmorMeta meta && meta.hasTrim()) {
                ArmorTrim trim = meta.getTrim();
                if (trim != null && trim.getPattern().equals(defaultPattern)) {
                    patternToSet = trim.getPattern();
                }
            }
        }

        // Update the slot with the determined value
        PlayerArmorSlots slots = TrimManager.getSlots(uuid);
        switch (slot) {
            case HEAD -> slots.setHelmet(patternToSet);
            case CHEST -> slots.setChestplate(patternToSet);
            case LEGS -> slots.setLeggings(patternToSet);
            case FEET -> slots.setBoots(patternToSet);
        }
    }
}
