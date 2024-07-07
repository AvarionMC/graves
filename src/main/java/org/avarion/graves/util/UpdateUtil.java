package org.avarion.graves.util;

import org.avarion.graves.Graves;
import org.avarion.graves.util.Version;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

public class UpdateUtil {
    public static void run(Graves plugin, int pluginId) {
        Version lastVersion = getLatestVersion(pluginId);
        if (lastVersion == null) {
            plugin.getLogger().severe("Couldn't fetch latest version information from SpigotMC.");
            return;
        }

        final Version currentVersion = new Version(plugin.getDescription().getVersion());
        if (lastVersion.compareTo(currentVersion) < 0) {
            plugin.getLogger().warning("New version available: " + lastVersion + ", you have: " + currentVersion);
        }
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
