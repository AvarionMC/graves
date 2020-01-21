package com.rngservers.graves.gui;

import com.rngservers.graves.Main;
import com.rngservers.graves.grave.Grave;
import com.rngservers.graves.grave.GraveManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class GUIManager {
    private Main plugin;
    private GraveManager graveManager;

    public GUIManager(Main plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    public void teleportGrave(Player player, ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "graveLocation");
        String[] cords = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING).split("_");
        try {
            World world = plugin.getServer().getWorld(cords[0]);
            Double x = Double.parseDouble(cords[1]);
            Double y = Double.parseDouble(cords[2]);
            Double z = Double.parseDouble(cords[3]);
            Location location = new Location(world, x, y, z);
            location.add(0.5, 1, 0.5);
            player.teleport(location);
            String graveTeleportMessage = plugin.getConfig().getString("settings.graveTeleportMessage").replace("&", "§");
            if (!graveTeleportMessage.equals("")) {
                player.sendMessage(graveTeleportMessage);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public void openGraveGUI(Player player) {
        openGraveGUI(player, player);
    }

    public void openGraveGUI(Player player, OfflinePlayer otherPlayer) {
        List<ItemStack> graveItems = graveItems(otherPlayer);
        if (graveItems.size() == 0) {
            if (player.equals(otherPlayer)) {
                String guiEmpty = plugin.getConfig().getString("settings.guiEmpty").replace("&", "§");
                if (!guiEmpty.equals("")) {
                    player.sendMessage(guiEmpty);
                }
            } else {
                player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "] "
                        + ChatColor.GOLD + otherPlayer.getName() + ChatColor.RESET + " does not have any graves!");
            }
            return;
        }
        String guiName = plugin.getConfig().getString("settings.guiTitle").replace("$player", otherPlayer.getName())
                .replace("&", "§");
        GravesGUI gui = new GravesGUI("§5§3§1§6§r§0§r" + guiName, graveItems, GraveManager.getInventorySize(graveItems.size()));
        gui.openInventory(player);
    }

    public List<ItemStack> graveItems(OfflinePlayer player) {
        ConcurrentMap<Location, Grave> graves = graveManager.getGraves(player);
        List<ItemStack> items = new ArrayList<>();

        for (ConcurrentMap.Entry<Location, Grave> entry : graves.entrySet()) {
            Grave grave = entry.getValue();
            Material graveBlock = Material.matchMaterial(plugin.getConfig().getString("settings.graveBlock"));
            if (graveBlock == null) {
                graveBlock = Material.CHEST;
            }
            ItemStack item = new ItemStack(graveBlock, 1);
            ItemMeta meta = item.getItemMeta();
            String graveHeadName = plugin.getConfig().getString("settings.graveHeadSkin");
            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta) meta;
                if (graveHeadName.equals("$entity") || graveHeadName.equals("")) {
                    if (grave.getPlayer() != null) {
                        skull.setOwningPlayer(grave.getPlayer());
                    } else if (grave.getEntityType() != null) {
                        // TODO Mob heads
                    }
                } else {
                    if (graveManager.getGraveHead() != null) {
                        skull.setOwningPlayer(graveManager.getGraveHead());
                    }
                }
                item.setItemMeta(skull);
            }
            List<String> lores = new ArrayList<String>();
            List<String> loreLines = plugin.getConfig().getStringList("settings.guiLore");
            for (String lore : loreLines) {
                String line = ChatColor.GRAY + lore.replace("$location", "LOC")
                        .replace("$item", grave.getItemAmount().toString())
                        .replace("$time", "Time")
                        .replace("$x", String.valueOf(grave.getLocation().getBlockX()))
                        .replace("$y", String.valueOf(grave.getLocation().getBlockY()))
                        .replace("$z", String.valueOf(grave.getLocation().getBlockZ()))
                        .replace("&", "§");
                if (grave.getLevel() != null && grave.getLevel() > 0) {
                    line = line.replace("$level", grave.getLevel().toString());
                } else {
                    line = line.replace("$level", "0");
                }
                lores.add(line);
            }
            String guiGrave = plugin.getConfig().getString("settings.guiGrave")
                    .replace("$x", String.valueOf(grave.getLocation().getBlockX()))
                    .replace("$y", String.valueOf(grave.getLocation().getBlockY()))
                    .replace("$z", String.valueOf(grave.getLocation().getBlockZ()))
                    .replace("&", "§");
            meta.setDisplayName(guiGrave);
            meta.setLore(lores);
            item.setItemMeta(meta);
            NamespacedKey key = new NamespacedKey(plugin, "graveLocation");
            String keyValue = grave.getLocation().getWorld().getName() + "_"
                    + grave.getLocation().getX() + "_" + grave.getLocation().getY() + "_" + grave.getLocation().getZ();
            item.getItemMeta().getPersistentDataContainer().set(key, PersistentDataType.STRING, keyValue);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }
}
