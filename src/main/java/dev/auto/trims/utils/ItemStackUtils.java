package dev.auto.trims.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

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
            || t == Material.FLINT_AND_STEEL;
    }
}
