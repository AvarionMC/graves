package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerDropItemListener implements Listener {

    private final Graves plugin;

    public PlayerDropItemListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(@NotNull PlayerDropItemEvent event) {
        ItemStack itemStack = event.getItemDrop().getItemStack();

        if (plugin.getEntityManager().getGraveUUIDFromItemStack(itemStack) != null) {
            event.getItemDrop().remove();
        }
    }

}
