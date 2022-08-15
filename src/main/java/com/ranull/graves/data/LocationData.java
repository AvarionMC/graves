package com.ranull.graves.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

public class LocationData implements Serializable {
    UUID uuid;
    float yaw;
    float pitch;
    double x;
    double y;
    double z;

    public LocationData(Location location) {
        uuid = location.getWorld() != null ? location.getWorld().getUID() : null;
        yaw = location.getYaw();
        pitch = location.getPitch();
        x = location.getX();
        y = location.getY();
        z = location.getZ();
    }

    public Location getLocation() {
        return uuid != null ? new Location(Bukkit.getWorld(uuid), x, y, z, yaw, pitch) : null;
    }
}