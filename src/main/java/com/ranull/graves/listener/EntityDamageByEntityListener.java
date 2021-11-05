package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageByEntityListener implements Listener {
    private final Graves plugin;

    public EntityDamageByEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        if ((plugin.getVersionManager().is_v1_7() || entity instanceof ArmorStand) && plugin.hasItemsAdder()) {
            event.setCancelled(plugin.getItemsAdder().getGrave(entity) != null);
        }
    }
}
