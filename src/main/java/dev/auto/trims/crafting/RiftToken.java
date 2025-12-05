package dev.auto.trims.crafting;

import dev.auto.trims.world.WorldManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class RiftToken {
    /**
     * Creates a generic Rift Token item used as the recipe result placeholder and for recipe discovery.
     * This item does not contain any structure-binding data; it only has name/lore and hidden attributes.
     */
    public static ItemStack getRiftTokenItem() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);

        MiniMessage mm = MiniMessage.miniMessage();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize("<bold><aqua>ʀɪꜰᴛ ᴛᴏᴋᴇɴ"));
        meta.lore(List.of(
                mm.deserialize("<grey>Craft a token linked"),
                mm.deserialize("<grey>to a mysterious structure")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Overloaded variant: creates a functional Rift Token bound to the provided structure.
     * This mirrors the behavior previously implemented in onPrepare: applies cooldown, glint override,
     * max stack size, and persists the structure id into PDC under the rift key.
     *
     * The recipe placeholder should continue using {@link #getRiftTokenItem()} to avoid exposing a specific structure.
     *
     * @param structure target structure to bind into the token (required)
     * @return functional Rift Token ItemStack with PDC set
     */
    public static ItemStack getRiftTokenItem(Structure structure) {
        if (structure == null) return getRiftTokenItem();

        String structureName = CraftUtils.getPrettyStructureName(structure);

        ItemStack result = new ItemStack(Material.ENDER_EYE);
        // Match behavior from onPrepare
        result.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(15).cooldownGroup(RiftCraftListener.riftKey).build());
        result.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        result.setData(DataComponentTypes.MAX_STACK_SIZE, 1);

        MiniMessage mm = MiniMessage.miniMessage();
        ItemMeta resultMeta = result.getItemMeta();
        resultMeta.displayName(mm.deserialize("<bold><aqua>ʀɪꜰᴛ ᴛᴏᴋᴇɴ<reset> - %st%".replace("%st%", structureName)));
        resultMeta.lore(List.of(
                mm.deserialize("<grey>Place this item to open a rift"),
                mm.deserialize("<grey>for you and your party")
        ));
        resultMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Bind the structure id in PDC using the same key
        PersistentDataContainer data = resultMeta.getPersistentDataContainer();
        data.set(RiftCraftListener.riftKey, PersistentDataType.INTEGER, WorldManager.getIdFromStructure(structure));

        result.setItemMeta(resultMeta);
        return result;
    }
}
