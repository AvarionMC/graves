package org.avarion.graves.util;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public final class MaterialUtil {
    public static boolean isLava(@NotNull Material material) {
        return isLava(material.name());
    }

    public static boolean isLava(@NotNull String string) {
        return switch (string) {
            case "LAVA", "STATIONARY_LAVA" -> true;
            default -> false;
        };
    }

    public static boolean isSafeNotSolid(Material material) {
        return !isSolid(material) && !isLava(material);
    }

    public static boolean isSafeSolid(Material material) {
        return isSolid(material) && !isLava(material);
    }

    private static boolean isSolid(@NotNull Material material) {
        return material.isSolid() || isSafe(material);
    }

    private static boolean isSafe(@NotNull Material material) {
        return isSafe(material.name());
    }

    private static boolean isSafe(@NotNull String string) {
        return switch (string) {
            case "SCAFFOLDING", "POWDER_SNOW" -> true;
            default -> false;
        };
    }

    public static boolean isWater(@NotNull Material material) {
        return isWater(material.name());
    }

    public static boolean isWater(@NotNull String string) {
        return switch (string) {
            case "WATER", "STATIONARY_WATER" -> true;
            default -> false;
        };
    }

    public static boolean isPlayerHead(@NotNull String string) {
        return switch (string) {
            case "PLAYER_HEAD", "SKULL" -> true;
            default -> false;
        };
    }

}
