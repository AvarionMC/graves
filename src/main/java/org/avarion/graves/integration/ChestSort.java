package org.avarion.graves.integration;

import de.jeff_media.chestsort.api.ChestSortAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class ChestSort {

    public void sortInventory(Inventory inventory) {
        if (inventory.getSize() > 36) {
            ChestSortAPI.sortInventory(inventory, 36, inventory.getSize() - 1);
        }
        else {
            ChestSortAPI.sortInventory(inventory);
        }
    }

    public boolean hasSortingEnabled(Player player) {
        return ChestSortAPI.hasSortingEnabled(player);
    }
}
