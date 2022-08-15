package com.ranull.graves.command;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Graveyard;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GraveyardsCommand implements CommandExecutor, TabCompleter {
    private final Graves plugin;

    public GraveyardsCommand(Graves plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
                             @NotNull String string, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            // Disable for everyone except Ranull, not ready for production.
            if (!player.getName().equals("Ranull")) {
                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Graveyards not ready for production.");

                return true;
            }

            if (args.length < 1) {
                player.sendMessage("/graveyards create");
                player.sendMessage("/graveyards modify");
            } else if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 2) {
                    player.sendMessage("/graveyard create (type)");
                } else if (args[1].equalsIgnoreCase("worldguard")) {
                    if (plugin.getIntegrationManager().getWorldGuard() != null) {
                        if (plugin.getConfig().getBoolean("settings.graveyard.worldguard.enabled")) {
                            if (args.length < 3) {
                                player.sendMessage("/graveyard create worldguard (region)");
                            } else {
                                String region = args[2];
                                World world = plugin.getIntegrationManager().getWorldGuard().getRegionWorld(args[2]);

                                if (world != null) {
                                    if (plugin.getIntegrationManager().getWorldGuard().isMember(region, player) || player.isOp()) {
                                        Graveyard graveyard = plugin.getGraveyardManager()
                                                .createGraveyard(player.getLocation(), region, world, Graveyard.Type.WORLDGUARD);

                                        player.sendMessage("creating graveyard " + region);
                                        plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
                                    } else {
                                        player.sendMessage("you are not a member of this region");
                                    }
                                } else {
                                    player.sendMessage("region not found " + region);
                                }
                            }
                        } else {
                            player.sendMessage("worldguard support disabled");
                        }
                    } else {
                        player.sendMessage("worldguard not detected");
                    }
                } else if (args[1].equalsIgnoreCase("towny")) {
                    if (plugin.getIntegrationManager().hasTowny()) {
                        if (plugin.getConfig().getBoolean("settings.graveyard.towny.enabled")) {
                            if (args.length < 3) {
                                player.sendMessage("/graveyard create towny (name)");
                            } else {
                                String name = args[2].replace("_", " ");

                                if (plugin.getIntegrationManager().getTowny().hasTownPlot(player, name)) {
                                    Graveyard graveyard = plugin.getGraveyardManager()
                                            .createGraveyard(player.getLocation(), name, player.getWorld(),
                                                    Graveyard.Type.TOWNY);

                                    player.sendMessage("creating graveyard " + name);
                                    plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
                                } else {
                                    player.sendMessage("plot not found " + name);
                                }
                            }
                        } else {
                            player.sendMessage("towny support disabled");
                        }
                    } else {
                        player.sendMessage("towny not detected");
                    }
                } else {
                    player.sendMessage("unknown type " + args[1]);
                }
            } else if (args[0].equalsIgnoreCase("modify")) {
                if (plugin.getGraveyardManager().isModifyingGraveyard(player)) {
                    plugin.getGraveyardManager().stopModifyingGraveyard(player);
                } else {
                    if (args.length < 2) {
                        player.sendMessage("/graveyard modify (type)");
                    } else if (args[1].equalsIgnoreCase("worldguard")) {
                        if (plugin.getIntegrationManager().getWorldGuard() != null) {
                            if (plugin.getConfig().getBoolean("settings.graveyard.worldguard.enabled")) {
                                if (args.length < 3) {
                                    player.sendMessage("/graveyard modify worldguard (region)");
                                } else {
                                    String region = args[2];
                                    World world = plugin.getIntegrationManager().getWorldGuard().getRegionWorld(args[2]);

                                    if (world != null) {
                                        Graveyard graveyard = plugin.getGraveyardManager()
                                                .getGraveyardByKey("worldguard|" + world.getName() + "|" + region);

                                        if (graveyard != null) {
                                            player.sendMessage("graveyard found");
                                            plugin.getGraveyardManager().startModifyingGraveyard(player, graveyard);
                                        } else {
                                            player.sendMessage("graveyard " + region + " not found");
                                        }
                                    }
                                }
                            } else {
                                player.sendMessage("worldguard support disabled");
                            }
                        } else {
                            player.sendMessage("worldguard not detected");
                        }
                    } else {
                        player.sendMessage("unknown type " + args[1]);
                    }
                }
            }
        } else {
            commandSender.sendMessage("Only players can run graveyard commands");
        }

        return true;
    }

    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command,
                                      @NotNull String string, @NotNull String @NotNull [] args) {
        return new ArrayList<>();
    }
}
