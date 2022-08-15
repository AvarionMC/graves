package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.SkinUtil;
import com.ranull.graves.util.StringUtil;
import com.ranull.graves.util.UUIDUtil;
import com.ranull.graves.util.YAMLUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ImportManager {
    private final Graves plugin;

    public ImportManager(Graves plugin) {
        this.plugin = plugin;
    }

    public List<Grave> importExternalPluginGraves() {
        List<Grave> graveList = new ArrayList<>();

        graveList.addAll(importAngelChest());

        return graveList;
    }

    private List<Grave> importAngelChest() {
        List<Grave> graveList = new ArrayList<>();
        File angelChest = new File(plugin.getPluginsFolder(), "AngelChest");

        if (angelChest.exists()) {
            File angelChests = new File(angelChest, "angelchests");

            if (angelChests.exists()) {
                File[] files = angelChests.listFiles();

                if (files != null) {
                    for (File file : files) {
                        Grave grave = convertAngelChestToGrave(file);

                        if (grave != null) {
                            graveList.add(grave);
                        }
                    }
                }
            }
        }

        return graveList;
    }

    public Grave convertAngelChestToGrave(File file) {
        FileConfiguration angelChest = loadFile(file);

        if (angelChest != null) {
            Grave grave = new Grave(UUID.randomUUID());
            UUID worldUUID = UUIDUtil.getUUID(angelChest.getString("worldid", "null"));
            String[] logfileSplit = angelChest.getString("logfile", "").split("_");

            if (worldUUID != null) {
                World world = plugin.getServer().getWorld(worldUUID);

                if (world == null && logfileSplit.length > 1) {
                    world = plugin.getServer().getWorld(logfileSplit[1]);
                }

                if (world != null) {
                    int x = angelChest.getInt("x", 0);
                    int y = angelChest.getInt("y", 0);
                    int z = angelChest.getInt("z", 0);

                    grave.setLocationDeath(new Location(world, x, y, z));
                }
            }

            grave.setOwnerType(EntityType.PLAYER);
            grave.setOwnerUUID(UUIDUtil.getUUID(angelChest.getString("owner", null)));

            if (logfileSplit.length > 0) {
                grave.setOwnerName(logfileSplit[0]);
            }

            if (grave.getOwnerUUID() != null) {
                Player player = plugin.getServer().getPlayer(grave.getOwnerUUID());

                grave.setOwnerTexture(SkinUtil.getTexture(player));
                grave.setOwnerTextureSignature(SkinUtil.getSignature(player));
            }

            //grave.setTimeCreation(angelChest.getLong("created", System.currentTimeMillis()));
            grave.setTimeCreation(System.currentTimeMillis());
            //grave.setTimeAlive(angelChest.getInt("secondsLeft", 0) * 10000L);
            grave.setTimeAlive(plugin.getConfig("grave.time", grave).getInt("grave.time") * 1000L);
            grave.setProtection(angelChest.getBoolean("isProtected", false));
            grave.setExperience(angelChest.getInt("experience", 0));

            if (angelChest.isConfigurationSection("deathCause")) {
                String damageCause = angelChest.getString("deathCause.damageCause", "VOID");
                String killer = angelChest.getString("deathCause.killer", "null");

                grave.setKillerName(!killer.equals("null") ? killer : StringUtil.format(damageCause));
            }

            List<ItemStack> itemStackList = new ArrayList<>();

            if (angelChest.contains("armorInv")) {
                List<ItemStack> armorItemStackList = (List<ItemStack>) angelChest.getList("armorInv",
                        new ArrayList<ItemStack>());

                Collections.reverse(armorItemStackList);
                itemStackList.addAll(armorItemStackList);
            }

            if (angelChest.contains("storageInv")) {
                itemStackList.addAll((List<ItemStack>) angelChest.getList("storageInv", new ArrayList<ItemStack>()));
            }

            if (angelChest.contains("extraInv")) {
                itemStackList.addAll((List<ItemStack>) angelChest.getList("extraInv", new ArrayList<ItemStack>()));
            }

            if (!itemStackList.isEmpty()) {
                String title = StringUtil.parseString(plugin.getConfig("gui.grave.title", grave)
                        .getString("gui.grave.title"), grave.getLocationDeath(), grave, plugin);
                Grave.StorageMode storageMode = plugin.getGraveManager()
                        .getStorageMode(plugin.getConfig("storage.mode", grave).getString("storage.mode"));

                Inventory inventory = plugin.getGraveManager().createGraveInventory(grave, grave.getLocationDeath(),
                        itemStackList, title, storageMode);

                grave.setInventory(inventory);
            }

            return grave;
        }

        return null;
    }

    private FileConfiguration loadFile(File file) {
        if (YAMLUtil.isValidYAML(file)) {
            try {
                return YamlConfiguration.loadConfiguration(file);
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
