package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.ExperienceUtil;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.SkinUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.*;

public class EntityDeathListener implements Listener {
    private final Graves plugin;
    private final Map<UUID, List<ItemStack>> removedItemStackMap;

    public EntityDeathListener(Graves plugin) {
        this.plugin = plugin;
        this.removedItemStackMap = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDeathLowest(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            removedItemStackMap.put(event.getEntity().getUniqueId(), new ArrayList<>(event.getDrops()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeathMonitor(EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        String entityName = plugin.getEntityManager().getEntityName(livingEntity);
        Location location = LocationUtil.roundLocation(livingEntity.getLocation());
        List<String> permissionList = livingEntity instanceof Player ? plugin.getPermissionList(livingEntity) : null;
        List<String> worldList = plugin.getConfig("world", livingEntity, permissionList)
                .getStringList("world");
        List<ItemStack> removedItemStackList = new ArrayList<>();

        // Removed items
        if (removedItemStackMap.containsKey(livingEntity.getUniqueId())) {
            removedItemStackList.addAll(removedItemStackMap.get(livingEntity.getUniqueId()));
            removedItemStackMap.remove(livingEntity.getUniqueId());
        }

        // Grave zombie
        if (plugin.getEntityManager().hasDataByte(livingEntity, "graveZombie")) {
            Grave grave = plugin.getEntityManager().getGraveFromEntityData(livingEntity);

            if (grave == null || !plugin.getConfig("zombie.drop", grave).getBoolean("zombie.drop")) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }

            plugin.debugMessage("Grave not created for " + entityName
                    + " because they were a grave zombie", 2);

            return;
        }

        // Permissions
        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;

            if (!player.hasPermission("graves.place")) {
                plugin.debugMessage("Grave not created for " + entityName
                        + " because they don't have permission to place graves", 2);

                return;
            } else if (player.hasPermission("essentials.keepinv")) {
                plugin.debugMessage(entityName
                        + " has essentials.keepinv", 2);
            }
        }

        // Enabled
        if (!plugin.getConfig("grave.enabled", livingEntity, permissionList).getBoolean("grave.enabled")) {
            if (livingEntity instanceof Player) {
                plugin.debugMessage("Grave not created for " + entityName
                        + " because they have graves disabled", 2);
            }

            return;
        }

        // Empty inventory
        if (event.getDrops().size() <= 0) {
            plugin.debugMessage("Grave not created for " + entityName
                    + " because they had an empty inventory", 2);

            return;
        }

        // Keep inventory
        if (event instanceof PlayerDeathEvent && ((PlayerDeathEvent) event).getKeepInventory()) {
            plugin.debugMessage("Grave not created for " + entityName
                    + " because they had keep inventory", 2);

            return;
        }

        // Creature spawn reason
        if (livingEntity instanceof Creature) {
            List<String> spawnReasonList = plugin.getConfig("spawn.reason", livingEntity, permissionList)
                    .getStringList("spawn.reason");

            if (plugin.getEntityManager().hasDataString(livingEntity, "spawnReason")
                    && (!spawnReasonList.contains("ALL") && !spawnReasonList.contains(plugin.getEntityManager()
                    .getDataString(livingEntity, "spawnReason")))) {
                plugin.debugMessage("Grave not created for " + entityName
                        + " because they had an invalid spawn reason", 2);

                return;
            }
        }

        // World
        if (!worldList.contains("ALL") && !worldList.contains(livingEntity.getWorld().getName())) {
            plugin.debugMessage("Grave not created for " + entityName
                    + " because they are not in a valid world", 2);

            return;
        }

        // Block Ignore
        if (plugin.getGraveManager().shouldIgnoreBlock(location.getBlock(), livingEntity, permissionList)) {
            plugin.getEntityManager().sendMessage("message.ignore", livingEntity,
                    StringUtil.format(location.getBlock().getType().name()), location, permissionList);

            return;
        }

        // WorldGuard
        if (plugin.getIntegrationManager().hasWorldGuard()) {
            boolean hasCreateGrave = plugin.getIntegrationManager().getWorldGuard().hasCreateGrave(location);

            if (hasCreateGrave) {
                if (livingEntity instanceof Player) {
                    if (!plugin.getIntegrationManager().getWorldGuard().canCreateGrave(livingEntity, location)) {
                        plugin.getEntityManager().sendMessage("message.region-create-deny",
                                livingEntity, location, permissionList);
                        plugin.debugMessage("Grave not created for " + entityName
                                + " because they are in a region with graves-create set to deny", 2);

                        return;
                    }
                } else if (!plugin.getIntegrationManager().getWorldGuard().canCreateGrave(location)) {
                    plugin.debugMessage("Grave not created for " + entityName
                            + " because they are in a region with graves-create set to deny", 2);

                    return;
                }
            } else if (!plugin.getLocationManager().canBuild(livingEntity, location, permissionList)) {
                plugin.getEntityManager().sendMessage("message.build-denied",
                        livingEntity, location, permissionList);
                plugin.debugMessage("Grave not created for " + entityName
                        + " because they don't have permission to build where they died", 2);

                return;
            }
        } else if (!plugin.getLocationManager().canBuild(livingEntity, location, permissionList)) {
            plugin.getEntityManager().sendMessage("message.build-denied",
                    livingEntity, location, permissionList);
            plugin.debugMessage("Grave not created for " + entityName
                    + " because they don't have permission to build where they died", 2);

            return;
        }

        // PvP, PvE, Environmental
        if (livingEntity.getLastDamageCause() != null) {
            EntityDamageEvent.DamageCause damageCause = livingEntity.getLastDamageCause().getCause();
            List<String> damageCauseList = plugin.getConfig("death.reason", livingEntity, permissionList)
                    .getStringList("death.reason");

            if (!damageCauseList.contains("ALL") && !damageCauseList.contains(damageCause.name())
                    && (damageCause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    && ((livingEntity.getKiller() != null
                    && !plugin.getConfig("death.player", livingEntity, permissionList)
                    .getBoolean("death.player"))
                    || (livingEntity.getKiller() == null
                    && !plugin.getConfig("death.entity", livingEntity, permissionList)
                    .getBoolean("death.entity")))
                    || (damageCause != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    && !plugin.getConfig("death.environmental", livingEntity, permissionList)
                    .getBoolean("death.environmental")))) {
                plugin.debugMessage("Grave not created for " + entityName
                        + " because they died to an invalid damage cause", 2);

                return;
            }
        }

        // Max
        if (plugin.getGraveManager().getGraveList(livingEntity).size()
                >= plugin.getConfig("grave.max", livingEntity, permissionList).getInt("grave.max")) {
            plugin.getEntityManager().sendMessage("message.max", livingEntity,
                    livingEntity.getLocation(), permissionList);
            plugin.debugMessage("Grave not created for " + entityName
                    + " because they reached maximum graves", 2);

            return;
        }

        // Token
        if (plugin.getVersionManager().hasPersistentData()
                && plugin.getConfig("token.enabled", livingEntity, permissionList)
                .getBoolean("token.enabled")) {
            String name = plugin.getConfig("token.name", livingEntity).getString("token.name", "basic");

            if (plugin.getConfig().isConfigurationSection("settings.token." + name)) {
                ItemStack itemStack = plugin.getRecipeManager().getGraveTokenFromPlayer(name, event.getDrops());

                if (itemStack != null) {
                    itemStack.setAmount(itemStack.getAmount() - 1);
                } else {
                    plugin.getEntityManager().sendMessage("message.no-token", livingEntity,
                            livingEntity.getLocation(), permissionList);
                    plugin.debugMessage("Grave not created for " + entityName
                            + " because they did not have a grave token", 2);

                    return;
                }
            }
        }

        // Drops
        List<ItemStack> graveItemStackList = new ArrayList<>();
        Iterator<ItemStack> iterator = event.getDrops().iterator();

        while (iterator.hasNext()) {
            ItemStack itemStack = iterator.next();

            if (itemStack != null) {
                // Ignore compass
                if (itemStack.getType() == Material.COMPASS && plugin.getEntityManager()
                        .getUUIDFromItemStack(itemStack) != null) {
                    if (plugin.getConfig("compass.destroy", livingEntity, permissionList)
                            .getBoolean("compass.destroy")) {
                        iterator.remove();

                        continue;
                    } else if (plugin.getConfig("compass.ignore", livingEntity, permissionList)
                            .getBoolean("compass.ignore")) {
                        continue;
                    }
                }

                // Ignored drops
                if (!plugin.getGraveManager().shouldIgnoreItemStack(itemStack, livingEntity, permissionList)) {
                    graveItemStackList.add(itemStack);
                    iterator.remove();
                }
            }
        }

        // Grave
        if (!graveItemStackList.isEmpty()) {
            Grave grave = new Grave(UUID.randomUUID());

            grave.setOwnerType(livingEntity.getType());
            grave.setOwnerName(entityName);
            grave.setOwnerNameDisplay(livingEntity instanceof Player ? ((Player) livingEntity).getDisplayName()
                    : livingEntity.getCustomName());
            grave.setOwnerUUID(livingEntity.getUniqueId());
            grave.setPermissionList(permissionList);
            grave.setOwnerTexture(SkinUtil.getTextureBase64(livingEntity, plugin));
            grave.setYaw(livingEntity.getLocation().getYaw());
            grave.setPitch(livingEntity.getLocation().getPitch());
            grave.setTimeAlive(plugin.getConfig("grave.time", grave).getInt("grave.time") * 1000L);

            plugin.debugMessage("Creating grave " + grave.getUUID() + " for entity " + entityName, 1);

            // Experience
            float percent = (float) plugin.getConfig("experience.store", grave)
                    .getDouble("experience.store");
            if (percent >= 0) {

                if (livingEntity instanceof Player) {
                    Player player = (Player) livingEntity;

                    if (player.hasPermission("graves.experience")) {
                        grave.setExperience(ExperienceUtil.getDropPercent(ExperienceUtil
                                .getPlayerExperience(player), percent));
                    } else {
                        grave.setExperience(event.getDroppedExp());
                    }

                    if (event instanceof PlayerDeathEvent) {
                        ((PlayerDeathEvent) event).setKeepLevel(false);
                    }
                } else {
                    grave.setExperience(ExperienceUtil.getDropPercent(event.getDroppedExp(), percent));
                }
            } else {
                grave.setExperience(event.getDroppedExp());
            }

            // Killer
            if (livingEntity.getKiller() != null) {
                grave.setKillerType(EntityType.PLAYER);
                grave.setKillerName(livingEntity.getKiller().getName());
                grave.setKillerNameDisplay(livingEntity.getKiller().getDisplayName());
                grave.setKillerUUID(livingEntity.getKiller().getUniqueId());
            } else if (livingEntity.getLastDamageCause() != null) {
                EntityDamageEvent entityDamageEvent = livingEntity.getLastDamageCause();

                if (entityDamageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                        && entityDamageEvent instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) entityDamageEvent;

                    grave.setKillerUUID(entityDamageByEntityEvent.getDamager().getUniqueId());
                    grave.setKillerType(entityDamageByEntityEvent.getDamager().getType());
                    grave.setKillerName(plugin.getEntityManager().getEntityName(entityDamageByEntityEvent
                            .getDamager()));
                } else {
                    grave.setKillerUUID(null);
                    grave.setKillerType(null);
                    grave.setKillerName(plugin.getGraveManager().getDamageReason(entityDamageEvent.getCause(), grave));
                }

                grave.setKillerNameDisplay(grave.getKillerName());
            }

            if (plugin.getConfig("protection.enabled", grave).getBoolean("protection.enabled")) {
                grave.setProtection(true);
                grave.setTimeProtection(plugin.getConfig("protection.time", grave)
                        .getInt("protection.time") * 1000L);
            }

            location = plugin.getLocationManager().getSafeGraveLocation(livingEntity, location, grave);

            // Location
            if (location != null) {
                int offsetX = plugin.getConfig("placement.offset.x", grave).getInt("placement.offset.x");
                int offsetY = plugin.getConfig("placement.offset.y", grave).getInt("placement.offset.y");
                int offsetZ = plugin.getConfig("placement.offset.z", grave).getInt("placement.offset.z");

                location.add(offsetX, offsetY, offsetZ);
                grave.setLocationDeath(location);

                GraveCreateEvent graveCreateEvent = new GraveCreateEvent(livingEntity, grave);

                plugin.getServer().getPluginManager().callEvent(graveCreateEvent);

                if (!graveCreateEvent.isCancelled()) {
                    if (event.getEntityType() != EntityType.PLAYER || event instanceof PlayerDeathEvent) {
                        graveItemStackList = plugin.getGraveManager().getGraveItemStackList(graveItemStackList,
                                removedItemStackList, livingEntity, permissionList);
                        String title = StringUtil.parseString(plugin
                                        .getConfig("gui.grave.title", livingEntity, permissionList)
                                        .getString("gui.grave.title"), livingEntity,
                                livingEntity.getLocation(), grave, plugin);
                        Grave.StorageMode storageMode = plugin.getGraveManager()
                                .getStorageMode(plugin.getConfig("storage.mode", livingEntity, permissionList)
                                        .getString("storage.mode"));

                        event.setDroppedExp(0);
                        grave.setInventory(plugin.getGraveManager().createGraveInventory(grave, livingEntity
                                .getLocation(), graveItemStackList, title, storageMode));
                        plugin.getGraveManager().placeGrave(location, grave);
                        plugin.getEntityManager().runCommands("event.command.create", livingEntity, location, grave);
                        plugin.getEntityManager().sendMessage("message.death", livingEntity, location, grave);
                        plugin.getDataManager().addGrave(grave);
                    } else {
                        event.getDrops().clear();
                        event.setDroppedExp(0);
                    }
                } else {
                    event.getDrops().addAll(graveItemStackList);
                }
            } else {
                event.getDrops().addAll(graveItemStackList);
                plugin.getEntityManager().sendMessage("message.failure", livingEntity,
                        livingEntity.getLocation(), grave);
                plugin.debugMessage("Grave not created for " + entityName
                        + " because a safe location could not be found", 2);
            }
        } else {
            plugin.debugMessage("Grave not created for " + entityName + " because they had no drops", 2);
        }
    }
}