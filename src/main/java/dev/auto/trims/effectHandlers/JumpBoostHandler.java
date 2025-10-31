package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.managers.EffectManager;
import dev.auto.trims.managers.TrimManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class JumpBoostHandler extends OptimizedHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.BOLT;

    public JumpBoostHandler(Main instance) {
        super(defaultPattern);
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 2;
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.JUMP_BOOST, 3600, amplifier, false, false));
        }
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        super.onArmorChange(e);
    }

    @EventHandler
    public void onStunSmash(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);
        if (instanceCount <= 0) return;

        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;

        // TODO finsish this tomorrow
    }
}
