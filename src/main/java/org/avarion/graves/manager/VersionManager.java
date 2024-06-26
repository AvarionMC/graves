package org.avarion.graves.manager;

import org.bukkit.Bukkit;

public final class VersionManager {
    public final String version;
    public final boolean hasHexColors;
    public final boolean isBukkit;
    public final boolean isMohist;

    public VersionManager() {
        boolean isMohist = false;
        boolean isBukkit = false;
        this.version = Bukkit.getServer().getBukkitVersion().split("-")[0];

        try {
            Class.forName("org.spigotmc.SpigotConfig", false, getClass().getClassLoader());
        }
        catch (ClassNotFoundException ignored) {
            isBukkit = true;
        }

        try {
            Class.forName("com.mohistmc.config.MohistConfigUtil", false, getClass().getClassLoader());

            isMohist = true;
        }
        catch (ClassNotFoundException ignored) {
        }

        this.isMohist = isMohist;
        this.isBukkit = isBukkit;
        this.hasHexColors = !isBukkit;
    }
}
