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

    public boolean isLocationSafePlayer(Location location) {
        Block block = location.getBlock();

        if (!block.getType().isSolid() && !MaterialUtil.isLava(block.getType())) {
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

        if (!block.getType().isSolid() && !MaterialUtil.isLava(block.getType())) {
            Block blockBelow = block.getRelative(BlockFace.DOWN);

            return blockBelow.getType().isSolid() && !MaterialUtil.isLava(blockBelow.getType());
        }

        return false;
    }

    public Location getSafeTeleportLocation(Player player, Location location, Grave grave, Graves plugin) {
        if (location.getWorld() != null) {
            if (plugin.getConfig("teleport.unsafe", grave).getBoolean("teleport.unsafe")
                    || isLocationSafePlayer(location)) {
                return location;
            } else if (plugin.getConfig("teleport.top", grave).getBoolean("teleport.top")) {
                Location topLocation = getTop(location, player, grave);

                if (topLocation != null && topLocation.getWorld() != null) {
                    if (topLocation.getWorld().getEnvironment() != World.Environment.NETHER
                            || plugin.getConfig("teleport.top-nether", grave).getBoolean("teleport.top-nether")) {
                        plugin.getPlayerManager().sendMessage("message.teleport-top", player, topLocation, grave);

                        return topLocation;
                    }
                }
            }
        }

        if (player.hasPermission("graves.bypass")) {
            return location;
        }

        return null;
    }

    public Location getSafeGraveLocation(LivingEntity livingEntity, Location location, Grave grave) {
        location = LocationUtil.roundLocation(location);
        boolean hasGrave = plugin.getDataManager().hasChunkData(location)
                && plugin.getDataManager().getChunkData(location).getBlockDataMap().containsKey(location);

        if (!hasGrave && isLocationSafeGrave(location)) {
            return location;
        } else if (location.getWorld() != null) {
            Block block = location.getBlock();
            int minHeight = getMinHeight(location);
            int maxHeight = location.getWorld().getMaxHeight();

            if (location.getY() < minHeight || location.getY() > maxHeight) { // Void
                return getVoid(location, livingEntity, grave);
            } else if (MaterialUtil.isLava(block.getType())) {
                return getLavaTop(location, livingEntity, grave); // Lava
            } else if (!hasGrave && MaterialUtil.isAir(block.getType())
                    || plugin.getConfig("replace.block.material", grave)
                    .getStringList("replace.block.material").contains(block.getType().name())) {
                return getGround(location, livingEntity, grave); // Air
            } else {
                return getRoof(location, livingEntity, grave); // Roof
            }
        }

        return null;
    }

    public Location getGround(Location location, LivingEntity livingEntity, Grave grave) {
        return findLocationDownFromY(location, livingEntity, location.getBlockY(), grave);
    }

    public Location getTop(Location location, LivingEntity livingEntity, Grave grave) {
        return findLocationDownFromY(location, livingEntity, location.getWorld() != null
                ? location.getWorld().getMaxHeight() : location.getBlockY(), grave);
    }

    private Location findLocationDownFromY(Location location, LivingEntity livingEntity, int y, Grave grave) {
        if (location.getWorld() != null && plugin.getConfig("placement.ground", grave)
                .getBoolean("placement.ground")) {
            location = location.clone();
            boolean notSolid = plugin.getConfig("replace.block.not-solid", grave)
                    .getBoolean("replace.block.not-solid");
            List<String> blockReplaceList = plugin.getConfig("replace.block.material", grave)
                    .getStringList("replace.block.material");
            int totalHeight = (getMinHeight(location) * -1) + location.getWorld().getMaxHeight();
            int counter = 0;

            location.setY(y);

            while (counter <= totalHeight) {
                Block block = location.getBlock();

                if ((plugin.getDataManager().hasChunkData(location)
                        && plugin.getDataManager().getChunkData(location).getBlockDataMap().containsKey(location))) {
                    return location.add(0, 1, 0);
                } else if (MaterialUtil.isLava(block.getType())) {
                    return getLavaTop(location, livingEntity, grave);
                } else if ((!notSolid || block.getType().isSolid()) && !blockReplaceList.contains(block.getType().name())
                        && !MaterialUtil.isAir(block.getType())
                        && isLocationSafeGrave(location.clone().add(0, 1, 0))) {
                    return location.add(0, 1, 0);
                }

                location.subtract(0, 1, 0);
                counter++;
            }
        }

        return null;
    }

    public Location getRoof(Location location, LivingEntity livingEntity, Grave grave) {
        if (location.getWorld() != null && plugin.getConfig("placement.ground", grave)
                .getBoolean("placement.ground")) {
            location = location.clone();
            boolean notSolid = plugin.getConfig("replace.block.not-solid", grave)
                    .getBoolean("replace.block.not-solid");
            List<String> blockReplaceList = plugin.getConfig("replace.block.material", grave)
                    .getStringList("replace.block.material");
            int totalHeight = (getMinHeight(location) * -1) + location.getWorld().getMaxHeight();
            int counter = 0;

            while (counter <= totalHeight) {
                Block block = location.getBlock();

                if ((plugin.getDataManager().hasChunkData(location)
                        && plugin.getDataManager().getChunkData(location).getBlockDataMap().containsKey(location))) {
                    return location.add(0, 1, 0);
                } else if (MaterialUtil.isLava(block.getType())) {
                    return getLavaTop(location, livingEntity, grave);
                } else if ((blockReplaceList.contains(block.getType().name()) || MaterialUtil.isAir(block.getType()))
                        && isLocationSafeGrave(location.clone().subtract(0, 1, 0))) {
                    return location.subtract(0, 1, 0);
                }

                location.add(0, 1, 0);
                counter++;
            }
        }

        return null;
    }

    public Location getVoid(Location location, LivingEntity livingEntity, Grave grave) {
        if (plugin.getConfig("placement.void", grave).getBoolean("placement.void")) {
            if (livingEntity instanceof Player && plugin.getConfig("placement.void-smart", grave)
                    .getBoolean("placement.void-smart")) {
                Location solidLocation = plugin.getLocationManager().getLastSolidLocation(livingEntity);

                if (solidLocation != null && solidLocation.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                    return solidLocation;
                }
            }

            // Calculate block closest to the void
            location = location.clone();

            if (location.getWorld() != null) {
                Location bottomLocation = getBottom(location, grave);

                if (bottomLocation != null) {
                    return bottomLocation;
                }

                location.setY(getMinHeight(location));

                return MaterialUtil.isAir(location.getBlock().getType()) ? location : null;
            }
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

                if (((blockReplaceList.contains(block.getType().name())
                        || !MaterialUtil.isAir(block.getType()))
                        && !plugin.getCompatibility().hasTitleData(relativeBlock))
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

    public Location getLavaTop(Location location, LivingEntity livingEntity, Grave grave) {
        if (livingEntity instanceof Player
                && plugin.getConfig("placement.lava-smart", grave)
                .getBoolean("placement.lava-smart")) {
            Location solidLocation = plugin.getLocationManager().getLastSolidLocation(livingEntity);

            if (solidLocation != null && solidLocation.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                return solidLocation;
            }
        }

        if (plugin.getConfig("placement.lava-top", grave).getBoolean("placement.lava-top")) {
            location = location.clone();

            if (location.getWorld() != null) {
                List<String> blockReplaceList = plugin.getConfig("replace.block.material", grave)
                        .getStringList("replace.block.material");
                int counter = 0;

                while (counter <= location.getWorld().getMaxHeight()) {
                    Block block = location.getBlock();

                    if ((blockReplaceList.contains(block.getType().name())
                            || MaterialUtil.isAir(block.getType()))
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
                    && (!plugin.hasProtectionLib()
                    || (!plugin.getConfig("placement.can-build-protectionlib", player, permissionList)
                    .getBoolean("placement.can-build-protectionlib")
                    || plugin.getProtectionLib().canBuild(location, player)));
        }

        return true;
    }

    public int getMinHeight(Location location) {
        return location.getWorld() != null && plugin.getVersionManager().hasMinHeight()
                ? location.getWorld().getMinHeight() : 0;
    }
}
