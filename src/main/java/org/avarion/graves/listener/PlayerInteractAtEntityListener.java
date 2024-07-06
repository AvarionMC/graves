package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.type.Grave;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractAtEntityListener implements Listener {

    private final Graves plugin;

    public PlayerInteractAtEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(@NotNull PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() == EquipmentSlot.HAND
            && event.getRightClicked() instanceof ArmorStand
            && player.getGameMode() != GameMode.SPECTATOR) {
            Entity entity = event.getRightClicked();
            Grave grave = plugin.getEntityDataManager().getGrave(entity);

            if (grave != null) {
                event.setCancelled(true);
                plugin.getGraveManager().openGrave(player, entity.getLocation(), grave);
            }
        }
    }

}
