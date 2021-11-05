package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.listener.integration.furniturelib.ProjectBreakListener;
import com.ranull.graves.listener.integration.furniturelib.ProjectClickListener;
import com.ranull.graves.manager.EntityDataManager;
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
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class FurnitureLib extends EntityDataManager {
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

        registerListeners();
        new Furniture(plugin);
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(projectClickListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(projectBreakListener, plugin);
    }

    public void unregisterListeners() {
        if (projectClickListener != null) {
            HandlerList.unregisterAll(projectClickListener);
        }

        if (projectBreakListener != null) {
            HandlerList.unregisterAll(projectBreakListener);
        }
    }

    public boolean canBuild(Location location, Player player) {
        return furnitureLib.getPermManager().useProtectionLib()
                && furnitureLib.getPermManager().canBuild(player, location);
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
                createEntityData(objectID.getStartLocation(), objectID.getUUID(), grave.getUUID(),
                        EntityData.Type.FURNITURELIB);
            } else {
                plugin.debugMessage("Can't find FurnitureLib furniture " + name, 1);
            }
        }
    }

    public void removeFurniture(Grave grave) {
        removeFurniture(getLoadedEntityDataList(grave));
    }

    public void removeFurniture(EntityData entityData) {
        removeFurniture(Collections.singletonList(entityData));
    }

    public void removeFurniture(List<EntityData> entityDataList) {
        List<EntityData> removeEntityDataList = new ArrayList<>();

        for (EntityData entityData : entityDataList) {
            for (ObjectID objectID : furnitureLib.getFurnitureManager().getObjectList()) {
                if (objectID.getUUID() != null && objectID.getUUID().equals(entityData.getUUIDEntity())) {
                    furnitureLib.getFurnitureManager().remove(objectID);
                    removeEntityDataList.add(entityData);
                }
            }
        }

        plugin.getDataManager().removeEntityData(removeEntityDataList);
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

    public class Furniture extends FurniturePlugin {
        public Furniture(Plugin plugin) {
            super(plugin);
            register();
        }

        @Override
        public void registerProjects() {
            String path = "data" + File.separator + "plugin" + File.separator + "furniturelib";

            try {
                new Project("Grave1", getPlugin(), getResource(path + File.separator + "grave_1.dModel"));
                new Project("Grave2", getPlugin(), getResource(path + File.separator + "grave_2.dModel"));
                new Project("Grave3", getPlugin(), getResource(path + File.separator + "grave_3.dModel"));
            } catch (Exception exception) {
                plugin.warningMessage(exception.getMessage());
            }
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
}