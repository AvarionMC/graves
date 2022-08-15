package com.ranull.graves.integration;

import com.github.puregero.multilib.MultiLib;
import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.Base64Util;
import com.ranull.graves.util.StringUtil;
import com.ranull.graves.util.UUIDUtil;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class MultiPaper {
    private final Graves plugin;

    public MultiPaper(Graves plugin) {
        this.plugin = plugin;

        registerListeners();
    }

    public String getLocalServerName() {
        return MultiLib.getLocalServerName();
    }

    public void notifyGraveCreation(Grave grave) {
        MultiLib.notify("graves:grave_create", Base64Util.objectToBase64(grave) + "|"
                + Base64Util.objectToBase64(grave.getInventoryItemStack()));
    }

    public void notifyGraveRemoval(Grave grave) {
        MultiLib.notify("graves:grave_remove", grave.getUUID().toString());
    }

    public void notifyBlockCreation(BlockData blockData) {
        MultiLib.notify("graves:block_create", Base64Util.objectToBase64(blockData));
    }

    public void notifyHologramCreation(HologramData hologramData) {
        MultiLib.notify("graves:hologram_create", Base64Util.objectToBase64(hologramData));
    }

    public void notifyEntityCreation(EntityData entityData) {
        MultiLib.notify("graves:entity_create", Base64Util.objectToBase64(entityData));
    }

    @SuppressWarnings("unchecked")
    private void registerListeners() {
        MultiLib.onString(plugin, "graves:grave_create", data -> {
            String[] dataSplit = data.split("\\|");
            Grave grave = (Grave) Base64Util.base64ToObject(dataSplit[0]);
            List<ItemStack> itemStackList = (List<ItemStack>) Base64Util.base64ToObject(dataSplit[1]);

            if (grave != null && itemStackList != null) {
                String title = StringUtil.parseString(plugin.getConfig("gui.grave.title", grave)
                        .getString("gui.grave.title"), grave.getLocationDeath(), grave, plugin);
                Grave.StorageMode storageMode = plugin.getGraveManager()
                        .getStorageMode(plugin.getConfig("storage.mode", grave).getString("storage.mode"));

                grave.setInventory(plugin.getGraveManager().createGraveInventory(grave, grave.getLocationDeath(),
                        itemStackList, title, storageMode));
                plugin.getDataManager().addGrave(grave);
                plugin.debugMessage("MultiLib, importing grave " + grave.getUUID(), 2);
            } else {
                plugin.debugMessage("MultiLib, ERROR grave_create is malformed ", 2);
            }
        });

        MultiLib.onString(plugin, "graves:block_create", data -> {
            BlockData blockData = (BlockData) Base64Util.base64ToObject(data);

            if (blockData != null) {
                plugin.getDataManager().addBlockData(blockData);
                plugin.debugMessage("MultiLib, importing block for grave " + blockData.getGraveUUID().toString(), 2);
            } else {
                plugin.debugMessage("MultiLib, ERROR block_create is malformed ", 2);
            }
        });

        MultiLib.onString(plugin, "graves:hologram_create", data -> {
            HologramData hologramData = (HologramData) Base64Util.base64ToObject(data);

            if (hologramData != null) {
                plugin.getDataManager().addHologramData(hologramData);
                plugin.debugMessage("MultiLib, importing hologram for grave "
                        + hologramData.getUUIDGrave().toString(), 2);
            } else {
                plugin.debugMessage("MultiLib, ERROR hologram_create is malformed ", 2);
            }
        });

        MultiLib.onString(plugin, "graves:entity_create", data -> {
            EntityData entityData = (EntityData) Base64Util.base64ToObject(data);

            if (entityData != null) {
                plugin.getDataManager().addEntityData(entityData);
                plugin.debugMessage("MultiLib, importing entity for grave " + entityData.getUUIDGrave().toString(), 2);
            } else {
                plugin.debugMessage("MultiLib, ERROR entity_create is malformed ", 2);
            }
        });

        MultiLib.onString(plugin, "graves:grave_remove", data -> {
            UUID uuid = UUIDUtil.getUUID(data);

            if (uuid != null) {
                if (plugin.getCacheManager().getGraveMap().containsKey(uuid)) {
                    plugin.getDataManager().removeGrave(uuid);
                    plugin.debugMessage("MultiLib, removing grave " + uuid, 2);
                }
            } else {
                plugin.debugMessage("MultiLib, ERROR grave_remove is malformed ", 2);
            }
        });
    }
}