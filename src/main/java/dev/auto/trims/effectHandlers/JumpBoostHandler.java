package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.managers.EffectManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JumpBoostHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.BOLT;
    private final Set<UUID> lv4Players = new HashSet<>();

    public JumpBoostHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount >= 4) {
            lv4Players.add(id);
        }
        else {
            lv4Players.remove(id);
        }

        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 2;
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.JUMP_BOOST, 3600, amplifier, false, false));
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lv4Players.remove(id);
    }

    public void onSwap(PlayerSwapHandItemsEvent event) {}
    public void onClickInIventory(InventoryClickEvent player) {}
    public void onDrag(InventoryDragEvent event) {}

    // TODO Rocket crossbow. Figure out angle of multishot and raycast then figure out if explosive or propulsion.
    // TODO add visual indicator for which mode is selected.
}
