package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.integration.ItemsAdderData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.BlockFaceUtil;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ItemsAdder {
    private final Graves plugin;

    public ItemsAdder(Graves plugin) {
        this.plugin = plugin;
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
        location.setYaw(BlockFaceUtil.getBlockFaceYaw(BlockFaceUtil.getYawBlockFace(location.getYaw())));

        if (plugin.getConfig("itemsadder.enabled", grave)
                .getBoolean("itemsadder.enabled")) {
            String type = plugin.getConfig("itemsadder.name", grave)
                    .getString("itemsadder.type", "furniture");

            if (type.equalsIgnoreCase("furniture")) {
                String name = plugin.getConfig("itemsadder.name", grave)
                        .getString("itemsadder.name", "");
                CustomFurniture customFurniture = createCustomFurniture(name, location);

                if (customFurniture != null && customFurniture.getArmorstand() != null) {
                    plugin.getDataManager().addItemsAdderData(new ItemsAdderData(customFurniture.getArmorstand()
                            .getLocation(), customFurniture.getArmorstand().getUniqueId(), grave.getUUID()));
                } else {
                    plugin.debugMessage("Can't find ItemsAdder furniture " + name, 1);
                }
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
                        //CustomFurniture.remove((ArmorStand) entity, false); // Does not seem to work.
                        entity.remove(); // Use this because CustomFurniture.remove() does not work.
                        removedItemsAdderDataList.add(itemsAdderData);
                    }
                }
            }

            plugin.getDataManager().removeItemsAdderData(removedItemsAdderDataList);
        }
    }

    public void createBlock(Location location, Grave grave) {
        if (plugin.getConfig("itemsadder.enabled", grave)
                .getBoolean("itemsadder.enabled")) {
            String type = plugin.getConfig("itemsadder.name", grave)
                    .getString("itemsadder.type", "block");

            if (type.equalsIgnoreCase("block")) {
                String name = plugin.getConfig("itemsadder.name", grave)
                        .getString("itemsadder.name", "");
                CustomBlock customBlock = createCustomBlock(name, location);

                if (customBlock == null) {
                    plugin.debugMessage("Can't find ItemsAdder block " + name, 1);
                }
            }
        }
    }

    private CustomFurniture createCustomFurniture(String name, Location location) {
        return CustomFurniture.spawn(name, location.getBlock());
    }

    private CustomBlock createCustomBlock(String name, Location location) {
        return CustomBlock.place(name, location);
    }
}