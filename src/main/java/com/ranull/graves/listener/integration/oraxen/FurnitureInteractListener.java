package com.ranull.graves.listener.integration.oraxen;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.Oraxen;
import com.ranull.graves.inventory.Grave;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class FurnitureInteractListener implements Listener {
    private final Graves plugin;
    private final Oraxen oraxen;

    public FurnitureInteractListener(Graves plugin, Oraxen oraxen) {
        this.plugin = plugin;
        this.oraxen = oraxen;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnitureInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (entity instanceof ItemFrame) {
            Grave grave = oraxen.getGraveFromOraxen(entity);

            if (grave != null) {
                event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(), entity.getLocation(), grave));
            }
        }
    }
}
