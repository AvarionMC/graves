package com.ranull.graves.data;

import org.bukkit.Location;

import java.util.UUID;

public class HologramData extends EntityData {
    private final int line;

    public HologramData(Location location, UUID uuidEntity, UUID uuidGrave, int line) {
        super(location, uuidEntity, uuidGrave, Type.HOLOGRAM);

        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
