package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

public class BlockPistonExtendListener implements Listener {
    private final Graves plugin;

    public BlockPistonExtendListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        Block block = event.getBlock().getRelative(event.getDirection());

        if (plugin.getBlockManager().getGraveFromBlock(block) != null) {
            event.setCancelled(true);
        } else {
            for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 0.5, 0.5, 0.5)) {
                if (plugin.getHologramManager().getGrave(entity) != null) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
