package com.rngservers.graves.commands;

import com.rngservers.graves.Main;
import com.rngservers.graves.data.DataManager;
import com.rngservers.graves.grave.GraveManager;
import com.rngservers.graves.grave.Messages;
import com.rngservers.graves.gui.GUIManager;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Graves implements CommandExecutor {
    private Main plugin;
    private DataManager data;
    private GraveManager graveManager;
    private GUIManager guiManager;
    private Messages messages;

    public Graves(Main plugin, DataManager data, GraveManager graveManager, GUIManager guiManager, Messages messages) {
        this.plugin = plugin;
        this.data = data;
        this.graveManager = graveManager;
        this.guiManager = guiManager;
        this.messages = messages;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("graves.gui")) {
                    guiManager.openGraveGUI(player);
                } else {
                    messages.permissionDenied(sender);
                }
            } else {
                help(sender);
            }
            return true;
        }
        if (args[0].equals("list")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length == 1) {
                    if (player.hasPermission("graves.gui")) {
                        guiManager.openGraveGUI(player);
                    } else {
                        messages.permissionDenied(sender);
                    }
                }
                if (args.length == 2) {
                    if (player.hasPermission("graves.gui.other")) {
                        OfflinePlayer otherPlayer = plugin.getServer().getOfflinePlayer(args[1]);
                        if (otherPlayer != null) {
                            guiManager.openGraveGUI(player, otherPlayer);
                        }
                    } else {
                        messages.permissionDenied(sender);
                    }
                }
            } else {
                help(sender);
            }
            return true;
        }
        if (args[0].equals("help")) {
            help(sender);
            return true;
        }
        if (args[0].equals("cleanup")) {
            if (!sender.hasPermission("graves.cleanup")) {
                messages.permissionDenied(sender);
                return true;
            }
            Integer count = graveManager.cleanupHolograms();
            sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "]"
                    + ChatColor.RESET + " Removed " + ChatColor.GOLD + count.toString() + ChatColor.WHITE + " holograms!");
        }
        if (args[0].equals("reload")) {
            if (!sender.hasPermission("graves.reload")) {
                messages.permissionDenied(sender);
                return true;
            }
            plugin.reloadConfig();
            data.graveReplaceLoad();
            graveManager.graveHeadLoad();
            graveManager.graveIgnoreLoad();
            graveManager.hologramLinesLoad();
            sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "]"
                    + ChatColor.RESET + " Reloaded config file!");
        }
        return true;
    }

    public void help(CommandSender sender) {
        String version = "2.2";
        String author = "RandomUnknown";

        sender.sendMessage(
                ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "Graves " + ChatColor.GRAY + "v" + version);
        if (sender.hasPermission("graves.gui")) {
            sender.sendMessage(
                    ChatColor.GRAY + "/graves " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Player graves");
        }
        if (sender.hasPermission("graves.gui")) {
            if (sender.hasPermission("graves.gui.other")) {
                sender.sendMessage(
                        ChatColor.GRAY + "/graves list {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Player graves");
            } else {
                sender.sendMessage(
                        ChatColor.GRAY + "/graves list " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Player graves");
            }
        }
        if (sender.hasPermission("graves.cleanup")) {
            sender.sendMessage(ChatColor.GRAY + "/graves cleanup " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Remove all holograms");
        }
        if (sender.hasPermission("graves.reload")) {
            sender.sendMessage(ChatColor.GRAY + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Reload plugin");
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "Author: " + ChatColor.GRAY + author);
    }
}
