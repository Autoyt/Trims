package dev.auto.trims.crafting;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

public class CraftUtils {
    /**
     * Removes registered crafting recipes.
     */
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

    /**
     * Gives a player a trimmed netherite set.
     * @apiNote Debug status
     * @param player
     * @param pattern
     * @param material
     */
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

    /**
     * @param structure target structure to get pretty name for
     * @return Pretty name of the structure, e.g. "Village" instead of "village_plains"
     */
    public static String getPrettyStructureName(Structure structure) {
        RegistryAccess access = RegistryAccess.registryAccess();
        Registry<@NotNull Structure> registry = access.getRegistry(RegistryKey.STRUCTURE);

        NamespacedKey key = registry.getKey(structure);
        if (key == null) {
            var typeKey = access.getRegistry(RegistryKey.STRUCTURE_TYPE)
                                .getKey(structure.getStructureType());
            if (typeKey == null) {
                return "Unknown Structure";
            }
            return toPrettyName(typeKey.getKey());
        }

        return toPrettyName(key.getKey());
    }

    private static String toPrettyName(String id) {
        return Arrays.stream(id.split("_"))
                .filter(s -> !s.isEmpty())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }
}
