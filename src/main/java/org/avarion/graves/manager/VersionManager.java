package org.avarion.graves.manager;

public final class VersionManager {
    public final String version;
    public final boolean hasHexColors;
    public boolean isBukkit;
    public boolean isMohist;

    public VersionManager() {
        this.version = BukkitVersion.getVersion();

        try {
            Class.forName("org.spigotmc.SpigotConfig", false, getClass().getClassLoader());

            this.isBukkit = false;
        }
        catch (ClassNotFoundException ignored) {
            this.isBukkit = true;
        }

        this.hasHexColors = !isBukkit;

        try {
            Class.forName("com.mohistmc.config.MohistConfigUtil", false, getClass().getClassLoader());

            this.isMohist = true;
        }
        catch (ClassNotFoundException ignored) {
            this.isMohist = false;
        }
    }
}
