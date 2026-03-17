package org.avarion.graves.util;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public final class EntityUtil {

    private EntityUtil() {
        // Don't do anything here
    }

    public static boolean hasDeathDrops(@NotNull EntityDeathEvent event) {
        return !event.getDrops().isEmpty() || event.getDroppedExp() > 0;
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
