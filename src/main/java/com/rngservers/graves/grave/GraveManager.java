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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class GraveManager {
    private Main plugin;
    DataManager data;
    private Map<Location, Grave> graves;
    private OfflinePlayer graveHead;
    private List<String> hologramLines = new ArrayList<>();

    public GraveManager(Main plugin, DataManager data) {
        this.plugin = plugin;
        this.data = data;
        graves = data.getSavedGraves();
        graveHeadLoad();
        hologramLinesLoad();
        removeGraveTimer();
        hologramUpdater();
    }

    public void removeGraveTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!graves.isEmpty()) {
                    for (Iterator<Map.Entry<Location, Grave>> iterator = graves.entrySet()
                            .iterator(); iterator.hasNext(); ) {
                        if (iterator.hasNext()) {
                            Map.Entry<Location, Grave> entry = iterator.next();
                            Grave grave = entry.getValue();
                            Integer graveTime = plugin.getConfig().getInt("settings.graveTime") * 1000;
                            Long diff = System.currentTimeMillis() - grave.getTime();
                            if (diff >= graveTime) {
                                dropGrave(grave);
                                dropExperience(grave);
                                removeGrave(grave);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }


    public void hologramUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!graves.isEmpty()) {
                    for (Iterator<Map.Entry<Location, Grave>> iterator = graves.entrySet()
                            .iterator(); iterator.hasNext(); ) {
                        if (iterator.hasNext()) {
                            Map.Entry<Location, Grave> entry = iterator.next();
                            if (entry.getValue() != null) {
                                updateHologram(entry.getValue());
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public Grave getGrave(Location location) {
        return graves.get(roundLocation(location));
    }

    public void saveGraves() {
        for (Map.Entry<Location, Grave> entry : graves.entrySet()) {
            data.saveGrave(entry.getValue());
        }
    }

    public void createGrave(Player player) {
        Location location = getPlaceLocation(player.getLocation());
        if (location == null) {
            String graveFailure = plugin.getConfig().getString("settings.graveFailure")
                    .replace("&", "§");
            if (!graveFailure.equals("")) {
                player.sendMessage(graveFailure);
            }
            return;
        }

        Inventory inventory = player.getInventory();
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", player.getName()).replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = player.getName() + "'s Grave";
        }
        Grave grave = new Grave(roundLocation(location), inventory, graveTitle);
        grave.setPlayer(player);
        grave.setKiller(player.getKiller());
        grave.setReplace(location.getBlock().getType());

        Boolean expStore = plugin.getConfig().getBoolean("settings.expStore");
        if (expStore) {
            grave.setExperience(player.getTotalExperience());
        }

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
    }

    public void createGrave(LivingEntity entity, List<ItemStack> items) {
        Location location = getPlaceLocation(entity.getLocation());
        if (location == null) {
            return;
        }

        Inventory inventory = plugin.getServer().createInventory(null, 54);
        for (ItemStack item : items) {
            if (item != null) {
                inventory.addItem(item);
            }
        }
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", getEntityName(entity.getType())).replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = getEntityName(entity.getType()) + "'s Grave";
        }
        Grave grave = new Grave(roundLocation(location), inventory, graveTitle);
        grave.setEntityType(entity.getType());
        grave.setReplace(location.getBlock().getType());

        placeGrave(grave);

        graves.put(location, grave);
    }

    public void placeGrave(Grave grave) {
        Material graveBlock = Material.matchMaterial(plugin.getConfig().getString("settings.graveBlock"));
        if (graveBlock == null) {
            graveBlock = Material.CHEST;
        }
        grave.getLocation().getBlock().setType(graveBlock);
        String graveHeadName = plugin.getConfig().getString("settings.graveHeadSkin");
        if (graveBlock.equals(Material.PLAYER_HEAD)) {
            Rotatable skullRotate = (Rotatable) grave.getLocation().getBlock().getBlockData();
            BlockFace skullBlockFace = getSkullBlockFace(grave.getPlayer().getPlayer());
            Skull skull = (Skull) grave.getLocation().getBlock().getState();
            if (skullBlockFace != null) {
                skullRotate.setRotation(skullBlockFace);
                skull.setBlockData(skullRotate);
            }
            if (graveHeadName.equals("$entity") || graveHeadName.equals("")) {
                if (grave.getPlayer() != null) {
                    skull.setOwningPlayer(grave.getPlayer());
                } else if (grave.getEntityType() != null) {
                    plugin.getServer().broadcastMessage("TODO, Mob heads");
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
        if (location.getBlock().getType().isAir()) {
            return location;
        }
        if (data.graveReplace().contains(location.getBlock().getType()) || data.graveReplace().contains("ALL")) {
            return location;
        }
        Location top = getTop(location);
        if (top != null) {
            return top;
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

    public void removeGrave(Grave grave) {
        Material replace = grave.getReplace();
        if (replace == null) {
            replace = Material.AIR;
        }
        grave.getLocation().getBlock().setType(replace);

        data.removeGrave(grave);
        graves.remove(grave.getLocation());

        closeGrave(grave);
        lootSound(grave.getLocation());
        removeHologram(grave);
    }

    public void closeGrave(Grave grave) {
        List<HumanEntity> viewers = grave.getInventory().getViewers();
        for (HumanEntity viewer : new ArrayList<>(viewers)) {
            grave.getInventory().getViewers().remove(viewer);
            viewer.closeInventory();
        }
    }

    public void dropGrave(Grave grave) {
        for (ItemStack item : grave.getInventory()) {
            if (item != null) {
                grave.getLocation().getWorld().dropItemNaturally(grave.getLocation(), item);
                grave.getInventory().remove(item);
            }
        }
    }

    public void giveExperience(Grave grave, Player player) {
        if (grave.getExperience() != null && grave.getExperience() > 0) {
            player.giveExp(grave.getExperience());
            String expMessage = plugin.getConfig().getString("settings.expMessage")
                    .replace("$xp", grave.getExperience().toString()).replace("&", "§");
            if (!expMessage.equals("")) {
                player.sendMessage(expMessage);
            }
            grave.setExperience(null);
        }
    }

    public void dropExperience(Grave grave) {
        if (grave.getExperience() != null && grave.getExperience() > 0) {
            ExperienceOrb eo = (ExperienceOrb) grave.getLocation().getWorld().spawnEntity(grave.getLocation(), EntityType.EXPERIENCE_ORB);
            eo.setExperience(grave.getExperience());
            grave.setExperience(null);
        }
    }

    public void updateHologram(Grave grave) {
        if (!grave.getHolograms().isEmpty()) {
            for (Iterator<Map.Entry<UUID, Integer>> iterator = grave.getHolograms().entrySet()
                    .iterator(); iterator.hasNext(); ) {
                if (iterator.hasNext()) {
                    Map.Entry<UUID, Integer> entry = iterator.next();
                    ArmorStand armorStand = (ArmorStand) plugin.getServer().getEntity(entry.getKey());
                    if (armorStand != null) {
                        armorStand.setCustomName(parseHologram(entry.getValue(), grave));
                    }
                }
            }
        }
    }

    public String parseHologram(Integer lineNumber, Grave grave) {
        Integer graveTime = plugin.getConfig().getInt("settings.graveTime") * 1000;
        Long time = (graveTime - (System.currentTimeMillis() - grave.getTime())) / 1000;
        String timeString = getTimeString(time);
        return hologramLines.get(lineNumber).replace("$player", grave.getPlayer().getName())
                .replace("$time", timeString)
                .replace("$itemCount", getItemAmount(grave.getInventory()).toString())
                .replace("$xp", grave.getExperience().toString())
                .replace("&", "§");
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

        Integer lineNumber = 0;
        for (String ignored : plugin.getConfig().getStringList("settings.hologramLines")) {
            ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
            armorStand.setSmall(true);
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setCustomName(parseHologram(lineNumber, grave));
            armorStand.setCustomNameVisible(true);
            armorStand.addScoreboardTag("graveHologramLine:" + lineNumber);
            armorStand.addScoreboardTag("graveHologramCords:" + grave.getLocation().getX() + "_" +
                    grave.getLocation().getY() + "_" + grave.getLocation().getZ());
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
                    Map.Entry<UUID, Integer> entry = iterator.next();
                    ArmorStand armorStand = (ArmorStand) plugin.getServer().getEntity(entry.getKey());
                    armorStand.remove();
                    iterator.remove();
                }
            }
        }
    }

    public void lootSound(Location location) {
        String lootSound = plugin.getConfig().getString("settings.lootSound");
        if (!lootSound.equals("")) {
            location.getWorld().playSound(location, Sound.valueOf(lootSound.toUpperCase()), 1, 1);
        }
        String lootEffect = plugin.getConfig().getString("settings.lootEffect");
        if (!lootEffect.equals("")) {
            location.getWorld().playEffect(location, Effect.valueOf(lootEffect), 0);
        }
    }

    public void graveProtected(Player player, Location location) {
        String graveProtectedMessage = plugin.getConfig().getString("settings.graveProtectedMessage")
                .replace("&", "§");
        if (!graveProtectedMessage.equals("")) {
            player.sendMessage(graveProtectedMessage);
        }
        String graveProtectedSound = plugin.getConfig().getString("settings.graveProtectedSound");
        if (!graveProtectedSound.equals("")) {
            location.getWorld().playSound(location, Sound.valueOf(graveProtectedSound.toUpperCase()), 1, 1);
        }
    }

    public void permissionDenied(Player player) {
        String permissionDenied = plugin.getConfig().getString("settings.permissionDenied")
                .replace("&", "§");
        if (!permissionDenied.equals("")) {
            player.sendMessage(permissionDenied);
        }
    }

    @SuppressWarnings("deprecation")
    public void graveHeadLoad() {
        String graveHeadName = plugin.getConfig().getString("settings.graveHeadSkin");
        if (!graveHeadName.equals("")) {
            graveHead = plugin.getServer().getOfflinePlayer(graveHeadName);
        }
    }

    public void hologramLinesLoad() {
        hologramLines.clear();
        for (String line : plugin.getConfig().getStringList("settings.hologramLines")) {
            hologramLines.add(line);
        }
        Collections.reverse(hologramLines);
    }

    public static String getEntityName(EntityType entityType) {
        String name = WordUtils.capitalizeFully(entityType.toString()).replace("_", " ");
        return name;
    }

    public static Location roundLocation(Location location) {
        return new Location(location.getWorld(), Math.round(location.getBlockX()), Math.round(location.getY()),
                Math.round(location.getBlockZ()));
    }

    public static Integer getItemAmount(Inventory inventory) {
        Integer count = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null) {
                count++;
            }
        }
        return count;
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