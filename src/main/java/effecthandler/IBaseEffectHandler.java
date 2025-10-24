package effecthandler;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;

public interface IBaseEffectHandler {
    void onTick();
    void onArmorEquip(PlayerArmorChangeEvent event);
}
