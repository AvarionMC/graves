package com.ranull.graves.listeners;

import com.ranull.graves.Graves;
import com.ranull.graves.inventory.GraveInventory;
import com.ranull.graves.inventory.GraveListInventory;
import com.ranull.graves.manager.GUIManager;
import com.ranull.graves.manager.GraveManager;
import com.ranull.graves.manager.MessageManager;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
        Player player = event.getEntity();

        if (!player.hasPermission("graves.place")) {
            return;
        }

        List<String> worlds = plugin.getConfig().getStringList("settings.worlds");
        if (!worlds.contains(Objects.requireNonNull(player.getLocation().getWorld()).getName()) && !worlds.contains("ALL")) {
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.ignoreKeepInventory") &&
                Objects.requireNonNull(player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY))) {
            return;
        }

        if (player.getLastDamageCause() != null && player.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
            if (player.getKiller() != null) {
                if (!plugin.getConfig().getBoolean("settings.createPvP")) {
                    return;
                }
            } else {
                if (!plugin.getConfig().getBoolean("settings.createPvE")) {
                    return;
                }
            }
        } else {
            if (!plugin.getConfig().getBoolean("settings.createEnvironmental")) {
                return;
            }
        }

        int maxGraves = graveManager.getMaxGraves(player);

        if (maxGraves > 0 && graveManager.getPlayerGraves(player.getUniqueId()).size() >= maxGraves) {
            messageManager.maxGraves(player);
            return;
        }

        List<ItemStack> eventDrops = new ArrayList<>(event.getDrops());

        if (plugin.getConfig().getBoolean("settings.token")) {
            ItemStack graveToken = graveManager.getGraveTokenFromPlayer(player);
            if (graveToken != null) {
                graveToken.setAmount(graveToken.getAmount() - 1);
            } else {
                messageManager.tokenNoTokenMessage(player);
                return;
            }
        }

        List<ItemStack> newDrops = new ArrayList<>(event.getDrops());
        Iterator<ItemStack> iterator = newDrops.iterator();

        event.getDrops().clear();

        while (iterator.hasNext()) {
            ItemStack itemStack = iterator.next();

            if (graveManager.shouldIgnore(itemStack)) {
                iterator.remove();
                event.getDrops().add(itemStack);
            }
        }

        List<String> entities = plugin.getConfig().getStringList("settings.entities");
        if (entities.contains(event.getEntity().getType().toString()) || entities.contains("ALL")) {
            if (newDrops.size() > 0) {
                GraveInventory graveInventory = graveManager.createGrave(event.getEntity(), newDrops);
                if (graveInventory != null) {
                    if (event.getEntity().hasPermission("graves.experience")) {
                        if (plugin.getConfig().getBoolean("settings.expStore")) {
                            int playerExp = graveManager.getPlayerDropExp(event.getEntity());
                            if (playerExp > 0) {
                                graveInventory.setExperience(playerExp);
                            }
                        } else {
                            graveInventory.setExperience(event.getDroppedExp());
                        }

                        event.setDroppedExp(0);
                        event.setKeepLevel(false);
                    }

                    graveManager.runCreateCommands(graveInventory, event.getEntity());
                } else {
                    event.getDrops().clear();
                    event.getDrops().addAll(eventDrops);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();

        if (livingEntity instanceof Player) {
            return;
        }

        if (!plugin.getConfig().getBoolean("settings.zombieDrops") &&
                graveManager.isGraveZombie(livingEntity)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }

        List<String> worlds = plugin.getConfig().getStringList("settings.worlds");
        List<String> entities = plugin.getConfig().getStringList("settings.entities");

        List<ItemStack> eventDrops = new ArrayList<>(event.getDrops());

        if (worlds.contains(livingEntity.getWorld().getName()) || worlds.contains("ALL")) {
            if (entities.contains(event.getEntity().getType().toString()) || entities.contains("ALL")) {
                List<ItemStack> newDrops = new ArrayList<>(event.getDrops());
                Iterator<ItemStack> iterator = newDrops.iterator();

                event.getDrops().clear();

                while (iterator.hasNext()) {
                    ItemStack itemStack = iterator.next();
                    if (graveManager.shouldIgnore(itemStack)) {
                        iterator.remove();
                        event.getDrops().add(itemStack);
                    }
                }

                if (newDrops.size() > 0) {
                    GraveInventory graveInventory = graveManager.createGrave(event.getEntity(), newDrops);

                    if (graveInventory != null) {
                        graveInventory.setExperience(event.getDroppedExp());
                        event.setDroppedExp(0);
                    } else {
                        event.getDrops().clear();
                        event.getDrops().addAll(eventDrops);
                    }

                    graveManager.runCreateCommands(graveInventory, event.getEntity());
                }
            }
        }
    }

    @EventHandler
    public void onGraveOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) ||
                player.getGameMode() == GameMode.SPECTATOR ||
                (event.getHand() != null && !event.getHand().equals(EquipmentSlot.HAND))) {
            return;
        }

        if (player.hasPermission("graves.autoloot")) {
            if (event.getPlayer().isSneaking()) {
                return;
            }
        }

        GraveInventory graveInventory = graveManager.getGraveInventory(Objects.requireNonNull(event.getClickedBlock()).getLocation());

        if (graveInventory != null) {
            event.setCancelled(true);

            if (event.getPlayer().hasPermission("graves.open")) {
                if (graveManager.hasPermission(graveInventory, event.getPlayer())) {
                    player.openInventory(graveInventory.getInventory());

                    graveManager.runOpenCommands(graveInventory, event.getPlayer());

                    messageManager.graveOpen(graveInventory.getLocation());
                } else {
                    messageManager.graveProtect(event.getPlayer(), event.getClickedBlock().getLocation());
                }
            } else {
                messageManager.permissionDenied(player);
            }
        }
    }

    @EventHandler
    public void onGraveSneakOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) ||
                player.getGameMode() == GameMode.SPECTATOR ||
                !player.hasPermission("graves.autoloot") ||
                !player.isSneaking()) {
            return;
        }

        GraveInventory graveInventory = graveManager.getGraveInventory(Objects.requireNonNull(event.getClickedBlock()).getLocation());

        if (graveInventory != null) {
            event.setCancelled(true);

            if (graveManager.hasPermission(graveInventory, player)) {
                graveManager.autoLoot(graveInventory, player);
                graveManager.runLootCommands(graveInventory, player);

                messageManager.graveOpen(graveInventory.getLocation());

                if (graveInventory.getItemAmount() <= 0) {
                    if (!plugin.getConfig().getBoolean("settings.zombieOnlyBreak")) {
                        graveManager.graveSpawnZombie(graveInventory, player);
                    }
                }
            } else {
                messageManager.graveProtect(player, graveInventory.getLocation());
            }
        }
    }

    @EventHandler
    public void onHologramOpen(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (player.hasPermission("graves.autoloot")) {
            if (player.isSneaking()) {
                return;
            }
        }

        if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            if (plugin.getConfig().getBoolean("settings.hologramOpen")) {
                GraveInventory graveInventory = graveManager.getGraveFromHologram((ArmorStand) event.getRightClicked());

                if (graveInventory != null) {
                    event.setCancelled(true);

                    if (player.hasPermission("graves.open")) {
                        if (graveManager.hasPermission(graveInventory, player)) {
                            player.openInventory(graveInventory.getInventory());

                            graveManager.runOpenCommands(graveInventory, player);

                            messageManager.graveOpen(graveInventory.getLocation());
                        } else {
                            messageManager.graveProtect(player, graveInventory.getLocation());
                        }
                    } else {
                        messageManager.permissionDenied(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHologramSneakOpen(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (!player.hasPermission("graves.autoloot") || !player.isSneaking()) {
            return;
        }

        if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            if (plugin.getConfig().getBoolean("settings.hologramOpen")) {
                event.setCancelled(true);

                GraveInventory graveInventory = graveManager.getGraveFromHologram((ArmorStand) event.getRightClicked());

                if (graveInventory != null) {
                    if (graveManager.hasPermission(graveInventory, player)) {
                        graveManager.autoLoot(graveInventory, player);
                        graveManager.runLootCommands(graveInventory, player);

                        messageManager.graveOpen(graveInventory.getLocation());

                        if (graveInventory.getItemAmount() <= 0) {
                            if (!plugin.getConfig().getBoolean("settings.zombieOnlyBreak")) {
                                graveManager.graveSpawnZombie(graveInventory, player);
                            }
                        }
                    } else {
                        messageManager.graveProtect(player, graveInventory.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onGraveClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GraveInventory) {
            GraveInventory graveInventory = (GraveInventory) event.getInventory().getHolder();
            messageManager.graveClose(graveInventory.getLocation());

            if (graveInventory.getItemAmount() == 0) {
                Player player = (Player) event.getPlayer();
                graveInventory.getInventory().getViewers().remove(player);

                graveManager.giveExperience(graveInventory, player);
                graveManager.removeHologram(graveInventory);
                graveManager.replaceGrave(graveInventory);
                graveManager.removeGrave(graveInventory);
                graveManager.runLootCommands(graveInventory, player);

                messageManager.graveLoot(graveInventory.getLocation(), player);

                if (plugin.getConfig().getBoolean("settings.zombieOwner")) {
                    if (graveInventory.getPlayer() != null && graveInventory.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                        graveManager.spawnZombie(graveInventory, player);
                        return;
                    }
                }

                if (plugin.getConfig().getBoolean("settings.zombieOther")) {
                    if (graveInventory.getPlayer() != null && !graveInventory.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                        graveManager.spawnZombie(graveInventory, player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onGraveListClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof GraveListInventory) {
            event.setCancelled(true);

            if (event.getCurrentItem() != null) {
                Player player = (Player) event.getView().getPlayer();

                if (plugin.getConfig().getBoolean("settings.protect") &&
                        plugin.getConfig().getBoolean("settings.protectChange") &&
                        event.getClick().equals(ClickType.RIGHT)) {
                    GraveInventory graveInventory = graveManager.getGraveInventory(guiManager.getGraveLocation(event.getCurrentItem()));
                    if (graveInventory != null) {
                        long createdDiff = System.currentTimeMillis() - graveInventory.getCreatedTime();

                        if ((graveInventory.getProtected() && graveInventory.getProtectTime() < 1) ||
                                (createdDiff < graveInventory.getProtectTime())) {
                            graveManager.setGraveProtection(graveInventory, !graveInventory.getProtected());
                            graveManager.updateHologram(graveInventory);

                            guiManager.openGraveGUI(player, graveInventory.getPlayer());
                            return;
                        }
                    }
                }
                if (plugin.getConfig().getBoolean("settings.teleport")) {
                    if (player.hasPermission("graves.teleport")) {
                        guiManager.teleportGrave(player, event.getCurrentItem());
                    } else {
                        messageManager.permissionDenied(player);
                    }
                } else {
                    if (player.hasPermission("graves.bypass")) {
                        guiManager.teleportGrave(player, event.getCurrentItem());
                    } else {
                        String teleportDisabledMessage = Objects.requireNonNull(plugin.getConfig().getString("settings.teleportDisabledMessage"))
                                .replace("&", "§");
                        if (!teleportDisabledMessage.equals("")) {
                            player.sendMessage(teleportDisabledMessage);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        GraveInventory graveInventory = graveManager.getGraveInventory(location);

        if (graveInventory != null) {
            if (plugin.getConfig().getBoolean("settings.break")) {
                if (graveManager.hasPermission(graveInventory, player)) {
                    graveManager.dropGrave(graveInventory);
                    graveManager.dropExperience(graveInventory);
                    graveManager.removeHologram(graveInventory);
                    graveManager.replaceGrave(graveInventory);
                    graveManager.removeGrave(graveInventory);
                    graveManager.runBreakCommands(graveInventory, player);

                    messageManager.graveClose(graveInventory.getLocation());

                    if (plugin.getConfig().getBoolean("settings.zombieOwner")) {
                        if (graveInventory.getPlayer() != null && graveInventory.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                            graveManager.spawnZombie(graveInventory, player);
                            return;
                        }
                    }

                    if (plugin.getConfig().getBoolean("settings.zombieOther")) {
                        if (graveInventory.getPlayer() != null && !graveInventory.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                            graveManager.spawnZombie(graveInventory, player);
                        }
                    }
                } else {
                    messageManager.graveProtect(player, location);
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    // EntityExplodeEvent is called when TNT and other entities explode (but not beds in the nether/end)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveExplodeByEntity(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();

            GraveInventory graveInventory = graveManager.getGraveInventory(block.getLocation());
            if (graveInventory != null) {
                if ((System.currentTimeMillis() - graveInventory.getCreatedTime()) < 1000) {
                    iterator.remove();
                    continue;
                }

                if (plugin.getConfig().getBoolean("settings.explode")) {
                    graveManager.dropGrave(graveInventory);
                    graveManager.dropExperience(graveInventory);
                    graveManager.removeHologram(graveInventory);
                    graveManager.replaceGrave(graveInventory);
                    graveManager.removeGrave(graveInventory);
                    graveManager.runExplodeCommands(graveInventory, event.getEntity());

                    messageManager.graveClose(graveInventory.getLocation());

                    if (plugin.getConfig().getBoolean("settings.zombieExplode")) {
                        graveManager.spawnZombie(graveInventory);
                    }
                } else {
                    iterator.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveExplodeByBlock(BlockExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();

            GraveInventory graveInventory = graveManager.getGraveInventory(block.getLocation());
            if (graveInventory != null) {
                if ((System.currentTimeMillis() - graveInventory.getCreatedTime()) < 1000) {
                    iterator.remove();
                    continue;
                }

                if (plugin.getConfig().getBoolean("settings.explode")) {
                    graveManager.dropGrave(graveInventory);
                    graveManager.dropExperience(graveInventory);
                    graveManager.removeHologram(graveInventory);
                    graveManager.replaceGrave(graveInventory);
                    graveManager.removeGrave(graveInventory);
                    graveManager.runExplodeCommands(graveInventory, event.getBlock());

                    messageManager.graveClose(graveInventory.getLocation());

                    if (plugin.getConfig().getBoolean("settings.zombieExplode")) {
                        graveManager.spawnZombie(graveInventory);
                    }
                } else {
                    iterator.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (graveManager.getGraveInventory(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }

        if (graveManager.hasRecipeData(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (plugin.getConfig().getBoolean("settings.walkOver")) {
            Player player = event.getPlayer();

            if (!player.hasPermission("graves.autoloot") || player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            GraveInventory graveInventory = graveManager.getGraveInventory(player.getLocation());

            if (graveInventory == null) {
                graveInventory = graveManager.getGraveInventory(player.getLocation().clone().subtract(0, 1, 0));
            }

            if (graveInventory != null) {
                if (graveManager.hasPermission(graveInventory, player)) {
                    graveManager.autoLoot(graveInventory, player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (graveManager.getGraveInventory(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveBreakOther(BlockFromToEvent event) {
        if (graveManager.getGraveInventory(event.getToBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (graveManager.getGraveInventory(event.getBlock().getRelative(event.getDirection()).getLocation()) != null) {
            event.setCancelled(true);
        }
    }
}
