package com.ranull.graves.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class FileUtil {
    public static void moveFile(File file, String name) {
        try {
            Files.move(file.toPath(), file.toPath().resolveSibling(name));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
