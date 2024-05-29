package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.*;
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

import java.lang.reflect.Method;
import java.util.*;

public final class EntityManager extends EntityDataManager {

    private final Graves plugin;

    public EntityManager(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
    }

    public void swingMainHand(Player player) {
        player.swingMainHand();
    }

    public ItemStack createGraveCompass(Player player, Location location, Grave grave) {
        if (true) {
            Material material = Material.COMPASS;

            if (plugin.getConfigBool("compass.recovery", grave)) {
                try {
                    material = Material.valueOf("RECOVERY_COMPASS");
                }
                catch (IllegalArgumentException ignored) {
                }
            }

            ItemStack itemStack = new ItemStack(material);
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (itemMeta instanceof CompassMeta) {
                    CompassMeta compassMeta = (CompassMeta) itemMeta;

                    compassMeta.setLodestoneTracked(false);
                    compassMeta.setLodestone(location);
                }
                else if (itemStack.getType().name().equals("RECOVERY_COMPASS")) {
                    try {
                        // Not known in 1.18 yet...
                        Method setLastDeathLocationMethod = player.getClass().getMethod("setLastDeathLocation", Location.class);
                        setLastDeathLocationMethod.invoke(player, location);                    }
                    catch (Exception ignored) {
                    }
                }

                List<String> loreList = new ArrayList<>();
                int customModelData = plugin.getConfigInt("compass.model-data", grave, -1);

                if (customModelData > -1) {
                    itemMeta.setCustomModelData(customModelData);
                }

                if (plugin.getConfigBool("compass.glow", grave)) {
                    itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                itemMeta.setDisplayName(ChatColor.WHITE
                                        + StringUtil.parseString(plugin.getConfigString("compass.name", grave), grave, plugin));
                itemMeta.getPersistentDataContainer()
                        .set(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING, grave.getUUID()
                                                                                                     .toString());

                for (String string : plugin.getConfigStringList("compass.lore", grave)) {
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

        if (true) {
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
        if (itemStack != null && itemStack.getItemMeta() != null) {
            if (itemStack.getItemMeta()
                         .getPersistentDataContainer()
                         .has(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)) {
                return UUIDUtil.getUUID(itemStack.getItemMeta()
                                                 .getPersistentDataContainer()
                                                 .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING));
            }
        }

        return null;
    }

    public void teleportEntity(Entity entity, Location location, Grave grave) {
        if (canTeleport(entity, location)) {
            location = LocationUtil.roundLocation(location);
            BlockFace blockFace = BlockFaceUtil.getYawBlockFace(grave.getYaw());
            Location locationTeleport = location.clone()
                                                .getBlock()
                                                .getRelative(blockFace)
                                                .getRelative(blockFace)
                                                .getLocation()
                                                .add(0.5, 0, 0.5);

            if (plugin.getLocationManager().isLocationSafePlayer(locationTeleport)) {
                locationTeleport.setYaw(BlockFaceUtil.getBlockFaceYaw(blockFace.getOppositeFace()));
                locationTeleport.setPitch(20);
            }
            else {
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
                            plugin.getEntityManager()
                                  .playPlayerSound("sound.teleport", player, locationTeleport, grave);
                        }
                        else {
                            plugin.getEntityManager()
                                  .sendMessage("message.no-money", player, player.getLocation(), grave);
                        }
                    }
                    else {
                        player.teleport(locationTeleport);
                    }
                }
                else {
                    entity.teleport(locationTeleport);
                }
            }
            else {
                plugin.getEntityManager().sendMessage("message.teleport-failure", entity, location, grave);
            }
        }
        else {
            plugin.getEntityManager().sendMessage("message.region-teleport-deny", entity, location, grave);
        }
    }

    public double getTeleportCost(Location location1, Location location2, Grave grave) {
        double cost = plugin.getConfigDbl("teleport.cost", grave);

        if (plugin.getConfig("teleport.cost", grave).isString("teleport.cost")) {
            String costString = StringUtil.parseString(plugin.getConfigString("teleport.cost", grave), location2, grave, plugin);

            try {
                cost = Double.parseDouble(costString);
            }
            catch (NumberFormatException ignored) {
                plugin.debugMessage(costString + " cost is not a double", 1);
            }
        }

        double costDifferentWorld = plugin.getConfigDbl("teleport.cost-different-world", grave);

        if (plugin.getConfigBool("teleport.cost-distance-increase", grave)) {
            double distance = Math.sqrt(NumberConversions.square(location1.getBlockX() - location2.getBlockX())
                                        + NumberConversions.square(location1.getBlockZ() - location2.getBlockZ()));
            cost = Math.round(cost * (distance / 16));
        }

        if (location1.getWorld() != null
            && location2.getWorld() != null
            && costDifferentWorld > 0
            && !location1.getWorld().getName().equals(location2.getWorld().getName())) {
            cost += costDifferentWorld;
        }

        return cost;
    }

