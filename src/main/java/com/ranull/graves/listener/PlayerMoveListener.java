package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {
    private final Graves plugin;

    public PlayerMoveListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() != null && (event.getTo().getBlockX() != event.getFrom().getBlockX()
                || event.getTo().getBlockY() != event.getFrom().getBlockY()
                || event.getTo().getBlockZ() != event.getFrom().getBlockZ())) {
            Player player = event.getPlayer();
            Location location = LocationUtil.roundLocation(player.getLocation());

            if (location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()
                    && plugin.getLocationManager().isLocationSafePlayer(location)) {
                plugin.getLocationManager().setLastSolidLocation(player, location.clone());
            }

            if (location.getWorld() != null && plugin.getDataManager().hasChunkData(location)) {
                ChunkData chunkData = plugin.getDataManager().getChunkData(location);
                BlockData blockData = null;

                if (chunkData.getBlockDataMap().containsKey(location)) {
                    blockData = chunkData.getBlockDataMap().get(location);
                } else if (chunkData.getBlockDataMap().containsKey(location.add(0, 1, 0))) {
                    blockData = chunkData.getBlockDataMap().get(location);
                } else if (chunkData.getBlockDataMap().containsKey(location.subtract(0, 2, 0))) {
                    blockData = chunkData.getBlockDataMap().get(location);
                }

                if (blockData != null && plugin.getDataManager().getGraveMap().containsKey(blockData.getGraveUUID())) {
                    Grave grave = plugin.getDataManager().getGraveMap().get(blockData.getGraveUUID());

                    if (plugin.getConfig("block.walk-over", grave).getBoolean("block.walk-over")) {
                        plugin.getGraveManager().autoLootGrave(event.getPlayer(), location, grave);
                    }
                }
            }
        }
    }
}
