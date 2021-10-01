package com.ranull.graves.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PermissionUtil {
    public static int getHighestInt(Player player, String permission) {
        List<Integer> gravePermissions = new ArrayList<>();

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().contains(permission)) {
                try {
                    gravePermissions.add(Integer.parseInt(perm.getPermission().replace(permission, "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!gravePermissions.isEmpty()) {
            return Collections.max(gravePermissions);
        }

        return 0;
    }

    public static double getHighestDouble(Player player, String permission) {
        List<Double> gravePermissions = new ArrayList<>();

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().contains(permission)) {
                try {
                    gravePermissions.add(Double.parseDouble(perm.getPermission().replace(permission, "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!gravePermissions.isEmpty()) {
            return Collections.max(gravePermissions);
        }

        return 0;
    }
}
