package com.rngservers.graves.data;

import com.rngservers.graves.Main;
import com.rngservers.graves.chest.Grave;
import com.rngservers.graves.chest.GraveManager;
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
import java.util.*;

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

    public Map<Location, Grave> getSavedGraves() {
        Map<Location, Grave> graves = new HashMap<>();
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
                Integer experience = data.getInt(worlds + "." + cords + ".experience");

                Grave grave = null;
                if (data.isSet(worlds + "." + cords + ".player")) {
                    OfflinePlayer player = plugin.getServer().getOfflinePlayer(UUID.fromString(data.getString(worlds + "." + cords + ".player")));
                    grave = createGrave(location, items.toArray(new ItemStack[0]), experience, replace, player);

                } else if (data.isSet(worlds + "." + cords + ".entity")) {
                    EntityType entityType = EntityType.valueOf(data.getString(worlds + "." + cords + ".entity"));
                    grave = createGrave(location, items.toArray(new ItemStack[0]), experience, replace, entityType);
                }
                if (grave != null) {
                    if (data.isSet(worlds + "." + cords + ".killer")) {
                        OfflinePlayer killer = plugin.getServer().getOfflinePlayer(UUID.fromString(data.getString(worlds + "." + cords + ".killer")));
                        grave.setKiller(killer);
                    }
                    graves.put(location, grave);
                    data.set(worlds + "." + cords, null);
                }
            }
        }
        saveData();
        return graves;
    }

    public Grave createGrave(Location location, ItemStack[] items, Integer experience, Material replace, OfflinePlayer player) {
        Inventory inventory = plugin.getServer().createInventory(null, 54);
        for (ItemStack item : items) {
            if (item != null) {
                inventory.addItem(item);
            }
        }
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", player.getName()).replace("&", "§");

        Grave grave = new Grave(location, inventory, graveTitle);
        grave.setPlayer(player);
        grave.setReplace(replace);
        grave.setExperience(experience);

        return grave;
    }

    public Grave createGrave(Location location, ItemStack[] items, Integer experience, Material replace, EntityType entityType) {
        Inventory inventory = plugin.getServer().createInventory(null, 54);
        for (ItemStack item : items) {
            if (item != null) {
                inventory.addItem(item);
            }
        }
        String graveTitle = plugin.getConfig().getString("settings.graveTitle")
                .replace("$entity", GraveManager.getEntityName(entityType)).replace("&", "§");

        Grave grave = new Grave(location, inventory, graveTitle);
        grave.setEntityType(entityType);
        grave.setReplace(replace);
        grave.setExperience(experience);

        return grave;
    }

    public void saveGrave(Grave grave) {
        if (grave.getItemAmount() == 0) {
            return;
        }
        Inventory inventory = grave.getInventory();
        if (inventory != null) {
            String world = grave.getLocation().getWorld().getName();
            int x = grave.getLocation().getBlockX();
            int y = grave.getLocation().getBlockY();
            int z = grave.getLocation().getBlockZ();

            if (grave.getPlayer() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".player", grave.getPlayer().getUniqueId().toString());
            } else if (grave.getEntityType() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".entity", grave.getEntityType().toString());
            }
            data.set(world + "." + x + "_" + y + "_" + z + ".time", grave.getTime());
            data.set(world + "." + x + "_" + y + "_" + z + ".replace", grave.getReplace().toString());
            if (grave.getKiller() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".killer", grave.getKiller().getUniqueId().toString());
            }
            if (grave.getExperience() != null) {
                data.set(world + "." + x + "_" + y + "_" + z + ".experience", grave.getExperience().toString());
            }
            int counter = 0;
            for (ItemStack item : inventory.getStorageContents()) {
                if (item != null) {
                    data.set(world + "." + x + "_" + y + "_" + z + ".items." + counter, item);
                    counter++;
                }
            }
        }
        saveData();
    }

    public void removeGrave(Grave grave) {
        String world = grave.getLocation().getWorld().getName();
        int x = grave.getLocation().getBlockX();
        int y = grave.getLocation().getBlockY();
        int z = grave.getLocation().getBlockZ();
        data.set(world + "." + x + "_" + y + "_" + z, null);
        saveData();
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
