package com.ranull.graves.command;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GravesCommand implements CommandExecutor, TabCompleter {
    private final Graves plugin;

    public GravesCommand(Graves plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
                             @NotNull String string, String[] args) {
        if (args.length < 1) {
            if (commandSender.hasPermission("graves.gui")) {
                if (commandSender instanceof Player) {
                    plugin.getGUIManager().openGraveList((Player) commandSender);
                } else {
                    sendHelpMenu(commandSender);
                }
            } else {
                plugin.getPlayerManager().sendMessage("message.permission-denied", commandSender);
            }
        } else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gui")) {
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;

                if (args.length == 1) {
                    if (player.hasPermission("graves.gui")) {
                        plugin.getGUIManager().openGraveList(player);
                    } else {
                        plugin.getPlayerManager().sendMessage("message.permission-denied", player);
                    }
                } else if (args.length == 2) {
                    if (player.hasPermission("graves.gui.other")) {
                        OfflinePlayer otherPlayer = plugin.getServer().getOfflinePlayer(args[1]);

                        if (!plugin.getGraveManager().getGraveList(otherPlayer).isEmpty()) {
                            plugin.getGUIManager().openGraveList(player, otherPlayer.getUniqueId());
                        } else {
                            commandSender.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » "
                                    + ChatColor.RESET + ChatColor.GRAY + args[1] + ChatColor.RESET + " has no graves.");
                        }
                    } else {
                        plugin.getPlayerManager().sendMessage("message.permission-denied", player);
                    }
                }
            } else {
                sendHelpMenu(commandSender);
            }
        } else if (args[0].equalsIgnoreCase("help")) {
            sendHelpMenu(commandSender);
        } else if (plugin.getRecipeManager() != null && args[0].equalsIgnoreCase("givetoken")) {
            if (commandSender.hasPermission("graves.givetoken")) {
                if (args.length == 1) {
                    commandSender.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + ChatColor.RESET + "/graves givetoken {player} {token}");
                } else if (args.length == 2) {
                    if (commandSender instanceof Player) {
                        Player player = (Player) commandSender;
                        ItemStack itemStack = plugin.getRecipeManager().getToken(args[1].toLowerCase());

                        if (itemStack != null) {
                            plugin.getPlayerManager().sendMessage("message.give-token", player);
                            player.getInventory().addItem(itemStack);
                        } else {
                            player.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Token " + args[1] + " not found.");
                        }
                    } else {
                        commandSender.sendMessage("Only players can run this command, " +
                                "try /graves givetoken {name} {token}");
                    }
                } else if (args.length == 3) {
                    Player player = plugin.getServer().getPlayer(args[1]);

                    if (player != null) {
                        ItemStack itemStack = plugin.getRecipeManager().getToken(args[2].toLowerCase());

                        if (itemStack != null) {
                            plugin.getPlayerManager().sendMessage("message.give-token", player);
                            player.getInventory().addItem(itemStack);
                        } else {
                            player.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Token " + args[2] + " not found.");
                        }
                    } else {
                        commandSender.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Player " + args[1] + " not found.");
                    }
                } else if (args.length == 4) {
                    Player player = plugin.getServer().getPlayer(args[1]);

                    if (player != null) {
                        ItemStack itemStack = plugin.getRecipeManager().getToken(args[2].toLowerCase());

                        if (itemStack != null) {
                            try {
                                int amount = Integer.parseInt(args[3]);
                                int count = 0;

                                while (count < amount) {
                                    player.getInventory().addItem(itemStack);
                                    count++;
                                }
                            } catch (NumberFormatException ignored) {
                                player.getInventory().addItem(itemStack);
                            }

                            plugin.getPlayerManager().sendMessage("message.give-token", player);
                        } else {
                            player.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Token " + args[2] + " not found.");
                        }
                    } else {
                        commandSender.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Player " + args[1] + " not found.");
                    }
                }
            } else if (commandSender instanceof Player) {
                plugin.getPlayerManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (commandSender.hasPermission("graves.reload")) {
                plugin.reload();
                commandSender.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Reloaded config file.");
            } else if (commandSender instanceof Player) {
                plugin.getPlayerManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        } else if (args[0].equalsIgnoreCase("purge")) {
            if (commandSender.hasPermission("graves.purge")) {
                List<Grave> graveList = new ArrayList<>(plugin.getDataManager().getGraveMap().values());

                for (Grave grave : graveList) {
                    plugin.getGraveManager().removeGrave(grave);
                }

                commandSender.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + graveList.size() + " Graves purged.");
            } else if (commandSender instanceof Player) {
                plugin.getPlayerManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        }

        return true;
    }

    public void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Graves "
                + ChatColor.DARK_GRAY + "v" + plugin.getDescription().getVersion());

        if (sender.hasPermission("graves.gui")) {
            sender.sendMessage(
                    ChatColor.GRAY + "/graves " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Graves GUI");
        }

        if (sender.hasPermission("graves.gui")) {
            if (sender.hasPermission("graves.gui.other")) {
                sender.sendMessage(
                        ChatColor.GRAY + "/graves list {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET +
                                " View player graves");
            } else {
                sender.sendMessage(
                        ChatColor.GRAY + "/graves list " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET +
                                " Player graves");
            }
        }

        if (sender.hasPermission("graves.givetoken")) {
            if (plugin.getConfig().getBoolean("settings.token")) {
                sender.sendMessage(ChatColor.GRAY + "/graves givetoken {player} {amount} " + ChatColor.DARK_GRAY + "-"
                        + ChatColor.RESET + " Give grave token");
            }
        }

        if (sender.hasPermission("graves.reload")) {
            sender.sendMessage(ChatColor.GRAY + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Reload plugin");
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "Author: " + ChatColor.GRAY + "Ranull");
    }

    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                      @NotNull String string, @NotNull String @NotNull [] args) {
        List<String> stringList = new ArrayList<>();

        if (args.length == 1) {
            stringList.add("help");

            if (commandSender.hasPermission("graves.gui")) {
                stringList.add("list");
                stringList.add("gui");
            }

            if (commandSender.hasPermission("graves.reload")) {
                stringList.add("reload");
            }

            if (commandSender.hasPermission("graves.reload")) {
                stringList.add("reload");
            }

            if (plugin.getRecipeManager() != null && commandSender.hasPermission("graves.givetoken")) {
                stringList.add("givetoken");
            }
        } else if (args.length > 1) {
            if ((args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gui"))
                    && commandSender.hasPermission("graves.gui.other")) {
                plugin.getServer().getOnlinePlayers().forEach((player -> stringList.add(player.getName())));
            } else if (plugin.getRecipeManager() != null && args[0].equalsIgnoreCase("givetoken")
                    && commandSender.hasPermission("graves.givetoken")) {
                if (args.length == 2) {
                    plugin.getServer().getOnlinePlayers().forEach((player -> stringList.add(player.getName())));
                } else if (args.length == 3) {
                    stringList.addAll(plugin.getRecipeManager().getTokenList());
                }
            }
        }

        return stringList;
    }
}
