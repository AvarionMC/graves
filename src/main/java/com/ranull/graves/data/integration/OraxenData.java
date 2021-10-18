package com.ranull.graves.data.integration;

import org.bukkit.Location;

import java.util.UUID;

public class OraxenData {
    private final Location location;
    private final UUID uuidEntity;
    private final UUID uuidGrave;

    public OraxenData(Location location, UUID uuidEntity, UUID uuidGrave) {
        this.location = location;
        this.uuidEntity = uuidEntity;
        this.uuidGrave = uuidGrave;
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
}
