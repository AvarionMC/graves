package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.manager.CacheManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayerDeathListener implements Listener {

    private final Graves plugin;

    public PlayerDeathListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathEvent(@NotNull PlayerDeathEvent event) {
        List<ItemStack> itemStackList = event.getDrops();

        itemStackList.removeIf(itemStack -> itemStack != null
                                            && plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null
                                            && plugin.getConfigBool("compass.destroy", event.getEntity()));

        CacheManager.removedItemStackMap.put(event.getEntity().getUniqueId(), new ArrayList<>(itemStackList));
    }

}