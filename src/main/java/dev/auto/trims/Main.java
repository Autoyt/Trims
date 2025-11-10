package dev.auto.trims;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.auto.trims.commands.DebugCommands;
import dev.auto.trims.commands.DisplayEntityDebugCommand;
import dev.auto.trims.crafting.CraftUtils;
import dev.auto.trims.crafting.RelayAppleListener;
import dev.auto.trims.crafting.RiftCraftListener;
import dev.auto.trims.crafting.TrimCraftListener;
import dev.auto.trims.effectHandlers.*;
import dev.auto.trims.listeners.GameListeners;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.world.WorldManager;
import dev.auto.trims.world.WorldObjective;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.popcraft.chunky.api.ChunkyAPI;

public final class Main extends JavaPlugin {
    @Getter
    private static Main instance;
    @Getter
    private static ChunkyAPI chunky;
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
        ConfigurationSerialization.registerClass(WorldObjective.class, "WorldObjective");

        // Config
        configSave(false);

        pl.registerEvents(new GameListeners(), this);
        pl.registerEvents(new NightVisionHandler(this), this);
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

        pl.registerEvents(new WorldManager(), this);

        InvisibiltyHandler invisibiltyHandler = new InvisibiltyHandler(this);
        pl.registerEvents(invisibiltyHandler, this);
        PacketEvents.getAPI().getEventManager().registerListener(invisibiltyHandler, PacketListenerPriority.LOW);

        getLogger().info("Trim listeners registered");

        registerCommand("debug", new DebugCommands(this));
        registerCommand("de", new DisplayEntityDebugCommand());

        TrimCraftListener craftListener = new TrimCraftListener(this);
        pl.registerEvents(craftListener, this);

        pl.registerEvents(new RiftCraftListener(), this);
        pl.registerEvents(new RelayAppleListener(), this);

        tickTask = TrimManager.start();
        for (Player p : getServer().getOnlinePlayers()) {
            getLogger().info("Building slots for " + p.getName());
            TrimManager.buildSlots(p.getUniqueId());
        }

        try {
            chunky = Bukkit.getServer().getServicesManager().load(ChunkyAPI.class);
        }
        catch (Exception e) {
            getLogger().warning("Exception thrown whilst loading chunky API");
        }
    }

    @Override
    public void onDisable() {
        instance = null;

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

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
