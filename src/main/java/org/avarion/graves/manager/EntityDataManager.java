package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.data.ChunkData;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.*;

public class EntityDataManager {

    private final Graves plugin;

    public EntityDataManager(Graves plugin) {
        this.plugin = plugin;
    }

    public void createEntityData(Entity entity, Grave grave, EntityData.Type type) {
        createEntityData(entity.getLocation(), entity.getUniqueId(), grave.getUUID(), type);
    }

    public void createEntityData(Location location, UUID entityUUID, UUID graveUUID, EntityData.Type type) {
        EntityData entityData = new EntityData(location.clone(), entityUUID, graveUUID, type);

        plugin.getDataManager().addEntityData(entityData);

        if (plugin.getIntegrationManager().hasMultiPaper()) {
            plugin.getIntegrationManager().getMultiPaper().notifyEntityCreation(entityData);
        }
    }

    public EntityData getEntityData(Location location, UUID uuid) {
        if (plugin.getDataManager().hasChunkData(location)) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(location);

            if (chunkData.getEntityDataMap().containsKey(uuid)) {
                return chunkData.getEntityDataMap().get(uuid);
            }
        }

        return null;
    }

    public Grave getGrave(Location location, UUID uuid) {
        EntityData entityData = getEntityData(location, uuid);

        return entityData != null && plugin.getCacheManager().getGraveMap().containsKey(entityData.getUUIDGrave())
               ? plugin.getCacheManager().getGraveMap().get(entityData.getUUIDGrave())
               : null;
    }

    public Grave getGrave(Entity entity) {
        return getGrave(entity.getLocation(), entity.getUniqueId());
    }

    public void removeEntityData(EntityData entityData) {
        removeEntityData(Collections.singletonList(entityData));
    }

    public List<EntityData> getLoadedEntityDataList(Grave grave) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<String, ChunkData> chunkDataEntry : plugin.getCacheManager().getChunkMap().entrySet()) {
            ChunkData chunkData = chunkDataEntry.getValue();

            if (chunkDataEntry.getValue().isLoaded()) {
                for (EntityData entityData : new ArrayList<>(chunkData.getEntityDataMap().values())) {
                    if (grave.getUUID().equals(entityData.getUUIDGrave())) {
                        entityDataList.add(entityData);
                    }
                }
            }
        }

        return entityDataList;
    }

    public Map<EntityData, Entity> getEntityDataMap(List<EntityData> entityDataList) {
        Map<EntityData, Entity> entityDataMap = new HashMap<>();

        for (EntityData entityData : entityDataList) {
            for (Entity entity : entityData.getLocation().getChunk().getEntities()) {
                if (entity.getUniqueId().equals(entityData.getUUIDEntity())) {
                    entityDataMap.put(entityData, entity);
                }
            }
        }

        return entityDataMap;
    }

    public void removeEntries(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            entry.getValue().remove();
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    public void removeEntityData(List<EntityData> entityDataList) {
        List<EntityData> removedEntityDataList = new ArrayList<>();

        for (EntityData entityData : entityDataList) {
            for (Entity entity : entityData.getLocation().getChunk().getEntities()) {
                if (entity.getUniqueId().equals(entityData.getUUIDEntity())) {
                    removedEntityDataList.add(entityData);
                }
            }
        }

        plugin.getDataManager().removeEntityData(removedEntityDataList);
    }

}
