package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.event.GraveTimeoutEvent;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.util.ColorUtil;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.MaterialUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class GraveManager {
    private final Graves plugin;

    public GraveManager(Graves plugin) {
        this.plugin = plugin;

        graveTimer();
    }

    @SuppressWarnings("ConstantConditions")
    public void graveTimer() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<Grave> graveRemoveList = new ArrayList<>();
            List<EntityData> entityDataRemoveList = new ArrayList<>();
            List<BlockData> blockDataRemoveList = new ArrayList<>();

            // Graves
            for (Map.Entry<UUID, Grave> entry : plugin.getDataManager().getGraveMap().entrySet()) {
                Grave grave = entry.getValue();

                if (grave.getTimeAliveRemaining() >= 0 && grave.getTimeAliveRemaining() <= 1000) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        GraveTimeoutEvent graveTimeoutEvent = new GraveTimeoutEvent(grave);

                        plugin.getServer().getPluginManager().callEvent(graveTimeoutEvent);

                        if (!graveTimeoutEvent.isCancelled()) {
                            if (graveTimeoutEvent.getLocation() != null
                                    && plugin.getConfig("drop.timeout", grave)
                                    .getBoolean("drop.timeout")) {
                                dropGraveItems(graveTimeoutEvent.getLocation(), grave);
                                dropGraveExperience(graveTimeoutEvent.getLocation(), grave);
                            }

                            closeGrave(grave);
                            graveRemoveList.add(grave);
                        }
                    });
                }

                // Protection
                if (grave.getProtection() && grave.getTimeProtectionRemaining() == 0) {
                    toggleGraveProtection(grave);
                }
            }

            // Chunks
            for (Map.Entry<String, ChunkData> chunkDataEntry : plugin.getDataManager().getChunkDataMap()
                    .entrySet()) {
                ChunkData chunkData = chunkDataEntry.getValue();

                if (chunkData.isLoaded()) {
                    Location location = new Location(chunkData.getWorld(),
                            chunkData.getX() << 4, 0, chunkData.getZ() << 4);

                    // Entity data
                    for (EntityData entityData : new ArrayList<>(chunkData.getEntityDataMap().values())) {
                        if (plugin.getDataManager().getGraveMap().containsKey(entityData.getUUIDGrave())) {
                            if (entityData instanceof HologramData) {
                                HologramData hologramData = (HologramData) entityData;
                                Chunk chunk = chunkData.getWorld().getChunkAt(chunkData.getX(), chunkData.getZ());
                                Grave grave = plugin.getDataManager().getGraveMap().get(entityData.getUUIDGrave());
                                List<String> lineList = plugin.getConfig("hologram.line", grave)
                                        .getStringList("hologram.line");

                                Collections.reverse(lineList);

                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    int counter = 0;
                                    for (Entity entity : chunk.getEntities()) {
                                        if (entity.getUniqueId().equals(entityData.getUUIDEntity())) {
                                            if (hologramData.getLine() < lineList.size()) {
                                                entity.setCustomName(StringUtil.parseString(lineList
                                                        .get(hologramData.getLine()), location, grave, plugin));
                                            } else {
                                                entityDataRemoveList.add(hologramData);
                                            }
                                        }

                                        counter++;
                                    }

                                    if (counter < chunk.getEntities().length) {
                                        entityDataRemoveList.add(hologramData);
                                    }
                                });
                            }
                        } else {
                            entityDataRemoveList.add(entityData);
                        }
                    }

                    // Blocks
                    for (BlockData blockData : new ArrayList<>(chunkData.getBlockDataMap().values())) {
                        if (blockData.getLocation().getWorld() != null) {
                            if (plugin.getDataManager().getGraveMap().containsKey(blockData.getGraveUUID())) {
                                graveParticle(blockData.getLocation(), plugin.getDataManager().getGraveMap()
                                        .get(blockData.getGraveUUID()));
                            } else {
                                blockDataRemoveList.add(blockData);
                            }
                        }
                    }
                }
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                graveRemoveList.forEach(GraveManager.this::removeGrave);
                entityDataRemoveList.forEach(GraveManager.this::removeEntityData);
                blockDataRemoveList.forEach(blockData -> plugin.getBlockManager().removeBlock(blockData));
                graveRemoveList.clear();
                blockDataRemoveList.clear();
                entityDataRemoveList.clear();

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getOpenInventory() != null) { // Mohist, might return null even when Bukkit shouldn't.
                        Inventory inventory = player.getOpenInventory().getTopInventory();

                        if (inventory.getHolder() instanceof GraveList) {
                            plugin.getGUIManager().setGraveListItems(inventory,
                                    ((GraveList) inventory.getHolder()).getUUID());
                        } else if (inventory.getHolder() instanceof GraveMenu) {
                            plugin.getGUIManager().setGraveMenuItems(inventory,
                                    ((GraveMenu) inventory.getHolder()).getGrave());
                        }
                    }
                }
            });
        }, 0L, 20L);
    }

    @SuppressWarnings("ConstantConditions")
    public void onDisable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory() != null) { // Mohist, might return null even when Bukkit shouldn't.
                InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();

                if (inventoryHolder instanceof Grave || inventoryHolder instanceof GraveList
                        || inventoryHolder instanceof GraveMenu) {
                    player.closeInventory();
                }
            }
        }
    }

    public void toggleGraveProtection(Grave grave) {
        grave.setProtection(!grave.getProtection());
        plugin.getDataManager().updateGrave(grave, "protection", String.valueOf(grave.getProtection() ? 1 : 0));
    }

    public void graveParticle(Location location, Grave grave) {
        if (location.getWorld() != null && plugin.getConfig("particle.enabled", grave)
                .getBoolean("particle.enabled")) {
            Particle particle = Particle.REDSTONE;
            String particleType = plugin.getConfig("particle.type", grave).getString("particle.type");

            if (particleType != null && !particleType.equals("")) {
                try {
                    particle = Particle.valueOf(plugin.getConfig("particle.type", grave)
                            .getString("particle.type"));
                } catch (IllegalArgumentException ignored) {
                    plugin.debugMessage(particleType + " is not a Particle ENUM", 1);
                }
            }

            int count = plugin.getConfig("particle.count", grave).getInt("particle.count");
            double offsetX = plugin.getConfig("particle.offset.x", grave).getDouble("particle.offset.x");
            double offsetY = plugin.getConfig("particle.offset.y", grave).getDouble("particle.offset.y");
            double offsetZ = plugin.getConfig("particle.offset.z", grave).getDouble("particle.offset.z");
            location = location.clone().add(offsetX + 0.5, offsetY + 0.5, offsetZ + 0.5);

            if (location.getWorld() != null) {
                if (particle == Particle.REDSTONE) {
                    int size = plugin.getConfig("particle.dust-size", grave).getInt("particle.dust-size");
                    Color color = ColorUtil.getColor(plugin.getConfig("particle.dust-color", grave)
                            .getString("particle.dust-color", "RED"));

                    if (color == null) {
                        color = Color.RED;
                    }

                    location.getWorld().spawnParticle(particle, location, count, new Particle.DustOptions(color, size));
                } else {
                    location.getWorld().spawnParticle(particle, location, count);
                }
            }
        }
    }

    public void removeGrave(Grave grave) {
        plugin.getBlockManager().removeBlock(grave);
        plugin.getHologramManager().removeHologram(grave);
        plugin.getDataManager().removeGrave(grave);

        if (plugin.hasFurnitureLib()) {
            plugin.getFurnitureLib().removeFurniture(grave);
        }

        if (plugin.hasFurnitureEngine()) {
            plugin.getFurnitureEngine().removeFurniture(grave);
        }

        if (plugin.hasItemsAdder()) {
            plugin.getItemsAdder().removeFurniture(grave);
        }

        if (plugin.hasOraxen()) {
            plugin.getOraxen().removeFurniture(grave);
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
                plugin.getFurnitureLib().removeEntityData(entityData);

                break;
            }

            case FURNITUREENGINE: {
                plugin.getFurnitureEngine().removeEntityData(entityData);

                break;
            }

            case ITEMSADDER: {
                plugin.getItemsAdder().removeEntityData(entityData);

                break;
            }

            case ORAXEN: {
                plugin.getOraxen().removeEntityData(entityData);

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

                if (inventoryHolder instanceof GraveMenu) {
                    GraveMenu graveMenu = (GraveMenu) inventoryHolder;

                    if (graveMenu.getGrave().getUUID().equals(grave.getUUID())) {
                        player.closeInventory();
                    }
                }
            }
        }
    }

    public Grave createGrave(Entity entity, List<ItemStack> itemStackList) {
        return createGrave(entity, itemStackList, plugin.getPermissionList(entity));
    }

    public Grave createGrave(Entity entity, List<ItemStack> itemStackList, List<String> permissionList) {
        Grave grave = new Grave(UUID.randomUUID());
        String entityName = plugin.getEntityManager().getEntityName(entity);

        grave.setOwnerType(entity.getType());
        grave.setOwnerName(entityName);
        grave.setOwnerNameDisplay(entity instanceof Player ? ((Player) entity).getDisplayName()
                : entity.getCustomName());
        grave.setOwnerUUID(entity.getUniqueId());
        grave.setInventory(createGraveInventory(grave, entity.getLocation(), itemStackList,
                StringUtil.parseString(plugin.getConfig("gui.grave.title", entity, permissionList)
                        .getString("gui.grave.title"), entity, entity.getLocation(), grave, plugin),
                getStorageMode(plugin.getConfig("storage.mode", entity, permissionList).getString("storage.mode"))));
        plugin.debugMessage("Creating grave " + grave.getUUID() + " for entity " + entityName, 1);

        return grave;
    }

    public Grave.StorageMode getStorageMode(String string) {
        try {
            Grave.StorageMode storageMode = Grave.StorageMode.valueOf(string.toUpperCase());

            if (storageMode == Grave.StorageMode.CHESTSORT && !plugin.hasChestSort()) {
                return Grave.StorageMode.COMPACT;
            }

            return storageMode;
        } catch (NullPointerException | IllegalArgumentException ignored) {
        }

        return Grave.StorageMode.COMPACT;
    }

    public void placeGrave(Location location, Grave grave) {
        location.setYaw(grave.getYaw());
        location.setPitch(grave.getPitch());

        plugin.getBlockManager().createBlock(location, grave);
        plugin.getHologramManager().createHologram(location, grave);

        if (plugin.hasWorldEdit()) {
            plugin.getWorldEdit().createSchematic(location, grave);
        }

        if (plugin.hasFurnitureLib()) {
            plugin.getFurnitureLib().createFurniture(location, grave);
        }

        if (plugin.hasFurnitureEngine()) {
            plugin.getFurnitureEngine().createFurniture(location, grave);
        }

        if (plugin.hasItemsAdder()) {
            plugin.getItemsAdder().createFurniture(location, grave);
        }

        if (plugin.hasOraxen()) {
            plugin.getOraxen().createFurniture(location, grave);
        }
    }

    public Inventory createGraveInventory(InventoryHolder inventoryHolder, Location location,
                                          List<ItemStack> itemStackList, String title, Grave.StorageMode storageMode) {
        if (location.getWorld() != null) {
            if (storageMode == Grave.StorageMode.COMPACT || storageMode == Grave.StorageMode.CHESTSORT) {
                Inventory tempInventory = plugin.getServer().createInventory(null, 54);
                int counter = 0;

                for (ItemStack itemStack : itemStackList) {
                    if (getItemStacksSize(tempInventory.getContents()) < tempInventory.getSize()) {
                        if (itemStack != null && !MaterialUtil.isAir(itemStack.getType())) {
                            tempInventory.addItem(itemStack);
                            counter++;
                        }
                    } else if (itemStack != null) {
                        location.getWorld().dropItem(location, itemStack);
                    }
                }

                counter = 0;

                for (ItemStack itemStack : tempInventory.getContents()) {
                    if (itemStack != null) {
                        counter++;
                    }
                }

                Inventory inventory = plugin.getServer().createInventory(inventoryHolder,
                        InventoryUtil.getInventorySize(counter), title);

                for (ItemStack itemStack : tempInventory.getContents()) {
                    if (itemStack != null) {
                        inventory.addItem(itemStack).forEach((key, value) -> location.getWorld().dropItem(location, value));
                    }
                }

                if (storageMode == Grave.StorageMode.CHESTSORT && plugin.hasChestSort()) {
                    plugin.getChestSort().sortInventory(inventory);
                }

                return inventory;
            } else if (storageMode == Grave.StorageMode.EXACT) {
                ItemStack itemStackAir = new ItemStack(Material.AIR);

                Inventory inventory = plugin.getServer().createInventory(inventoryHolder,
                        InventoryUtil.getInventorySize(itemStackList.size()), title);

                int counter = 0;
                for (ItemStack itemStack : itemStackList) {
                    if (counter < inventory.getSize()) {
                        inventory.setItem(counter, itemStack != null ? itemStack : itemStackAir);
                    } else if (itemStack != null) {
                        location.getWorld().dropItem(location, itemStack);
                    }

                    counter++;
                }

                return inventory;
            }
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

    public List<ItemStack> getGraveItemStackList(List<ItemStack> itemStackList, LivingEntity livingEntity, List<String> permissionList) {
        return getGraveItemStackList(itemStackList, new ArrayList<>(), livingEntity, permissionList);
    }

    public List<ItemStack> getGraveItemStackList(List<ItemStack> itemStackList, List<ItemStack> removedItemStackList,
                                                 LivingEntity livingEntity, List<String> permissionList) {
        if (livingEntity instanceof Player && getStorageMode(plugin.getConfig("storage.mode",
                livingEntity, permissionList).getString("storage.mode")) == Grave.StorageMode.EXACT) {
            Player player = (Player) livingEntity;
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
                        } else {
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
            plugin.getPlayerManager().playWorldSound("ENTITY_EXPERIENCE_ORB_PICKUP", player);
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

        plugin.getDataManager().getGraveMap().forEach((key, value) -> {
            if (value.getOwnerUUID() != null && value.getOwnerUUID().equals(uuid)) {
                graveList.add(value);
            }
        });

        return graveList;
    }

    public int getGraveCount(Entity entity) {
        return getGraveList(entity).size();
    }

    public boolean openGrave(Player player, Location location, Grave grave) {
        plugin.getPlayerManager().swingMainHand(player);

        if (plugin.getPlayerManager().canOpenGrave(player, grave)) {
            Map<ItemStack, UUID> compassFromInventory = plugin.getPlayerManager()
                    .getCompassFromInventory(player);

            if (compassFromInventory != null && !compassFromInventory.isEmpty()) {
                Map.Entry<ItemStack, UUID> entry = compassFromInventory.entrySet().iterator().next();

                if (grave.getUUID().equals(entry.getValue())) {
                    player.getInventory().remove(entry.getKey());
                }
            }

            if (player.isSneaking() && player.hasPermission("graves.autoloot")) {
                autoLootGrave(player, location, grave);
            } else if (player.hasPermission("graves.open")) {
                player.openInventory(grave.getInventory());
                plugin.getPlayerManager().runCommands("command.open", player, location, grave);
                plugin.getPlayerManager().playWorldSound("sound.open", location, grave);
            }

            return true;
        } else {
            plugin.getPlayerManager().sendMessage("message.protection", player, location, grave);
            plugin.getPlayerManager().playWorldSound("sound.protection", location, grave);
        }

        return false;
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
                } else {
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

    public void autoLootGrave(Player player, Location location, Grave grave) {
        Grave.StorageMode storageMode = getStorageMode(plugin.getConfig("storage.mode", grave)
                .getString("storage.mode"));

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
                                    || (counter == 38 && InventoryUtil.isChestplate(itemStack))
                                    || (counter == 37 && InventoryUtil.isLeggings(itemStack))
                                    || (counter == 36 && InventoryUtil.isBoots(itemStack))) {
                                InventoryUtil.playArmorEquipSound(player, itemStack);
                            }
                        } else {
                            itemStackListLeftOver.add(itemStack);
                        }
                    } else {
                        itemStackListLeftOver.add(itemStack);
                    }
                }

                counter++;
            }

            grave.getInventory().clear();

            for (ItemStack itemStack : itemStackListLeftOver) {
                for (Map.Entry<Integer, ItemStack> itemStackEntry : player.getInventory().addItem(itemStack).entrySet()) {
                    grave.getInventory().addItem(itemStackEntry.getValue())
                            .forEach((key, value) -> player.getWorld().dropItem(player.getLocation(), value));
                }
            }
        } else {
            InventoryUtil.equipArmor(grave.getInventory(), player);
            InventoryUtil.equipItems(grave.getInventory(), player);
        }

        player.updateInventory();
        plugin.getDataManager().updateGrave(grave, "inventory",
                InventoryUtil.inventoryToString(grave.getInventory()));
        plugin.getPlayerManager().runCommands("command.open", player, location, grave);

        if (grave.getItemAmount() <= 0) {
            plugin.getPlayerManager().runCommands("command.loot", player, location, grave);
            plugin.getPlayerManager().sendMessage("message.loot", player, location, grave);
            plugin.getPlayerManager().playWorldSound("sound.close", location, grave);
            plugin.getEntityManager().spawnZombie(location, player, player, grave);
            giveGraveExperience(player, grave);
            playEffect("effect.loot", location, grave);
            closeGrave(grave);
            removeGrave(grave);
            plugin.debugMessage("Grave " + grave.getUUID() + " autolooted by " + player.getName(), 1);
        } else {
            plugin.getPlayerManager().playWorldSound("sound.open", location, grave);
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
                } catch (IllegalArgumentException exception) {
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
                .getStringList("ignore.item.material").contains(itemStack.getType().name())) {
            return true;
        }

        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (itemMeta.hasDisplayName()) {
                    for (String string : plugin.getConfig("ignore.item.name", entity, permissionList)
                            .getStringList("ignore.item.name")) {
                        if (!string.equals("")
                                && itemMeta.getDisplayName().equals(string.replace("&", "§"))) {
                            return true;
                        }
                    }

                    for (String string : plugin.getConfig("ignore.item.name-contains", entity, permissionList)
                            .getStringList("ignore.item.name-contains")) {
                        if (!string.equals("")
                                && itemMeta.getDisplayName().contains(string.replace("&", "§"))) {
                            return true;
                        }
                    }
                }

                if (itemMeta.hasLore() && itemMeta.getLore() != null) {
                    for (String string : plugin.getConfig("ignore.item.lore", entity, permissionList)
                            .getStringList("ignore.item.lore")) {
                        if (!string.equals("")) {
                            for (String lore : itemMeta.getLore()) {
                                if (lore.equals(string.replace("&", "§"))) {
                                    return true;
                                }
                            }
                        }
                    }

                    for (String string : plugin.getConfig("ignore.item.lore-contains", entity, permissionList)
                            .getStringList("ignore.item.lore-contains")) {
                        if (!string.equals("")) {
                            for (String lore : itemMeta.getLore()) {
                                if (lore.contains(string.replace("&", "§"))) {
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
