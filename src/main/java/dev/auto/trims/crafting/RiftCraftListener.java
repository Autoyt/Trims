package dev.auto.trims.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Map;

public class RiftCraftListener implements Listener {
    public static final NamespacedKey riftKey = new NamespacedKey("trims", "rift_dupe");
    public static final NamespacedKey riftPlayerCooldownKey = new NamespacedKey("trims", "rift_player_cooldown");
    public RiftCraftListener() {

        Material[] mats = structures.keySet().toArray(new Material[0]);
        var choices = new RecipeChoice.MaterialChoice(mats);

        ItemStack placeholder = RiftToken.getRiftTokenItem();

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

        ItemStack result = RiftToken.getRiftTokenItem(structure);
        inv.setResult(result);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(riftKey);
    }
}