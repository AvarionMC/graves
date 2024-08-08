package org.avarion.graves.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class FileUtil {

    private FileUtil() {
        // Don't do anything here
    }

    public static void moveFile(@NotNull File file, String name) {
        try {
            Files.move(file.toPath(), file.toPath().resolveSibling(name));
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
