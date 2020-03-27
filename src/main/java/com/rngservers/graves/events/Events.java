package com.rngservers.graves.events;

import com.rngservers.graves.Graves;
import com.rngservers.graves.inventory.GraveInventory;
import com.rngservers.graves.manager.GraveManager;
import com.rngservers.graves.inventory.GraveListInventory;
import com.rngservers.graves.manager.MessageManager;
import com.rngservers.graves.manager.GUIManager;
import org.bukkit.GameRule;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Events implements Listener {
    private Graves plugin;
    private GraveManager graveManager;
    private GUIManager guiManager;
    private MessageManager messageManager;

    public Events(Graves plugin, GraveManager graveManager, GUIManager guiManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
        this.guiManager = guiManager;
        this.messageManager = messageManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().hasPermission("graves.place")) {
            return;
        }
        List<String> worlds = plugin.getConfig().getStringList("settings.worlds");
        if (!worlds.contains(event.getEntity().getLocation().getWorld().getName()) && !worlds.contains("ALL")) {
            return;
        }
        if (!plugin.getConfig().getBoolean("settings.ignoreKeepInventory")) {
            if (event.getEntity().getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY)) {
                return;
            }
        }
        if (event.getEntity().getLastDamageCause() != null && event.getEntity().getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
            if (event.getEntity() != null && event.getEntity().getKiller() instanceof Player) {
                Boolean graveCreatePvP = plugin.getConfig().getBoolean("settings.graveCreatePvP");
                if (!graveCreatePvP) {
                    return;
                }
            } else {
                Boolean graveCreatePvE = plugin.getConfig().getBoolean("settings.graveCreatePvE");
                if (!graveCreatePvE) {
                    return;
                }
            }
        } else {
            Boolean graveCreateEnvironmental = plugin.getConfig().getBoolean("settings.graveCreateEnvironmental");
            if (!graveCreateEnvironmental) {
                return;
            }
        }
        Integer graveMax = graveManager.getMaxGraves(event.getEntity());
        if (graveMax > 0) {
            if (graveManager.getGraves(event.getEntity()).size() >= graveMax) {
                messageManager.graveMax(event.getEntity());
                return;
            }
        }
        List<ItemStack> resetDrops = new ArrayList<>(event.getDrops());
        Boolean graveToken = plugin.getConfig().getBoolean("settings.graveToken");
        if (graveToken) {
            ItemStack token = graveManager.getGraveTokenFromPlayer(event.getEntity());
            if (token != null) {
                token.setAmount(token.getAmount() - 1);
            } else {
                messageManager.graveTokenNoTokenMessage(event.getEntity());
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
        List<String> graveEntities = plugin.getConfig().getStringList("settings.graveEntities");
        if (graveEntities.contains(event.getEntity().getType().toString()) || graveEntities.contains("ALL")) {
            if (newDrops.size() > 0) {
                GraveInventory grave = graveManager.createGrave(event.getEntity(), newDrops);
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        Boolean graveZombieDrops = plugin.getConfig().getBoolean("settings.graveZombieDrops");
        if (!graveZombieDrops) {
            if (graveManager.isGraveZombie(event.getEntity())) {
                event.getDrops().clear();
                event.setDroppedExp(0);
                return;
            }
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
                    GraveInventory grave = graveManager.createGrave(event.getEntity(), newDrops);
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
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || !event.getHand().equals(EquipmentSlot.HAND)) {
            return;
        }
        if (event.getPlayer().hasPermission("graves.autoloot")) {
            if (event.getPlayer().isSneaking()) {
                return;
            }
        }
        GraveInventory grave = graveManager.getGrave(event.getClickedBlock().getLocation());
        if (grave != null) {
            if (!event.getPlayer().hasPermission("graves.open")) {
                messageManager.permissionDenied(event.getPlayer());
                event.setCancelled(true);
                return;
            }
            if (graveManager.hasPermission(grave, event.getPlayer())) {
                event.getPlayer().openInventory(grave.getInventory());
                messageManager.graveOpen(grave.getLocation());
                graveManager.runOpenCommands(grave, event.getPlayer());
            } else {
                messageManager.graveProtected(event.getPlayer(), event.getClickedBlock().getLocation());
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
        GraveInventory grave = graveManager.getGrave(event.getClickedBlock().getLocation());
        if (grave != null) {
            if (graveManager.hasPermission(grave, event.getPlayer())) {
                graveManager.autoLoot(grave, event.getPlayer());
                messageManager.graveOpen(grave.getLocation());
                graveManager.runLootCommands(grave, event.getPlayer());
                if (grave.getItemAmount() <= 0) {
                    Boolean graveZombieOnlyBreak = plugin.getConfig().getBoolean("settings.graveZombieOnlyBreak");
                    if (!graveZombieOnlyBreak) {
                        graveManager.graveSpawnZombie(grave, event.getPlayer());
                    }
                }
            } else {
                messageManager.graveProtected(event.getPlayer(), grave.getLocation());
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
                GraveInventory grave = graveManager.getGraveFromHologram(armorStand);
                if (grave != null) {
                    if (!event.getPlayer().hasPermission("graves.open")) {
                        messageManager.permissionDenied(event.getPlayer());
                        event.setCancelled(true);
                        return;
                    }
                    if (graveManager.hasPermission(grave, event.getPlayer())) {
                        event.getPlayer().openInventory(grave.getInventory());
                        messageManager.graveOpen(grave.getLocation());
                        graveManager.runOpenCommands(grave, event.getPlayer());
                    } else {
                        messageManager.graveProtected(event.getPlayer(), grave.getLocation());
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
                GraveInventory grave = graveManager.getGraveFromHologram(armorStand);
                if (grave != null) {
                    if (graveManager.hasPermission(grave, event.getPlayer())) {
                        graveManager.autoLoot(grave, event.getPlayer());
                        messageManager.graveOpen(grave.getLocation());
                        graveManager.runLootCommands(grave, event.getPlayer());
                        if (grave.getItemAmount() <= 0) {
                            Boolean graveZombieOnlyBreak = plugin.getConfig().getBoolean("settings.graveZombieOnlyBreak");
                            if (!graveZombieOnlyBreak) {
                                graveManager.graveSpawnZombie(grave, event.getPlayer());
                            }
                        }
                    } else {
                        messageManager.graveProtected(event.getPlayer(), grave.getLocation());
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onGraveClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GraveInventory) {
            GraveInventory grave = (GraveInventory) event.getInventory().getHolder();
            messageManager.graveClose(grave.getLocation());
            if (grave.getItemAmount() == 0) {
                Player player = (Player) event.getPlayer();
                grave.getInventory().getViewers().remove(player);
                graveManager.giveExperience(grave, player);
                graveManager.removeHologram(grave);
                graveManager.replaceGrave(grave);
                graveManager.removeGrave(grave);
                messageManager.graveLoot(grave.getLocation(), player);
                graveManager.runLootCommands(grave, player);
                Boolean graveZombieOwner = plugin.getConfig().getBoolean("settings.graveZombieOwner");
                if (graveZombieOwner) {
                    if (grave.getPlayer() != null && grave.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                        graveManager.spawnZombie(grave, event.getPlayer());
                        return;
                    }
                }
                Boolean graveZombieOther = plugin.getConfig().getBoolean("settings.graveZombieOther");
                if (graveZombieOther) {
                    if (grave.getPlayer() != null && !grave.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                        graveManager.spawnZombie(grave, event.getPlayer());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onGraveBreak(BlockBreakEvent event) {
        GraveInventory grave = graveManager.getGrave(event.getBlock().getLocation());
        if (grave != null) {
            if (graveManager.hasPermission(grave, event.getPlayer())) {
                graveManager.dropGrave(grave);
                graveManager.dropExperience(grave);
                graveManager.removeHologram(grave);
                graveManager.replaceGrave(grave);
                graveManager.removeGrave(grave);
                graveManager.runBreakCommands(grave, event.getPlayer());
                messageManager.graveClose(grave.getLocation());
                Boolean graveZombieOwner = plugin.getConfig().getBoolean("settings.graveZombieOwner");
                if (graveZombieOwner) {
                    if (grave.getPlayer() != null && grave.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                        graveManager.spawnZombie(grave, event.getPlayer());
                        return;
                    }
                }
                Boolean graveZombieOther = plugin.getConfig().getBoolean("settings.graveZombieOther");
                if (graveZombieOther) {
                    if (grave.getPlayer() != null && !grave.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                        graveManager.spawnZombie(grave, event.getPlayer());
                    }
                }
            } else {
                messageManager.graveProtected(event.getPlayer(), event.getBlock().getLocation());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        GraveInventory grave = graveManager.getGrave(event.getBlock().getLocation());
        if (grave != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        GraveInventory grave = graveManager.getGrave(event.getBlock().getLocation());
        if (grave != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveBreakNaturally(BlockFromToEvent event) {
        GraveInventory grave = graveManager.getGrave(event.getToBlock().getLocation());
        if (grave != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtendGrave(BlockPistonExtendEvent event) {
        GraveInventory grave = graveManager.getGrave(event.getBlock().getRelative(event.getDirection()).getLocation());
        if (grave != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            GraveInventory grave = graveManager.getGrave(block.getLocation());
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
                    messageManager.graveClose(grave.getLocation());
                    Boolean graveZombieExplode = plugin.getConfig().getBoolean("settings.graveZombieExplode");
                    if (graveZombieExplode) {
                        graveManager.spawnZombie(grave);
                    }
                } else {
                    iterator.remove();
                }
            }
        }
    }

    @EventHandler
    public void onGraveListClick(InventoryClickEvent event) {
        Player player = (Player) event.getView().getPlayer();
        if (event.getInventory() != null && event.getInventory().getHolder() instanceof GraveListInventory) {
            event.setCancelled(true);
        }
        if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof GraveListInventory) {
            event.setCancelled(true);
            Boolean graveTeleport = plugin.getConfig().getBoolean("settings.graveTeleport");
            if (event.getCurrentItem() != null) {
                Boolean graveProtected = plugin.getConfig().getBoolean("settings.graveProtected");
                if (graveProtected) {
                    if (event.getClick().equals(ClickType.RIGHT)) {
                        Boolean graveProtectedChange = plugin.getConfig().getBoolean("settings.graveProtectedChange");
                        if (graveProtectedChange) {
                            GraveInventory grave = graveManager.getGrave(guiManager.getGraveLocation(event.getCurrentItem()));
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
                        messageManager.permissionDenied(player);
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
