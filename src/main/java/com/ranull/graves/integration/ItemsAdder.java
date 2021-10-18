package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.integration.ItemsAdderData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.ResourceUtil;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ItemsAdder {
    private final Graves plugin;
    private final Plugin itemsAdderPlugin;

    public ItemsAdder(Graves plugin, Plugin itemsAdderPlugin) {
        this.plugin = plugin;
        this.itemsAdderPlugin = itemsAdderPlugin;

        saveData();
    }

    public void saveData() {
        if (plugin.getConfig().getBoolean("settings.integration.itemsadder.write")) {
            ResourceUtil.copyResources("data/plugin/" + itemsAdderPlugin.getName().toLowerCase() + "/data",
                    plugin.getPluginsFolder() + "/" + itemsAdderPlugin.getName() + "/data", plugin);
            ResourceUtil.copyResources("data/model/grave.json", plugin.getPluginsFolder() + "/"
                    + itemsAdderPlugin.getName() + "/data/resource_pack/assets/graves/models/graves/grave.json", plugin);
            plugin.debugMessage("Saving " + itemsAdderPlugin.getName() + " data.", 1);
        }
    }

    public ItemsAdderData getItemsAdderData(Entity entity) {
        if (plugin.getDataManager().hasChunkData(entity.getLocation())) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(entity.getLocation());

            if (chunkData.getItemsAdderMap().containsKey(entity.getUniqueId())) {
                return chunkData.getItemsAdderMap().get(entity.getUniqueId());
            }
        }

        return null;
    }

    public Grave getGraveFromItemsAdder(Entity entity) {
        ItemsAdderData itemsAdderData = getItemsAdderData(entity);

        return itemsAdderData != null && plugin.getDataManager().getGraveMap()
                .containsKey(itemsAdderData.getUUIDGrave()) ? plugin.getDataManager().getGraveMap()
                .get(itemsAdderData.getUUIDGrave()) : null;
    }

    public void createFurniture(Location location, Grave grave) {
        location = LocationUtil.roundLocation(location).add(0.5, 0, 0.5);

        location.setYaw(BlockFaceUtil.getBlockFaceYaw(BlockFaceUtil.getYawBlockFace(grave.getYaw()).getOppositeFace()));
        location.setPitch(grave.getPitch());

        if (plugin.getConfig("itemsadder.furniture.enabled", grave)
                .getBoolean("itemsadder.furniture.enabled")) {
            String name = plugin.getConfig("itemsadder.furniture.name", grave)
                    .getString("itemsadder.furniture.name", "");
            CustomFurniture customFurniture = createCustomFurniture(name, location);

            if (customFurniture != null && customFurniture.getArmorstand() != null) {
                customFurniture.teleport(location);
                plugin.getDataManager().addItemsAdderData(new ItemsAdderData(customFurniture.getArmorstand()
                        .getLocation(), customFurniture.getArmorstand().getUniqueId(), grave.getUUID()));
                plugin.debugMessage("Placing ItemsAdder furniture for " + grave.getUUID() + " at "
                        + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                        + (location.getBlockY() + 0.5) + "Y, " + (location.getBlockZ() + 0.5) + "z", 1);
            } else {
                plugin.debugMessage("Can't find ItemsAdder furniture " + name, 1);
            }
        }
    }

    public void removeFurniture(Grave grave) {
        List<ItemsAdderData> itemsAdderDataList = new ArrayList<>();

        for (Map.Entry<String, ChunkData> chunkDataEntry : plugin.getDataManager().getChunkDataMap().entrySet()) {
            ChunkData chunkData = chunkDataEntry.getValue();

            if (chunkDataEntry.getValue().isLoaded()) {
                for (ItemsAdderData itemsAdderData : new ArrayList<>(chunkData.getItemsAdderMap().values())) {
                    if (grave.getUUID().equals(itemsAdderData.getUUIDGrave())) {
                        itemsAdderDataList.add(itemsAdderData);
                    }
                }
            }
        }

        removeFurniture(itemsAdderDataList);
    }

    public void removeFurniture(ItemsAdderData itemsAdderData) {
        removeFurniture(Collections.singletonList(itemsAdderData));
    }

    public void removeFurniture(List<ItemsAdderData> furnitureLibDataList) {
        List<ItemsAdderData> removedItemsAdderDataList = new ArrayList<>();

        if (!furnitureLibDataList.isEmpty()) {
            for (ItemsAdderData itemsAdderData : furnitureLibDataList) {
                for (Entity entity : itemsAdderData.getLocation().getChunk().getEntities()) {
                    if (entity instanceof ArmorStand && entity.getUniqueId().equals(itemsAdderData.getUUIDEntity())) {
                        CustomFurniture.remove(entity, false);
                        entity.remove();
                        removedItemsAdderDataList.add(itemsAdderData);
                    }
                }
            }

            plugin.getDataManager().removeItemsAdderData(removedItemsAdderDataList);
        }
    }

    public void createBlock(Location location, Grave grave) {
        if (plugin.getConfig("itemsadder.block.enabled", grave)
                .getBoolean("itemsadder.block.enabled")) {
            String name = plugin.getConfig("itemsadder.block.name", grave)
                    .getString("itemsadder.block.name", "");
            CustomBlock customBlock = createCustomBlock(name, location);

            if (customBlock != null) {
                plugin.debugMessage("Placing ItemsAdder block for " + grave.getUUID() + " at "
                        + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                        + (location.getBlockY() + 0.5) + "Y, " + (location.getBlockZ() + 0.5) + "z", 1);
            } else {
                plugin.debugMessage("Can't find ItemsAdder block " + name, 1);
            }
        }
    }

    public boolean isCustomBlock(Location location) {
        return CustomBlock.byAlreadyPlaced(location.getBlock()) != null;
    }

    public void removeBlock(Location location) {
        CustomBlock.remove(location);
    }

    private CustomFurniture createCustomFurniture(String name, Location location) {
        return CustomFurniture.spawn(name, location.getBlock());
    }

    private CustomBlock createCustomBlock(String name, Location location) {
        return CustomBlock.place(name, location);
    }
}