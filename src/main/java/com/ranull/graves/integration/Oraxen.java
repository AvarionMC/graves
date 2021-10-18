package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.integration.OraxenData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.listener.integration.oraxen.FurnitureBreakListener;
import com.ranull.graves.listener.integration.oraxen.FurnitureInteractListener;
import com.ranull.graves.manager.VersionManager;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.ResourceUtil;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Oraxen {
    private final Graves plugin;
    private final Plugin oraxenPlugin;
    private final boolean hasFurniture;
    private final FurnitureInteractListener furnitureInteractListener;
    private final FurnitureBreakListener furnitureBreakListener;

    public Oraxen(Graves plugin, Plugin oraxenPlugin) {
        this.plugin = plugin;
        this.oraxenPlugin = oraxenPlugin;
        VersionManager versionManager = plugin.getVersionManager();
        this.hasFurniture = !versionManager.is_v1_7() && !versionManager.is_v1_8() && !versionManager.is_v1_9()
                && !versionManager.is_v1_10() && !versionManager.is_v1_11() && !versionManager.is_v1_12()
                && !versionManager.is_v1_13() && !versionManager.is_v1_14() && !versionManager.is_v1_15();
        this.furnitureInteractListener = new FurnitureInteractListener(plugin, this);
        this.furnitureBreakListener = new FurnitureBreakListener(this);

        saveData();
        plugin.getServer().getPluginManager().registerEvents(furnitureInteractListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(furnitureBreakListener, plugin);
    }

    public void unregister() {
        if (furnitureInteractListener != null) {
            HandlerList.unregisterAll(furnitureInteractListener);
        }

        if (furnitureBreakListener != null) {
            HandlerList.unregisterAll(furnitureBreakListener);
        }
    }

    public void saveData() {
        if (plugin.getConfig().getBoolean("settings.integration.oraxen.write")) {
            ResourceUtil.copyResources("data/plugin/" + oraxenPlugin.getName().toLowerCase() + "/items",
                    plugin.getPluginsFolder() + "/" + oraxenPlugin.getName() + "/items", plugin);
            ResourceUtil.copyResources("data/model/grave.json",
                    plugin.getPluginsFolder() + "/" + oraxenPlugin.getName()
                            + "/pack/assets/minecraft/models/graves/grave.json", plugin);
            plugin.debugMessage("Saving " + oraxenPlugin.getName() + " data.", 1);
        }
    }

    public OraxenData getOraxenData(Entity entity) {
        if (plugin.getDataManager().hasChunkData(entity.getLocation())) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(entity.getLocation());

            if (chunkData.getOraxenDataMap().containsKey(entity.getUniqueId())) {
                return chunkData.getOraxenDataMap().get(entity.getUniqueId());
            }
        }

        return null;
    }

    public Grave getGraveFromOraxen(Entity entity) {
        OraxenData oraxenData = getOraxenData(entity);

        return oraxenData != null && plugin.getDataManager().getGraveMap()
                .containsKey(oraxenData.getUUIDGrave()) ? plugin.getDataManager().getGraveMap()
                .get(oraxenData.getUUIDGrave()) : null;
    }

    public boolean hasFurniture() {
        return hasFurniture;
    }

    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfig("oraxen.furniture.enabled", grave)
                .getBoolean("oraxen.furniture.enabled")) {
            if (hasFurniture()) {
                String name = plugin.getConfig("oraxen.furniture.name", grave)
                        .getString("oraxen.furniture.name", "");
                FurnitureMechanic furnitureMechanic = getFurnitureMechanic(name);

                if (furnitureMechanic != null && location.getWorld() != null) {
                    furnitureMechanic.place(BlockFaceUtil.getBlockFaceRotation(BlockFaceUtil
                                    .getYawBlockFace(grave.getYaw())), grave.getYaw(),
                            BlockFace.UP, location, name);

                    ItemFrame itemFrame = FurnitureMechanic.getItemFrame(location);

                    if (itemFrame != null) {
                        plugin.getDataManager().addOraxenData(new OraxenData(location, itemFrame.getUniqueId(),
                                grave.getUUID()));
                    }

                    plugin.debugMessage("Placing Oraxen furniture for " + grave.getUUID() + " at "
                            + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                            + (location.getBlockY() + 0.5) + "Y, " + (location.getBlockZ() + 0.5) + "z", 1);
                }
            } else {
                plugin.warningMessage("This version of Minecraft does not support " + oraxenPlugin.getName()
                        + " furniture");
            }
        }
    }

    public void removeFurniture(Grave grave) {
        List<OraxenData> oraxenDataList = new ArrayList<>();

        for (Map.Entry<String, ChunkData> chunkDataEntry : plugin.getDataManager().getChunkDataMap().entrySet()) {
            ChunkData chunkData = chunkDataEntry.getValue();

            if (chunkDataEntry.getValue().isLoaded()) {
                for (OraxenData oraxenData : new ArrayList<>(chunkData.getOraxenDataMap().values())) {
                    if (grave.getUUID().equals(oraxenData.getUUIDGrave())) {
                        oraxenDataList.add(oraxenData);
                    }
                }
            }
        }

        removeFurniture(oraxenDataList);
    }

    public void removeFurniture(OraxenData oraxenData) {
        removeFurniture(Collections.singletonList(oraxenData));
    }

    public void removeFurniture(List<OraxenData> oraxenDataList) {
        List<OraxenData> removedOraxenDataList = new ArrayList<>();

        if (!oraxenDataList.isEmpty()) {
            for (OraxenData oraxenData : oraxenDataList) {
                for (Entity entity : oraxenData.getLocation().getChunk().getEntities()) {
                    if (entity instanceof ItemFrame && entity.getUniqueId().equals(oraxenData.getUUIDEntity())) {
                        entity.remove();
                        removedOraxenDataList.add(oraxenData);
                    }
                }
            }

            plugin.getDataManager().removeOraxenData(removedOraxenDataList);
        }
    }

    public void createBlock(Location location, Grave grave) {
        if (plugin.getConfig("oraxen.block.enabled", grave)
                .getBoolean("oraxen.block.enabled")) {
            String name = plugin.getConfig("oraxen.block.name", grave)
                    .getString("oraxen.block.name", "");
            NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(name);

            if (noteBlockMechanic != null && location.getWorld() != null) {
                location.getBlock().setBlockData(NoteBlockMechanicFactory
                        .createNoteBlockData(noteBlockMechanic.getCustomVariation()), false);
                plugin.debugMessage("Placing Oraxen block for " + grave.getUUID() + " at "
                        + location.getWorld().getName() + ", " + (location.getBlockX() + 0.5) + "x, "
                        + (location.getBlockY() + 0.5) + "Y, " + (location.getBlockZ() + 0.5) + "z", 1);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public boolean isCustomBlock(Location location) {
        if (location.getBlock().getBlockData() instanceof NoteBlock) {
            NoteBlock noteBlock = (NoteBlock) location.getBlock().getBlockData();

            return NoteBlockMechanicFactory.getBlockMechanic((int) (noteBlock
                    .getInstrument().getType()) * 25 + (int) noteBlock.getNote().getId()
                    + (noteBlock.isPowered() ? 400 : 0) - 26) != null;
        }

        return false;
    }

    public void removeBlock(Location location) {
        location.getBlock().setType(Material.AIR);
    }

    public FurnitureMechanic getFurnitureMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("furniture");

        return mechanicFactory != null ? (FurnitureMechanic) mechanicFactory.getMechanic(string) : null;
    }

    public NoteBlockMechanic getNoteBlockMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");

        return mechanicFactory != null ? (NoteBlockMechanic) mechanicFactory.getMechanic(string) : null;
    }
}