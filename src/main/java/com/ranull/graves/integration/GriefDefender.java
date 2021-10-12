package com.ranull.graves.integration;

import com.griefdefender.api.Registry;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.api.permission.flag.FlagDefinition;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.api.util.generator.DummyObjectProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GriefDefender {
    private final FlagDefinition.Builder definitionBuilder;
    private final FlagData.Builder flagDataBuilder;
    private final CatalogRegistryModule<Flag> catalogRegistryModule;

    public GriefDefender() {
        Registry registry = com.griefdefender.api.GriefDefender.getRegistry();

        this.definitionBuilder = registry.createBuilder(FlagDefinition.Builder.class);
        this.flagDataBuilder = registry.createBuilder(FlagData.Builder.class);
        this.catalogRegistryModule = registry.getRegistryModuleFor(Flag.class).orElse(null);
    }

    public void registerFlag() {
        //registerCustomType(generateFlag("graves-create")); // TODO
    }

    private List<Flag> generateFlag(String name) {
        Set<Context> flagContexts = new HashSet<>();
        flagContexts.add(new Context(ContextKeys.SOURCE, "minecraft:player"));

        return definitionBuilder
                .reset()
                .name(name)
                .admin(true)
                .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                .defaultValue(Tristate.TRUE)
                //.description(Component.text("Create graves"))
                .group("admin")
                .flagData(flagDataBuilder
                        .reset()
                        .flag(createFor(name))
                        .contexts(flagContexts)
                        .build())
                .build().getFlags();
    }

    private Flag createFor(String name) {
        return DummyObjectProvider.createFor(Flag.class, name);
    }

    private void registerCustomType(List<Flag> flagList) {
        flagList.forEach((catalogRegistryModule::registerCustomType));
    }
}
