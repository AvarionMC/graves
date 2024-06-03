package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.jetbrains.annotations.NotNull;

public class BlockFromToListener implements Listener {

    private final Graves plugin;

    public BlockFromToListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(@NotNull BlockFromToEvent event) {
        if (plugin.getBlockManager().getGraveFromBlock(event.getToBlock()) != null) {
            event.setCancelled(true);
        }
    }

}
