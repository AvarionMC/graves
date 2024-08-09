package org.avarion.graves.util;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

public final class UpdateUtil {
    private UpdateUtil() {

    }

    public static @Nullable Version getLatestVersion(int pluginId) {
        URL url;
        try {
            url = new URI("https://api.spigotmc.org/legacy/update.php?resource=" + pluginId).toURL();

            InputStream inputStream = url.openStream();
            Scanner scanner = new Scanner(inputStream);

            if (scanner.hasNext()) {
                return new Version(scanner.next());
            }
        }
        catch (URISyntaxException | IOException ignored) {
        }

        return null;
    }
}
