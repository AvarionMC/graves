package com.ranull.graves.integration;

import net.md_5.bungee.api.chat.BaseComponent;

public final class MineDown {
    public String parseString(String string) {
        return BaseComponent.toLegacyText(de.themoep.minedown.MineDown.parse(string));
    }
}