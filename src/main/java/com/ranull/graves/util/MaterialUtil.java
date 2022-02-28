package com.ranull.graves.util;

import org.bukkit.Material;

public final class MaterialUtil {
    public static boolean isAir(Material material) {
        return isAir(material.name());
    }

    public static boolean isAir(String string) {
        switch (string) {
            case "AIR":
            case "CAVE_AIR":
            case "VOID_AIR":
                return true;
            default:
                return false;
        }
    }

    public static boolean isLava(Material material) {
        return isLava(material.name());
    }

    public static boolean isLava(String string) {
        switch (string) {
            case "LAVA":
            case "STATIONARY_LAVA":
                return true;
            default:
                return false;
        }
    }

    public static boolean isSafeNotSolid(Material material) {
        return !isSolid(material) && !isLava(material);
    }

    public static boolean isSafeSolid(Material material) {
        return isSolid(material) && !isLava(material);
    }

    private static boolean isSolid(Material material) {
        return material.isSolid() || isSafe(material);
    }

    private static boolean isSafe(Material material) {
        return isSafe(material.name());
    }

    private static boolean isSafe(String string) {
        switch (string) {
            case "SCAFFOLDING":
            case "POWDER_SNOW":
                return true;
            default:
                return false;
        }
    }

    public static boolean isWater(Material material) {
        return isWater(material.name());
    }

    public static boolean isWater(String string) {
        switch (string) {
            case "WATER":
            case "STATIONARY_WATER":
                return true;
            default:
                return false;
        }
    }

    public static boolean isPlayerHead(Material material) {
        return isPlayerHead(material.name());
    }

    public static boolean isPlayerHead(String string) {
        switch (string) {
            case "PLAYER_HEAD":
            case "SKULL":
                return true;
            default:
                return false;
        }
    }
}
