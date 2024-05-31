package org.avarion.graves.manager;

import org.bukkit.Bukkit;


public class BukkitVersion {

    public static String getVersion() {
        return Bukkit.getServer().getBukkitVersion().split("-")[0];
    }

}
