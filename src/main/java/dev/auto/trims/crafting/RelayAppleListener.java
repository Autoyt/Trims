package dev.auto.trims.crafting;

import dev.auto.trims.world.WorldManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class RelayAppleListener implements Listener {
    public static final NamespacedKey relayKey = new NamespacedKey("trims", "relay_dupe");
    public RelayAppleListener() {
        ItemStack placeholder = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        var mm = MiniMessage.miniMessage();

        ItemMeta phMeta = placeholder.getItemMeta();
        phMeta.displayName(mm.deserialize("<bold><gold>ʀᴇʟᴀʏ ᴀᴘᴘʟᴇ"));
        phMeta.lore(List.of(
                mm.deserialize("<grey>Craft a relay apple to"),
                mm.deserialize("<grey>extract from the world early")
        ));
        phMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        placeholder.setItemMeta(phMeta);

        ShapelessRecipe recipe = new ShapelessRecipe(relayKey, placeholder);
        recipe.addIngredient(Material.ENCHANTED_GOLDEN_APPLE);
        recipe.addIngredient(Material.ENDER_EYE);

        Bukkit.addRecipe(recipe);
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || !(event.getRecipe() instanceof ShapelessRecipe s)) return;
        if (!s.getKey().equals(relayKey)) return;

        var mm = MiniMessage.miniMessage();

        CraftingInventory inv = event.getInventory();
        ItemStack result = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);

        ItemMeta resultMeta = result.getItemMeta();
        resultMeta.displayName(mm.deserialize("<bold><light_purple>ʀᴇʟᴀʏ ᴀᴘᴘʟᴇ"));

        resultMeta.lore(List.of(
                mm.deserialize("<grey>Use this item to extract"),
                mm.deserialize("<grey>early from the world")
        ));

        Consumable consumable = Consumable.consumable()
                .consumeSeconds(10)
                .animation(ItemUseAnimation.BRUSH)
                .addEffect(ConsumeEffect.applyStatusEffects(
                        List.of(
                        new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 0)
                        ), 1)
                )
                .build();

        result.setData(DataComponentTypes.CONSUMABLE, consumable);
        result.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(60).cooldownGroup(relayKey).build());
        result.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        result.setData(DataComponentTypes.MAX_STACK_SIZE, 16);

        PersistentDataContainer data = resultMeta.getPersistentDataContainer();
        data.set(relayKey, PersistentDataType.BOOLEAN, true);

        resultMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        result.setItemMeta(resultMeta);
        inv.setResult(result);
    }
}
