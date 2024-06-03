package org.avarion.graves.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LocationUtil {

    @Contract("_ -> new")
    public static @NotNull Location roundLocation(@NotNull Location location) {
        return new Location(location.getWorld(), Math.round(location.getBlockX()), Math.round(location.getY()), Math.round(location.getBlockZ()));
    }

    public static @Nullable String locationToString(@NotNull Location location) {
        return location.getWorld() != null ? location.getWorld().getName()
                                             + "|"
                                             + location.getBlockX()
                                             + "|"
                                             + location.getBlockY()
                                             + "|"
                                             + location.getBlockZ() : null;
    }

    public static @Nullable String chunkToString(@NotNull Location location) {
        return location.getWorld() != null ? location.getWorld().getName() + "|" + (location.getBlockX() >> 4) + "|" + (
                location.getBlockZ()
                >> 4) : null;
    }

    @Contract("_ -> new")
    public static @NotNull Location chunkStringToLocation(@NotNull String string) {
        String[] strings = string.split("\\|");

        return new Location(Bukkit.getServer().getWorld(strings[0]), Integer.parseInt(strings[1])
                                                                     << 4, 0, Integer.parseInt(strings[2]) << 4);
    }

    @Contract("_ -> new")
    public static @NotNull Location stringToLocation(@NotNull String string) {
        String[] strings = string.split("\\|");

        return new Location(Bukkit.getServer()
                                  .getWorld(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]), Integer.parseInt(strings[3]));
    }

    public static Location getClosestLocation(Location locationBase, @NotNull List<Location> locationList) {
        Location locationClosest = null;

        for (Location location : locationList) {
            if (locationClosest == null) {
                locationClosest = location;
            }
            else if (location.distanceSquared(locationBase) < locationClosest.distanceSquared(locationBase)) {
                locationClosest = location;
            }
        }

        return locationClosest;
    }

}
