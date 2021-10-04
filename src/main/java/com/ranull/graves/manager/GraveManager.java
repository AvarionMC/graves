package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class GraveManager {
    private final Graves plugin;

    public GraveManager(Graves plugin) {
        this.plugin = plugin;

        graveTimer();
    }

    public void graveTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Grave> graveRemoveList = new ArrayList<>();
                List<BlockData> blockDataRemoveList = new ArrayList<>();
                List<HologramData> hologramDataRemoveList = new ArrayList<>();

                // Graves
                for (Map.Entry<UUID, Grave> entry : plugin.getDataManager().getGraveMap().entrySet()) {
                    Grave grave = entry.getValue();

                    if (grave.getTimeAliveRemaining() >= 0 && grave.getTimeAliveRemaining() <= 1000) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
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
                            }
                        }.runTask(plugin);
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

                        // Holograms
                        for (HologramData hologramData : new ArrayList<>(chunkData.getHologramDataMap().values())) {
                            if (plugin.getDataManager().getGraveMap().containsKey(hologramData.getUUIDGrave())) {
                                Chunk chunk = chunkData.getWorld().getChunkAt(chunkData.getX(), chunkData.getZ());
                                Grave grave = plugin.getDataManager().getGraveMap().get(hologramData.getUUIDGrave());
                                List<String> lineList = plugin.getConfig("hologram.line", grave)
                                        .getStringList("hologram.line");

                                Collections.reverse(lineList);

                                int counter = 0;
                                for (Entity entity : chunk.getEntities()) {
                                    if (entity.getUniqueId().equals(hologramData.getUUIDEntity())) {
                                        if (hologramData.getLine() < lineList.size()) {
                                            entity.setCustomName(StringUtil.parseString(lineList
                                                    .get(hologramData.getLine()), location, grave, plugin));
                                        } else {
                                            hologramDataRemoveList.add(hologramData);
                                        }
                                    }

                                    counter++;
                                }

                                if (counter < chunk.getEntities().length) {
                                    hologramDataRemoveList.add(hologramData);
                                }
                            } else {
                                hologramDataRemoveList.add(hologramData);
                            }
                        }

                        // Blocks
                        for (BlockData blockData : new ArrayList<>(chunkData.getBlockDataMap().values())) {
                            if (blockData.getLocation().getWorld() != null) {
                                if (plugin.getDataManager().getGraveMap().containsKey(blockData.getGraveUUID())) {
                                    Grave grave = plugin.getDataManager().getGraveMap().get(blockData.getGraveUUID());

                                    graveParticle(blockData.getLocation(), grave);
                                } else {
                                    blockDataRemoveList.add(blockData);
                                }
                            }
                        }
                    }
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        graveRemoveList.forEach(GraveManager.this::removeGrave);
                        blockDataRemoveList.forEach(blockData -> plugin.getBlockManager().removeBlock(blockData));
                        hologramDataRemoveList.forEach(hologramData -> plugin.getHologramManager()
                                .removeHologram(hologramData));

                        for (Player player : plugin.getServer().getOnlinePlayers()) {
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
                }.runTask(plugin);
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    public void onDisable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();

            if (inventoryHolder instanceof Grave || inventoryHolder instanceof GraveList
                    || inventoryHolder instanceof GraveMenu) {
                player.closeInventory();
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
                    plugin.debugMessage(particleType + " is not a Particle ENUM");
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
        plugin.getBlockManager().removeBlocks(grave);
        plugin.getHologramManager().removeHolograms(grave);
        plugin.getDataManager().removeGrave(grave);
        plugin.debugMessage("Removing grave " + grave.getUUID());
    }

    public void closeGrave(Grave grave) {
        List<HumanEntity> inventoryViewers = grave.getInventory().getViewers();

        for (HumanEntity humanEntity : new ArrayList<>(inventoryViewers)) {
            grave.getInventory().getViewers().remove(humanEntity);
            humanEntity.closeInventory();
            plugin.debugMessage("Closing grave " + grave.getUUID() + " for " + humanEntity.getName());
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();

            if (inventoryHolder instanceof GraveMenu) {
                GraveMenu graveMenu = (GraveMenu) inventoryHolder;

                if (graveMenu.getGrave().getUUID().equals(grave.getUUID())) {
                    player.closeInventory();
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
        grave.setOwnerUUID(entity.getUniqueId());
        grave.setInventory(createGraveInventory(grave, entity.getLocation(), itemStackList,
                StringUtil.parseString(plugin.getConfig("gui.grave.title", entity, permissionList)
                        .getString("gui.grave.title"), entity, entity.getLocation(), grave, plugin)));
        plugin.debugMessage("Creating grave " + grave.getUUID() + " for entity " + entityName);

        return grave;
    }

    public void placeGrave(Location location, Grave grave) {
        plugin.getBlockManager().createBlock(location, grave);
        plugin.getHologramManager().createHologram(location, grave);

        if (plugin.hasWorldEdit()) {
            plugin.getSchematicManager().createSchematic(location, grave);
        }
    }

    public Inventory createGraveInventory(InventoryHolder inventoryHolder, Location location,
                                          List<ItemStack> itemStackList, String title) {
        Inventory tempInventory = plugin.getServer().createInventory(null,
                InventoryUtil.getInventorySize(itemStackList.size()));
        int count = 0;

        for (ItemStack itemStack : itemStackList) {
            if (itemStack != null && !MaterialUtil.isAir(itemStack.getType())) {
                tempInventory.addItem(itemStack);
            }
        }

        for (ItemStack itemStack : tempInventory.getContents()) {
            if (itemStack != null) {
                count++;
            }
        }

        Inventory inventory = plugin.getServer().createInventory(inventoryHolder,
                InventoryUtil.getInventorySize(count), title);

        for (ItemStack itemStack : tempInventory.getContents()) {
            if (itemStack != null && location.getWorld() != null) {
                inventory.addItem(itemStack).forEach((key, value) -> location.getWorld().dropItem(location, value));
            }
        }

        return inventory;
    }

    public void breakGrave(Grave grave) {
        breakGrave(grave.getLocationDeath(), grave);
    }

    public void breakGrave(Location location, Grave grave) {
        dropGraveItems(location, grave);
        dropGraveExperience(location, grave);
        removeGrave(grave);
        plugin.debugMessage("Grave " + grave.getUUID() + " broken ");
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
        InventoryUtil.equipArmor(grave.getInventory(), player);
        InventoryUtil.equipItems(grave.getInventory(), player);
        player.updateInventory();
        plugin.getPlayerManager().runCommands("command.open", player, location, grave);

        if (grave.getItemAmount() <= 0) {
            plugin.getPlayerManager().runCommands("command.loot", player, location, grave);
            plugin.getPlayerManager().sendMessage("message.loot", player, location, grave);
            plugin.getPlayerManager().playWorldSound("sound.close", location, grave);
            plugin.getEntityManager().spawnZombie(location, player, player, grave);
            playEffect("effect.loot", location);
            closeGrave(grave);
            giveGraveExperience(player, grave);
            removeGrave(grave);
            plugin.debugMessage("Grave " + grave.getUUID() + " autolooted by " + player.getName());
        } else {
            plugin.getPlayerManager().playWorldSound("sound.open", location, grave);
        }
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
                    plugin.debugMessage(string.toUpperCase() + " is not an Effect ENUM");
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
