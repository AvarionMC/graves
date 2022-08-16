package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public final class LocationManager {
    private final Graves plugin;

    public LocationManager(Graves plugin) {
        this.plugin = plugin;
    }

    public void setLastSolidLocation(Entity entity, Location location) {
        plugin.getCacheManager().getLastLocationMap().put(entity.getUniqueId(), location);
    }

    public Location getLastSolidLocation(Entity entity) {
        Location location = plugin.getCacheManager().getLastLocationMap().get(entity.getUniqueId());

        return location != null && location.getWorld() != null
                && location.getWorld().equals(entity.getWorld())
                && location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid() ? location : null;
    }

    public void removeLastSolidLocation(Entity entity) {
        plugin.getCacheManager().getLastLocationMap().remove(entity.getUniqueId());
    }

    public Location getSafeTeleportLocation(Entity entity, Location location, Grave grave, Graves plugin) {
        if (location.getWorld() != null) {
            if (plugin.getConfig("teleport.unsafe", grave).getBoolean("teleport.unsafe")
                    || isLocationSafePlayer(location)) {
                return location;
            } else if (plugin.getConfig("teleport.top", grave).getBoolean("teleport.top")) {
                Location topLocation = getTop(location, entity, grave);

                if (topLocation != null && isLocationSafePlayer(topLocation) && topLocation.getWorld() != null) {
                    plugin.getEntityManager().sendMessage("message.teleport-top", entity, topLocation, grave);

                    return topLocation;
                }
            }
        }

        return null;
    }

    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave) {
        location = LocationUtil.roundLocation(location);

        if (location.getWorld() != null) {
            Block block = location.getBlock();

            if (!hasGrave(location) && isLocationSafeGrave(location)) {
                return location;
            } else {
                if (isVoid(location) || !isInsideBorder(location)) {
                    return getVoid(location, livingEntity, grave);
                } else if (MaterialUtil.isLava(block.getType())) {
                    return getLavaTop(location, livingEntity, grave);
                } else {
                    Location graveLocation = (MaterialUtil.isAir(block.getType())
                            || MaterialUtil.isWater(block.getType()))
                            ? (plugin.getConfig("placement.ground", grave)
                            .getBoolean("placement.ground") ? getGround(location, livingEntity, grave) : null)
                            : getRoof(location, livingEntity, grave);

                    if (graveLocation != null) {
                        return graveLocation;
                    }
                }
            }
        }

        return getVoid(location, livingEntity, grave);
    }

    public Location getTop(Location location, Entity entity, Grave grave) {
        return findLocationDownFromY(location, entity, location.getWorld() != null
                ? location.getWorld().getMaxHeight() : location.getBlockY(), grave);
    }

    public Location getRoof(Location location, Entity entity, Grave grave) {
        return findLocationUpFromY(location, entity, location.getBlockY(), grave);
    }

    public Location getGround(Location location, Entity entity, Grave grave) {
        return findLocationDownFromY(location, entity, location.getBlockY(), grave);
    }

    private Location findLocationDownFromY(Location location, Entity entity, int y, Grave grave) {
        location = location.clone();
        int counter = 0;

        location.setY(y);

        if (location.getWorld() != null) {
            while (counter <= (getMinHeight(location) * -1) + location.getWorld().getMaxHeight()) {
                if (MaterialUtil.isLava(location.getBlock().getType())) {
                    return getLavaTop(location, entity, grave);
                } else if (isLocationSafeGrave(location) && !hasGrave(location)) {
                    return location;
                }

                location.subtract(0, 1, 0);
                counter++;
            }
        }

        return null;
    }

    private Location findLocationUpFromY(Location location, Entity entity, int y, Grave grave) {
        location = location.clone();
        int counter = 0;

        location.setY(y);

        if (location.getWorld() != null) {
            while (counter <= (getMinHeight(location) * -1) + location.getWorld().getMaxHeight()) {
                if (MaterialUtil.isLava(location.getBlock().getType())) {
                    return getLavaTop(location, entity, grave);
                } else if (isLocationSafeGrave(location) && !hasGrave(location)) {
                    return location;
                }

                location.add(0, 1, 0);
                counter++;
            }
        }

        return null;
    }

    public Location getVoid(Location location, Entity entity, Grave grave) {
        if (plugin.getConfig("placement.void", grave).getBoolean("placement.void")) {
            location = location.clone();

            if (plugin.getConfig("placement.void-smart", grave).getBoolean("placement.void-smart")) {
                Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);

                if (solidLocation != null) {
                    return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
                }
            }

            if (location.getWorld() != null) {
                Location bottomLocation = getRoof(location, entity, grave);

                if (bottomLocation != null) {
                    return bottomLocation;
                }

                location.setY(getMinHeight(location));

                return location;
            }
        }

        return null;
    }

    public Location getLavaTop(Location location, Entity entity, Grave grave) {
        if (plugin.getConfig("placement.lava-smart", grave).getBoolean("placement.lava-smart")) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);

            if (solidLocation != null) {
                return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
            }
        }

        if (plugin.getConfig("placement.lava-top", grave).getBoolean("placement.lava-top")) {
            location = location.clone();

            if (location.getWorld() != null) {
                int counter = 0;

                while (counter <= location.getWorld().getMaxHeight()) {
                    Block block = location.getBlock();

                    if ((MaterialUtil.isAir(block.getType()))
                            && !plugin.getCompatibility().hasTitleData(block)
                            && !MaterialUtil.isLava(block.getType())) {
                        return location;
                    }

                    location.add(0, 1, 0);
                    counter++;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canBuild(LivingEntity livingEntity, Location location, List<String> permissionList) {
        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;

            return (!plugin.getConfig("placement.can-build", player, permissionList)
                    .getBoolean("placement.can-build")
                    || plugin.getCompatibility().canBuild(player, location, plugin))
                    && (!plugin.getIntegrationManager().hasProtectionLib()
                    || (!plugin.getConfig("placement.can-build-protectionlib", player, permissionList)
                    .getBoolean("placement.can-build-protectionlib")
                    || plugin.getIntegrationManager().getProtectionLib().canBuild(location, player)));
        }

        return true;
    }

    public boolean isLocationSafePlayer(Location location) {
        Block block = location.getBlock();

        if (isInsideBorder(location) && !block.getType().isSolid() && !MaterialUtil.isLava(block.getType())) {
            Block blockAbove = block.getRelative(BlockFace.UP);
            Block blockBelow = block.getRelative(BlockFace.DOWN);

            return !block.getType().isSolid() && !MaterialUtil.isLava(blockAbove.getType())
                    && !MaterialUtil.isAir(blockBelow.getType()) && !MaterialUtil.isLava(blockBelow.getType());
        }

        return false;
    }

    public boolean isLocationSafeGrave(Location location) {
        location = LocationUtil.roundLocation(location);
        Block block = location.getBlock();

        return isInsideBorder(location) && MaterialUtil.isSafeNotSolid(block.getType())
                && MaterialUtil.isSafeSolid(block.getRelative(BlockFace.DOWN).getType());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGrave(Location location) {
        return plugin.getDataManager().hasChunkData(location)
                && plugin.getDataManager().getChunkData(location).getBlockDataMap().containsKey(location);
    }

    public boolean isInsideBorder(Location location) {
        return plugin.getVersionManager().is_v1_7() || plugin.getVersionManager().is_v1_8()
                || plugin.getVersionManager().is_v1_9() || plugin.getVersionManager().is_v1_10()
                || plugin.getVersionManager().is_v1_11()
                || (location.getWorld() != null && location.getWorld().getWorldBorder().isInside(location));
    }

    public boolean isVoid(Location location) {
        return location.getWorld() != null && (location.getY() < getMinHeight(location)
                || location.getY() > location.getWorld().getMaxHeight());
    }

    public int getMinHeight(Location location) {
        return location.getWorld() != null && plugin.getVersionManager().hasMinHeight()
                ? location.getWorld().getMinHeight() : 0;
    }
}
