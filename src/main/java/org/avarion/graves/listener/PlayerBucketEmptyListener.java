package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerBucketEmptyListener implements Listener {

    private final Graves plugin;

    public PlayerBucketEmptyListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

        if (plugin.getBlockManager().getGraveFromBlock(block) != null) {
            block.getState().update();
            event.setCancelled(true);
        }
    }

}
