package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractAtEntityListener implements Listener {
    private final Graves plugin;

    public PlayerInteractAtEntityListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if ((!plugin.getVersionManager().hasSecondHand() || event.getHand() == EquipmentSlot.HAND)
                && event.getRightClicked() instanceof ArmorStand
                && (plugin.getVersionManager().is_v1_7() || player.getGameMode() != GameMode.SPECTATOR)) {
            Location location = event.getRightClicked().getLocation();
            Grave grave = plugin.getHologramManager().getGraveFromHologram(event.getRightClicked());

            if (grave != null) {
                event.setCancelled(plugin.getGraveManager().openGrave(player, location, grave));
            }
        }
    }
}
