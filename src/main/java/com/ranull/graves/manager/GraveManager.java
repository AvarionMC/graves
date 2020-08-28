package com.ranull.graves.manager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.graves.api.events.GraveCreateEvent;
import com.ranull.graves.inventory.GraveInventory;
import com.ranull.graves.inventory.GraveListInventory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class GraveManager {
    private Graves plugin;
    private DataManager dataManager;
    private MessageManager messageManager;
    private ConcurrentMap<Location, GraveInventory> gravesMap = new ConcurrentHashMap<>();
    private List<String> hologramLines = new ArrayList<>();
    private List<Material> graveItemIgnore = new ArrayList<>();
    private List<Material> graveIgnore = new ArrayList<>();
    private OfflinePlayer graveHead;

    public GraveManager(Graves plugin, DataManager dataManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = messageManager;
        getSavedGraves();
        graveHeadLoad();
        graveItemIgnoreLoad();
        graveIgnoreLoad();
        hologramLinesLoad();
    }

    private void getSavedGraves() {
        plugin.getServer().getLogger().info("[Graves] Waiting 2 seconds before loading saved graves.");
        new BukkitRunnable() {
            @Override
            public void run() {
                gravesMap = dataManager.getSavedGraves();
                createHolograms();
            }
        }.runTaskLater(plugin, 40L);
        removeGraveTimer();
        updateGraveTimer();
    }

    public void updateGraveTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ConcurrentMap.Entry<Location, GraveInventory> entry : gravesMap.entrySet()) {
                    GraveInventory graveInventory = entry.getValue();

                    if (plugin.getServer().getWorlds().contains(graveInventory.getLocation().getWorld()) &&
                            Objects.requireNonNull(graveInventory.getLocation().getWorld())
                                    .isChunkLoaded(graveInventory.getLocation().getChunk())) {
                        if (plugin.getConfig().getBoolean("settings.hologram")) {
                            updateHologram(graveInventory);
                        }

                        if (plugin.getConfig().getBoolean("settings.particle")) {
                            graveParticle(graveInventory);
                        }

                        if (plugin.getConfig().getBoolean("settings.hologramAutoCleanup")) {
                            cleanupBrokenHolograms();
                        }

                        if (graveInventory.getProtectTime() > 0 && graveInventory.getProtected()) {
                            if (System.currentTimeMillis() - graveInventory.getCreatedTime() >= graveInventory.getProtectTime()) {
                                setGraveProtection(graveInventory, false);

                                if (graveInventory.getPlayer() != null &&
                                        plugin.getConfig().getBoolean("settings.protectUnlink")) {
                                    graveInventory.setUnlink(true);

                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            if (graveInventory.getPlayer().isOnline() &&
                                                    graveInventory.getPlayer().getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof GraveListInventory) {
                                                if (getPlayerGraves(graveInventory.getPlayer().getUniqueId()).size() >= 1) {
                                                    plugin.getGuiManager().openGraveGUI(graveInventory.getPlayer().getPlayer(), graveInventory.getPlayer().getPlayer());
                                                } else {
                                                    graveInventory.getPlayer().getPlayer().closeInventory();
                                                }
                                            }
                                        }
                                    }.runTask(plugin);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    public void removeGraveTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ConcurrentMap.Entry<Location, GraveInventory> entry : gravesMap.entrySet()) {
                    GraveInventory graveInventory = entry.getValue();

                    if (plugin.getServer().getWorlds().contains(graveInventory.getLocation().getWorld())) {
                        if (graveInventory.getAliveTime() > 0) {
                            if ((System.currentTimeMillis() - graveInventory.getCreatedTime()) >=
                                    graveInventory.getAliveTime()) {
                                if (plugin.getConfig().getBoolean("settings.timeoutDrop")) {
                                    dropGrave(graveInventory);
                                    dropExperience(graveInventory);
                                } else {
                                    destroyGrave(graveInventory);
                                }

                                removeHologram(graveInventory);
                                replaceGrave(graveInventory);
                                removeGrave(graveInventory);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public OfflinePlayer getGraveHead() {
        return graveHead;
    }

    public void graveParticle(GraveInventory graveInventory) {
        try {
            Particle particle = Particle.valueOf(plugin.getConfig().getString("settings.particleType"));

            Color color = getColor(Objects.requireNonNull(plugin.getConfig().getString("settings.particleColor")));

            if (color != null) {
                int count = plugin.getConfig().getInt("settings.particleCount");
                int size = plugin.getConfig().getInt("settings.particleSize");
                int height = plugin.getConfig().getInt("settings.particleHeight");

                Location location = graveInventory.getLocation().clone().add(0.5, 0.5, 0.5).add(0, height, 0);
                Objects.requireNonNull(graveInventory.getLocation().getWorld())
                        .spawnParticle(particle, location, count, new Particle.DustOptions(color, size));
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    public ConcurrentMap<Location, GraveInventory> getPlayerGraves(UUID uuid) {
        ConcurrentMap<Location, GraveInventory> playerGraves = new ConcurrentHashMap<>();

        for (ConcurrentMap.Entry<Location, GraveInventory> entry : gravesMap.entrySet()) {
            if (entry.getValue().getPlayer().getUniqueId().equals(uuid) && !entry.getValue().getUnlink()) {
                playerGraves.put(entry.getKey(), entry.getValue());
            }
        }

        return playerGraves;
    }

    public GraveInventory getGraveInventory(Location location) {
        return gravesMap.get(roundLocation(location));
    }

    public int getAliveTime(Player player) {
        List<Integer> gravePermissions = new ArrayList<>();

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().contains("graves.time.")) {
                try {
                    gravePermissions.add(Integer.parseInt(perm.getPermission().replace("graves.time.", "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!gravePermissions.isEmpty()) {
            return Collections.max(gravePermissions) * 1000;
        } else {
            return plugin.getConfig().getInt("settings.time") * 1000;
        }
    }

    public ItemStack getGraveTokenFromPlayer(Player player) {
        for (ItemStack itemStack : player.getInventory().getStorageContents()) {
            if (itemStack != null) {
                if (hasRecipeData(itemStack)) {
                    return itemStack;
                }
            }
        }

        return null;
    }

    public ItemStack getGraveToken() {
        Material tokenMaterial = Material.matchMaterial(Objects.requireNonNull(plugin.getConfig()
                .getString("settings.tokenItem")));

        if (tokenMaterial != null) {
            ItemStack itemStack = new ItemStack(tokenMaterial);

            setRecipeData(itemStack);

            if (itemStack.hasItemMeta()) {
                ItemMeta itemMeta = itemStack.getItemMeta();

                if (itemMeta != null) {
                    String graveTokenName = Objects.requireNonNull(plugin.getConfig()
                            .getString("settings.tokenName"))
                            .replace("&", "§");

                    itemMeta.setDisplayName(graveTokenName);

                    List<String> graveTokenLore = plugin.getConfig().getStringList("settings.tokenLore");
                    List<String> graveTokenLoreReplaced = new ArrayList<>();

                    for (String lore : graveTokenLore) {
                        graveTokenLoreReplaced.add(lore.replace("&", "§"));
                    }

                    itemMeta.setLore(graveTokenLoreReplaced);

                    itemStack.setItemMeta(itemMeta);
                }
            }

            return itemStack;
        }

        return null;
    }


    public void setRecipeData(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "gravesRecipe"),
                    PersistentDataType.INTEGER, 1);

            itemStack.setItemMeta(itemMeta);
        }
    }

    public boolean hasRecipeData(ItemStack item) {
        if (item.hasItemMeta()) {
            return Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(
                    new NamespacedKey(plugin, "gravesRecipe"), PersistentDataType.INTEGER);
        }

        return false;
    }

    public void setGraveProtection(GraveInventory graveInventory, Boolean protect) {
        graveInventory.setProtected(protect);

        messageManager.graveChangeProtect(graveInventory.getLocation());
    }

    public String parseProtect(GraveInventory graveInventory) {
        String protect;

        if (graveInventory.getProtected()) {
            protect = plugin.getConfig().getString("settings.protectProtectedMessage");
        } else {
            protect = plugin.getConfig().getString("settings.protectUnprotectedMessage");
        }

        if (protect != null) {
            if (graveInventory.getProtectTime() > 0) {
                Long protectTime = (graveInventory.getProtectTime() - System.currentTimeMillis() - graveInventory.getCreatedTime()) / 1000;
                protect = protect.replace("$time", getTimeString(protectTime));
            } else {
                String timeInfinite = plugin.getConfig().getString("settings.timeInfinite");
                if (timeInfinite != null) {
                    protect = protect.replace("$time", timeInfinite);
                }
            }

            return protect.replace("&", "§");
        }

        return "";
    }

    public int getProtectTime(Player player) {
        int protectTime = getPermissionHighestInt(player, "graves.protect.");

        if (protectTime > 0) {
            return protectTime * 1000;
        }

        return plugin.getConfig().getInt("settings.protectTime") * 1000;
    }

    public double getTeleportCost(Player player) {
        double teleportCost = getPermissionHighestDouble(player, "graves.teleport.");

        if (teleportCost > 0) {
            return teleportCost;
        }

        return plugin.getConfig().getDouble("settings.teleportCost");
    }

    public int getMaxGraves(Player player) {
        int maxGraves = getPermissionHighestInt(player, "graves.amount.");

        if (maxGraves > 0) {
            return maxGraves;
        }

        return plugin.getConfig().getInt("settings.maxGraves");
    }

    public int getPermissionHighestInt(Player player, String permission) {
        List<Integer> gravePermissions = new ArrayList<>();

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().contains(permission)) {
                try {
                    gravePermissions.add(Integer.parseInt(perm.getPermission().replace(permission, "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!gravePermissions.isEmpty()) {
            return Collections.max(gravePermissions);
        }

        return 0;
    }

    public double getPermissionHighestDouble(Player player, String permission) {
        List<Double> gravePermissions = new ArrayList<>();

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().contains(permission)) {
                try {
                    gravePermissions.add(Double.parseDouble(perm.getPermission().replace(permission, "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!gravePermissions.isEmpty()) {
            return Collections.max(gravePermissions);
        }

        return 0;
    }

    public GraveInventory createGrave(Player player, List<ItemStack> items) {
        if (graveIgnore.contains(player.getLocation().getBlock().getType())) {
            String ignoreMessage = Objects.requireNonNull(plugin.getConfig().getString("settings.ignoreMessage"))
                    .replace("$block", formatString(player.getLocation().getBlock().getType().toString()))
                    .replace("&", "§");
            if (!ignoreMessage.equals("")) {
                player.sendMessage(ignoreMessage);
            }

            return null;
        }

        Location location = getPlaceLocation(player.getLocation());

        if (location == null) {
            return null;
        }

        if (player.getLocation().distance(location) >= plugin.getConfig().getInt("settings.maxSearch")) {
            if (player.getLocation().getY() > 0 && player.getLocation().getY() < 256) {
                location = player.getLocation();
            }
        }

        if (plugin.getConfig().getBoolean("settings.onlyCanBuild")) {
            if (!canBuild(player, location)) {
                messageManager.buildDenied(player);

                return null;
            }
        }

        if (location == null) {
            String failureMessage = Objects.requireNonNull(plugin.getConfig()
                    .getString("settings.failureMessage"))
                    .replace("&", "§");
            if (!failureMessage.equals("")) {
                player.sendMessage(failureMessage);
            }

            return null;
        }

        Inventory inventory = createInventory(items);

        String graveTitle = Objects.requireNonNull(plugin.getConfig().getString("settings.title"))
                .replace("$entity", player.getName())
                .replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = player.getName() + "'s Grave";
        }

        GraveInventory graveInventory = new GraveInventory(roundLocation(location), inventory, graveTitle);

        graveInventory.setAliveTime(getAliveTime(player));

        if (plugin.getConfig().getBoolean("settings.protect") && player.hasPermission("graves.protection")) {
            graveInventory.setProtected(true);
            graveInventory.setProtectTime(getProtectTime(player));
        }

        graveInventory.setPlayer(player);
        graveInventory.setKiller(player.getKiller());
        graveInventory.setReplace(location.getBlock().getType());

        GraveCreateEvent graveCreateEvent = new GraveCreateEvent(graveInventory);
        plugin.getServer().getPluginManager().callEvent(graveCreateEvent);

        if (graveCreateEvent.isCancelled()) {
            return null;
        }

        placeGrave(graveInventory);

        gravesMap.put(graveInventory.getLocation(), graveInventory);

        String timeString = getTimeString((long) getAliveTime(player) / 1000);
        String deathMessage = Objects.requireNonNull(plugin.getConfig().getString("settings.deathMessage"))
                .replace("$world", Objects.requireNonNull(location.getWorld()).getName())
                .replace("$x", String.valueOf(location.getBlockX()))
                .replace("$y", String.valueOf(location.getBlockY()))
                .replace("$z", String.valueOf(location.getBlockZ()))
                .replace("$time", String.valueOf(timeString))
                .replace("&", "§");
        if (!deathMessage.equals("")) {
            player.sendMessage(deathMessage);
        }

        return graveInventory;
    }

    public GraveInventory createGrave(LivingEntity entity, List<ItemStack> items) {
        if (graveIgnore.contains(entity.getLocation().getBlock().getType())) {
            return null;
        }

        Location location = getPlaceLocation(entity.getLocation());

        if (location == null) {
            return null;
        }

        Inventory inventory = plugin.getServer().createInventory(null, getInventorySize(items.size()));

        for (ItemStack item : items) {
            if (item != null) {
                inventory.addItem(item);
            }
        }

        String graveTitle = Objects.requireNonNull(plugin.getConfig().getString("settings.title"))
                .replace("$entity", formatString(entity.getType().toString()))
                .replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = formatString(entity.getType().toString()) + "'s Grave";
        }

        GraveInventory graveInventory = new GraveInventory(roundLocation(location), inventory, graveTitle);

        graveInventory.setAliveTime(plugin.getConfig().getInt("settings.time") * 1000);
        graveInventory.setEntityType(entity.getType());
        graveInventory.setReplace(location.getBlock().getType());

        GraveCreateEvent graveCreateEvent = new GraveCreateEvent(graveInventory);
        plugin.getServer().getPluginManager().callEvent(graveCreateEvent);

        if (graveCreateEvent.isCancelled()) {
            return null;
        }

        placeGrave(graveInventory);

        gravesMap.put(location, graveInventory);

        return graveInventory;
    }

    public Inventory createInventory(List<ItemStack> items) {
        Inventory newInventory = plugin.getServer().createInventory(null, getInventorySize(items.size()));

        for (ItemStack item : items) {
            newInventory.addItem(item);
        }

        return newInventory;
    }

    public void placeGrave(GraveInventory graveInventory) {
        Material graveMaterial = Material.matchMaterial(Objects.requireNonNull(plugin.getConfig()
                .getString("settings.block")));

        if (graveMaterial == null) {
            graveMaterial = Material.PLAYER_HEAD;
        }

        Block block = graveInventory.getLocation().getBlock();
        BlockData blockData = block.getBlockData();

        if (blockData instanceof Levelled) {
            Levelled leveled = (Levelled) blockData;
            if (leveled.getLevel() != 0) {
                graveInventory.setReplace(Material.AIR);
            }
        }

        if (block.getType() == Material.NETHER_PORTAL) {
            graveInventory.setReplace(Material.AIR);
        }

        if (block.getBlockData() instanceof Openable) {
            graveInventory.setReplace(Material.AIR);
        }

        block.setType(graveMaterial);

        if (blockData instanceof Waterlogged) {
            Waterlogged waterlogged = (Waterlogged) blockData;
            waterlogged.setWaterlogged(false);
            block.setBlockData(waterlogged);
        }

        if (graveMaterial == Material.PLAYER_HEAD && block.getState() instanceof Skull) {
            Skull skull = (Skull) block.getState();

            Rotatable skullRotate = (Rotatable) block.getBlockData();
            BlockFace skullBlockFace;

            if (graveInventory.getPlayer() != null && graveInventory.getPlayer().getPlayer() != null) {
                skullBlockFace = getSkullBlockFace(graveInventory.getPlayer().getPlayer());
            } else {
                skullBlockFace = BlockFace.NORTH;
            }

            if (skullBlockFace != null) {
                skullRotate.setRotation(skullBlockFace);
                skull.setBlockData(skullRotate);
            }

            String headSkin = plugin.getConfig().getString("settings.headSkin");
            int headSkinType = plugin.getConfig().getInt("settings.headSkinType");

            if (headSkin != null) {
                if (headSkinType == 0) {
                    if (graveInventory.getPlayer() != null) {
                        skull.setOwningPlayer(graveInventory.getPlayer());
                    } else if (graveInventory.getEntityType() != null) {
                        // TODO Mob Heads
                    }
                } else if (headSkinType == 1) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            addSkullBlockTexture(graveInventory.getLocation().getBlock(), headSkin);
                        }
                    }.runTaskLater(plugin, 1L);
                } else if (headSkinType == 2) {
                    if (graveHead != null && headSkin.length() <= 16) {
                        skull.setOwningPlayer(graveHead);
                    }
                }
            }

            skull.update();
        }

        if (plugin.getConfig().getBoolean("settings.hologram")) {
            createHologram(graveInventory);
        }
    }

    public Location getPlaceLocation(Location location) {
        location = roundLocation(location);

        if (location.getY() < 0 || location.getY() > 256) {
            if (plugin.getConfig().getBoolean("settings.placeVoid")) {
                Location topLocation = getTop(location);

                if (topLocation != null) {
                    return topLocation;
                }

                return getVoid(location);
            } else {
                return null;
            }
        }

        if (plugin.getConfig().getBoolean("settings.placeLavaTop") &&
                location.getBlock().getType() == Material.LAVA) {

            Location lavaTopLocation = getLavaTop(location);
            if (lavaTopLocation != null) {
                return lavaTopLocation;
            }
        }

        if (plugin.getConfig().getBoolean("settings.placeGround")) {
            Location groundLocation = getGround(location);

            if (groundLocation != null) {
                return groundLocation;
            }
        }

        if (dataManager.graveReplace().contains(location.getBlock().getType()) ||
                isAir(location.getBlock().getType())) {
            return location;
        }

        return getTop(location);
    }

    public Location getTeleportLocation(Player player, Location location) {
        location.add(0.5, 1.0, 0.5);

        if (plugin.getConfig().getBoolean("settings.teleportUnsafe")) {
            return location;
        }

        if (isSafe(location)) {
            return location;
        }

        if (plugin.getConfig().getBoolean("settings.teleportTop")) {
            Location highestBlock = getHighestBlock(location);

            if (highestBlock != null) {
                highestBlock.add(0.5, 0, 0.5);

                String teleportTopMessage = Objects.requireNonNull(plugin.getConfig()
                        .getString("settings.teleportTopMessage"))
                        .replace("&", "§");

                if (Objects.requireNonNull(highestBlock.getWorld()).getEnvironment() != World.Environment.NETHER) {
                    if (!teleportTopMessage.equals("")) {
                        player.sendMessage(teleportTopMessage);
                    }

                    return highestBlock;
                } else {
                    if (plugin.getConfig().getBoolean("settings.teleportTopNether")) {
                        if (!teleportTopMessage.equals("")) {
                            player.sendMessage(teleportTopMessage);
                        }

                        return highestBlock;
                    }
                }
            }
        }

        if (player.hasPermission("graves.bypass")) {
            return location;
        }

        return null;
    }

    public Boolean isSafe(Location location) {
        Location up = location.clone().add(0, 1, 0);
        if (isLava(location)) {
            return false;
        }
        return (isAir(location.getBlock().getType()) || !isSoldNotLava(location.getBlock().getType())) &&
                (isAir(up.getBlock().getType()) || !isSoldNotLava(up.getBlock().getType()));
    }

    public Boolean isSoldNotLava(Material material) {
        return material.isSolid() && material != Material.LAVA;
    }

    public Boolean isLava(Location location) {
        Location up = location.clone().add(0, 1, 0);
        Location below = location.clone().subtract(0, 1, 0);

        return location.getBlock().getType() == Material.LAVA ||
                up.getBlock().getType() == Material.LAVA ||
                below.getBlock().getType() == Material.LAVA;
    }

    public Location getHighestBlock(Location location) {
        location = location.clone();
        location.setY(256);

        return getGround(location);
    }

    public Location getGround(Location location) {
        Block block = location.getBlock();

        int max = 0;
        while (max <= 256) {
            if (!dataManager.graveReplace().contains(block.getType()) && !isAir(block.getType())) {
                return block.getLocation().add(0, 1, 0);
            }

            block = block.getLocation().subtract(0, 1, 0).getBlock();
            max++;
        }

        return null;
    }

    public Location getTop(Location location) {
        location.setY(256);

        Block block = location.getBlock();

        int max = 0;
        while (max <= 256) {
            if (dataManager.graveReplace().contains(block.getType()) || !isAir(block.getType())) {
                return block.getLocation().add(0, 1, 0);
            }

            block = block.getLocation().subtract(0, 1, 0).getBlock();
            max++;
        }

        return null;
    }


    public Location getLavaTop(Location location) {
        Block block = location.getBlock();

        int max = 0;
        while (max <= 256) {
            if ((dataManager.graveReplace().contains(block.getType()) ||
                    isAir(block.getType())) && block.getType() != Material.LAVA) {
                return block.getLocation();
            }

            block = block.getLocation().add(0, 1, 0).getBlock();
            max++;
        }

        return null;
    }

    public Location getVoid(Location location) {
        location.setY(0);

        Block block = location.getBlock();

        int max = 0;

        while (max <= 256) {
            if (dataManager.graveReplace().contains(block.getType())) {
                return block.getLocation().add(0, 1, 0);
            }

            if (isAir(block.getType())) {
                return block.getLocation();
            }

            block = block.getLocation().add(0, 1, 0).getBlock();
            max++;
        }

        return null;
    }

    public void replaceGrave(GraveInventory graveInventory) {
        Material replaceMaterial = graveInventory.getReplaceMaterial();

        if (replaceMaterial == null) {
            replaceMaterial = Material.AIR;
        }

        graveInventory.getLocation().getBlock().setType(replaceMaterial);

        closeGrave(graveInventory);
    }

    public void removeGrave(GraveInventory graveInventory) {
        gravesMap.remove(graveInventory.getLocation());
    }

    public void dropGrave(GraveInventory graveInventory) {
        if (graveInventory != null) {
            for (ItemStack item : graveInventory.getInventory()) {
                if (item != null) {
                    Objects.requireNonNull(graveInventory.getLocation().getWorld())
                            .dropItemNaturally(graveInventory.getLocation(), item);
                }
            }

            graveInventory.getInventory().clear();
        }
    }

    public void closeGrave(GraveInventory graveInventory) {
        List<HumanEntity> inventoryViewers = graveInventory.getInventory().getViewers();

        for (HumanEntity humanEntity : new ArrayList<>(inventoryViewers)) {
            graveInventory.getInventory().getViewers().remove(humanEntity);

            humanEntity.closeInventory();
        }
    }

    public void closeGraves() {
        if (!gravesMap.isEmpty()) {
            for (ConcurrentMap.Entry<Location, GraveInventory> entry : gravesMap.entrySet()) {
                List<HumanEntity> inventoryViewers = entry.getValue().getInventory().getViewers();

                for (HumanEntity humanEntity : new ArrayList<>(inventoryViewers)) {
                    entry.getValue().getInventory().getViewers().remove(humanEntity);

                    humanEntity.closeInventory();
                }
            }
        }
    }

    public void removeHolograms() {
        if (!gravesMap.isEmpty()) {
            for (Map.Entry<Location, GraveInventory> entry : gravesMap.entrySet()) {
                removeHologram(entry.getValue());
            }
        }
    }

    public void createHolograms() {
        if (!gravesMap.isEmpty()) {
            for (Map.Entry<Location, GraveInventory> entry : gravesMap.entrySet()) {
                if (entry.getValue().getHolograms().isEmpty()) {
                    createHologram(entry.getValue());
                }
            }
        }
    }

    public boolean hasPermission(GraveInventory graveInventory, Player player) {
        boolean owner = false;
        boolean killer = false;
        boolean bypass = false;
        boolean unprotect = false;
        boolean entity = false;

        if (player.hasPermission("graves.bypass")) {
            bypass = true;
        }

        if (graveInventory.getEntityType() != null) {
            entity = true;
        }

        if (plugin.getConfig().getBoolean("settings.killerOpen") && graveInventory.getKiller() != null &&
                player.getUniqueId().equals(graveInventory.getKiller().getUniqueId())) {
            killer = true;
        }

        if (!graveInventory.getProtected()) {
            unprotect = true;
        }

        if (plugin.getConfig().getBoolean("settings.protect")) {
            if (graveInventory.getPlayer() != null) {
                if (player.getUniqueId().equals(graveInventory.getPlayer().getUniqueId())) {
                    owner = true;
                }
            }
        } else {
            bypass = true;
        }

        return owner || killer || entity || unprotect || bypass;
    }

    public void giveExperience(GraveInventory graveInventory, Player player) {
        if (graveInventory.getExperience() > 0) {
            player.giveExp(graveInventory.getExperience());

            String expMessage = Objects.requireNonNull(plugin.getConfig().getString("settings.expMessage"))
                    .replace("$level", getLevelFromExp(graveInventory.getExperience()))
                    .replace("$xp", String.valueOf(graveInventory.getExperience()))
                    .replace("&", "§");
            if (!expMessage.equals("")) {
                player.sendMessage(expMessage);
            }

            graveInventory.setExperience(0);
        }
    }

    public void dropExperience(GraveInventory graveInventory) {
        if (graveInventory.getExperience() > 0) {
            ExperienceOrb orb = (ExperienceOrb) Objects.requireNonNull(graveInventory.getLocation().getWorld())
                    .spawnEntity(graveInventory.getLocation(), EntityType.EXPERIENCE_ORB);
            orb.setExperience(graveInventory.getExperience());
            graveInventory.setExperience(0);
        }
    }

    public void destroyGrave(GraveInventory graveInventory) {
        graveInventory.getInventory().clear();
        graveInventory.setExperience(0);
    }

    public void updateHologram(GraveInventory graveInventory) {
        if (plugin.getConfig().getBoolean("settings.hologram")) {
            if (!graveInventory.getHolograms().isEmpty()) {
                for (ConcurrentMap.Entry<UUID, Integer> entry : graveInventory.getHolograms().entrySet()) {
                    ArmorStand armorStand = (ArmorStand) plugin.getServer().getEntity(entry.getKey());

                    if (armorStand != null) {
                        armorStand.setCustomName(parseHologram(entry.getValue(), graveInventory));
                    }
                }
            }
        }
    }

    public void autoLoot(GraveInventory graveInventory, Player player) {
        equipArmor(graveInventory.getInventory(), player);
        equipItems(graveInventory.getInventory(), player);

        if (graveInventory.getItemAmount() == 0) {
            giveExperience(graveInventory, player);
            removeHologram(graveInventory);
            replaceGrave(graveInventory);
            removeGrave(graveInventory);

            messageManager.graveLoot(graveInventory.getLocation(), player);
        }
    }

    public void equipArmor(Inventory inventory, Player player) {
        List<ItemStack> itemList = Arrays.asList(inventory.getContents());
        Collections.reverse(itemList);

        for (ItemStack itemStack : itemList) {
            if (itemStack != null) {
                if (player.getInventory().getHelmet() == null) {
                    if (isHelmet(itemStack)) {
                        player.getInventory().setHelmet(itemStack);
                        inventory.removeItem(itemStack);
                    }
                }

                if (player.getInventory().getChestplate() == null) {
                    if (isChestplate(itemStack)) {
                        player.getInventory().setChestplate(itemStack);
                        inventory.removeItem(itemStack);
                    }
                }

                if (player.getInventory().getLeggings() == null) {
                    if (isLeggings(itemStack)) {
                        player.getInventory().setLeggings(itemStack);
                        inventory.removeItem(itemStack);
                    }
                }

                if (player.getInventory().getBoots() == null) {
                    if (isBoots(itemStack)) {
                        player.getInventory().setBoots(itemStack);
                        inventory.removeItem(itemStack);
                    }
                }
            }
        }
    }

    public boolean isHelmet(ItemStack itemStack) {
        if (itemStack != null) {
            return itemStack.getType().toString().equals("DIAMOND_HELMET") ||
                    itemStack.getType().toString().equals("GOLDEN_HELMET") ||
                    itemStack.getType().toString().equals("IRON_HELMET") ||
                    itemStack.getType().toString().equals("LEATHER_HELMET") ||
                    itemStack.getType().toString().equals("CHAINMAIL_HELMET") ||
                    itemStack.getType().toString().equals("TURTLE_HELMET") ||
                    itemStack.getType().toString().equals("NETHERITE_HELMET");
        }

        return false;
    }

    public boolean isChestplate(ItemStack itemStack) {
        if (itemStack != null) {
            return itemStack.getType().toString().equals("DIAMOND_CHESTPLATE") ||
                    itemStack.getType().toString().equals("GOLDEN_CHESTPLATE") ||
                    itemStack.getType().toString().equals("IRON_CHESTPLATE") ||
                    itemStack.getType().toString().equals("LEATHER_CHESTPLATE") ||
                    itemStack.getType().toString().equals("CHAINMAIL_CHESTPLATE") ||
                    itemStack.getType().toString().equals("NETHERITE_CHESTPLATE") ||
                    itemStack.getType().toString().equals("ELYTRA");
        }

        return false;
    }

    public boolean isLeggings(ItemStack itemStack) {
        if (itemStack != null) {
            return itemStack.getType().toString().equals("DIAMOND_LEGGINGS") ||
                    itemStack.getType().toString().equals("GOLDEN_LEGGINGS") ||
                    itemStack.getType().toString().equals("IRON_LEGGINGS") ||
                    itemStack.getType().toString().equals("LEATHER_LEGGINGS") ||
                    itemStack.getType().toString().equals("CHAINMAIL_LEGGINGS") ||
                    itemStack.getType().toString().equals("NETHERITE_LEGGINGS");
        }

        return false;
    }

    public boolean isBoots(ItemStack itemStack) {
        if (itemStack != null) {
            return itemStack.getType().toString().equals("DIAMOND_BOOTS") ||
                    itemStack.getType().toString().equals("GOLDEN_BOOTS") ||
                    itemStack.getType().toString().equals("IRON_BOOTS") ||
                    itemStack.getType().toString().equals("LEATHER_BOOTS") ||
                    itemStack.getType().toString().equals("CHAINMAIL_BOOTS") ||
                    itemStack.getType().toString().equals("NETHERITE_BOOTS");
        }

        return false;
    }

    public void equipItems(Inventory inventory, Player player) {
        int freeSlots = getPlayerFreeSlots(player);
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                if (count < freeSlots) {
                    player.getInventory().addItem(item);
                    inventory.removeItem(item);

                    count++;
                }
            }
        }
    }

    public int getPlayerFreeSlots(Player player) {
        int freeSlots = 0;
        int count = 0;
        for (ItemStack itemStack : player.getInventory().getStorageContents()) {
            if (count <= 36) {
                if (itemStack == null) {
                    freeSlots++;
                    count++;
                }
            }
        }

        return freeSlots;
    }

    public String parseHologram(Integer lineNumber, GraveInventory graveInventory) {
        String line = hologramLines.get(lineNumber).replace("$itemCount",
                String.valueOf(getItemAmount(graveInventory.getInventory())))
                .replace("&", "§");

        if (graveInventory.getAliveTime() > 0) {
            Long aliveTime = (graveInventory.getAliveTime() - (System.currentTimeMillis() - graveInventory.getCreatedTime())) / 1000;
            String timeString = getTimeString(aliveTime);
            line = line.replace("$time", timeString);
        } else {
            String timeInfinite = Objects.requireNonNull(plugin.getConfig().getString("settings.timeInfinite"))
                    .replace("&", "§");
            line = line.replace("$time", timeInfinite);
        }

        if (graveInventory.getProtected()) {
            String protect = plugin.getConfig().getString("settings.protectUnprotectedMessage");
            if (graveInventory.getProtected()) {
                protect = plugin.getConfig().getString("settings.protectProtectedMessage");
            }

            if (protect != null) {
                if (graveInventory.getProtectTime() > 0) {
                    Long protectTime = (graveInventory.getProtectTime() - (System.currentTimeMillis() -
                            graveInventory.getCreatedTime())) / 1000;
                    protect = protect.replace("$time", getTimeString(protectTime));
                } else {
                    protect = protect.replace("$time", Objects.requireNonNull(plugin.getConfig()
                            .getString("settings.timeInfinite")));
                }

                protect = protect.replace("&", "§");
                line = line.replace("$protect", protect);
            }
        } else {
            line = line.replace("$protect", Objects.requireNonNull(plugin.getConfig()
                    .getString("settings.protectUnprotectedMessage")).replace("&", "§"));
        }

        if (graveInventory.getPlayer() != null && graveInventory.getPlayer().getName() != null) {
            line = line.replace("$entity", graveInventory.getPlayer().getName());
        } else if (graveInventory.getEntityType() != null) {
            line = line.replace("$entity", formatString(graveInventory.getEntityType().toString()));
        }

        if (graveInventory.getExperience() > 0) {
            line = line.replace("$level", getLevelFromExp(graveInventory.getExperience()));
            line = line.replace("$xp", String.valueOf(graveInventory.getExperience()));
        } else {
            line = line.replace("$level", "0");
            line = line.replace("$xp", "0");
        }

        return line;
    }

    public int cleanupHolograms() {
        int count = 0;

        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ArmorStand) {
                    for (String tag : entity.getScoreboardTags()) {
                        if (tag.contains("graveHologram")) {
                            entity.remove();
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    public void cleanupBrokenHolograms() {
        if (!plugin.getServer().getWorlds().isEmpty()) {
            for (World world : plugin.getServer().getWorlds()) {
                if (!world.getEntities().isEmpty()) {
                    Iterator<Entity> iterator = world.getEntities().iterator();

                    while (iterator.hasNext()) {
                        Object object = iterator.next();

                        if (object != null && object instanceof ArmorStand) {
                            ArmorStand armorStand = (ArmorStand) object;
                            GraveInventory graveInventory = getGraveFromHologram(armorStand);

                            if (graveInventory == null) {
                                for (String tag : armorStand.getScoreboardTags()) {
                                    if (tag.contains("graveHologram")) {
                                        armorStand.remove();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void createHologram(GraveInventory graveInventory) {
        Material graveMaterial = Material.matchMaterial(Objects.requireNonNull(
                plugin.getConfig().getString("settings.block")));

        if (graveMaterial == null) {
            graveMaterial = Material.PLAYER_HEAD;
        }

        Location location = graveInventory.getLocation().clone().add(0.5, 0, 0.5);
        if (graveMaterial == Material.PLAYER_HEAD) {
            location.subtract(0, 1.40, 0);
        } else if (graveMaterial == Material.AIR) {
            location.subtract(0, 2, 0);
            location.add(0, plugin.getConfig().getDouble("settings.airHeight"), 0);
        } else {
            location.subtract(0, 1, 0);
        }

        int lineNumber = 0;
        for (String ignored : plugin.getConfig().getStringList("settings.hologramLines")) {
            location.add(0, 0.28, 0);

            ArmorStand armorStand = Objects.requireNonNull(location.getWorld()).spawn(location, ArmorStand.class);
            armorStand.setInvulnerable(true);
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setCustomName(parseHologram(lineNumber, graveInventory));
            armorStand.setCustomNameVisible(plugin.getConfig().getBoolean("settings.hologram"));

            armorStand.addScoreboardTag("graveHologram");
            armorStand.addScoreboardTag("graveHologramLocation:" + Objects.requireNonNull(graveInventory.getLocation()
                    .getWorld()).getName() + "#" + graveInventory.getLocation().getX() + "#" + graveInventory.getLocation().getY() +
                    "#" + graveInventory.getLocation().getZ());

            graveInventory.addHologram(armorStand.getUniqueId(), lineNumber);

            lineNumber++;
        }
    }

    public void removeHologram(GraveInventory graveInventory) {
        if (!graveInventory.getHolograms().isEmpty()) {
            for (Iterator<Map.Entry<UUID, Integer>> iterator = graveInventory.getHolograms().entrySet()
                    .iterator(); iterator.hasNext(); ) {
                ConcurrentMap.Entry<UUID, Integer> entry = iterator.next();

                ArmorStand armorStand = (ArmorStand) plugin.getServer().getEntity(entry.getKey());

                if (armorStand != null) {
                    armorStand.remove();
                }

                iterator.remove();
            }
        }
    }

    public GraveInventory getGraveFromHologram(ArmorStand armorStand) {
        for (String tag : armorStand.getScoreboardTags()) {
            if (tag.contains("graveHologramLocation:")) {
                String[] cords = tag.replace("graveHologramLocation:", "").split("#");

                try {
                    World world = plugin.getServer().getWorld(cords[0]);

                    double x = Double.parseDouble(cords[1]);
                    double y = Double.parseDouble(cords[2]);
                    double z = Double.parseDouble(cords[3]);

                    Location location = new Location(world, x, y, z);

                    return getGraveInventory(location);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                }
            }
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    public void graveHeadLoad() {
        String graveHeadName = plugin.getConfig().getString("settings.headSkin");

        if (graveHeadName != null && !graveHeadName.equals("") && graveHeadName.length() <= 16) {
            graveHead = plugin.getServer().getOfflinePlayer(graveHeadName);
        }
    }

    public Boolean shouldIgnore(ItemStack itemStack) {
        if (graveItemIgnore.contains(itemStack.getType())) {
            return true;
        }

        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (itemMeta.hasDisplayName()) {
                    for (String string : plugin.getConfig().getStringList("settings.itemIgnoreName")) {
                        string = string.replace("&", "§");
                        if (itemMeta.getDisplayName().equals(string)) {
                            return true;
                        }
                    }

                    for (String string : plugin.getConfig().getStringList("settings.itemIgnoreNameContains")) {
                        string = string.replace("&", "§");
                        if (itemMeta.getDisplayName().contains(string)) {
                            return true;
                        }
                    }
                }

                if (itemMeta.hasLore()) {
                    for (String string : plugin.getConfig().getStringList("settings.itemIgnoreLore")) {
                        for (String lore : Objects.requireNonNull(itemMeta.getLore())) {
                            string = string.replace("&", "§");
                            if (lore.equals(string)) {
                                return true;
                            }
                        }
                    }

                    for (String string : plugin.getConfig().getStringList("settings.itemIgnoreLoreContains")) {
                        for (String lore : Objects.requireNonNull(itemMeta.getLore())) {
                            string = string.replace("&", "§");
                            if (lore.contains(string)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public void graveItemIgnoreLoad() {
        graveItemIgnore.clear();

        for (String line : plugin.getConfig().getStringList("settings.itemIgnore")) {
            Material material = Material.matchMaterial(line.toUpperCase());
            if (material != null) {
                graveItemIgnore.add(material);
            }
        }
    }

    public void graveIgnoreLoad() {
        graveIgnore.clear();

        for (String line : plugin.getConfig().getStringList("settings.ignore")) {
            Material material = Material.matchMaterial(line.toUpperCase());
            if (material != null) {
                graveIgnore.add(material);
            }
        }
    }

    public void graveSpawnZombie(GraveInventory graveInventory, Player player) {
        if (graveInventory.getPlayer() != null && graveInventory.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            if (!plugin.getConfig().getBoolean("settings.zombieOwner")) {
                return;
            }
        } else {
            if (!plugin.getConfig().getBoolean("settings.zombieOther")) {
                return;
            }
        }

        spawnZombie(graveInventory, player);
    }

    public void spawnZombie(GraveInventory grave, LivingEntity target) {
        LivingEntity livingEntity = spawnZombie(grave);

        if (livingEntity instanceof Monster) {
            Monster monster = (Monster) livingEntity;
            monster.setTarget(target);
        }
    }

    public LivingEntity spawnZombie(GraveInventory graveInventory) {
        if (graveInventory.getPlayer() != null) {
            EntityType graveZombieType = EntityType.ZOMBIE;

            try {
                graveZombieType = EntityType.valueOf(Objects.requireNonNull(plugin.getConfig()
                        .getString("settings.zombieType")).toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }

            LivingEntity livingEntity = (LivingEntity) Objects.requireNonNull(graveInventory.getLocation().getWorld())
                    .spawnEntity(graveInventory.getLocation(), graveZombieType);

            if (plugin.getConfig().getBoolean("settings.zombieOwnerHead")) {
                Objects.requireNonNull(livingEntity.getEquipment()).setHelmet(getPlayerSkull(graveInventory.getPlayer()));
            }

            if (!plugin.getConfig().getBoolean("settings.zombiePickup")) {
                livingEntity.setCanPickupItems(false);
            }

            String zombieName = Objects.requireNonNull(plugin.getConfig().getString("settings.zombieName"))
                    .replace("$owner", Objects.requireNonNull(graveInventory.getPlayer().getName()))
                    .replace("&", "§");

            if (!zombieName.equals("")) {
                livingEntity.setCustomName(zombieName);
            }

            livingEntity.getScoreboardTags().add("graveZombie");

            return livingEntity;
        }
        return null;
    }

    public boolean isGraveZombie(LivingEntity livingEntity) {
        for (String tag : livingEntity.getScoreboardTags()) {
            if (tag.contains("graveZombie")) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getPlayerSkull(OfflinePlayer player) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta itemMeta = (SkullMeta) itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setOwningPlayer(player);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    public void addSkullBlockTexture(Block block, String base64) {
        if (block.getType() != Material.PLAYER_HEAD) {
            return;
        }

        Skull skull = (Skull) block.getState();

        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), null);

        gameProfile.getProperties().put("textures", new Property("textures",base64));

        try {
            Field profileField = skull.getClass().getDeclaredField("profile");

            profileField.setAccessible(true);

            profileField.set(skull, gameProfile);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
        }

        skull.update();
    }

    public void hologramLinesLoad() {
        hologramLines.clear();
        hologramLines.addAll(plugin.getConfig().getStringList("settings.hologramLines"));
        Collections.reverse(hologramLines);
    }

    public void runCreateCommands(GraveInventory graveInventory, LivingEntity livingEntity) {
        for (String command : plugin.getConfig().getStringList("settings.createCommands")) {
            if (plugin.getConfig().getBoolean("settings.commandsOnlyPlayers") && graveInventory.getEntityType() != null) {
                return;
            }

            runConsoleCommand(parseCommand(command, livingEntity, graveInventory));
        }
    }

    public void runLootCommands(GraveInventory graveInventory, Player player) {
        for (String command : plugin.getConfig().getStringList("settings.lootCommands")) {
            if (plugin.getConfig().getBoolean("settings.commandsOnlyPlayers") && graveInventory.getEntityType() != null) {
                return;
            }

            runConsoleCommand(parseCommand(command, player, graveInventory));
        }
    }

    public void runOpenCommands(GraveInventory graveInventory, Player player) {
        for (String command : plugin.getConfig().getStringList("settings.openCommands")) {
            if (plugin.getConfig().getBoolean("settings.commandsOnlyPlayers") && graveInventory.getEntityType() != null) {
                return;
            }

            runConsoleCommand(parseCommand(command, player, graveInventory));
        }
    }

    public void runBreakCommands(GraveInventory graveInventory, Player player) {
        for (String command : plugin.getConfig().getStringList("settings.breakCommands")) {
            if (plugin.getConfig().getBoolean("settings.commandsOnlyPlayers") && graveInventory.getEntityType() != null) {
                return;
            }

            runConsoleCommand(parseCommand(command, player, graveInventory));
        }
    }

    public void runExplodeCommands(GraveInventory graveInventory, Entity entity) {
        runExplodeCommands(graveInventory, getEntityName(entity));
    }

    public void runExplodeCommands(GraveInventory graveInventory, Block block) {
        runExplodeCommands(graveInventory, getBlockName(block));
    }

    public void runExplodeCommands(GraveInventory graveInventory, String entityOrBlockName) {
        for (String command : plugin.getConfig().getStringList("settings.explodeCommands")) {
            if (plugin.getConfig().getBoolean("settings.commandsOnlyPlayers") && graveInventory.getEntityType() != null) {
                return;
            }

            runConsoleCommand(parseCommand(command, entityOrBlockName, graveInventory));
        }
    }

    public void runTeleportCommands(GraveInventory graveInventory, Player player) {
        for (String command : plugin.getConfig().getStringList("settings.teleportCommands")) {
            if (plugin.getConfig().getBoolean("settings.commandsOnlyPlayers") && graveInventory.getEntityType() != null) {
                return;
            }

            runConsoleCommand(parseCommand(command, player, graveInventory));
        }
    }

    public void runConsoleCommand(String command) {
        ServerCommandEvent commandEvent = new ServerCommandEvent(plugin.getServer().getConsoleSender(), command);
        plugin.getServer().getPluginManager().callEvent(commandEvent);

        if (!commandEvent.isCancelled()) {
            plugin.getServer().getScheduler().callSyncMethod(plugin, ()
                    -> plugin.getServer().dispatchCommand(commandEvent.getSender(), commandEvent.getCommand()));
        }
    }

    public String parseCommand(String command, Entity entity, GraveInventory graveInventory) {
        return parseCommand(command, getEntityName(entity), graveInventory);
    }

    public String parseCommand(String command, String entityOrBlockName, GraveInventory graveInventory) {
        command = command.replace("$entity", entityOrBlockName)
                .replace("$block", entityOrBlockName)
                .replace("$player", entityOrBlockName)
                .replace("$owner", getOwnerName(graveInventory))
                .replace("$world", Objects.requireNonNull(graveInventory.getLocation().getWorld()).getName())
                .replace("$x", String.valueOf(graveInventory.getLocation().getBlockX()))
                .replace("$y", String.valueOf(graveInventory.getLocation().getBlockY()))
                .replace("$z", String.valueOf(graveInventory.getLocation().getBlockZ()))
                .replace("&", "§");

        if (graveInventory.getAliveTime() > 0) {
            long aliveTime = (graveInventory.getAliveTime() - (System.currentTimeMillis() - graveInventory.getCreatedTime())) / 1000;
            String timeString = getTimeString(aliveTime);
            command = command.replace("$time", timeString);
        } else {
            String timeInfinite = Objects.requireNonNull(plugin.getConfig().getString("settings.timeInfinite"))
                    .replace("&", "§");
            command = command.replace("$time", timeInfinite);
        }

        return command;
    }

    public static String getEntityName(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            return player.getName();
        }

        return formatString(entity.getType().toString());
    }

    public static String getBlockName(Block block) {
        return formatString(block.getType().toString());
    }

    public static String getOwnerName(GraveInventory graveInventory) {
        if (graveInventory.getPlayer() != null) {
            return graveInventory.getPlayer().getName();
        } else if (graveInventory.getEntityType() != null) {
            return formatString(graveInventory.getEntityType().toString());
        }
        return "Player";
    }

    public static Color getColor(String string) {
        switch (string.toUpperCase()) {
            case "AQUA":
                return Color.AQUA;
            case "BLACK":
                return Color.BLACK;
            case "BLUE":
                return Color.BLUE;
            case "FUCHSIA":
                return Color.FUCHSIA;
            case "GRAY":
                return Color.GRAY;
            case "GREEN":
                return Color.GREEN;
            case "LIME":
                return Color.LIME;
            case "MAROON":
                return Color.MAROON;
            case "NAVY":
                return Color.NAVY;
            case "OLIVE":
                return Color.OLIVE;
            case "ORANGE":
                return Color.ORANGE;
            case "PURPLE":
                return Color.PURPLE;
            case "RED":
                return Color.RED;
            case "SILVER":
                return Color.SILVER;
            case "TEAL":
                return Color.TEAL;
            case "WHITE":
                return Color.WHITE;
            case "YELLOW":
                return Color.YELLOW;
            default:
                return null;
        }
    }

    public static String formatString(String string) {
        string = string.replace("_", " ");
        return WordUtils.capitalizeFully(string);
    }

    public static Location roundLocation(Location location) {
        return new Location(location.getWorld(), Math.round(location.getBlockX()), Math.round(location.getY()),
                Math.round(location.getBlockZ()));
    }

    public static int getItemAmount(Inventory inventory) {
        int count = 0;

        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null) {
                count++;
            }
        }

        return count;
    }

    public void saveGraves() {
        for (ConcurrentMap.Entry<Location, GraveInventory> entry : gravesMap.entrySet()) {
            dataManager.saveGrave(entry.getValue());
        }
    }

    public static int getInventorySize(Integer size) {
        if (size <= 9) {
            return 9;
        } else if (size <= 18) {
            return 18;
        } else if (size <= 27) {
            return 27;
        } else if (size <= 36) {
            return 36;
        } else if (size <= 45) {
            return 45;
        } else {
            return 54;
        }
    }

    private BlockFace getSkullBlockFace(LivingEntity livingEntity) {
        float direction = livingEntity.getLocation().getYaw() % 360;

        if (direction < 0) {
            direction += 360;
        }

        direction = Math.round(direction / 45);

        switch ((int) direction) {
            case 0:
                return BlockFace.NORTH;
            case 1:
                return BlockFace.NORTH_EAST;
            case 2:
                return BlockFace.EAST;
            case 3:
                return BlockFace.SOUTH_EAST;
            case 4:
                return BlockFace.SOUTH;
            case 5:
                return BlockFace.SOUTH_WEST;
            case 6:
                return BlockFace.WEST;
            case 7:
                return BlockFace.NORTH_WEST;
            default:
                return null;
        }
    }

    public boolean canBuild(Player player, Location location) {
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(location.getBlock(), location.getBlock().getState(),
                location.getBlock(), new ItemStack(Material.AIR), player, true, EquipmentSlot.HAND);
        plugin.getServer().getPluginManager().callEvent(placeEvent);

        return !placeEvent.isCancelled() && placeEvent.canBuild();
    }

    public int getPlayerDropExp(Player player) {
        int experience = getPlayerExp(player);

        if (experience > 0) {
            float expStorePercent = (float) plugin.getConfig().getDouble("settings.expStorePercent");
            return (int) (experience * expStorePercent);
        }

        return -1;
    }

    public int getPlayerExp(Player player) {
        int experience = Math.round(getExpAtLevel(player.getLevel()) * player.getExp());
        int level = player.getLevel();

        while (level > 0) {
            level--;
            experience += getExpAtLevel(level);
        }

        if (experience < 0) {
            return -1;
        }

        return experience;
    }

    public int getExpAtLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        }

        return 9 * level - 158;
    }

    public String getLevelFromExp(long experience) {
        double result = 0;

        if (experience > 1395) {
            result = (Math.sqrt(72 * experience - 54215) + 325) / 18;
        } else if (experience > 315) {
            result = Math.sqrt(40 * experience - 7839) / 10 + 8.1;
        } else if (experience > 0) {
            result = Math.sqrt(experience + 9) - 3;
        }

        result = Math.round(result * 100.0) / 100.0;

        if (plugin.getConfig().getBoolean("settings.expLevelRound")) {
            return String.valueOf((int) result);
        }

        return String.valueOf(result);
    }

    public boolean isAir(Material material) {
        switch (material) {
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
                return true;
            default:
                return false;
        }
    }

    public String getTimeString(Long seconds) {
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hour = TimeUnit.SECONDS.toHours(seconds) - (day * 24);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        String timeDay = "";
        String timeHour = "";
        String timeMinute = "";
        String timeSecond = "";

        if (day > 0) {
            timeDay = Objects.requireNonNull(plugin.getConfig().getString("settings.timeDay"))
                    .replace("$d", String.valueOf(day))
                    .replace("&", "§");
        }

        if (hour > 0) {
            timeHour = Objects.requireNonNull(plugin.getConfig().getString("settings.timeHour"))
                    .replace("$h", String.valueOf(hour))
                    .replace("&", "§");
        }

        if (minute > 0) {
            timeMinute = Objects.requireNonNull(plugin.getConfig().getString("settings.timeMinute"))
                    .replace("$m", String.valueOf(minute))
                    .replace("&", "§");
        }

        if (second > 0) {
            timeSecond = Objects.requireNonNull(plugin.getConfig().getString("settings.timeSecond"))
                    .replace("$s", String.valueOf(second))
                    .replace("&", "§");
        }

        return StringUtils.normalizeSpace(timeDay + timeHour + timeMinute + timeSecond);
    }
}