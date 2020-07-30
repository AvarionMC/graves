package com.ranull.graves.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GraveListInventory implements InventoryHolder {
    private Inventory inventory;

    public GraveListInventory(String name, List<ItemStack> items, Integer size) {
        inventory = Bukkit.createInventory(this, size, name);

        for (ItemStack item : items) {
            inventory.addItem(item);
        }
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}