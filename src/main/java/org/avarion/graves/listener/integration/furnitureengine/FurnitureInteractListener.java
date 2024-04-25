package org.avarion.graves.listener.integration.furnitureengine;

import com.mira.furnitureengine.api.events.FurnitureInteractEvent;
import org.avarion.graves.Graves;
import org.avarion.graves.integration.FurnitureEngine;
import org.avarion.graves.type.Grave;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FurnitureInteractListener implements Listener {

    private final Graves plugin;
    private final FurnitureEngine furnitureEngine;

    public FurnitureInteractListener(Graves plugin, FurnitureEngine furnitureEngine) {
        this.plugin = plugin;
        this.furnitureEngine = furnitureEngine;
    }

    @EventHandler
    public void onFurnitureBreak(FurnitureInteractEvent event) {
        ItemFrame itemFrame = furnitureEngine.getItemFrame(event.getFurnitureLocation());

        if (itemFrame != null) {
            Grave grave = furnitureEngine.getGrave(itemFrame.getLocation(), itemFrame.getUniqueId());

            if (grave != null) {
                event.setCancelled(plugin.getGraveManager()
                                         .openGrave(event.getPlayer(), itemFrame.getLocation(), grave));
            }
        }
    }

}
