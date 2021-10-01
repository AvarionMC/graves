package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.ExperienceUtil;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EntityDeathListener implements Listener {
    private final Graves plugin;

    public EntityDeathListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Location location = livingEntity.getLocation();
        List<String> permissionList = livingEntity instanceof Player ? plugin.getPermissionList(livingEntity) : null;
        List<String> worldList = plugin.getConfig("world", livingEntity, permissionList)
                .getStringList("world");

        // Grave zombie
        if (plugin.getEntityManager().hasDataByte(livingEntity, "graveZombie")) {
            Grave grave = plugin.getEntityManager().getGraveFromEntityData(livingEntity);

            if (grave == null || !plugin.getConfig("zombie.drop", grave).getBoolean("zombie.drop")) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }

            return;
        }

        // Creature spawn reason
        if (livingEntity instanceof Creature) {
            List<String> spawnReasonList = plugin.getConfig("spawn.reason", livingEntity, permissionList)
                    .getStringList("spawn.reason");

            if (plugin.getEntityManager().hasDataString(livingEntity, "spawnReason")
                    && (!spawnReasonList.contains("ALL") && !spawnReasonList.contains(plugin.getEntityManager()
                    .getDataString(livingEntity, "spawnReason")))) {
                return;
            }
        }

        // Empty inventory
        if (event.getDrops().size() <= 0) {
            return;
        }

        // Enabled
        if (!plugin.getConfig("grave.enabled", livingEntity, permissionList).getBoolean("grave.enabled")) {
            return;
        }

        // World
        if (!worldList.contains("ALL") && !worldList.contains(livingEntity.getWorld().getName())) {
            return;
        }

        // Block Ignore
        if (plugin.getGraveManager().shouldIgnoreBlock(location.getBlock(), livingEntity, permissionList)) {
            plugin.getPlayerManager().sendMessage("message.ignore", livingEntity,
                    StringUtil.format(location.getBlock().getType().name()), location, permissionList);

            return;
        }

        // WorldGuard
        if (plugin.hasWorldGuard()) {
            if (livingEntity instanceof Player) {
                if (!plugin.getFlagManager().canCreateGrave((Player) livingEntity, location)) {
                    plugin.getPlayerManager().sendMessage("message.worldguard-create-deny",
                            livingEntity, location, permissionList);

                    return;
                }
            } else {
                if (!plugin.getFlagManager().canCreateGrave(location)) {
                    return;
                }
            }
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
                return;
            }
        }

        // Max
        if (plugin.getGraveManager().getGraveList(livingEntity).size()
                >= plugin.getConfig("grave.max", livingEntity, permissionList).getInt("grave.max")) {
            plugin.getPlayerManager().sendMessage("message.max", livingEntity,
                    livingEntity.getLocation(), permissionList);

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
                    plugin.getPlayerManager().sendMessage("message.no-token", livingEntity,
                            livingEntity.getLocation(), permissionList);

                    return;
                }
            }
        }

        // Drops
        List<ItemStack> graveItemStackList = new ArrayList<>();
        Iterator<ItemStack> iterator = event.getDrops().iterator();

        while (iterator.hasNext()) {
            ItemStack itemStack = iterator.next();

            // Ignore compass
            if (itemStack.getType() == Material.COMPASS && plugin.getPlayerManager()
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

        // Grave
        if (!graveItemStackList.isEmpty()) {
            Grave grave = plugin.getGraveManager().createGrave(livingEntity, graveItemStackList, permissionList);

            // Player
            if (livingEntity instanceof Player) {
                Player player = (Player) livingEntity;

                // Has permission
                if (!player.hasPermission("graves.place")) {
                    return;
                }

                // Can build
                if (plugin.getConfig("placement.can-build", grave).getBoolean("placement.can-build")
                        && !plugin.getCompatibility().canBuild(player, player.getLocation(), plugin)) {
                    plugin.getPlayerManager().sendMessage("message.build-denied", player,
                            player.getLocation(), grave);
                    return;
                }
            }

            if (plugin.getConfig("experience.store", grave).getBoolean("experience.store")) {
                float percent = (float) plugin.getConfig("experience.store-percent", grave)
                        .getDouble("experience.store-percent");

                if (livingEntity instanceof Player) {
                    Player player = (Player) livingEntity;

                    if (player.hasPermission("graves.experience")) {
                        grave.setExperience(ExperienceUtil.getDropPercent(ExperienceUtil
                                .getPlayerExperience(player), percent));
                    } else {
                        grave.setExperience(event.getDroppedExp());
                    }
                } else {
                    grave.setExperience(ExperienceUtil.getDropPercent(event.getDroppedExp(), percent));
                }
            } else {
                grave.setExperience(event.getDroppedExp());
            }

            if (livingEntity instanceof Player) {
                Player player = (Player) livingEntity;

                // Experience
                if (player.hasPermission("graves.experience") && plugin.getConfig("expStore", grave)
                        .getBoolean("expStore")) {
                    grave.setExperience(ExperienceUtil.getPlayerDropExperience(player,
                            (float) plugin.getConfig("expStorePercent", grave)
                                    .getDouble("expStorePercent")));
                } else {
                    grave.setExperience(event.getDroppedExp());
                }

                if (event instanceof PlayerDeathEvent) {
                    ((PlayerDeathEvent) event).setKeepLevel(false);
                }
            } else {
                grave.setExperience(event.getDroppedExp());
            }

            // Killer
            if (livingEntity.getKiller() != null) {
                grave.setKillerType(EntityType.PLAYER);
                grave.setKillerName(livingEntity.getKiller().getName());
                grave.setKillerUUID(livingEntity.getKiller().getUniqueId());
            } else if (livingEntity.getLastDamageCause() != null) {
                EntityDamageEvent entityDamageEvent = livingEntity.getLastDamageCause();

                if (entityDamageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                        && entityDamageEvent instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) entityDamageEvent;

                    grave.setKillerUUID(entityDamageByEntityEvent.getDamager().getUniqueId());
                    grave.setKillerType(entityDamageByEntityEvent.getDamager().getType());
                    grave.setKillerName(plugin.getEntityManager().getEntityName(entityDamageByEntityEvent.getDamager()));
                } else {
                    grave.setKillerUUID(null);
                    grave.setKillerType(null);
                    grave.setKillerName(StringUtil.format(entityDamageEvent.getCause().name()));
                }
            }

            if (plugin.getConfig("protection.enabled", grave).getBoolean("protection.enabled")) {
                grave.setProtection(true);
                grave.setTimeProtection(plugin.getConfig("protection.time", grave)
                        .getInt("protection.time") * 1000L);
            }

            grave.setTimeAlive(plugin.getConfig("grave.time", grave).getInt("grave.time") * 1000L);
            grave.setOwnerTexture(SkinUtil.getTextureBase64(livingEntity, plugin));
            grave.setYaw(livingEntity.getLocation().getYaw());
            grave.setPitch(livingEntity.getLocation().getPitch());
            grave.setPermissionList(permissionList);

            location = plugin.getLocationManager().getSafeGraveLocation(livingEntity,
                    livingEntity.getLocation(), grave, plugin);

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
                    event.setDroppedExp(0);
                    plugin.getGraveManager().placeGrave(location, grave);
                    plugin.getPlayerManager().runCommands("command.create", livingEntity, location, grave);
                    plugin.getPlayerManager().sendMessage("message.death", livingEntity, location, grave);
                    plugin.getDataManager().addGrave(grave);
                } else {
                    event.getDrops().addAll(graveItemStackList);
                }
            } else {
                event.getDrops().addAll(graveItemStackList);
                plugin.getPlayerManager().sendMessage("message.failure", livingEntity,
                        livingEntity.getLocation(), grave);
                plugin.debugMessage("Safe location not found " + plugin.getGraveManager());
            }
        } else {
            plugin.debugMessage("Grave not being created for " + plugin.getEntityManager()
                    .getEntityName(livingEntity) + " because they had no drops");
        }
    }
}