package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayerRespawnListener implements Listener {

    private final Graves plugin;

    public PlayerRespawnListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<String> permissionList = plugin.getPermissionList(player);
        List<Grave> graveList = plugin.getGraveManager().getGraveList(player);

        if (!graveList.isEmpty()) {
            Grave grave = graveList.get(graveList.size() - 1);

            plugin.getServer()
                  .getScheduler()
                  .runTaskLater(plugin, () -> plugin.getEntityManager()
                                                    .runFunction(player, plugin.getConfigString("respawn.function", player, permissionList, "none"), grave), 1L);

            if (plugin.getConfigBool("respawn.compass", player, permissionList)
                && grave.getLivedTime()
                   <= plugin.getConfigInt("respawn.compass-time", player, permissionList) * 1000L) {
                List<Location> locationList = plugin.getGraveManager()
                                                    .getGraveLocationList(event.getRespawnLocation(), grave);

                if (!locationList.isEmpty()) {
                    ItemStack itemStack = plugin.getEntityManager()
                                                .createGraveCompass(player, locationList.get(0), grave);

                    if (itemStack != null) {
                        player.getInventory().addItem(itemStack);
                    }
                }
            }
        }
    }

}
