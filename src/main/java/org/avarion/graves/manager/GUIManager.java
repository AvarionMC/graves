package org.avarion.graves.manager;

import org.avarion.graves.Graves;
import org.avarion.graves.inventory.GraveList;
import org.avarion.graves.inventory.GraveMenu;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.InventoryUtil;
import org.avarion.graves.util.StringUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    public void openGraveList(Entity entity, @NotNull Entity entity2) {
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
                        plugin.getGUIManager()
                              .setGraveListItems(inventory, ((GraveList) inventory.getHolder()).getUUID());
                    }
                    else if (inventory.getHolder() instanceof GraveMenu) {
                        plugin.getGUIManager()
                              .setGraveMenuItems(inventory, ((GraveMenu) inventory.getHolder()).getGrave());
                    }
                }
            }
        }
    }

    public void openGraveList(Entity entity, UUID uuid, boolean sound) {
        if (entity instanceof Player player) {
            List<String> permissionList = plugin.getPermissionList(player);
            List<Grave> playerGraveList = plugin.getGraveManager().getGraveList(uuid);

            if (!playerGraveList.isEmpty()) {
                GraveList graveList = new GraveList(uuid, playerGraveList);
                Inventory inventory = plugin.getServer()
                                            .createInventory(graveList, InventoryUtil.getInventorySize(playerGraveList.size()), StringUtil.parseString(plugin.getConfigString("gui.menu.list.title", player, permissionList, "Graves Main Menu"), player, plugin));

                setGraveListItems(inventory, playerGraveList);
                graveList.setInventory(inventory);
                player.openInventory(graveList.getInventory());

                if (sound) {
                    plugin.getEntityManager().playPlayerSound("sound.menu-open", player, permissionList);
                }
            }
            else {
                plugin.getEntityManager().sendMessage("message.empty", player, permissionList);
            }
        }
    }

    public void setGraveListItems(Inventory inventory, UUID uuid) {
        setGraveListItems(inventory, plugin.getGraveManager().getGraveList(uuid));
    }

    public void setGraveListItems(@NotNull Inventory inventory, @NotNull List<Grave> graveList) {
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
        if (entity instanceof Player player) {
            GraveMenu graveMenu = new GraveMenu(grave);
            String title = StringUtil.parseString(plugin.getConfigString("gui.menu.grave.title", player, grave.getPermissionList(), "Grave"), player, plugin);
            Inventory inventory = plugin.getServer()
                                        .createInventory(graveMenu, InventoryUtil.getInventorySize(5), title);

            setGraveMenuItems(inventory, grave);
            graveMenu.setInventory(inventory);
            player.openInventory(graveMenu.getInventory());

            if (sound) {
                plugin.getEntityManager().playPlayerSound("sound.menu-open", player, grave);
            }
        }
    }

    public void setGraveMenuItems(@NotNull Inventory inventory, Grave grave) {
        inventory.clear();

        ConfigurationSection configurationSection = plugin.getConfigSection("gui.menu.grave.slot", grave);
        if (configurationSection == null) {
            return;
        }

        Map<Integer, Integer> slotMapping = buildSlotMapping(configurationSection);

        for (Map.Entry<Integer, Integer> entry : slotMapping.entrySet()) {
            int configKey = entry.getKey();
            int actualSlot = entry.getValue();
            inventory.setItem(actualSlot, plugin.getItemStackManager().createGraveMenuItemStack(configKey, grave));
        }
    }

    static public boolean reportedInvalidSlots = false;

    private @NotNull Map<Integer, Integer> buildSlotMapping(@NotNull ConfigurationSection configurationSection) {
        Map<Integer, Integer> mapping = new HashMap<>();
        Set<Integer> usedSlots = new HashSet<>();

        for (String slotKey : configurationSection.getKeys(false)) {
            String message = null;

            try {
                int configuredSlot = Integer.parseInt(slotKey);

                if (configuredSlot < 0 || configuredSlot >= 9) {
                    message = "  - Slot " + configuredSlot + " is out of bounds (must be 0-8). Skipping.";
                }
                else if (usedSlots.contains(configuredSlot)) {
                    message = "  - Slot " + configuredSlot + " is already used. Skipping.";
                }
                else {
                    mapping.put(configuredSlot, configuredSlot);
                    usedSlots.add(configuredSlot);
                }
            }
            catch (NumberFormatException exception) {
                message = "  - Slot key '" + slotKey + "' is not an integer. Skipping.";
            }

            if (message != null && !reportedInvalidSlots) {
                plugin.getLogger()
                      .warning("Invalid slot configurations found in gui.menu.grave.slot. Check your config.");
                plugin.getLogger().warning(message);
            }
        }

        reportedInvalidSlots = true;
        return mapping;
    }
}
