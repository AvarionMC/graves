package com.rngservers.graves.events;

import com.rngservers.graves.Main;
import com.rngservers.graves.grave.Grave;
import com.rngservers.graves.grave.GraveManager;
import com.rngservers.graves.grave.Messages;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Iterator;
import java.util.List;

public class Events implements Listener {
    private Main plugin;
    private GraveManager graveManager;
    private Messages messages;

    public Events(Main plugin, GraveManager chestManager, Messages messages) {
        this.plugin = plugin;
        this.graveManager = chestManager;
        this.messages = messages;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().hasPermission("graves.place")) {
            return;
        }
        List<String> graveEntities = plugin.getConfig().getStringList("settings.graveEntities");
        if (graveEntities.contains(event.getEntity().getType().toString()) || graveEntities.contains("ALL")) {
            if (graveManager.getItemAmount(event.getEntity().getInventory()) > 0) {
                graveManager.createGrave(event.getEntity());
                event.getDrops().clear();
                event.getEntity().getInventory().clear();
                Boolean expStore = plugin.getConfig().getBoolean("settings.expStore");
                if (expStore) {
                    event.setNewTotalExp(0);
                    event.setDroppedExp(0);
                    event.setKeepLevel(false);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        List<String> graveEntities = plugin.getConfig().getStringList("settings.graveEntities");
        if (graveEntities.contains(event.getEntity().getType().toString()) || graveEntities.contains("ALL")) {
            if (event.getDrops().size() > 0) {
                graveManager.createGrave(event.getEntity(), event.getDrops());
                event.getDrops().clear();
            }
        }
    }

    @EventHandler
    public void onGraveOpen(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (event.getPlayer().hasPermission("graves.autoloot")) {
            if (event.getPlayer().isSneaking()) {
                return;
            }
        }
        Grave grave = graveManager.getGrave(event.getClickedBlock().getLocation());
        if (grave != null) {
            if (!event.getPlayer().hasPermission("graves.open")) {
                messages.permissionDenied(event.getPlayer());
                event.setCancelled(true);
                return;
            }
            if (graveManager.hasPermission(grave, event.getPlayer())) {
                event.getPlayer().openInventory(grave.getInventory());
                messages.graveOpen(grave.getLocation());
            } else {
                messages.graveProtected(event.getPlayer(), event.getClickedBlock().getLocation());
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGraveSneakOpen(PlayerInteractEvent event) {
        if (!event.getPlayer().hasPermission("graves.autoloot")) {
            return;
        }
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (!event.getPlayer().isSneaking()) {
            return;
        }
        Grave grave = graveManager.getGrave(event.getClickedBlock().getLocation());
        if (grave != null) {
            if (graveManager.hasPermission(grave, event.getPlayer())) {
                graveManager.autoLoot(grave, event.getPlayer());
                messages.graveOpen(grave.getLocation());
            } else {
                messages.graveProtected(event.getPlayer(), grave.getLocation());
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHologramOpen(PlayerInteractAtEntityEvent event) {
        if (event.getPlayer().hasPermission("graves.autoloot")) {
            if (event.getPlayer().isSneaking()) {
                return;
            }
        }
        if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            Boolean hologramOpen = plugin.getConfig().getBoolean("settings.hologramOpen");
            if (hologramOpen) {
                ArmorStand armorStand = (ArmorStand) event.getRightClicked();
                Grave grave = graveManager.getGraveFromHologram(armorStand);
                if (grave != null) {
                    if (!event.getPlayer().hasPermission("graves.open")) {
                        messages.permissionDenied(event.getPlayer());
                        event.setCancelled(true);
                        return;
                    }
                    if (graveManager.hasPermission(grave, event.getPlayer())) {
                        event.getPlayer().openInventory(grave.getInventory());
                        messages.graveOpen(grave.getLocation());
                    } else {
                        messages.graveProtected(event.getPlayer(), grave.getLocation());
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onHologramSneakOpen(PlayerInteractAtEntityEvent event) {
        if (!event.getPlayer().hasPermission("graves.autoloot")) {
            return;
        }
        if (!event.getPlayer().isSneaking()) {
            return;
        }
        if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            Boolean hologramOpen = plugin.getConfig().getBoolean("settings.hologramOpen");
            if (hologramOpen) {
                ArmorStand armorStand = (ArmorStand) event.getRightClicked();
                Grave grave = graveManager.getGraveFromHologram(armorStand);
                if (grave != null) {
                    if (graveManager.hasPermission(grave, event.getPlayer())) {
                        graveManager.autoLoot(grave, event.getPlayer());
                        messages.graveOpen(grave.getLocation());
                    } else {
                        messages.graveProtected(event.getPlayer(), grave.getLocation());
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onGraveClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Grave) {
            Grave grave = (Grave) event.getInventory().getHolder();
            messages.graveClose(grave.getLocation());
            if (grave.getItemAmount() == 0) {
                Player player = (Player) event.getPlayer();
                grave.getInventory().getViewers().remove(player);
                messages.graveLoot(grave.getLocation(), player);
                graveManager.giveExperience(grave, player);
                graveManager.removeHologram(grave);
                graveManager.removeGrave(grave);
            }
        }
    }

    @EventHandler
    public void onGraveBreak(BlockBreakEvent event) {
        Grave grave = graveManager.getGrave(event.getBlock().getLocation());
        if (grave != null) {
            if (!event.getPlayer().hasPermission("graves.break")) {
                messages.permissionDenied(event.getPlayer());
                event.setCancelled(true);
                return;
            }
            Boolean isOwner = false;
            Boolean isKiller = false;
            Boolean ignore = false;

            if (event.getPlayer().hasPermission("graves.bypass")) {
                ignore = true;
            }
            Boolean graveProtected = plugin.getConfig().getBoolean("settings.graveProtected");
            if (graveProtected) {
                if (event.getPlayer().equals(grave.getPlayer())) {
                    isOwner = true;
                }
            } else {
                ignore = true;
            }
            Boolean killerOpen = plugin.getConfig().getBoolean("settings.killerOpen");
            if (killerOpen) {
                if (event.getPlayer().equals(grave.getKiller())) {
                    isKiller = true;
                }
            }
            if (isOwner || isKiller || ignore) {
                graveManager.dropGrave(grave);
                graveManager.dropExperience(grave);
                graveManager.removeHologram(grave);
                graveManager.removeGrave(grave);
                event.getBlock().setType(Material.AIR);
            } else {
                messages.graveProtected(event.getPlayer(), event.getBlock().getLocation());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onGraveBreakNaturally(BlockFromToEvent event) {
        Grave grave = graveManager.getGrave(event.getToBlock().getLocation());
        if (grave != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGraveExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Grave grave = graveManager.getGrave(block.getLocation());
            if (grave != null) {
                Boolean graveExplode = plugin.getConfig().getBoolean("settings.graveExplode");
                if (graveExplode) {
                    graveManager.dropGrave(grave);
                    graveManager.dropExperience(grave);
                    graveManager.removeHologram(grave);
                    graveManager.removeGrave(grave);
                } else {
                    event.blockList().remove(block);
                }
            }
        }
    }
}
