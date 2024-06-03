package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.event.GraveOpenEvent;
import org.avarion.graves.type.Grave;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.jetbrains.annotations.NotNull;

public class InventoryOpenListener implements Listener {

    private final Graves plugin;

    public InventoryOpenListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Grave grave) {
            GraveOpenEvent graveOpenEvent = new GraveOpenEvent(event.getView(), grave);

            plugin.getServer().getPluginManager().callEvent(graveOpenEvent);
            event.setCancelled(graveOpenEvent.isCancelled());
        }
    }

}
