package com.ranull.graves;

import com.ranull.graves.command.GravesCommand;
import com.ranull.graves.compatibility.Compatibility;
import com.ranull.graves.compatibility.CompatibilityBlockData;
import com.ranull.graves.compatibility.CompatibilityMaterialData;
import com.ranull.graves.integration.*;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.listener.*;
import com.ranull.graves.manager.*;
import com.ranull.graves.update.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Graves extends JavaPlugin {
    private VersionManager versionManager;
    private DataManager dataManager;
    private BlockManager blockManager;
    private HologramManager hologramManager;
    private GUIManager guiManager;
    private RecipeManager recipeManager;
    private PlayerManager playerManager;
    private LocationManager locationManager;
    private EntityManager entityManager;
    private GraveManager graveManager;
    private IntegrationManager integrationManager;
    private Compatibility compatibility;

    @Override
    public void onEnable() {
        integrationManager.reload();

        versionManager = new VersionManager(this);
        dataManager = new DataManager(this, DataManager.Type.SQLITE);
        blockManager = new BlockManager(this);
        hologramManager = new HologramManager(this);
        guiManager = new GUIManager(this);
        playerManager = new PlayerManager(this);
        locationManager = new LocationManager(this);
        entityManager = new EntityManager(this);
        graveManager = new GraveManager(this);

        if (versionManager.hasPersistentData()) {
            recipeManager = new RecipeManager(this);
        }

        updateChecker();
        configChecker();

        new Metrics(this, 12849);
        PluginCommand pluginCommand = getCommand("graves");

        if (pluginCommand != null) {
            pluginCommand.setExecutor(new GravesCommand(this));
        }

        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerBucketEmptyListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockFromToListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPistonExtendListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryDragListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryOpenListener(this), this);
        getServer().getPluginManager().registerEvents(new CreatureSpawnListener(this), this);

        if (!versionManager.is_v1_7()) {
            getServer().getPluginManager().registerEvents(new PlayerInteractAtEntityListener(this), this);
        }

        if (!versionManager.is_v1_7() && !versionManager.is_v1_8()) {
            getServer().getPluginManager().registerEvents(new BlockExplodeListener(this), this);
        }

        //getServer().getPluginManager().registerEvents(new GraveTestListener(this), this); // Test Listener
    }

    @Override
    public void onDisable() {
        graveManager.onDisable();
        dataManager.onDisable();
        integrationManager.onDisable();

        if (recipeManager != null) {
            recipeManager.onDisable();
        }
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();

        integrationManager = new IntegrationManager(this);

        integrationManager.loadWorldGuard();
    }

    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        configChecker();
        updateChecker();
        dataManager.reload();
        recipeManager.reload();
        integrationManager.reload();
        infoMessage("Config reloaded.");
    }

    public void debugMessage(String string, int level) {
        int debug = getConfig().getInt("settings.debug", 0);

        if (debug >= level) {
            getLogger().info("Debug: " + string);
        }
    }

    public void warningMessage(String string) {
        getLogger().info("Warning: " + string);
    }

    public void infoMessage(String string) {
        getLogger().info("Info: " + string);
    }

    public void testMessage(String string) {
        getLogger().info("Test: " + string);
    }

    public void updateMessage(String string) {
        getLogger().info("Update: " + string);
    }

    public void integrationMessage(String string) {
        getLogger().info("Integration: " + string);
    }

    private void updateChecker() {
        String response = new UpdateChecker(this, 74208).getVersion();

        if (response != null) {
            try {
                double pluginVersion = Double.parseDouble(getDescription().getVersion());
                double pluginVersionLatest = Double.parseDouble(response);

                if (pluginVersion < pluginVersionLatest) {
                    updateMessage("Outdated version detected " + pluginVersion + ", latest version is "
                            + pluginVersionLatest + ", https://www.spigotmc.org/resources/graves.74208/");
                }
            } catch (NumberFormatException exception) {
                if (!getDescription().getVersion().equalsIgnoreCase(response)) {
                    updateMessage("Outdated version detected " + getDescription().getVersion()
                            + ", latest version is " + response + ", https://www.spigotmc.org/resources/graves.74208/");
                }
            }
        }
    }

    private void configChecker() {
        double currentConfigVersion = 2;
        double configVersion = getConfig().getDouble("config-version");

        if (configVersion < currentConfigVersion) {
            warningMessage("Outdated config detected (v" + configVersion + "), this version is out of date, " +
                    "current version is (v" + currentConfigVersion + "), please rename or delete your current config to " +
                    "regenerate it, if you choose not to default options may be applied and current confirmation changes " +
                    "might not work.");
        }

        if (versionManager.hasBlockData()) {
            compatibility = new CompatibilityBlockData();
        } else {
            compatibility = new CompatibilityMaterialData();

            infoMessage("Legacy version detected, Graves will run but may have problems with material names, " +
                    "the default config is setup for the latest version of the game, you can alter the config manually to fix " +
                    "any issues you encounter, you will need to find the names of materials and sounds for your version.");
        }
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public GraveManager getGraveManager() {
        return graveManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public BlockManager getBlockManager() {
        return blockManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public Compatibility getCompatibility() {
        return compatibility;
    }

    public boolean hasVault() {
        return integrationManager.getVault() != null;
    }

    public boolean hasWorldEdit() {
        return integrationManager.getWorldEdit() != null;
    }

    public boolean hasWorldGuard() {
        return integrationManager.getWorldGuard() != null;
    }

    public boolean hasFurnitureLib() {
        return integrationManager.getFurnitureLib() != null;
    }

    public boolean hasProtectionLib() {
        return integrationManager.getProtectionLib() != null;
    }

    public boolean hasItemsAdder() {
        return integrationManager.getItemsAdder() != null;
    }

    public boolean hasPlaceholderAPI() {
        return integrationManager.getPlaceholder() != null;
    }

    public Vault getVault() {
        return integrationManager.getVault();
    }

    public WorldEdit getWorldEdit() {
        return integrationManager.getWorldEdit();
    }

    public WorldGuard getWorldGuard() {
        return integrationManager.getWorldGuard();
    }

    public FurnitureLib getFurnitureLib() {
        return integrationManager.getFurnitureLib();
    }

    public ItemsAdder getItemsAdder() {
        return integrationManager.getItemsAdder();
    }

    public ProtectionLib getProtectionLib() {
        return integrationManager.getProtectionLib();
    }

    public List<String> getPermissionList(Entity entity) {
        List<String> permissionList = new ArrayList<>();
        List<String> permissionListSorted = new ArrayList<>();

        if (entity instanceof Player) {
            Player player = (Player) entity;

            for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
                if (permissionAttachmentInfo.getPermission().startsWith("graves.permission.")) {
                    String permission = permissionAttachmentInfo.getPermission()
                            .replace("graves.permission.", "").toLowerCase();

                    if (getConfig().isConfigurationSection("settings.permission." + permission)) {
                        permissionList.add(permission);
                    }
                }
            }

            ConfigurationSection configurationSection = getConfig().getConfigurationSection("settings.permission");

            if (configurationSection != null) {
                for (String permission : configurationSection.getKeys(false)) {
                    if (permissionList.contains(permission)) {
                        permissionListSorted.add(permission);
                    }
                }
            }
        }

        return permissionListSorted;
    }

    public ConfigurationSection getConfig(String config, Grave grave) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList());
    }

    public ConfigurationSection getConfig(String config, Entity entity) {
        return getConfig(config, entity.getType(), getPermissionList(entity));
    }

    public ConfigurationSection getConfig(String config, Entity entity, List<String> permissionList) {
        return getConfig(config, entity.getType(), permissionList);
    }

    public ConfigurationSection getConfig(String config, EntityType entityType, List<String> permissionList) {
        if (permissionList != null && !permissionList.isEmpty()) {
            for (String permission : permissionList) {
                String section = "settings.permission." + permission;

                if (getConfig().isConfigurationSection(section)) {
                    ConfigurationSection configurationSection = getConfig().getConfigurationSection(section);

                    if (configurationSection != null && (versionManager.hasConfigContains()
                            ? configurationSection.contains(config, true)
                            : configurationSection.contains(config))) {
                        return configurationSection;
                    }
                }
            }
        }

        if (entityType != null) {
            String section = "settings.entity." + entityType.name();

            if (getConfig().isConfigurationSection(section)) {
                ConfigurationSection configurationSection = getConfig().getConfigurationSection(section);

                if (configurationSection != null && (versionManager.hasConfigContains()
                        ? configurationSection.contains(config, true)
                        : configurationSection.contains(config))) {
                    return configurationSection;
                }
            }
        }

        return getConfig().getConfigurationSection("settings.default.default");
    }
}
