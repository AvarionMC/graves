package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.util.Version;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerJoinListener implements Listener {

    private final Graves plugin;

    public PlayerJoinListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("settings.update.check") && player.hasPermission("graves.update.notify")) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                final Version latestVersion = plugin.getLatestVersion();

                if (latestVersion == null) {
                    return;
                }

                if (plugin.getVersion().compareTo(latestVersion) < 0) {
                    player.sendMessage(ChatColor.RED
                                       + "☠"
                                       + ChatColor.DARK_GRAY
                                       + " » "
                                       + ChatColor.RESET
                                       + "Outdated version detected "
                                       + plugin.getVersion()
                                       + ", latest version is "
                                       + latestVersion
                                       + ", https://www.spigotmc.org/resources/"
                                       + plugin.getSpigotID()
                                       + "/");
                }
            });
        }
    }

}
