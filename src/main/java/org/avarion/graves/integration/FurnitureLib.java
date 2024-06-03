package org.avarion.graves.integration;

import de.Ste3et_C0st.FurnitureLib.Crafting.Project;
import de.Ste3et_C0st.FurnitureLib.Utilitis.LocationUtil;
import de.Ste3et_C0st.FurnitureLib.main.FurniturePlugin;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.entity.fEntity;
import org.avarion.graves.Graves;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.listener.integration.furniturelib.ProjectBreakListener;
import org.avarion.graves.listener.integration.furniturelib.ProjectClickListener;
import org.avarion.graves.manager.EntityDataManager;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

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
        return furnitureLib.getPermManager().useProtectionLib() && furnitureLib.getPermManager()
                                                                               .canBuild(player, location);
    }

    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfigBool("furniturelib.enabled", grave)) {
            String name = plugin.getConfigString("furniturelib.name", grave, "");
            Project project = furnitureLib.getFurnitureManager().getProject(name);

            if (project != null && project.haveModelSchematic()) {
                location.getBlock().setType(Material.AIR);

                ObjectID objectID = new ObjectID(project.getName(), project.getPlugin().getName(), location);

                location.setYaw(furnitureLib.getLocationUtil()
                                            .FaceToYaw(LocationUtil.yawToFace(location.getYaw()).getOppositeFace()));
                furnitureLib.spawn(project, objectID);
                objectID.setUUID(UUID.randomUUID());
                objectID.getBlockList()
                        .stream()
                        .filter(signLocation -> signLocation.getBlock().getType().name().contains("SIGN"))
                        .forEach((signLocation) -> setSign(signLocation.getBlock(), plugin.getConfigStringList("furniturelib.line", grave), grave));

                if (plugin.getConfigBool("furniturelib.head.replace", grave)) {
                    objectID.getPacketList().forEach((fEntity) -> setSkull(fEntity, grave));
                }

                furnitureLib.getFurnitureManager().addObjectID(objectID);
                createEntityData(objectID.getStartLocation(), objectID.getUUID(), grave.getUUID(), EntityData.Type.FURNITURELIB);
            }
            else {
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

    public void removeFurniture(@NotNull List<EntityData> entityDataList) {
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

    private void setSign(@NotNull Block block, List<String> stringList, Grave grave) {
        if (block.getState() instanceof Sign sign) {
            int counter = 0;

            for (String string : stringList) {
                if (counter <= 4) {
                    sign.setLine(counter, StringUtil.parseString(string, block.getLocation(), grave, plugin));
                    counter++;
                }
                else {
                    break;
                }
            }

            sign.update(true, false);
        }
    }

    private void setSkull(@NotNull fEntity fEntity, Grave grave) {
        List<String> materialList = plugin.getConfigStringList("furniturelib.head.material", grave);
        ItemStack itemStack = plugin.getCompatibility().getSkullItemStack(grave, plugin);

        if (fEntity.getItemInMainHand() != null
            && materialList.contains(fEntity.getItemInMainHand().getType().name())
            && isSkullTextureBlank(fEntity.getItemInMainHand())) {
            fEntity.setItemInMainHand(itemStack);
        }

        if (fEntity.getItemInOffHand() != null
            && materialList.contains(fEntity.getItemInOffHand().getType().name())
            && isSkullTextureBlank(fEntity.getItemInOffHand())) {
            fEntity.setItemInOffHand(itemStack);
        }

        if (fEntity.getHelmet() != null
            && materialList.contains(fEntity.getHelmet().getType().name())
            && isSkullTextureBlank(fEntity.getHelmet())) {
            fEntity.setHelmet(itemStack);
        }

        if (fEntity.getChestPlate() != null
            && materialList.contains(fEntity.getChestPlate().getType().name())
            && isSkullTextureBlank(fEntity.getChestPlate())) {
            fEntity.setChestPlate(itemStack);
        }

        if (fEntity.getLeggings() != null
            && materialList.contains(fEntity.getLeggings().getType().name())
            && isSkullTextureBlank(fEntity.getLeggings())) {
            fEntity.setLeggings(itemStack);
        }

        if (fEntity.getBoots() != null
            && materialList.contains(fEntity.getBoots().getType().name())
            && isSkullTextureBlank(fEntity.getBoots())) {
            fEntity.setBoots(itemStack);
        }
    }

    private boolean isSkullTextureBlank(ItemStack itemStack) {
        return plugin.getCompatibility().getSkullTexture(itemStack) == null;
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
                new Project("Grave1", getPlugin(), getResource(path + File.separator + "Grave1.dModel"));
                new Project("Grave2", getPlugin(), getResource(path + File.separator + "Grave2.dModel"));
                new Project("Grave3", getPlugin(), getResource(path + File.separator + "Grave3.dModel"));
                new Project("Skull1", getPlugin(), getResource(path + File.separator + "Skull1.dModel"));
            }
            catch (Exception exception) {
                plugin.warningMessage(exception.getMessage());
            }
        }

        @Override
        public void applyPluginFunctions() {
            furnitureLib.getFurnitureManager()
                        .getProjects()
                        .stream()
                        .filter(project -> project.getPlugin().getName().equals(getPlugin().getDescription().getName()))
                        .forEach(Project::applyFunction);
        }

        @Override
        public void onFurnitureLateSpawn(ObjectID objectID) {
        }

    }

}
