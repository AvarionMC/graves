package com.ranull.graves.listener.integration.oraxen;

import com.ranull.graves.integration.Oraxen;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;

public class HangingBreakListener implements Listener {
    private final Oraxen oraxen;

    public HangingBreakListener(Oraxen oraxen) {
        this.oraxen = oraxen;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHangingBreak(HangingBreakEvent event) {
        event.setCancelled(event.getEntity() instanceof ItemFrame && oraxen.getGrave(event.getEntity()) != null);
    }
}
