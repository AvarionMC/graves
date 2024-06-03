package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.data.BlockData;
import org.avarion.graves.data.ChunkData;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BlockManager {

    private final Graves plugin;

    public BlockManager(Graves plugin) {
        this.plugin = plugin;
    }

    public BlockData getBlockData(Block block) {
        if (plugin.getDataManager().hasChunkData(block.getLocation())) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(block.getLocation());

            if (chunkData.getBlockDataMap().containsKey(block.getLocation())) {
                return chunkData.getBlockDataMap().get(block.getLocation());
            }
        }

        return null;
    }

    public Grave getGraveFromBlock(Block block) {
        BlockData blockData = getBlockData(block);

        return blockData != null && CacheManager.graveMap.containsKey(blockData.getGraveUUID())
               ? CacheManager.graveMap.get(blockData.getGraveUUID())
               : null;
    }

    public void createBlock(Location location, Grave grave) {
        location = LocationUtil.roundLocation(location);

        if (location.getWorld() != null) {
            Material material;

            if (plugin.getConfigBool("block.enabled", grave)) {
                String materialString = plugin.getConfigString("block.material", grave, "CHEST");

                material = Material.matchMaterial(materialString);
            }
            else {
                material = null;
            }

            int offsetX = plugin.getConfigInt("block.offset.x", grave);
            int offsetY = plugin.getConfigInt("block.offset.y", grave);
            int offsetZ = plugin.getConfigInt("block.offset.z", grave);

            location.add(offsetX, offsetY, offsetZ);

            BlockData blockData = plugin.getCompatibility().setBlockData(location, material, grave, plugin);

            plugin.getDataManager().addBlockData(blockData);

            if (plugin.getIntegrationManager().hasMultiPaper()) {
                plugin.getIntegrationManager().getMultiPaper().notifyBlockCreation(blockData);
            }

            if (plugin.getIntegrationManager().hasItemsAdder()) {
                plugin.getIntegrationManager().getItemsAdder().createBlock(location, grave);
            }

            if (plugin.getIntegrationManager().hasOraxen()) {
                plugin.getIntegrationManager().getOraxen().createBlock(location, grave);
            }

            if (material != null) {
                plugin.debugMessage("Placing grave block for "
                                    + grave.getUUID()
                                    + " at "
                                    + location.getWorld()
                                              .getName()
                                    + ", "
                                    + (location.getBlockX() + 0.5)
                                    + "x, "
                                    + (location.getBlockY() + 0.5)
                                    + "y, "
                                    + (location.getBlockZ() + 0.5)
                                    + "z", 1);
            }
            else {
                plugin.debugMessage("Placing access location for "
                                    + grave.getUUID()
                                    + " at "
                                    + location.getWorld()
                                              .getName()
                                    + ", "
                                    + (location.getBlockX() + 0.5)
                                    + "x, "
                                    + (location.getBlockY() + 0.5)
                                    + "y, "
                                    + (location.getBlockZ() + 0.5)
                                    + "z", 1);
            }
        }
    }

    public List<BlockData> getBlockDataList(Grave grave) {
        List<BlockData> blockDataList = new ArrayList<>();

        for (Map.Entry<String, ChunkData> chunkDataEntry : CacheManager.chunkMap.entrySet()) {
            for (BlockData blockData : new ArrayList<>(chunkDataEntry.getValue().getBlockDataMap().values())) {
                if (grave.getUUID().equals(blockData.getGraveUUID())) {
                    blockDataList.add(blockData);
                }
            }
        }

        return blockDataList;
    }

    public List<Location> getBlockList(Grave grave) {
        List<Location> locationList = new ArrayList<>();

        for (Map.Entry<String, ChunkData> chunkDataEntry : CacheManager.chunkMap.entrySet()) {
            for (BlockData blockData : new ArrayList<>(chunkDataEntry.getValue().getBlockDataMap().values())) {
                if (grave.getUUID().equals(blockData.getGraveUUID())) {
                    locationList.add(blockData.getLocation());
                }
            }
        }

        return locationList;
    }

    public void removeBlock(Grave grave) {
        for (ChunkData chunkData : CacheManager.chunkMap.values()) {

            if (chunkData.isLoaded()) {
                for (BlockData blockData : new ArrayList<>(chunkData.getBlockDataMap().values())) {
                    if (grave.getUUID().equals(blockData.getGraveUUID())) {
                        removeBlock(blockData);
                    }
                }
            }
        }
    }

    public void removeBlock(BlockData blockData) {
        Location location = blockData.getLocation();

        if (plugin.getIntegrationManager().hasItemsAdder() && plugin.getIntegrationManager()
                                                                    .getItemsAdder()
                                                                    .isCustomBlock(location)) {
            plugin.getIntegrationManager().getItemsAdder().removeBlock(location);
        }

        if (plugin.getIntegrationManager().hasOraxen() && plugin.getIntegrationManager()
                                                                .getOraxen()
                                                                .isCustomBlock(location)) {
            plugin.getIntegrationManager().getOraxen().removeBlock(location);
        }

        if (location.getWorld() != null) {
            if (blockData.getReplaceMaterial() != null) {
                Material material = Material.matchMaterial(blockData.getReplaceMaterial());

                if (material != null) {
                    blockData.getLocation().getBlock().setType(material);
                }
            }
            else {
                blockData.getLocation().getBlock().setType(Material.AIR);
            }

            if (blockData.getReplaceData() != null) {
                blockData.getLocation()
                         .getBlock()
                         .setBlockData(plugin.getServer().createBlockData(blockData.getReplaceData()));
            }

            plugin.getDataManager().removeBlockData(location);
            plugin.debugMessage("Replacing grave block for "
                                + blockData.getGraveUUID()
                                + " at "
                                + location.getWorld()
                                          .getName()
                                + ", "
                                + (location.getBlockX() + 0.5)
                                + "x, "
                                + (location.getBlockY() + 0.5)
                                + "y, "
                                + (location.getBlockZ() + 0.5)
                                + "z", 1);
        }
    }

}
