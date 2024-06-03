package org.avarion.graves.integration;

import dev.sergiferry.playernpc.api.NPC;
import dev.sergiferry.playernpc.api.NPCLib;
import org.avarion.graves.Graves;
import org.avarion.graves.data.ChunkData;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.listener.integration.playernpc.NPCInteractListener;
import org.avarion.graves.manager.CacheManager;
import org.avarion.graves.manager.EntityDataManager;
import org.avarion.graves.type.Grave;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class PlayerNPC extends EntityDataManager {

    private final Graves plugin;
    private final NPCLib npcLib;
    private final NPCInteractListener npcInteractListener;

    public PlayerNPC(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
        this.npcLib = NPCLib.getInstance();
        this.npcInteractListener = new NPCInteractListener(plugin);

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
        for (ChunkData chunkData : CacheManager.chunkMap.values()) {
            for (EntityData entityData : chunkData.getEntityDataMap().values()) {
                if (entityData.getType() == EntityData.Type.PLAYERNPC) {
                    if (CacheManager.graveMap.containsKey(entityData.getUUIDGrave())) {
                        Grave grave = CacheManager.graveMap.get(entityData.getUUIDGrave());

                        if (grave != null) {
                            plugin.getIntegrationManager()
                                  .getPlayerNPC()
                                  .createCorpse(entityData.getUUIDEntity(), entityData.getLocation(), grave, false);
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
            if (plugin.getConfigBool("playernpc.corpse.enabled", grave)
                && grave.getOwnerType() == EntityType.PLAYER) {
                Player player = plugin.getServer().getPlayer(grave.getOwnerUUID());
                Location npcLocation = location.clone();

                if (player != null
                    && npcLocation.getWorld() != null
                    && npcLib.getGlobalNPC(plugin, uuid.toString()) == null) {
                    location.getBlock().setType(Material.AIR);
                    NPC.Pose pose = NPC.Pose.SWIMMING;

                    try {
                        pose = NPC.Pose.valueOf(plugin.getConfigString("playernpc.corpse.pose", grave));
                    }
                    catch (IllegalArgumentException ignored) {
                    }

                    if (pose == NPC.Pose.SWIMMING) {
                        npcLocation.add(0.5, -0.2, 0.5);
                    }

                    NPC.Global npc = npcLib.generateGlobalNPC(plugin, uuid.toString(), npcLocation);
                    NPC.Skin.Custom skin = NPC.Skin.Custom.getLoadedSkin(plugin, grave.getUUID().toString());

                    if (skin == null
                        && grave.getOwnerTexture() != null
                        && grave.getOwnerTextureSignature() != null
                        && grave.getOwnerName() != null) {
                        skin = NPC.Skin.Custom.createCustomSkin(plugin, grave.getUUID()
                                                                             .toString(), grave.getOwnerTexture(), grave.getOwnerTextureSignature());
                    }

                    npc.setSkin(skin);
                    npc.setPose(pose);
                    npc.setAutoCreate(true);
                    npc.setAutoShow(true);
                    npc.setCustomData("grave_uuid", grave.getUUID().toString());

                    npc.setCollidable(plugin.getConfigBool("playernpc.corpse.collide", grave));

                    if (plugin.getConfigBool("playernpc.corpse.armor", grave)) {
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

                    if (plugin.getConfigBool("playernpc.corpse.hand", grave)) {
                        if (grave.getEquipmentMap().containsKey(EquipmentSlot.HAND)) {
                            npc.setItemInRightHand(grave.getEquipmentMap().get(EquipmentSlot.HAND));
                        }

                        if (grave.getEquipmentMap().containsKey(EquipmentSlot.OFF_HAND)) {
                            npc.setItemInLeftHand(grave.getEquipmentMap().get(EquipmentSlot.OFF_HAND));
                        }
                    }

                    if (plugin.getConfigBool("playernpc.corpse.glow.enabled", grave)) {
                        try {
                            npc.setGlowing(true, ChatColor.valueOf(plugin.getConfigString("playernpc.corpse.glow.color", grave)));
                        }
                        catch (IllegalArgumentException ignored) {
                            npc.setGlowing(true);
                        }
                    }

                    npc.forceUpdate();
                    plugin.debugMessage("Spawning PlayerNPC NPC for "
                                        + grave.getUUID()
                                        + " at "
                                        + npcLocation.getWorld().getName()
                                        + ", "
                                        + (npcLocation.getBlockX() + 0.5)
                                        + "x, "
                                        + (npcLocation.getBlockY() + 0.5)
                                        + "y, "
                                        + (npcLocation.getBlockZ() + 0.5)
                                        + "z", 1);

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

    public void removeCorpse(@NotNull Map<EntityData, NPC.Global> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, NPC.Global> entry : entityDataMap.entrySet()) {
            npcLib.removeGlobalNPC(entry.getValue());
            entityDataList.add(entry.getKey());
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    private @NotNull Map<EntityData, NPC.Global> getEntityDataNPCMap(@NotNull List<EntityData> entityDataList) {
        Map<EntityData, NPC.Global> entityDataMap = new HashMap<>();

        for (EntityData entityData : entityDataList) {
            for (NPC.Global npc : npcLib.getAllGlobalNPCs()) {
                if (npc.hasCustomData("grave_uuid") && npc.getCustomData("grave_uuid")
                                                          .equals(entityData.getUUIDGrave().toString())) {
                    entityDataMap.put(entityData, npc);
                }
            }
        }

        return entityDataMap;
    }

}
