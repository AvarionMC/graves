package com.ranull.graves;

import com.google.common.base.Charsets;
import com.ranull.graves.command.GravesCommand;
import com.ranull.graves.command.GraveyardsCommand;
import com.ranull.graves.compatibility.Compatibility;
import com.ranull.graves.compatibility.CompatibilityBlockData;
import com.ranull.graves.compatibility.CompatibilityMaterialData;
import com.ranull.graves.listener.*;
import com.ranull.graves.manager.*;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.*;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

public class Graves extends JavaPlugin {
    private VersionManager versionManager;
    private IntegrationManager integrationManager;
    private CacheManager cacheManager;
    private DataManager dataManager;
    private ImportManager importManager;
    private BlockManager blockManager;
    private ItemStackManager itemStackManager;
    private EntityDataManager entityDataManager;
    private HologramManager hologramManager;
    private GUIManager guiManager;
    private EntityManager entityManager;
    private RecipeManager recipeManager;
    private LocationManager locationManager;
    private GraveManager graveManager;
    private GraveyardManager graveyardManager;
    private Compatibility compatibility;
    private FileConfiguration fileConfiguration;

    @Override
    public void onLoad() {
        saveDefaultConfig();

        integrationManager = new IntegrationManager(this);

        integrationManager.loadWorldGuard();
    }

    @Override
    public void onEnable() {
        integrationManager.load();

        versionManager = new VersionManager();
        cacheManager = new CacheManager();
        dataManager = new DataManager(this);
        importManager = new ImportManager(this);
        blockManager = new BlockManager(this);
        itemStackManager = new ItemStackManager(this);
        entityDataManager = new EntityDataManager(this);
        hologramManager = new HologramManager(this);
        guiManager = new GUIManager(this);
        entityManager = new EntityManager(this);
        locationManager = new LocationManager(this);
        graveManager = new GraveManager(this);
        graveyardManager = new GraveyardManager(this);

        registerMetrics();
        registerCommands();
        registerListeners();
        registerRecipes();
        saveTextFiles();

        getServer().getScheduler().runTask(this, () -> {
            compatibilityChecker();
            updateConfig();
            updateChecker();
        });
    }

    @Override
    public void onDisable() {
        dataManager.closeConnection();
        graveManager.unload();
        graveyardManager.unload();
        integrationManager.unload();

        if (recipeManager != null) {
            recipeManager.unload();
        }
    }

    @Override
    public void saveDefaultConfig() {
        ResourceUtil.copyResources("config", getConfigFolder().getPath(), false, this);
    }

    @Override
    public void reloadConfig() {
        File singleConfigFile = new File(getDataFolder(), "config.yml");

        if (!singleConfigFile.exists()) {
            fileConfiguration = getConfigFiles(getConfigFolder());
        } else {
            fileConfiguration = getConfigFile(singleConfigFile);
            loadResourceDefaults(fileConfiguration, singleConfigFile.getName());
        }
    }

