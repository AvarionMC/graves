package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.util.InventoryUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

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

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getDataManager().updateGrave(grave, "inventory",
                            InventoryUtil.inventoryToString(grave.getInventory()));
                }
            }.runTaskLater(plugin, 1L);
        } else if (inventoryHolder instanceof GraveList) {
            event.setCancelled(true);
        }
    }
}
