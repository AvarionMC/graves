package com.ranull.graves.integration;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MiniMessage {
    private final net.kyori.adventure.text.minimessage.MiniMessage miniMessage;

    public MiniMessage() {
        miniMessage = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
    }

    public String parseString(String string) {
        return LegacyComponentSerializer.legacySection().serialize(miniMessage.deserialize(string));
    }
}