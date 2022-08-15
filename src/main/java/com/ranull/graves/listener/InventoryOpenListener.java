package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveOpenEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class InventoryOpenListener implements Listener {
    private final Graves plugin;

    public InventoryOpenListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Grave) {
            Grave grave = (Grave) event.getInventory().getHolder();
            GraveOpenEvent graveOpenEvent = new GraveOpenEvent(event.getView(), grave);

            plugin.getServer().getPluginManager().callEvent(graveOpenEvent);
            event.setCancelled(graveOpenEvent.isCancelled());
        }
    }
}
