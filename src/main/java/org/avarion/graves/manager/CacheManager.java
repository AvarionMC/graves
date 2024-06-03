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
    static public final Map<UUID, Grave> graveMap = new HashMap<>();
    static public final Map<String, ChunkData> chunkMap = new HashMap<>();
    static public final Map<UUID, Location> lastLocationMap = new HashMap<>();
    static public final Map<UUID, List<ItemStack>> removedItemStackMap = new HashMap<>();
}
