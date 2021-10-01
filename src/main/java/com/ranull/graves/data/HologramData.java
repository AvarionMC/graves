package com.ranull.graves.data;

import org.bukkit.Location;

import java.util.UUID;

public class HologramData {
    private final Location location;
    private final UUID uuidEntity;
    private final UUID uuidGrave;
    private final int line;

    public HologramData(Location location, UUID uuidEntity, UUID uuidGrave, int line) {
        this.location = location;
        this.uuidEntity = uuidEntity;
        this.uuidGrave = uuidGrave;
        this.line = line;
    }

    public Location getLocation() {
        return location.clone();
    }

    public UUID getUUIDEntity() {
        return uuidEntity;
    }

    public UUID getUUIDGrave() {
        return uuidGrave;
    }

    public int getLine() {
        return line;
    }
}
