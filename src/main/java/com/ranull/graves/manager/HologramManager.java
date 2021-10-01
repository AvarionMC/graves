package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class HologramManager {
    private final Graves plugin;

    public HologramManager(Graves plugin) {
        this.plugin = plugin;
    }

    public HologramData getHologramData(Entity entity) {
        if (plugin.getDataManager().hasChunkData(entity.getLocation())) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(entity.getLocation());

            if (chunkData.getHologramDataMap().containsKey(entity.getUniqueId())) {
                return chunkData.getHologramDataMap().get(entity.getUniqueId());
            }
        }

        return null;
    }

    public Grave getGraveFromHologram(Entity entity) {
        HologramData hologramData = getHologramData(entity);

        return hologramData != null && plugin.getDataManager().getGraveMap().containsKey(hologramData.getUUIDGrave())
                ? plugin.getDataManager().getGraveMap().get(hologramData.getUUIDGrave()) : null;
    }

    public void createHologram(Location location, Grave grave) {
        if (!plugin.getVersionManager().is_v1_7() && location.getWorld() != null
                && plugin.getConfig("hologram.enabled", grave).getBoolean("hologram.enabled")) {
            double offsetX = plugin.getConfig("hologram.offset.x", grave).getDouble("hologram.offset.x");
            double offsetY = plugin.getConfig("hologram.offset.y", grave).getDouble("hologram.offset.y");
            double offsetZ = plugin.getConfig("hologram.offset.z", grave).getDouble("hologram.offset.z");
            location = LocationUtil.roundLocation(location).add(offsetX + 0.5, offsetY - 1.5, offsetZ + 0.5);
            List<String> lineList = plugin.getConfig("hologram.line", grave)
                    .getStringList("hologram.line");
            double lineHeight = plugin.getConfig("hologram.height-line", grave)
                    .getDouble("hologram.height-line");
            int lineNumber = 0;

            Collections.reverse(lineList);

            for (String line : lineList) {
                location.add(0, lineHeight, 0);

                if (location.getWorld() != null) {
                    ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
                    plugin.getDataManager().addHologram(new HologramData(location, armorStand.getUniqueId(),
                            grave.getUUID(), lineNumber));

                    if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                        armorStand.setInvulnerable(true);
                    }

                    armorStand.setGravity(false);
                    armorStand.setVisible(false);
                    armorStand.setCustomNameVisible(true);
                    armorStand.setCustomName(StringUtil.parseString(line, location, grave, plugin));

                    lineNumber++;
                }
            }
        }
    }

    public void removeHolograms(Grave grave) {
        List<HologramData> hologramDataList = new ArrayList<>();

        for (Map.Entry<String, ChunkData> chunkDataEntry : plugin.getDataManager().getChunkDataMap().entrySet()) {
            ChunkData chunkData = chunkDataEntry.getValue();

            if (chunkDataEntry.getValue().isLoaded()) {
                for (HologramData hologramData : new ArrayList<>(chunkData.getHologramDataMap().values())) {
                    if (grave.getUUID().equals(hologramData.getUUIDGrave())) {
                        hologramDataList.add(hologramData);
                    }
                }
            }
        }

        removeHologram(hologramDataList);
    }

    public void removeHologram(HologramData hologramData) {
        removeHologram(Collections.singletonList(hologramData));
    }

    public void removeHologram(List<HologramData> hologramDataList) {
        if (!hologramDataList.isEmpty()) {
            for (HologramData hologramData : hologramDataList) {
                for (Entity entity : hologramData.getLocation().getChunk().getEntities()) {
                    if (entity.getUniqueId().equals(hologramData.getUUIDEntity())) {
                        entity.remove();
                    }
                }

            }

            plugin.getDataManager().removeHologram(hologramDataList);
        }
    }
}
