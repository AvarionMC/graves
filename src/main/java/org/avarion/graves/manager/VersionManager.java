package org.avarion.graves.manager;

import org.bukkit.Bukkit;

public final class VersionManager {
    public final String version;
    public final boolean hasHexColors;
    public final boolean isBukkit;
    public final boolean isMohist;

    public VersionManager() {
        boolean mohist = false;
        boolean bukkit = false;
        this.version = Bukkit.getServer().getBukkitVersion().split("-")[0];

        try {
            Class.forName("org.spigotmc.SpigotConfig", false, getClass().getClassLoader());
        }
        catch (ClassNotFoundException ignored) {
            bukkit = true;
        }

        try {
            Class.forName("com.mohistmc.config.MohistConfigUtil", false, getClass().getClassLoader());

            mohist = true;
        }
        catch (ClassNotFoundException ignored) {
        }

        this.isMohist = mohist;
        this.isBukkit = bukkit;
        this.hasHexColors = !bukkit;
    }
}
