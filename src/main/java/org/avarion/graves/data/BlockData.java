package org.avarion.graves.data;

import org.bukkit.Location;

import java.io.Serializable;
import java.util.UUID;

public record BlockData(Location location, UUID graveUUID, String replaceMaterial,
                        String replaceData) implements Serializable {

    @Override
    public Location location() {
        return location.clone();
    }

    public enum BlockType {
        DEATH, NORMAL, GRAVEYARD
    }

}
