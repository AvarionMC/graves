package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.type.Grave;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractEntityListener implements Listener {

    private final Graves plugin;

    public PlayerInteractEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() == EquipmentSlot.HAND
            && event.getRightClicked() instanceof ItemFrame
            && player.getGameMode() != GameMode.SPECTATOR) {
            Entity entity = event.getRightClicked();
            Grave grave = plugin.getEntityDataManager().getGrave(entity);

            if (grave != null) {
                event.setCancelled(plugin.getGraveManager().openGrave(player, entity.getLocation(), grave));
            }
        }
    }

}
