package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
            int instanceCount = getTrimCount(id, defaultPattern);

            if (instanceCount > 0) {
                // Request via coordinator; it will add/refresh and handle removals when not desired
                TrimManager.wantEffect(id, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3600, 0, false, false));
            }

        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }
}
