package com.ranull.graves.util;

import org.bukkit.entity.Entity;

public final class EntityUtil {
    public static boolean hasPermission(Entity entity, String permission) {
        return entity.hasPermission(permission);
    }
}
