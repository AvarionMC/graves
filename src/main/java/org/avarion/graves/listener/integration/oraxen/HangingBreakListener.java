package org.avarion.graves.listener.integration.oraxen;

import org.avarion.graves.integration.Oraxen;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.jetbrains.annotations.NotNull;

public class HangingBreakListener implements Listener {

    private final Oraxen oraxen;

    public HangingBreakListener(Oraxen oraxen) {
        this.oraxen = oraxen;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHangingBreak(@NotNull HangingBreakEvent event) {
        event.setCancelled(event.getEntity() instanceof ItemFrame && oraxen.getGrave(event.getEntity()) != null);
    }

}
