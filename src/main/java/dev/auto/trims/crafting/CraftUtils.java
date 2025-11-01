package dev.auto.trims.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Iterator;

public class CraftUtils {
    public static void removeCraftingRecipes() {
        // Trims recipes
        Iterator<Recipe> trimRecipe = Bukkit.recipeIterator();
        while (trimRecipe.hasNext()) {
            Recipe r = trimRecipe.next();
            if (r == null) continue;

            if (r instanceof Keyed keyed) {
                NamespacedKey key = keyed.getKey();
                String name = key.getKey();

                if (name.endsWith("smithing_template")) {
                    trimRecipe.remove();
                }
            }
        }
    }

    public static void giveTrimmedNetheriteSet(Player player, TrimPattern pattern, TrimMaterial material) {
        if (player == null || pattern == null || material == null) return;

        ItemStack helm = makeTrimmed(Material.NETHERITE_HELMET, pattern, material);
        ItemStack chest = makeTrimmed(Material.NETHERITE_CHESTPLATE, pattern, material);
        ItemStack legs = makeTrimmed(Material.NETHERITE_LEGGINGS, pattern, material);
        ItemStack boots = makeTrimmed(Material.NETHERITE_BOOTS, pattern, material);

        player.getInventory().addItem(helm, chest, legs, boots);
    }

    private static ItemStack makeTrimmed(Material mat, TrimPattern pattern, TrimMaterial material) {
        ItemStack item = new ItemStack(mat);
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 4);

        ArmorMeta meta = (ArmorMeta) item.getItemMeta();
        meta.setTrim(new ArmorTrim(material, pattern));
        item.setItemMeta(meta);

        return item;
    }
}
