package com.rngservers.graves.commands;

import com.rngservers.graves.Main;
import com.rngservers.graves.data.DataManager;
import com.rngservers.graves.grave.GraveManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Graves implements CommandExecutor {
    private Main plugin;
    private DataManager data;
    private GraveManager graveManager;

    public Graves(Main plugin, DataManager data, GraveManager graveManager) {
        this.plugin = plugin;
        this.data = data;
        this.graveManager = graveManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String version = "1.6";
        String author = "RandomUnknown";

        if (args.length < 1) {
            sender.sendMessage(
                    ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "Graves " + ChatColor.GRAY + "v" + version);
            sender.sendMessage(
                    ChatColor.GRAY + "/graves " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Plugin info");
            if (sender.hasPermission("graves.cleanup")) {
                sender.sendMessage(ChatColor.GRAY + "/graves cleanup " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                        + " Remove all holograms");
            }
            if (sender.hasPermission("graves.reload")) {
                sender.sendMessage(ChatColor.GRAY + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                        + " Reload plugin");
            }
            sender.sendMessage(ChatColor.DARK_GRAY + "Author: " + ChatColor.GRAY + author);
            return true;
        }
        if (args[0].equals("cleanup")) {
            if (!sender.hasPermission("graves.cleanup")) {
                sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "]"
                        + ChatColor.RESET + " No Permission!");
                return true;
            }
            Integer count = graveManager.cleanupHolograms();
            sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "]"
                    + ChatColor.RESET + " Removed " + ChatColor.GOLD + count.toString() + ChatColor.WHITE + " holograms!");
        }
        if (args[0].equals("reload")) {
            if (!sender.hasPermission("graves.reload")) {
                sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "]"
                        + ChatColor.RESET + " No Permission!");
                return true;
            }
            plugin.reloadConfig();
            data.graveReplaceLoad();
            graveManager.graveHeadLoad();
            graveManager.hologramLinesLoad();
            sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "]"
                    + ChatColor.RESET + " Reloaded config file!");
        }
        return true;
    }
}
