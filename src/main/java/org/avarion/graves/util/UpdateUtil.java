package org.avarion.graves.util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public final class UpdateUtil {

    public static @Nullable String getLatestVersion(int resourceId) {
        try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource="
                                               + resourceId).openStream(); Scanner scanner = new Scanner(inputStream)) {

            if (scanner.hasNext()) {
                return scanner.next();
            }
        }
        catch (IOException ignored) {
        }

        return null;
    }

}
