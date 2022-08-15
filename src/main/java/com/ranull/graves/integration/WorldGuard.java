package com.ranull.graves.integration;

import com.ranull.graves.type.Graveyard;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class WorldGuard {
    private final JavaPlugin plugin;
    private final com.sk89q.worldguard.WorldGuard worldGuard;
    private final StateFlag createFlag;
    private final StateFlag teleportFlag;

    public WorldGuard(JavaPlugin plugin) {
        this.plugin = plugin;
        this.worldGuard = com.sk89q.worldguard.WorldGuard.getInstance();
        this.createFlag = getFlag("graves-create");
        this.teleportFlag = getFlag("graves-teleport");
    }

    private StateFlag getFlag(String string) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            Flag<?> flag = worldGuard.getFlagRegistry().get(string);

            if (flag instanceof StateFlag) {
                return (StateFlag) flag;
            }
        } else {
            try {
                StateFlag flag = new StateFlag(string, true);

                worldGuard.getFlagRegistry().register(flag);

                return flag;
            } catch (FlagConflictException exception) {
                Flag<?> flag = worldGuard.getFlagRegistry().get(string);

                if (flag instanceof StateFlag) {
                    return (StateFlag) flag;
                }
            }
        }

        return null;
    }

    public boolean hasCreateGrave(Location location) {
        if (location.getWorld() != null && createFlag != null) {
            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (regionManager != null) {
                ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(BlockVector3
                        .at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

                for (ProtectedRegion protectedRegion : applicableRegions.getRegions()) {
                    if (protectedRegion.getFlag(createFlag) != null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean canCreateGrave(Entity entity, Location location) {
        return entity instanceof Player && createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity), createFlag);
    }

    public boolean canCreateGrave(Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                (RegionAssociable) null, createFlag);
    }

    public boolean canTeleport(Entity entity, Location location) {
        return entity instanceof Player && createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity), teleportFlag);
    }

    public boolean canTeleport(Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                (RegionAssociable) null, teleportFlag);
    }

    public World getRegionWorld(String region) {
        for (RegionManager regionManager : worldGuard.getPlatform().getRegionContainer().getLoaded()) {
            if (regionManager.getRegions().containsKey(region)) {
                return plugin.getServer().getWorld(regionManager.getName());
            }
        }

        return null;
    }

    public boolean isMember(String region, Player player) {
        for (RegionManager regionManager : worldGuard.getPlatform().getRegionContainer().getLoaded()) {
            if (regionManager.getRegions().containsKey(region)) {
                ProtectedRegion protectedRegion = regionManager.getRegion(region);

                if (protectedRegion != null) {
                    return protectedRegion.isMember(WorldGuardPlugin.inst().wrapPlayer(player));
                }
            }
        }

        return false;
    }

    public boolean isInsideRegion(Location location, String region) {
        if (location.getWorld() != null) {
            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (regionManager != null) {
                ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(BlockVector3
                        .at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

                for (ProtectedRegion protectedRegion : applicableRegions.getRegions()) {
                    if (protectedRegion.getId().equals(region)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public Location calculateRoughLocation(Graveyard graveyard) {
        RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(graveyard.getWorld()));

        if (regionManager != null) {
            ProtectedRegion protectedRegion = regionManager.getRegion(graveyard.getName());

            if (protectedRegion != null) {
                int xMax = protectedRegion.getMaximumPoint().getBlockX();
                int yMax = protectedRegion.getMaximumPoint().getBlockY();
                int zMax = protectedRegion.getMaximumPoint().getBlockZ();
                int xMin = protectedRegion.getMinimumPoint().getBlockX();
                int yMin = protectedRegion.getMinimumPoint().getBlockY();
                int zMin = protectedRegion.getMinimumPoint().getBlockZ();

                return new Location(graveyard.getWorld(), xMax - xMin, yMax - yMin, zMax - zMin);
            }
        }

        return null;
    }

    public List<String> getRegionKeyList(Location location) {
        List<String> regionNameList = new ArrayList<>();

        if (location.getWorld() != null) {
            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (regionManager != null) {
                ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(BlockVector3
                        .at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

                for (ProtectedRegion protectedRegion : applicableRegions.getRegions()) {
                    regionNameList.add("worldguard|" + location.getWorld().getName() + "|" + protectedRegion.getId());
                }
            }
        }

        return regionNameList;
    }
}
