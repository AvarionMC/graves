package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.MaterialUtil;
import com.ranull.graves.util.StringUtil;
import com.ranull.graves.util.UUIDUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class EntityManager {
    private final Graves plugin;

    public EntityManager(Graves plugin) {
        this.plugin = plugin;
    }

    public void spawnZombie(Location location, Entity entity, LivingEntity targetEntity, Grave grave) {
        if ((plugin.getConfig("zombie.spawn-owner", grave).getBoolean("zombie.spawn-owner")
                && grave.getOwnerUUID().equals(entity.getUniqueId())
                || plugin.getConfig("zombie.spawn-other", grave).getBoolean("zombie.spawn-other")
                && !grave.getOwnerUUID().equals(entity.getUniqueId()))) {
            spawnZombie(location, targetEntity, grave);
        }
    }

    public void spawnZombie(Location location, Grave grave) {
        spawnZombie(location, null, grave);
    }

    @SuppressWarnings("deprecation")
    private void spawnZombie(Location location, LivingEntity targetEntity, Grave grave) {
        if (location != null && location.getWorld() != null && grave.getOwnerType() == EntityType.PLAYER) {
            String zombieType = plugin.getConfig("zombie.type", grave)
                    .getString("zombie.type", "ZOMBIE").toUpperCase();
            EntityType entityType = EntityType.ZOMBIE;

            try {
                entityType = EntityType.valueOf(zombieType);
            } catch (IllegalArgumentException exception) {
                plugin.debugMessage(zombieType + " is not a EntityType ENUM", 1);
            }

            if (entityType.name().equals("ZOMBIE") && MaterialUtil.isWater(location.getBlock().getType())) {
                try {
                    entityType = EntityType.valueOf("DROWNED");
                } catch (IllegalArgumentException ignored) {
                }
            }

            Entity entity = location.getWorld().spawnEntity(location, entityType);

            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;

                if (livingEntity.getEquipment() != null) {
                    if (plugin.getConfig("zombie.owner-head", grave).getBoolean("zombie.owner-head")) {
                        livingEntity.getEquipment().setHelmet(plugin.getCompatibility().getEntitySkullItemStack(grave, plugin));
                    }

                    livingEntity.getEquipment().setChestplate(null);
                    livingEntity.getEquipment().setLeggings(null);
                    livingEntity.getEquipment().setBoots(null);
                }

                double zombieHealth = plugin.getConfig("zombie.health", grave).getDouble("zombie.health");

                if (zombieHealth >= 0.5) {
                    livingEntity.setMaxHealth(zombieHealth);
                    livingEntity.setHealth(zombieHealth);
                }

                if (!plugin.getConfig("zombie.pickup", grave).getBoolean("zombie.pickup")) {
                    livingEntity.setCanPickupItems(false);
                }

                String zombieName = StringUtil.parseString(plugin.getConfig("zombie.name", grave)
                        .getString("zombie.name"), location, grave, plugin);

                if (!zombieName.equals("")) {
                    livingEntity.setCustomName(zombieName);
                }

                setDataByte(livingEntity, "graveZombie");
                setDataString(livingEntity, "graveUUID", grave.getUUID().toString());

                if (livingEntity instanceof Mob && targetEntity != null && !targetEntity.isInvulnerable()
                        && (!(targetEntity instanceof Player) || ((Player) targetEntity).getGameMode()
                        != GameMode.CREATIVE)) {
                    ((Mob) livingEntity).setTarget(targetEntity);
                }

                if (livingEntity instanceof Zombie) {
                    Zombie zombie = (Zombie) livingEntity;

                    if (zombie.isBaby()) {
                        zombie.setBaby(false);
                    }
                }
            }

            plugin.debugMessage("Zombie type " + getEntityName(entity) + " spawned for grave " + grave.getUUID(), 1);
        }
    }

    @SuppressWarnings({"redundant"})
    public String getEntityName(Entity entity) {
        if (entity != null) {
            if (entity instanceof Player) {
                return ((Player) entity).getName(); // Need redundancy for legacy support
            } else if (!plugin.getVersionManager().is_v1_7()) {
                return entity.getName();
            }

            return StringUtil.format(entity.getType().toString());
        }

        return "null";
    }

    public boolean hasDataString(Entity entity, String string) {
        return plugin.getVersionManager().hasPersistentData() ? entity.getPersistentDataContainer()
                .has(new NamespacedKey(plugin, string), PersistentDataType.STRING) : entity.hasMetadata(string);
    }

    public boolean hasDataByte(Entity entity, String string) {
        return plugin.getVersionManager().hasPersistentData() ? entity.getPersistentDataContainer()
                .has(new NamespacedKey(plugin, string), PersistentDataType.BYTE) : entity.hasMetadata(string);
    }

    public String getDataString(Entity entity, String key) {
        if (plugin.getVersionManager().hasPersistentData() && entity.getPersistentDataContainer()
                .has(new NamespacedKey(plugin, key), PersistentDataType.STRING)) {
            return entity.getPersistentDataContainer().get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
        } else {
            return entity.getMetadata(key).toString();
        }
    }

    public void setDataString(Entity entity, String key, String string) {
        if (plugin.getVersionManager().hasPersistentData()) {
            entity.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, string);
        } else {
            entity.setMetadata(key, new FixedMetadataValue(plugin, string));
        }
    }

    public void setDataByte(Entity entity, String key) {
        if (plugin.getVersionManager().hasPersistentData()) {
            entity.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.BYTE, (byte) 1);
        } else {
            entity.setMetadata(key, new FixedMetadataValue(plugin, (byte) 1));
        }
    }

    public Grave getGraveFromEntityData(Entity entity) {
        if (plugin.getVersionManager().hasPersistentData() && entity.getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)) {
            return plugin.getDataManager().getGraveMap().get(UUIDUtil.getUUID(entity.getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)));
        } else if (entity.hasMetadata("graveUUID")) {
            List<MetadataValue> metadataValue = entity.getMetadata("graveUUID");

            if (!metadataValue.isEmpty()) {
                return plugin.getDataManager().getGraveMap().get(UUIDUtil.getUUID(metadataValue.get(0).asString()));
            }
        }

        return null;
    }
}
