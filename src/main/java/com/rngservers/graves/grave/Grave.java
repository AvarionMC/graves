package com.rngservers.graves.grave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Grave implements InventoryHolder {
    private Location location;
    private Inventory inventory;
    private Integer level;
    private Material replace;
    private EntityType entity;
    private OfflinePlayer player;
    private OfflinePlayer killer;
    private Long createdTime;
    private Integer aliveTime;
    private Boolean protect;
    private Integer protectTime;
    private ConcurrentMap<UUID, Integer> holograms = new ConcurrentHashMap<>();

    public Grave(Location location, Inventory itemInventory, String title) {
        this.location = location;
        this.createdTime = System.currentTimeMillis();
        inventory = Bukkit.getServer().createInventory(this, itemInventory.getSize(), title);
        inventory.setContents(itemInventory.getContents());
    }

    public Location getLocation() {
        return this.location;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Integer getLevel() {
        return this.level;
    }

    public Material getReplace() {
        return this.replace;
    }

    public EntityType getEntityType() {
        return this.entity;
    }

    public OfflinePlayer getPlayer() {
        return this.player;
    }

    public OfflinePlayer getKiller() {
        return this.killer;
    }

    public Long getCreatedTime() {
        return this.createdTime;
    }

    public Boolean getProtected() {
        return this.protect;
    }

    public Integer getProtectTime() {
        return this.protectTime;
    }

    public void setProtected(Boolean protect) {
        this.protect = protect;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public Integer getItemAmount() {
        return GraveManager.getItemAmount(this.inventory);
    }

    public void setLevel(Integer level) {
        this.level = level;
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

    public void setProtectTime(Integer protectTime) {
        this.protectTime = protectTime;
    }

    public Integer getAliveTime() {
        return this.aliveTime;
    }

    public void setAliveTime(Integer aliveTime) {
        this.aliveTime = aliveTime;
    }

    public void setHolograms(ConcurrentMap<UUID, Integer> holograms) {
        this.holograms = holograms;
    }

    public void addHologram(UUID uuid, Integer lineNumber) {
        this.holograms.put(uuid, lineNumber);
    }

    public ConcurrentMap<UUID, Integer> getHolograms() {
        return this.holograms;
    }
}
