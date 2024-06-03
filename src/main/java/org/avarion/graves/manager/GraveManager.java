package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.data.BlockData;
import org.avarion.graves.data.ChunkData;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.data.HologramData;
import org.avarion.graves.event.GraveTimeoutEvent;
import org.avarion.graves.inventory.GraveList;
import org.avarion.graves.inventory.GraveMenu;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.ColorUtil;
import org.avarion.graves.util.InventoryUtil;
import org.avarion.graves.util.StringUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class GraveManager {
    private final Graves plugin;

    public GraveManager(Graves plugin) {
        this.plugin = plugin;

        startGraveTimer();
    }

    private void startGraveTimer() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            List<Grave> graveRemoveList = new ArrayList<>();
            List<EntityData> entityDataRemoveList = new ArrayList<>();
            List<BlockData> blockDataRemoveList = new ArrayList<>();

            // Graves
            for (Map.Entry<UUID, Grave> entry : CacheManager.graveMap.entrySet()) {
                Grave grave = entry.getValue();

                if (grave.getTimeAliveRemaining() >= 0 && grave.getTimeAliveRemaining() <= 1000) {
                    GraveTimeoutEvent graveTimeoutEvent = new GraveTimeoutEvent(grave);

                    plugin.getServer().getPluginManager().callEvent(graveTimeoutEvent);

                    if (!graveTimeoutEvent.isCancelled()) {
                        if (graveTimeoutEvent.getLocation() != null && plugin.getConfigBool("drop.timeout", grave)) {
                            dropGraveItems(graveTimeoutEvent.getLocation(), grave);
                            dropGraveExperience(graveTimeoutEvent.getLocation(), grave);
                        }

                        if (grave.getOwnerType() == EntityType.PLAYER && grave.getOwnerUUID() != null) {
                            Player player = plugin.getServer().getPlayer(grave.getOwnerUUID());

                            if (player != null) {
                                plugin.getEntityManager()
                                      .sendMessage("message.timeout", player, graveTimeoutEvent.getLocation(), grave);
                            }
                        }

                        graveRemoveList.add(grave);
                    }
                }

                // Protection
                if (grave.getProtection() && grave.getTimeProtectionRemaining() == 0) {
                    toggleGraveProtection(grave);
                }
            }

            // Chunks
            for (Map.Entry<String, ChunkData> entry : CacheManager.chunkMap.entrySet()) {
                ChunkData chunkData = entry.getValue();

                if (chunkData.isLoaded()) {
                    Location location = new Location(chunkData.getWorld(), chunkData.getX() << 4, 0, chunkData.getZ()
                                                                                                     << 4);

                    // Entity data
                    for (EntityData entityData : new ArrayList<>(chunkData.getEntityDataMap().values())) {
                        if (CacheManager.graveMap.containsKey(entityData.getUUIDGrave())) {
                            if (plugin.isEnabled() && entityData instanceof HologramData hologramData) {
                                Grave grave = CacheManager.graveMap.get(hologramData.getUUIDGrave());

                                if (grave != null) {
                                    List<String> lineList = plugin.getConfigStringList("hologram.line", grave);

                                    Collections.reverse(lineList);

                                    for (Entity entity : entityData.getLocation().getChunk().getEntities()) {
                                        if (entity.getUniqueId().equals(entityData.getUUIDEntity())) {
                                            if (hologramData.getLine() < lineList.size()) {
                                                entity.setCustomName(StringUtil.parseString(lineList.get(hologramData.getLine()), location, grave, plugin));
                                            }
                                            else {
                                                entityDataRemoveList.add(hologramData);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            entityDataRemoveList.add(entityData);
                        }
                    }

                    // Blocks
                    for (BlockData blockData : new ArrayList<>(chunkData.getBlockDataMap().values())) {
                        if (blockData.location().getWorld() != null) {
                            if (CacheManager.graveMap.containsKey(blockData.graveUUID())) {
                                graveParticle(blockData.location(), CacheManager.graveMap.get(blockData.graveUUID()));
                            }
                            else {
                                blockDataRemoveList.add(blockData);
                            }
                        }
                    }
                }
            }

            if (plugin.isEnabled()) {
                graveRemoveList.forEach(GraveManager.this::removeGrave);
                entityDataRemoveList.forEach(GraveManager.this::removeEntityData);
                blockDataRemoveList.forEach(blockData -> plugin.getBlockManager().removeBlock(blockData));
                graveRemoveList.clear();
                blockDataRemoveList.clear();
                entityDataRemoveList.clear();
                plugin.getGUIManager().refreshMenus();
            }
        }, 10L, 20L);
    }

    @SuppressWarnings("ConstantConditions")
    public void unload() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory() != null
                && player.getOpenInventory().getTopInventory()
                   != null) { // Mohist, might return null even when Bukkit shouldn't.
                InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();

                try {
                    if (inventoryHolder instanceof Grave
                        || inventoryHolder instanceof GraveList
                        || inventoryHolder instanceof GraveMenu) {
                        player.closeInventory();
                    }
                }
                catch (Exception ignored) {
                }
            }
        }
    }

    public void toggleGraveProtection(Grave grave) {
        grave.setProtection(!grave.getProtection());
        plugin.getDataManager().updateGrave(grave, "protection", String.valueOf(grave.getProtection() ? 1 : 0));
    }

    public void graveParticle(Location location, Grave grave) {
        if (location.getWorld() != null && plugin.getConfigBool("particle.enabled", grave)) {
            Particle particle = Particle.REDSTONE;
            String particleType = plugin.getConfigString("particle.type", grave);

            if (particleType != null && !particleType.isEmpty()) {
                try {
                    particle = Particle.valueOf(plugin.getConfigString("particle.type", grave));
                }
                catch (IllegalArgumentException ignored) {
                    plugin.debugMessage(particleType + " is not a Particle ENUM", 1);
                }
            }

            int count = plugin.getConfigInt("particle.count", grave);
            double offsetX = plugin.getConfigDbl("particle.offset.x", grave);
            double offsetY = plugin.getConfigDbl("particle.offset.y", grave);
            double offsetZ = plugin.getConfigDbl("particle.offset.z", grave);
            location = location.clone().add(offsetX + 0.5, offsetY + 0.5, offsetZ + 0.5);

            if (location.getWorld() != null) {
                switch (particle.name()) {
                    case "REDSTONE" -> {
                        int size = plugin.getConfigInt("particle.dust-size", grave);
                        Color color = ColorUtil.getColor(plugin.getConfigString("particle.dust-color", grave, "RED"));
                        if (color == null) {
                            color = Color.RED;
                        }
                        location.getWorld()
                                .spawnParticle(particle, location, count, new Particle.DustOptions(color, size));
                    }
                    case "SHRIEK" -> location.getWorld().spawnParticle(particle, location, count, 1);
                    default -> location.getWorld().spawnParticle(particle, location, count);
                }
            }
        }
    }

    public void removeGrave(Grave grave) {
        closeGrave(grave);
        plugin.getBlockManager().removeBlock(grave);
        plugin.getHologramManager().removeHologram(grave);
        plugin.getEntityManager().removeEntity(grave);
        plugin.getDataManager().removeGrave(grave);

        if (plugin.getIntegrationManager().hasMultiPaper()) {
            plugin.getIntegrationManager().getMultiPaper().notifyGraveRemoval(grave);
        }

        if (plugin.getIntegrationManager().hasFurnitureLib()) {
            plugin.getIntegrationManager().getFurnitureLib().removeFurniture(grave);
        }

        if (plugin.getIntegrationManager().hasFurnitureEngine()) {
            plugin.getIntegrationManager().getFurnitureEngine().removeFurniture(grave);
        }

        if (plugin.getIntegrationManager().hasItemsAdder()) {
            plugin.getIntegrationManager().getItemsAdder().removeFurniture(grave);
        }

        if (plugin.getIntegrationManager().hasOraxen()) {
            plugin.getIntegrationManager().getOraxen().removeFurniture(grave);
        }

        if (plugin.getIntegrationManager().hasPlayerNPC()) {
            plugin.getIntegrationManager().getPlayerNPC().removeCorpse(grave);
        }


        plugin.debugMessage("Removing grave " + grave.getUUID(), 1);
    }

    public void removeEntityData(EntityData entityData) {
        switch (entityData.getType()) {
            case HOLOGRAM -> {
                plugin.getHologramManager().removeHologram(entityData);

            }
            case FURNITURELIB -> {
                plugin.getIntegrationManager().getFurnitureLib().removeEntityData(entityData);

            }
            case FURNITUREENGINE -> {
                plugin.getIntegrationManager().getFurnitureEngine().removeEntityData(entityData);

            }
            case ITEMSADDER -> {
                plugin.getIntegrationManager().getItemsAdder().removeEntityData(entityData);

            }
            case ORAXEN -> {
                plugin.getIntegrationManager().getOraxen().removeEntityData(entityData);

            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void closeGrave(Grave grave) {
        List<HumanEntity> inventoryViewers = grave.getInventory().getViewers();

        for (HumanEntity humanEntity : new ArrayList<>(inventoryViewers)) {
            grave.getInventory().getViewers().remove(humanEntity);
            humanEntity.closeInventory();
            plugin.debugMessage("Closing grave " + grave.getUUID() + " for " + humanEntity.getName(), 1);
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory() != null) { // Mohist, might return null even when Bukkit shouldn't.
                InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();

                if (inventoryHolder instanceof GraveMenu graveMenu) {

                    if (graveMenu.getGrave().getUUID().equals(grave.getUUID())) {
                        player.closeInventory();
                    }
                }
            }
        }
    }

    public Grave.StorageMode getStorageMode(String string) {
        try {
            Grave.StorageMode storageMode = Grave.StorageMode.valueOf(string.toUpperCase());

            if (storageMode == Grave.StorageMode.CHESTSORT && !plugin.getIntegrationManager().hasChestSort()) {
                return Grave.StorageMode.COMPACT;
            }

            return storageMode;
        }
        catch (NullPointerException | IllegalArgumentException ignored) {
        }

        return Grave.StorageMode.COMPACT;
    }

    public void placeGrave(Location location, Grave grave) {
        plugin.getBlockManager().createBlock(location, grave);
        plugin.getHologramManager().createHologram(location, grave);
        plugin.getEntityManager().createArmorStand(location, grave);
        plugin.getEntityManager().createItemFrame(location, grave);

        if (plugin.getIntegrationManager().hasWorldEdit()) {
            plugin.getIntegrationManager().getWorldEdit().createSchematic(location, grave);
        }

        if (plugin.getIntegrationManager().hasFurnitureLib()) {
            plugin.getIntegrationManager().getFurnitureLib().createFurniture(location, grave);
        }

        if (plugin.getIntegrationManager().hasFurnitureEngine()) {
            plugin.getIntegrationManager().getFurnitureEngine().createFurniture(location, grave);
        }

        if (plugin.getIntegrationManager().hasItemsAdder()) {
            plugin.getIntegrationManager().getItemsAdder().createFurniture(location, grave);
        }

        if (plugin.getIntegrationManager().hasOraxen()) {
            plugin.getIntegrationManager().getOraxen().createFurniture(location, grave);
        }

        if (plugin.getIntegrationManager().hasPlayerNPC()) {
            plugin.getIntegrationManager().getPlayerNPC().createCorpse(location, grave);
        }
    }

    public Inventory getGraveInventory(Grave grave, LivingEntity livingEntity, List<ItemStack> graveItemStackList, List<ItemStack> removedItemStackList, List<String> permissionList) {
        List<ItemStack> filterGraveItemStackList = filterGraveItemStackList(graveItemStackList, removedItemStackList, livingEntity, permissionList);
        String title = StringUtil.parseString(plugin.getConfigString("gui.grave.title", grave), livingEntity, grave.getLocationDeath(), grave, plugin);
        Grave.StorageMode storageMode = getStorageMode(plugin.getConfigString("storage.mode", grave));

        return plugin.getGraveManager()
                     .createGraveInventory(grave, grave.getLocationDeath(), filterGraveItemStackList, title, storageMode);
    }

    public Inventory createGraveInventory(InventoryHolder inventoryHolder, Location location, List<ItemStack> itemStackList, String title, Grave.StorageMode storageMode) {
        itemStackList.removeIf(itemStack -> itemStack != null
                                            && itemStack.containsEnchantment(Enchantment.VANISHING_CURSE));

        if (storageMode == Grave.StorageMode.COMPACT || storageMode == Grave.StorageMode.CHESTSORT) {
            Inventory tempInventory = plugin.getServer().createInventory(null, 54);
            int counter = 0;

            for (ItemStack itemStack : itemStackList) {
                if (getItemStacksSize(tempInventory.getContents()) < tempInventory.getSize()) {
                    if (itemStack != null && !itemStack.getType().isAir()) {
                        tempInventory.addItem(itemStack);
                        counter++;
                    }
                }
                else if (itemStack != null && location != null && location.getWorld() != null) {
                    location.getWorld().dropItem(location, itemStack);
                }
            }

            counter = 0;

            for (ItemStack itemStack : tempInventory.getContents()) {
                if (itemStack != null) {
                    counter++;
                }
            }

            Inventory inventory = plugin.getServer()
                                        .createInventory(inventoryHolder, InventoryUtil.getInventorySize(counter), title);

            for (ItemStack itemStack : tempInventory.getContents()) {
                if (itemStack != null && location != null && location.getWorld() != null) {
                    inventory.addItem(itemStack).forEach((key, value) -> location.getWorld().dropItem(location, value));
                }
            }

            if (storageMode == Grave.StorageMode.CHESTSORT && plugin.getIntegrationManager().hasChestSort()) {
                plugin.getIntegrationManager().getChestSort().sortInventory(inventory);
            }

            return inventory;
        }

        if (storageMode == Grave.StorageMode.EXACT) {
            ItemStack itemStackAir = new ItemStack(Material.AIR);
            Inventory inventory = plugin.getServer()
                                        .createInventory(inventoryHolder, InventoryUtil.getInventorySize(itemStackList.size()), title);

            int counter = 0;
            for (ItemStack itemStack : itemStackList) {
                if (counter < inventory.getSize()) {
                    inventory.setItem(counter, itemStack != null ? itemStack : itemStackAir);
                }
                else if (itemStack != null && location != null && location.getWorld() != null) {
                    location.getWorld().dropItem(location, itemStack);
                }

                counter++;
            }

            return inventory;
        }

        return null;
    }

    public int getItemStacksSize(ItemStack[] itemStacks) {
        int counter = 0;

        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                counter++;
            }
        }

        return counter;
    }

    public List<ItemStack> filterGraveItemStackList(List<ItemStack> itemStackList, List<ItemStack> removedItemStackList, LivingEntity livingEntity, List<String> permissionList) {
        itemStackList = new ArrayList<>(itemStackList);

        if (livingEntity instanceof Player player
            && getStorageMode(plugin.getConfigString("storage.mode", livingEntity, permissionList))
               == Grave.StorageMode.EXACT) {
            List<ItemStack> playerInventoryContentList = Arrays.asList(player.getInventory().getContents());

            List<ItemStack> itemStackListNew = new ArrayList<>(playerInventoryContentList);
            List<ItemStack> differenceList = new ArrayList<>(removedItemStackList);

            differenceList.removeIf(itemStackList::contains);
            itemStackListNew.removeAll(differenceList);
            itemStackList.removeAll(playerInventoryContentList);

            if (!itemStackList.isEmpty()) {
                int counter = 0;

                for (ItemStack itemStack : new ArrayList<>(itemStackListNew)) {
                    if (!itemStackList.isEmpty()) {
                        if (itemStack == null) {
                            itemStackListNew.set(counter, itemStackList.get(0));
                        }
                        else {
                            itemStackListNew.add(itemStackList.get(0));
                        }

                        itemStackList.remove(0);
                    }

                    counter++;
                }
            }

            return itemStackListNew;
        }

        return itemStackList;
    }

    public void breakGrave(Location location, Grave grave) {
        dropGraveItems(location, grave);
        dropGraveExperience(location, grave);
        removeGrave(grave);
        plugin.debugMessage("Grave " + grave.getUUID() + " broken", 1);
    }

    public void dropGraveItems(Location location, Grave grave) {
        if (grave != null && location.getWorld() != null) {
            for (ItemStack itemStack : grave.getInventory()) {
                if (itemStack != null) {
                    location.getWorld().dropItemNaturally(location, itemStack);
                }
            }

            grave.getInventory().clear();
        }
    }

    public void giveGraveExperience(Player player, Grave grave) {
        if (grave.getExperience() > 0) {
            player.giveExp(grave.getExperience());
            grave.setExperience(0);
            plugin.getEntityManager().playWorldSound("ENTITY_EXPERIENCE_ORB_PICKUP", player);
        }
    }

    public void dropGraveExperience(Location location, Grave grave) {
        if (grave.getExperience() > 0 && location.getWorld() != null) {
            ExperienceOrb experienceOrb = (ExperienceOrb) location.getWorld()
                                                                  .spawnEntity(location, EntityType.EXPERIENCE_ORB);

            experienceOrb.setExperience(grave.getExperience());
            grave.setExperience(0);
        }
    }

    public List<Grave> getGraveList(Player player) {
        return getGraveList(player.getUniqueId());
    }

    public List<Grave> getGraveList(OfflinePlayer player) {
        return getGraveList(player.getUniqueId());
    }

    public List<Grave> getGraveList(Entity entity) {
        return getGraveList(entity.getUniqueId());
    }

    public List<Grave> getGraveList(UUID uuid) {
        List<Grave> graveList = new ArrayList<>();

        CacheManager.graveMap.forEach((key, value) -> {
            if (value.getOwnerUUID() != null && value.getOwnerUUID().equals(uuid)) {
                graveList.add(value);
            }
        });

        return graveList;
    }

    public int getGraveCount(Entity entity) {
        return getGraveList(entity).size();
    }

    public boolean openGrave(Entity entity, Location location, Grave grave) {
        if (entity instanceof Player player) {

            plugin.getEntityManager().swingMainHand(player);

            if (plugin.getEntityManager().canOpenGrave(player, grave)) {
                cleanupCompasses(player, grave);

                if (player.isSneaking() && player.hasPermission("graves.autoloot")) {
                    autoLootGrave(player, location, grave);
                }
                else if (player.hasPermission("graves.open")) {
                    player.openInventory(grave.getInventory());
                    plugin.getEntityManager().runCommands("event.command.open", player, location, grave);
                    plugin.getEntityManager().playWorldSound("sound.open", location, grave);
                }

                return true;
            }
            else {
                plugin.getEntityManager().sendMessage("message.protection", player, location, grave);
                plugin.getEntityManager().playWorldSound("sound.protection", location, grave);
            }
        }

        return false;
    }

    public void cleanupCompasses(Player player, Grave grave) {
        for (Map.Entry<ItemStack, UUID> entry : plugin.getEntityManager()
                                                      .getCompassesFromInventory(player)
                                                      .entrySet()) {
            if (grave.getUUID().equals(entry.getValue())) {
                player.getInventory().remove(entry.getKey());
            }
        }
    }

    public List<Location> getGraveLocationList(Location baseLocation, Grave grave) {
        List<Location> locationList = new ArrayList<>(plugin.getBlockManager().getBlockList(grave));
        Map<Double, Location> locationMap = new HashMap<>();
        List<Location> otherWorldLocationList = new ArrayList<>();

        if (baseLocation.getWorld() != null) {
            if (!locationList.contains(grave.getLocationDeath())) {
                locationList.add(grave.getLocationDeath());
            }

            for (Location location : locationList) {
                if (location.getWorld() != null && baseLocation.getWorld().equals(location.getWorld())) {
                    locationMap.put(location.distanceSquared(baseLocation), location);
                }
                else {
                    otherWorldLocationList.add(location);
                }
            }

            locationList = new ArrayList<>(new TreeMap<>(locationMap).values());

            locationList.addAll(otherWorldLocationList);
        }

        return locationList;
    }

    public Location getGraveLocation(Location location, Grave grave) {
        List<Location> locationList = plugin.getGraveManager().getGraveLocationList(location, grave);

        return !locationList.isEmpty() ? locationList.get(0) : null;
    }

    public void autoLootGrave(Entity entity, Location location, Grave grave) {
        if (entity instanceof Player player) {
            Grave.StorageMode storageMode = getStorageMode(plugin.getConfigString("storage.mode", grave));

            if (storageMode == Grave.StorageMode.EXACT) {
                List<ItemStack> itemStackListLeftOver = new ArrayList<>();
                int counter = 0;
                int inventorySize = player.getInventory().getSize();

                for (ItemStack itemStack : grave.getInventory().getContents()) {
                    if (itemStack != null) {
                        if (player.getInventory().getItem(counter) == null) {
                            if (counter < inventorySize) {
                                player.getInventory().setItem(counter, itemStack);
                                grave.getInventory().remove(itemStack);

                                if ((counter == 39 && InventoryUtil.isHelmet(itemStack))
                                    || (counter == 38
                                        && InventoryUtil.isChestplate(itemStack))
                                    || (counter == 37 && InventoryUtil.isLeggings(itemStack))
                                    || (counter == 36 && InventoryUtil.isBoots(itemStack))) {
                                    InventoryUtil.playArmorEquipSound(player, itemStack);
                                }
                            }
                            else {
                                itemStackListLeftOver.add(itemStack);
                            }
                        }
                        else {
                            itemStackListLeftOver.add(itemStack);
                        }
                    }

                    counter++;
                }

                grave.getInventory().clear();

                for (ItemStack itemStack : itemStackListLeftOver) {
                    for (Map.Entry<Integer, ItemStack> itemStackEntry : player.getInventory()
                                                                              .addItem(itemStack)
                                                                              .entrySet()) {
                        grave.getInventory()
                             .addItem(itemStackEntry.getValue())
                             .forEach((key, value) -> player.getWorld().dropItem(player.getLocation(), value));
                    }
                }
            }
            else {
                InventoryUtil.equipArmor(grave.getInventory(), player);
                InventoryUtil.equipItems(grave.getInventory(), player);
            }

            player.updateInventory();
            plugin.getDataManager()
                  .updateGrave(grave, "inventory", InventoryUtil.inventoryToString(grave.getInventory()));
            plugin.getEntityManager().runCommands("event.command.open", player, location, grave);

            if (grave.getItemAmount() <= 0) {
                plugin.getEntityManager().runCommands("event.command.loot", player, location, grave);
                plugin.getEntityManager().sendMessage("message.loot", player, location, grave);
                plugin.getEntityManager().playWorldSound("sound.close", location, grave);
                plugin.getEntityManager().spawnZombie(location, player, player, grave);
                giveGraveExperience(player, grave);
                playEffect("effect.loot", location, grave);
                closeGrave(grave);
                removeGrave(grave);
                plugin.debugMessage("Grave " + grave.getUUID() + " autolooted by " + player.getName(), 1);
            }
            else {
                plugin.getEntityManager().playWorldSound("sound.open", location, grave);
            }
        }
    }

    public String getDamageReason(EntityDamageEvent.DamageCause damageCause, Grave grave) {
        return plugin.getConfigString("message.death-reason."
                                      + damageCause.name(), grave, StringUtil.format(damageCause.name()));
    }

    public void playEffect(String string, Location location, Grave grave) {
        playEffect(string, location, 0, grave);
    }

    public void playEffect(String string, Location location, int data, Grave grave) {
        if (location.getWorld() != null) {
            if (grave != null) {
                string = plugin.getConfigString(string, grave);
            }

            if (string != null && !string.isEmpty()) {
                try {
                    location.getWorld().playEffect(location, Effect.valueOf(string.toUpperCase()), data);
                }
                catch (IllegalArgumentException exception) {
                    plugin.debugMessage(string.toUpperCase() + " is not an Effect ENUM", 1);
                }
            }
        }
    }

    public boolean shouldIgnoreItemStack(@NotNull ItemStack itemStack, Entity entity, List<String> permissionList) {
        if (plugin.getConfigStringList("ignore.item.material", entity, permissionList)
                  .contains(itemStack.getType().name())) {
            return true;
        }

        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (itemMeta.hasDisplayName()) {
                    for (String string : plugin.getConfigStringList("ignore.item.name", entity, permissionList)) {
                        if (!string.isEmpty() && itemMeta.getDisplayName()
                                                          .equals(StringUtil.parseString(string, plugin))) {
                            return true;
                        }
                    }

                    for (String string : plugin.getConfigStringList("ignore.item.name-contains", entity, permissionList)) {
                        if (!string.isEmpty() && itemMeta.getDisplayName()
                                                          .contains(StringUtil.parseString(string, plugin))) {
                            return true;
                        }
                    }
                }

                if (itemMeta.hasLore() && itemMeta.getLore() != null) {
                    for (String string : plugin.getConfigStringList("ignore.item.lore", entity, permissionList)) {
                        if (!string.isEmpty()) {
                            for (String lore : itemMeta.getLore()) {
                                if (lore.equals(StringUtil.parseString(string, plugin))) {
                                    return true;
                                }
                            }
                        }
                    }

                    for (String string : plugin.getConfigStringList("ignore.item.lore-contains", entity, permissionList)) {
                        if (!string.isEmpty()) {
                            for (String lore : itemMeta.getLore()) {
                                if (lore.contains(StringUtil.parseString(string, plugin))) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean shouldIgnoreBlock(Block block, Entity entity, Grave grave) {
        return shouldIgnoreBlock(block, entity, grave.getPermissionList());
    }

    public boolean shouldIgnoreBlock(Block block, Entity entity, List<String> permissionList) {
        List<String> stringList = plugin.getConfigStringList("ignore.block.material", entity, permissionList);

        for (String string : stringList) {
            if (!string.isEmpty() && string.equals(block.getType().name())) {
                return true;
            }
        }

        return false;
    }
}
