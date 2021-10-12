package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.*;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.NumberConversions;

import java.util.*;

public final class PlayerManager {
    private final Graves plugin;

    public PlayerManager(Graves plugin) {
        this.plugin = plugin;
    }

    public void swingMainHand(Player player) {
        if (plugin.getVersionManager().hasSwingHand()) {
            player.swingMainHand();
        } else {
            ReflectionUtil.swingMainHand(player);
        }
    }

    public ItemStack getCompassItemStack(Location location, Grave grave) {
        if (plugin.getVersionManager().hasPersistentData() && plugin.getVersionManager().hasLodestone()) {
            ItemStack itemStack = new ItemStack(Material.COMPASS);
            CompassMeta compassMeta = (CompassMeta) itemStack.getItemMeta();

            if (compassMeta != null) {
                List<String> loreList = new ArrayList<>();

                compassMeta.setLodestoneTracked(false);
                compassMeta.setLodestone(location);
                compassMeta.setDisplayName(ChatColor.RESET + StringUtil.parseString(plugin
                        .getConfig("compass.name", grave).getString("compass.name"), grave, plugin));
                compassMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "graveUUID"),
                        PersistentDataType.STRING, grave.getUUID().toString());

                for (String string : plugin.getConfig("compass.lore", grave).getStringList("compass.lore")) {
                    loreList.add(ChatColor.GRAY + StringUtil.parseString(string, location, grave, plugin));
                }

                compassMeta.setLore(loreList);
                itemStack.setItemMeta(compassMeta);
            }

