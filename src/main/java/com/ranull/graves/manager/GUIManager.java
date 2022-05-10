package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GUIManager {
    private final Graves plugin;

    public GUIManager(Graves plugin) {
        this.plugin = plugin;
    }

    public void openGraveList(Entity entity) {
        openGraveList(entity, entity.getUniqueId(), true);
    }

    public void openGraveList(Entity entity, boolean sound) {
        openGraveList(entity, entity.getUniqueId(), sound);
    }

    public void openGraveList(Entity entity, Entity entity2) {
        openGraveList(entity, entity2.getUniqueId(), true);
    }

    public void openGraveList(Entity entity, UUID uuid) {
        openGraveList(entity, uuid, true);
    }

    public void openGraveList(Entity entity, UUID uuid, boolean sound) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            List<Grave> graveList = plugin.getGraveManager().getGraveList(uuid);
            List<String> permissionList = plugin.getPermissionList(player);

            if (!graveList.isEmpty()) {
                GraveList graveListMenu = new GraveList(uuid, graveList);
                String title = StringUtil.parseString(plugin.getConfig("gui.menu.list.title", player, permissionList)
                        .getString("gui.menu.list.title", "Graves Main Menu"), player, plugin);

                Inventory inventory = plugin.getServer().createInventory(graveListMenu,
                        InventoryUtil.getInventorySize(graveList.size()), title);

                setGraveListItems(inventory, graveList);
                graveListMenu.setInventory(inventory);
                player.openInventory(graveListMenu.getInventory());

                if (sound) {
                    plugin.getEntityManager().playPlayerSound("sound.menu-open", player, permissionList);
                }
            } else {
                plugin.getEntityManager().sendMessage("message.empty", player, permissionList);
            }
        }
    }

    public void setGraveListItems(Inventory inventory, UUID uuid) {
        setGraveListItems(inventory, plugin.getGraveManager().getGraveList(uuid));
    }

    public void setGraveListItems(Inventory inventory, List<Grave> graveList) {
        inventory.clear();

        int count = 1;

        for (Grave grave : graveList) {
            inventory.addItem(createGraveListItemStack(count, grave));
            count++;
        }
    }

    public void openGraveMenu(Entity entity, Grave grave) {
        openGraveMenu(entity, grave, true);
    }

    public void openGraveMenu(Entity entity, Grave grave, boolean sound) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            GraveMenu graveMenu = new GraveMenu(grave);
            String title = StringUtil.parseString(plugin.getConfig("gui.menu.grave.title", player, grave.getPermissionList())
                    .getString("gui.menu.grave.title", "Grave"), player, plugin);
            Inventory inventory = plugin.getServer().createInventory(graveMenu, InventoryUtil.getInventorySize(5), title);

            setGraveMenuItems(inventory, grave);
            graveMenu.setInventory(inventory);
            player.openInventory(graveMenu.getInventory());

            if (sound) {
                plugin.getEntityManager().playPlayerSound("sound.menu-open", player, grave);
            }
        }
    }

    public void setGraveMenuItems(Inventory inventory, Grave grave) {
        inventory.clear();

        ConfigurationSection configurationSection = plugin.getConfig("gui.menu.grave.slot", grave)
                .getConfigurationSection("gui.menu.grave.slot");

        if (configurationSection != null) {
            for (String string : configurationSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(string);

                    inventory.setItem(slot, createGraveMenuItemStack(slot, grave));
                } catch (NumberFormatException exception) {
                    plugin.debugMessage(string + " is not an int", 1);
                }
            }
        }
    }

    private ItemStack createGraveListItemStack(int number, Grave grave) {
        Material material;

        if (plugin.getConfig("gui.menu.list.item.block", grave).getBoolean("gui.menu.list.item.block")) {
            String materialString = plugin.getConfig("block.material", grave)
                    .getString("block.material", "CHEST");

            if (materialString.equals("PLAYER_HEAD") && !plugin.getVersionManager().hasBlockData()) {
                materialString = "SKULL_ITEM";
            }

            material = Material.matchMaterial(materialString);
        } else {
            material = Material.matchMaterial(plugin.getConfig("gui.menu.list.item.material", grave)
                    .getString("gui.menu.list.item.block", "CHEST"));
        }

        if (material == null) {
            material = Material.CHEST;
        }

        ItemStack itemStack = new ItemStack(material);

        if (itemStack.getType().name().equals("PLAYER_HEAD") || itemStack.getType().name().equals("SKULL_ITEM")) {
            itemStack = plugin.getCompatibility().getEntitySkullItemStack(grave, plugin);
        }

        if (itemStack.getItemMeta() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            String name = ChatColor.WHITE + StringUtil.parseString(plugin.getConfig("gui.menu.list.name", grave)
                    .getString("gui.menu.list.name"), grave, plugin).replace("%number%",
                    String.valueOf(number));
            List<String> loreList = new ArrayList<>();
            int customModelData = plugin.getConfig("gui.menu.list.model-data", grave)
                    .getInt("gui.menu.list.model-data", -1);

            for (String string : plugin.getConfig("gui.menu.list.lore", grave).getStringList("gui.menu.list.lore")) {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin));
            }

            if (plugin.getConfig().getBoolean("gui.menu.list.glow")) {
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (customModelData > -1) {
                itemMeta.setCustomModelData(customModelData);
            }

            itemMeta.setDisplayName(name);
            itemMeta.setLore(loreList);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    private ItemStack createGraveMenuItemStack(int slot, Grave grave) {
        String materialString = plugin.getConfig("gui.menu.grave.slot." + slot + ".material", grave)
                .getString("gui.menu.grave.slot." + slot + ".material", "PAPER");
        Material material = Material.matchMaterial(materialString);

        if (material == null) {
            material = Material.PAPER;

            plugin.debugMessage(materialString.toUpperCase() + " is not a Material ENUM", 1);
        }

        ItemStack itemStack = new ItemStack(material);

        if (itemStack.getItemMeta() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            String name = ChatColor.WHITE + StringUtil.parseString(plugin.getConfig("gui.menu.grave.slot." + slot + ".name", grave)
                    .getString("gui.menu.grave.slot." + slot + ".name"), grave, plugin);
            List<String> loreList = new ArrayList<>();
            int customModelData = plugin.getConfig("gui.menu.grave.slot." + slot + ".model-data", grave)
                    .getInt("gui.menu.grave.slot." + slot + ".model-data", -1);

            for (String string : plugin.getConfig("gui.menu.grave.slot." + slot + ".lore", grave)
                    .getStringList("gui.menu.grave.slot." + slot + ".lore")) {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin));
            }

            if (plugin.getConfig().getBoolean("gui.menu.grave.slot." + slot + ".glow")) {
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (customModelData > -1) {
                itemMeta.setCustomModelData(customModelData);
            }

            itemMeta.setDisplayName(name);
            itemMeta.setLore(loreList);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }
}
