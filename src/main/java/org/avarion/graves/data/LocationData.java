package org.avarion.graves.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

public class LocationData implements Serializable {

    final UUID uuid;
    final float yaw;
    final float pitch;
    final double x;
    final double y;
    final double z;

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
