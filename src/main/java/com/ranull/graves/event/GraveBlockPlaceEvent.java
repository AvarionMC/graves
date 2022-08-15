package com.ranull.graves.event;

import com.ranull.graves.data.BlockData;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GraveBlockPlaceEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;
    private Location location;
    private BlockData.BlockType blockType;
    private boolean cancel;

    public GraveBlockPlaceEvent(Grave grave, Location location, BlockData.BlockType blockType) {
        this.location = location;
        this.grave = grave;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Block getBlock() {
        return location.getBlock();
    }

    public Grave getGrave() {
        return grave;
    }

    public BlockData.BlockType getBlockType() {
        return blockType;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}