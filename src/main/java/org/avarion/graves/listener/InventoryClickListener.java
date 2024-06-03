package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.inventory.GraveList;
import org.avarion.graves.inventory.GraveMenu;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.InventoryUtil;
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

                plugin.getServer()
                      .getScheduler()
                      .runTaskLater(plugin, () -> plugin.getDataManager()
                                                        .updateGrave(grave, "inventory", InventoryUtil.inventoryToString(grave.getInventory())), 1L);
            }
            else if (event.getWhoClicked() instanceof Player player) {

                if (inventoryHolder instanceof GraveList) {
                    GraveList graveList = (GraveList) event.getInventory().getHolder();
                    Grave grave = graveList.getGrave(event.getSlot());

                    if (grave != null) {
                        plugin.getEntityManager()
                              .runFunction(player, plugin.getConfigString("gui.menu.list.function", grave, "menu"), grave);
                        plugin.getGUIManager().setGraveListItems(graveList.getInventory(), graveList.getUUID());
                    }

                    event.setCancelled(true);
                }
                else if (inventoryHolder instanceof GraveMenu) {
                    GraveMenu graveMenu = (GraveMenu) event.getInventory().getHolder();
                    Grave grave = graveMenu.getGrave();

                    if (grave != null) {
                        plugin.getEntityManager()
                              .runFunction(player, plugin.getConfigString("gui.menu.grave.slot."
                                                                          + event.getSlot()
                                                                          + ".function", grave, "none"), grave);
                        plugin.getGUIManager().setGraveMenuItems(graveMenu.getInventory(), grave);
                    }

                    event.setCancelled(true);
                }
            }
        }
    }

}
