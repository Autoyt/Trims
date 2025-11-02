package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.managers.EffectManager;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class LuckHandler extends OptimizedHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.WAYFINDER;
    private final Set<UUID> lv4Players = new HashSet<>();

    public LuckHandler(Main instance) {
        super(defaultPattern);
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount >= 4) {
            lv4Players.add(id);
        }
        else {
            lv4Players.remove(id);
        }

        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 1;
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.LUCK, 3600, amplifier, false, false));
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        super.onArmorChange(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lv4Players.remove(id);
    }
}
