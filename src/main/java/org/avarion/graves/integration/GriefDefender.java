package org.avarion.graves.integration;

import com.griefdefender.api.Registry;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.registry.CatalogRegistryModule;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;

public final class GriefDefender {

    private final Flag createFlag;

    public GriefDefender() {
        Registry registry = com.griefdefender.api.GriefDefender.getRegistry();
        createFlag = buildCreateFlag();
        Flag teleportFlag = buildTeleportFlag();
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

    @SuppressWarnings("SameReturnValue")
    public boolean canTeleport(Entity entity, Location location) {
        return true;
    }
}
