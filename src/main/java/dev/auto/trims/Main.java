package dev.auto.trims;

import com.github.retrooper.packetevents.PacketEvents;
import commands.DebugCommands;
import effectHandlers.NightVisionHandler;
import effectHandlers.SpeedHandler;
import effectHandlers.TrimManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import listeners.GameListeners;
import org.bukkit.entity.Player;
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
        getServer().getPluginManager().registerEvents(new NightVisionHandler(this), this);
        getServer().getPluginManager().registerEvents(new SpeedHandler(this), this);

        DebugCommands debug = new DebugCommands(this);
        Objects.requireNonNull(getCommand("debug")).setExecutor(debug);
        Objects.requireNonNull(getCommand("debug")).setTabCompleter(debug);

        // Start the effect ticker and build slots for all currently online players (covers /reload and late enables)
        TrimManager.start();
        for (Player p : getServer().getOnlinePlayers()) {
            TrimManager.buildSlots(p.getUniqueId());
        }
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
