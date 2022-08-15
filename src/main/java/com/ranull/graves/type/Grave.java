package com.ranull.graves.type;

import com.ranull.graves.data.LocationData;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

public class Grave implements InventoryHolder, Serializable {
    private final UUID uuid;
    private transient Inventory inventory;
    private Map<EquipmentSlot, ItemStack> equipmentMap;
    private List<String> permissionList;
    private LocationData locationDeath;
    private float yaw;
    private float pitch;
    private EntityType ownerType;
    private String ownerName;
    private String ownerNameDisplay;
    private UUID ownerUUID;
    private String ownerTexture;
    private String ownerTextureSignature;
    private EntityType killerType;
    private String killerName;
    private String killerNameDisplay;
    private UUID killerUUID;
    private int experience;
    private boolean protection;
    private long timeAlive;
    private long timeCreation;
    private long timeProtection;

    public Grave(UUID uuid) {
        this.uuid = uuid;
        this.timeCreation = System.currentTimeMillis();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Map<EquipmentSlot, ItemStack> getEquipmentMap() {
        return equipmentMap;
    }

    public void setEquipmentMap(Map<EquipmentSlot, ItemStack> equipmentMap) {
        this.equipmentMap = equipmentMap;
    }

    public List<ItemStack> getInventoryItemStack() {
        return inventory != null ? Arrays.asList(inventory.getContents()) : new ArrayList<>();
    }

    public UUID getUUID() {
        return uuid;
    }

    public List<String> getPermissionList() {
        return permissionList;
    }

    public void setPermissionList(List<String> permissionList) {
        this.permissionList = permissionList;
    }

    public Location getLocationDeath() {
        return locationDeath != null ? locationDeath.getLocation() : null;
    }

    public void setLocationDeath(Location locationDeath) {
        this.locationDeath = new LocationData(locationDeath);
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public EntityType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(EntityType ownerType) {
        this.ownerType = ownerType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerNameDisplay() {
        return ownerNameDisplay;
    }

    public void setOwnerNameDisplay(String ownerNameDisplay) {
        this.ownerNameDisplay = ownerNameDisplay;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public String getOwnerTexture() {
        return ownerTexture;
    }

    public void setOwnerTexture(String ownerTexture) {
        this.ownerTexture = ownerTexture;
    }

    public String getOwnerTextureSignature() {
        return ownerTextureSignature;
    }

    public void setOwnerTextureSignature(String ownerTextureSignature) {
        this.ownerTextureSignature = ownerTextureSignature;
    }

    public EntityType getKillerType() {
        return killerType;
    }

    public void setKillerType(EntityType killerType) {
        this.killerType = killerType;
    }

    public String getKillerName() {
        return killerName;
    }

    public void setKillerName(String killerName) {
        this.killerName = killerName;
    }

    public String getKillerNameDisplay() {
        return killerNameDisplay;
    }

    public void setKillerNameDisplay(String killerNameDisplay) {
        this.killerNameDisplay = killerNameDisplay;
    }

    public UUID getKillerUUID() {
        return killerUUID;
    }

    public void setKillerUUID(UUID killerUUID) {
        this.killerUUID = killerUUID;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public boolean getProtection() {
        return protection;
    }

    public void setProtection(boolean protection) {
        this.protection = protection;
    }

    public long getTimeAlive() {
        return timeAlive;
    }

    public void setTimeAlive(long aliveTime) {
        this.timeAlive = aliveTime;
    }

    public long getTimeCreation() {
        return timeCreation;
    }

    public void setTimeCreation(long timeCreation) {
        this.timeCreation = timeCreation;
    }

    public long getTimeProtection() {
        return timeProtection;
    }

    public void setTimeProtection(long timeProtection) {
        this.timeProtection = timeProtection;
    }

    public long getTimeAliveRemaining() {
        if (timeAlive < 0) {
            return -1;
        } else {
            long timeAliveRemaining = (timeAlive + 1000) - (System.currentTimeMillis() - timeCreation);

            return timeAliveRemaining >= 0 ? timeAliveRemaining : 0;
        }
    }

    public long getTimeProtectionRemaining() {
        if (timeProtection < 0) {
            return -1;
        } else {
            long timeProtectionRemaining = (timeProtection + 1000) - (System.currentTimeMillis() - timeCreation);

            return timeProtectionRemaining >= 0 ? timeProtectionRemaining : 0;
        }
    }

    public long getLivedTime() {
        return System.currentTimeMillis() - timeCreation;
    }

    public int getItemAmount() {
        int counter = 0;

        if (inventory != null) {
            for (ItemStack itemStack : inventory.getContents()) {
                if (itemStack != null) {
                    counter++;
                }
            }
        }

        return counter;
    }

    public enum StorageMode {
        EXACT,
        COMPACT,
        CHESTSORT
    }
}
