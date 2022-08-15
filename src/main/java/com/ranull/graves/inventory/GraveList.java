package com.ranull.graves.inventory;

import com.ranull.graves.type.Grave;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class GraveList implements InventoryHolder {
    private final UUID uuid;
    private final List<Grave> graveList;
    private Inventory inventory;

    public GraveList(UUID uuid, List<Grave> graveList) {
        this.uuid = uuid;
        this.graveList = graveList;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Grave getGrave(int slot) {
        return slot >= 0 && graveList.size() > slot ? graveList.get(slot) : null;
    }
}
