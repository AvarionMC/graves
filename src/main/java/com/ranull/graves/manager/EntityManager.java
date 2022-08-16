package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.*;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.NumberConversions;

import java.util.*;

public final class EntityManager extends EntityDataManager {
    private final Graves plugin;

    public EntityManager(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
    }

    public void swingMainHand(Player player) {
        if (plugin.getVersionManager().hasSwingHand()) {
            player.swingMainHand();
        } else {
            ReflectionUtil.swingMainHand(player);
        }
    }

    public ItemStack createGraveCompass(Player player, Location location, Grave grave) {
        if (plugin.getVersionManager().hasPersistentData()) {
            Material material = Material.COMPASS;

            if (plugin.getConfig("compass.recovery", grave).getBoolean("compass.recovery")) {
                try {
                    material = Material.valueOf("RECOVERY_COMPASS");
                } catch (IllegalArgumentException ignored) {
                }
            }

            ItemStack itemStack = new ItemStack(material);
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (plugin.getVersionManager().hasCompassMeta() && itemMeta instanceof CompassMeta) {
                    CompassMeta compassMeta = (CompassMeta) itemMeta;

                    compassMeta.setLodestoneTracked(false);
                    compassMeta.setLodestone(location);
                } else if (itemStack.getType().name().equals("RECOVERY_COMPASS")) {
                    try {
                        player.setLastDeathLocation(location);
                    } catch (NoSuchMethodError ignored) {
                    }
                }

                List<String> loreList = new ArrayList<>();
                int customModelData = plugin.getConfig("compass.model-data", grave).getInt("compass.model-data", -1);

                if (customModelData > -1) {
                    itemMeta.setCustomModelData(customModelData);
                }

                if (plugin.getConfig("compass.glow", grave).getBoolean("compass.glow")) {
                    itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                itemMeta.setDisplayName(ChatColor.WHITE + StringUtil.parseString(plugin
                        .getConfig("compass.name", grave).getString("compass.name"), grave, plugin));
                itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "graveUUID"),
                        PersistentDataType.STRING, grave.getUUID().toString());

                for (String string : plugin.getConfig("compass.lore", grave).getStringList("compass.lore")) {
                    loreList.add(ChatColor.GRAY + StringUtil.parseString(string, location, grave, plugin));
                }

                itemMeta.setLore(loreList);
                itemStack.setItemMeta(itemMeta);
            }

