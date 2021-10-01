package com.ranull.graves.data;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkData {
    private final World world;
    private final int x;
    private final int z;
    private final Map<Location, BlockData> blockDataMap;
    private final Map<UUID, HologramData> hologramDataMap;

    public ChunkData(Location location) {
        this.world = location.getWorld();
        this.x = location.getBlockX() >> 4;
        this.z = location.getBlockZ() >> 4;
        this.blockDataMap = new HashMap<>();
        this.hologramDataMap = new HashMap<>();
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

    public boolean isLoaded() {
        return world != null && world.isChunkLoaded(x, z);
    }

    public Location getLocation() {
        return new Location(world, x >> 4, 0, z >> 4);
    }

    public Map<Location, BlockData> getBlockDataMap() {
        return blockDataMap;
    }

    public Map<UUID, HologramData> getHologramDataMap() {
        return hologramDataMap;
    }

    public void addBlockData(BlockData blockData) {
        blockDataMap.put(blockData.getLocation(), blockData);
    }

    public void removeBlockData(Location location) {
        blockDataMap.remove(location);
    }

    public void addHologramData(HologramData hologramData) {
        hologramDataMap.put(hologramData.getUUIDEntity(), hologramData);
    }

    public void removeBlockData(HologramData hologramData) {
        hologramDataMap.remove(hologramData.getUUIDEntity());
    }
}
