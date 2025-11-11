package dev.auto.trims.particles.utils;

public class ColorUtils {
    public static int hexToRgbInt(String hex) {
        hex = hex.replace("#", "").replace("0x", "");
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Hex must be 6 chars (RRGGBB)");
        }
        return Integer.parseInt(hex, 16);
}

}
