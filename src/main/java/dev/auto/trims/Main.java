package dev.auto.trims;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.auto.trims.commands.DebugCommands;
import dev.auto.trims.crafting.CraftEventListener;
import dev.auto.trims.crafting.CraftUtils;
import dev.auto.trims.effectHandlers.*;
import dev.auto.trims.listeners.GameListeners;
import dev.auto.trims.managers.TrimManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class Main extends JavaPlugin {
    @Getter
    private static Main instance;
    BukkitTask tickTask;

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

        // Config
        configSave(false);

        pl.registerEvents(new GameListeners(), this);
        pl.registerEvents(new NightVisionHandler(this), this);
//        pl.registerEvents(new testHandler(this),this);
        pl.registerEvents(new SpeedHandler(this), this);
        pl.registerEvents(new FireResistanceHandler(this), this);
        pl.registerEvents(new LuckHandler(this), this);
        pl.registerEvents(new LevitationHandler(this), this);
        pl.registerEvents(new DolphinsGraceHandler(this), this);
        pl.registerEvents(new TrialOmenHandler(this), this);
        pl.registerEvents(new HeroOfTheVillagerHandler(this), this);
        pl.registerEvents(new SuicideVestHandler(this), this);
        pl.registerEvents(new HasteHandler(this), this);
        pl.registerEvents(new AbsorptionListener(this), this);
        pl.registerEvents(new JumpBoostHandler(this), this);
        pl.registerEvents(new SaturationHandler(this), this);
        pl.registerEvents(new TeleportHandler(this), this);
        pl.registerEvents(new ConduitPowerHandler(this), this);
        pl.registerEvents(new StrengthHandler(this), this);
        pl.registerEvents(new ResistanceHandler(this), this);

        InvisibiltyHandler invisibiltyHandler = new InvisibiltyHandler(this);
        pl.registerEvents(invisibiltyHandler, this);
        PacketEvents.getAPI().getEventManager().registerListener(invisibiltyHandler, PacketListenerPriority.LOW);

        getLogger().info("Trim listeners registered");

        DebugCommands debug = new DebugCommands(this);
        Objects.requireNonNull(getCommand("debug")).setExecutor(debug);
        Objects.requireNonNull(getCommand("debug")).setTabCompleter(debug);

        CraftEventListener craftListener = new CraftEventListener(this);
        pl.registerEvents(craftListener, this);

        tickTask = TrimManager.start();
        for (Player p : getServer().getOnlinePlayers()) {
            getLogger().info("Building slots for " + p.getName());
            TrimManager.buildSlots(p.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        instance = null;
        tickTask.cancel();
        PacketEvents.getAPI().terminate();
    }

    public void configSave(boolean overwriteFromJar) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        saveResource("config.yml", overwriteFromJar);
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }
}
