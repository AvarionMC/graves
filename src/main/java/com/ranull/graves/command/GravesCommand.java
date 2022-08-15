package com.ranull.graves.command;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
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
            if (commandSender instanceof Player) {
                if (commandSender.hasPermission("graves.gui")) {
                    plugin.getGUIManager().openGraveList((Player) commandSender);
                } else {
                    plugin.getEntityManager().sendMessage("message.permission-denied", commandSender);
                }
            } else {
                sendHelpMenu(commandSender);
            }
        } else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gui")) {
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;

                if (args.length == 1) {
                    if (player.hasPermission("graves.gui")) {
                        plugin.getGUIManager().openGraveList(player);
                    } else {
                        plugin.getEntityManager().sendMessage("message.permission-denied", player);
                    }
                } else if (args.length == 2) {
                    if (player.hasPermission("graves.gui.other")) {
                        OfflinePlayer otherPlayer = plugin.getServer().getOfflinePlayer(args[1]);

                        if (!plugin.getGraveManager().getGraveList(otherPlayer).isEmpty()) {
                            plugin.getGUIManager().openGraveList(player, otherPlayer.getUniqueId());
                        } else {
                            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                                    + ChatColor.RESET + ChatColor.RED + args[1] + ChatColor.RESET + " has no graves.");
                        }
                    } else {
                        plugin.getEntityManager().sendMessage("message.permission-denied", player);
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
                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + ChatColor.RESET + "/graves givetoken {player} {token}");
                } else if (args.length == 2) {
                    if (commandSender instanceof Player) {
                        Player player = (Player) commandSender;
                        ItemStack itemStack = plugin.getRecipeManager().getToken(args[1].toLowerCase());

                        if (itemStack != null) {
                            plugin.getEntityManager().sendMessage("message.give-token", player);
                            player.getInventory().addItem(itemStack);
                        } else {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
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
                            plugin.getEntityManager().sendMessage("message.give-token", player);
                            player.getInventory().addItem(itemStack);
                        } else {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Token " + args[2] + " not found.");
                        }
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
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

                            plugin.getEntityManager().sendMessage("message.give-token", player);
                        } else {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Token " + args[2] + " not found.");
                        }
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + "Player " + args[1] + " not found.");
                    }
                }
            } else if (commandSender instanceof Player) {
                plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (commandSender.hasPermission("graves.reload")) {
                plugin.reload();
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Reloaded config file.");
            } else if (commandSender instanceof Player) {
                plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        } else if (args[0].equalsIgnoreCase("dump")) {
            if (commandSender.hasPermission("graves.dump")) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Running dump functions...");
                plugin.dumpServerInfo(commandSender);
            } else if (commandSender instanceof Player) {
                plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        } else if (args[0].equalsIgnoreCase("debug")) {
            if (commandSender.hasPermission("graves.debug")) {
                if (args.length > 1) {
                    try {
                        plugin.getConfig().set("settings.debug.level", Integer.parseInt(args[1]));

                        if (commandSender instanceof Player) {
                            List<String> stringList = plugin.getConfig().getStringList("settings.debug.user");

                            stringList.add(((Player) commandSender).getUniqueId().toString());
                            commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + ChatColor.RESET + "Debug level changed to: (" + args[1]
                                    + "), User (" + commandSender.getName() + ") added, This won't persist "
                                    + "across restarts or reloads.");
                        } else {
                            plugin.getLogger().info("Debug level changed to: (" + args[1]
                                    + "), This won't persist across restarts or reloads.");
                        }
                    } catch (NumberFormatException ignored) {
                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                + ChatColor.RESET + args[1] + " is not a valid int.");
                    }
                } else {
                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + ChatColor.RESET + "/graves debug {level}");
                }
            } else if (commandSender instanceof Player) {
                plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        } else if (args[0].equalsIgnoreCase("cleanup")) {
            if (commandSender.hasPermission("graves.cleanup")) {
                List<Grave> graveList = new ArrayList<>(plugin.getCacheManager().getGraveMap().values());

                for (Grave grave : graveList) {
                    plugin.getGraveManager().removeGrave(grave);
                }

                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                        + ChatColor.RESET + graveList.size() + " graves cleaned up.");
            } else if (commandSender instanceof Player) {
                plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        } else if (args[0].equalsIgnoreCase("purge")) {
            if (commandSender.hasPermission("graves.purge")) {
                if (args.length > 1 && !args[1].equalsIgnoreCase("graves") && !args[1].equalsIgnoreCase("grave")) {
                    if (args[1].equalsIgnoreCase("holograms") || args[1].equalsIgnoreCase("hologram")) {
                        int count = 0;

                        for (World world : plugin.getServer().getWorlds()) {
                            for (Entity entity : world.getEntities()) {
                                if (entity.getScoreboardTags().contains("graveHologram")) {
                                    entity.remove();
                                    count++;
                                }
                            }
                        }

                        commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » "
                                + ChatColor.RESET + count + " holograms purged.");
                    }
                } else {
                    List<Grave> graveList = new ArrayList<>(plugin.getCacheManager().getGraveMap().values());

                    for (Grave grave : graveList) {
                        plugin.getGraveManager().removeGrave(grave);
                    }

                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + graveList.size() + " graves purged.");
                }
            } else {
                plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        } else if (args[0].equalsIgnoreCase("import")) {
            if (commandSender.hasPermission("graves.import")) {
                // Disable for everyone except Ranull, not ready for production.
                if (!commandSender.getName().equals("Ranull")) {
                    commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                            + "Import functionality not ready for production.");

                    return true;
                }

                List<Grave> graveList = plugin.getImportManager().importExternalPluginGraves();

                for (Grave grave : graveList) {
                    plugin.getDataManager().addGrave(grave);
                    plugin.getGraveManager().placeGrave(grave.getLocationDeath(), grave);
                }

                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + " Imported " + graveList.size() + " graves.");
            } else {
                plugin.getEntityManager().sendMessage("message.permission-denied", (Player) commandSender);
            }
        }

        return true;
    }

    public void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + "Graves "
                + ChatColor.DARK_GRAY + "v" + plugin.getVersion());

        if (sender.hasPermission("graves.gui")) {
            sender.sendMessage(
                    ChatColor.RED + "/graves " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET + " Graves GUI");
        }

        if (sender.hasPermission("graves.gui")) {
            if (sender.hasPermission("graves.gui.other")) {
                sender.sendMessage(
                        ChatColor.RED + "/graves list {player} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET +
                                " View player graves");
            } else {
                sender.sendMessage(
                        ChatColor.RED + "/graves list " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET +
                                " Player graves");
            }
        }

        if (sender.hasPermission("graves.givetoken")) {
            if (plugin.getConfig().getBoolean("settings.token")) {
                sender.sendMessage(ChatColor.RED + "/graves givetoken {player} {amount} " + ChatColor.DARK_GRAY + "-"
                        + ChatColor.RESET + " Give grave token");
            }
        }

        if (sender.hasPermission("graves.reload")) {
            sender.sendMessage(ChatColor.RED + "/graves reload " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Reload plugin");
        }

        if (sender.hasPermission("graves.dump")) {
            sender.sendMessage(ChatColor.RED + "/graves dump " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Dump server information");
        }

        if (sender.hasPermission("graves.debug")) {
            sender.sendMessage(ChatColor.RED + "/graves debug {level} " + ChatColor.DARK_GRAY + "-" + ChatColor.RESET
                    + " Change debug level");
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "Author: " + ChatColor.RED + "Ranull");
    }

    @Override
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

            if (commandSender.hasPermission("graves.dump")) {
                stringList.add("dump");
            }

            if (commandSender.hasPermission("graves.debug")) {
                stringList.add("debug");
            }

            if (commandSender.hasPermission("graves.cleanup")) {
                stringList.add("cleanup");
            }

            if (commandSender.hasPermission("graves.purge")) {
                stringList.add("purge");
            }

            if (plugin.getRecipeManager() != null && commandSender.hasPermission("graves.givetoken")) {
                stringList.add("givetoken");
            }
        } else if (args.length > 1) {
            if ((args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gui"))
                    && commandSender.hasPermission("graves.gui.other")) {
                plugin.getServer().getOnlinePlayers().forEach((player -> stringList.add(player.getName())));
            } else if (args[0].equals("debug") && commandSender.hasPermission("graves.debug")) {
                stringList.add("0");
                stringList.add("1");
                stringList.add("2");
            } else if (args[0].equals("purge") && commandSender.hasPermission("graves.purge")) {
                if (args.length == 2) {
                    stringList.add("graves");
                    stringList.add("holograms");
                }
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
