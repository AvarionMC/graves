package com.rngservers.graves.chest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class Grave implements InventoryHolder {
    private Location location;
    private Inventory inventory;
    private Integer experience;
    private Material replace;
    private EntityType entity;
    private OfflinePlayer player;
    private OfflinePlayer killer;
    private Long time;

    public Grave(Location location, Inventory itemInventory, String title) {
        this.location = location;
        this.time = System.currentTimeMillis();

        inventory = Bukkit.getServer().createInventory(this, 54, title);
        inventory.setContents(itemInventory.getContents());
    }

    public Location getLocation() {
        return location;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Integer getExperience() {
        return experience;
    }

    public Material getReplace() {
        return replace;
    }

    public EntityType getEntityType() {
        return entity;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public OfflinePlayer getKiller() {
        return killer;
    }

    public Long getTime() {
        return time;
    }

    public Integer getItemAmount() {
        return GraveManager.getItemAmount(inventory);
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public void setReplace(Material replace) {
        this.replace = replace;
    }

    public void setEntityType(EntityType entity) {
        this.entity = entity;
    }

    public void setPlayer(OfflinePlayer player) {
        this.player = player;
    }

    public void setKiller(OfflinePlayer killer) {
        this.killer = killer;
    }
}
