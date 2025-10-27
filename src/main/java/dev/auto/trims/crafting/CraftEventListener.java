package dev.auto.trims.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;

public class CraftEventListener implements Listener {
    public static void removeCraftingRecipes() {
        // Trims recipes
        Iterator<Recipe> trimRecipe = Bukkit.recipeIterator();
        while (trimRecipe.hasNext()) {
            Recipe r = trimRecipe.next();
            if (r == null) continue;

            if (r instanceof Keyed keyed) {
                NamespacedKey key = keyed.getKey();
                String name = key.getKey();

                if (name.endsWith("smithing_template") && !name.startsWith("netherite")) {
                    trimRecipe.remove();
                }
            }
        }
    }
}
