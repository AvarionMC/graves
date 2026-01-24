package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.LocationUtil;
import org.avarion.graves.util.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LocationManager {

    private final Graves plugin;

    public LocationManager(Graves plugin) {
        this.plugin = plugin;
    }

    public void setLastSolidLocation(@NotNull Entity entity, Location location) {
        CacheManager.lastLocationMap.put(entity.getUniqueId(), location);
    }

    public @Nullable Location getLastSolidLocation(@NotNull Entity entity) {
        Location location = CacheManager.lastLocationMap.get(entity.getUniqueId());

        return location != null
               && location.getWorld() != null
               && location.getWorld().equals(entity.getWorld())
               && location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid() ? location : null;
    }

    public void removeLastSolidLocation(@NotNull Entity entity) {
        CacheManager.lastLocationMap.remove(entity.getUniqueId());
    }

    public @Nullable Location getSafeTeleportLocation(Entity entity, @NotNull Location location, Grave grave, Graves plugin) {
        if (location.getWorld() != null) {
            if (plugin.getConfigBool("teleport.unsafe", grave) || isLocationSafePlayer(location)) {
                return location;
            }
            else if (plugin.getConfigBool("teleport.top", grave)) {
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
            }
            else {
                if (isVoid(location) || !isInsideBorder(location)) {
                    return getVoid(location, livingEntity, grave);
                }
                else if (MaterialUtil.isLava(block.getType())) {
                    return getLavaTop(location, livingEntity, grave);
                }
                else {
                    // If a grave already exists at this location, try to find a nearby alternative
                    if (hasGrave(location)) {
                        Location nearbyLocation = findNearbyLocation(location, grave);
                        if (nearbyLocation != null) {
                            return nearbyLocation;
                        }
                    }

                    Location graveLocation;
                    if (block.getType().isAir() || MaterialUtil.isWater(block.getType())) {
                        graveLocation = plugin.getConfigBool("placement.ground", grave)
                                        ? getGround(location, livingEntity, grave)
                                        : null;
                    }
                    else {
                        graveLocation = getRoof(location, livingEntity, grave);
                    }

                    if (graveLocation != null) {
                        return graveLocation;
                    }
                }
            }
        }

        return getVoid(location, livingEntity, grave);
    }

    /**
     * Finds a nearby safe location for a grave when the exact location is already occupied.
     * Searches in a spiral pattern around the original location, checking both horizontally
     * and vertically (up/down) for each position.
     */
    private @Nullable Location findNearbyLocation(Location location, Grave grave) {
        if (location.getWorld() == null) {
            return null;
        }

        int maxRadius = plugin.getConfigInt("placement.offset-max-radius", grave, 5);

        // Search in expanding squares around the original location
        for (int radius = 1; radius <= maxRadius; radius++) {
            // Check all positions at this radius
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Only check positions on the edge of the square (not inside)
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    Location candidate = location.clone().add(dx, 0, dz);
                    candidate = LocationUtil.roundLocation(candidate);

                    // Check the same Y level first
                    if (!hasGrave(candidate) && isLocationSafeGrave(candidate)) {
                        return candidate;
                    }

                    // Check one block up
                    Location candidateUp = candidate.clone().add(0, 1, 0);
                    if (!hasGrave(candidateUp) && isLocationSafeGrave(candidateUp)) {
                        return candidateUp;
                    }

                    // Check one block down
                    Location candidateDown = candidate.clone().add(0, -1, 0);
                    if (!hasGrave(candidateDown) && isLocationSafeGrave(candidateDown)) {
                        return candidateDown;
                    }
                }
            }
        }

        return null;
    }

    public Location getTop(Location location, Entity entity, Grave grave) {
        return findLocationDownFromY(location, entity, location.getWorld() != null
                                                       ? location.getWorld().getMaxHeight()
                                                       : location.getBlockY(), grave);
    }

    public Location getRoof(Location location, Entity entity, Grave grave) {
        return findLocationUpFromY(location, entity, location.getBlockY(), grave);
    }

    public Location getGround(Location location, Entity entity, Grave grave) {
        return findLocationDownFromY(location, entity, location.getBlockY(), grave);
    }

    private @Nullable Location findLocationDownFromY(Location location, Entity entity, int y, Grave grave) {
        location = location.clone();
        int counter = 0;

        location.setY(y);

        if (location.getWorld() != null) {
            while (counter <= (getMinHeight(location) * -1) + location.getWorld().getMaxHeight()) {
                if (MaterialUtil.isLava(location.getBlock().getType())) {
                    return getLavaTop(location, entity, grave);
                }
                else if (isLocationSafeGrave(location) && !hasGrave(location)) {
                    return location;
                }

                location.subtract(0, 1, 0);
                counter++;
            }
        }

        return null;
    }

    private @Nullable Location findLocationUpFromY(Location location, Entity entity, int y, Grave grave) {
        location = location.clone();
        int counter = 0;

        location.setY(y);

        if (location.getWorld() != null) {
            while (counter <= (getMinHeight(location) * -1) + location.getWorld().getMaxHeight()) {
                if (MaterialUtil.isLava(location.getBlock().getType())) {
                    return getLavaTop(location, entity, grave);
                }
                else if (isLocationSafeGrave(location) && !hasGrave(location)) {
                    return location;
                }

                location.add(0, 1, 0);
                counter++;
            }
        }

        return null;
    }

    public @Nullable Location getVoid(Location location, Entity entity, Grave grave) {
        if (plugin.getConfigBool("placement.void", grave)) {
            location = location.clone();

            if (plugin.getConfigBool("placement.void-smart", grave)) {
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

    public @Nullable Location getLavaTop(Location location, Entity entity, Grave grave) {
        if (plugin.getConfigBool("placement.lava-smart", grave)) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(entity);

            if (solidLocation != null) {
                return !hasGrave(solidLocation) ? solidLocation : getRoof(solidLocation, entity, grave);
            }
        }

        if (plugin.getConfigBool("placement.lava-top", grave)) {
            location = location.clone();

            if (location.getWorld() != null) {
                int counter = 0;

                while (counter <= location.getWorld().getMaxHeight()) {
                    Block block = location.getBlock();

                    if (block.getType().isAir()
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
        if (livingEntity instanceof Player player) {

            // Check if the player has permission to build
            boolean canBuild = !plugin.getConfigBool("placement.can-build", player, permissionList)
                               || plugin.getCompatibility().canBuild(player, location, plugin);

            // Check if the player can build in a claim using ProtectionLib
            boolean canBuildProtectionLib = !plugin.getIntegrationManager().hasProtectionLib()
                                            || !plugin.getConfigBool("placement.can-build-protectionlib", player, permissionList)
                                            || plugin.getIntegrationManager().getProtectionLib().canBuild(location, player);

            // Check if the player can build in a claim using SimpleClaimSystem
            boolean canBuildSCS = !plugin.getIntegrationManager().hasSimpleClaimSystem()
                                  || !plugin.getConfigBool("placement.can-build-simpleclaimsystem", player, permissionList)
                                  || plugin.getIntegrationManager().getSimpleClaimSystem().canBuild(player, location);

            return canBuild && canBuildProtectionLib && canBuildSCS;
        }

        return true;
    }

    public boolean isLocationSafePlayer(@NotNull Location location) {
        Block block = location.getBlock();

        if (isInsideBorder(location) && !block.getType().isSolid() && !MaterialUtil.isLava(block.getType())) {
            Block blockAbove = block.getRelative(BlockFace.UP);
            Block blockBelow = block.getRelative(BlockFace.DOWN);

            return !block.getType().isSolid()
                   && !MaterialUtil.isLava(blockAbove.getType())
                   && !blockBelow.getType()
                                 .isAir()
                   && !MaterialUtil.isLava(blockBelow.getType());
        }

        return false;
    }

    public boolean isLocationSafeGrave(Location location) {
        location = LocationUtil.roundLocation(location);
        Block block = location.getBlock();

        return isInsideBorder(location)
               && MaterialUtil.isSafeNotSolid(block.getType())
               && MaterialUtil.isSafeSolid(block.getRelative(BlockFace.DOWN).getType());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasGrave(Location location) {
        return plugin.getDataManager().hasChunkData(location) && plugin.getDataManager()
                                                                       .getChunkData(location)
                                                                       .getBlockDataMap()
                                                                       .containsKey(location);
    }

    public boolean isInsideBorder(@NotNull Location location) {
        return (location.getWorld() != null && location.getWorld().getWorldBorder().isInside(location));
    }

    public boolean isVoid(@NotNull Location location) {
        return location.getWorld() != null && (location.getY() < getMinHeight(location)
                                               || location.getY() > location.getWorld().getMaxHeight());
    }

    public int getMinHeight(@NotNull Location location) {
        return location.getWorld() != null ? location.getWorld().getMinHeight() : 0;
    }

}
