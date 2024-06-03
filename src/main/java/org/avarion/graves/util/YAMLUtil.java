package org.avarion.graves.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class YAMLUtil {

    public static boolean isValidYAML(@NotNull File file) {
        return !file.getName().startsWith(".") && file.getName().endsWith(".yml");
    }

}
