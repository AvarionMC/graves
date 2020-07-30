package com.ranull.graves.api.events;

import com.ranull.graves.inventory.GraveInventory;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GraveCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled;
    private GraveInventory graveInventory;

    public GraveCreateEvent(GraveInventory graveInventory) {
        this.isCancelled = false;
        this.graveInventory = graveInventory;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public GraveInventory getGraveInventory() {
        return graveInventory;
    }
}
