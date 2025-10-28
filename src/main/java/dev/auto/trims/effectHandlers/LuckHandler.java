package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
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

public class LuckHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.WAYFINDER;
    private final Set<UUID> lv4Players = new HashSet<>();
    public AtomicInteger counter = new AtomicInteger(0);

    public LuckHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);

        Bukkit.getScheduler().runTaskTimer(instance, new FXSweeper(this), 1, 5);
    }

    @Override
    public void OnlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount >= 4) {
            lv4Players.add(id);
        }
        else {
            lv4Players.remove(id);
        }

        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 1;
            TrimManager.wantEffect(id, new PotionEffect(PotionEffectType.LUCK, 2400, amplifier, false, false));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Player killer = livingEntity.getKiller();
        if (killer == null) return;

        UUID id = killer.getUniqueId();
        if (!lv4Players.contains(id)) return;

        int looting = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);

        int base = ThreadLocalRandom.current().nextInt(0, 2 + looting);
        int extra = killer.isOnGround() ? base : Math.min(base * 2, 3);
        if (extra > 0) {
            event.getDrops().add(new ItemStack(Material.EMERALD, extra));

            if (counter.incrementAndGet() <= 20) {
                Location loc = livingEntity.getLocation().clone().add(0, 0.5, 0);
                World world = loc.getWorld();
                world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1f);
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.3, 0.3, 0.3, 1);
            }
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lv4Players.remove(id);
    }
}

class FXSweeper implements Runnable {
    private final LuckHandler instance;

    FXSweeper(LuckHandler instance) {
        this.instance = instance;
    }

    @Override
    public void run() {
        instance.counter.set(0);
    }
}
