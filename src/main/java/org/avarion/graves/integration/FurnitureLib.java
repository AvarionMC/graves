package org.avarion.graves.integration;


import de.Ste3et_C0st.FurnitureLib.Crafting.Project;
import de.Ste3et_C0st.FurnitureLib.Utilitis.LocationUtil;
import de.Ste3et_C0st.FurnitureLib.main.FurniturePlugin;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.entity.fEntity;
import de.Ste3et_C0st.FurnitureLib.main.entity.fInventory;
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
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FurnitureLib extends EntityDataManager {

    private final Graves plugin;
    private final de.Ste3et_C0st.FurnitureLib.main.FurnitureLib libInstance;
    private final ProjectClickListener projectClickListener;
    private final ProjectBreakListener projectBreakListener;

    public FurnitureLib(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
        this.libInstance = de.Ste3et_C0st.FurnitureLib.main.FurnitureLib.getInstance();
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
        return libInstance.getPermManager().useProtectionLib() && libInstance.getPermManager()
                                                                             .canBuild(player, location);
    }

    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfigBool("furniturelib.enabled", grave)) {
            String name = plugin.getConfigString("furniturelib.name", grave, "");
            Project project = libInstance.getFurnitureManager().getProject(name);

            if (project != null && project.haveModelSchematic()) {
                location.getBlock().setType(Material.AIR);

                ObjectID objectID = new ObjectID(project.getName(), project.getPlugin().getName(), location);

                location.setYaw(libInstance.getLocationUtil()
                                           .FaceToYaw(LocationUtil.yawToFace(location.getYaw()).getOppositeFace()));
                libInstance.spawn(project, objectID);
                objectID.setUUID(UUID.randomUUID());
                objectID.getBlockList()
                        .stream()
                        .filter(signLocation -> signLocation.getBlock().getType().name().contains("SIGN"))
                        .forEach((signLocation) -> setSign(signLocation.getBlock(), plugin.getConfigStringList("furniturelib.line", grave), grave));

                if (plugin.getConfigBool("furniturelib.head.replace", grave)) {
                    objectID.getPacketList().forEach(fEntity -> setSkull(fEntity, grave));
                }

                libInstance.getFurnitureManager().addObjectID(objectID);
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

    public void removeFurniture(@NotNull List<EntityData> entityDataList) {
        List<EntityData> removeEntityDataList = new ArrayList<>();

        for (EntityData entityData : entityDataList) {
            for (ObjectID objectID : libInstance.getFurnitureManager().getObjectList()) {
                if (objectID.getUUID() != null && objectID.getUUID().equals(entityData.getUUIDEntity())) {
                    libInstance.getFurnitureManager().remove(objectID);
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
                    sign.getSide(Side.FRONT).setLine(counter, StringUtil.parseString(string, block.getLocation(), grave, plugin));
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

        fInventory inventory = new fInventory(fEntity.getEntityID());

        if (inventory.getItemInMainHand() != null
            && materialList.contains(inventory.getItemInMainHand().getType().name())
            && isSkullTextureBlank(inventory.getItemInMainHand())) {
            inventory.setItemInMainHand(itemStack);
        }

        if (inventory.getItemInOffHand() != null
            && materialList.contains(inventory.getItemInOffHand().getType().name())
            && isSkullTextureBlank(inventory.getItemInOffHand())) {
            inventory.setItemInOffHand(itemStack);
        }

        if (inventory.getHelmet() != null
            && materialList.contains(inventory.getHelmet().getType().name())
            && isSkullTextureBlank(inventory.getHelmet())) {
            inventory.setHelmet(itemStack);
        }

        if (inventory.getChestPlate() != null
            && materialList.contains(inventory.getChestPlate().getType().name())
            && isSkullTextureBlank(inventory.getChestPlate())) {
            inventory.setChestPlate(itemStack);
        }

        if (inventory.getLeggings() != null
            && materialList.contains(inventory.getLeggings().getType().name())
            && isSkullTextureBlank(inventory.getLeggings())) {
            inventory.setLeggings(itemStack);
        }

        if (inventory.getBoots() != null
            && materialList.contains(inventory.getBoots().getType().name())
            && isSkullTextureBlank(inventory.getBoots())) {
            inventory.setBoots(itemStack);
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
            libInstance.getFurnitureManager()
                       .getProjects()
                       .stream()
                       .filter(project -> project.getPlugin().getName().equals(getPlugin().getDescription().getName()))
                       .forEach(Project::applyFunction);
        }

        @Override
        public void onFurnitureLateSpawn(ObjectID objectID) {
            // We shouldn't do anything here. Not even call the super method! That one shouldn't do anything anymore now.
        }

    }

}
