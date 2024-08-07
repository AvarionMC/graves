package org.avarion.graves.manager;

import org.avarion.graves.data.ChunkData;
import org.avarion.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CacheManager {
    public static final Map<UUID, Grave> graveMap = new HashMap<>();
    public static final Map<String, ChunkData> chunkMap = new HashMap<>();
    public static final Map<UUID, Location> lastLocationMap = new HashMap<>();
    public static final Map<UUID, List<ItemStack>> removedItemStackMap = new HashMap<>();

    private CacheManager() {
        // Nothing to see here...
    }
}
