package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.integration.FurnitureLibData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.listener.integration.furniturelib.ProjectBreakListener;
import com.ranull.graves.listener.integration.furniturelib.ProjectClickListener;
import com.ranull.graves.util.StringUtil;
import de.Ste3et_C0st.FurnitureLib.Crafting.Project;
import de.Ste3et_C0st.FurnitureLib.Utilitis.LocationUtil;
import de.Ste3et_C0st.FurnitureLib.main.FurniturePlugin;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.*;

public final class FurnitureLib extends FurniturePlugin {
    private final Graves plugin;
    private final de.Ste3et_C0st.FurnitureLib.main.FurnitureLib furnitureLib;
    private final ProjectClickListener projectClickListener;
    private final ProjectBreakListener projectBreakListener;

    public FurnitureLib(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
        this.furnitureLib = de.Ste3et_C0st.FurnitureLib.main.FurnitureLib.getInstance();
        this.projectClickListener = new ProjectClickListener(plugin, this);
        this.projectBreakListener = new ProjectBreakListener(this);

        plugin.getServer().getPluginManager().registerEvents(projectClickListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(projectBreakListener, plugin);
    }

    public boolean canBuild(Location location, Player player) {
        return furnitureLib.getPermManager().useProtectionLib()
                && furnitureLib.getPermManager().canBuild(player, location);
    }

    public FurnitureLibData getFurnitureLibData(Location location, UUID uuid) {
        if (plugin.getDataManager().hasChunkData(location)) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(location);

            if (chunkData.getFurnitureLibMap().containsKey(uuid)) {
                return chunkData.getFurnitureLibMap().get(uuid);
            }
        }

        return null;
    }

    public Grave getGraveFromFurnitureLib(Location location, UUID uuid) {
        FurnitureLibData furnitureLibData = getFurnitureLibData(location, uuid);

        return furnitureLibData != null && plugin.getDataManager().getGraveMap()
                .containsKey(furnitureLibData.getUUIDGrave())
                ? plugin.getDataManager().getGraveMap().get(furnitureLibData.getUUIDGrave()) : null;
    }

    public void unregister() {
        if (projectClickListener != null) {
            HandlerList.unregisterAll(projectClickListener);
        }

        if (projectBreakListener != null) {
            HandlerList.unregisterAll(projectBreakListener);
        }
    }

    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfig("furniturelib.enabled", grave)
                .getBoolean("furniturelib.enabled")) {
            String name = plugin.getConfig("furniturelib.name", grave)
                    .getString("furniturelib.name", "");
            Project project = furnitureLib.getFurnitureManager().getProject(name);

            if (project != null && project.haveModelSchematic()) {
                ObjectID objectID = new ObjectID(project.getName(), project.getPlugin().getName(), location);

                location.setYaw(furnitureLib.getLocationUtil().FaceToYaw(LocationUtil.yawToFace(grave.getYaw())
                        .getOppositeFace()));
                furnitureLib.spawn(project, objectID);
                objectID.setUUID(UUID.randomUUID());
                objectID.getBlockList().stream()
                        .filter(blockLocation -> blockLocation.getBlock().getType().name().contains("SIGN"))
                        .forEach((signLocation) -> setSign(signLocation.getBlock(),
                                plugin.getConfig("furniturelib.line", grave)
                                        .getStringList("furniturelib.line"), grave));
                furnitureLib.getFurnitureManager().addObjectID(objectID);
                plugin.getDataManager().addFurnitureLibData(new FurnitureLibData(objectID.getStartLocation(),
                        objectID.getUUID(), grave.getUUID()));
            } else {
                plugin.debugMessage("Can't find FurnitureLib furniture " + name, 1);
            }
        }
    }

    public void removeFurniture(Grave grave) {
        List<FurnitureLibData> furnitureLibDataList = new ArrayList<>();

        for (Map.Entry<String, ChunkData> chunkDataEntry : plugin.getDataManager().getChunkDataMap().entrySet()) {
            ChunkData chunkData = chunkDataEntry.getValue();

            if (chunkDataEntry.getValue().isLoaded()) {
                for (FurnitureLibData furnitureLibData : new ArrayList<>(chunkData.getFurnitureLibMap().values())) {
                    if (grave.getUUID().equals(furnitureLibData.getUUIDGrave())) {
                        furnitureLibDataList.add(furnitureLibData);
                    }
                }
            }
        }

        removeFurniture(furnitureLibDataList);
    }

    public void removeFurniture(FurnitureLibData furnitureLibData) {
        removeFurniture(Collections.singletonList(furnitureLibData));
    }

    public void removeFurniture(List<FurnitureLibData> furnitureLibDataList) {
        List<FurnitureLibData> removedFurnitureDataList = new ArrayList<>();

        if (!furnitureLibDataList.isEmpty()) {
            for (FurnitureLibData furnitureLibData : furnitureLibDataList) {
                for (ObjectID objectID : furnitureLib.getFurnitureManager().getObjectList()) {
                    if (objectID.getUUID() != null && objectID.getUUID().equals(furnitureLibData.getUUIDEntity())) {
                        furnitureLib.getFurnitureManager().remove(objectID);
                        removedFurnitureDataList.add(furnitureLibData);
                    }
                }
            }

            plugin.getDataManager().removeFurnitureLibData(removedFurnitureDataList);
        }
    }

    private void setSign(Block block, List<String> stringList, Grave grave) {
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            int counter = 0;

            for (String string : stringList) {
                if (counter <= 4) {
                    sign.setLine(counter, StringUtil.parseString(string, block.getLocation(), grave, plugin));
                    counter++;
                } else {
                    break;
                }
            }

            sign.update(true, false);
        }
    }

    @Override
    public void registerProjects() {
        /*
        try {
            new Project("Graves_Grave", getPlugin(),
                    getResource("models" + File.separator + "grave.dModel"));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
         */
    }

    @Override
    public void applyPluginFunctions() {
        furnitureLib.getFurnitureManager().getProjects().stream().filter(project -> project.getPlugin().getName()
                .equals(getPlugin().getDescription().getName())).forEach(Project::applyFunction);
    }

    @Override
    public void onFurnitureLateSpawn(ObjectID objectID) {
    }
}