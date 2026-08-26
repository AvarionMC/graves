package org.avarion.graves.util;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public final class UpdateUtil {
    private static final int TIMEOUT_MILLIS = 5000;

    private UpdateUtil() {

    }

    public static @Nullable Version getLatestVersion(int pluginId) {
        try {
            URL url = new URI("https://api.spigotmc.org/legacy/update.php?resource=" + pluginId).toURL();

            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);

            try (InputStream inputStream = connection.getInputStream(); Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    return new Version(scanner.next());
                }
            }
        }
        catch (URISyntaxException | IOException ignored) {
        }

        return null;
    }
}
