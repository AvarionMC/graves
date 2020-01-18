package com.rngservers.graves.grave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Grave implements InventoryHolder {
    private Location location;
    private Inventory inventory;
    private Integer experience;
    private Material replace;
    private EntityType entity;
    private OfflinePlayer player;
    private OfflinePlayer killer;
    private Long createdTime;
    private Integer aliveTime;
    private Map<UUID, Integer> holograms = new HashMap<>();

    public Grave(Location location, Inventory itemInventory, String title) {
        this.location = location;
        this.createdTime = System.currentTimeMillis();
        inventory = Bukkit.getServer().createInventory(this, itemInventory.getSize(), title);
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

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
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

    public Integer getAliveTime() {
        return aliveTime;
    }

    public void setAliveTime(Integer aliveTime) {
        this.aliveTime = aliveTime;
    }

    public void setHolograms(Map<UUID, Integer> holograms) {
        this.holograms = holograms;
    }

    public void addHologram(UUID uuid, Integer lineNumber) {
        this.holograms.put(uuid, lineNumber);
    }

    public Map<UUID, Integer> getHolograms() {
        return holograms;
    }
}
