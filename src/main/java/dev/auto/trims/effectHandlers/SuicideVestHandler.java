package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.trim.TrimPattern;

public class SuicideVestHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.SHAPER;

    public SuicideVestHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }
}
