package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayerDeathListener implements Listener {
    private final Graves plugin;

    public PlayerDeathListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        List<ItemStack> itemStackList = event.getDrops();
        Iterator<ItemStack> iterator = itemStackList.iterator();

        while (iterator.hasNext()) {
            ItemStack itemStack = iterator.next();

            if (itemStack != null) {
                if (plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null
                        && plugin.getConfig("compass.destroy", event.getEntity()).getBoolean("compass.destroy")) {
                    iterator.remove();
                }
            }
        }

        plugin.getCacheManager().getRemovedItemStackMap()
                .put(event.getEntity().getUniqueId(), new ArrayList<>(itemStackList));
    }
}