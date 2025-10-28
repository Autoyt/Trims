package dev.auto.trims.effectHandlers.heavyEvents;

import org.bukkit.event.player.PlayerMoveEvent;

public interface MovementListener {
    void onMovement(PlayerMoveEvent event);
}
