package org.avarion.graves.listener;

import org.avarion.graves.Graves;
import org.avarion.graves.event.GraveBreakEvent;
import org.avarion.graves.type.Grave;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public class BlockBreakListener implements Listener {

    private final Graves plugin;

    public BlockBreakListener(Graves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Grave grave = plugin.getBlockManager().getGraveFromBlock(block);

        if (grave != null) {
            if (plugin.getConfigBool("grave.break", grave)) {
                if (plugin.getEntityManager().canOpenGrave(player, grave)) {
                    GraveBreakEvent graveBreakEvent = new GraveBreakEvent(block, player, grave);

                    graveBreakEvent.setDropItems(plugin.getConfigBool("drop.break", grave));
                    plugin.getServer().getPluginManager().callEvent(graveBreakEvent);

                    if (!graveBreakEvent.isCancelled()) {
                        if (plugin.getConfigBool("drop.auto-loot.enabled", grave)) {
                            plugin.getGraveManager().autoLootGrave(player, block.getLocation(), grave);

                            if (graveBreakEvent.isDropItems() && plugin.getConfigBool("drop.auto-loot.break", grave)) {
                                plugin.getGraveManager().breakGrave(block.getLocation(), grave);
                            }
                            else {
                                event.setCancelled(true);

                                return;
                            }
                        }
                        else if (graveBreakEvent.isDropItems()) {
                            plugin.getGraveManager().breakGrave(block.getLocation(), grave);
                        }
                        else {
                            plugin.getGraveManager().removeGrave(grave);
                        }

                        if (graveBreakEvent.getBlockExp() > 0) {
                            plugin.getGraveManager().dropGraveExperience(block.getLocation(), grave);
                        }

                        plugin.getGraveManager().closeGrave(grave);
                        plugin.getGraveManager().playEffect("effect.loot", block.getLocation(), grave);
                        plugin.getEntityManager().spawnZombie(block.getLocation(), player, player, grave);
                        plugin.getEntityManager()
                              .runCommands("event.command.break", player, block.getLocation(), grave);
                    }
                    else {
                        event.setCancelled(true);
                    }
                }
                else {
                    plugin.getEntityManager().sendMessage("message.protection", player, player.getLocation(), grave);
                    event.setCancelled(true);
                }
            }
            else {
                event.setCancelled(true);
            }
        }
    }

}
