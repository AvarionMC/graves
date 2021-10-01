package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerDropItemListener implements Listener {
    private final Graves plugin;

    public PlayerDropItemListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(PlayerDropItemEvent event) {
        ItemStack itemStack = event.getItemDrop().getItemStack();

        if (itemStack.getType() == Material.COMPASS && plugin.getPlayerManager().getUUIDFromItemStack(itemStack) != null) {
            event.getItemDrop().remove();
        }
    }
}
