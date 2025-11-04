package dev.auto.trims.customEvents;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BorderLandsOnLoadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final UUID worldID;

    public BorderLandsOnLoadEvent(UUID worldID) {
        this.worldID = worldID;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
