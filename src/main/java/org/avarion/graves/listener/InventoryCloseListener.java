package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.event.GraveCloseEvent;
import org.avarion.graves.type.Grave;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

public class InventoryCloseListener implements Listener {

    private final Graves plugin;

    public InventoryCloseListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Grave grave && event.getPlayer() instanceof Player player) {

            GraveCloseEvent graveCloseEvent = new GraveCloseEvent(event.getView(), grave);

            plugin.getServer().getPluginManager().callEvent(graveCloseEvent);

            if (grave.getItemAmount() <= 0) {
                grave.getInventory().getViewers().remove(player);
                plugin.getEntityManager().runCommands("event.command.loot", player, player.getLocation(), grave);
                plugin.getEntityManager().sendMessage("message.loot", player, player.getLocation(), grave);
                plugin.getEntityManager().spawnZombie(grave.getLocationDeath(), player, player, grave);
                plugin.getGraveManager().giveGraveExperience(player, grave);
                plugin.getGraveManager().removeGrave(grave);
            }

            plugin.getEntityManager().playWorldSound("sound.close", player, grave);
        }
    }

}
