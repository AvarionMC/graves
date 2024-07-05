package org.avarion.graves.listener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.avarion.graves.Graves;
import org.avarion.graves.manager.CacheManager;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;

public class PlayerDeathListener implements EventExecutor {

    private final Graves plugin;

    public PlayerDeathListener(Graves plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if (!(event instanceof PlayerDeathEvent deathEvent)) {
            return;
        }
        
        List<ItemStack> itemStackList = deathEvent.getDrops();
        Iterator<ItemStack> iterator = itemStackList.iterator();

        while (iterator.hasNext()) {
            ItemStack itemStack = iterator.next();

            if (itemStack != null) {
                if (plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null
                    && plugin.getConfigBool("compass.destroy", deathEvent.getEntity())) {
                    iterator.remove();
                }
            }
        }

        CacheManager.removedItemStackMap
              .put(deathEvent.getEntity().getUniqueId(), new ArrayList<>(itemStackList));
    }

}
