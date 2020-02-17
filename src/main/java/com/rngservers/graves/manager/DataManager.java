package com.rngservers.graves.manager;

import com.rngservers.graves.Graves;
import com.rngservers.graves.inventory.GraveInventory;
import com.rngservers.graves.manager.GraveManager;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataManager {
    private Graves plugin;
    private FileConfiguration data;
    private File dataFile;
    private List<Material> graveReplace = new ArrayList<>();

    public DataManager(Graves plugin) {
        this.plugin = plugin;
        createDataFile();
        graveReplaceLoad();
    }

    public ConcurrentMap<Location, GraveInventory> getSavedGraves() {
        ConcurrentMap<Location, GraveInventory> graves = new ConcurrentHashMap<>();
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
                Integer aliveTime = null;
                if (data.isSet(worlds + "." + cords + ".alive")) {
                    aliveTime = data.getInt(worlds + "." + cords + ".alive");
                }

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
                GraveInventory grave = createGrave(location, items, graveTitle);
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

                Boolean protect = data.getBoolean(worlds + "." + cords + ".protect");
                grave.setProtected(protect);

                Integer protectTime = data.getInt(worlds + "." + cords + ".protectTime");
                if (protectTime == 0) {
                    protectTime = null;
                }
                grave.setProtectTime(protectTime);

                if (data.isSet(worlds + "." + cords + ".level")) {
                    Integer level = data.getInt(worlds + "." + cords + ".level");
                    grave.setExperience(level);
                }
                grave.setReplace(replace);
                grave.setPlayer(player);
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

    private GraveInventory createGrave(Location location, List<ItemStack> items, String graveTitle) {
        Inventory inventory = plugin.getServer().createInventory(null, GraveManager.getInventorySize(items.size()));
        for (ItemStack item : items) {
            if (item != null) {
                inventory.addItem(item);
            }
        }
        GraveInventory grave = new GraveInventory(location, inventory, graveTitle);
        return grave;
    }

    public void saveGrave(GraveInventory grave) {
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
            if (grave.getKiller() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".killer", grave.getKiller().getUniqueId().toString());
            }
            if (grave.getExperience() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".level", grave.getExperience());
            }
            if (grave.getProtected() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".protect", grave.getProtected());
            }
            if (grave.getProtectTime() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".protectTime", grave.getProtectTime());
            } else {
                data.set(world + "." + x + "_" + y + "_" + z + ".protectTime", 0);
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
