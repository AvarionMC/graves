package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.inventory.GraveList;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.InventoryUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class InventoryDragListener implements Listener {

    private final Graves plugin;

    public InventoryDragListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();

        if (inventoryHolder instanceof Grave) {
            Grave grave = (Grave) event.getInventory().getHolder();

            plugin.getServer()
                  .getScheduler()
                  .runTaskLater(plugin, () -> plugin.getDataManager()
                                                    .updateGrave(grave, "inventory", InventoryUtil.inventoryToString(grave.getInventory())), 1L);
        }
        else if (inventoryHolder instanceof GraveList) {
            event.setCancelled(true);
        }
    }

}
