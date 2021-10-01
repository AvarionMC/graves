package com.ranull.graves.update;

import com.ranull.graves.Graves;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class UpdateChecker {
    private final Graves plugin;
    private final int resourceId;

    public UpdateChecker(Graves plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public String getVersion() {
        try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource="
                + resourceId).openStream(); Scanner scanner = new Scanner(inputStream)) {

            if (scanner.hasNext()) {
                return scanner.next();
            }
        } catch (IOException exception) {
            plugin.updateMessage("Cannot look for updates: " + exception.getMessage());
        }

        return null;
    }
}