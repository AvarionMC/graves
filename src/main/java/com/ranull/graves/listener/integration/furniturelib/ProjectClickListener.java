package com.ranull.graves.listener.integration.furniturelib;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.FurnitureLib;
import com.ranull.graves.type.Grave;
import de.Ste3et_C0st.FurnitureLib.SchematicLoader.Events.ProjectClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ProjectClickListener implements Listener {
    private final Graves plugin;
    private final FurnitureLib furnitureLib;

    public ProjectClickListener(Graves plugin, FurnitureLib furnitureLib) {
        this.plugin = plugin;
        this.furnitureLib = furnitureLib;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectClick(ProjectClickEvent event) {
        if (event.getID().getUUID() != null) {
            Grave grave = furnitureLib.getGrave(event.getLocation(), event.getID().getUUID());

            if (grave != null) {
                event.setCancelled(plugin.getGraveManager().openGrave(event.getPlayer(), event.getLocation(), grave));
            }
        }
    }
}
