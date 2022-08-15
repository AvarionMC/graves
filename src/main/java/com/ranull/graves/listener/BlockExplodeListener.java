package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveExplodeEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.Iterator;

public class BlockExplodeListener implements Listener {
    private final Graves plugin;

    public BlockExplodeListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

            if (grave != null) {
                Location location = block.getLocation();

                if ((System.currentTimeMillis() - grave.getTimeCreation()) < 1000) {
                    iterator.remove();
                } else if (plugin.getConfig("explode", grave).getBoolean("explode")) {
                    GraveExplodeEvent graveExplodeEvent = new GraveExplodeEvent(location, null, grave);

                    plugin.getServer().getPluginManager().callEvent(graveExplodeEvent);

                    if (!graveExplodeEvent.isCancelled()) {
                        if (plugin.getConfig("drop.explode", grave).getBoolean("drop.explode")) {
                            plugin.getGraveManager().breakGrave(location, grave);
                        } else {
                            plugin.getGraveManager().removeGrave(grave);
                        }

                        plugin.getGraveManager().closeGrave(grave);
                        plugin.getGraveManager().playEffect("effect.loot", location, grave);
                        plugin.getEntityManager().runCommands("event.command.explode",
                                event.getBlock().getType().name(), location, grave);

                        if (plugin.getConfig("zombie.explode", grave).getBoolean("zombie.explode")) {
                            plugin.getEntityManager().spawnZombie(location, grave);
                        }
                    } else {
                        iterator.remove();
                    }
                } else {
                    iterator.remove();
                }
            }
        }
    }
}
