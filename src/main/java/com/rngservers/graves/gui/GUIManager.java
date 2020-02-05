package com.rngservers.graves.gui;

import com.rngservers.graves.Main;
import com.rngservers.graves.grave.Grave;
import com.rngservers.graves.grave.GraveManager;
import com.rngservers.graves.hooks.Vault;
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
    private Vault vault;

    public GUIManager(Main plugin, GraveManager graveManager, Vault vault) {
        this.plugin = plugin;
        this.graveManager = graveManager;
        this.vault = vault;
    }

    public void teleportGrave(Player player, ItemStack item) {
        Double graveTeleportCost = plugin.getConfig().getDouble("settings.graveTeleportCost");
        if (vault != null) {
            Double balance = vault.getEconomy().getBalance(player);
            if (balance < graveTeleportCost) {
                String notEnoughMoneyMessage = plugin.getConfig().getString("settings.notEnoughMoneyMessage")
                        .replace("$money", graveTeleportCost.toString()).replace("&", "§");
                if (!notEnoughMoneyMessage.equals("")) {
                    player.sendMessage(notEnoughMoneyMessage);
                }
                return;
            } else {
                vault.getEconomy().withdrawPlayer(player, graveTeleportCost);
            }
        }
        Location location = getGraveLocation(item);
        if (location != null) {
            location = graveManager.getTeleportLocation(player, location);
            if (location != null) {
                player.teleport(location);
                String graveTeleportMessage = plugin.getConfig().getString("settings.graveTeleportMessage")
                        .replace("$money", graveTeleportCost.toString()).replace("&", "§");
                if (!graveTeleportMessage.equals("")) {
                    player.sendMessage(graveTeleportMessage);
                }
                String graveTeleportSound = plugin.getConfig().getString("settings.graveTeleportSound");
                if (!graveTeleportSound.equals("")) {
                    player.getWorld().playSound(player.getLocation(), Sound.valueOf(graveTeleportSound.toUpperCase()), 1.0F, 1.0F);
                }
            } else {
                String graveTeleportFailedMessage = plugin.getConfig().getString("settings.graveTeleportFailedMessage")
                        .replace("$money", graveTeleportCost.toString()).replace("&", "§");
                if (!graveTeleportFailedMessage.equals("")) {
                    player.sendMessage(graveTeleportFailedMessage);
                }
            }
        }
    }

    public Location getGraveLocation(ItemStack item) {
        NamespacedKey key = new NamespacedKey(this.plugin, "graveLocation");
        String[] cords = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING).split("#");
        try {
            World world = plugin.getServer().getWorld(cords[0]);
            Double x = Double.parseDouble(cords[1]);
            Double y = Double.parseDouble(cords[2]);
            Double z = Double.parseDouble(cords[3]);
            Location location = new Location(world, x, y, z);
            return location;
        } catch (NumberFormatException ignored) {
            return null;
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
        String guiName = plugin.getConfig().getString("settings.guiTitle")
                .replace("$entity", otherPlayer.getName())
                .replace("$player", otherPlayer.getName())
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
            if (graveBlock == null || graveBlock.equals(Material.AIR)) {
                graveBlock = Material.PLAYER_HEAD;
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
                        .replace("$protect", graveManager.parseProtect(grave))
                        .replace("&", "§");
                if (grave.getExperience() != null && grave.getExperience() > 0) {
                    line = line.replace("$level", graveManager.getLevelFromExp(grave.getExperience()));
                    line = line.replace("$xp", grave.getExperience().toString());
                } else {
                    line = line.replace("$level", "0");
                    line = line.replace("$xp", "0");
                }
                line.replace("$x", String.valueOf(grave.getLocation().getBlockX()))
                        .replace("$y", String.valueOf(grave.getLocation().getBlockY()))
                        .replace("$z", String.valueOf(grave.getLocation().getBlockZ()));
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
            String keyValue = grave.getLocation().getWorld().getName() + "#"
                    + grave.getLocation().getX() + "#" + grave.getLocation().getY() + "#" + grave.getLocation().getZ();
            item.getItemMeta().getPersistentDataContainer().set(key, PersistentDataType.STRING, keyValue);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }
}
