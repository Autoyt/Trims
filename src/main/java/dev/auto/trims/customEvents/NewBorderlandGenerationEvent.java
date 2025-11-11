package dev.auto.trims.customEvents;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NewBorderlandGenerationEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID worldID;
    private final long durationMillis;

    public NewBorderlandGenerationEvent(UUID worldID, long durationMillis) {
        this.worldID = worldID;
        this.durationMillis = durationMillis;
    }
    public UUID getWorldID() {
        return worldID;
    }

    public String getWorldName() {
        return worldID.toString();
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
