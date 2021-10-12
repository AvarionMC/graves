package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.*;
import net.milkbowl.vault.economy.Economy;
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
    private PlaceholderAPI placeholder;

    public IntegrationManager(Graves plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        if (furnitureLib != null) {
            furnitureLib.unregister();
        }

        loadVault();
        loadWorldEdit();
        loadWorldGuard();
        //loadGriefDefender();
        loadPlaceholderAPI();
        loadFurnitureLib();
        loadProtectionLib();
        loadItemsAdder();
    }

    public void onDisable() {
        if (furnitureLib != null) {
            furnitureLib.unregister();
        }

        if (placeholder != null) {
            placeholder.unregister();
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

    public FurnitureLib getFurnitureLib() {
        return furnitureLib;
    }

    public ProtectionLib getProtectionLib() {
        return protectionLib;
    }

    public ItemsAdder getItemsAdder() {
        return itemsAdder;
    }

    public PlaceholderAPI getPlaceholder() {
        return placeholder;
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

                    worldEdit = new WorldEdit(plugin);

                    plugin.integrationMessage("Hooked into " + worldEditPlugin.getName() + " "
                            + worldEditPlugin.getDescription().getVersion() + ".");
                } catch (ClassNotFoundException ignored) {
                    plugin.integrationMessage("Outdated WorldEdit detected, Only WorldEdit 7+ is supported. " +
                            "Disabling WorldEdit support.");
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
                    Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagConflictException",
                            false, getClass().getClassLoader());

                    worldGuard = new WorldGuard(plugin);

                    plugin.integrationMessage("Hooked into " + worldGuardPlugin.getName() + " "
                            + worldGuardPlugin.getDescription().getVersion() + ".");
                } catch (ClassNotFoundException ignored) {
                    plugin.integrationMessage("Outdated WorldGuard detected, Only WorldEdit 6.2+ is supported. " +
                            "Disabling WorldGuard support.");
                }
            }
        } else {
            worldGuard = null;
        }
    }

    public void loadGriefDefender() {
        if (plugin.getConfig().getBoolean("settings.integration.griefdefender.enabled")) {
            Plugin griefDefenderPlugin = plugin.getServer().getPluginManager().getPlugin("GriefDefender");

            if (griefDefenderPlugin != null) {
                griefDefender = new GriefDefender();

                griefDefender.registerFlag();

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
                itemsAdder = new ItemsAdder(plugin);

                plugin.integrationMessage("Hooked into " + itemsAdderPlugin.getName() + " "
                        + itemsAdderPlugin.getDescription().getVersion() + ".");
            }
        } else {
            itemsAdder = null;
        }
    }

    private void loadPlaceholderAPI() {
        if (placeholder != null) {
            placeholder.unregister();
        }

        if (plugin.getConfig().getBoolean("settings.integration.placeholderapi.enabled")) {
            Plugin placeholderAPIPlugin = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");

            if (placeholderAPIPlugin != null && placeholderAPIPlugin.isEnabled()) {
                placeholder = new PlaceholderAPI(plugin);

                placeholder.register();

                plugin.integrationMessage("Hooked into " + placeholderAPIPlugin.getName() + " "
                        + placeholderAPIPlugin.getDescription().getVersion() + ".");
            }
        } else {
            placeholder = null;
        }
    }
}