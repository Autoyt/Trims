package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.managers.EffectManager;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.particles.FXUtilities;
import dev.auto.trims.particles.utils.CircleFX;
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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class HeroOfTheVillagerHandler extends OptimizedHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.SENTRY;

    public HeroOfTheVillagerHandler(Main instance) {
        super(defaultPattern);
        this.instance = instance;

        TrimManager.handlers.add(this);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 1;
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 3600, amplifier, false, false));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Player killer = livingEntity.getKiller();
        if (killer == null) return;

        UUID id = killer.getUniqueId();
        int instanceCount = getTrimCount(id);
        if (!(instanceCount > 0)) return;

        int looting = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);

        int base = ThreadLocalRandom.current().nextInt(0, 2 + looting);
        int extra = killer.isOnGround() ? base : Math.min(base * 2, 3);
        if (extra > 0) {
            event.getDrops().add(new ItemStack(Material.EMERALD, extra));

            Location loc = livingEntity.getLocation().clone().add(0, 0.5, 0);
                World world = loc.getWorld();
                world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1f);
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.3, 0.3, 0.3, 1);
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        super.onArmorChange(event);
    }
}

