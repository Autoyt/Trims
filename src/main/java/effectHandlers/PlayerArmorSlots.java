package effectHandlers;

import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PlayerArmorSlots {
    @Nullable
    private TrimPattern helmet;
    @Nullable
    private TrimPattern chestplate;
    @Nullable
    private TrimPattern leggings;
    @Nullable
    private TrimPattern boots;

    @Nullable
    public TrimPattern helmet() {
        return helmet;
    }

    @Nullable
    public TrimPattern chestplate() {
        return chestplate;
    }

    @Nullable
    public TrimPattern leggings() {
        return leggings;
    }

    @Nullable
    public TrimPattern boots() {
        return boots;
    }

    public void setHelmet(@Nullable TrimPattern v) {
        helmet = v;
    }

    public void setChestplate(@Nullable TrimPattern v) {
        chestplate = v;
    }

    public void setLeggings(@Nullable TrimPattern v) {
        leggings = v;
    }

    public void setBoots(@Nullable TrimPattern v) {
        boots = v;
    }

    public int instancesOfTrim(TrimPattern target) {
        int n = 0;
        if (Objects.equals(helmet, target)) n++;
        if (Objects.equals(chestplate, target)) n++;
        if (Objects.equals(leggings, target)) n++;
        if (Objects.equals(boots, target)) n++;
        return n;
    }

    @Override
    public String toString() {
        return "PlayerArmorSlots{" +
                "helmet=" + helmet +
                ", chestplate=" + chestplate +
                ", leggings=" + leggings +
                ", boots=" + boots +
                '}';
    }
}