package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LocationManager {
    private final Graves plugin;
    private final Map<UUID, Location> lastSolidLocationMap;

    public LocationManager(Graves plugin) {
        this.plugin = plugin;
        this.lastSolidLocationMap = new HashMap<>();
    }

    public void setLastSolidLocation(Entity entity, Location location) {
        lastSolidLocationMap.put(entity.getUniqueId(), location);
    }

    public Location getLastSolidLocation(Entity entity) {
        return lastSolidLocationMap.get(entity.getUniqueId());
    }

    public void removeLastSolidLocation(Entity entity) {
        lastSolidLocationMap.remove(entity.getUniqueId());
    }

    public boolean isLocationSafe(Location location) {
        Block block = location.getBlock();

        if (!block.getType().isSolid() && !MaterialUtil.isLava(block.getType())) {
            Block blockAbove = block.getRelative(BlockFace.UP);
            Block blockBelow = block.getRelative(BlockFace.DOWN);

            return !blockAbove.getType().isSolid() && !MaterialUtil.isLava(blockAbove.getType())
                    && !MaterialUtil.isAir(blockBelow.getType()) && !MaterialUtil.isLava(blockBelow.getType());
        }

        return false;
    }

    public Location getSafeTeleportLocation(Player player, Location location, Grave grave, Graves plugin) {
        if (location.getWorld() != null) {
            if (plugin.getConfig("teleport.unsafe", grave).getBoolean("teleport.unsafe")
                    || isLocationSafe(location)) {
                return location;
            } else if (plugin.getConfig("teleport.top", grave).getBoolean("teleport.top")) {
                Location groundLocation = getGround(location, grave);

                if (groundLocation != null && groundLocation.getWorld() != null) {
                    if (groundLocation.getWorld().getEnvironment() != World.Environment.NETHER
                            || plugin.getConfig("teleport.top-nether", grave).getBoolean("teleport.top-nether")) {
                        plugin.getPlayerManager().sendMessage("message.teleport-top", player, groundLocation, grave);

                        return groundLocation;
                    }
                }
            }
        }

        if (player.hasPermission("graves.bypass")) {
            return location;
        }

        return null;
    }

    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave, Graves plugin) {
        location = LocationUtil.roundLocation(location);
        List<String> blockReplaceList = plugin.getConfig("replace.block.material", grave)
                .getStringList("replace.block.material");

        if (location.getWorld() != null) {
            Block block = location.getBlock();
            int minHeight = getMinHeight(location);
            int maxHeight = location.getWorld().getMaxHeight();

            if (location.getY() < minHeight || location.getY() > maxHeight) {
                // Died in void
                if (plugin.getConfig("placement.void", grave).getBoolean("placement.void")) {
                    if (livingEntity instanceof Player
                            && plugin.getConfig("placement.void-smart", grave)
                            .getBoolean("placement.void-smart")) {
                        Location solidLocation = plugin.getLocationManager().getLastSolidLocation(livingEntity);

                        if (solidLocation != null && solidLocation.getBlock().getRelative(BlockFace.DOWN)
                                .getType().isSolid()) {
                            return solidLocation;
                        }
                    }

                    // Calculate block closest to the void
                    return getVoid(location, grave);
                } else {
                    // Don't create in void
                    return null;
                }
            } else if (MaterialUtil.isLava(block.getType())) {
                // Died in lava
                if (livingEntity instanceof Player
                        && plugin.getConfig("placement.lava-smart", grave)
                        .getBoolean("placement.lava-smart")) {
                    Location solidLocation = plugin.getLocationManager().getLastSolidLocation(livingEntity);

                    if (solidLocation != null && solidLocation.getBlock()
                            .getRelative(BlockFace.DOWN).getType().isSolid()) {
                        return solidLocation;
                    }
                }

                if (plugin.getConfig("placement.lava-top", grave).getBoolean("placement.lava-top")) {
                    Location lavaLocation = getLava(location, grave);

                    return lavaLocation != null ? lavaLocation : location;
                }
            } else if (MaterialUtil.isAir(block.getType()) || blockReplaceList.contains(block.getType().name())) {
                // Died in air
                if (plugin.getConfig("placement.ground", grave).getBoolean("placement.ground")) {
                    Location groundLocation = getGround(location, grave);

                    return groundLocation != null ? groundLocation : location;
                } else {
                    return location;
                }
            }
        }

        return location;
    }

    public Location getGround(Location location, Grave grave) {
        location = location.clone();

        if (location.getWorld() != null) {
            boolean notSolid = plugin.getConfig("replace.block.not-solid", grave)
                    .getBoolean("replace.block.not-solid");
            List<String> blockReplaceList = plugin.getConfig("replace.block.material", grave)
                    .getStringList("replace.block.material");
            int totalHeight = (getMinHeight(location) * -1) + location.getWorld().getMaxHeight();
            int counter = 0;

            while (counter <= totalHeight) {
                if ((!notSolid || location.getBlock().getType().isSolid())
                        && !blockReplaceList.contains(location.getBlock().getType().name())
                        && !MaterialUtil.isAir(location.getBlock().getType())
                        && isLocationSafe(location.clone().add(0, 1, 0))) {
                    return location.add(0, 1, 0);
                }

                location.subtract(0, 1, 0);
                counter++;
            }
        }

        return null;
    }

    public Location getVoid(Location location, Grave grave) {
        location = location.clone();

        if (location.getWorld() != null) {
            Location bottomLocation = getBottom(location, grave);

            if (bottomLocation != null) {
                return bottomLocation;
            }

            location.setY(getMinHeight(location));

            return MaterialUtil.isAir(location.getBlock().getType()) ? location : null;
        }

        return null;
    }

    public Location getBottom(Location location, Grave grave) {
        location = location.clone();

        if (location.getWorld() != null) {
            List<String> blockReplaceList = plugin.getConfig("replace.block.material", grave)
                    .getStringList("replace.block.material");
            int minHeight = getMinHeight(location);
            int totalHeight = (minHeight * -1) + location.getWorld().getMaxHeight();
            Block block = location.getBlock();
            int counter = 0;

            location.setY(minHeight);

            while (counter <= totalHeight) {
                Block relativeBlock = block.getRelative(BlockFace.UP);

                if ((blockReplaceList.contains(block.getType().name())
                        || !MaterialUtil.isAir(block.getType()))
                        && (blockReplaceList.contains(relativeBlock.getType().name())
                        || MaterialUtil.isAir(relativeBlock.getType()))) {
                    return block.getLocation().add(0, 1, 0);
                }

                block = block.getLocation().add(0, 1, 0).getBlock();
                counter++;
            }
        }

        return null;
    }

    public Location getLava(Location location, Grave grave) {
        location = location.clone();

        if (location.getWorld() != null) {
            List<String> blockReplaceList = plugin.getConfig("replace.block.material", grave)
                    .getStringList("replace.block.material");
            int counter = 0;

            while (counter <= location.getWorld().getMaxHeight()) {
                if ((blockReplaceList.contains(location.getBlock().getType().name())
                        || MaterialUtil.isAir(location.getBlock().getType()))
                        && !MaterialUtil.isLava(location.getBlock().getType())) {
                    return location;
                }

                location.add(0, 1, 0);
                counter++;
            }
        }

        return null;
    }

    public int getMinHeight(Location location) {
        return location.getWorld() != null && plugin.getVersionManager().hasMinHeight()
                ? location.getWorld().getMinHeight() : 0;
    }
}
