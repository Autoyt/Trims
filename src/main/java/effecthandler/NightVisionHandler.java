package effecthandler;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.UUID;

public class NightVisionHandler implements Listener, IBaseEffectHandler{
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.DUNE;

    public NightVisionHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @Override
    public void onTick() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            int instanceCount = TrimManager.getSlots(player.getUniqueId()).instancesOfTrim(this.defaultPattern);
            player.sendMessage(Component.text("Instance count: " + instanceCount));
            if (instanceCount == 0) continue;

            if (instanceCount >= 4) {
                handleLV4(player);
            }

            PotionEffect current = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (current == null || current.getDuration() <= 60) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0, false, false));
            }
        }
    }

    @EventHandler
    @Override
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final EquipmentSlot slot = event.getSlot();

        ItemStack item = event.getNewItem();

        PlayerArmorSlots slots = TrimManager.getSlots(uuid);
        switch (slot) {
            case HEAD  -> slots.setHelmet(null);
            case CHEST -> slots.setChestplate(null);
            case LEGS  -> slots.setLeggings(null);
            case FEET  -> slots.setBoots(null);
            default -> { return; }
        }

        if (item == null || item.getType() == Material.AIR) return;
        if (!(item.getItemMeta() instanceof ArmorMeta meta)) return;
        if (!meta.hasTrim()) return;

        ArmorTrim trim = meta.getTrim();

        assert trim != null;
        TrimPattern pattern = trim.getPattern();
        if (pattern != this.defaultPattern) return;

        int instanceCount = slots.instancesOfTrim(this.defaultPattern);

        if (instanceCount >= 1) {
            Component msg = MiniMessage.miniMessage().deserialize("<red>[<yellow>Warning</yellow><red>] You have already reached <bold>Max</bold> effectiveness of this effect! Adding this trim will have no positive effect. <red>");
            player.sendMessage(msg);
        }

        switch (slot) {
            case HEAD -> slots.setHelmet(pattern);
            case CHEST -> slots.setChestplate(pattern);
            case LEGS -> slots.setLeggings(pattern);
            case FEET -> slots.setBoots(pattern);
            default -> {
                return;
            }
        }
    }

    private void handleLV4(Player player) {
        instance.getLogger().info("LV4 detected!");
    }
}
