package com.ranull.graves.data;

import org.bukkit.Location;

import java.util.UUID;

public class EntityData {
    private final Location location;
    private final UUID uuidEntity;
    private final UUID uuidGrave;
    private final Type type;

    public EntityData(Location location, UUID uuidEntity, UUID uuidGrave, Type type) {
        this.location = location;
        this.uuidEntity = uuidEntity;
        this.uuidGrave = uuidGrave;
        this.type = type;
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

    public Type getType() {
        return type;
    }

    public enum Type {
        HOLOGRAM,
        FURNITURELIB,
        FURNITUREENGINE,
        ITEMSADDER,
        ORAXEN,
    }
}
