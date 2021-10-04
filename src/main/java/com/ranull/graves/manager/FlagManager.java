package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FlagManager {
    private final Graves plugin;
    private final WorldGuard worldGuard;
    private final Plugin worldGuardPlugin;
    private final StateFlag createFlag;
    private final StateFlag teleportFlag;

    public FlagManager(Graves plugin) {
        this.plugin = plugin;
        this.worldGuard = WorldGuard.getInstance();
        this.worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        createFlag = getFlag("graves-create");
        teleportFlag = getFlag("graves-teleport");
    }

    private StateFlag getFlag(String string) {
        if (worldGuardPlugin != null && worldGuardPlugin.isEnabled()) {
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

    public boolean canCreateGrave(Player player, Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer(player), createFlag);
    }

    public boolean canCreateGrave(Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                (RegionAssociable) null, createFlag);
    }

    public boolean canTeleport(Player player, Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer(player), teleportFlag);
    }

    public boolean canTeleport(Location location) {
        return createFlag != null
                && worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(location),
                (RegionAssociable) null, teleportFlag);
    }
}
