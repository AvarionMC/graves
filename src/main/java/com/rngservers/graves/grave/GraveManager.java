package com.rngservers.graves.grave;

import com.rngservers.graves.Main;
import com.rngservers.graves.data.DataManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachmentInfo;
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
                    if (plugin.getServer().getWorlds().contains(grave.getLocation().getWorld())) { // Check if world is valid
                        updateHologram(grave);
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
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public ConcurrentMap<Location, Grave> getGraves(OfflinePlayer player) {
        ConcurrentMap<Location, Grave> playerGraves = new ConcurrentHashMap<>();
        for (ConcurrentMap.Entry<Location, Grave> entry : graves.entrySet()) {
            if (entry.getValue().getPlayer().equals(player)) {
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
        return plugin.getConfig().getInt("settings.graveTime") * 1000;
    }

    public Grave createGrave(Player player) {
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

        Inventory inventory = formatInventory(player.getInventory());
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", player.getName()).replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = player.getName() + "'s Grave";
        }
        Grave grave = new Grave(roundLocation(location), inventory, graveTitle);
        grave.setAliveTime(getGraveTime(player));
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

    public Inventory formatInventory(PlayerInventory inventory) {
        Integer itemAmount = getItemAmount(inventory);
        Inventory newInventory = plugin.getServer().createInventory(null, getInventorySize(itemAmount));
        List<ItemStack> armor = Arrays.asList(inventory.getArmorContents());
        Collections.reverse(armor);
        for (ItemStack item : armor) {
            if (item != null) {
                newInventory.addItem(item);
            }
        }
        inventory.setArmorContents(new ItemStack[]{});
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                newInventory.addItem(item);
            }
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
        if (location.getY() < 0 || location.getY() > 256) {
            return getTop(location);
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
        if (location.getBlock().getType().isAir()) {
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
            if (data.graveReplace().contains(block.getType())) {
                return block.getLocation();
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
            if (data.graveReplace().contains(block.getType()) || !block.getType().isAir()) {
                return block.getLocation().add(0, 1, 0);
            }
            block = block.getLocation().subtract(0, 1, 0).getBlock();
            max++;
        }
        return null;
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

    public Boolean hasPermission(Grave grave, Player player) {
        Boolean owner = false;
        Boolean killer = false;
        Boolean bypass = false;
        if (player.hasPermission("graves.bypass")) {
            bypass = true;
        }
        Boolean graveProtected = plugin.getConfig().getBoolean("settings.graveProtected");
        if (graveProtected) {
            if (player.equals(grave.getPlayer())) {
                owner = true;
            }
        } else {
            bypass = true;
        }
        Boolean killerOpen = plugin.getConfig().getBoolean("settings.killerOpen");
        if (killerOpen) {
            if (player.equals(grave.getKiller())) {
                killer = true;
            }
        }
        if (owner || killer || bypass) {
            return true;
        } else {
            return false;
        }
    }

    public void giveExperience(Grave grave, Player player) {
        String expMessage = plugin.getConfig().getString("settings.expMessage").replace("&", "§");
        if (grave.getLevel() != null && grave.getLevel() > 0) {
            expMessage = expMessage.replace("$level", grave.getLevel().toString());
            player.setLevel(player.getLevel() + grave.getLevel());
            grave.setLevel(null);
            player.sendMessage(expMessage);
        }
    }

    public void dropExperience(Grave grave) {
        /*
        if (grave.getExperience() != null && grave.getExperience() > 0) {
            // TODO
            //ExperienceOrb orb = (ExperienceOrb) grave.getLocation().getWorld().spawnEntity(grave.getLocation(), EntityType.EXPERIENCE_ORB);
            //orb.setExperience(grave.getExperience());
            grave.setExperience(null);
        }
         */
    }

    public void destroyGrave(Grave grave) {
        grave.getInventory().clear();
        grave.setLevel(null);
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
        ItemStack helmet = inventory.getItem(0);
        ItemStack chestplate = inventory.getItem(1);
        ItemStack leggings = inventory.getItem(2);
        ItemStack boots = inventory.getItem(3);

        if (helmet != null) {
            if (helmet.getType().equals(Material.DIAMOND_HELMET) || helmet.getType().equals(Material.GOLDEN_HELMET)
                    || helmet.getType().equals(Material.IRON_HELMET) || helmet.getType().equals(Material.LEATHER_HELMET)
                    || helmet.getType().equals(Material.CHAINMAIL_HELMET) || helmet.getType().equals(Material.TURTLE_HELMET)) {
                if (player.getInventory().getHelmet() == null) {
                    player.getInventory().setHelmet(helmet);
                    inventory.removeItem(helmet);
                }
            }
        }
        if (chestplate != null) {
            if (chestplate.getType().equals(Material.DIAMOND_CHESTPLATE) || chestplate.getType().equals(Material.GOLDEN_CHESTPLATE)
                    || chestplate.getType().equals(Material.IRON_CHESTPLATE) || chestplate.getType().equals(Material.LEATHER_CHESTPLATE)
                    || chestplate.getType().equals(Material.CHAINMAIL_CHESTPLATE)) {
                if (player.getInventory().getChestplate() == null) {
                    player.getInventory().setChestplate(chestplate);
                    inventory.removeItem(chestplate);
                }
            }
        }
        if (leggings != null) {
            if (leggings.getType().equals(Material.DIAMOND_LEGGINGS) || leggings.getType().equals(Material.GOLDEN_LEGGINGS)
                    || leggings.getType().equals(Material.IRON_LEGGINGS) || leggings.getType().equals(Material.LEATHER_LEGGINGS)
                    || leggings.getType().equals(Material.CHAINMAIL_LEGGINGS)) {
                if (player.getInventory().getLeggings() == null) {
                    player.getInventory().setLeggings(leggings);
                    inventory.removeItem(leggings);
                }
            }
        }
        if (boots != null) {
            if (boots.getType().equals(Material.DIAMOND_BOOTS) || boots.getType().equals(Material.GOLDEN_BOOTS)
                    || boots.getType().equals(Material.IRON_BOOTS) || boots.getType().equals(Material.LEATHER_BOOTS)
                    || boots.getType().equals(Material.CHAINMAIL_BOOTS)) {
                if (player.getInventory().getBoots() == null) {
                    player.getInventory().setBoots(boots);
                    inventory.removeItem(boots);
                }
            }
        }
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
        Long time = (grave.getAliveTime() - (System.currentTimeMillis() - grave.getCreatedTime())) / 1000;
        String timeString = getTimeString(time);
        String line = hologramLines.get(lineNumber)
                .replace("$time", timeString)
                .replace("$player", "$entity")
                .replace("$itemCount", getItemAmount(grave.getInventory()).toString())
                .replace("&", "§");
        if (grave.getPlayer() != null) {
            line = line.replace("$entity", grave.getPlayer().getName());
        } else if (grave.getEntityType() != null) {
            line = line.replace("$entity", formatString(grave.getEntityType().toString()));
        }
        if (grave.getLevel() != null) {
            line = line.replace("$level", grave.getLevel().toString());
        } else {
            line = line.replace("$level", "0");
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
            armorStand.addScoreboardTag("graveHologramLocation:" + grave.getLocation().getWorld().getName() + "_"
                    + grave.getLocation().getX() + "_" + grave.getLocation().getY() + "_" + grave.getLocation().getZ());
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
                String[] cords = tag.replace("graveHologramLocation:", "").split("_");
                try {
                    World world = plugin.getServer().getWorld(cords[0]);
                    Double x = Double.parseDouble(cords[1]);
                    Double y = Double.parseDouble(cords[2]);
                    Double z = Double.parseDouble(cords[3]);
                    Location location = new Location(world, x, y, z);
                    return getGrave(location);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    public List<Material> getGraveIgnore() {
        return graveIgnore;
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