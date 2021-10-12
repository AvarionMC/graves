package com.ranull.graves.listener.integration.furniturelib;

import com.ranull.graves.Graves;
import de.Ste3et_C0st.FurnitureLib.SchematicLoader.Events.ProjectBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ProjectBreakListener implements Listener {
    private final Graves plugin;

    public ProjectBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectBreak(ProjectBreakEvent event) {
        event.setCancelled(event.getID().getUUID() != null && plugin.getFurnitureLib()
                .getGraveFromFurnitureLib(event.getLocation(), event.getID().getUUID()) != null);
    }
}
