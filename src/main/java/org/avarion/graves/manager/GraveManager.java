package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.data.BlockData;
import org.avarion.graves.data.ChunkData;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.data.HologramData;
import org.avarion.graves.event.GraveTimeoutEvent;
import org.avarion.graves.integration.ChestSort;
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

import java.util.*;

public final class GraveManager {
    private static final ItemStack itemStackAir = new ItemStack(Material.AIR);
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
            for (Map.Entry<UUID, Grave> entry : plugin.getCacheManager().getGraveMap().entrySet()) {
                Grave grave = entry.getValue();

                if (grave.getTimeAliveRemaining() >= 0 && grave.getTimeAliveRemaining() <= 1000) {
                    GraveTimeoutEvent graveTimeoutEvent = new GraveTimeoutEvent(grave);

                    plugin.getServer().getPluginManager().callEvent(graveTimeoutEvent);

                    if (!graveTimeoutEvent.isCancelled()) {
                        if (graveTimeoutEvent.getLocation() != null && plugin.getConfig("drop.timeout", grave)
                                                                             .getBoolean("drop.timeout")) {
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
            for (Map.Entry<String, ChunkData> entry : plugin.getCacheManager().getChunkMap().entrySet()) {
                ChunkData chunkData = entry.getValue();

                if (chunkData.isLoaded()) {
                    Location location = new Location(chunkData.getWorld(), chunkData.getX() << 4, 0, chunkData.getZ()
                                                                                                     << 4);

                    // Entity data
                    for (EntityData entityData : new ArrayList<>(chunkData.getEntityDataMap().values())) {
                        if (plugin.getCacheManager().getGraveMap().containsKey(entityData.getUUIDGrave())) {
                            if (plugin.isEnabled() && entityData instanceof HologramData hologramData) {
                                Grave grave = plugin.getCacheManager().getGraveMap().get(hologramData.getUUIDGrave());

                                if (grave != null) {
                                    List<String> lineList = plugin.getConfig("hologram.line", grave)
                                                                  .getStringList("hologram.line");

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
                        if (blockData.getLocation().getWorld() != null) {
                            if (plugin.getCacheManager().getGraveMap().containsKey(blockData.getGraveUUID())) {
                                graveParticle(blockData.getLocation(), plugin.getCacheManager()
                                                                             .getGraveMap()
                                                                             .get(blockData.getGraveUUID()));
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
        if (location.getWorld() != null && plugin.getConfig("particle.enabled", grave).getBoolean("particle.enabled")) {
            Particle particle = Particle.REDSTONE;
            String particleType = plugin.getConfig("particle.type", grave).getString("particle.type");

            if (particleType != null && !particleType.equals("")) {
                try {
                    particle = Particle.valueOf(plugin.getConfig("particle.type", grave).getString("particle.type"));
                }
                catch (IllegalArgumentException ignored) {
                    plugin.debugMessage(particleType + " is not a Particle ENUM", 1);
                }
            }

            int count = plugin.getConfig("particle.count", grave).getInt("particle.count");
            double offsetX = plugin.getConfig("particle.offset.x", grave).getDouble("particle.offset.x");
            double offsetY = plugin.getConfig("particle.offset.y", grave).getDouble("particle.offset.y");
            double offsetZ = plugin.getConfig("particle.offset.z", grave).getDouble("particle.offset.z");
            location = location.clone().add(offsetX + 0.5, offsetY + 0.5, offsetZ + 0.5);

            if (location.getWorld() != null) {
                switch (particle.name()) {
                    case "REDSTONE":
                        int size = plugin.getConfig("particle.dust-size", grave).getInt("particle.dust-size");
                        Color color = ColorUtil.getColor(plugin.getConfig("particle.dust-color", grave)
                                                               .getString("particle.dust-color", "RED"));

                        if (color == null) {
                            color = Color.RED;
                        }

                        location.getWorld()
                                .spawnParticle(particle, location, count, new Particle.DustOptions(color, size));
                        break;
                    case "SHRIEK":
                        location.getWorld().spawnParticle(particle, location, count, 1);
                        break;
                    default:
                        location.getWorld().spawnParticle(particle, location, count);
                        break;
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
            case HOLOGRAM: {
                plugin.getHologramManager().removeHologram(entityData);

                break;
            }

            case FURNITURELIB: {
                plugin.getIntegrationManager().getFurnitureLib().removeEntityData(entityData);

                break;
            }

            case FURNITUREENGINE: {
                plugin.getIntegrationManager().getFurnitureEngine().removeEntityData(entityData);

                break;
            }

            case ITEMSADDER: {
                plugin.getIntegrationManager().getItemsAdder().removeEntityData(entityData);

                break;
            }

            case ORAXEN: {
                plugin.getIntegrationManager().getOraxen().removeEntityData(entityData);

                break;
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
            return Grave.StorageMode.valueOf(string.toUpperCase());
        }
        catch (IllegalArgumentException | NullPointerException ignored) {
            return Grave.StorageMode.COMPACT;
        }
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
        String title = StringUtil.parseString(plugin.getConfig("gui.grave.title", grave)
                                                    .getString("gui.grave.title"), livingEntity, grave.getLocationDeath(), grave, plugin);
        Grave.StorageMode storageMode = getStorageMode(plugin.getConfig("storage.mode", grave)
                                                             .getString("storage.mode"));

        return plugin.getGraveManager()
                     .createGraveInventory(grave, grave.getLocationDeath(), filterGraveItemStackList, title, storageMode);
    }

    public Inventory createGraveInventory(InventoryHolder inventoryHolder, Location location, List<ItemStack> itemStackList, String title, Grave.StorageMode storageMode) {
        itemStackList.removeIf(itemStack -> itemStack != null
                                            && itemStack.containsEnchantment(Enchantment.VANISHING_CURSE));

        Inventory inventory = plugin.getServer()
                                    .createInventory(inventoryHolder, InventoryUtil.getInventorySize(itemStackList.size()), title);

        int curPos = -1;

        if (storageMode == Grave.StorageMode.COMPACT) {
            for (ItemStack itemStack : itemStackList) {
                curPos++;

                if (itemStack == null || itemStack.getType().isAir()) {
                    continue;
                }

                if (curPos >= 36) { // armor & offhand
                    inventory.setItem(curPos, itemStack);
                }
                else {
                    inventory.addItem(itemStack);
                }
            }

            ChestSort chestSort = plugin.getIntegrationManager().getChestSort();
            if (chestSort != null) { // && chestSort.hasSortingEnabled(inventoryHolder)) { // TODO!!
                chestSort.sortInventory(inventory);
            }

            return inventory;
        }
        else if (storageMode == Grave.StorageMode.EXACT) {
            for (ItemStack itemStack : itemStackList) {
                curPos++;

                if (itemStack == null || itemStack.getType().isAir()) {
                    continue;
                }

                inventory.setItem(curPos, itemStack);
            }

            return inventory;
        }

        return null;
    }

    public List<ItemStack> filterGraveItemStackList(List<ItemStack> itemStackList, LivingEntity livingEntity, List<String> permissionList) {
        return filterGraveItemStackList(itemStackList, new ArrayList<>(), livingEntity, permissionList);
    }

    public List<ItemStack> filterGraveItemStackList(List<ItemStack> itemStackList, List<ItemStack> removedItemStackList, LivingEntity livingEntity, List<String> permissionList) {
        itemStackList = new ArrayList<>(itemStackList);

        if (livingEntity instanceof Player player
            && getStorageMode(plugin.getConfig("storage.mode", livingEntity, permissionList).getString("storage.mode"))
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

    public void breakGrave(Grave grave) {
        breakGrave(grave.getLocationDeath(), grave);
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

        plugin.getCacheManager().getGraveMap().forEach((key, value) -> {
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
        if (!(entity instanceof Player player)) {
            return;
        }

        Inventory playerInv = player.getInventory();
        Inventory graveInv = grave.getInventory();
        Grave.StorageMode storageMode = getStorageMode(plugin.getConfig("storage.mode", grave)
                                                             .getString("storage.mode"));

        int counter = -1;
        int playerInvSize = playerInv.getSize();

        for (ItemStack itemStack : grave.getInventory().getContents()) {
            counter++;

            if (itemStack == null || itemStack.getType().isAir()) {
                continue;
            }

            final boolean posOccupied = playerInv.getItem(counter) != null;

            if (counter >= 36 && counter <= 40 && !posOccupied) {
                playerInv.setItem(counter, itemStack);
                graveInv.remove(itemStack);

                InventoryUtil.playArmorEquipSound(player, itemStack);
            }
            else if (storageMode == Grave.StorageMode.COMPACT) {
                playerInv.addItem(itemStack);
                graveInv.remove(itemStack);
            }
            // EXACT mode
            else if (!posOccupied && counter < playerInvSize) {
                playerInv.setItem(counter, itemStack);
                graveInv.remove(itemStack);
            }
        }

        player.updateInventory();
        plugin.getDataManager().updateGrave(grave, "inventory", InventoryUtil.inventoryToString(graveInv));
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

    public String getDamageReason(EntityDamageEvent.DamageCause damageCause, Grave grave) {
        return plugin.getConfig("message.death-reason." + damageCause.name(), grave)
                     .getString("message.death-reason." + damageCause.name(), StringUtil.format(damageCause.name()));
    }

    public void playEffect(String string, Location location) {
        playEffect(string, location, null);
    }

    public void playEffect(String string, Location location, Grave grave) {
        playEffect(string, location, 0, grave);
    }

    public void playEffect(String string, Location location, int data, Grave grave) {
        if (location.getWorld() != null) {
            if (grave != null) {
                string = plugin.getConfig(string, grave).getString(string);
            }

            if (string != null && !string.equals("")) {
                try {
                    location.getWorld().playEffect(location, Effect.valueOf(string.toUpperCase()), data);
                }
                catch (IllegalArgumentException exception) {
                    plugin.debugMessage(string.toUpperCase() + " is not an Effect ENUM", 1);
                }
            }
        }
    }

    public boolean shouldIgnoreItemStack(ItemStack itemStack, Entity entity, Grave grave) {
        return shouldIgnoreItemStack(itemStack, entity, grave.getPermissionList());
    }

    public boolean shouldIgnoreItemStack(ItemStack itemStack, Entity entity, List<String> permissionList) {
        if (plugin.getConfig("ignore.item.material", entity, permissionList)
                  .getStringList("ignore.item.material")
                  .contains(itemStack.getType().name())) {
            return true;
        }

        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (itemMeta.hasDisplayName()) {
                    for (String string : plugin.getConfig("ignore.item.name", entity, permissionList)
                                               .getStringList("ignore.item.name")) {
                        if (!string.equals("") && itemMeta.getDisplayName()
                                                          .equals(StringUtil.parseString(string, plugin))) {
                            return true;
                        }
                    }

                    for (String string : plugin.getConfig("ignore.item.name-contains", entity, permissionList)
                                               .getStringList("ignore.item.name-contains")) {
                        if (!string.equals("") && itemMeta.getDisplayName()
                                                          .contains(StringUtil.parseString(string, plugin))) {
                            return true;
                        }
                    }
                }

                if (itemMeta.hasLore() && itemMeta.getLore() != null) {
                    for (String string : plugin.getConfig("ignore.item.lore", entity, permissionList)
                                               .getStringList("ignore.item.lore")) {
                        if (!string.equals("")) {
                            for (String lore : itemMeta.getLore()) {
                                if (lore.equals(StringUtil.parseString(string, plugin))) {
                                    return true;
                                }
                            }
                        }
                    }

                    for (String string : plugin.getConfig("ignore.item.lore-contains", entity, permissionList)
                                               .getStringList("ignore.item.lore-contains")) {
                        if (!string.equals("")) {
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
        List<String> stringList = plugin.getConfig("ignore.block.material", entity, permissionList)
                                        .getStringList("ignore.block.material");

        for (String string : stringList) {
            if (!string.equals("") && string.equals(block.getType().name())) {
                return true;
            }
        }

        return false;
    }
}
