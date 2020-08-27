package com.ranull.graves.manager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ranull.graves.Graves;
import com.ranull.graves.hooks.VaultHook;
import com.ranull.graves.inventory.GraveInventory;
import com.ranull.graves.inventory.GraveListInventory;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public void teleportGrave(Player player, ItemStack itemStack) {
        double teleportCost = graveManager.getTeleportCost(player);

        if (vaultHook != null && player.getUniqueId()
                .equals(UUID.fromString(Objects.requireNonNull(Objects.requireNonNull(itemStack.getItemMeta())
                        .getPersistentDataContainer().get(new NamespacedKey(plugin, "graveOwner"),
                                PersistentDataType.STRING))))) {
            double playerBalance = vaultHook.getEconomy().getBalance(player);

            if (playerBalance < teleportCost) {
                String notEnoughMoneyMessage = Objects.requireNonNull(plugin.getConfig()
                        .getString("settings.notEnoughMoneyMessage"))
                        .replace("$money", String.valueOf(teleportCost))
                        .replace("&", "§");

                if (!notEnoughMoneyMessage.equals("")) {
                    player.sendMessage(notEnoughMoneyMessage);
                }

                return;
            } else {
                vaultHook.getEconomy().withdrawPlayer(player, teleportCost);
            }
        }

        Location graveLocation = getGraveLocation(itemStack);

        if (graveLocation != null) {
            Location graveTeleportLocation = graveManager.getTeleportLocation(player, graveLocation);

            if (graveTeleportLocation != null) {
                player.teleport(graveTeleportLocation);

                GraveInventory graveInventory = graveManager.getGraveInventory(new Location(graveLocation.getWorld(),
                        graveLocation.getX(), graveLocation.getY() - 1, graveLocation.getZ()));

                if (graveInventory != null) {
                    graveManager.runTeleportCommands(graveInventory, player);
                }

                String teleportMessage = Objects.requireNonNull(plugin.getConfig()
                        .getString("settings.teleportMessage"))
                        .replace("$money", String.valueOf(teleportCost))
                        .replace("&", "§");

                if (!teleportMessage.equals("")) {
                    player.sendMessage(teleportMessage);
                }

                String teleportSound = plugin.getConfig().getString("settings.teleportSound");

                if (teleportSound != null && !teleportSound.equals("")) {
                    player.getWorld().playSound(player.getLocation(),
                            Sound.valueOf(teleportSound.toUpperCase()), 1.0F, 1.0F);
                }
            } else {
                String teleportFailedMessage = Objects.requireNonNull(plugin.getConfig()
                        .getString("settings.teleportFailedMessage"))
                        .replace("$money", String.valueOf(teleportCost))
                        .replace("&", "§");

                if (!teleportFailedMessage.equals("")) {
                    player.sendMessage(teleportFailedMessage);
                }
            }
        }
    }

    public Location getGraveLocation(ItemStack itemStack) {
        NamespacedKey key = new NamespacedKey(plugin, "graveLocation");

        String[] cords = Objects.requireNonNull(Objects.requireNonNull(itemStack.getItemMeta()).getPersistentDataContainer()
                .get(key, PersistentDataType.STRING)).split("#");

        try {
            World world = plugin.getServer().getWorld(cords[0]);

            double x = Double.parseDouble(cords[1]);
            double y = Double.parseDouble(cords[2]);
            double z = Double.parseDouble(cords[3]);

            return new Location(world, x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void openGraveGUI(Player player) {
        openGraveGUI(player, player);
    }

    public void openGraveGUI(Player player, OfflinePlayer otherPlayer) {
        List<ItemStack> graveItems = getGraveListItems(otherPlayer);

        if (graveItems.size() == 0) {
            if (player.equals(otherPlayer)) {
                String guiEmpty = Objects.requireNonNull(plugin.getConfig().getString("settings.guiEmpty"))
                        .replace("&", "§");

                if (!guiEmpty.equals("")) {
                    player.sendMessage(guiEmpty);
                }
            } else {
                player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Graves" + ChatColor.GRAY + "] "
                        + ChatColor.GRAY + otherPlayer.getName() + ChatColor.RESET + " does not have any graves!");
            }

            return;
        }

        String guiName = Objects.requireNonNull(plugin.getConfig().getString("settings.guiTitle"))
                .replace("$entity", Objects.requireNonNull(otherPlayer.getName()))
                .replace("$player", otherPlayer.getName())
                .replace("&", "§");

        GraveListInventory graveListInventory = new GraveListInventory(guiName, graveItems,
                GraveManager.getInventorySize(graveItems.size()));

        player.openInventory(graveListInventory.getInventory());
    }

    public List<ItemStack> getGraveListItems(OfflinePlayer player) {
        List<ItemStack> itemList = new ArrayList<>();

        for (ConcurrentMap.Entry<Location, GraveInventory> entry :
                graveManager.getPlayerGraves(player.getUniqueId()).entrySet()) {
            GraveInventory graveInventory = entry.getValue();

            Material graveMaterial = Material.matchMaterial(Objects.requireNonNull(
                    plugin.getConfig().getString("settings.block")));

            if (graveMaterial == null || graveMaterial == Material.AIR) {
                graveMaterial = Material.PLAYER_HEAD;
            }

            ItemStack itemStack = new ItemStack(graveMaterial);
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                if (graveMaterial == Material.PLAYER_HEAD && itemMeta instanceof SkullMeta) {
                    String headSkin = plugin.getConfig().getString("settings.headSkin");

                    if (headSkin != null) {
                        int headSkinType = plugin.getConfig().getInt("settings.headSkinType");

                        SkullMeta skullMeta = (SkullMeta) itemMeta;

                        if (headSkinType == 0) {
                            if (graveInventory.getPlayer() != null) {
                                skullMeta.setOwningPlayer(graveInventory.getPlayer());
                                itemStack.setItemMeta(skullMeta);
                            }
                        } else if (headSkinType == 1) {
                            addSkullItemStackTexture(itemStack, headSkin);
                            itemMeta = itemStack.getItemMeta();
                        } else if (headSkinType == 2) {
                            if (graveManager.getGraveHead() != null && headSkin.length() <= 16) {
                                skullMeta.setOwningPlayer(graveManager.getGraveHead());
                                itemStack.setItemMeta(skullMeta);
                            }
                        }
                    }
                }

                List<String> loreList = new ArrayList<>();
                List<String> loreLines = plugin.getConfig().getStringList("settings.guiLore");

                for (String lore : loreLines) {
                    String line = ChatColor.GRAY + lore.replace("$location", "LOC")
                            .replace("$item", String.valueOf(graveInventory.getItemAmount()))
                            .replace("$protect", graveManager.parseProtect(graveInventory))
                            .replace("&", "§");

                    if (graveInventory.getExperience() > 0) {
                        line = line.replace("$level", graveManager.getLevelFromExp(
                                graveInventory.getExperience()));
                        line = line.replace("$xp", String.valueOf(graveInventory.getExperience()));
                    } else {
                        line = line.replace("$level", "0");
                        line = line.replace("$xp", "0");
                    }

                    line = line.replace("$world", graveInventory.getLocation().getWorld().getName())
                            .replace("$x", String.valueOf(graveInventory.getLocation().getBlockX()))
                            .replace("$y", String.valueOf(graveInventory.getLocation().getBlockY()))
                            .replace("$z", String.valueOf(graveInventory.getLocation().getBlockZ()));

                    loreList.add(line);
                }

                String guiGrave = Objects.requireNonNull(plugin.getConfig().getString("settings.guiGrave"))
                        .replace("$world", graveInventory.getLocation().getWorld().getName())
                        .replace("$x", String.valueOf(graveInventory.getLocation().getBlockX()))
                        .replace("$y", String.valueOf(graveInventory.getLocation().getBlockY()))
                        .replace("$z", String.valueOf(graveInventory.getLocation().getBlockZ()))
                        .replace("&", "§");

                itemMeta.setDisplayName(guiGrave);
                itemMeta.setLore(loreList);

                String locationValue = Objects.requireNonNull(graveInventory.getLocation().getWorld()).getName() + "#"
                        + graveInventory.getLocation().getX() + "#" + graveInventory.getLocation().getY() +
                        "#" + graveInventory.getLocation().getZ();

                itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "graveLocation"),
                        PersistentDataType.STRING, locationValue);

                itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "graveOwner"),
                        PersistentDataType.STRING, graveInventory.getPlayer().getUniqueId().toString());

                itemStack.setItemMeta(itemMeta);
            }

            itemList.add(itemStack);
        }

        return itemList;
    }

    public static ItemStack addSkullItemStackTexture(ItemStack itemStack, String base64) {
        if (itemStack.getType() != Material.PLAYER_HEAD) {
            return itemStack;
        }
        
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

        GameProfile profile = new GameProfile(UUID.randomUUID(), null);

        profile.getProperties().put("textures", new Property("textures", base64));

        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");

            profileField.setAccessible(true);

            profileField.set(skullMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException exception) {
            exception.printStackTrace();
        }

        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }
}