package dev.auto.trims.utils;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import javax.annotation.Nullable;
import java.util.Map;

public class ItemStackUtils {
    public static boolean isWeapon(@Nullable ItemStack it) {
        if (it == null || it.getType().isAir()) return false;
        Material t = it.getType();
        return t.name().endsWith("_SWORD")
            || t.name().endsWith("_AXE")
            || t == Material.TRIDENT
            || t == Material.BOW
            || t == Material.CROSSBOW
            || t == Material.MACE
            || t == Material.FLINT_AND_STEEL
            || t == Material.LAVA_BUCKET;
    }
}
