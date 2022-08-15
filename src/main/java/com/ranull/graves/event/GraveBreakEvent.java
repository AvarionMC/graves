package com.ranull.graves.event;

import com.ranull.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public class GraveBreakEvent extends BlockBreakEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Grave grave;
    private boolean dropItems;

    public GraveBreakEvent(Block block, Player player, Grave grave) {
        super(block, player);

        this.dropItems = true;
        this.grave = grave;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Grave getGrave() {
        return grave;
    }

    public int getBlockExp() {
        return grave.getExperience();
    }

    public boolean isDropItems() {
        return this.dropItems;
    }

    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}