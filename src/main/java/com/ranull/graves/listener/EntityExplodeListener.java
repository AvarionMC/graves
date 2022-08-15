package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.GraveExplodeEvent;
import com.ranull.graves.type.Grave;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

public class EntityExplodeListener implements Listener {
    private final Graves plugin;

    public EntityExplodeListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

            if (grave != null) {
                Location location = block.getLocation().clone();

                if ((System.currentTimeMillis() - grave.getTimeCreation()) < 1000) {
                    iterator.remove();
                } else if (plugin.getConfig("grave.explode", grave).getBoolean("grave.explode")) {
                    GraveExplodeEvent graveExplodeEvent = new GraveExplodeEvent(location, event.getEntity(), grave);

                    plugin.getServer().getPluginManager().callEvent(graveExplodeEvent);

                    if (!graveExplodeEvent.isCancelled()) {
                        if (plugin.getConfig("drop.explode", grave).getBoolean("drop.explode")) {
                            plugin.getGraveManager().breakGrave(location, grave);
                        } else {
                            plugin.getGraveManager().removeGrave(grave);
                        }

                        plugin.getGraveManager().closeGrave(grave);
                        plugin.getGraveManager().playEffect("effect.loot", location, grave);
                        plugin.getEntityManager().runCommands("event.command.explode", event.getEntity(), location, grave);

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
