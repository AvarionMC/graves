package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.listener.integration.playernpc.NPCInteractListener;
import com.ranull.graves.manager.EntityDataManager;
import com.ranull.graves.type.Grave;
import dev.sergiferry.playernpc.api.NPC;
import dev.sergiferry.playernpc.api.NPCLib;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;

public final class PlayerNPC extends EntityDataManager {
    private final Graves plugin;
    private final NPCLib npcLib;
    private final NPCInteractListener npcInteractListener;

    public PlayerNPC(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
        this.npcLib = NPCLib.getInstance();
        this.npcInteractListener = new NPCInteractListener(plugin, this);

        if (!NPCLib.getInstance().isRegistered(plugin)) {
            NPCLib.getInstance().registerPlugin(plugin);
        }

        registerListeners();
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(npcInteractListener, plugin);
    }

    public void unregisterListeners() {
        if (npcInteractListener != null) {
            HandlerList.unregisterAll(npcInteractListener);
        }
    }

    public void createCorpses() {
        for (ChunkData chunkData : plugin.getCacheManager().getChunkMap().values()) {
            for (EntityData entityData : chunkData.getEntityDataMap().values()) {
                if (entityData.getType() == EntityData.Type.PLAYERNPC) {
                    if (plugin.getCacheManager().getGraveMap().containsKey(entityData.getUUIDGrave())) {
                        Grave grave = plugin.getCacheManager().getGraveMap().get(entityData.getUUIDGrave());

                        if (grave != null) {
                            plugin.getIntegrationManager().getPlayerNPC().createCorpse(entityData.getUUIDEntity(),
                                    entityData.getLocation(), grave, false);
                        }
                    }
                }
            }
        }
    }

    public void createCorpse(Location location, Grave grave) {
        createCorpse(UUID.randomUUID(), location, grave, true);
    }

    public void createCorpse(UUID uuid, Location location, Grave grave, boolean createEntityData) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.getConfig("playernpc.corpse.enabled", grave).getBoolean("playernpc.corpse.enabled")
                    && grave.getOwnerType() == EntityType.PLAYER) {
                Player player = plugin.getServer().getPlayer(grave.getOwnerUUID());
                Location npcLocation = location.clone();

                if (player != null && npcLocation.getWorld() != null
                        && npcLib.getGlobalNPC(plugin, uuid.toString()) == null) {
                    location.getBlock().setType(Material.AIR);
                    NPC.Pose pose = NPC.Pose.SWIMMING;

                    try {
                        pose = NPC.Pose.valueOf(plugin.getConfig("playernpc.corpse.pose", grave)
                                .getString("playernpc.corpse.pose"));
                    } catch (IllegalArgumentException ignored) {
                    }

                    if (pose == NPC.Pose.SWIMMING) {
                        npcLocation.add(0.5, -0.2, 0.5);
                    }

                    NPC.Global npc = npcLib.generateGlobalNPC(plugin, uuid.toString(), npcLocation);
                    NPC.Skin.Custom skin = NPC.Skin.Custom.getLoadedSkin(plugin, grave.getUUID().toString());

                    if (skin == null && grave.getOwnerTexture() != null
                            && grave.getOwnerTextureSignature() != null
                            && grave.getOwnerName() != null) {
                        skin = NPC.Skin.Custom.createCustomSkin(plugin, grave.getUUID().toString(),
                                grave.getOwnerTexture(), grave.getOwnerTextureSignature());
                    }

                    npc.setSkin(skin);
                    npc.setPose(pose);
                    npc.setAutoCreate(true);
                    npc.setAutoShow(true);
                    npc.setCustomData("grave_uuid", grave.getUUID().toString());

                    npc.setCollidable(plugin.getConfig("playernpc.corpse.collide", grave)
                            .getBoolean("playernpc.corpse.collide"));

                    if (plugin.getConfig("playernpc.corpse.armor", grave).getBoolean("playernpc.corpse.armor")) {
                        if (grave.getEquipmentMap().containsKey(EquipmentSlot.HEAD)) {
                            npc.setHelmet(grave.getEquipmentMap().get(EquipmentSlot.HEAD));
                        }

                        if (grave.getEquipmentMap().containsKey(EquipmentSlot.CHEST)) {
                            npc.setChestPlate(grave.getEquipmentMap().get(EquipmentSlot.CHEST));
                        }

                        if (grave.getEquipmentMap().containsKey(EquipmentSlot.LEGS)) {
                            npc.setLeggings(grave.getEquipmentMap().get(EquipmentSlot.LEGS));
                        }

                        if (grave.getEquipmentMap().containsKey(EquipmentSlot.FEET)) {
                            npc.setBoots(grave.getEquipmentMap().get(EquipmentSlot.FEET));
                        }
                    }

                    if (plugin.getConfig("playernpc.corpse.hand", grave).getBoolean("playernpc.corpse.hand")) {
                        if (grave.getEquipmentMap().containsKey(EquipmentSlot.HAND)) {
                            npc.setItemInRightHand(grave.getEquipmentMap().get(EquipmentSlot.HAND));
                        }

                        if (plugin.getVersionManager().hasSecondHand()
                                && grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND)) {
                            npc.setItemInLeftHand(grave.getEquipmentMap().get(EquipmentSlot.OFF_HAND));
                        }
                    }

                    if (plugin.getConfig("playernpc.corpse.glow.enabled", grave)
                            .getBoolean("playernpc.corpse.glow.enabled")) {
                        try {
                            npc.setGlowing(true, ChatColor.valueOf(plugin
                                    .getConfig("playernpc.corpse.glow.color", grave)
                                    .getString("playernpc.corpse.glow.color")));
                        } catch (IllegalArgumentException ignored) {
                            npc.setGlowing(true);
                        }
                    }

                    npc.forceUpdate();
                    plugin.debugMessage("Spawning PlayerNPC NPC for " + grave.getUUID() + " at "
                            + npcLocation.getWorld().getName() + ", " + (npcLocation.getBlockX() + 0.5) + "x, "
                            + (npcLocation.getBlockY() + 0.5) + "y, " + (npcLocation.getBlockZ() + 0.5) + "z", 1);

                    if (createEntityData) {
                        createEntityData(location, uuid, grave.getUUID(), EntityData.Type.PLAYERNPC);
                    }
                }
            }
        });
    }

    public void removeCorpse(Grave grave) {
        removeCorpse(getEntityDataNPCMap(getLoadedEntityDataList(grave)));
    }

    public void removeCorpse(EntityData entityData) {
        removeCorpse(getEntityDataNPCMap(Collections.singletonList(entityData)));
    }

    public void removeCorpse(Map<EntityData, NPC.Global> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, NPC.Global> entry : entityDataMap.entrySet()) {
            npcLib.removeGlobalNPC(entry.getValue());
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    private Map<EntityData, NPC.Global> getEntityDataNPCMap(List<EntityData> entityDataList) {
        Map<EntityData, NPC.Global> entityDataMap = new HashMap<>();

        for (EntityData entityData : entityDataList) {
            for (NPC.Global npc : npcLib.getAllGlobalNPCs()) {
                if (npc.hasCustomData("grave_uuid")
                        && npc.getCustomData("grave_uuid").equals(entityData.getUUIDGrave().toString())) {
                    entityDataMap.put(entityData, npc);
                }
            }
        }

        return entityDataMap;
    }
}