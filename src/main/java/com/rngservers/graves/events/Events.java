package com.rngservers.graves.events;

import com.rngservers.graves.Main;
import com.rngservers.graves.grave.Grave;
import com.rngservers.graves.grave.GraveManager;
import com.rngservers.graves.messages.Messages;
import com.rngservers.graves.gui.GUIManager;
import org.bukkit.GameRule;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Events implements Listener {
    private Main plugin;
    private GraveManager graveManager;
    private GUIManager guiManager;
    private Messages messages;

    public Events(Main plugin, GraveManager chestManager, GUIManager guiManager, Messages messages) {
        this.plugin = plugin;
        this.graveManager = chestManager;
        this.guiManager = guiManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().hasPermission("graves.place")) {
            return;
        }
        Boolean graveOnlyCanBuild = plugin.getConfig().getBoolean("settings.graveOnlyCanBuild");
        if (graveOnlyCanBuild) {
            if (!graveManager.canBuild(event.getEntity(), event.getEntity().getLocation())) {
                messages.buildDenied(event.getEntity());
                return;
            }
        }
        if (!plugin.getConfig().getBoolean("settings.ignoreKeepInventory")) {
            if (event.getEntity().getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY)) {
                return;
            }
        }
        Integer graveMax = plugin.getConfig().getInt("settings.graveMax");
        if (graveMax > 0) {
            if (graveManager.getGraves(event.getEntity()).size() >= graveMax) {
                messages.graveMax(event.getEntity());
                return;
            }
        }
        List<ItemStack> resetDrops = new ArrayList<>(event.getDrops());
        Boolean graveToken = plugin.getConfig().getBoolean("settings.graveToken");
        if (graveToken) {
            ItemStack token = graveManager.getGraveTokenFromPlayer(event.getEntity());
            if (token != null) {
                Iterator<ItemStack> iterator = event.getDrops().iterator();
                while (iterator.hasNext()) {
                    ItemStack item = iterator.next();
                    item.setAmount(item.getAmount() - 1);
                }
            } else {
                messages.graveTokenNoTokenMessage(event.getEntity());
                return;
            }
        }
        List<ItemStack> newDrops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        Iterator<ItemStack> iterator = newDrops.iterator();
        while (iterator.hasNext()) {
            ItemStack itemStack = iterator.next();
            if (graveManager.shouldIgnore(itemStack)) {
                iterator.remove();
                event.getDrops().add(itemStack);
            }
        }
        List<String> worlds = plugin.getConfig().getStringList("settings.worlds");
        if (worlds.contains(event.getEntity().getLocation().getWorld().getName()) || worlds.contains("ALL")) {
            List<String> graveEntities = plugin.getConfig().getStringList("settings.graveEntities");
            if (graveEntities.contains(event.getEntity().getType().toString()) || graveEntities.contains("ALL")) {
                if (newDrops.size() > 0) {
                    Grave grave = graveManager.createGrave(event.getEntity(), newDrops);
                    if (grave != null) {
                        if (event.getEntity().hasPermission("graves.experience")) {
                            Boolean expStore = plugin.getConfig().getBoolean("settings.expStore");
                            if (expStore) {
                                Integer playerExp = graveManager.getPlayerDropExp(event.getEntity());
                                if (playerExp != null) {
                                    grave.setExperience(playerExp);
                                }
                            } else {
                                grave.setExperience(event.getDroppedExp());
                            }
                            event.setDroppedExp(0);
                            event.setKeepLevel(false);
                        }
                        graveManager.runCreateCommands(grave, event.getEntity());
                    } else {
                        event.getDrops().clear();
                        event.getDrops().addAll(resetDrops);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        List<ItemStack> resetDrops = new ArrayList<>(event.getDrops());
        List<String> graveEntities = plugin.getConfig().getStringList("settings.graveEntities");
        List<String> worlds = plugin.getConfig().getStringList("settings.worlds");
        if (worlds.contains(event.getEntity().getLocation().getWorld().getName()) || worlds.contains("ALL")) {
            if (graveEntities.contains(event.getEntity().getType().toString()) || graveEntities.contains("ALL")) {
                List<ItemStack> newDrops = new ArrayList<>(event.getDrops());
                event.getDrops().clear();
                Iterator<ItemStack> iterator = newDrops.iterator();
                while (iterator.hasNext()) {
                    ItemStack itemStack = iterator.next();
                    if (graveManager.shouldIgnore(itemStack)) {
                        iterator.remove();
                        event.getDrops().add(itemStack);
                    }
                }
                if (newDrops.size() > 0) {
                    Grave grave = graveManager.createGrave(event.getEntity(), newDrops);
                    if (grave != null) {
                        grave.setExperience(event.getDroppedExp());
                        event.setDroppedExp(0);
                    } else {
                        event.getDrops().clear();
                        event.getDrops().addAll(resetDrops);
                    }
                    graveManager.runCreateCommands(grave, event.getEntity());
                }
            }
        }
    }

    @EventHandler
    public void onGraveOpen(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getHand().equals(EquipmentSlot.HAND)) {
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
                graveManager.runOpenCommands(grave, event.getPlayer());
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
                graveManager.runLootCommands(grave, event.getPlayer());
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
                        graveManager.runLootCommands(grave, event.getPlayer());
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
                graveManager.giveExperience(grave, player);
                graveManager.removeHologram(grave);
                graveManager.replaceGrave(grave);
                graveManager.removeGrave(grave);
                messages.graveLoot(grave.getLocation(), player);
                graveManager.runLootCommands(grave, player);
            }
        }
    }

    @EventHandler
    public void onGraveBreak(BlockBreakEvent event) {
        Grave grave = graveManager.getGrave(event.getBlock().getLocation());
        if (grave != null) {
            if (graveManager.hasPermission(grave, event.getPlayer())) {
                graveManager.dropGrave(grave);
                graveManager.dropExperience(grave);
                graveManager.removeHologram(grave);
                graveManager.replaceGrave(grave);
                graveManager.removeGrave(grave);
                graveManager.runBreakCommands(grave, event.getPlayer());
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Grave grave = graveManager.getGrave(block.getLocation());
            if (grave != null) {
                if ((System.currentTimeMillis() - grave.getCreatedTime()) < 1000) {
                    iterator.remove();
                    continue;
                }
                Boolean graveExplode = plugin.getConfig().getBoolean("settings.graveExplode");
                if (graveExplode) {
                    graveManager.dropGrave(grave);
                    graveManager.dropExperience(grave);
                    graveManager.removeHologram(grave);
                    graveManager.replaceGrave(grave);
                    graveManager.removeGrave(grave);
                    graveManager.runExplodeCommands(grave, event.getEntity());
                } else {
                    iterator.remove();
                }
            }
        }
    }

    @EventHandler
    public void onGraveInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getView().getPlayer();
        if (title.startsWith("§5§3§1§6§r§0§r")) {
            if (event.getSlotType().equals(InventoryType.SlotType.CONTAINER)) {
                event.setCancelled(true);
                Boolean graveTeleport = plugin.getConfig().getBoolean("settings.graveTeleport");
                if (event.getCurrentItem() != null) {
                    Boolean graveProtected = plugin.getConfig().getBoolean("settings.graveProtected");
                    if (graveProtected) {
                        if (event.getClick().equals(ClickType.RIGHT)) {
                            Boolean graveProtectedChange = plugin.getConfig().getBoolean("settings.graveProtectedChange");
                            if (graveProtectedChange) {
                                Grave grave = graveManager.getGrave(guiManager.getGraveLocation(event.getCurrentItem()));
                                Long diff = System.currentTimeMillis() - grave.getCreatedTime();
                                if (grave != null) {
                                    if (grave.getProtected() != null && grave.getProtectTime() == null || diff < grave.getProtectTime()) {
                                        if (grave.getProtected()) {
                                            graveManager.protectGrave(grave, false);
                                        } else {
                                            graveManager.protectGrave(grave, true);
                                        }
                                        graveManager.updateHologram(grave);
                                        guiManager.openGraveGUI(player, grave.getPlayer());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    if (graveTeleport) {
                        if (player.hasPermission("graves.teleport")) {
                            guiManager.teleportGrave(player, event.getCurrentItem());
                        } else {
                            messages.permissionDenied(player);
                        }
                    } else {
                        if (player.hasPermission("graves.bypass")) {
                            guiManager.teleportGrave(player, event.getCurrentItem());
                        } else {
                            String graveTeleportDisabled = plugin.getConfig().getString("settings.graveTeleportDisabled")
                                    .replace("&", "§");
                            if (!graveTeleportDisabled.equals("")) {
                                player.sendMessage(graveTeleportDisabled);
                            }
                        }
                    }
                }
            }
        }
    }
}
