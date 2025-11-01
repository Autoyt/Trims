package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.effectHandlers.helpers.StatusBar;
import dev.auto.trims.managers.TrimManager;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.UUID;

public class testHandler extends OptimizedHandler implements IBaseEffectHandler {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.DUNE;

    public testHandler(Main instance) {
        super(defaultPattern);

        setActivationFunction(uuid -> {
            int instanceCount = getTrimCount(uuid);
            return instanceCount > 2;
        });

        setBossBarConsumer((uuid, statusBar) -> {
            int instanceCount = getTrimCount(uuid);
            if (instanceCount < 0) return;

            statusBar.setTitle("New").setColor(BossBar.Color.GREEN);
        });

        setHideCooldown(20 * 2);


        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount > 0) {
            StatusBar bar = getStatusBar(id);
            if (bar == null) return;
            bar.incrementProgress(0.01f);
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        onArmorChange(event);
    }
}
