package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class HologramManager extends EntityDataManager {
    private final Graves plugin;

    public HologramManager(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
    }

    public void createHologram(Location location, Grave grave) {
        if (!plugin.getVersionManager().is_v1_7()
                && plugin.getConfig("hologram.enabled", grave).getBoolean("hologram.enabled")) {
            double offsetX = plugin.getConfig("hologram.offset.x", grave).getDouble("hologram.offset.x");
            double offsetY = plugin.getConfig("hologram.offset.y", grave).getDouble("hologram.offset.y");
            double offsetZ = plugin.getConfig("hologram.offset.z", grave).getDouble("hologram.offset.z");
            boolean marker = plugin.getConfig("hologram.marker", grave).getBoolean("hologram.marker");
            location = LocationUtil.roundLocation(location)
                    .add(offsetX + 0.5, offsetY + (marker ? 0.49 : -0.49), offsetZ + 0.5);
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

                    armorStand.setVisible(false);
                    armorStand.setGravity(false);
                    armorStand.setCustomNameVisible(true);
                    armorStand.setSmall(true);
                    armorStand.setCustomName(StringUtil.parseString(line, location, grave, plugin));

                    if (!plugin.getVersionManager().is_v1_7()) {
                        try {
                            armorStand.setMarker(marker);
                        } catch (NoSuchMethodError ignored) {
                        }
                    }

                    if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                        armorStand.setInvulnerable(true);
                    }

                    if (plugin.getVersionManager().hasScoreboardTags()) {
                        armorStand.getScoreboardTags().add("graveHologram");
                        armorStand.getScoreboardTags().add("graveHologramGraveUUID:" + grave.getUUID());
                    }

                    HologramData hologramData = new HologramData(location, armorStand.getUniqueId(),
                            grave.getUUID(), lineNumber);

                    plugin.getDataManager().addHologramData(hologramData);
                    lineNumber++;

                    if (plugin.getIntegrationManager().hasMultiPaper()) {
                        plugin.getIntegrationManager().getMultiPaper().notifyHologramCreation(hologramData);
                    }
                }
            }
        }
    }

    public void removeHologram(Grave grave) {
        removeHologram(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    public void removeHologram(EntityData entityData) {
        removeHologram(getEntityDataMap(Collections.singletonList(entityData)));
    }

    public void removeHologram(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            entry.getValue().remove();
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }
}
