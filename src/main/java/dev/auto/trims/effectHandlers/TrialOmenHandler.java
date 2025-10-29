package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.utils.ItemStackUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount >= 4) {
            lv4Players.add(id);
            ItemStackUtils.enforceMaceBan(player);
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
            ItemStackUtils.enforceMaceBan(attacker);
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
            ItemStackUtils.enforceMaceBan(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!lv4Players.contains(id)) return;

        ItemStack newMain = event.getOffHandItem();
        if (newMain != null && newMain.getType() == Material.MACE) {
            Component message = MiniMessage.miniMessage().deserialize("<red><bold>You can't use that while you're in the trial omen effect!");
            player.sendMessage(message);
            ItemStackUtils.enforceMaceBan(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!lv4Players.contains(p.getUniqueId())) return;

        if (e.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack hovered = e.getCurrentItem();
            ItemStack offhand = p.getInventory().getItemInOffHand();
            boolean involvesMace = (hovered != null && hovered.getType() == Material.MACE)
                    || (offhand != null && offhand.getType() == Material.MACE);
            if (involvesMace) {
                Component message = MiniMessage.miniMessage().deserialize("<red><bold>You can't use that while you're in the trial omen effect!");
                p.sendMessage(message);
                ItemStackUtils.enforceMaceBan(p);
                e.setCancelled(true);
                return;
            }
        }

        if (e.getClick() == ClickType.NUMBER_KEY) {
            int hb = e.getHotbarButton();
            int held = p.getInventory().getHeldItemSlot();
            if (hb == held) {
                ItemStack hovered = e.getCurrentItem();
                ItemStack hotbarItem = p.getInventory().getItem(hb);
                boolean involvesMace = (hovered != null && hovered.getType() == Material.MACE)
                        || (hotbarItem != null && hotbarItem.getType() == Material.MACE);
                if (involvesMace) {
                    Component message = MiniMessage.miniMessage().deserialize("<red><bold>You can't use that while you're in the trial omen effect!");
                    p.sendMessage(message);
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack current = e.getCurrentItem();
            if (current != null && current.getType() == Material.MACE) {
                Component message = MiniMessage.miniMessage().deserialize("<red><bold>You can't use that while you're in the trial omen effect!");
                p.sendMessage(message);
                e.setCancelled(true);
                return;
            }
        }

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        boolean movingMace = (cursor != null && cursor.getType() == Material.MACE)
                || (current != null && current.getType() == Material.MACE);
        if (!movingMace) return;

        if (e.getSlot() == 40) {
            ItemStackUtils.enforceMaceBan(p);
            e.setCancelled(true);
            return;
        }

        if (e.getClickedInventory() instanceof PlayerInventory) {
            int held = p.getInventory().getHeldItemSlot();
            if (e.getSlotType() == InventoryType.SlotType.QUICKBAR && e.getSlot() == held) {
                ItemStackUtils.enforceMaceBan(p);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
        int instanceCount = getTrimCount(event.getPlayer().getUniqueId(), defaultPattern);
        if (instanceCount >= 4) {
            ItemStackUtils.enforceMaceBan(event.getPlayer());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ItemStackUtils.enforceMaceBan(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        ItemStackUtils.enforceMaceBan(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!lv4Players.contains(p.getUniqueId())) return;
        if (event.getItem().getItemStack().getType() != Material.MACE) return;
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> ItemStackUtils.enforceMaceBan(p));
    }
}
