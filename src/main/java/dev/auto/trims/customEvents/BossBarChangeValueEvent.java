package dev.auto.trims.customEvents;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BossBarChangeValueEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID id;
    private final Float value;

    public BossBarChangeValueEvent(UUID id, Float value) {
        this.id = id;
        this.value = value;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(id);
    }

    public UUID getId() {
        return id;
    }
    public Float getValue() {
        return value;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
