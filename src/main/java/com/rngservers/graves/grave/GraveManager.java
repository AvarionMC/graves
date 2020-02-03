package com.rngservers.graves.grave;

import com.rngservers.graves.Main;
import com.rngservers.graves.data.DataManager;
import com.rngservers.graves.messages.Messages;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.ConsoleCommandSender;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class GraveManager {
    private Main plugin;
    private DataManager data;
    private Messages messages;
    private ConcurrentMap<Location, Grave> graves = new ConcurrentHashMap<>();
    private List<String> hologramLines = new ArrayList<>();
    private List<Material> graveItemIgnore = new ArrayList<>();
    private List<Material> graveIgnore = new ArrayList<>();
    private OfflinePlayer graveHead;

    public GraveManager(Main plugin, DataManager data, Messages messages) {
        this.plugin = plugin;
        this.data = data;
        this.messages = messages;
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
                graves = data.getSavedGraves();
                createHolograms();

                plugin.getServer().getLogger().info("[Graves] Loaded saved graves!");
            }
        }.runTaskLater(plugin, 20L);
        removeGraveTimer();
    }

    public void removeGraveTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ConcurrentMap.Entry<Location, Grave> entry : graves.entrySet()) {
                    Grave grave = entry.getValue();
                    if (plugin.getServer().getWorlds().contains(grave.getLocation().getWorld())) {
                        updateHologram(grave);
                        graveParticle(grave);
                        removeBrokenHolograms();
                        if (grave.getProtectTime() != null && grave.getProtected()) {
                            Long diff = System.currentTimeMillis() - grave.getCreatedTime();
                            if (diff >= grave.getProtectTime()) {
                                protectGrave(grave, false);
                            }
                        }
                        if (grave.getAliveTime() != null) {
                            long diff = System.currentTimeMillis() - grave.getCreatedTime();
                            if (diff >= grave.getAliveTime()) {
                                Boolean graveTimeoutDrop = plugin.getConfig().getBoolean("settings.graveTimeoutDrop");
                                if (graveTimeoutDrop) {
                                    dropGrave(grave);
                                    dropExperience(grave);
                                } else {
                                    destroyGrave(grave);
                                }
                                removeHologram(grave);
                                replaceGrave(grave);
                                removeGrave(grave);
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

    public void graveParticle(Grave grave) {
        Particle particle;
        try {
            particle = Particle.valueOf(plugin.getConfig().getString("settings.graveParticle"));
        } catch (IllegalArgumentException ignored) {
            return;
        }
        Color color = getColor(plugin.getConfig().getString("settings.graveParticleColor"));
        Integer count = plugin.getConfig().getInt("settings.graveParticleCount");
        Integer size = plugin.getConfig().getInt("settings.graveParticleSize");
        Integer height = plugin.getConfig().getInt("settings.graveParticleHeight");
        if (particle != null && color != null) {
            Location location = grave.getLocation().clone().add(0.5, 0.5, 0.5).add(0, height, 0);
            grave.getLocation().getWorld().spawnParticle(particle, location, count, new Particle.DustOptions(color, size));
        }
    }

    public ConcurrentMap<Location, Grave> getGraves(OfflinePlayer player) {
        ConcurrentMap<Location, Grave> playerGraves = new ConcurrentHashMap<>();
        for (ConcurrentMap.Entry<Location, Grave> entry : graves.entrySet()) {
            if (entry.getValue().getPlayer().getUniqueId().equals(player.getUniqueId())) {
                playerGraves.put(entry.getKey(), entry.getValue());
            }
        }
        return playerGraves;
    }

    public Grave getGrave(Location location) {
        return graves.get(roundLocation(location));
    }

    public void saveGraves() {
        for (ConcurrentMap.Entry<Location, Grave> entry : graves.entrySet()) {
            data.saveGrave(entry.getValue());
        }
    }

    public Integer getGraveTime(Player player) {
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
            return getGraveTime();
        }
    }

    public Integer getGraveTime() {
        Integer graveTime = plugin.getConfig().getInt("settings.graveTime") * 1000;
        if (graveTime > 0) {
            return graveTime;
        } else {
            return null;
        }
    }

    public ItemStack getGraveTokenFromPlayer(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (item != null) {
                if (hasRecipeData(item)) {
                    return item;
                }
            }
        }
        return null;
    }

    public ItemStack getGraveToken() {
        Material tokenMaterial = Material.matchMaterial(plugin.getConfig().getString("settings.graveTokenItem"));
        ItemStack item = new ItemStack(tokenMaterial);
        setRecipeData(item);
        ItemMeta meta = item.getItemMeta();

        String graveTokenName = plugin.getConfig().getString("settings.graveTokenName").replace("&", "§");
        meta.setDisplayName(graveTokenName);

        List<String> graveTokenLore = plugin.getConfig().getStringList("settings.graveTokenLore");
        List<String> graveTokenLoreReplaced = new ArrayList<>();
        for (String lore : graveTokenLore) {
            graveTokenLoreReplaced.add(lore.replace("&", "§"));
        }
        meta.setLore(graveTokenLoreReplaced);

        item.setItemMeta(meta);
        return item;
    }


    public ItemStack setRecipeData(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "gravesRecipe");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    public Boolean hasRecipeData(ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "gravesRecipe");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }

    public void protectGrave(Grave grave, Boolean protect) {
        grave.setProtected(protect);
        messages.graveChangeProtect(grave.getLocation());
    }

    public String parseProtect(Grave grave) {
        String protect;
        if (grave.getProtected() != null && grave.getProtected()) {
            protect = plugin.getConfig().getString("settings.graveProtectedProtectedMessage");
        } else {
            protect = plugin.getConfig().getString("settings.graveProtectedUnprotectedMessage");
        }
        if (grave.getProtectTime() != null) {
            Long protectTime = (grave.getProtectTime() - System.currentTimeMillis() - grave.getCreatedTime()) / 1000;
            protect.replace("$time", getTimeString(protectTime));
        } else {
            String timeInfinite = plugin.getConfig().getString("settings.timeInfinite");
            protect.replace("$time", timeInfinite);
        }
        return protect.replace("&", "§");
    }


    public Integer getProtectTime(Player player) {
        List<Integer> gravePermissions = new ArrayList<>();
        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().contains("graves.protect.")) {
                try {
                    gravePermissions.add(Integer.parseInt(perm.getPermission().replace("graves.protect.", "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (!gravePermissions.isEmpty()) {
            return Collections.max(gravePermissions) * 1000;
        }
        return getProtectTime();
    }


    public Integer getProtectTime() {
        Integer graveTime = plugin.getConfig().getInt("settings.graveProtectedTime") * 1000;
        if (graveTime > 0) {
            return graveTime;
        }
        return null;
    }

    public Grave createGrave(Player player, List<ItemStack> items) {
        if (graveIgnore.contains(player.getLocation().getBlock().getType())) {
            String graveIgnoreMessage = plugin.getConfig().getString("settings.graveIgnoreMessage")
                    .replace("$block", formatString(player.getLocation().getBlock().getType().toString()))
                    .replace("&", "§");
            if (!graveIgnoreMessage.equals("")) {
                player.sendMessage(graveIgnoreMessage);
            }
            return null;
        }
        Location location = getPlaceLocation(player.getLocation());
        if (location == null) {
            String graveFailure = plugin.getConfig().getString("settings.graveFailure")
                    .replace("&", "§");
            if (!graveFailure.equals("")) {
                player.sendMessage(graveFailure);
            }
            return null;
        }

        Inventory inventory = createInventory(items);
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", player.getName()).replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = player.getName() + "'s Grave";
        }
        Grave grave = new Grave(roundLocation(location), inventory, graveTitle);
        grave.setAliveTime(getGraveTime(player));
        Boolean graveProtected = plugin.getConfig().getBoolean("settings.graveProtected");
        if (graveProtected) {
            grave.setProtected(true);
            grave.setProtectTime(getProtectTime(player));
        }
        grave.setPlayer(player);
        grave.setKiller(player.getKiller());
        grave.setReplace(location.getBlock().getType());

        placeGrave(grave);
        graves.put(grave.getLocation(), grave);

        Integer chestTime = plugin.getConfig().getInt("settings.graveTime");
        String timeString = getTimeString((long) chestTime);
        String deathMessage = plugin.getConfig().getString("settings.deathMessage")
                .replace("$x", String.valueOf(location.getBlockX()))
                .replace("$y", String.valueOf(location.getBlockY()))
                .replace("$z", String.valueOf(location.getBlockZ()))
                .replace("$time", timeString)
                .replace("&", "§");
        if (!deathMessage.equals("")) {
            player.sendMessage(deathMessage);
        }
        return grave;
    }

    public Grave createGrave(LivingEntity entity, List<ItemStack> items) {
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
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", formatString(entity.getType().toString())).replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = formatString(entity.getType().toString()) + "'s Grave";
        }
        Grave grave = new Grave(roundLocation(location), inventory, graveTitle);
        grave.setAliveTime(getGraveTime());
        grave.setEntityType(entity.getType());
        grave.setReplace(location.getBlock().getType());
        placeGrave(grave);
        graves.put(location, grave);
        return grave;
    }

    public Inventory createInventory(List<ItemStack> items) {
        Inventory newInventory = plugin.getServer().createInventory(null, getInventorySize(items.size()));
        for (ItemStack item : items) {
            newInventory.addItem(item);
        }
        return newInventory;
    }

    public void placeGrave(Grave grave) {
        Material graveBlock = Material.matchMaterial(plugin.getConfig().getString("settings.graveBlock"));
        if (graveBlock == null) {
            graveBlock = Material.CHEST;
        }
        Boolean water = false;
        if (grave.getLocation().getBlock().getType().equals(Material.WATER) ||
                grave.getLocation().getBlock().getBlockData() instanceof Waterlogged) {
            water = true;
        }
        grave.getLocation().getBlock().setType(graveBlock);
        if (!water && grave.getLocation().getBlock().getBlockData() instanceof Waterlogged) {
            Waterlogged waterlogged = (Waterlogged) grave.getLocation().getBlock().getBlockData();
            waterlogged.setWaterlogged(false);
            grave.getLocation().getBlock().setBlockData(waterlogged);
        }
        String graveHeadName = plugin.getConfig().getString("settings.graveHeadSkin");
        if (graveBlock.equals(Material.PLAYER_HEAD)) {
            Skull skull = (Skull) grave.getLocation().getBlock().getState();
            Rotatable skullRotate = (Rotatable) grave.getLocation().getBlock().getBlockData();
            BlockFace skullBlockFace;
            if (grave.getPlayer() != null) {
                skullBlockFace = getSkullBlockFace(grave.getPlayer().getPlayer());
            } else {
                skullBlockFace = BlockFace.NORTH;
            }
            if (skullBlockFace != null) {
                skullRotate.setRotation(skullBlockFace);
                skull.setBlockData(skullRotate);
            }
            if (graveHeadName.equals("$entity") || graveHeadName.equals("")) {
                if (grave.getPlayer() != null) {
                    skull.setOwningPlayer(grave.getPlayer());
                } else if (grave.getEntityType() != null) {
                    // TODO Mob heads
                }
            } else {
                if (graveHead != null) {
                    skull.setOwningPlayer(graveHead);
                }
            }
            skull.update();
        }
        Boolean hologram = plugin.getConfig().getBoolean("settings.hologram");
        if (hologram) {
            createHologram(grave);
        }
    }

    public Location getPlaceLocation(Location location) {
        location = roundLocation(location);
        Boolean placeVoid = plugin.getConfig().getBoolean("settings.placeVoid");
        if (location.getY() < 0 || location.getY() > 256) {
            if (placeVoid) {
                Location top = getTop(location);
                if (top != null) {
                    return top;
                }
                return getVoid(location);
            } else {
                return null;
            }
        }
        Boolean placeGround = plugin.getConfig().getBoolean("settings.placeGround");
        if (placeGround) {
            Location ground = getGround(location);
            if (ground != null) {
                return ground;
            }
        }
        if (data.graveReplace().contains(location.getBlock().getType()) || data.graveReplace().contains("ALL")) {
            return location;
        }
        if (isAir(location.getBlock().getType())) {
            return location;
        }
        Location top = getTop(location);
        if (top != null) {
            return top;
        }
        return null;
    }

    public Location getGround(Location location) {
        Block block = location.getBlock();
        int max = 0;
        while (max <= 256) {
            if (!data.graveReplace().contains(block.getType()) && !isAir(block.getType())) {
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
            if (data.graveReplace().contains(block.getType()) || !isAir(block.getType())) {
                return block.getLocation().add(0, 1, 0);
            }
            block = block.getLocation().subtract(0, 1, 0).getBlock();
            max++;
        }
        return null;
    }

    public Location getVoid(Location location) {
        location.setY(0);
        Block block = location.getBlock();
        int max = 0;
        while (max <= 256) {
            if (data.graveReplace().contains(block.getType())) {
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

    public void replaceGrave(Grave grave) {
        Material replace = grave.getReplace();
        if (replace == null) {
            replace = Material.AIR;
        }
        grave.getLocation().getBlock().setType(replace);
        closeGrave(grave);
    }

    public void removeGrave(Grave grave) {
        graves.remove(grave.getLocation());
    }

    public void dropGrave(Grave grave) {
        if (grave != null) {
            for (ItemStack item : grave.getInventory()) {
                if (item != null) {
                    grave.getLocation().getWorld().dropItemNaturally(grave.getLocation(), item);
                }
            }
            grave.getInventory().clear();
        }
    }

    public void closeGrave(Grave grave) {
        List<HumanEntity> viewers = grave.getInventory().getViewers();
        for (HumanEntity viewer : new ArrayList<>(viewers)) {
            grave.getInventory().getViewers().remove(viewer);
            viewer.closeInventory();
        }
    }

    public void closeGraves() {
        if (!graves.isEmpty()) {
            Iterator iterator = graves.entrySet().iterator();
            while (iterator.hasNext()) {
                ConcurrentMap.Entry<Location, Grave> entry = (Map.Entry<Location, Grave>) iterator.next();
                Grave grave = entry.getValue();
                List<HumanEntity> viewers = grave.getInventory().getViewers();
                for (HumanEntity viewer : new ArrayList<>(viewers)) {
                    grave.getInventory().getViewers().remove(viewer);
                    viewer.closeInventory();
                }
            }
        }
    }

    public void removeHolograms() {
        if (!graves.isEmpty()) {
            Iterator iterator = graves.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Location, Grave> entry = (Map.Entry) iterator.next();
                removeHologram(entry.getValue());
            }
        }
    }

    public void createHolograms() {
        if (!graves.isEmpty()) {
            Iterator iterator = graves.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Location, Grave> entry = (Map.Entry) iterator.next();
                Grave grave = entry.getValue();
                if (grave.getHolograms().isEmpty()) {
                    createHologram(grave);
                }
            }
        }
    }

    public Boolean hasPermission(Grave grave, Player player) {
        Boolean owner = false;
        Boolean killer = false;
        Boolean bypass = false;
        Boolean unprotect = false;
        Boolean entity = false;
        if (player.hasPermission("graves.bypass")) {
            bypass = true;
        }
        if (grave.getEntityType() != null) {
            entity = true;
        }
        if (grave.getProtected() != null && !grave.getProtected()) {
            unprotect = true;
        }
        Boolean graveProtected = plugin.getConfig().getBoolean("settings.graveProtected");
        if (graveProtected) {
            if (grave.getPlayer() != null) {
                if (player.getUniqueId().equals(grave.getPlayer().getUniqueId())) {
                    owner = true;
                }
            }
        } else {
            bypass = true;
        }
        Boolean killerOpen = plugin.getConfig().getBoolean("settings.killerOpen");
        if (killerOpen) {
            if (grave.getKiller() != null) {
                if (player.getUniqueId().equals(grave.getKiller().getUniqueId())) {
                    killer = true;
                }
            }
        }
        if (owner || killer || entity || unprotect || bypass) {
            return true;
        } else {
            return false;
        }
    }

    public void giveExperience(Grave grave, Player player) {
        String expMessage = plugin.getConfig().getString("settings.expMessage").replace("&", "§");
        if (grave.getExperience() != null && grave.getExperience() > 0) {
            expMessage = expMessage.replace("$level", getLevelFromExp(grave.getExperience()));
            expMessage = expMessage.replace("$xp", grave.getExperience().toString());
            player.giveExp(grave.getExperience());
            grave.setExperience(null);
            player.sendMessage(expMessage);
        }
    }

    public void dropExperience(Grave grave) {
        if (grave.getExperience() != null && grave.getExperience() > 0) {
            ExperienceOrb orb = (ExperienceOrb) grave.getLocation().getWorld().spawnEntity(grave.getLocation(), EntityType.EXPERIENCE_ORB);
            orb.setExperience(grave.getExperience());
            grave.setExperience(null);
        }
    }

    public void destroyGrave(Grave grave) {
        grave.getInventory().clear();
        grave.setExperience(null);
    }

    public void updateHologram(Grave grave) {
        boolean hologram = plugin.getConfig().getBoolean("settings.hologram");
        if (hologram) {
            if (!grave.getHolograms().isEmpty()) {
                for (Iterator<ConcurrentMap.Entry<UUID, Integer>> iterator = grave.getHolograms().entrySet()
                        .iterator(); iterator.hasNext(); ) {
                    if (iterator.hasNext()) {
                        ConcurrentMap.Entry<UUID, Integer> entry = iterator.next();
                        ArmorStand armorStand = (ArmorStand) plugin.getServer().getEntity(entry.getKey());
                        if (armorStand != null) {
                            armorStand.setCustomName(parseHologram(entry.getValue(), grave));
                        }
                    }
                }
            }
        }
    }

    public void autoLoot(Grave grave, Player player) {
        equipArmor(grave.getInventory(), player);
        equipItems(grave.getInventory(), player);
        if (checkEmpty(grave)) {
            messages.graveLoot(grave.getLocation(), player);
            giveExperience(grave, player);
            removeHologram(grave);
            replaceGrave(grave);
            removeGrave(grave);
        }
    }

    public void equipArmor(Inventory inventory, Player player) {
        List<ItemStack> items = Arrays.asList(inventory.getContents());
        Collections.reverse(items);
        for (ItemStack item : items) {
            if (item != null) {
                if (player.getInventory().getHelmet() == null) {
                    if (isHelmet(item.getType())) {
                        player.getInventory().setHelmet(item);
                        inventory.removeItem(item);
                    }
                }
                if (player.getInventory().getChestplate() == null) {
                    if (isChestplate(item.getType())) {
                        player.getInventory().setChestplate(item);
                        inventory.removeItem(item);
                    }
                }
                if (player.getInventory().getLeggings() == null) {
                    if (isLeggings(item.getType())) {
                        player.getInventory().setLeggings(item);
                        inventory.removeItem(item);
                    }
                }
                if (player.getInventory().getBoots() == null) {
                    if (isBoots(item.getType())) {
                        player.getInventory().setBoots(item);
                        inventory.removeItem(item);
                    }
                }
            }
        }
    }

    public Boolean isHelmet(Material material) {
        if (material != null) {
            if (material.equals(Material.DIAMOND_HELMET) || material.equals(Material.GOLDEN_HELMET)
                    || material.equals(Material.IRON_HELMET) || material.equals(Material.LEATHER_HELMET)
                    || material.equals(Material.CHAINMAIL_HELMET) || material.equals(Material.TURTLE_HELMET)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isChestplate(Material material) {
        if (material != null) {
            if (material.equals(Material.DIAMOND_CHESTPLATE) || material.equals(Material.GOLDEN_CHESTPLATE)
                    || material.equals(Material.IRON_CHESTPLATE) || material.equals(Material.LEATHER_CHESTPLATE)
                    || material.equals(Material.CHAINMAIL_CHESTPLATE)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isLeggings(Material material) {
        if (material != null) {
            if (material.equals(Material.DIAMOND_LEGGINGS) || material.equals(Material.GOLDEN_LEGGINGS)
                    || material.equals(Material.IRON_LEGGINGS) || material.equals(Material.LEATHER_LEGGINGS)
                    || material.equals(Material.CHAINMAIL_LEGGINGS)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isBoots(Material material) {
        if (material != null) {
            if (material.equals(Material.DIAMOND_BOOTS) || material.equals(Material.GOLDEN_BOOTS)
                    || material.equals(Material.IRON_BOOTS) || material.equals(Material.LEATHER_BOOTS)
                    || material.equals(Material.CHAINMAIL_BOOTS)) {
                return true;
            }
        }
        return false;
    }

    public Boolean checkEmpty(Grave grave) {
        if (grave.getItemAmount() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public void equipItems(Inventory inventory, Player player) {
        Integer freeSlots = freeSlots(player);
        Integer counter = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                if (counter < freeSlots) {
                    player.getInventory().addItem(item);
                    inventory.removeItem(item);
                    counter++;
                }
            }
        }
    }

    public Integer freeSlots(Player player) {
        Integer freeSlots = 0;
        Integer counter = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (counter <= 36) {
                if (item == null) {
                    freeSlots++;
                    counter++;
                }
            }
        }
        return freeSlots;
    }

    public String parseHologram(Integer lineNumber, Grave grave) {
        String line = hologramLines.get(lineNumber).replace("$itemCount", grave.getItemAmount().toString()).replace("&", "§");
        if (grave.getAliveTime() != null) {
            Long aliveTime = (grave.getAliveTime() - (System.currentTimeMillis() - grave.getCreatedTime())) / 1000;
            String timeString = getTimeString(aliveTime);
            line = line.replace("$time", timeString);
        } else {
            String timeInfinite = plugin.getConfig().getString("settings.timeInfinite").replace("&", "§");
            line = line.replace("$time", timeInfinite);
        }
        if (grave.getProtected() != null) {
            String protect;
            if (grave.getProtected()) {
                protect = plugin.getConfig().getString("settings.graveProtectedProtectedMessage");
            } else {
                protect = plugin.getConfig().getString("settings.graveProtectedUnprotectedMessage");
            }
            if (grave.getProtectTime() != null) {
                Long protectTime = (grave.getProtectTime() - (System.currentTimeMillis() - grave.getCreatedTime())) / 1000;
                protect.replace("$time", getTimeString(protectTime));
            } else {
                String timeInfinite = plugin.getConfig().getString("settings.timeInfinite");
                protect.replace("$time", timeInfinite);
            }
            protect = protect.replace("&", "§");
            line = line.replace("$protect", protect);
        }
        if (grave.getPlayer() != null) {
            line = line.replace("$entity", grave.getPlayer().getName());
        } else if (grave.getEntityType() != null) {
            line = line.replace("$entity", formatString(grave.getEntityType().toString()));
        }
        if (grave.getExperience() != null) {
            line = line.replace("$level", getLevelFromExp(grave.getExperience()));
            line = line.replace("$xp", grave.getExperience().toString());
        } else {
            line = line.replace("$level", "0");
            line = line.replace("$xp", "0");
        }
        return line;
    }

    public Integer cleanupHolograms() {
        Integer count = 0;
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

    public void removeBrokenHolograms() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ArmorStand) {
                    ArmorStand armorStand = (ArmorStand) entity;
                    Grave grave = getGraveFromHologram(armorStand);
                    if (grave == null) {
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

    public void createHologram(Grave grave) {
        Material graveBlock = Material.matchMaterial(plugin.getConfig().getString("settings.graveBlock"));
        if (graveBlock == null) {
            graveBlock = Material.CHEST;
        }
        Location location = grave.getLocation().clone().add(0.5, 0, 0.5);
        if (graveBlock.equals(Material.PLAYER_HEAD)) {
            location.subtract(0, 1.40, 0);
        } else if (graveBlock.equals(Material.AIR)) {
            location.subtract(0, 2, 0);
            Double graveAirHeight = plugin.getConfig().getDouble("settings.graveAirHeight");
            location.add(0, graveAirHeight, 0);
        } else {
            location.subtract(0, 1, 0);
        }

        Integer lineNumber = 0;
        for (String ignored : plugin.getConfig().getStringList("settings.hologramLines")) {
            ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
            armorStand.setInvulnerable(true);
            //armorStand.setSmall(true);
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setCustomName(parseHologram(lineNumber, grave));
            Boolean hologram = plugin.getConfig().getBoolean("settings.hologram");
            if (hologram) {
                armorStand.setCustomNameVisible(true);
            } else {
                armorStand.setCustomNameVisible(false);
            }            armorStand.addScoreboardTag("graveHologram");
            armorStand.addScoreboardTag("graveHologramLocation:" + grave.getLocation().getWorld().getName() + "#"
                    + grave.getLocation().getX() + "#" + grave.getLocation().getY() + "#" + grave.getLocation().getZ());
            location.add(0, 0.28, 0);
            grave.addHologram(armorStand.getUniqueId(), lineNumber);
            lineNumber++;
        }
    }

    public void removeHologram(Grave grave) {
        if (!grave.getHolograms().isEmpty()) {
            for (Iterator<Map.Entry<UUID, Integer>> iterator = grave.getHolograms().entrySet()
                    .iterator(); iterator.hasNext(); ) {
                if (iterator.hasNext()) {
                    ConcurrentMap.Entry<UUID, Integer> entry = iterator.next();
                    ArmorStand armorStand = (ArmorStand) plugin.getServer().getEntity(entry.getKey());
                    if (armorStand != null) {
                        armorStand.remove();
                    }
                    iterator.remove();
                }
            }
        }
    }

    public Grave getGraveFromHologram(ArmorStand armorStand) {
        for (String tag : armorStand.getScoreboardTags()) {
            if (tag.contains("graveHologramLocation:")) {
                String[] cords = tag.replace("graveHologramLocation:", "").split("#");
                try {
                    World world = plugin.getServer().getWorld(cords[0]);
                    Double x = Double.parseDouble(cords[1]);
                    Double y = Double.parseDouble(cords[2]);
                    Double z = Double.parseDouble(cords[3]);
                    Location location = new Location(world, x, y, z);
                    return getGrave(location);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                }
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public void graveHeadLoad() {
        String graveHeadName = plugin.getConfig().getString("settings.graveHeadSkin");
        if (!graveHeadName.equals("")) {
            graveHead = plugin.getServer().getOfflinePlayer(graveHeadName);
        }
    }

    public Boolean shouldIgnore(ItemStack itemStack) {
        if (graveItemIgnore.contains(itemStack.getType())) {
            return true;
        }

        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta.hasDisplayName()) {
                List<String> graveItemIgnoreName = plugin.getConfig().getStringList("settings.graveItemIgnoreName");
                for (String string : graveItemIgnoreName) {
                    string = string.replace("&", "§");
                    if (itemMeta.getDisplayName().equals(string)) {
                        return true;
                    }
                }

                List<String> graveItemIgnoreNameContains = plugin.getConfig().getStringList("settings.graveItemIgnoreNameContains");
                for (String string : graveItemIgnoreNameContains) {
                    string = string.replace("&", "§");
                    if (itemMeta.getDisplayName().contains(string)) {
                        return true;
                    }
                }
            }

            if (itemMeta.hasLore()) {
                List<String> graveItemIgnoreLore = plugin.getConfig().getStringList("settings.graveItemIgnoreLore");
                for (String string : graveItemIgnoreLore) {
                    for (String lore : itemMeta.getLore()) {
                        string = string.replace("&", "§");
                        if (lore.equals(string)) {
                            return true;
                        }
                    }
                }

                List<String> graveItemIgnoreLoreContains = plugin.getConfig().getStringList("settings.graveItemIgnoreLoreContains");
                for (String string : graveItemIgnoreLoreContains) {
                    for (String lore : itemMeta.getLore()) {
                        string = string.replace("&", "§");
                        if (lore.contains(string)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void graveItemIgnoreLoad() {
        graveItemIgnore.clear();
        for (String line : plugin.getConfig().getStringList("settings.graveItemIgnore")) {
            Material material = Material.matchMaterial(line.toUpperCase());
            if (material != null) {
                graveItemIgnore.add(material);
            }
        }
    }

    public void graveIgnoreLoad() {
        graveIgnore.clear();
        for (String line : plugin.getConfig().getStringList("settings.graveIgnore")) {
            Material material = Material.matchMaterial(line.toUpperCase());
            if (material != null) {
                graveIgnore.add(material);
            }
        }
    }

    public void graveSpawnZombie(Grave grave, Player player) {
        if (grave.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            Boolean graveZombieOwner = plugin.getConfig().getBoolean("settings.graveZombieOwner");
            if (!graveZombieOwner) {
                return;
            }
        } else {
            Boolean graveZombieOther = plugin.getConfig().getBoolean("settings.graveZombieOther");
            if (!graveZombieOther) {
                return;
            }
        }
        spawnZombie(grave, player);
    }

    public void spawnZombie(Grave grave, LivingEntity target) {
        LivingEntity livingEntity = spawnZombie(grave);
        if (livingEntity != null && livingEntity instanceof Monster) {
            Monster monster = (Monster) livingEntity;
            monster.setTarget(target);
        }
    }

    public LivingEntity spawnZombie(Grave grave) {
        if (grave.getPlayer() != null) {
            EntityType graveZombieType = EntityType.ZOMBIE;
            try {
                graveZombieType = EntityType.valueOf(plugin.getConfig().getString("settings.graveZombieType").toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
            if (graveZombieType == null) {
                graveZombieType = EntityType.ZOMBIE;
            }
            LivingEntity livingEntity = (LivingEntity) grave.getLocation().getWorld().spawnEntity(grave.getLocation(), graveZombieType);
            Boolean graveZombieOwnerHead = plugin.getConfig().getBoolean("settings.graveZombieOwnerHead");
            if (graveZombieOwnerHead) {
                livingEntity.getEquipment().setHelmet(getPlayerSkull(grave.getPlayer()));
            }
            Boolean graveZombiePickup = plugin.getConfig().getBoolean("settings.graveZombiePickup");
            if (!graveZombiePickup) {
                livingEntity.setCanPickupItems(false);
            }
            String graveZombieName = plugin.getConfig().getString("settings.graveZombieName")
                    .replace("$owner", grave.getPlayer().getName())
                    .replace("&", "§");
            if (!graveZombieName.equals("")) {
                livingEntity.setCustomName(graveZombieName);
            }
            livingEntity.getScoreboardTags().add("graveZombie");
            return livingEntity;
        }
        return null;
    }

    public Boolean isGraveZombie(LivingEntity livingEntity) {
        for (String tag : livingEntity.getScoreboardTags()) {
            if (tag.contains("graveZombie")) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getPlayerSkull(OfflinePlayer player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        item.setItemMeta(meta);
        return item;
    }

    public void hologramLinesLoad() {
        hologramLines.clear();
        for (String line : plugin.getConfig().getStringList("settings.hologramLines")) {
            hologramLines.add(line);
        }
        Collections.reverse(hologramLines);
    }

    public void runCreateCommands(Grave grave, LivingEntity livingEntity) {
        List<String> graveCreateCommands = plugin.getConfig().getStringList("settings.graveCreateCommands");
        Boolean graveCommandsOnlyPlayers = plugin.getConfig().getBoolean("settings.graveCommandsOnlyPlayers");
        for (String command : graveCreateCommands) {
            if (graveCommandsOnlyPlayers && grave.getEntityType() != null) {
                return;
            }
            runConsoleCommand(formatCommand(command, livingEntity, grave));
        }
    }

    public void runLootCommands(Grave grave, Player player) {
        List<String> graveLootCommands = plugin.getConfig().getStringList("settings.graveLootCommands");
        Boolean graveCommandsOnlyPlayers = plugin.getConfig().getBoolean("settings.graveCommandsOnlyPlayers");
        for (String command : graveLootCommands) {
            if (graveCommandsOnlyPlayers && grave.getEntityType() != null) {
                return;
            }
            runConsoleCommand(formatCommand(command, player, grave));
        }
    }

    public void runOpenCommands(Grave grave, Player player) {
        List<String> graveOpenCommands = plugin.getConfig().getStringList("settings.graveOpenCommands");
        Boolean graveCommandsOnlyPlayers = plugin.getConfig().getBoolean("settings.graveCommandsOnlyPlayers");
        for (String command : graveOpenCommands) {
            if (graveCommandsOnlyPlayers && grave.getEntityType() != null) {
                return;
            }
            runConsoleCommand(formatCommand(command, player, grave));
        }
    }

    public void runBreakCommands(Grave grave, Player player) {
        List<String> graveBreakCommands = plugin.getConfig().getStringList("settings.graveBreakCommands");
        Boolean graveCommandsOnlyPlayers = plugin.getConfig().getBoolean("settings.graveCommandsOnlyPlayers");
        for (String command : graveBreakCommands) {
            if (graveCommandsOnlyPlayers && grave.getEntityType() != null) {
                return;
            }
            runConsoleCommand(formatCommand(command, player, grave));
        }
    }

    public void runExplodeCommands(Grave grave, Entity entity) {
        List<String> graveExplodeCommands = plugin.getConfig().getStringList("settings.graveExplodeCommands");
        Boolean graveCommandsOnlyPlayers = plugin.getConfig().getBoolean("settings.graveCommandsOnlyPlayers");
        for (String command : graveExplodeCommands) {
            if (graveCommandsOnlyPlayers && grave.getEntityType() != null) {
                return;
            }
            runConsoleCommand(formatCommand(command, entity, grave));
        }
    }

    public void runConsoleCommand(String command) {
        ConsoleCommandSender console = plugin.getServer().getConsoleSender();
        ServerCommandEvent commandEvent = new ServerCommandEvent(console, command);
        plugin.getServer().getPluginManager().callEvent(commandEvent);
        if (!commandEvent.isCancelled()) {
            plugin.getServer().getScheduler().callSyncMethod(plugin, () -> plugin.getServer().dispatchCommand(commandEvent.getSender(), commandEvent.getCommand()));
        }
    }

    public String formatCommand(String command, Entity entity, Grave grave) {
        command = command.replace("$entity", getEntityName(entity))
                .replace("$player", getEntityName(entity))
                .replace("$owner", getOwnerName(grave))
                .replace("$x", String.valueOf(grave.getLocation().getBlockX()))
                .replace("$y", String.valueOf(grave.getLocation().getBlockY()))
                .replace("$z", String.valueOf(grave.getLocation().getBlockZ()))
                .replace("&", "§");
        if (grave.getAliveTime() != null) {
            Long aliveTime = (grave.getAliveTime() - (System.currentTimeMillis() - grave.getCreatedTime())) / 1000;
            String timeString = getTimeString(aliveTime);
            command = command.replace("$time", timeString);
        } else {
            String timeInfinite = plugin.getConfig().getString("settings.timeInfinite").replace("&", "§");
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

    public static String getOwnerName(Grave grave) {
        String owner = "";
        if (grave.getPlayer() != null) {
            owner = grave.getPlayer().getName();
        } else if (grave.getEntityType() != null) {
            owner = formatString(grave.getEntityType().toString());
        }
        return owner;
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

    public static Integer getItemAmount(Inventory inventory) {
        Integer count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                count++;
            }
        }
        return count;
    }

    public static Integer getInventorySize(Integer size) {
        if (size <= 9) {
            return 9;
        }
        if (size <= 18) {
            return 18;
        }
        if (size <= 27) {
            return 27;
        }
        if (size <= 36) {
            return 36;
        }
        if (size <= 45) {
            return 45;
        }
        return 54;
    }

    private static BlockFace getSkullBlockFace(Player player) {
        float direction = player.getLocation().getYaw() % 360;
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

    public Boolean canBuild(Player player, Location location) {
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(location.getBlock(), location.getBlock().getState(), location.getBlock(), null, player, true, EquipmentSlot.HAND);
        plugin.getServer().getPluginManager().callEvent(placeEvent);
        if (placeEvent.canBuild()) {
            return true;
        }
        return false;
    }

    public Integer getPlayerDropExp(Player player) {
        Integer experience = getPlayerExp(player);
        if (experience != null) {
            Float expStorePercent = (float) plugin.getConfig().getDouble("settings.expStorePercent");
            return (int) (experience * expStorePercent);
        }
        return null;
    }

    public Integer getPlayerExp(Player player) {
        int experience = Math.round(getExpAtLevel(player.getLevel()) * player.getExp());
        int level = player.getLevel();

        while (level > 0) {
            level--;
            experience += getExpAtLevel(level);
        }
        if (experience < 0) {
            return null;
        }
        return experience;
    }

    public Integer getExpAtLevel(Integer level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level >= 16 && level <= 30) {
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
        Boolean expLevelRound = plugin.getConfig().getBoolean("settings.expLevelRound");
        if (expLevelRound) {
            return String.valueOf((int) result);
        }
        return String.valueOf(result);
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
            timeDay = plugin.getConfig().getString("settings.timeDay").replace("$d", String.valueOf(day))
                    .replace("&", "§");
        }
        if (hour > 0) {
            timeHour = plugin.getConfig().getString("settings.timeHour").replace("$h", String.valueOf(hour))
                    .replace("&", "§");
        }
        if (minute > 0) {
            timeMinute = plugin.getConfig().getString("settings.timeMinute").replace("$m", String.valueOf(minute))
                    .replace("&", "§");
        }
        if (second > 0) {
            timeSecond = plugin.getConfig().getString("settings.timeSecond").replace("$s", String.valueOf(second))
                    .replace("&", "§");
        }
        return StringUtils.normalizeSpace(timeDay + timeHour + timeMinute + timeSecond);
    }
}