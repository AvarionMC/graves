package com.rngservers.graves.grave;

import com.rngservers.graves.Main;
import com.rngservers.graves.data.DataManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    private List<Material> graveIgnore = new ArrayList<>();
    private OfflinePlayer graveHead;

    public GraveManager(Main plugin, DataManager data, Messages messages) {
        this.plugin = plugin;
        this.data = data;
        this.messages = messages;
        getSavedGraves();
        graveHeadLoad();
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

    public OfflinePlayer getGraveHead() {
        return graveHead;
    }

    public void removeGraveTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ConcurrentMap.Entry<Location, Grave> entry : graves.entrySet()) {
                    Grave grave = entry.getValue();
                    if (plugin.getServer().getWorlds().contains(grave.getLocation().getWorld())) {
                        updateHologram(grave);
                        removeBrokenHolograms();
                        if (grave.getProtectTime() != null && grave.getProtected()) {
                            Long diff = System.currentTimeMillis() - grave.getCreatedTime();
                            if (diff >= grave.getProtectTime()) {
                                protectGrave(grave, false);
                            }
                        }
                        if (grave.getAliveTime() != null) {
                            Long diff = System.currentTimeMillis() - grave.getCreatedTime();
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
        grave.getLocation().getBlock().setType(graveBlock);
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
        if (!this.graves.isEmpty()) {
            Iterator iterator = this.graves.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Location, Grave> entry = (Map.Entry) iterator.next();
                this.removeHologram(entry.getValue());
            }
        }
    }

    public void createHolograms() {
        if (!this.graves.isEmpty()) {
            Iterator iterator = this.graves.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Location, Grave> entry = (Map.Entry) iterator.next();
                Grave grave = entry.getValue();
                if (grave.getHolograms().isEmpty()) {
                    this.createHologram(grave);
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
            expMessage = expMessage.replace("$level", "$xp");
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
        Boolean hologram = plugin.getConfig().getBoolean("settings.hologram");
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
        line = line.replace("$level", "$xp");
        if (grave.getExperience() != null) {
            line = line.replace("$xp", grave.getExperience().toString());
        } else {
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
                        armorStand.remove();
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
            location.subtract(0, 0.40, 0);
        }
        if (graveBlock.equals(Material.AIR)) {
            location.subtract(0, 1, 0);
            Double graveAirHeight = plugin.getConfig().getDouble("settings.graveAirHeight");
            location.add(0, graveAirHeight, 0);
        }

        Integer lineNumber = 0;
        for (String ignored : plugin.getConfig().getStringList("settings.hologramLines")) {
            ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
            armorStand.setInvulnerable(true);
            armorStand.setSmall(true);
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setCustomName(parseHologram(lineNumber, grave));
            armorStand.setCustomNameVisible(true);
            armorStand.addScoreboardTag("graveHologram");
            armorStand.addScoreboardTag("graveHologramLocation:" + grave.getLocation().getWorld().getName() + "#"
                    + grave.getLocation().getX() + "#" + grave.getLocation().getY() + "#" + grave.getLocation().getZ());
            location.add(0, 0.25, 0);
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

    public void graveIgnoreLoad() {
        graveIgnore.clear();
        for (String line : plugin.getConfig().getStringList("settings.graveIgnore")) {
            Material material = Material.matchMaterial(line.toUpperCase());
            if (material != null) {
                graveIgnore.add(material);
            }
        }
    }

    public void hologramLinesLoad() {
        hologramLines.clear();
        for (String line : plugin.getConfig().getStringList("settings.hologramLines")) {
            hologramLines.add(line);
        }
        Collections.reverse(hologramLines);
    }

    public static String formatString(String string) {
        String format = WordUtils.capitalizeFully(string.toString()).replace("_", " ");
        return format;
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