            return itemStack;
        }

        return null;
    }

    public Map<ItemStack, UUID> getCompassFromInventory(HumanEntity player) {
        if (plugin.getVersionManager().hasPersistentData() && plugin.getVersionManager().hasLodestone()) {
            for (ItemStack itemStack : player.getInventory().getContents()) {
                UUID uuid = getUUIDFromItemStack(itemStack);

                if (uuid != null) {
                    return Collections.singletonMap(itemStack, uuid);
                }
            }
        }

        return null;
    }

    public UUID getUUIDFromItemStack(ItemStack itemStack) {
        if (plugin.getVersionManager().hasPersistentData() && itemStack != null && itemStack.getItemMeta() != null) {
            if (itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "graveUUID"),
                    PersistentDataType.STRING)) {
                return UUIDUtil.getUUID(itemStack.getItemMeta().getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "graveUUID"), PersistentDataType.STRING));
            }
        }

        return null;
    }

    public void teleportPlayer(Location location, Player player, Grave grave) {
        if (!plugin.hasWorldGuard() || plugin.getWorldGuard().canTeleport(player, location)) {
            location = LocationUtil.roundLocation(location);
            BlockFace blockFace = BlockFaceUtil.getYawBlockFace(grave.getYaw());
            Location locationTeleport = location.clone().getBlock().getRelative(blockFace).getRelative(blockFace)
                    .getLocation().add(0.5, 0, 0.5);

            if (plugin.getLocationManager().isLocationSafePlayer(locationTeleport)) {
                locationTeleport.setYaw(BlockFaceUtil.getBlockFaceYaw(blockFace.getOppositeFace()));
                locationTeleport.setPitch(20);
            } else {
                locationTeleport = plugin.getLocationManager()
                        .getSafeTeleportLocation(player, location.add(0, 1, 0), grave, plugin);

                if (locationTeleport != null) {
                    locationTeleport.add(0.5, 0, 0.5);
                    locationTeleport.setYaw(BlockFaceUtil.getBlockFaceYaw(blockFace));
                    locationTeleport.setPitch(90);
                }
            }

            if (locationTeleport != null && locationTeleport.getWorld() != null) {
                if (plugin.hasVault()) {
                    double teleportCost = getTeleportCost(player.getLocation(), locationTeleport, grave);

                    if (plugin.getVault().hasBalance(player, teleportCost)
                            && plugin.getVault().withdrawBalance(player, teleportCost)) {
                        player.teleport(locationTeleport);
                        plugin.getPlayerManager().sendMessage("message.teleport", player, locationTeleport, grave);
                        plugin.getPlayerManager().playPlayerSound("sound.teleport", player, locationTeleport, grave);
                    } else {
                        plugin.getPlayerManager().sendMessage("message.no-money", player, player.getLocation(), grave);
                    }
                } else {
                    player.teleport(locationTeleport);
                }
            }
        } else {
            plugin.getPlayerManager().sendMessage("message.worldguard-teleport-deny", player, location, grave);
        }
    }

    public double getTeleportCost(Location location1, Location location2, Grave grave) {
        double cost = plugin.getConfig("teleport.cost", grave).getDouble("teleport.cost");
        double costDifferentWorld = plugin.getConfig("teleport.cost-different-world", grave)
                .getDouble("teleport.cost-different-world");

        if (plugin.getConfig("teleport.cost-distance-increase", grave)
                .getBoolean("teleport.cost-distance-increase")) {
            double distance = Math.sqrt(NumberConversions.square(location1.getBlockX() - location2.getBlockX())
                    + NumberConversions.square(location1.getBlockZ() - location2.getBlockZ()));
            cost = Math.round(cost * (distance / 16));
        }

        if (location1.getWorld() != null && location2.getWorld() != null && costDifferentWorld > 0
                && !location1.getWorld().getName().equals(location2.getWorld().getName())) {
            cost += costDifferentWorld;
        }

        return cost;
    }

    public void playWorldSound(String string, Player player) {
        playWorldSound(string, player.getLocation(), null);
    }

    public void playWorldSound(String string, Player player, Grave grave) {
        playWorldSound(string, player.getLocation(), grave);
    }

    public void playWorldSound(String string, Location location, Grave grave) {
        playWorldSound(string, location, grave != null ? grave.getOwnerType() : null, grave != null
                ? grave.getPermissionList() : null, 1, 1);
    }

    public void playWorldSound(String string, Location location, EntityType entityType, List<String> permissionList,
                               float volume, float pitch) {
        if (location.getWorld() != null) {
            string = plugin.getConfig(string, entityType, permissionList).getString(string);

            if (string != null && !string.equals("")) {
                try {
                    location.getWorld().playSound(location, Sound.valueOf(string.toUpperCase()), volume, pitch);
                } catch (IllegalArgumentException exception) {
                    plugin.debugMessage(string.toUpperCase() + " is not a Sound ENUM", 1);
                }
            }
        }
    }

    public void playPlayerSound(String string, Player player, Grave grave) {
        playPlayerSound(string, player, player.getLocation(), grave.getPermissionList(), 1, 1);
    }

    public void playPlayerSound(String string, Player player, Location location, Grave grave) {
        playPlayerSound(string, player, location, grave.getPermissionList(), 1, 1);
    }

    public void playPlayerSound(String string, Player player, List<String> permissionList) {
        playPlayerSound(string, player, player.getLocation(), permissionList, 1, 1);
    }

    public void playPlayerSound(String string, Player player, Location location, List<String> permissionList) {
        playPlayerSound(string, player, location, permissionList, 1, 1);
    }

    public void playPlayerSound(String string, Player player, Location location, List<String> permissionList,
                                float volume, float pitch) {
        if (location.getWorld() != null && player != null) {
            string = plugin.getConfig(string, player, permissionList).getString(string);

            if (string != null && !string.equals("")) {
                try {
                    player.playSound(location, Sound.valueOf(string.toUpperCase()), volume, pitch);
                } catch (IllegalArgumentException exception) {
                    plugin.debugMessage(string.toUpperCase() + " is not a Sound ENUM", 1);
                }
            }
        }
    }

    public void sendMessage(String string, CommandSender commandSender) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            sendMessage(string, player, player.getLocation(), null, plugin.getPermissionList(player));
        }
    }

    public void sendMessage(String string, LivingEntity livingEntity) {
        sendMessage(string, livingEntity, livingEntity.getLocation(), null, plugin.getPermissionList(livingEntity));
    }

    public void sendMessage(String string, LivingEntity livingEntity, List<String> permissionList) {
        sendMessage(string, livingEntity, livingEntity.getLocation(), null, permissionList);
    }


    public void sendMessage(String string, LivingEntity livingEntity, Location location, List<String> permissionList) {
        sendMessage(string, livingEntity, location, null, permissionList);
    }

    public void sendMessage(String string, LivingEntity livingEntity, Location location, Grave grave) {
        sendMessage(string, livingEntity, location, grave, null);
    }

    public void sendMessage(String string, LivingEntity livingEntity, String name, Location location, List<String> permissionList) {
        sendMessage(string, livingEntity, name, location, null, permissionList);
    }

    private void sendMessage(String string, LivingEntity livingEntity, Location location, Grave grave, List<String> permissionList) {
        sendMessage(string, livingEntity, plugin.getEntityManager().getEntityName(livingEntity), location, grave, permissionList);
    }

    private void sendMessage(String string, LivingEntity livingEntity, String name, Location location, Grave grave, List<String> permissionList) {
        if (livingEntity instanceof Player) {
            if (grave != null) {
                string = plugin.getConfig(string, grave).getString(string);
            } else {
                string = plugin.getConfig(string, livingEntity.getType(), permissionList).getString(string);
            }

            String prefix = plugin.getConfig(string, livingEntity.getType(), permissionList)
                    .getString("message.prefix");

            if (prefix != null && !prefix.equals("")) {
                string = prefix + string;
            }

            if (string != null && !string.equals("")) {
                Player player = (Player) livingEntity;

                player.sendMessage(StringUtil.parseString(string, livingEntity, name, location, grave, plugin));
            }
        }
    }

    public void runCommands(String string, Entity entity, Location location, Grave grave) {
        runCommands(string, entity, null, location, grave);
    }

    public void runCommands(String string, String name, Location location, Grave grave) {
        runCommands(string, null, name, location, grave);
    }

    private void runCommands(String string, Entity entity, String name, Location location, Grave grave) {
        for (String command : plugin.getConfig(string, grave).getStringList(string)) {
            runConsoleCommand(StringUtil.parseString(command, entity, name, location, grave, plugin));
        }
    }

    private void runConsoleCommand(String string) {
        if (string != null && !string.equals("")) {
            ServerCommandEvent serverCommandEvent = new ServerCommandEvent(plugin.getServer().getConsoleSender(), string);

            plugin.getServer().getPluginManager().callEvent(serverCommandEvent);

            if ((plugin.getVersionManager().is_v1_7() || plugin.getVersionManager().is_v1_8())
                    || !serverCommandEvent.isCancelled()) {
                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> plugin.getServer()
                        .dispatchCommand(serverCommandEvent.getSender(), serverCommandEvent.getCommand()));
                plugin.debugMessage("Running console command " + string, 1);
            }
        }
    }

    public void runFunction(Player player, String function, Grave grave) {
        switch (function.toLowerCase()) {
            case "list": {
                plugin.getGUIManager().openGraveList(player);

                break;
            }
            case "menu": {
                plugin.getGUIManager().openGraveMenu(player, grave);

                break;
            }
            case "teleport":
            case "teleportation": {
                if (plugin.getConfig("teleport.enabled", grave).getBoolean("teleport.enabled")
                        || player.hasPermission("graves.bypass")) {
                    plugin.getPlayerManager().teleportPlayer(plugin.getGraveManager()
                            .getGraveLocationList(player.getLocation(), grave).get(0), player, grave);
                } else {
                    plugin.getPlayerManager().sendMessage("message.teleport-disabled", player,
                            player.getLocation(), grave);
                }

                break;
            }
            case "protect":
            case "protection": {
                if (grave.getTimeProtectionRemaining() > 0 || grave.getTimeProtectionRemaining() < 0) {
                    plugin.getGraveManager().toggleGraveProtection(grave);
                    plugin.getPlayerManager().playPlayerSound("sound.protection-change", player, grave);
                    plugin.getGUIManager().openGraveMenu(player, grave, false);
                }

                break;
            }
            case "distance": {
                Location location = plugin.getGraveManager().getGraveLocation(player.getLocation(), grave);

                if (location != null) {
                    if (player.getWorld().equals(location.getWorld())) {
                        plugin.getPlayerManager().sendMessage("message.distance", player, location, grave);
                    } else {
                        plugin.getPlayerManager().sendMessage("message.distance-world", player, location, grave);
                    }
                }

                break;
            }
            case "open":
            case "loot":
            case "virtual": {
                double distance = plugin.getConfig("virtual.distance", grave).getDouble("virtual.distance");

                if (distance < 0) {
                    plugin.getGraveManager().openGrave(player, player.getLocation(), grave);
                } else {
                    Location location = plugin.getGraveManager().getGraveLocation(player.getLocation(), grave);

                    if (location != null) {
                        if (player.getLocation().distance(location) <= distance) {
                            plugin.getGraveManager().openGrave(player, player.getLocation(), grave);
                        } else {
                            plugin.getPlayerManager().sendMessage("message.distance-virtual", player, location, grave);
                        }
                    }
                }

                break;
            }
            case "autoloot": {
                plugin.getGraveManager().autoLootGrave(player, player.getLocation(), grave);

                break;
            }
        }
    }

    public boolean canOpenGrave(Player player, Grave grave) {
        if (grave.getTimeProtectionRemaining() == 0 || player.hasPermission("graves.bypass")) {
            return true;
        } else if (grave.getProtection() && grave.getOwnerUUID() != null) {
            if (grave.getOwnerUUID().equals(player.getUniqueId())
                    && plugin.getConfig("protection.open.owner", grave)
                    .getBoolean("protection.open.owner")) {
                return true;
            } else {
                if (grave.getKillerUUID() != null) {
                    if (grave.getKillerUUID().equals(player.getUniqueId())
                            && plugin.getConfig("protection.open.killer", grave)
                            .getBoolean("protection.open.killer")) {
                        return true;
                    } else return !grave.getOwnerUUID().equals(player.getUniqueId())
                            && !grave.getKillerUUID().equals(player.getUniqueId())
                            && plugin.getConfig("protection.open.other", grave)
                            .getBoolean("protection.open.other");
                } else return (grave.getOwnerUUID().equals(player.getUniqueId())
                        && plugin.getConfig("protection.open.missing.owner", grave)
                        .getBoolean("protection.open.missing.owner"))
                        || (!grave.getOwnerUUID().equals(player.getUniqueId())
                        && plugin.getConfig("protection.open.missing.other", grave)
                        .getBoolean("protection.open.missing.other"));
            }
        } else {
            return true;
        }
    }
}
