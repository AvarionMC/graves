package com.rngservers.graves.data;

import com.rngservers.graves.Main;
import com.rngservers.graves.grave.Grave;
import com.rngservers.graves.grave.GraveManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataManager {
    private Main plugin;
    private FileConfiguration data;
    private File dataFile;
    private List<Material> graveReplace = new ArrayList<>();

    public DataManager(Main plugin) {
        this.plugin = plugin;
        createDataFile();
        graveReplaceLoad();
    }

    public ConcurrentMap<Location, Grave> getSavedGraves() {
        ConcurrentMap<Location, Grave> graves = new ConcurrentHashMap<>();
        for (String worlds : data.getKeys(false)) {
            for (String cords : data.getConfigurationSection(worlds).getKeys(false)) {
                String[] cord = cords.split("_");
                int x = Integer.valueOf(cord[0]);
                int y = Integer.valueOf(cord[1]);
                int z = Integer.valueOf(cord[2]);
                World world = plugin.getServer().getWorld(worlds);
                Location location = new Location(world, x, y, z);
                List<ItemStack> items = new ArrayList<>();
                for (String slot : data.getConfigurationSection(worlds + "." + cords + ".items").getKeys(false)) {
                    ItemStack item = data.getItemStack(worlds + "." + cords + ".items." + slot);
                    if (item != null) {
                        items.add(item);
                    }
                }

                Material replace = Material.matchMaterial(data.getString(worlds + "." + cords + ".replace"));
                Long time = data.getLong(worlds + "." + cords + ".time");
                Integer aliveTime = data.getInt(worlds + "." + cords + ".alive");

                OfflinePlayer player = null;
                EntityType entityType = null;
                String graveTitle = "Grave";
                if (data.isSet(worlds + "." + cords + ".player")) {
                    player = plugin.getServer().getOfflinePlayer(UUID.fromString(data.getString(worlds + "." + cords + ".player")));
                    graveTitle = graveTitle(player);
                } else if (data.isSet(worlds + "." + cords + ".entity")) {
                    entityType = EntityType.valueOf(data.getString(worlds + "." + cords + ".entity"));
                    graveTitle = graveTitle(entityType);
                }
                Grave grave = createGrave(location, items, graveTitle);
                if (player != null) {
                    grave.setPlayer(player);
                }
                if (entityType != null) {
                    grave.setEntityType(entityType);
                }
                if (data.isSet(worlds + "." + cords + ".killer")) {
                    OfflinePlayer killer = plugin.getServer().getOfflinePlayer(UUID.fromString(data.getString(worlds + "." + cords + ".killer")));
                    grave.setKiller(killer);
                }
                grave.setCreatedTime(time);
                grave.setAliveTime(aliveTime);
                if (data.isSet(worlds + "." + cords + ".level")) {
                    Integer level = data.getInt(worlds + "." + cords + ".level");
                    grave.setLevel(level);
                }
                if (data.isSet(worlds + "." + cords + ".experience")) {
                    Float experience = (float) data.getDouble(worlds + "." + cords + ".experience");
                    grave.setExperience(experience);
                }
                grave.setReplace(replace);
                grave.setPlayer(player);
                grave.setHolograms(convertListHologram(data.getStringList(worlds + "." + cords + ".hologram")));
                if (location != null && grave != null) {
                    graves.put(location, grave);
                }
                data.set(worlds + "." + cords, null);
            }
        }
        saveData();
        dataFile.delete();
        return graves;
    }

    public String graveTitle(OfflinePlayer player) {
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", player.getName()).replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = player.getName() + "'s Grave";
        }
        return graveTitle;
    }

    public String graveTitle(EntityType entityType) {
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", GraveManager.formatString(entityType.toString())).replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = GraveManager.formatString(entityType.toString()) + "'s Grave";
        }
        return graveTitle;
    }

    private Grave createGrave(Location location, List<ItemStack> items, String graveTitle) {
        Inventory inventory = plugin.getServer().createInventory(null, GraveManager.getInventorySize(items.size()));
        for (ItemStack item : items) {
            if (item != null) {
                inventory.addItem(item);
            }
        }
        Grave grave = new Grave(location, inventory, graveTitle);
        return grave;
    }

    public void saveGrave(Grave grave) {
        if (grave.getItemAmount() == 0) {
            return;
        }
        if (grave != null && grave.getLocation().getWorld() != null && grave.getInventory() != null) {
            String world = grave.getLocation().getWorld().getName();
            int x = grave.getLocation().getBlockX();
            int y = grave.getLocation().getBlockY();
            int z = grave.getLocation().getBlockZ();

            if (grave.getPlayer() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".player", grave.getPlayer().getUniqueId().toString());
            } else if (grave.getEntityType() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".entity", grave.getEntityType().toString());
            }
            data.set(world + "." + x + "_" + y + "_" + z + ".time", grave.getCreatedTime());
            data.set(world + "." + x + "_" + y + "_" + z + ".alive", grave.getAliveTime());
            data.set(world + "." + x + "_" + y + "_" + z + ".replace", grave.getReplace().toString());
            if (!grave.getHolograms().isEmpty()) {
                data.set(world + "." + x + "_" + y + "_" + z + ".hologram", convertMapHologram(grave.getHolograms()));
            }
            if (grave.getKiller() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".killer", grave.getKiller().getUniqueId().toString());
            }
            if (grave.getLevel() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".experience", grave.getLevel());
            }
            if (grave.getExperience() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".experience", grave.getExperience());
            }
            int counter = 0;
            for (ItemStack item : grave.getInventory()) {
                if (item != null) {
                    data.set(world + "." + x + "_" + y + "_" + z + ".items." + counter, item);
                    counter++;
                }
            }
            saveData();
        }
    }

    public List<String> convertMapHologram(ConcurrentMap<UUID, Integer> map) {
        List<String> list = new ArrayList<>();
        for (Iterator<ConcurrentMap.Entry<UUID, Integer>> iterator = map.entrySet()
                .iterator(); iterator.hasNext(); ) {
            if (iterator.hasNext()) {
                ConcurrentMap.Entry<UUID, Integer> entry = iterator.next();
                list.add(entry.getKey().toString() + ":" + entry.getValue().toString());
            }
        }
        return list;
    }

    public ConcurrentMap<UUID, Integer> convertListHologram(List<String> list) {
        ConcurrentMap<UUID, Integer> map = new ConcurrentHashMap<>();
        for (String string : list) {
            String[] parts = string.split(":");
            UUID uuid = UUID.fromString(parts[0]);
            Integer lineNumber = Integer.parseInt(parts[1]);
            map.put(uuid, lineNumber);
        }
        return map;
    }

    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createDataFile() {
        dataFile = new File(plugin.getDataFolder(), "graves.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        data = new YamlConfiguration();
        try {
            data.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void graveReplaceLoad() {
        graveReplace.clear();
        for (String line : plugin.getConfig().getStringList("settings.graveReplace")) {
            Material material = Material.matchMaterial(line.toUpperCase());
            if (material != null) {
                graveReplace.add(material);
            }
        }
    }

    public List<Material> graveReplace() {
        if (graveReplace.isEmpty()) {
            graveReplaceLoad();
        }
        return graveReplace;
    }
}
