package dev.auto.trims.crafting;

import dev.auto.trims.Main;
import dev.auto.trims.utils.WeightedPicker;
import dev.auto.trims.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

public class RandomizedRiftCraftListener implements Listener {
    public static final NamespacedKey riftKey = new NamespacedKey("trims", "rift_dupe_randomized");
    private final WeightedPicker<Structure> randomizer;

    public RandomizedRiftCraftListener() {
        ItemStack placeholder = RiftToken.getRiftTokenItem();

        ShapedRecipe riftRecipe = new ShapedRecipe(riftKey, placeholder);
        riftRecipe.shape("CPC", "PDP", "CLC");
        riftRecipe.setIngredient('C', Material.AMETHYST_SHARD);
        riftRecipe.setIngredient('P', Material.ENDER_PEARL);
        riftRecipe.setIngredient('L', Material.LODESTONE);
        riftRecipe.setIngredient('D', Material.DIAMOND);

        Bukkit.addRecipe(riftRecipe);

        var structures = Main.getInstance().getConfig().getConfigurationSection("trims.structure-options");
        if (structures == null) throw new IllegalStateException("No structure-options section in config");

        randomizer = new WeightedPicker<>();

        for (String structure : structures.getKeys(false)) {
            var structureConfig = structures.getConfigurationSection(structure);
            if (structureConfig == null) throw new IllegalStateException("No structure config for " + structure);

            int weight = structureConfig.getInt("chance");
            // Structures are stored as ints in the config, we parse them than get their structure via #WorldManager.getStructureFromId
            randomizer.add(WorldManager.getStructureFromId(Integer.parseInt(structure)), weight);
        }
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || !(event.getRecipe() instanceof ShapedRecipe s)) return;
        if (!s.getKey().equals(riftKey)) return;

        CraftingInventory inv = event.getInventory();
        ItemStack[] m = inv.getMatrix();
        if (m == null || m.length < 9) { inv.setResult(null); return; }

        ItemStack result = RiftToken.getRiftTokenItem();
        inv.setResult(result);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe() == null || !(event.getRecipe() instanceof ShapedRecipe s)) return;
        if (!s.getKey().equals(riftKey)) return;

        // Do not allow shift-click crafting for this recipe to ensure a single randomized token per craft
        if (event.isShiftClick()) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player p) {
                p.sendMessage("Shift-click crafting is disabled for Rift Tokens.");
            }
            return;
        }

        // Pick a random structure based on configured weights right when the craft is finalized
        Structure structure = randomizer.pick();
        if (structure == null) {
            event.setCancelled(true);
            return;
        }

        ItemStack token = RiftToken.getRiftTokenItem(structure);
        event.setCurrentItem(token);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(riftKey);
    }
}
