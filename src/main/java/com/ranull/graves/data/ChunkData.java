package com.ranull.graves.data;

import org.bukkit.Location;
import org.bukkit.World;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkData implements Serializable {
    private final World world;
    private final int x;
    private final int z;
    private final Map<Location, BlockData> blockDataMap;
    private final Map<UUID, EntityData> entityDataMap;

    public ChunkData(Location location) {
        this.world = location.getWorld();
        this.x = location.getBlockX() >> 4;
        this.z = location.getBlockZ() >> 4;
        this.blockDataMap = new HashMap<>();
        this.entityDataMap = new HashMap<>();
    }

    public World getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean hasData() {
        return !blockDataMap.isEmpty() || !entityDataMap.isEmpty();
    }

    public boolean isLoaded() {
        return world != null && world.isChunkLoaded(x, z);
    }

    public Location getLocation() {
        return new Location(world, x >> 4, 0, z >> 4);
    }

    public Map<Location, BlockData> getBlockDataMap() {
        return blockDataMap;
    }

    public void addBlockData(BlockData blockData) {
        blockDataMap.put(blockData.getLocation(), blockData);
    }

    public void removeBlockData(Location location) {
        blockDataMap.remove(location);
    }

    public Map<UUID, EntityData> getEntityDataMap() {
        return entityDataMap;
    }

    public void addEntityData(EntityData entityData) {
        entityDataMap.put(entityData.getUUIDEntity(), entityData);
    }

    public void removeEntityData(EntityData entityData) {
        entityDataMap.remove(entityData.getUUIDEntity());
    }
}
