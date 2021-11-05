package com.ranull.graves.listener.integration.furniturelib;

import com.ranull.graves.integration.FurnitureLib;
import de.Ste3et_C0st.FurnitureLib.SchematicLoader.Events.ProjectBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ProjectBreakListener implements Listener {
    private final FurnitureLib furnitureLib;

    public ProjectBreakListener(FurnitureLib furnitureLib) {
        this.furnitureLib = furnitureLib;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectBreak(ProjectBreakEvent event) {
        event.setCancelled(event.getID().getUUID() != null
                && furnitureLib.getGrave(event.getLocation(), event.getID().getUUID()) != null);
    }
}
