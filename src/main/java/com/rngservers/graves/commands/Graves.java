package com.rngservers.graves.commands;

import com.rngservers.graves.Main;
import com.rngservers.graves.data.DataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Graves implements CommandExecutor {
    private Main plugin;
    private DataManager data;

    public Graves(Main plugin, DataManager data) {
        this.plugin = plugin;
        this.data = data;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String version = "1.2";
        String author = "RandomUnknown";

        if (args.length < 1) {
            sender.sendMessage(
                    ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "Graves " + ChatColor.GRAY + "v" + version);
            sender.sendMessage(
                    ChatColor.GRAY + "/dc " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Plugin info");
            if (sender.hasPermission("graves.reload")) {
                sender.sendMessage(ChatColor.GRAY + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                        + " Reload plugin");
            }
            sender.sendMessage(ChatColor.DARK_GRAY + "Author: " + ChatColor.GRAY + author);
            return true;
        }
        if (args[0].equals("reload")) {
            if (!sender.hasPermission("graves.reload")) {
                sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "]"
                        + ChatColor.RESET + " No Permission!");
                return true;
            }
            plugin.reloadConfig();
            data.graveReplaceLoad();
            sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Graves" + ChatColor.DARK_GRAY + "]"
                    + ChatColor.RESET + " Reloaded config file!");
        }
        return true;
    }
}
