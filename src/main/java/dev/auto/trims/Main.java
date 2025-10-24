package dev.auto.trims;

import com.github.retrooper.packetevents.PacketEvents;
import commands.DebugCommands;
import effecthandler.NightVisionHandler;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import listeners.GameListeners;
import listeners.GhostStepListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Main extends JavaPlugin {
    private static Main instance;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("[TRIMS] is enabled!");

        getServer().getPluginManager().registerEvents(new GameListeners(), this);
        getServer().getPluginManager().registerEvents(new NightVisionHandler(instance), this);

        GhostStepListener ghost = new GhostStepListener();
        getServer().getPluginManager().registerEvents(ghost, this);

        DebugCommands debug = new DebugCommands(this, ghost);
        Objects.requireNonNull(getCommand("debug")).setExecutor(debug);
        Objects.requireNonNull(getCommand("debug")).setTabCompleter(debug);
    }

    @Override
    public void onDisable() {
        instance = null;
        PacketEvents.getAPI().terminate();
    }

    public static Main getInstance() {
        return instance;
    }
}
