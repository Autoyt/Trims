package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class FireResistanceHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.RIB;

    public FireResistanceHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @Override
    public void onTick() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            PlayerArmorSlots slots = TrimManager.getSlots(id);
            int instanceCount = slots.instancesOfTrim(this.defaultPattern);

            if (instanceCount > 0) {
                PotionEffect current = player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE);
                if (current == null || current.getDuration() <= 60) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));
                }
            } else {
                // Remove FIRE_RESISTANCE when no RIB-trim pieces are worn
                player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            }

        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }
}
