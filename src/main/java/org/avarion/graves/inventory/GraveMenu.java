package org.avarion.graves.inventory;

import org.avarion.graves.type.Grave;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class GraveMenu implements InventoryHolder {

    private final Grave grave;
    private Inventory inventory;

    public GraveMenu(Grave grave) {
        this.grave = grave;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Grave getGrave() {
        return grave;
    }

}
