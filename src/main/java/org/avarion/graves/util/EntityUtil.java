package org.avarion.graves.util;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public final class EntityUtil {

    private EntityUtil() {
        // Don't do anything here
    }

    public static boolean hasPermission(@NotNull Entity entity, String permission) {
        try {
            return entity.hasPermission(permission);
        }
        catch (NoSuchMethodError ignored) {
        }

        return true;
    }

}
