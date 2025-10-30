package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeleportHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.BOLT;
    private final Set<UUID> sneakingPlayers = new HashSet<>();

    public TeleportHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);
        // TODO allow bow to teleport players.
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {}

}
