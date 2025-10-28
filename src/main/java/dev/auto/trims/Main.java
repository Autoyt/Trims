package dev.auto.trims;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.auto.trims.commands.DebugCommands;
import dev.auto.trims.crafting.CraftEventListener;
import dev.auto.trims.crafting.CraftUtils;
import dev.auto.trims.effectHandlers.*;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import dev.auto.trims.listeners.GameListeners;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Main extends JavaPlugin {
    @Getter
    private static Main instance;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        final PluginManager pl = getServer().getPluginManager();
        instance = this;
        PacketEvents.getAPI().init();

        // Util calls
        CraftUtils.removeCraftingRecipes();

        pl.registerEvents(new GameListeners(), this);
        pl.registerEvents(new NightVisionHandler(this), this);
        pl.registerEvents(new SpeedHandler(this), this);
        pl.registerEvents(new FireResistanceHandler(this), this);
        pl.registerEvents(new LuckHandler(this), this);
        pl.registerEvents(new LevitationHandler(this), this);
        pl.registerEvents(new DolphinsGraceHandler(this), this);
        pl.registerEvents(new TrialOmenHandler(this), this);

        ConduitPowerHandler conduitHandler = new ConduitPowerHandler(this);
        pl.registerEvents(conduitHandler, this);
        Bukkit.getScheduler().runTaskTimer(this, conduitHandler, 1, 1);

        InvisibiltyHandler invisHandler = new InvisibiltyHandler(this);
        pl.registerEvents(invisHandler, this);
        PacketEvents.getAPI().getEventManager().registerListener(invisHandler, PacketListenerPriority.LOW);

        getLogger().info("Trim listeners registered");

        DebugCommands debug = new DebugCommands(this);
        Objects.requireNonNull(getCommand("debug")).setExecutor(debug);
        Objects.requireNonNull(getCommand("debug")).setTabCompleter(debug);

        CraftEventListener craftListener = new CraftEventListener(this);
        pl.registerEvents(craftListener, this);

        TrimManager.start();
        for (Player p : getServer().getOnlinePlayers()) {
            getLogger().info("Building slots for " + p.getName());
            TrimManager.buildSlots(p.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        instance = null;
        PacketEvents.getAPI().terminate();
    }
}
