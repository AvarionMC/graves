package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.data.BlockData;
import org.avarion.graves.data.ChunkData;
import org.avarion.graves.manager.CacheManager;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.LocationUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerMoveListener implements Listener {

    private final Graves plugin;

    public PlayerMoveListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR || event.getTo() == null || event.getTo()
                                                                                        .toVector()
                                                                                        .equals(event.getFrom()
                                                                                                     .toVector())) {
            return;
        }

        Location location = LocationUtil.roundLocation(player.getLocation());

        if (plugin.getLocationManager().isInsideBorder(location) && location.getBlock()
                                                                            .getRelative(BlockFace.DOWN)
                                                                            .getType()
                                                                            .isSolid() && plugin.getLocationManager()
                                                                                                .isLocationSafePlayer(location)) {
            plugin.getLocationManager().setLastSolidLocation(player, location.clone());
        }

        if (location.getWorld() != null && plugin.getDataManager().hasChunkData(location)) {
            ChunkData chunkData = plugin.getDataManager().getChunkData(location);
            BlockData blockData = null;

            if (chunkData.getBlockDataMap().containsKey(location)) {
                blockData = chunkData.getBlockDataMap().get(location);
            }
            else if (chunkData.getBlockDataMap().containsKey(location.clone().add(0, 1, 0))) {
                blockData = chunkData.getBlockDataMap().get(location.clone().add(0, 1, 0));
            }
            else if (chunkData.getBlockDataMap().containsKey(location.clone().subtract(0, 1, 0))) {
                blockData = chunkData.getBlockDataMap().get(location.clone().subtract(0, 1, 0));
            }

            if (blockData != null && CacheManager.graveMap.containsKey(blockData.graveUUID())) {
                Grave grave = CacheManager.graveMap.get(blockData.graveUUID());

                if (grave != null && plugin.getConfigBool("block.walk-over", grave) && plugin.getEntityManager()
                                                                                             .canOpenGrave(player, grave)) {
                    plugin.getGraveManager().cleanupCompasses(player, grave);
                    plugin.getGraveManager().autoLootGrave(event.getPlayer(), location, grave);
                }
            }
        }
    }

}
