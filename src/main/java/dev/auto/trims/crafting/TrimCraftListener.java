package dev.auto.trims.crafting;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Arrays;
import java.util.function.Predicate;

public class TrimCraftListener implements Listener {
    private final Main instance;
    private final NamespacedKey key = new NamespacedKey("trims", "trim_dupe");

    public TrimCraftListener(Main instance) {
        this.instance = instance;

        Material[] trimMats = Arrays.stream(Material.values())
                .filter(m -> m.name().endsWith("_ARMOR_TRIM_SMITHING_TEMPLATE"))
                .toArray(Material[]::new);

        if (trimMats.length == 0) {
            instance.getLogger().warning("[Trims] No trim templates found in Material enum; skipping trim dupe recipe registration.");
            return;
        }

        RecipeChoice.MaterialChoice trimChoice = new RecipeChoice.MaterialChoice(trimMats);

        ItemStack placeholder = new ItemStack(Material.PAPER);
        ShapedRecipe r = new ShapedRecipe(key, placeholder);
        r.shape("DTD", "TCT", "DTD");
        r.setIngredient('D', Material.DIAMOND);
        r.setIngredient('T', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        r.setIngredient('C', trimChoice);

        Bukkit.addRecipe(r);
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || !(event.getRecipe() instanceof ShapedRecipe s)) return;
        if (!s.getKey().equals(key)) return;

        CraftingInventory inv = event.getInventory();
        ItemStack[] m = inv.getMatrix();
        if (m == null || m.length < 9) { inv.setResult(null); return; }

        ItemStack center = m[4];

        Predicate<ItemStack> isTrimTemplate = it ->
        {
            if (it == null) return false;
            it.getType();
            return it.getType().name().endsWith("_ARMOR_TRIM_SMITHING_TEMPLATE");
        };
        Predicate<ItemStack> isNetheriteUpgrade = it ->
                it != null && it.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
        Predicate<ItemStack> isDiamond = it -> it != null && it.getType() == Material.DIAMOND;

        boolean ok =
                isDiamond.test(m[0]) && isDiamond.test(m[2]) && isDiamond.test(m[6]) && isDiamond.test(m[8]) &&
                isNetheriteUpgrade.test(m[1]) && isNetheriteUpgrade.test(m[3]) &&
                isNetheriteUpgrade.test(m[5]) && isNetheriteUpgrade.test(m[7]) &&
                isTrimTemplate.test(center);

        if (!ok) { inv.setResult(null); return; }

        ItemStack out = center.clone();
        out.setAmount(2);
        inv.setResult(out);
    }
}
