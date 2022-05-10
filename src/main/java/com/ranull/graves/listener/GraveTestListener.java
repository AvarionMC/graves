package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.event.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class GraveTestListener implements Listener {
    private final Graves plugin;

    public GraveTestListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(GraveCreateEvent event) {
        plugin.testMessage(plugin.getEntityManager().getEntityName(event.getEntity()) + " created a grave");
        //event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveOpen(GraveOpenEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " opened " + event.getGrave().getOwnerName() + "'s grave");
        //event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveClose(GraveCloseEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " closed " + event.getGrave().getOwnerName() + "'s grave");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveBreak(GraveBreakEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " broke " + event.getGrave().getOwnerName() + "'s grave");
        //event.setExpToDrop(0);
        //event.setDropItems(false);
        //event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveExplode(GraveExplodeEvent event) {
        plugin.testMessage(event.getGrave().getOwnerName() + "'s grave exploded");
        //event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveTimeout(GraveTimeoutEvent event) {
        plugin.testMessage(event.getGrave().getOwnerName() + "'s grave timed out");
        event.setLocation(event.getLocation());
        //event.setCancelled(true);
    }

    /*
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveZombieSpawn(GraveZombieSpawnEvent event) {
        plugin.testMessage(event.getGrave().getOwnerName() + "'s zombie spawned");
        event.setCancelled(true);
    }
     */
}
