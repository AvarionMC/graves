package com.rngservers.graves.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GravesGUI implements InventoryHolder {
    private final Inventory inv;
    private List<ItemStack> items;

    public GravesGUI(String name, List<ItemStack> items, Integer size) {
        this.items = items;
        inv = Bukkit.createInventory(this, size, name);
        initializeItems();
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void initializeItems() {
        for (ItemStack item : items) {
            inv.addItem(item);
        }
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
    }
}