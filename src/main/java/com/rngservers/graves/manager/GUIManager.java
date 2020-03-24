package com.rngservers.graves.manager;

import com.rngservers.graves.Graves;
import com.rngservers.graves.hooks.VaultHook;
import com.rngservers.graves.inventory.GraveInventory;
import com.rngservers.graves.inventory.GraveListInventory;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class GUIManager {
    private Graves plugin;
    private GraveManager graveManager;
    private VaultHook vaultHook;

    public GUIManager(Graves plugin, GraveManager graveManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.graveManager = graveManager;
        this.vaultHook = vaultHook;
    }

    public void teleportGrave(Player player, ItemStack item) {
        Double graveTeleportCost = graveManager.getTeleportCost(player);
        UUID graveOwnerUUID = UUID.fromString(item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "graveOwner"), PersistentDataType.STRING));
        if (vaultHook != null && player.getUniqueId().equals(graveOwnerUUID)) {
            Double balance = vaultHook.getEconomy().getBalance(player);
            if (balance < graveTeleportCost) {
                String notEnoughMoneyMessage = plugin.getConfig().getString("settings.notEnoughMoneyMessage")
                        .replace("$money", graveTeleportCost.toString()).replace("&", "§");
                if (!notEnoughMoneyMessage.equals("")) {
                    player.sendMessage(notEnoughMoneyMessage);
                }
                return;
            } else {
                vaultHook.getEconomy().withdrawPlayer(player, graveTeleportCost);
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
        NamespacedKey key = new NamespacedKey(plugin, "graveLocation");
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
        GraveListInventory gui = new GraveListInventory(guiName, graveItems, GraveManager.getInventorySize(graveItems.size()));
        gui.openInventory(player);
    }

    @SuppressWarnings("deprecation")
    public void addSkullItemStackTexture(ItemStack item, String base64) {
        if (!base64.equals("")) {
            UUID hashAsId = new UUID(base64.hashCode(), base64.hashCode());
            plugin.getServer().getUnsafe().modifyItemStack(item, "{SkullOwner:{Id:\"" + hashAsId + "\",Properties:{textures:[{Value:\"" + base64 + "\"}]}}}");
        }
    }

    public List<ItemStack> graveItems(OfflinePlayer player) {
        ConcurrentMap<Location, GraveInventory> graves = graveManager.getGraves(player);
        List<ItemStack> items = new ArrayList<>();

        for (ConcurrentMap.Entry<Location, GraveInventory> entry : graves.entrySet()) {
            GraveInventory grave = entry.getValue();
            Material graveBlock = Material.matchMaterial(plugin.getConfig().getString("settings.graveBlock"));
            if (graveBlock == null || graveBlock.equals(Material.AIR)) {
                graveBlock = Material.PLAYER_HEAD;
            }
            ItemStack item = new ItemStack(graveBlock, 1);
            String graveHeadSkin = plugin.getConfig().getString("settings.graveHeadSkin");
            addSkullItemStackTexture(item, graveHeadSkin);
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta) meta;
                if (graveHeadSkin.equals("$entity") || graveHeadSkin.equals("")) {
                    if (grave.getPlayer() != null) {
                        skull.setOwningPlayer(grave.getPlayer());
                    }
                } else if (graveHeadSkin.length() <= 16) {
                    if (graveManager.getGraveHead() != null) {
                        skull.setOwningPlayer(graveManager.getGraveHead());
                    }
                }
                item.setItemMeta(skull);
            }
            meta = item.getItemMeta();
            List<String> lores = new ArrayList<>();
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
            String locationValue = grave.getLocation().getWorld().getName() + "#"
                    + grave.getLocation().getX() + "#" + grave.getLocation().getY() + "#" + grave.getLocation().getZ();
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "graveLocation"), PersistentDataType.STRING, locationValue);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "graveOwner"), PersistentDataType.STRING, grave.getPlayer().getUniqueId().toString());
            item.setItemMeta(meta);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }
}
