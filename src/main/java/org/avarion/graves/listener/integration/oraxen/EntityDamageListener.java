package org.avarion.graves.listener.integration.oraxen;

import org.avarion.graves.integration.Oraxen;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class EntityDamageListener implements Listener {

    private final Oraxen oraxen;

    public EntityDamageListener(Oraxen oraxen) {
        this.oraxen = oraxen;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(@NotNull EntityDamageEvent event) {
        event.setCancelled(event.getEntity() instanceof ItemFrame && oraxen.getGrave(event.getEntity()) != null);
    }

}
