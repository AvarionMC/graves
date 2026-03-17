package org.avarion.graves.util;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class EntityUtil {

    private EntityUtil() {
        // Don't do anything here
    }

    public static boolean hasDeathDrops(@NotNull EntityDeathEvent event) {
        return hasDeathDrops(event.getDrops(), event.getDroppedExp());
    }

    public static boolean hasDeathDrops(@NotNull List<ItemStack> items, int droppedExp) {
        return !items.isEmpty() || droppedExp > 0;
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
