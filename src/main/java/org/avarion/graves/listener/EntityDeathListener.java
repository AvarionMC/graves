package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.data.BlockData;
import org.avarion.graves.event.GraveBlockPlaceEvent;
import org.avarion.graves.event.GraveCreateEvent;
import org.avarion.graves.manager.CacheManager;
import org.avarion.graves.type.Grave;
import org.avarion.graves.type.Graveyard;
import org.avarion.graves.util.*;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EntityDeathListener implements Listener {

    private final Graves plugin;

    public EntityDeathListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        String entityName = plugin.getEntityManager().getEntityName(livingEntity);
        Location location = LocationUtil.roundLocation(livingEntity.getLocation());
        List<String> permissionList = livingEntity instanceof Player ? plugin.getPermissionList(livingEntity) : null;
        List<String> worldList = plugin.getConfigStringList("world", livingEntity, permissionList);
        List<ItemStack> removedItemStackList = new ArrayList<>();

        // Removed items
        if (CacheManager.removedItemStackMap.containsKey(livingEntity.getUniqueId())) {
            removedItemStackList.addAll(CacheManager.removedItemStackMap
                                              .get(livingEntity.getUniqueId()));
            CacheManager.removedItemStackMap.remove(livingEntity.getUniqueId());
        }

        // Mohist
        if (event.getEntityType() == EntityType.PLAYER && !(event instanceof PlayerDeathEvent)) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            return;
        }

        // Grave zombie
        if (plugin.getEntityManager().hasDataByte(livingEntity, "graveZombie")) {
            EntityType zombieGraveEntityType = plugin.getEntityManager().hasDataString(livingEntity, "graveEntityType")
                                               ? EntityType.valueOf(plugin.getEntityManager()
                                                                          .getDataString(livingEntity, "graveEntityType"))
                                               : EntityType.PLAYER;
            List<String> zombieGravePermissionList = plugin.getEntityManager()
                                                           .hasDataString(livingEntity, "gravePermissionList")
                                                     ? Arrays.asList(plugin.getEntityManager()
                                                                           .getDataString(livingEntity, "gravePermissionList")
                                                                           .split("\\|"))
                                                     : null;

            if (!plugin.getConfigBool("zombie.drop", zombieGraveEntityType, zombieGravePermissionList)) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }

            return;
        }

        // Player
        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;

            if (plugin.getGraveyardManager().isModifyingGraveyard(player)) {
                plugin.getGraveyardManager().stopModifyingGraveyard(player);
            }

            if (!player.hasPermission("graves.place")) {
                plugin.debugMessage("Grave not created for "
                                    + entityName
                                    + " because they don't have permission to place graves", 2);

                return;
            }
            else if (player.hasPermission("essentials.keepinv")) {
                plugin.debugMessage(entityName + " has essentials.keepinv", 2);
            }
        }

        // Enabled
        if (!plugin.getConfigBool("grave.enabled", livingEntity, permissionList)) {
            if (livingEntity instanceof Player) {
                plugin.debugMessage("Grave not created for " + entityName + " because they have graves disabled", 2);
            }

            return;
        }

        // Keep inventory
        if (event instanceof PlayerDeathEvent) {
            try {
                if (((PlayerDeathEvent) event).getKeepInventory()) {
                    plugin.debugMessage("Grave not created for " + entityName + " because they had keep inventory", 2);

                    return;
                }
            }
            catch (NoSuchMethodError ignored) {
            }
        }

        // Empty inventory
        if (event.getDrops().size() <= 0) {
            plugin.debugMessage("Grave not created for " + entityName + " because they had an empty inventory", 2);

            return;
        }

        // Creature spawn reason
        if (livingEntity instanceof Creature) {
            List<String> spawnReasonList = plugin.getConfigStringList("spawn.reason", livingEntity, permissionList);

            if (plugin.getEntityManager().hasDataString(livingEntity, "spawnReason")
                && (!spawnReasonList.contains("ALL") && !spawnReasonList.contains(plugin.getEntityManager()
                                                                                        .getDataString(livingEntity, "spawnReason")))) {
                plugin.debugMessage("Grave not created for "
                                    + entityName
                                    + " because they had an invalid spawn reason", 2);

                return;
            }
        }

        // World
        if (!worldList.contains("ALL") && !worldList.contains(livingEntity.getWorld().getName())) {
            plugin.debugMessage("Grave not created for " + entityName + " because they are not in a valid world", 2);

            return;
        }

        // Ignore
        if (plugin.getGraveManager().shouldIgnoreBlock(location.getBlock(), livingEntity, permissionList)) {
            plugin.getEntityManager()
                  .sendMessage("message.ignore", livingEntity, StringUtil.format(location.getBlock()
                                                                                         .getType()
                                                                                         .name()), location, permissionList);

            return;
        }

        // WorldGuard
        if (plugin.getIntegrationManager().hasWorldGuard()) {
            boolean hasCreateGrave = plugin.getIntegrationManager().getWorldGuard().hasCreateGrave(location);

            if (hasCreateGrave) {
                if (livingEntity instanceof Player) {
                    if (!plugin.getIntegrationManager().getWorldGuard().canCreateGrave(livingEntity, location)) {
                        plugin.getEntityManager()
                              .sendMessage("message.region-create-deny", livingEntity, location, permissionList);
                        plugin.debugMessage("Grave not created for "
                                            + entityName
                                            + " because they are in a region with graves-create set to deny", 2);

                        return;
                    }
                }
                else if (!plugin.getIntegrationManager().getWorldGuard().canCreateGrave(location)) {
                    plugin.debugMessage("Grave not created for "
                                        + entityName
                                        + " because they are in a region with graves-create set to deny", 2);

                    return;
                }
            }
            else if (!plugin.getLocationManager().canBuild(livingEntity, location, permissionList)) {
                plugin.getEntityManager().sendMessage("message.build-denied", livingEntity, location, permissionList);
                plugin.debugMessage("Grave not created for "
                                    + entityName
                                    + " because they don't have permission to build where they died", 2);

                return;
            }
        }
        else if (!plugin.getLocationManager().canBuild(livingEntity, location, permissionList)) {
            plugin.getEntityManager().sendMessage("message.build-denied", livingEntity, location, permissionList);
            plugin.debugMessage("Grave not created for "
                                + entityName
                                + " because they don't have permission to build where they died", 2);

            return;
        }

        // PvP, PvE, Environmental
        if (livingEntity.getLastDamageCause() != null) {
            EntityDamageEvent.DamageCause damageCause = livingEntity.getLastDamageCause().getCause();
            List<String> damageCauseList = plugin.getConfigStringList("death.reason", livingEntity, permissionList);

            if (!damageCauseList.contains("ALL") && !damageCauseList.contains(damageCause.name()) && (damageCause
                                                                                                      == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                                                                                                      && ((livingEntity.getKiller()
                                                                                                           != null
                                                                                                           && !plugin.getConfigBool("death.player", livingEntity, permissionList))
                                                                                                          || (livingEntity.getKiller()
                                                                                                              == null
                                                                                                              && !plugin.getConfigBool("death.entity", livingEntity, permissionList)))
                                                                                                      || (damageCause
                                                                                                          != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                                                                                                          && !plugin.getConfigBool("death.environmental", livingEntity, permissionList)))) {
                plugin.debugMessage("Grave not created for "
                                    + entityName
                                    + " because they died to an invalid damage cause", 2);

                return;
            }
        }

        // Max
        if (plugin.getGraveManager().getGraveList(livingEntity).size()
            >= plugin.getConfigInt("grave.max", livingEntity, permissionList)) {
            plugin.getEntityManager()
                  .sendMessage("message.max", livingEntity, livingEntity.getLocation(), permissionList);
            plugin.debugMessage("Grave not created for " + entityName + " because they reached maximum graves", 2);

            return;
        }

        // Token
        if (plugin.getConfigBool("token.enabled", livingEntity, permissionList)) {
            String name = plugin.getConfigString("token.name", livingEntity, "basic");

            if (plugin.getConfig().isConfigurationSection("settings.token." + name)) {
                ItemStack itemStack = plugin.getRecipeManager().getGraveTokenFromPlayer(name, event.getDrops());

                if (itemStack != null) {
                    itemStack.setAmount(itemStack.getAmount() - 1);
                }
                else {
                    plugin.getEntityManager()
                          .sendMessage("message.no-token", livingEntity, livingEntity.getLocation(), permissionList);
                    plugin.debugMessage("Grave not created for "
                                        + entityName
                                        + " because they did not have a grave token", 2);

                    return;
                }
            }
        }

        // Drops
        List<ItemStack> graveItemStackList = new ArrayList<>();
        List<ItemStack> eventItemStackList = new ArrayList<>(event.getDrops());
        List<ItemStack> dropItemStackList = new ArrayList<>(eventItemStackList);
        Iterator<ItemStack> dropItemStackListIterator = dropItemStackList.iterator();
        int droppedExp = event.getDroppedExp();

        // Iterator
        while (dropItemStackListIterator.hasNext()) {
            ItemStack itemStack = dropItemStackListIterator.next();

            if (itemStack != null) {
                // Ignore compass
                if (plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null) {
                    if (plugin.getConfigBool("compass.destroy", livingEntity, permissionList)) {
                        dropItemStackListIterator.remove();
                        event.getDrops().remove(itemStack);

                        continue;
                    }
                    else if (plugin.getConfigBool("compass.ignore", livingEntity, permissionList)) {
                        continue;
                    }
                }

                if (!plugin.getGraveManager().shouldIgnoreItemStack(itemStack, livingEntity, permissionList)) {
                    graveItemStackList.add(itemStack);
                    dropItemStackListIterator.remove();
                }
            }
        }

        // Grave
        if (!graveItemStackList.isEmpty()) {
            Grave grave = new Grave(UUID.randomUUID());

            grave.setOwnerType(livingEntity.getType());
            grave.setOwnerName(entityName);
            grave.setOwnerNameDisplay(livingEntity instanceof Player
                                      ? ((Player) livingEntity).getDisplayName()
                                      : grave.getOwnerName());
            grave.setOwnerUUID(livingEntity.getUniqueId());
            grave.setPermissionList(permissionList);
            grave.setYaw(livingEntity.getLocation().getYaw());
            grave.setPitch(livingEntity.getLocation().getPitch());
            grave.setTimeAlive(plugin.getConfigInt("grave.time", grave) * 1000L);

            // Skin
            grave.setOwnerTexture(SkinUtil.getTexture(livingEntity));
            grave.setOwnerTextureSignature(SkinUtil.getSignature(livingEntity));

            // Experience
            double experiencePercent = plugin.getConfigDbl("experience.store", grave);

            if (experiencePercent >= 0) {
                if (livingEntity instanceof Player player) {

                    if (player.hasPermission("graves.experience")) {
                        grave.setExperience(ExperienceUtil.getDropPercent(ExperienceUtil.getPlayerExperience(player), experiencePercent));
                    }
                    else {
                        grave.setExperience(event.getDroppedExp());
                    }

                    if (event instanceof PlayerDeathEvent) {
                        ((PlayerDeathEvent) event).setKeepLevel(false);
                    }
                }
                else {
                    grave.setExperience(ExperienceUtil.getDropPercent(event.getDroppedExp(), experiencePercent));
                }
            }
            else {
                grave.setExperience(event.getDroppedExp());
            }

            // Killer
            if (livingEntity.getKiller() != null) {
                grave.setKillerType(EntityType.PLAYER);
                grave.setKillerName(livingEntity.getKiller().getName());
                grave.setKillerNameDisplay(livingEntity.getKiller().getDisplayName());
                grave.setKillerUUID(livingEntity.getKiller().getUniqueId());
            }
            else if (livingEntity.getLastDamageCause() != null) {
                EntityDamageEvent entityDamageEvent = livingEntity.getLastDamageCause();

                if (entityDamageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    && entityDamageEvent instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {

                    grave.setKillerUUID(entityDamageByEntityEvent.getDamager().getUniqueId());
                    grave.setKillerType(entityDamageByEntityEvent.getDamager().getType());
                    grave.setKillerName(plugin.getEntityManager()
                                              .getEntityName(entityDamageByEntityEvent.getDamager()));
                }
                else {
                    grave.setKillerUUID(null);
                    grave.setKillerType(null);
                    grave.setKillerName(plugin.getGraveManager().getDamageReason(entityDamageEvent.getCause(), grave));
                }

                grave.setKillerNameDisplay(grave.getKillerName());
            }

            // Protection
            if (plugin.getConfigBool("protection.enabled", grave)) {
                grave.setProtection(true);
                grave.setTimeProtection(plugin.getConfigInt("protection.time", grave) * 1000L);
            }

            GraveCreateEvent graveCreateEvent = new GraveCreateEvent(livingEntity, grave);

            plugin.getServer().getPluginManager().callEvent(graveCreateEvent);

            // Event create
            if (!graveCreateEvent.isCancelled()) {
                Map<Location, BlockData.BlockType> locationMap = new HashMap<>();
                Location safeLocation = plugin.getLocationManager().getSafeGraveLocation(livingEntity, location, grave);

                event.getDrops().clear();
                event.getDrops().addAll(dropItemStackList);
                event.setDroppedExp(0);
                grave.setLocationDeath(safeLocation != null ? safeLocation : location);
                grave.getLocationDeath().setYaw(grave.getYaw());
                grave.getLocationDeath().setPitch(grave.getPitch());

                // Graveyard
                if (plugin.getConfigBool("graveyard.enabled", grave)) {
                    Graveyard graveyard = plugin.getGraveyardManager()
                                                .getClosestGraveyard(grave.getLocationDeath(), livingEntity);

                    if (graveyard != null) {
                        Map<Location, BlockFace> graveyardFreeSpaces = plugin.getGraveyardManager()
                                                                             .getGraveyardFreeSpaces(graveyard);

                        if (!graveyardFreeSpaces.isEmpty()) {
                            if (plugin.getConfigBool("graveyard.death", grave)) {
                                locationMap.put(grave.getLocationDeath(), BlockData.BlockType.DEATH);
                            }

                            Map.Entry<Location, BlockFace> entry = graveyardFreeSpaces.entrySet().iterator().next();

                            entry.getKey()
                                 .setYaw(plugin.getConfig().getBoolean("settings.graveyard.facing")
                                         ? BlockFaceUtil.getBlockFaceYaw(entry.getValue())
                                         : grave.getYaw());
                            entry.getKey().setPitch(grave.getPitch());
                            locationMap.put(entry.getKey(), BlockData.BlockType.GRAVEYARD);
                        }
                        else {
                            locationMap.put(grave.getLocationDeath(), BlockData.BlockType.DEATH);
                        }
                    }
                    else {
                        locationMap.put(grave.getLocationDeath(), BlockData.BlockType.DEATH);
                    }
                }
                else {
                    locationMap.put(grave.getLocationDeath(), BlockData.BlockType.DEATH);
                }

                // Obituary
                if (plugin.getConfigBool("obituary.enabled", grave)) {
                    graveItemStackList.add(plugin.getItemStackManager().getGraveObituary(grave));
                }

                // Skull
                if (plugin.getConfigBool("head.enabled", grave)
                    && Math.random() < plugin.getConfigDbl("head.percent", grave)
                    && grave.getOwnerTexture() != null
                    && grave.getOwnerTextureSignature() != null) {
                    graveItemStackList.add(plugin.getItemStackManager().getGraveHead(grave));
                }

                // Inventory
                grave.setInventory(plugin.getGraveManager()
                                         .getGraveInventory(grave, livingEntity, graveItemStackList, removedItemStackList, permissionList));

                // Equipment
                grave.setEquipmentMap(plugin.getEntityManager().getEquipmentMap(livingEntity, grave));

                // Placeable
                locationMap.entrySet()
                           .removeIf(entry -> plugin.getLocationManager().hasGrave(entry.getKey())
                                              || plugin.getLocationManager().isVoid(entry.getKey())
                                              || !plugin.getLocationManager().isInsideBorder(entry.getKey()));

                if (!locationMap.isEmpty()) {
                    plugin.getEntityManager()
                          .sendMessage("message.death", livingEntity, grave.getLocationDeath(), grave);
                    plugin.getEntityManager()
                          .runCommands("event.command.create", livingEntity, grave.getLocationDeath(), grave);
                    plugin.getDataManager().addGrave(grave);

                    if (plugin.getIntegrationManager().hasMultiPaper()) {
                        plugin.getIntegrationManager().getMultiPaper().notifyGraveCreation(grave);
                    }

                    // Location
                    for (Map.Entry<Location, BlockData.BlockType> entry : locationMap.entrySet()) {
                        location = entry.getKey().clone();

                        int offsetX = 0;
                        int offsetY = 0;
                        int offsetZ = 0;

                        switch (entry.getValue()) {
                            case DEATH -> {
                            }
                            case NORMAL -> {
                                offsetX = plugin.getConfigInt("placement.offset.x", grave);
                                offsetY = plugin.getConfigInt("placement.offset.y", grave);
                                offsetZ = plugin.getConfigInt("placement.offset.z", grave);
                            }
                            case GRAVEYARD -> {
                                offsetX = plugin.getConfig().getInt("settings.graveyard.offset.x");
                                offsetY = plugin.getConfig().getInt("settings.graveyard.offset.y");
                                offsetZ = plugin.getConfig().getInt("settings.graveyard.offset.z");
                            }
                        }

                        location.add(offsetX, offsetY, offsetZ);

                        GraveBlockPlaceEvent graveBlockPlaceEvent = new GraveBlockPlaceEvent(grave, location, entry.getValue());

                        plugin.getServer().getPluginManager().callEvent(graveBlockPlaceEvent);

                        if (!graveBlockPlaceEvent.isCancelled()) {
                            plugin.getGraveManager().placeGrave(graveBlockPlaceEvent.getLocation(), grave);
                            plugin.getEntityManager().sendMessage("message.block", livingEntity, location, grave);
                            plugin.getEntityManager()
                                  .runCommands("event.command.block", livingEntity, graveBlockPlaceEvent.getLocation(), grave);
                        }
                    }
                }
                else {
                    if (event instanceof PlayerDeathEvent playerDeathEvent
                        && plugin.getConfigBool("placement.failure-keep-inventory", grave)) {

                        try {
                            playerDeathEvent.setKeepLevel(true);
                            playerDeathEvent.setKeepInventory(true);
                            plugin.getEntityManager()
                                  .sendMessage("message.failure-keep-inventory", livingEntity, location, grave);
                        }
                        catch (NoSuchMethodError ignored) {
                        }
                    }
                    else {
                        event.getDrops().addAll(eventItemStackList);
                        event.setDroppedExp(droppedExp);
                        plugin.getEntityManager().sendMessage("message.failure", livingEntity, location, grave);
                    }
                }
            }
        }
        else {
            plugin.debugMessage("Grave not created for " + entityName + " because they had no drops", 2);
        }
    }

}
