package org.avarion.graves.integration;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class MiniMessage {

    private final net.kyori.adventure.text.minimessage.MiniMessage miniMessage;

    public MiniMessage() {
        miniMessage = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
    }

    public @NotNull String parseString(String string) {
        return LegacyComponentSerializer.legacySection().serialize(miniMessage.deserialize(string));
    }

}
