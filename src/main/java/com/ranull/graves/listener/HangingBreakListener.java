package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;

public class HangingBreakListener implements Listener {
    private final Graves plugin;

    public HangingBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHangingBreak(HangingBreakEvent event) {
        event.setCancelled(event.getEntity() instanceof ItemFrame && plugin.getEntityDataManager()
                .getGrave(event.getEntity()) != null);
    }
}
