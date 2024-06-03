package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

public class CreatureSpawnListener implements Listener {

    private final Graves plugin;

    public CreatureSpawnListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(@NotNull CreatureSpawnEvent event) {
        if (!event.getEntity().isDead()) {
            plugin.getEntityManager().setDataString(event.getEntity(), "spawnReason", event.getSpawnReason().name());
        }
    }

}
