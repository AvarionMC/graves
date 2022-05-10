package com.ranull.graves.integration;

import com.griefdefender.api.Core;
import com.griefdefender.api.Registry;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.data.PlayerData;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionManager;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.registry.CatalogRegistryModule;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class GriefDefender {
    private final Core core;
    private final Registry registry;
    private final PermissionManager permissionManager;
    private final Flag createFlag;
    private final Flag teleportFlag;

    public GriefDefender() {
        core = com.griefdefender.api.GriefDefender.getCore();
        registry = com.griefdefender.api.GriefDefender.getRegistry();
        permissionManager = com.griefdefender.api.GriefDefender.getPermissionManager();
        createFlag = buildCreateFlag();
        teleportFlag = buildTeleportFlag();
        Optional<CatalogRegistryModule<Flag>> catalogRegistryModule = registry.getRegistryModuleFor(Flag.class);

        if (catalogRegistryModule.isPresent()) {
            catalogRegistryModule.get().registerCustomType(createFlag);
            catalogRegistryModule.get().registerCustomType(teleportFlag);
        }
    }

    private Flag buildCreateFlag() {
        return Flag.builder()
                .id("graves:graves-create")
                .name("graves-create")
                .permission("griefdefender.flag.graves.graves-create")
                .build();
    }

    private Flag buildTeleportFlag() {
        return Flag.builder()
                .id("graves:graves-teleport")
                .name("graves-teleport")
                .permission("griefdefender.flag.graves.graves-teleport")
                .build();
    }

    public boolean canCreateGrave(Player player, Location location) {
        if (location.getWorld() != null) {
            PlayerData playerData = core.getPlayerData(location.getWorld().getUID(), player.getUniqueId());

            if (playerData != null) {
                Claim claim = core.getClaimAt(location);
                Set<Context> contextSet = new HashSet<>();

                contextSet.add(new Context("graves:graves_create", player.getName()));

                Tristate tristate = permissionManager.getActiveFlagPermissionValue(null, location, claim,
                        playerData.getUser(), createFlag, player, player, contextSet, null, true);

                return tristate == Tristate.TRUE;
            }
        }

        return false;
    }

    /*

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
     */

    public boolean canTeleport(Entity entity, Location location) {
        return true; // TODO
    }
}