            return itemStack;
        }

        return null;
    }

    public Map<ItemStack, UUID> getCompassesFromInventory(HumanEntity player) {
        Map<ItemStack, UUID> itemStackUUIDMap = new HashMap<>();

        if (plugin.getVersionManager().hasPersistentData()) {
            for (ItemStack itemStack : player.getInventory().getContents()) {
                UUID uuid = getGraveUUIDFromItemStack(itemStack);

                if (uuid != null) {
                    itemStackUUIDMap.put(itemStack, uuid);
                }
            }
        }

        return itemStackUUIDMap;
    }

    public UUID getGraveUUIDFromItemStack(ItemStack itemStack) {
        if (plugin.getVersionManager().hasPersistentData() && itemStack != null && itemStack.getItemMeta() != null) {
            if (itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "graveUUID"),
                    PersistentDataType.STRING)) {
                return UUIDUtil.getUUID(itemStack.getItemMeta().getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING));
            }
        }

        return null;
    }

    public void teleportEntity(Entity entity, Location location, Grave grave) {
        if (canTeleport(entity, location)) {
            location = LocationUtil.roundLocation(location);
            BlockFace blockFace = BlockFaceUtil.getYawBlockFace(grave.getYaw());
            Location locationTeleport = location.clone().getBlock().getRelative(blockFace).getRelative(blockFace)
                    .getLocation().add(0.5, 0, 0.5);

            if (plugin.getLocationManager().isLocationSafePlayer(locationTeleport)) {
                locationTeleport.setYaw(BlockFaceUtil.getBlockFaceYaw(blockFace.getOppositeFace()));
                locationTeleport.setPitch(20);
            } else {
                locationTeleport = plugin.getLocationManager()
                        .getSafeTeleportLocation(entity, location.add(0, 1, 0), grave, plugin);

                if (locationTeleport != null) {
                    locationTeleport.add(0.5, 0, 0.5);
                    locationTeleport.setYaw(BlockFaceUtil.getBlockFaceYaw(blockFace));
                    locationTeleport.setPitch(90);
                }
            }

            if (locationTeleport != null && locationTeleport.getWorld() != null) {
                if (entity instanceof Player) {
                    Player player = (Player) entity;

                    if (plugin.getIntegrationManager().hasVault()) {
                        double teleportCost = getTeleportCost(entity.getLocation(), locationTeleport, grave);

                        if (plugin.getIntegrationManager().getVault().hasBalance(player, teleportCost)
                                && plugin.getIntegrationManager().getVault().withdrawBalance(player, teleportCost)) {
                            player.teleport(locationTeleport);
                            plugin.getEntityManager().sendMessage("message.teleport", player, locationTeleport, grave);
                            plugin.getEntityManager().playPlayerSound("sound.teleport", player, locationTeleport, grave);
                        } else {
                            plugin.getEntityManager().sendMessage("message.no-money", player, player.getLocation(), grave);
                        }
                    } else {
                        player.teleport(locationTeleport);
                    }
                } else {
                    entity.teleport(locationTeleport);
                }
            } else {
                plugin.getEntityManager().sendMessage("message.teleport-failure", entity, location, grave);
            }
        } else {
            plugin.getEntityManager().sendMessage("message.region-teleport-deny", entity, location, grave);
        }
    }

    public double getTeleportCost(Location location1, Location location2, Grave grave) {
        double cost = plugin.getConfig("teleport.cost", grave).getDouble("teleport.cost");

        if (plugin.getConfig("teleport.cost", grave).isString("teleport.cost")) {
            String costString = StringUtil.parseString(plugin.getConfig("teleport.cost", grave)
                    .getString("teleport.cost"), location2, grave, plugin);

            try {
                cost = Double.parseDouble(costString);
            } catch (NumberFormatException ignored) {
                plugin.debugMessage(costString + " cost is not a double", 1);
            }
        }

        double costDifferentWorld = plugin.getConfig("teleport.cost-different-world", grave)
                .getDouble("teleport.cost-different-world");

        if (plugin.getConfig("teleport.cost-distance-increase", grave)
                .getBoolean("teleport.cost-distance-increase")) {
            double distance = Math.sqrt(NumberConversions.square(location1.getBlockX() - location2.getBlockX())
                    + NumberConversions.square(location1.getBlockZ() - location2.getBlockZ()));
            cost = Math.round(cost * (distance / 16));
        }

        if (location1.getWorld() != null && location2.getWorld() != null && costDifferentWorld > 0
                && !location1.getWorld().getName().equals(location2.getWorld().getName())) {
            cost += costDifferentWorld;
        }

        return cost;
    }

    public boolean canTeleport(Entity entity, Location location) {
        return (!plugin.getIntegrationManager().hasWorldGuard()
                || plugin.getIntegrationManager().getWorldGuard().canTeleport(entity, location))
                && (!plugin.getIntegrationManager().hasGriefDefender()
                || plugin.getIntegrationManager().getGriefDefender().canTeleport(entity, location));
    }

    public void playWorldSound(String string, Player player) {
        playWorldSound(string, player.getLocation(), null);
    }

    public void playWorldSound(String string, Player player, Grave grave) {
        playWorldSound(string, player.getLocation(), grave);
    }

    public void playWorldSound(String string, Location location, Grave grave) {
        playWorldSound(string, location, grave != null ? grave.getOwnerType() : null, grave != null
                ? grave.getPermissionList() : null, 1, 1);
    }

    public void playWorldSound(String string, Location location, EntityType entityType, List<String> permissionList,
                               float volume, float pitch) {
        if (location.getWorld() != null) {
            string = plugin.getConfig(string, entityType, permissionList).getString(string);

            if (string != null && !string.equals("")) {
                try {
                    location.getWorld().playSound(location, Sound.valueOf(string.toUpperCase()), volume, pitch);
                } catch (IllegalArgumentException exception) {
                    plugin.debugMessage(string.toUpperCase() + " is not a Sound ENUM", 1);
                }
            }
        }
    }

    public void playPlayerSound(String string, Entity entity, Grave grave) {
        playPlayerSound(string, entity, entity.getLocation(), grave.getPermissionList(), 1, 1);
    }

    public void playPlayerSound(String string, Entity entity, Location location, Grave grave) {
        playPlayerSound(string, entity, location, grave.getPermissionList(), 1, 1);
    }

    public void playPlayerSound(String string, Entity entity, List<String> permissionList) {
        playPlayerSound(string, entity, entity.getLocation(), permissionList, 1, 1);
    }

    public void playPlayerSound(String string, Entity entity, Location location, List<String> permissionList) {
        playPlayerSound(string, entity, location, permissionList, 1, 1);
    }

    public void playPlayerSound(String string, Entity entity, Location location, List<String> permissionList,
                                float volume, float pitch) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            string = plugin.getConfig(string, entity, permissionList).getString(string);

            if (string != null && !string.equals("")) {
                try {
                    player.playSound(location, Sound.valueOf(string.toUpperCase()), volume, pitch);
                } catch (IllegalArgumentException exception) {
                    plugin.debugMessage(string.toUpperCase() + " is not a Sound ENUM", 1);
                }
            }
        }
    }

    public void sendMessage(String string, CommandSender commandSender) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            sendMessage(string, player, player.getLocation(), null, plugin.getPermissionList(player));
        }
    }

    public void sendMessage(String string, Entity entity) {
        sendMessage(string, entity, entity.getLocation(), null, plugin.getPermissionList(entity));
    }

    public void sendMessage(String string, Entity entity, List<String> permissionList) {
        sendMessage(string, entity, entity.getLocation(), null, permissionList);
    }

    public void sendMessage(String string, Entity entity, Location location, List<String> permissionList) {
        sendMessage(string, entity, location, null, permissionList);
    }

    public void sendMessage(String string, Entity entity, Location location, Grave grave) {
        sendMessage(string, entity, location, grave, null);
    }

    public void sendMessage(String string, Entity entity, String name, Location location, List<String> permissionList) {
        sendMessage(string, entity, name, location, null, permissionList);
    }

    private void sendMessage(String string, Entity entity, Location location, Grave grave, List<String> permissionList) {
        sendMessage(string, entity, getEntityName(entity), location, grave, permissionList);
    }

    private void sendMessage(String string, Entity entity, String name, Location location, Grave grave, List<String> permissionList) {
        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (grave != null) {
                string = plugin.getConfig(string, grave).getString(string);
            } else {
                string = plugin.getConfig(string, entity.getType(), permissionList).getString(string);
            }

            String prefix = plugin.getConfig("message.prefix", entity.getType(), permissionList)
                    .getString("message.prefix");

            if (prefix != null && !prefix.equals("")) {
                string = prefix + string;
            }

            if (string != null && !string.equals("")) {
                player.sendMessage(StringUtil.parseString(string, entity, name, location, grave, plugin));
            }
        }
    }

    public void runCommands(String string, Entity entity, Location location, Grave grave) {
        runCommands(string, entity, null, location, grave);
    }

    public void runCommands(String string, String name, Location location, Grave grave) {
        runCommands(string, null, name, location, grave);
    }

    private void runCommands(String string, Entity entity, String name, Location location, Grave grave) {
        for (String command : plugin.getConfig(string, grave).getStringList(string)) {
            if (command != null && !command.equals("")) {
                runConsoleCommand(StringUtil.parseString(command, entity, name, location, grave, plugin));
            }
        }
    }

    private void runConsoleCommand(String string) {
        if (string != null && !string.equals("")) {
            ServerCommandEvent serverCommandEvent = new ServerCommandEvent(plugin.getServer().getConsoleSender(), string);

            plugin.getServer().getPluginManager().callEvent(serverCommandEvent);

            if ((plugin.getVersionManager().is_v1_7() || plugin.getVersionManager().is_v1_8())
                    || !serverCommandEvent.isCancelled()) {
                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> plugin.getServer()
                        .dispatchCommand(serverCommandEvent.getSender(), serverCommandEvent.getCommand()));
                plugin.debugMessage("Running console command " + string, 1);
            }
        }
    }

    public boolean runFunction(Entity entity, String function) {
        return runFunction(entity, function, null);
    }

    public boolean runFunction(Entity entity, String function, Grave grave) {
        switch (function.toLowerCase()) {
            case "list": {
                plugin.getGUIManager().openGraveList(entity);

                return true;
            }
            case "menu": {
                plugin.getGUIManager().openGraveMenu(entity, grave);

                return true;
            }
            case "teleport":
            case "teleportation": {
                if (plugin.getConfig("teleport.enabled", grave).getBoolean("teleport.enabled")
                        && (EntityUtil.hasPermission(entity, "graves.teleport")
                        || EntityUtil.hasPermission(entity, "graves.bypass"))) {
                    plugin.getEntityManager().teleportEntity(entity, plugin.getGraveManager()
                            .getGraveLocationList(entity.getLocation(), grave).get(0), grave);
                } else {
                    plugin.getEntityManager().sendMessage("message.teleport-disabled", entity,
                            entity.getLocation(), grave);
                }

                return true;
            }
            case "protect":
            case "protection": {
                if (grave.getTimeProtectionRemaining() > 0 || grave.getTimeProtectionRemaining() < 0) {
                    plugin.getGraveManager().toggleGraveProtection(grave);
                    playPlayerSound("sound.protection-change", entity, grave);
                    plugin.getGUIManager().openGraveMenu(entity, grave, false);
                }

                return true;
            }
            case "distance": {
                Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);

                if (location != null) {
                    if (entity.getWorld().equals(location.getWorld())) {
                        plugin.getEntityManager().sendMessage("message.distance", entity, location, grave);
                    } else {
                        plugin.getEntityManager().sendMessage("message.distance-world", entity, location, grave);
                    }
                }

                return true;
            }
            case "open":
            case "loot":
            case "virtual": {
                double distance = plugin.getConfig("virtual.distance", grave).getDouble("virtual.distance");

                if (distance < 0) {
                    plugin.getGraveManager().openGrave(entity, entity.getLocation(), grave);
                } else {
                    Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);

                    if (location != null) {
                        if (entity.getLocation().distance(location) <= distance) {
                            plugin.getGraveManager().openGrave(entity, entity.getLocation(), grave);
                        } else {
                            plugin.getEntityManager().sendMessage("message.distance-virtual", entity, location, grave);
                        }
                    }
                }

                return true;
            }
            case "autoloot": {
                plugin.getGraveManager().autoLootGrave(entity, entity.getLocation(), grave);

                return true;
            }
        }

        return false;
    }

    public boolean canOpenGrave(Player player, Grave grave) {
        if (grave.getTimeProtectionRemaining() == 0 || player.hasPermission("graves.bypass")) {
            return true;
        } else if (grave.getProtection() && grave.getOwnerUUID() != null) {
            if (grave.getOwnerUUID().equals(player.getUniqueId())
                    && plugin.getConfig("protection.open.owner", grave)
                    .getBoolean("protection.open.owner")) {
                return true;
            } else {
                if (grave.getKillerUUID() != null) {
                    if (grave.getKillerUUID().equals(player.getUniqueId())
                            && plugin.getConfig("protection.open.killer", grave)
                            .getBoolean("protection.open.killer")) {
                        return true;
                    } else return !grave.getOwnerUUID().equals(player.getUniqueId())
                            && !grave.getKillerUUID().equals(player.getUniqueId())
                            && plugin.getConfig("protection.open.other", grave)
                            .getBoolean("protection.open.other");
                } else return (grave.getOwnerUUID().equals(player.getUniqueId())
                        && plugin.getConfig("protection.open.missing.owner", grave)
                        .getBoolean("protection.open.missing.owner"))
                        || (!grave.getOwnerUUID().equals(player.getUniqueId())
                        && plugin.getConfig("protection.open.missing.other", grave)
                        .getBoolean("protection.open.missing.other"));
            }
        } else {
            return true;
        }
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
                        livingEntity.getEquipment().setHelmet(plugin.getCompatibility().getSkullItemStack(grave, plugin));
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
                setDataString(livingEntity, "graveEntityType", grave.getOwnerType().name());

                if (grave.getPermissionList() != null && !grave.getPermissionList().isEmpty()) {
                    setDataString(livingEntity, "gravePermissionList", String.join("|", grave.getPermissionList()));
                }

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

    public void createArmorStand(Location location, Grave grave) {
        if (!plugin.getVersionManager().is_v1_7()
                && plugin.getConfig("armor-stand.enabled", grave).getBoolean("armor-stand.enabled")) {
            double offsetX = plugin.getConfig("armor-stand.offset.x", grave).getDouble("armor-stand.offset.x");
            double offsetY = plugin.getConfig("armor-stand.offset.y", grave).getDouble("armor-stand.offset.y");
            double offsetZ = plugin.getConfig("armor-stand.offset.z", grave).getDouble("armor-stand.offset.z");
            boolean marker = plugin.getConfig("armor-stand.marker", grave).getBoolean("armor-stand.marker");
            location = LocationUtil.roundLocation(location)
                    .add(offsetX + 0.5, offsetY, offsetZ + 0.5);

            location.setYaw(grave.getYaw());
            location.setPitch(grave.getPitch());

            if (location.getWorld() != null) {
                Material material = Material.matchMaterial(plugin.getConfig("armor-stand.material", grave)
                        .getString("armor-stand.material", "AIR"));

                if (material != null && !MaterialUtil.isAir(material)) {
                    ItemStack itemStack = new ItemStack(material, 1);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    int customModelData = plugin.getConfig("armor-stand.model-data", grave)
                            .getInt("armor-stand.model-data", -1);

                    if (itemMeta != null) {
                        if (customModelData > -1) {
                            itemMeta.setCustomModelData(customModelData);
                        }

                        itemStack.setItemMeta(itemMeta);
                        location.getBlock().setType(Material.AIR);

                        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);

                        createEntityData(location, armorStand.getUniqueId(), grave.getUUID(),
                                EntityData.Type.ARMOR_STAND);

                        if (!plugin.getVersionManager().is_v1_7()) {
                            try {
                                armorStand.setMarker(marker);
                            } catch (NoSuchMethodError ignored) {
                            }
                        }

                        if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                            armorStand.setInvulnerable(true);
                        }

                        if (plugin.getVersionManager().hasScoreboardTags()) {
                            armorStand.getScoreboardTags().add("graveArmorStand");
                            armorStand.getScoreboardTags().add("graveArmorStandUUID:" + grave.getUUID());
                        }

                        armorStand.setVisible(false);
                        armorStand.setGravity(false);
                        armorStand.setCustomNameVisible(false);
                        armorStand.setSmall(plugin.getConfig("armor-stand.small", grave)
                                .getBoolean("armor-stand.small"));

                        if (armorStand.getEquipment() != null) {
                            EquipmentSlot equipmentSlot = EquipmentSlot.HEAD;

                            try {
                                equipmentSlot = EquipmentSlot.valueOf(plugin.getConfig("armor-stand.slot", grave)
                                        .getString("armor-stand.slot", "HEAD"));
                            } catch (IllegalArgumentException ignored) {
                            }

                            armorStand.getEquipment().setItem(equipmentSlot, itemStack);
                        }
                    }
                }
            }
        }
    }

    public void createItemFrame(Location location, Grave grave) {
        if (plugin.getConfig("item-frame.enabled", grave).getBoolean("item-frame.enabled")) {
            double offsetX = plugin.getConfig("item-frame.offset.x", grave).getDouble("item-frame.offset.x");
            double offsetY = plugin.getConfig("item-frame.offset.y", grave).getDouble("item-frame.offset.y");
            double offsetZ = plugin.getConfig("item-frame.offset.z", grave).getDouble("item-frame.offset.z");
            location = LocationUtil.roundLocation(location).add(offsetX + 0.5, offsetY, offsetZ + 0.5);

            location.setYaw(grave.getYaw());
            location.setPitch(grave.getPitch());

            if (location.getWorld() != null) {
                Material material = Material.matchMaterial(plugin.getConfig("item-frame.material", grave)
                        .getString("item-frame.material", "AIR"));

                if (material != null && !MaterialUtil.isAir(material)) {
                    ItemStack itemStack = new ItemStack(material, 1);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    int customModelData = plugin.getConfig("item-frame.model-data", grave)
                            .getInt("item-frame.model-data", -1);

                    if (itemMeta != null) {
                        if (customModelData > -1) {
                            itemMeta.setCustomModelData(customModelData);
                        }

                        itemStack.setItemMeta(itemMeta);
                        location.getBlock().setType(Material.AIR);

                        ItemFrame itemFrame = location.getWorld().spawn(location, ItemFrame.class);

                        itemFrame.setFacingDirection(BlockFace.UP);
                        itemFrame.setRotation(BlockFaceUtil.getBlockFaceRotation(BlockFaceUtil
                                .getYawBlockFace(location.getYaw())));
                        itemFrame.setVisible(false);
                        itemFrame.setGravity(false);
                        itemFrame.setCustomNameVisible(false);
                        itemFrame.setItem(itemStack);

                        if (!plugin.getVersionManager().is_v1_7() && !plugin.getVersionManager().is_v1_8()) {
                            itemFrame.setInvulnerable(true);
                        }

                        if (plugin.getVersionManager().hasScoreboardTags()) {
                            itemFrame.getScoreboardTags().add("graveItemFrame");
                            itemFrame.getScoreboardTags().add("graveItemFrameUUID:" + grave.getUUID());
                        }

                        createEntityData(location, itemFrame.getUniqueId(), grave.getUUID(),
                                EntityData.Type.ITEM_FRAME);
                    }
                }
            }
        }
    }

    public void removeEntity(Grave grave) {
        removeEntity(getEntityDataMap(getLoadedEntityDataList(grave)));
    }

    public void removeEntity(Map<EntityData, Entity> entityDataMap) {
        List<EntityData> entityDataList = new ArrayList<>();

        for (Map.Entry<EntityData, Entity> entry : entityDataMap.entrySet()) {
            if (entry.getKey().getType() == EntityData.Type.ARMOR_STAND
                    || entry.getKey().getType() == EntityData.Type.ITEM_FRAME
                    || entry.getKey().getType() == EntityData.Type.HOLOGRAM) {
                entry.getValue().remove();
                entityDataList.add(entry.getKey());
            }
        }

        plugin.getDataManager().removeEntityData(entityDataList);
    }

    public Map<EquipmentSlot, ItemStack> getEquipmentMap(LivingEntity livingEntity, Grave grave) {
        Map<EquipmentSlot, ItemStack> equipmentSlotItemStackMap = new HashMap<>();

        if (livingEntity.getEquipment() != null) {
            EntityEquipment entityEquipment = livingEntity.getEquipment();

            if (entityEquipment.getHelmet() != null
                    && grave.getInventory().contains(entityEquipment.getHelmet())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.HEAD, entityEquipment.getHelmet());
            }

            if (entityEquipment.getChestplate() != null
                    && grave.getInventory().contains(entityEquipment.getChestplate())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.CHEST, entityEquipment.getChestplate());
            }

            if (entityEquipment.getLeggings() != null
                    && grave.getInventory().contains(entityEquipment.getLeggings())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.LEGS, entityEquipment.getLeggings());
            }

            if (entityEquipment.getBoots() != null
                    && grave.getInventory().contains(entityEquipment.getBoots())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.FEET, entityEquipment.getBoots());
            }

            if (plugin.getVersionManager().hasSecondHand()) {
                if (entityEquipment.getItemInMainHand().getType() != Material.AIR
                        && grave.getInventory().contains(entityEquipment.getItemInMainHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.HAND, entityEquipment.getItemInMainHand());
                }

                if (entityEquipment.getItemInOffHand().getType() != Material.AIR
                        && grave.getInventory().contains(entityEquipment.getItemInOffHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.OFF_HAND, entityEquipment.getItemInOffHand());
                }
            } else {
                if (entityEquipment.getItemInHand().getType() != Material.AIR
                        && grave.getInventory().contains(entityEquipment.getItemInHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.HAND, entityEquipment.getItemInHand());
                }
            }
        }

        return equipmentSlotItemStackMap;
    }

    @SuppressWarnings("redundant")
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
            return plugin.getCacheManager().getGraveMap().get(UUIDUtil.getUUID(entity.getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)));
        } else if (entity.hasMetadata("graveUUID")) {
            List<MetadataValue> metadataValue = entity.getMetadata("graveUUID");

            if (!metadataValue.isEmpty()) {
                return plugin.getCacheManager().getGraveMap().get(UUIDUtil.getUUID(metadataValue.get(0).asString()));
            }
        }

        return null;
    }
}
