package com.ranull.graves.data;

import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

public class BlockData implements Serializable {
    private final Location location;
    private final UUID graveUUID;
    private final String replaceMaterial;
    private final String replaceData;

    public BlockData(Location location, UUID graveUUID, String replaceMaterial, String replaceData) {
        this.location = location;
        this.graveUUID = graveUUID;
        this.replaceMaterial = replaceMaterial;
        this.replaceData = replaceData;
    }

    public Location getLocation() {
        return location.clone();
    }

    public UUID getGraveUUID() {
        return graveUUID;
    }

    public String getReplaceMaterial() {
        return replaceMaterial;
    }

    public String getReplaceData() {
        return replaceData;
    }

    public enum BlockType {
        DEATH,
        NORMAL,
        GRAVEYARD
    }
}
