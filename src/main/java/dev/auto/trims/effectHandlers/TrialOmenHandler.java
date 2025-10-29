package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrialOmenHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.RAISER;
    private final Set<UUID> lv4Players = new HashSet<>();

    public TrialOmenHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    public void onlinePlayerTick(Player player) {
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
            TrimManager.wantEffect(id, new PotionEffect(PotionEffectType.TRIAL_OMEN, 3600, amplifier, false, false));
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
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!lv4Players.contains(id)) return;

        ItemStack mainhand = player.getInventory().getItem(event.getNewSlot());
        if (mainhand == null) return;

        if (mainhand.getType() == Material.MACE) {
            Component message = MiniMessage.miniMessage().deserialize("<red><bold>You can't use that while you're in the trial omen effect!");
            player.sendMessage(message);

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!lv4Players.contains(id)) return;

        // If a mace would end up in the main hand after swap, cancel it
        ItemStack newMain = event.getOffHandItem();
        if (newMain != null && newMain.getType() == Material.MACE) {
            Component message = MiniMessage.miniMessage().deserialize("<red><bold>You can't use that while you're in the trial omen effect!");
            player.sendMessage(message);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }
}
