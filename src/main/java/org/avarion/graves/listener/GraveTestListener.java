package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.event.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class GraveTestListener implements Listener {

    private final Graves plugin;

    public GraveTestListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(@NotNull GraveCreateEvent event) {
        plugin.testMessage(plugin.getEntityManager().getEntityName(event.getEntity()) + " created a grave");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveOpen(@NotNull GraveOpenEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " opened " + event.getGrave().getOwnerName() + "'s grave");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveClose(@NotNull GraveCloseEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " closed " + event.getGrave().getOwnerName() + "'s grave");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveBreak(@NotNull GraveBreakEvent event) {
        plugin.testMessage(event.getPlayer().getName() + " broke " + event.getGrave().getOwnerName() + "'s grave");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveExplode(@NotNull GraveExplodeEvent event) {
        plugin.testMessage(event.getGrave().getOwnerName() + "'s grave exploded");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveTimeout(@NotNull GraveTimeoutEvent event) {
        plugin.testMessage(event.getGrave().getOwnerName() + "'s grave timed out");
        event.setLocation(event.getLocation());
    }
}