    @Override
    @NotNull
    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            reloadConfig();
        }

        return fileConfiguration;
    }

    public void reload() {
        saveDefaultConfig();
        saveTextFiles();
        reloadConfig();
        updateConfig();
        unregisterListeners();
        registerListeners();
        dataManager.reload();
        integrationManager.reload();

        if (recipeManager != null) {
            recipeManager.reload();
        }

        infoMessage(getName() + " reloaded.");
    }

    public void saveTextFiles() {
        ResourceUtil.copyResources("data/text/readme.txt", getDataFolder().getPath()
                + "/readme.txt", this);
        ResourceUtil.copyResources("data/text/placeholders.txt", getDataFolder().getPath()
                + "/placeholders.txt", this);

        if (integrationManager != null) {
            if (integrationManager.hasPlaceholderAPI()) {
                ResourceUtil.copyResources("data/text/placeholderapi.txt", getDataFolder().getPath()
                        + "/placeholderapi.txt", this);
            }

            if (integrationManager.hasFurnitureLib()) {
                ResourceUtil.copyResources("data/text/furniturelib.txt", getDataFolder().getPath()
                        + "/furniturelib.txt", this);
            }
        }
    }

    private void registerMetrics() {
        Metrics metrics = new Metrics(this, getMetricsID());

        metrics.addCustomChart(new SingleLineChart("graves", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return cacheManager.getGraveMap().size();
            }
        }));
    }

    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerBucketEmptyListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockFromToListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPistonExtendListener(this), this);
        getServer().getPluginManager().registerEvents(new HangingBreakListener(this), this);
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

    public void unregisterListeners() {
        HandlerList.unregisterAll(this);
    }

    private void registerRecipes() {
        if (versionManager.hasPersistentData() && !versionManager.isMohist()) {
            recipeManager = new RecipeManager(this);
        }
    }

    private void registerCommands() {
        PluginCommand gravesPluginCommand = getCommand("graves");
        PluginCommand graveyardsPluginCommand = getCommand("graveyards");

        if (gravesPluginCommand != null) {
            GravesCommand gravesCommand = new GravesCommand(this);

            gravesPluginCommand.setExecutor(gravesCommand);
            gravesPluginCommand.setTabCompleter(gravesCommand);
        }

        if (graveyardsPluginCommand != null) {
            GraveyardsCommand graveyardsCommand = new GraveyardsCommand(this);

            graveyardsPluginCommand.setExecutor(graveyardsCommand);
            graveyardsPluginCommand.setTabCompleter(graveyardsCommand);
        }
    }

    public void debugMessage(String string, int level) {
        if (getConfig().getInt("settings.debug.level", 0) >= level) {
            getLogger().info("Debug: " + string);

            for (String admin : getConfig().getStringList("settings.debug.admin")) {
                Player player = getServer().getPlayer(admin);
                UUID uuid = UUIDUtil.getUUID(admin);

                if (uuid != null) {
                    Player uuidPlayer = getServer().getPlayer(uuid);

                    if (uuidPlayer != null) {
                        player = uuidPlayer;
                    }
                }

                if (player != null) {
                    String debug = !integrationManager.hasMultiPaper() ? "Debug:" : "Debug ("
                            + integrationManager.getMultiPaper().getLocalServerName() + "):";

                    player.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RED + debug
                            + ChatColor.RESET + " " + string);
                }
            }
        }
    }

    public void warningMessage(String string) {
        getLogger().info("Warning: " + string);
    }

    public void compatibilityMessage(String string) {
        getLogger().info("Compatibility: " + string);
    }

    public void infoMessage(String string) {
        getLogger().info("Information: " + string);
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

    private void updateConfig() {
        double currentConfigVersion = 3;
        double configVersion = getConfig().getInt("config-version");

        if (configVersion < currentConfigVersion) {
            new File(getDataFolder(), "outdated").mkdirs();

            File singleConfigFile = new File(getDataFolder(), "config.yml");
            File folderConfigFile = new File(getDataFolder(), "config");

            if (singleConfigFile.exists()) {
                FileUtil.moveFile(singleConfigFile, "outdated/config.yml-" + configVersion);
            } else {
                FileUtil.moveFile(folderConfigFile, "outdated/config-" + configVersion);
            }

            warningMessage("Outdated config detected (v" + configVersion + "), current version is (v"
                    + currentConfigVersion + "), renaming outdated config file.");
            saveDefaultConfig();
            reloadConfig();
        }
    }

    private void updateChecker() {
        if (getConfig().getBoolean("settings.update.check")) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                String latestVersion = getLatestVersion();

                if (latestVersion != null) {
                    try {
                        double pluginVersion = Double.parseDouble(getDescription().getVersion());
                        double pluginVersionLatest = Double.parseDouble(latestVersion);

                        if (pluginVersion < pluginVersionLatest) {
                            getLogger().info("Update: Outdated version detected " + pluginVersion
                                    + ", latest version is " + pluginVersionLatest
                                    + ", https://www.spigotmc.org/resources/" + getSpigotID() + "/");
                        }
                    } catch (NumberFormatException exception) {
                        if (!getDescription().getVersion().equalsIgnoreCase(latestVersion)) {
                            getLogger().info("Update: Outdated version detected " + getDescription().getVersion()
                                    + ", latest version is " + latestVersion + ", https://www.spigotmc.org/resources/"
                                    + getSpigotID() + "/");
                        }
                    }
                }
            });
        }
    }

    private void compatibilityChecker() {
        compatibility = versionManager.hasBlockData() ? new CompatibilityBlockData() : new CompatibilityMaterialData();

        if (!versionManager.hasBlockData()) {
            infoMessage("Legacy version detected, Graves will run but may have problems with material names, " +
                    "the default config is setup for the latest version of the game, you can alter the config manually to fix " +
                    "any issues you encounter, you will need to find the names of materials and sounds for your version.");
        }

        if (versionManager.isBukkit()) {
            infoMessage("Bukkit detected, some functions won't work on Bukkit, like hex codes.");
        }

        if (versionManager.isMohist()) {
            infoMessage("Mohist detected, not injecting custom recipes.");
        }
    }

    public void dumpServerInfo(CommandSender commandSender) {
        if (isEnabled()) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                String serverDumpInfo = ServerUtil.getServerDumpInfo(this);
                String message = serverDumpInfo;

                if (getConfig().getString("settings.dump.method", "HASTEBIN")
                        .equalsIgnoreCase("HASTEBIN")) {
                    String response = HastebinUtil.postDataToHastebin(serverDumpInfo, true);

                    if (response != null) {
                        message = response;
                    }
                }

                if (serverDumpInfo.equals(message)) {
                    try {
                        String name = "graves-dump-" + System.currentTimeMillis() + ".txt";
                        PrintWriter printWriter = new PrintWriter(name, "UTF-8");

                        printWriter.write(serverDumpInfo);
                        printWriter.close();

                        message = name;
                    } catch (FileNotFoundException | UnsupportedEncodingException exception) {
                        exception.printStackTrace();
                    }
                }

                commandSender.sendMessage(ChatColor.RED + "☠" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET
                        + "Dumped: " + message);
            });
        }
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public GraveManager getGraveManager() {
        return graveManager;
    }

    public GraveyardManager getGraveyardManager() {
        return graveyardManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public BlockManager getBlockManager() {
        return blockManager;
    }

    public ItemStackManager getItemStackManager() {
        return itemStackManager;
    }

    public EntityDataManager getEntityDataManager() {
        return entityDataManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ImportManager getImportManager() {
        return importManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
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

    private void loadResourceDefaults(FileConfiguration fileConfiguration, String resource) {
        InputStream inputStream = getResource(resource);

        if (inputStream != null) {
            fileConfiguration.addDefaults(YamlConfiguration
                    .loadConfiguration(new InputStreamReader(inputStream, Charsets.UTF_8)));
        }
    }

    private void bakeDefaults(FileConfiguration fileConfiguration) {
        try {
            fileConfiguration.options().copyDefaults(true);
            fileConfiguration.loadFromString(fileConfiguration.saveToString());
        } catch (InvalidConfigurationException ignored) {
        }
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

    private FileConfiguration getConfigFiles(File folder) {
        FileConfiguration fileConfiguration = new YamlConfiguration();
        File[] files = folder.listFiles();

        if (files != null) {
            Arrays.sort(files);

            List<File> fileList = new LinkedList<>(Arrays.asList(files));
            File mainConfig = new File(getConfigFolder(), "config.yml");

            if (fileList.contains(mainConfig)) {
                fileList.remove(mainConfig);
                fileList.add(0, mainConfig);
            }

            for (File file : fileList) {
                if (YAMLUtil.isValidYAML(file)) {
                    if (file.isDirectory()) {
                        fileConfiguration.addDefaults(getConfigFiles(file));
                    } else {
                        FileConfiguration savedFileConfiguration = getConfigFile(file);

                        if (savedFileConfiguration != null) {
                            fileConfiguration.addDefaults(savedFileConfiguration);
                            bakeDefaults(fileConfiguration);
                            loadResourceDefaults(fileConfiguration, "config" + File.separator + file.getName());
                        } else {
                            warningMessage("Unable to load config " + file.getName());
                        }
                    }
                }
            }
        }

        return fileConfiguration;
    }

    private FileConfiguration getConfigFile(File file) {
        FileConfiguration fileConfiguration = null;

        if (YAMLUtil.isValidYAML(file)) {
            try {
                fileConfiguration = YamlConfiguration.loadConfiguration(file);
            } catch (IllegalArgumentException exception) {
                exception.printStackTrace();
            }
        }

        return fileConfiguration;
    }

    public final File getConfigFolder() {
        return new File(getDataFolder(), "config");
    }

    public final File getPluginsFolder() {
        return getDataFolder().getParentFile();
    }

    public String getVersion() {
        return getDescription().getVersion();
    }

    public String getLatestVersion() {
        return UpdateUtil.getLatestVersion(getSpigotID());
    }

    public final int getSpigotID() {
        return 74208;
    }

    public final int getMetricsID() {
        return 12849;
    }
}
