package dev.auto.trims.crafting;

import dev.auto.trims.world.WorldManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public class RiftCraftListener implements Listener {
    public static final NamespacedKey riftKey = new NamespacedKey("trims", "rift_dupe");
    public RiftCraftListener() {

        Material[] mats = structures.keySet().toArray(new Material[0]);
        var choices = new RecipeChoice.MaterialChoice(mats);

        ItemStack placeholder = new ItemStack(Material.ENDER_EYE);

        var mm = MiniMessage.miniMessage();
        ItemMeta phMeta = placeholder.getItemMeta();
        phMeta.displayName(mm.deserialize("<bold><aqua>ʀɪꜰᴛ ᴛᴏᴋᴇɴ"));
        phMeta.lore(List.of(
                mm.deserialize("<grey>Craft a token linked"),
                mm.deserialize("<grey>to a mysterious structure")
        ));
        phMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        placeholder.setItemMeta(phMeta);

        ShapedRecipe riftRecipe = new ShapedRecipe(riftKey, placeholder);
        riftRecipe.shape("CPC", "PIP", "CLC");
        riftRecipe.setIngredient('C', Material.AMETHYST_SHARD);
        riftRecipe.setIngredient('P', Material.ENDER_PEARL);
        riftRecipe.setIngredient('L', Material.LODESTONE);
        riftRecipe.setIngredient('I', choices);

        Bukkit.addRecipe(riftRecipe);
    }
    private static final Map<Material, Structure> structures = Map.of(
            Material.ECHO_SHARD, Structure.ANCIENT_CITY,
            Material.GOLD_BLOCK, Structure.DESERT_PYRAMID,
            Material.OXIDIZED_COPPER, Structure.TRIAL_CHAMBERS,
            Material.IRON_BLOCK, Structure.PILLAGER_OUTPOST,
            Material.MAP, Structure.SHIPWRECK,
            Material.TOTEM_OF_UNDYING, Structure.MANSION,
            Material.HEART_OF_THE_SEA, Structure.MONUMENT,
            Material.ENDER_EYE, Structure.STRONGHOLD,
            Material.COMPASS, Structure.JUNGLE_PYRAMID,
            Material.BRUSH, Structure.TRAIL_RUINS
    );

    public static Structure getStructureFromMaterial(Material material) {
        return structures.get(material);
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || !(event.getRecipe() instanceof ShapedRecipe s)) return;
        if (!s.getKey().equals(riftKey)) return;

        CraftingInventory inv = event.getInventory();
        ItemStack[] m = inv.getMatrix();
        if (m == null || m.length < 9) { inv.setResult(null); return; }

        Material structureMat = null;
        for (ItemStack stack : m) {
            if (stack == null || stack.getType().isAir()) continue;
            if (structures.containsKey(stack.getType())) {
                structureMat = stack.getType();
                break;
            }
        }

        if (structureMat == null) {
            inv.setResult(null);
            return;
        }

        Structure structure = structures.get(structureMat);
        if (structure == null) {
            inv.setResult(null);
            return;
        }

        String structureName = CraftUtils.getPrettyStructureName(structure);

        ItemStack result = new ItemStack(Material.ENDER_EYE);
        result.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(15).cooldownGroup(riftKey).build());
        result.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        result.setData(DataComponentTypes.MAX_STACK_SIZE, 1);

        ItemMeta resultMeta = result.getItemMeta();
        resultMeta.displayName(MiniMessage.miniMessage()
                .deserialize("<bold><aqua>ʀɪꜰᴛ ᴛᴏᴋᴇɴ<reset> - %st%"
                        .replace("%st%", structureName)));

        resultMeta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<grey>Place this item to open a rift"),
                MiniMessage.miniMessage().deserialize("<grey>for you and your party")
        ));

        resultMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer data = resultMeta.getPersistentDataContainer();
        data.set(riftKey, PersistentDataType.INTEGER, WorldManager.getIdFromStructure(structure));

        result.setItemMeta(resultMeta);
        inv.setResult(result);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(riftKey);
    }
}