package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

public class BlockPlaceListener implements Listener {

    private final Graves plugin;

    public BlockPlaceListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        Block block = event.getBlock();
        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        if (grave != null || (plugin.getRecipeManager() != null && plugin.getRecipeManager()
                                                                         .isToken(event.getItemInHand()))) {
            event.setCancelled(true);
        }
    }

}
