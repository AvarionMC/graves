package com.ranull.graves.util;

import java.io.File;

public final class YAMLUtil {
    public static boolean isValidYAML(File file) {
        return !file.getName().startsWith(".") && file.getName().endsWith(".yml");
    }
}
