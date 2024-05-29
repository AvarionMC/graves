package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.data.HologramData;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.LocationUtil;
import org.avarion.graves.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.Collections;
import java.util.List;

public final class HologramManager extends EntityDataManager {

    private final Graves plugin;

    public HologramManager(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
    }

    public void createHologram(Location location, Grave grave) {
        if (plugin.getConfigBool("hologram.enabled", grave)) {
            double offsetX = plugin.getConfigDbl("hologram.offset.x", grave);
            double offsetY = plugin.getConfigDbl("hologram.offset.y", grave);
            double offsetZ = plugin.getConfigDbl("hologram.offset.z", grave);
            boolean marker = plugin.getConfigBool("hologram.marker", grave);
            location = LocationUtil.roundLocation(location)
                                   .add(offsetX + 0.5, offsetY + (marker ? 0.49 : -0.49), offsetZ + 0.5);
            List<String> lineList = plugin.getConfigStringList("hologram.line", grave);
            double lineHeight = plugin.getConfigDbl("hologram.height-line", grave);
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

                    try {
                        armorStand.setMarker(marker);
                    }
                    catch (NoSuchMethodError ignored) {
                    }

                    armorStand.setInvulnerable(true);
                    armorStand.getScoreboardTags().add("graveHologram");
                    armorStand.getScoreboardTags().add("graveHologramGraveUUID:" + grave.getUUID());

                    HologramData hologramData = new HologramData(location, armorStand.getUniqueId(), grave.getUUID(), lineNumber);

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
        removeEntries(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    public void removeHologram(EntityData entityData) {
        removeEntries(getEntityDataMap(Collections.singletonList(entityData)));
    }

}
