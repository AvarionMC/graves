package com.ranull.graves.inventory;

import com.ranull.graves.manager.GraveManager;
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

public class GraveInventory implements InventoryHolder {
    private Location location;
    private Inventory inventory;
    private Material replace;
    private EntityType entity;
    private OfflinePlayer player;
    private OfflinePlayer killer;
    private ConcurrentMap<UUID, Integer> holograms = new ConcurrentHashMap<>();
    private long createdTime;
    private int aliveTime;
    private int protectTime;
    private int experience;
    private boolean protect;
    private boolean unlink;

    public GraveInventory(Location location, Inventory itemInventory, String title) {
        this.location = location;
        this.createdTime = System.currentTimeMillis();

        inventory = Bukkit.getServer().createInventory(this, itemInventory.getSize(), title);
        inventory.setContents(itemInventory.getContents());
    }

    public Location getLocation() {
        return this.location;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public Material getReplaceMaterial() {
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

    public ConcurrentMap<UUID, Integer> getHolograms() {
        return this.holograms;
    }

    public long getCreatedTime() {
        return this.createdTime;
    }

    public int getAliveTime() {
        return this.aliveTime;
    }

    public int getProtectTime() {
        return this.protectTime;
    }

    public int getExperience() {
        return this.experience;
    }

    public boolean getProtected() {
        return this.protect;
    }

    public void setProtected(boolean protect) {
        this.protect = protect;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void setExperience(int experience) {
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

    public void setProtectTime(int protectTime) {
        this.protectTime = protectTime;
    }

    public void setAliveTime(int aliveTime) {
        this.aliveTime = aliveTime;
    }

    public void addHologram(UUID uuid, int lineNumber) {
        this.holograms.put(uuid, lineNumber);
    }

    public int getItemAmount() {
        return GraveManager.getItemAmount(this.inventory);
    }

    public boolean getUnlink() {
        return unlink;
    }

    public void setUnlink(boolean unlink) {
        this.unlink = unlink;
    }
}
