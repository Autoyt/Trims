package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.managers.EffectManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrialOmenHandler extends OptimizedHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.RAISER;
    private final Set<UUID> lv4Players = new HashSet<>();

    public TrialOmenHandler(Main instance) {
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
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.TRIAL_OMEN, 3600, amplifier, false, false));
        }
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(entity instanceof Player victim)) return;

        if (!lv4Players.contains(victim.getUniqueId())) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.MACE) {
            Location loc = victim.getLocation();
            loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f);

            PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false);
            attacker.addPotionEffect(blindness);
            attacker.damage(6, victim);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lv4Players.remove(id);
    }


    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        super.onArmorChange(event);
    }
}
