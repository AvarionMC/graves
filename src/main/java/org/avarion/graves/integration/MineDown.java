package org.avarion.graves.integration;

import net.md_5.bungee.api.chat.BaseComponent;
import org.jetbrains.annotations.NotNull;

public final class MineDown {

    public @NotNull String parseString(String string) {
        return BaseComponent.toLegacyText(de.themoep.minedown.MineDown.parse(string));
    }

}
