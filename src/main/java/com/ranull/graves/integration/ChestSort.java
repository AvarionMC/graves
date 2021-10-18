package com.ranull.graves.integration;

import de.jeff_media.chestsort.api.ChestSortAPI;
import org.bukkit.inventory.Inventory;

public final class ChestSort {
    public void sortInventory(Inventory inventory) {
        ChestSortAPI.sortInventory(inventory);
    }
}