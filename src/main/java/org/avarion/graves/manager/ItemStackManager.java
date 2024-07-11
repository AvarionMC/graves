package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ItemStackManager extends EntityDataManager {
    private final Graves plugin;

    public ItemStackManager(Graves plugin) {
        super(plugin);

        this.plugin = plugin;
    }

    public @NotNull ItemStack getGraveObituary(Grave grave) {
        ItemStack itemStack = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();

        if (bookMeta != null) {
            List<String> lineList = new ArrayList<>();
            List<String> loreList = new ArrayList<>();

            for (String lore : plugin.getConfigStringList("obituary.line", grave)) {
                lineList.add(StringUtil.parseString(lore, grave.getLocationDeath(), grave, plugin));
            }

            for (String string : plugin.getConfigStringList("obituary.lore", grave)) {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin));
            }

            int customModelData = plugin.getConfigInt("obituary.model-data", grave, -1);

            if (customModelData > -1) {
                bookMeta.setCustomModelData(customModelData);
            }

            if (plugin.getConfigBool("obituary.glow", grave)) {
                bookMeta.addEnchant(Enchantment.DURABILITY, 1, true);

                bookMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            bookMeta.setGeneration(null);
            bookMeta.setPages(String.join("\n", lineList));
            bookMeta.setLore(loreList);
            bookMeta.setTitle(ChatColor.WHITE
                              + StringUtil.parseString(plugin.getConfigString("obituary.title", grave), grave, plugin));
            bookMeta.setAuthor(StringUtil.parseString(plugin.getConfigString("obituary.author", grave), grave, plugin));
            itemStack.setItemMeta(bookMeta);
        }

        return itemStack;
    }

    public @NotNull ItemStack getGraveHead(Grave grave) {
        ItemStack itemStack = plugin.getCompatibility().getSkullItemStack(grave, plugin);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            List<String> loreList = new ArrayList<>();

            for (String string : plugin.getConfigStringList("head.lore", grave)) {
                loreList.add(ChatColor.GRAY + StringUtil.parseString(string, grave.getLocationDeath(), grave, plugin));
            }

            int customModelData = plugin.getConfigInt("head.model-data", grave, -1);

            if (customModelData > -1) {
                itemMeta.setCustomModelData(customModelData);
            }

            itemMeta.setLore(loreList);
            itemMeta.setDisplayName(ChatColor.WHITE
                                    + StringUtil.parseString(plugin.getConfigString("head.name", grave), grave, plugin));
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    public @NotNull ItemStack createGraveListItemStack(int number, Grave grave) {
        Material material;

        if (plugin.getConfigBool("gui.menu.list.item.block", grave)) {
            String materialString = plugin.getConfigString("block.material", grave, "CHEST");

            material = Material.matchMaterial(materialString);
        }
        else {
            material = Material.matchMaterial(plugin.getConfig("gui.menu.list.item.material", grave)
                                                    .getString("gui.menu.list.item.block", "CHEST"));
        }

        if (material == null) {
            material = Material.CHEST;
        }

        ItemStack itemStack = new ItemStack(material);

        if (itemStack.getType().name().equals("PLAYER_HEAD") || itemStack.getType().name().equals("SKULL_ITEM")) {
            itemStack = plugin.getCompatibility().getSkullItemStack(grave, plugin);
        }

        if (itemStack.getItemMeta() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            String name = ChatColor.WHITE
                          + StringUtil.parseString(plugin.getConfigString("gui.menu.list.name", grave), grave, plugin)
                                      .replace("%number%", String.valueOf(number));
            List<String> loreList = new ArrayList<>();
            int customModelData = plugin.getConfigInt("gui.menu.list.model-data", grave, -1);

            for (String string : plugin.getConfigStringList("gui.menu.list.lore", grave)) {
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

    public @NotNull ItemStack createGraveMenuItemStack(int slot, Grave grave) {
        String materialString = plugin.getConfigString("gui.menu.grave.slot." + slot + ".material", grave, "PAPER");
        Material material = Material.matchMaterial(materialString);

        if (material == null) {
            material = Material.PAPER;

            plugin.debugMessage(materialString.toUpperCase() + " is not a Material ENUM", 1);
        }

        ItemStack itemStack = new ItemStack(material);

        if (itemStack.getItemMeta() != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            String name = ChatColor.WHITE + StringUtil.parseString(plugin.getConfigString("gui.menu.grave.slot."
                                                                                          + slot
                                                                                          + ".name", grave), grave, plugin);
            List<String> loreList = new ArrayList<>();
            int customModelData = plugin.getConfigInt("gui.menu.grave.slot." + slot + ".model-data", grave, -1);

            for (String string : plugin.getConfigStringList("gui.menu.grave.slot." + slot + ".lore", grave)) {
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
