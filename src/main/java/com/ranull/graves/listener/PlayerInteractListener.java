package com.ranull.graves.listener;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.Grave;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.List;
import java.util.UUID;

public class PlayerInteractListener implements Listener {
    private final Graves plugin;

    public PlayerInteractListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if ((!plugin.getVersionManager().hasSecondHand() || (event.getHand() != null
                && event.getHand() == EquipmentSlot.HAND))
                && (plugin.getVersionManager().is_v1_7() || player.getGameMode() != GameMode.SPECTATOR)) {
            if (event.getClickedBlock() != null && event.useInteractedBlock() != Event.Result.DENY
                    && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Block block = event.getClickedBlock();
                Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

                if (grave == null) {
                    Block blockRelative = block.getRelative(event.getBlockFace());

                    if (!blockRelative.getType().isSolid()) {
                        grave = plugin.getBlockManager().getGraveFromBlock(blockRelative);
                    }
                }

                if (grave != null) {
                    event.setCancelled(plugin.getGraveManager().openGrave(player, block.getLocation(), grave));
                }
            }

            if (event.getItem() != null && event.getItem().getType() == Material.COMPASS
                    && plugin.getVersionManager().hasLodestone()) {
                ItemStack itemStack = event.getItem();

                if (itemStack.getItemMeta() != null && itemStack.getItemMeta() instanceof CompassMeta) {
                    CompassMeta compassMeta = (CompassMeta) itemStack.getItemMeta();
                    UUID uuid = plugin.getPlayerManager().getUUIDFromItemStack(itemStack);

                    if (compassMeta.getLodestone() != null && uuid != null) {
                        if (plugin.getDataManager().getGraveMap().containsKey(uuid)) {
                            Grave grave = plugin.getDataManager().getGraveMap().get(uuid);
                            List<Location> locationList = plugin.getGraveManager()
                                    .getGraveLocationList(player.getLocation(), grave);

                            if (!locationList.isEmpty()) {
                                Location location = locationList.get(0);

                                if (!compassMeta.getLodestone().equals(location)) {
                                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(),
                                            plugin.getPlayerManager().getCompassItemStack(location, grave));
                                }

                                if (player.getWorld().equals(location.getWorld())) {
                                    plugin.getPlayerManager().sendMessage("message.distance", player,
                                            location, grave);
                                } else {
                                    plugin.getPlayerManager().sendMessage("message.distance-world", player,
                                            location, grave);
                                }
                            }
                        } else {
                            player.getInventory().remove(itemStack);
                        }
                    }
                }
            }
        }
    }
}
