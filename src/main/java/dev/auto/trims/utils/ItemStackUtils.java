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
            || t == Material.FLINT_AND_STEEL;
    }

    public static void enforceMaceBan(Player p) {
        Runnable task = () -> {
            PlayerInventory inv = p.getInventory();
            boolean changed = false;

            ItemStack main = inv.getItemInMainHand();
            if (main != null && main.getType() == Material.MACE) {
                inv.setItemInMainHand(null);
                Map<Integer, ItemStack> leftover = inv.addItem(main);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
                }
                changed = true;
            }

            ItemStack off = inv.getItemInOffHand();
            if (off != null && off.getType() == Material.MACE) {
                inv.setItemInOffHand(null);
                Map<Integer, ItemStack> leftover = inv.addItem(off);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
                }
                changed = true;
            }

            if (changed) p.updateInventory();
        };

        Bukkit.getScheduler().runTask(Main.getInstance(), task);
    }


}
