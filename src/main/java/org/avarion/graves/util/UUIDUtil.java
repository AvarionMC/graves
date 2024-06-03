package org.avarion.graves.util;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class UUIDUtil {

    public static @Nullable UUID getUUID(String string) {
        try {
            return UUID.fromString(string);
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}
