package dev.auto.trims;

import com.github.retrooper.packetevents.PacketEvents;
import dev.auto.trims.commands.DebugCommands;
import dev.auto.trims.effectHandlers.FireResistanceHandler;
import dev.auto.trims.effectHandlers.NightVisionHandler;
import dev.auto.trims.effectHandlers.SpeedHandler;
import dev.auto.trims.effectHandlers.TrimManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import dev.auto.trims.listeners.GameListeners;
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
        PacketEvents.getAPI().init();

        getLogger().info("enabled!");

        getServer().getPluginManager().registerEvents(new GameListeners(), this);
        getServer().getPluginManager().registerEvents(new NightVisionHandler(this), this);
        getServer().getPluginManager().registerEvents(new SpeedHandler(this), this);
        getServer().getPluginManager().registerEvents(new FireResistanceHandler(this), this);

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
