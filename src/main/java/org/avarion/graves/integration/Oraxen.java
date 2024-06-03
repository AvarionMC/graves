package org.avarion.graves.integration;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.avarion.graves.Graves;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.listener.integration.oraxen.EntityDamageListener;
import org.avarion.graves.listener.integration.oraxen.HangingBreakListener;
import org.avarion.graves.listener.integration.oraxen.PlayerInteractEntityListener;
import org.avarion.graves.manager.EntityDataManager;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.ResourceUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Oraxen extends EntityDataManager {

    private final Graves plugin;
    private final Plugin oraxenPlugin;
    private final PlayerInteractEntityListener playerInteractEntityListener;
    private final EntityDamageListener entityDamageListener;
    private final HangingBreakListener hangingBreakListener;

    public Oraxen(Graves plugin, Plugin oraxenPlugin) {
        super(plugin);

        this.plugin = plugin;
        this.oraxenPlugin = oraxenPlugin;
        this.playerInteractEntityListener = new PlayerInteractEntityListener(plugin, this);
        this.entityDamageListener = new EntityDamageListener(this);
        this.hangingBreakListener = new HangingBreakListener(this);

        saveData();
        registerListeners();
    }

    public void saveData() {
        if (plugin.getConfig().getBoolean("settings.integration.oraxen.write")) {
            ResourceUtil.copyResources("data/plugin/"
                                       + oraxenPlugin.getName().toLowerCase()
                                       + "/items", plugin.getPluginsFolder()
                                                   + "/"
                                                   + oraxenPlugin.getName()
                                                   + "/items", plugin);
            ResourceUtil.copyResources("data/model/grave.json", plugin.getPluginsFolder()
                                                                + "/"
                                                                + oraxenPlugin.getName()
                                                                + "/pack/assets/minecraft/models/graves/grave.json", plugin);
            plugin.debugMessage("Saving " + oraxenPlugin.getName() + " data.", 1);
        }
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(playerInteractEntityListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(entityDamageListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(hangingBreakListener, plugin);
    }

    public void unregisterListeners() {
        if (playerInteractEntityListener != null) {
            HandlerList.unregisterAll(playerInteractEntityListener);
        }

        if (entityDamageListener != null) {
            HandlerList.unregisterAll(entityDamageListener);
        }

        if (hangingBreakListener != null) {
            HandlerList.unregisterAll(hangingBreakListener);
        }
    }

    public void createFurniture(Location location, Grave grave) {
        if (plugin.getConfigBool("oraxen.furniture.enabled", grave)) {
            try {
                String name = plugin.getConfigString("oraxen.furniture.name", grave, "");
                FurnitureMechanic furnitureMechanic = getFurnitureMechanic(name);

                if (furnitureMechanic != null && location.getWorld() != null) {
                    location.getBlock().setType(Material.AIR);

                    ItemFrame itemFrame = (ItemFrame) furnitureMechanic.place(location, location.getYaw(), BlockFace.UP
                            // , BlockFaceUtil.getBlockFaceRotation(BlockFaceUtil.getYawBlockFace(location.getYaw()))
                    );

                    if (itemFrame != null) {
                        createEntityData(location, itemFrame.getUniqueId(), grave.getUUID(), EntityData.Type.ORAXEN);
                        plugin.debugMessage("Placing Oraxen furniture for "
                                            + grave.getUUID()
                                            + " at "
                                            + location.getWorld().getName()
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
            catch (NoSuchMethodError ignored) {
                plugin.warningMessage("This version of Minecraft does not support "
                                      + oraxenPlugin.getName()
                                      + " furniture");
            }
        }
    }

    public void removeFurniture(Grave grave) {
        removeEntries(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    public void createBlock(Location location, Grave grave) {
        if (plugin.getConfigBool("oraxen.block.enabled", grave)) {
            String name = plugin.getConfigString("oraxen.block.name", grave, "");
            NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(name);

            if (noteBlockMechanic != null && location.getWorld() != null) {
                location.getBlock()
                        .setBlockData(NoteBlockMechanicFactory.createNoteBlockData(noteBlockMechanic.getCustomVariation()), false);
                plugin.debugMessage("Placing Oraxen block for "
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

    @SuppressWarnings("deprecation")
    public boolean isCustomBlock(@NotNull Location location) {
        if (location.getBlock().getBlockData() instanceof NoteBlock noteBlock) {

            return NoteBlockMechanicFactory.getBlockMechanic((int) (noteBlock.getInstrument().getType()) * 25
                                                             + (int) noteBlock.getNote().getId()
                                                             + (noteBlock.isPowered() ? 400 : 0) - 26) != null;
        }

        return false;
    }

    public void removeBlock(@NotNull Location location) {
        location.getBlock().setType(Material.AIR);
    }

    public @Nullable FurnitureMechanic getFurnitureMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("furniture");

        return mechanicFactory != null ? (FurnitureMechanic) mechanicFactory.getMechanic(string) : null;
    }

    public @Nullable NoteBlockMechanic getNoteBlockMechanic(String string) {
        MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");

        return mechanicFactory != null ? (NoteBlockMechanic) mechanicFactory.getMechanic(string) : null;
    }

}
