package org.avarion.graves.manager;

import org.bukkit.Bukkit;


public class BukkitVersion {

    public static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

}
