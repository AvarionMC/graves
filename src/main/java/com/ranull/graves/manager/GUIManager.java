package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.GraveList;
import com.ranull.graves.inventory.GraveMenu;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.StringUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

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

    @SuppressWarnings("ConstantConditions")
    public void refreshMenus() {
        if (plugin.isEnabled()) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getOpenInventory() != null) { // Mohist, might return null even when Bukkit shouldn't.
                    Inventory inventory = player.getOpenInventory().getTopInventory();

                    if (inventory.getHolder() instanceof GraveList) {
                        plugin.getGUIManager().setGraveListItems(inventory,
                                ((GraveList) inventory.getHolder()).getUUID());
                    } else if (inventory.getHolder() instanceof GraveMenu) {
                        plugin.getGUIManager().setGraveMenuItems(inventory,
                                ((GraveMenu) inventory.getHolder()).getGrave());
                    }
                }
            }
        }
    }

    public void openGraveList(Entity entity, UUID uuid, boolean sound) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            List<String> permissionList = plugin.getPermissionList(player);
            List<Grave> playerGraveList = plugin.getGraveManager().getGraveList(uuid);

            if (!playerGraveList.isEmpty()) {
                GraveList graveList = new GraveList(uuid, playerGraveList);
                Inventory inventory = plugin.getServer().createInventory(graveList,
                        InventoryUtil.getInventorySize(playerGraveList.size()),
                        StringUtil.parseString(plugin.getConfig("gui.menu.list.title", player, permissionList)
                                .getString("gui.menu.list.title", "Graves Main Menu"), player, plugin));

                setGraveListItems(inventory, playerGraveList);
                graveList.setInventory(inventory);
                player.openInventory(graveList.getInventory());

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
            inventory.addItem(plugin.getItemStackManager().createGraveListItemStack(count, grave));
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

                    inventory.setItem(slot, plugin.getItemStackManager().createGraveMenuItemStack(slot, grave));
                } catch (NumberFormatException exception) {
                    plugin.debugMessage(string + " is not an int", 1);
                }
            }
        }
    }
}
