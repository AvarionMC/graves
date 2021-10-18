package com.ranull.graves.data;

import com.ranull.graves.data.integration.FurnitureLibData;
import com.ranull.graves.data.integration.ItemsAdderData;
import com.ranull.graves.data.integration.OraxenData;
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
    private final Map<UUID, FurnitureLibData> furnitureLibDataMap;
    private final Map<UUID, ItemsAdderData> itemsAdderDataMap;
    private final Map<UUID, OraxenData> oraxenDataMap;

    public ChunkData(Location location) {
        this.world = location.getWorld();
        this.x = location.getBlockX() >> 4;
        this.z = location.getBlockZ() >> 4;
        this.blockDataMap = new HashMap<>();
        this.hologramDataMap = new HashMap<>();
        this.furnitureLibDataMap = new HashMap<>();
        this.itemsAdderDataMap = new HashMap<>();
        this.oraxenDataMap = new HashMap<>();
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

    public Map<UUID, FurnitureLibData> getFurnitureLibMap() {
        return furnitureLibDataMap;
    }

    public Map<UUID, ItemsAdderData> getItemsAdderMap() {
        return itemsAdderDataMap;
    }

    public Map<UUID, OraxenData> getOraxenDataMap() {
        return oraxenDataMap;
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

    public void removeHologramData(HologramData hologramData) {
        hologramDataMap.remove(hologramData.getUUIDEntity());
    }

    public void addFurnitureLibData(FurnitureLibData furnitureData) {
        furnitureLibDataMap.put(furnitureData.getUUIDEntity(), furnitureData);
    }

    public void removeFurnitureLibData(FurnitureLibData furnitureLibData) {
        furnitureLibDataMap.remove(furnitureLibData.getUUIDEntity());
    }

    public void addItemsAdderData(ItemsAdderData furnitureData) {
        itemsAdderDataMap.put(furnitureData.getUUIDEntity(), furnitureData);
    }

    public void removeItemsAdderData(ItemsAdderData itemsAdderData) {
        furnitureLibDataMap.remove(itemsAdderData.getUUIDEntity());
    }

    public void addOraxenData(OraxenData oraxenData) {
        oraxenDataMap.put(oraxenData.getUUIDEntity(), oraxenData);
    }

    public void removeOraxenData(OraxenData oraxenData) {
        oraxenDataMap.remove(oraxenData.getUUIDEntity());
    }
}
