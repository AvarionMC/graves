package com.ranull.graves.util;

import java.util.UUID;

public final class UUIDUtil {
    public static UUID getUUID(String string) {
        try {
            return UUID.fromString(string);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
