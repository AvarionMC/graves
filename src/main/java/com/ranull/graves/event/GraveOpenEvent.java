package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

public class GraveOpenEvent extends InventoryOpenEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;

    public GraveOpenEvent(InventoryView inventoryView, Grave grave) {
        super(inventoryView);

        this.grave = grave;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public Grave getGrave() {
        return grave;
    }
}