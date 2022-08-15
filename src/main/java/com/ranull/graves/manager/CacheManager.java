package com.ranull.graves.manager;

import com.ranull.graves.data.ChunkData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CacheManager {
    private final Map<UUID, Grave> graveMap;
    private final Map<String, ChunkData> chunkMap;
    private final Map<UUID, Location> lastLocationMap;
    private final Map<UUID, List<ItemStack>> removedItemStackMap;

    public CacheManager() {
        this.graveMap = new HashMap<>();
        this.chunkMap = new HashMap<>();
        this.lastLocationMap = new HashMap<>();
        this.removedItemStackMap = new HashMap<>();
    }

    public Map<UUID, Grave> getGraveMap() {
        return graveMap;
    }

    public Map<String, ChunkData> getChunkMap() {
        return chunkMap;
    }

    public Map<UUID, Location> getLastLocationMap() {
        return lastLocationMap;
    }

    public Map<UUID, List<ItemStack>> getRemovedItemStackMap() {
        return removedItemStackMap;
    }
}
