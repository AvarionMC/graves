package com.ranull.graves.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;

public final class LocationUtil {
    public static Location roundLocation(Location location) {
        return new Location(location.getWorld(), Math.round(location.getBlockX()), Math.round(location.getY()),
                Math.round(location.getBlockZ()));
    }

    public static String locationToString(Location location) {
        return location.getWorld() != null ? location.getWorld().getName() + "|" + location.getBlockX()
                + "|" + location.getBlockY() + "|" + location.getBlockZ() : null;
    }

    public static String chunkToString(Location location) {
        return location.getWorld() != null ? location.getWorld().getName() + "|" + (location.getBlockX() >> 4)
                + "|" + (location.getBlockZ() >> 4) : null;
    }

    public static Location chunkStringToLocation(String string) {
        String[] strings = string.split("\\|");

        return new Location(Bukkit.getServer().getWorld(strings[0]), Integer.parseInt(strings[1]) << 4,
                0, Integer.parseInt(strings[2]) << 4);
    }

    public static Location stringToLocation(String string) {
        String[] strings = string.split("\\|");

        return new Location(Bukkit.getServer().getWorld(strings[0]), Integer.parseInt(strings[1]),
                Integer.parseInt(strings[2]), Integer.parseInt(strings[3]));
    }

    public static Location getClosestLocation(Location locationBase, List<Location> locationList) {
        Location locationClosest = null;

        for (Location location : locationList) {
            if (locationClosest == null) {
                locationClosest = location;
            } else if (location.distanceSquared(locationBase) < locationClosest.distanceSquared(locationBase)) {
                locationClosest = location;
            }
        }

        return locationClosest;
    }
}
