package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final Graves plugin;

    public PlayerJoinListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("settings.update.check") && player.hasPermission("graves.update.notify")) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String latestVersion = plugin.getLatestVersion();

                if (latestVersion != null) {
                    try {
                        double pluginVersion = Double.parseDouble(plugin.getVersion());
                        double pluginVersionLatest = Double.parseDouble(latestVersion);

                        if (pluginVersion < pluginVersionLatest) {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Outdated version detected " + pluginVersion
                                    + ", latest version is " + pluginVersionLatest
                                    + ", https://www.spigotmc.org/resources/" + plugin.getSpigotID() + "/");
                        }
                    } catch (NumberFormatException exception) {
                        if (!plugin.getVersion().equalsIgnoreCase(latestVersion)) {
                            player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                                    + "Outdated version detected " + plugin.getVersion()
                                    + ", latest version is " + latestVersion + ", https://www.spigotmc.org/resources/"
                                    + plugin.getSpigotID() + "/");
                        }
                    }
                }
            });
        }
    }
}