    public boolean canTeleport(Entity entity, Location location) {
        return (!plugin.getIntegrationManager().hasWorldGuard() || plugin.getIntegrationManager()
                                                                         .getWorldGuard()
                                                                         .canTeleport(entity, location))
               && (!plugin.getIntegrationManager().hasGriefDefender() || plugin.getIntegrationManager()
                                                                               .getGriefDefender()
                                                                               .canTeleport(entity, location));
    }

    public void playWorldSound(String string, Player player) {
        playWorldSound(string, player.getLocation(), null);
    }

    public void playWorldSound(String string, Player player, Grave grave) {
        playWorldSound(string, player.getLocation(), grave);
    }

    public void playWorldSound(String string, Location location, Grave grave) {
        playWorldSound(string, location, grave != null ? grave.getOwnerType() : null, grave != null
                                                                                      ? grave.getPermissionList()
                                                                                      : null, 1, 1);
    }

    public void playWorldSound(String string, Location location, EntityType entityType, List<String> permissionList, float volume, float pitch) {
        if (location.getWorld() != null) {
            string = plugin.getConfigString(string, entityType, permissionList);

            if (string != null && !string.isEmpty()) {
                try {
                    location.getWorld().playSound(location, Sound.valueOf(string.toUpperCase()), volume, pitch);
                }
                catch (IllegalArgumentException exception) {
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

    public void playPlayerSound(String string, Entity entity, Location location, List<String> permissionList, float volume, float pitch) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            string = plugin.getConfigString(string, entity, permissionList);

            if (string != null && !string.isEmpty()) {
                try {
                    player.playSound(location, Sound.valueOf(string.toUpperCase()), volume, pitch);
                }
                catch (IllegalArgumentException exception) {
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
                string = plugin.getConfigString(string, grave);
            }
            else {
                string = plugin.getConfigString(string, entity.getType(), permissionList);
            }

            String prefix = plugin.getConfigString("message.prefix", entity.getType(), permissionList);

            if (prefix != null && !prefix.isEmpty()) {
                string = prefix + string;
            }

            if (string != null && !string.isEmpty()) {
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
        for (String command : plugin.getConfigStringList(string, grave)) {
            if (command != null && !command.isEmpty()) {
                runConsoleCommand(StringUtil.parseString(command, entity, name, location, grave, plugin));
            }
        }
    }

    private void runConsoleCommand(String string) {
        if (string != null && !string.isEmpty()) {
            ServerCommandEvent serverCommandEvent = new ServerCommandEvent(plugin.getServer()
                                                                                 .getConsoleSender(), string);

            plugin.getServer().getPluginManager().callEvent(serverCommandEvent);

            if (!serverCommandEvent.isCancelled()) {
                plugin.getServer()
                      .getScheduler()
                      .callSyncMethod(plugin, () -> plugin.getServer()
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
            case "list" -> {
                plugin.getGUIManager().openGraveList(entity);

                return true;
            }
            case "menu" -> {
                plugin.getGUIManager().openGraveMenu(entity, grave);

                return true;
            }
            case "teleport", "teleportation" -> {
                if (plugin.getConfigBool("teleport.enabled", grave)
                    && (EntityUtil.hasPermission(entity, "graves.teleport")
                        || EntityUtil.hasPermission(entity, "graves.bypass"))) {
                    plugin.getEntityManager()
                          .teleportEntity(entity, plugin.getGraveManager()
                                                        .getGraveLocationList(entity.getLocation(), grave)
                                                        .get(0), grave);
                }
                else {
                    plugin.getEntityManager()
                          .sendMessage("message.teleport-disabled", entity, entity.getLocation(), grave);
                }

                return true;
            }
            case "protect", "protection" -> {
                if (grave.getTimeProtectionRemaining() > 0 || grave.getTimeProtectionRemaining() < 0) {
                    plugin.getGraveManager().toggleGraveProtection(grave);
                    playPlayerSound("sound.protection-change", entity, grave);
                    plugin.getGUIManager().openGraveMenu(entity, grave, false);
                }

                return true;
            }
            case "distance" -> {
                Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);

                if (location != null) {
                    if (entity.getWorld().equals(location.getWorld())) {
                        plugin.getEntityManager().sendMessage("message.distance", entity, location, grave);
                    }
                    else {
                        plugin.getEntityManager().sendMessage("message.distance-world", entity, location, grave);
                    }
                }

                return true;
            }
            case "open", "loot", "virtual" -> {
                double distance = plugin.getConfigDbl("virtual.distance", grave);

                if (distance < 0) {
                    plugin.getGraveManager().openGrave(entity, entity.getLocation(), grave);
                }
                else {
                    Location location = plugin.getGraveManager().getGraveLocation(entity.getLocation(), grave);

                    if (location != null) {
                        if (entity.getLocation().distance(location) <= distance) {
                            plugin.getGraveManager().openGrave(entity, entity.getLocation(), grave);
                        }
                        else {
                            plugin.getEntityManager().sendMessage("message.distance-virtual", entity, location, grave);
                        }
                    }
                }

                return true;
            }
            case "autoloot" -> {
                plugin.getGraveManager().autoLootGrave(entity, entity.getLocation(), grave);

                return true;
            }
        }

        return false;
    }

    public boolean canOpenGrave(Player player, Grave grave) {
        if (grave.getTimeProtectionRemaining() == 0 || player.hasPermission("graves.bypass")) {
            return true;
        }
        else if (grave.getProtection() && grave.getOwnerUUID() != null) {
            if (grave.getOwnerUUID().equals(player.getUniqueId())
                && plugin.getConfigBool("protection.open.owner", grave)) {
                return true;
            }
            else {
                if (grave.getKillerUUID() != null) {
                    if (grave.getKillerUUID().equals(player.getUniqueId())
                        && plugin.getConfigBool("protection.open.killer", grave)) {
                        return true;
                    }
                    else {
                        return !grave.getOwnerUUID().equals(player.getUniqueId())
                               && !grave.getKillerUUID()
                                        .equals(player.getUniqueId())
                               && plugin.getConfigBool("protection.open.other", grave);
                    }
                }
                else {
                    return (grave.getOwnerUUID().equals(player.getUniqueId())
                            && plugin.getConfigBool("protection.open.missing.owner", grave)) || (!grave.getOwnerUUID()
                                                                                             .equals(player.getUniqueId())
                                                                                                 && plugin.getConfigBool("protection.open.missing.other", grave));
                }
            }
        }
        else {
            return true;
        }
    }

    public void spawnZombie(Location location, Entity entity, LivingEntity targetEntity, Grave grave) {
        if ((plugin.getConfigBool("zombie.spawn-owner", grave) && grave.getOwnerUUID()
                                                                                                    .equals(entity.getUniqueId())
             || plugin.getConfigBool("zombie.spawn-other", grave) && !grave.getOwnerUUID()
                                                                                                        .equals(entity.getUniqueId()))) {
            spawnZombie(location, targetEntity, grave);
        }
    }

    public void spawnZombie(Location location, Grave grave) {
        spawnZombie(location, null, grave);
    }

    @SuppressWarnings("deprecation")
    private void spawnZombie(Location location, LivingEntity targetEntity, Grave grave) {
        if (location != null && location.getWorld() != null && grave.getOwnerType() == EntityType.PLAYER) {
            String zombieType = plugin.getConfigString("zombie.type", grave, "ZOMBIE").toUpperCase();
            EntityType entityType = EntityType.ZOMBIE;

            try {
                entityType = EntityType.valueOf(zombieType);
            }
            catch (IllegalArgumentException exception) {
                plugin.debugMessage(zombieType + " is not a EntityType ENUM", 1);
            }

            if (entityType.name().equals("ZOMBIE") && MaterialUtil.isWater(location.getBlock().getType())) {
                try {
                    entityType = EntityType.valueOf("DROWNED");
                }
                catch (IllegalArgumentException ignored) {
                }
            }

            Entity entity = location.getWorld().spawnEntity(location, entityType);

            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;

                if (livingEntity.getEquipment() != null) {
                    if (plugin.getConfigBool("zombie.owner-head", grave)) {
                        livingEntity.getEquipment()
                                    .setHelmet(plugin.getCompatibility().getSkullItemStack(grave, plugin));
                    }

                    livingEntity.getEquipment().setChestplate(null);
                    livingEntity.getEquipment().setLeggings(null);
                    livingEntity.getEquipment().setBoots(null);
                }

                double zombieHealth = plugin.getConfigDbl("zombie.health", grave);

                if (zombieHealth >= 0.5) {
                    livingEntity.setMaxHealth(zombieHealth);
                    livingEntity.setHealth(zombieHealth);
                }

                if (!plugin.getConfigBool("zombie.pickup", grave)) {
                    livingEntity.setCanPickupItems(false);
                }

                String zombieName = StringUtil.parseString(plugin.getConfigString("zombie.name", grave), location, grave, plugin);

                if (!zombieName.isEmpty()) {
                    livingEntity.setCustomName(zombieName);
                }

                setDataByte(livingEntity, "graveZombie");
                setDataString(livingEntity, "graveUUID", grave.getUUID().toString());
                setDataString(livingEntity, "graveEntityType", grave.getOwnerType().name());

                if (grave.getPermissionList() != null && !grave.getPermissionList().isEmpty()) {
                    setDataString(livingEntity, "gravePermissionList", String.join("|", grave.getPermissionList()));
                }

                if (livingEntity instanceof Mob
                    && targetEntity != null
                    && !targetEntity.isInvulnerable()
                    && (!(targetEntity instanceof Player)
                        || ((Player) targetEntity).getGameMode() != GameMode.CREATIVE)) {
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
        if (plugin.getConfigBool("armor-stand.enabled", grave)) {
            double offsetX = plugin.getConfigDbl("armor-stand.offset.x", grave);
            double offsetY = plugin.getConfigDbl("armor-stand.offset.y", grave);
            double offsetZ = plugin.getConfigDbl("armor-stand.offset.z", grave);
            boolean marker = plugin.getConfigBool("armor-stand.marker", grave);
            location = LocationUtil.roundLocation(location).add(offsetX + 0.5, offsetY, offsetZ + 0.5);

            location.setYaw(grave.getYaw());
            location.setPitch(grave.getPitch());

            if (location.getWorld() != null) {
                Material material = Material.matchMaterial(plugin.getConfigString("armor-stand.material", grave, "AIR"));

                if (material != null && !MaterialUtil.isAir(material)) {
                    ItemStack itemStack = new ItemStack(material, 1);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    int customModelData = plugin.getConfigInt("armor-stand.model-data", grave, -1);

                    if (itemMeta != null) {
                        if (customModelData > -1) {
                            itemMeta.setCustomModelData(customModelData);
                        }

                        itemStack.setItemMeta(itemMeta);
                        location.getBlock().setType(Material.AIR);

                        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);

                        createEntityData(location, armorStand.getUniqueId(), grave.getUUID(), EntityData.Type.ARMOR_STAND);

                        if (true) {
                            try {
                                armorStand.setMarker(marker);
                            }
                            catch (NoSuchMethodError ignored) {
                            }
                        }

                        if (true) {
                            armorStand.setInvulnerable(true);
                        }

                        if (true) {
                            armorStand.getScoreboardTags().add("graveArmorStand");
                            armorStand.getScoreboardTags().add("graveArmorStandUUID:" + grave.getUUID());
                        }

                        armorStand.setVisible(false);
                        armorStand.setGravity(false);
                        armorStand.setCustomNameVisible(false);
                        armorStand.setSmall(plugin.getConfigBool("armor-stand.small", grave));

                        if (armorStand.getEquipment() != null) {
                            EquipmentSlot equipmentSlot = EquipmentSlot.HEAD;

                            try {
                                equipmentSlot = EquipmentSlot.valueOf(plugin.getConfigString("armor-stand.slot", grave, "HEAD"));
                            }
                            catch (IllegalArgumentException ignored) {
                            }

                            armorStand.getEquipment().setItem(equipmentSlot, itemStack);
                        }
                    }
                }
            }
        }
    }

    public void createItemFrame(Location location, Grave grave) {
        if (plugin.getConfigBool("item-frame.enabled", grave)) {
            double offsetX = plugin.getConfigDbl("item-frame.offset.x", grave);
            double offsetY = plugin.getConfigDbl("item-frame.offset.y", grave);
            double offsetZ = plugin.getConfigDbl("item-frame.offset.z", grave);
            location = LocationUtil.roundLocation(location).add(offsetX + 0.5, offsetY, offsetZ + 0.5);

            location.setYaw(grave.getYaw());
            location.setPitch(grave.getPitch());

            if (location.getWorld() != null) {
                Material material = Material.matchMaterial(plugin.getConfigString("item-frame.material", grave, "AIR"));

                if (material != null && !MaterialUtil.isAir(material)) {
                    ItemStack itemStack = new ItemStack(material, 1);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    int customModelData = plugin.getConfigInt("item-frame.model-data", grave, -1);

                    if (itemMeta != null) {
                        if (customModelData > -1) {
                            itemMeta.setCustomModelData(customModelData);
                        }

                        itemStack.setItemMeta(itemMeta);
                        location.getBlock().setType(Material.AIR);

                        ItemFrame itemFrame = location.getWorld().spawn(location, ItemFrame.class);

                        itemFrame.setFacingDirection(BlockFace.UP);
                        itemFrame.setRotation(BlockFaceUtil.getBlockFaceRotation(BlockFaceUtil.getYawBlockFace(location.getYaw())));
                        itemFrame.setVisible(false);
                        itemFrame.setGravity(false);
                        itemFrame.setCustomNameVisible(false);
                        itemFrame.setItem(itemStack);

                        if (true) {
                            itemFrame.setInvulnerable(true);
                        }

                        if (true) {
                            itemFrame.getScoreboardTags().add("graveItemFrame");
                            itemFrame.getScoreboardTags().add("graveItemFrameUUID:" + grave.getUUID());
                        }

                        createEntityData(location, itemFrame.getUniqueId(), grave.getUUID(), EntityData.Type.ITEM_FRAME);
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

            if (entityEquipment.getHelmet() != null && grave.getInventory().contains(entityEquipment.getHelmet())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.HEAD, entityEquipment.getHelmet());
            }

            if (entityEquipment.getChestplate() != null && grave.getInventory()
                                                                .contains(entityEquipment.getChestplate())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.CHEST, entityEquipment.getChestplate());
            }

            if (entityEquipment.getLeggings() != null && grave.getInventory().contains(entityEquipment.getLeggings())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.LEGS, entityEquipment.getLeggings());
            }

            if (entityEquipment.getBoots() != null && grave.getInventory().contains(entityEquipment.getBoots())) {
                equipmentSlotItemStackMap.put(EquipmentSlot.FEET, entityEquipment.getBoots());
            }

            if (true) {
                if (entityEquipment.getItemInMainHand().getType() != Material.AIR && grave.getInventory()
                                                                                          .contains(entityEquipment.getItemInMainHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.HAND, entityEquipment.getItemInMainHand());
                }

                if (entityEquipment.getItemInOffHand().getType() != Material.AIR && grave.getInventory()
                                                                                         .contains(entityEquipment.getItemInOffHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.OFF_HAND, entityEquipment.getItemInOffHand());
                }
            }
            else {
                if (entityEquipment.getItemInMainHand().getType() != Material.AIR && grave.getInventory()
                                                                                          .contains(entityEquipment.getItemInMainHand())) {
                    equipmentSlotItemStackMap.put(EquipmentSlot.HAND, entityEquipment.getItemInMainHand());
                }
            }
        }

        return equipmentSlotItemStackMap;
    }

    @SuppressWarnings("redundant")
    public String getEntityName(Entity entity) {
        if (entity != null) {
            if (entity instanceof Player) {
                return entity.getName(); // Need redundancy for legacy support
            }
            else if (true) {
                return entity.getName();
            }

            return StringUtil.format(entity.getType().toString());
        }

        return "null";
    }

    public boolean hasDataString(Entity entity, String string) {
        return entity.getPersistentDataContainer().has(new NamespacedKey(plugin, string), PersistentDataType.STRING);
    }

    public boolean hasDataByte(Entity entity, String string) {
        return entity.getPersistentDataContainer().has(new NamespacedKey(plugin, string), PersistentDataType.BYTE);
    }

    public String getDataString(Entity entity, String key) {
        if (entity.getPersistentDataContainer().has(new NamespacedKey(plugin, key), PersistentDataType.STRING)) {
            return entity.getPersistentDataContainer().get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
        }
        else {
            return entity.getMetadata(key).toString();
        }
    }

    public void setDataString(Entity entity, String key, String string) {
        if (true) {
            entity.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, string);
        }
        else {
            entity.setMetadata(key, new FixedMetadataValue(plugin, string));
        }
    }

    public void setDataByte(Entity entity, String key) {
        if (true) {
            entity.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.BYTE, (byte) 1);
        }
        else {
            entity.setMetadata(key, new FixedMetadataValue(plugin, (byte) 1));
        }
    }

    public Grave getGraveFromEntityData(Entity entity) {
        if (entity.getPersistentDataContainer()
                  .has(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)) {
            return plugin.getCacheManager()
                         .getGraveMap()
                         .get(UUIDUtil.getUUID(entity.getPersistentDataContainer()
                                                     .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING)));
        }
        else if (entity.hasMetadata("graveUUID")) {
            List<MetadataValue> metadataValue = entity.getMetadata("graveUUID");

            if (!metadataValue.isEmpty()) {
                return plugin.getCacheManager().getGraveMap().get(UUIDUtil.getUUID(metadataValue.get(0).asString()));
            }
        }

        return null;
    }

}
