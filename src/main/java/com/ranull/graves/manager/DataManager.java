package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.GraveInventory;
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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataManager {
    private Graves plugin;
    private FileConfiguration dataConfig;
    private File dataFile;
    private List<Material> graveReplace = new ArrayList<>();

    public DataManager(Graves plugin) {
        this.plugin = plugin;
        createDataFile();
        graveReplaceLoad();
    }

    public ConcurrentMap<Location, GraveInventory> getSavedGraves() {
        ConcurrentMap<Location, GraveInventory> gravesList = new ConcurrentHashMap<>();

        for (String worlds : dataConfig.getKeys(false)) {
            for (String cordsString : Objects.requireNonNull(dataConfig.getConfigurationSection(worlds)).getKeys(false)) {
                World world = plugin.getServer().getWorld(worlds);

                String[] cords = cordsString.split("_");

                int x = Integer.parseInt(cords[0]);
                int y = Integer.parseInt(cords[1]);
                int z = Integer.parseInt(cords[2]);

                Location location = new Location(world, x, y, z);

                List<ItemStack> itemList = new ArrayList<>();
                for (String slot : Objects.requireNonNull(dataConfig.getConfigurationSection(worlds + "." +
                        cordsString + ".items")).getKeys(false)) {
                    ItemStack itemStack = dataConfig.getItemStack(worlds + "." + cordsString + ".items." + slot);

                    if (itemStack != null) {
                        itemList.add(itemStack);
                    }
                }

                OfflinePlayer player = null;
                EntityType entityType = null;

                String title = "Grave";

                if (dataConfig.isSet(worlds + "." + cordsString + ".player")) {
                    player = plugin.getServer().getOfflinePlayer(UUID
                            .fromString(Objects.requireNonNull(dataConfig
                                    .getString(worlds + "." + cordsString + ".player"))));
                    title = getGraveTitle(player);
                } else if (dataConfig.isSet(worlds + "." + cordsString + ".entity")) {
                    entityType = EntityType.valueOf(dataConfig.getString(worlds + "." + cordsString + ".entity"));
                    title = getGraveTitle(entityType);
                }

                GraveInventory graveInventory = createGrave(location, itemList, title);

                if (player != null) {
                    graveInventory.setPlayer(player);
                }

                if (entityType != null) {
                    graveInventory.setEntityType(entityType);
                }

                if (dataConfig.isSet(worlds + "." + cordsString + ".alive")) {
                    graveInventory.setAliveTime(dataConfig.getInt(worlds + "." + cordsString + ".alive"));
                }

                if (dataConfig.isSet(worlds + "." + cordsString + ".time")) {
                    graveInventory.setCreatedTime(dataConfig.getLong(worlds + "." + cordsString + ".time"));
                }

                if (dataConfig.isSet(worlds + "." + cordsString + ".protect")) {
                    graveInventory.setProtected(dataConfig.getBoolean(worlds + "." + cordsString + ".protect"));
                }

                if (dataConfig.isSet(worlds + "." + cordsString + ".unlink")) {
                    graveInventory.setUnlink(dataConfig.getBoolean(worlds + "." + cordsString + ".unlink"));
                }

                if (dataConfig.isSet(worlds + "." + cordsString + ".protectTime")) {
                    graveInventory.setProtectTime(dataConfig.getInt(worlds + "." + cordsString + ".protectTime"));
                }

                if (dataConfig.isSet(worlds + "." + cordsString + ".killer")) {
                    OfflinePlayer killer = plugin.getServer().getOfflinePlayer(UUID.
                            fromString(Objects.requireNonNull(dataConfig.getString(worlds + "." + cordsString + ".killer"))));

                    graveInventory.setKiller(killer);
                }

                if (dataConfig.isSet(worlds + "." + cordsString + ".level")) {
                    graveInventory.setExperience(dataConfig.getInt(worlds + "." + cordsString + ".level"));
                }

                if (dataConfig.isSet(worlds + "." + cordsString + ".replace")) {
                    Material replaceMaterial = Material
                            .matchMaterial(Objects.requireNonNull(dataConfig.getString(worlds + "." +
                                    cordsString + ".replace")));

                    graveInventory.setReplace(replaceMaterial);
                }

                gravesList.put(location, graveInventory);

                dataConfig.set(worlds + "." + cordsString, null);
            }
        }
        saveData();

        if (dataFile.delete()) {
            plugin.getServer().getLogger().info("[Graves] Loaded saved graves!");
        }

        return gravesList;
    }

    public String getGraveTitle(OfflinePlayer player) {
        String graveTitle = Objects.requireNonNull(plugin.getConfig().getString("settings.title"))
                .replace("$entity", Objects.requireNonNull(player.getName()))
                .replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = player.getName() + "'s Grave";
        }

        return graveTitle;
    }

    public String getGraveTitle(EntityType entityType) {
        String graveTitle = Objects.requireNonNull(plugin.getConfig().getString("settings.title"))
                .replace("$entity", GraveManager.formatString(entityType.toString()))
                .replace("&", "§");
        if (graveTitle.equals("")) {
            graveTitle = GraveManager.formatString(entityType.toString()) + "'s Grave";
        }

        return graveTitle;
    }

    private GraveInventory createGrave(Location location, List<ItemStack> itemList, String graveTitle) {
        Inventory inventory = plugin.getServer().createInventory(null,
                GraveManager.getInventorySize(itemList.size()));

        for (ItemStack itemStack : itemList) {
            if (itemStack != null) {
                inventory.addItem(itemStack);
            }
        }

        return new GraveInventory(location, inventory, graveTitle);
    }

    public void saveGrave(GraveInventory graveInventory) {
        if (graveInventory.getItemAmount() == 0) {
            return;
        }

        if (graveInventory.getLocation().getWorld() != null) {
            String world = graveInventory.getLocation().getWorld().getName();
            int x = graveInventory.getLocation().getBlockX();
            int y = graveInventory.getLocation().getBlockY();
            int z = graveInventory.getLocation().getBlockZ();

            dataConfig.set(world + "." + x + "_" + y + "_" + z + ".time", graveInventory.getCreatedTime());
            dataConfig.set(world + "." + x + "_" + y + "_" + z + ".replace", graveInventory.getReplaceMaterial().toString());
            dataConfig.set(world + "." + x + "_" + y + "_" + z + ".protect", graveInventory.getProtected());
            dataConfig.set(world + "." + x + "_" + y + "_" + z + ".unlink", graveInventory.getUnlink());

            if (graveInventory.getPlayer() != null) {
                dataConfig.set(world + "." + x + "_" + y + "_" + z + ".player", graveInventory.getPlayer().getUniqueId().toString());
            } else if (graveInventory.getEntityType() != null) {
                dataConfig.set(world + "." + x + "_" + y + "_" + z + ".entity", graveInventory.getEntityType().toString());
            }

            if (graveInventory.getAliveTime() > 0) {
                dataConfig.set(world + "." + x + "_" + y + "_" + z + ".alive", graveInventory.getAliveTime());
            }

            if (graveInventory.getKiller() != null) {
                dataConfig.set(world + "." + x + "_" + y + "_" + z + ".killer", graveInventory.getKiller().getUniqueId().toString());
            }

            if (graveInventory.getExperience() > 0) {
                dataConfig.set(world + "." + x + "_" + y + "_" + z + ".level", graveInventory.getExperience());
            }

            dataConfig.set(world + "." + x + "_" + y + "_" + z + ".protectTime", Math.max(graveInventory.getProtectTime(), 0));

            int count = 0;
            for (ItemStack itemStack : graveInventory.getInventory()) {
                if (itemStack != null) {
                    dataConfig.set(world + "." + x + "_" + y + "_" + z + ".items." + count, itemStack);
                    count++;
                }
            }

            saveData();
        }
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createDataFile() {
        dataFile = new File(plugin.getDataFolder(), "graves.yml");
        if (!dataFile.exists()) {
            try {
                if (dataFile.createNewFile()) {
                    plugin.getLogger().info("Loaded data file!");
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        dataConfig = new YamlConfiguration();
        try {
            dataConfig.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void graveReplaceLoad() {
        graveReplace.clear();

        for (String line : plugin.getConfig().getStringList("settings.replace")) {
            Material replaceMaterial = Material.matchMaterial(line.toUpperCase());

            if (replaceMaterial != null) {
                graveReplace.add(replaceMaterial);
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
