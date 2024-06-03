package org.avarion.graves.listener.integration.furnitureengine;

import com.mira.furnitureengine.api.events.FurnitureBreakEvent;
import org.avarion.graves.integration.FurnitureEngine;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class FurnitureBreakListener implements Listener {

    private final FurnitureEngine furnitureEngine;

    public FurnitureBreakListener(FurnitureEngine furnitureEngine) {
        this.furnitureEngine = furnitureEngine;
    }

    @EventHandler
    public void onFurnitureBreak(@NotNull FurnitureBreakEvent event) {
        ItemFrame itemFrame = furnitureEngine.getItemFrame(event.getFurnitureLocation());

        if (itemFrame != null) {
            event.setCancelled(furnitureEngine.getGrave(event.getFurnitureLocation(), itemFrame.getUniqueId()) != null);
        }
    }

}
