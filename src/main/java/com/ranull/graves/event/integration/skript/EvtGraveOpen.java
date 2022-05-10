package com.ranull.graves.event.integration.skript;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class EvtGraveOpen extends SkriptEvent {
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        return false;
    }

    @Override
    public boolean check(@NotNull Event event) {
        return false;
    }

    @NotNull
    @Override
    public String toString(Event event, boolean debug) {
        return "";
    }
}