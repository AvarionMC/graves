package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class IntegrationManager {
    private final Graves plugin;
    private Vault vault;
    private WorldEdit worldEdit;
    private WorldGuard worldGuard;
    private GriefDefender griefDefender;
    private FurnitureLib furnitureLib;
    private ProtectionLib protectionLib;
    private ItemsAdder itemsAdder;
    private Oraxen oraxen;
    private ChestSort chestSort;
    private ItemBridge itemBridge;
    private PlaceholderAPI placeholderAPI;

    public IntegrationManager(Graves plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        onDisable();
        loadVault();
        loadWorldEdit();
        loadWorldGuard();
        //loadGriefDefender();
        loadPlaceholderAPI();
        loadFurnitureLib();
        loadProtectionLib();
        loadItemsAdder();
        loadOraxen();
        loadChestSort();
        loadItemBridge();
        loadCompatibilityWarnings();
    }

    public void onDisable() {
        if (furnitureLib != null) {
            furnitureLib.unregister();
        }

        if (oraxen != null) {
            oraxen.unregister();
        }

        if (placeholderAPI != null) {
            placeholderAPI.unregister();
        }
    }

    public Vault getVault() {
        return vault;
    }

    public WorldEdit getWorldEdit() {
        return worldEdit;
    }

    public WorldGuard getWorldGuard() {
        return worldGuard;
    }

    public GriefDefender getGriefDefender() {
        return griefDefender;
    }

    public FurnitureLib getFurnitureLib() {
        return furnitureLib;
    }

    public ProtectionLib getProtectionLib() {
        return protectionLib;
    }

    public ItemsAdder getItemsAdder() {
        return itemsAdder;
    }

    public Oraxen getOraxen() {
        return oraxen;
    }

    public ChestSort getChestSort() {
        return chestSort;
    }

    public PlaceholderAPI getPlaceholderAPI() {
        return placeholderAPI;
    }

    private void loadVault() {
        if (plugin.getConfig().getBoolean("settings.integration.vault.enabled")) {
            Plugin vaultPlugin = plugin.getServer().getPluginManager().getPlugin("Vault");

            if (vaultPlugin != null && vaultPlugin.isEnabled()) {
                RegisteredServiceProvider<Economy> registeredServiceProvider = plugin.getServer().getServicesManager()
                        .getRegistration(Economy.class);

                if (registeredServiceProvider != null) {
                    vault = new Vault(registeredServiceProvider.getProvider());

                    plugin.integrationMessage("Hooked into " + vaultPlugin.getName() + " "
                            + vaultPlugin.getDescription().getVersion() + ".");
                }
            }
        } else {
            vault = null;
        }
    }

    private void loadWorldEdit() {
        if (plugin.getConfig().getBoolean("settings.integration.worldedit.enabled")) {
            Plugin worldEditPlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");

            if (worldEditPlugin != null && worldEditPlugin.isEnabled()) {
                try {
                    Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats", false, getClass()
                            .getClassLoader());

                    worldEdit = new WorldEdit(plugin, worldEditPlugin);

                    plugin.integrationMessage("Hooked into " + worldEditPlugin.getName() + " "
                            + worldEditPlugin.getDescription().getVersion() + ".");
                } catch (ClassNotFoundException ignored) {
                    plugin.integrationMessage(worldEditPlugin.getName() + " "
                            + worldEditPlugin.getDescription().getVersion()
                            + " detected, Only WorldEdit 7+ is supported.Disabling WorldEdit support.");
                }
            }
        } else {
            worldEdit = null;
        }
    }

    public void loadWorldGuard() {
        if (plugin.getConfig().getBoolean("settings.integration.worldguard.enabled")) {
            Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

            if (worldGuardPlugin != null) {
                try {
                    Class.forName("com.sk89q.worldguard.WorldGuard", false, getClass().getClassLoader());
                    Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagConflictException",
                            false, getClass().getClassLoader());

                    worldGuard = new WorldGuard(plugin);

                    plugin.integrationMessage("Hooked into " + worldGuardPlugin.getName() + " "
                            + worldGuardPlugin.getDescription().getVersion() + ".");
                } catch (ClassNotFoundException ignored) {
                    plugin.integrationMessage(worldGuardPlugin.getName() + " "
                            + worldGuardPlugin.getDescription().getVersion()
                            + " detected, Only WorldGuard 6.2+ is supported. Disabling WorldGuard support.");
                }
            }
        } else {
            worldGuard = null;
        }
    }

    public void loadGriefDefender() {
        if (plugin.getConfig().getBoolean("settings.integration.griefdefender.enabled")) {
            Plugin griefDefenderPlugin = plugin.getServer().getPluginManager().getPlugin("GriefDefender");

            if (griefDefenderPlugin != null && griefDefenderPlugin.isEnabled()) {
                griefDefender = new GriefDefender();

                //griefDefender.registerFlag();

                plugin.integrationMessage("Hooked into " + griefDefenderPlugin.getName() + " "
                        + griefDefenderPlugin.getDescription().getVersion() + ".");
            }
        } else {
            griefDefender = null;
        }
    }

    public void loadFurnitureLib() {
        if (plugin.getConfig().getBoolean("settings.integration.furniturelib.enabled")) {
            Plugin furnitureLibPlugin = plugin.getServer().getPluginManager().getPlugin("FurnitureLib");

            if (furnitureLibPlugin != null && furnitureLibPlugin.isEnabled()) {
                furnitureLib = new FurnitureLib(plugin);

                plugin.integrationMessage("Hooked into " + furnitureLibPlugin.getName() + " "
                        + furnitureLibPlugin.getDescription().getVersion() + ".");
            }
        } else {
            furnitureLib = null;
        }
    }

    public void loadProtectionLib() {
        if (plugin.getConfig().getBoolean("settings.integration.protectionlib.enabled")) {
            Plugin protectionLibPlugin = plugin.getServer().getPluginManager().getPlugin("ProtectionLib");

            if (protectionLibPlugin != null && protectionLibPlugin.isEnabled()) {
                protectionLib = new ProtectionLib(plugin, protectionLibPlugin);

                plugin.integrationMessage("Hooked into " + protectionLibPlugin.getName() + " "
                        + protectionLibPlugin.getDescription().getVersion() + ".");
            }
        } else {
            protectionLib = null;
        }
    }

    public void loadItemsAdder() {
        if (plugin.getConfig().getBoolean("settings.integration.itemsadder.enabled")) {
            Plugin itemsAdderPlugin = plugin.getServer().getPluginManager().getPlugin("ItemsAdder");

            if (itemsAdderPlugin != null && itemsAdderPlugin.isEnabled()) {
                itemsAdder = new ItemsAdder(plugin, itemsAdderPlugin);

                plugin.integrationMessage("Hooked into " + itemsAdderPlugin.getName() + " "
                        + itemsAdderPlugin.getDescription().getVersion() + ".");
            }
        } else {
            itemsAdder = null;
        }
    }

    public void loadOraxen() {
        if (plugin.getConfig().getBoolean("settings.integration.oraxen.enabled")) {
            Plugin oraxenPlugin = plugin.getServer().getPluginManager().getPlugin("Oraxen");

            if (oraxenPlugin != null && oraxenPlugin.isEnabled()) {
                oraxen = new Oraxen(plugin, oraxenPlugin);

                plugin.integrationMessage("Hooked into " + oraxenPlugin.getName() + " "
                        + oraxenPlugin.getDescription().getVersion() + ".");
            }
        } else {
            oraxen = null;
        }
    }

    public void loadChestSort() {
        if (plugin.getConfig().getBoolean("settings.integration.chestsort.enabled")) {
            Plugin chestSortPlugin = plugin.getServer().getPluginManager().getPlugin("ChestSort");

            if (chestSortPlugin != null && chestSortPlugin.isEnabled()) {
                chestSort = new ChestSort();

                plugin.integrationMessage("Hooked into " + chestSortPlugin.getName() + " "
                        + chestSortPlugin.getDescription().getVersion() + ".");
            }
        } else {
            chestSort = null;
        }
    }

    public void loadItemBridge() {
        if (plugin.getConfig().getBoolean("settings.integration.itembridge.enabled")) {
            Plugin itemBridgePlugin = plugin.getServer().getPluginManager().getPlugin("ItemBridge");

            if (itemBridgePlugin != null && itemBridgePlugin.isEnabled()) {
                if (itemBridge == null) {
                    itemBridge = new ItemBridge(plugin);
                }

                plugin.integrationMessage("Hooked into " + itemBridgePlugin.getName() + " "
                        + itemBridgePlugin.getDescription().getVersion() + ".");
            }
        } else {
            itemBridge = null;
        }
    }

    private void loadPlaceholderAPI() {
        if (placeholderAPI != null) {
            placeholderAPI.unregister();
        }

        if (plugin.getConfig().getBoolean("settings.integration.placeholderapi.enabled")) {
            Plugin placeholderAPIPlugin = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");

            if (placeholderAPIPlugin != null && placeholderAPIPlugin.isEnabled()) {
                placeholderAPI = new PlaceholderAPI(plugin);

                placeholderAPI.register();

                plugin.integrationMessage("Hooked into " + placeholderAPIPlugin.getName() + " "
                        + placeholderAPIPlugin.getDescription().getVersion() + ".");
            }
        } else {
            placeholderAPI = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void loadCompatibilityWarnings() {
        if (plugin.getConfig().getBoolean("settings.compatibility.warning")) {
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getGameRuleValue("keepInventory").equals("true")) {
                    plugin.compatibilityMessage("World \"" + world.getName()
                            + "\" has keepInventory set to true, Graves will not be created here.");
                }
            }

            Plugin essentialsPlugin = plugin.getServer().getPluginManager().getPlugin("Essentials");

            if (essentialsPlugin != null && essentialsPlugin.isEnabled()) {
                plugin.compatibilityMessage(essentialsPlugin.getName()
                        + " Detected, make sure you don't have the essentials.keepinv or essentials.keepxp permissions.");
            }

            similarPluginWarning("DeadChest");
            similarPluginWarning("DeathChest");
            similarPluginWarning("DeathChestPro");
            similarPluginWarning("SavageDeathChest");
            similarPluginWarning("AngelChest");
        }
    }

    private void similarPluginWarning(String string) {
        Plugin similarPlugin = plugin.getServer().getPluginManager().getPlugin(string);

        if (similarPlugin != null && similarPlugin.isEnabled()) {
            plugin.compatibilityMessage(string + " Detected, Graves listens to the death event after " + string +
                    ", and " + string + " clears the drop list. This means Graves will never be created for players " +
                    "if " + string + " is enabled, only non-player entities will create Graves if configured to do so.");
        }
    }
}