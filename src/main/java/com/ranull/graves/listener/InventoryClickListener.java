package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class InventoryClickListener implements Listener {
    private final Graves plugin;

    public InventoryClickListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();

        if (inventoryHolder != null) {
            if (inventoryHolder instanceof Grave) {
                Grave grave = (Grave) inventoryHolder;

                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        plugin.getDataManager().updateGrave(grave, "inventory",
                                InventoryUtil.inventoryToString(grave.getInventory())), 1L);
            } else if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();

                if (inventoryHolder instanceof GraveList) {
                    GraveList graveList = (GraveList) event.getInventory().getHolder();
                    Grave grave = graveList.getGrave(event.getSlot());

                    if (grave != null) {
                        plugin.getEntityManager().runFunction(player, plugin.getConfig("gui.menu.list.function", grave)
                                .getString("gui.menu.list.function", "menu"), grave);
                        plugin.getGUIManager().setGraveListItems(graveList.getInventory(), graveList.getUUID());
                    }

                    event.setCancelled(true);
                } else if (inventoryHolder instanceof GraveMenu) {
                    GraveMenu graveMenu = (GraveMenu) event.getInventory().getHolder();
                    Grave grave = graveMenu.getGrave();

                    if (grave != null) {
                        plugin.getEntityManager().runFunction(player,
                                plugin.getConfig("gui.menu.grave.slot." + event.getSlot() + ".function", grave)
                                        .getString("gui.menu.grave.slot." + event.getSlot()
                                                + ".function", "none"), grave);
                        plugin.getGUIManager().setGraveMenuItems(graveMenu.getInventory(), grave);
                    }

                    event.setCancelled(true);
                }
            }
        }
    }
}
