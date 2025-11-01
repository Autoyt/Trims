package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.utils.ItemStackUtils;
import dev.auto.trims.managers.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class InvisibiltyHandler extends OptimizedHandler implements IBaseEffectHandler, Listener, PacketListener {
    private final Main instance;
    private final static TrimPattern defaultPattern = TrimPattern.WILD;
    private final Set<UUID> hiddenTargets = new HashSet<>();
    private final Set<UUID> lv4Players = new HashSet<>();
    private static final ItemStack air = ItemStack.builder().type(ItemTypes.AIR).amount(1).build();

    public InvisibiltyHandler(Main instance) {
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
                EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.INVISIBILITY, 3600, 0, false, false));
            }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        super.onArmorChange(event);

        UUID id = event.getPlayer().getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount >= 4) {
            hidePlayer(id);
            lv4Players.add(id);
        }
        else {
            showPlayer(id);
            lv4Players.remove(id);
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (!lv4Players.contains(id)) {
            return;
        }

        org.bukkit.inventory.ItemStack mainhand = player.getInventory().getItem(event.getNewSlot());
        org.bukkit.inventory.ItemStack offhand = player.getInventory().getItemInOffHand();

        // Show the player if they are holding any weapon in either hand; otherwise keep them hidden
        if (ItemStackUtils.isWeapon(offhand) || ItemStackUtils.isWeapon(mainhand)) {
            showPlayer(id);
        }
        else {
            hidePlayer(id);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        hiddenTargets.remove(id);
        lv4Players.remove(id);
    }

    public void hidePlayer(UUID id) {
        hiddenTargets.add(id);

        Player target = Bukkit.getPlayer(id);
        if (target == null) return;

        List<Equipment> eq = Arrays.asList(
                new Equipment(EquipmentSlot.HELMET, air),
                new Equipment(EquipmentSlot.CHEST_PLATE, air),
                new Equipment(EquipmentSlot.LEGGINGS, air),
                new Equipment(EquipmentSlot.BOOTS, air),
                new Equipment(EquipmentSlot.OFF_HAND, air),
                new Equipment(EquipmentSlot.MAIN_HAND, air)
        );
        WrapperPlayServerEntityEquipment pkt = new WrapperPlayServerEntityEquipment(target.getEntityId(), eq);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer == target) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, pkt);
        }
    }

    private void showPlayer(UUID id) {
        hiddenTargets.remove(id);

        Player target = Bukkit.getPlayer(id);
        if (target == null) return;

        for (Player viewer : Bukkit.getOnlinePlayers()){
            if (viewer == target) continue; // don't send to the target themselves
            viewer.sendEquipmentChange(target, Map.of(
                org.bukkit.inventory.EquipmentSlot.HEAD, target.getEquipment().getHelmet(),
                org.bukkit.inventory.EquipmentSlot.CHEST, target.getEquipment().getChestplate(),
                org.bukkit.inventory.EquipmentSlot.LEGS, target.getEquipment().getLeggings(),
                org.bukkit.inventory.EquipmentSlot.FEET, target.getEquipment().getBoots(),
                org.bukkit.inventory.EquipmentSlot.HAND, target.getEquipment().getItemInMainHand(),
                org.bukkit.inventory.EquipmentSlot.OFF_HAND, target.getEquipment().getItemInOffHand()
            ));
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) return;

        Player viewer = (Player) event.getPlayer();
        WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);
        int entityId = wrapper.getEntityId();


        Player target = null;
        for (Player p : viewer.getWorld().getPlayers()) {
            if (p.getEntityId() == entityId) { target = p; break; }
        }
        if (target == null || !hiddenTargets.contains(target.getUniqueId())) return;
        if (target == viewer) return;

        List<Equipment> list = new ArrayList<>(wrapper.getEquipment());

        for (int i = 0; i < list.size(); i++) {
            EquipmentSlot s = list.get(i).getSlot();
            if (s == EquipmentSlot.MAIN_HAND ||s == EquipmentSlot.OFF_HAND || s == EquipmentSlot.HELMET || s == EquipmentSlot.CHEST_PLATE || s == EquipmentSlot.LEGGINGS || s == EquipmentSlot.BOOTS) {
                list.set(i, new Equipment(s, air));
            }
        }
        wrapper.setEquipment(list);
    }
}
