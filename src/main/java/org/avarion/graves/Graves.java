package org.avarion.graves;

import com.google.common.base.Charsets;
import org.avarion.graves.command.GravesCommand;
import org.avarion.graves.command.GraveyardsCommand;
import org.avarion.graves.compatibility.Compatibility;
import org.avarion.graves.compatibility.CompatibilityBlockData;
import org.avarion.graves.listener.*;
import org.avarion.graves.manager.*;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.*;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Graves extends JavaPlugin {
    private static final boolean IS_TEST = false;

    private VersionManager versionManager;
    private IntegrationManager integrationManager;
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

    private Version myVersion = null;

    @Override
    public void onLoad() {
        myVersion = new Version(getDescription().getVersion());

        saveDefaultConfig();

        integrationManager = new IntegrationManager(this);

        integrationManager.loadWorldGuard();
    }

    @Override
    public void onEnable() {
        integrationManager.load();

        versionManager = new VersionManager();
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
        if (dataManager != null) {
            dataManager.closeConnection();
            dataManager = null;
        }

        if (graveManager != null) {
            graveManager.unload();
            graveManager = null;
        }

        if (graveyardManager != null) {
            graveyardManager.unload();
            graveyardManager = null;
        }

        if (integrationManager != null) {
            integrationManager.unload();
            integrationManager = null;
        }

        if (recipeManager != null) {
            recipeManager.unload();
            recipeManager = null;
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
        }
        else {
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
        ResourceUtil.copyResources("data/text/readme.txt", getDataFolder().getPath() + "/readme.txt", this);
        ResourceUtil.copyResources("data/text/placeholders.txt", getDataFolder().getPath() + "/placeholders.txt", this);

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
        if (IS_TEST) {
            return;
        }

        Metrics metrics = new Metrics(this, getMetricsID());

        metrics.addCustomChart(new SingleLineChart("graves", CacheManager.graveMap::size));
    }

    public void registerListeners() {
        // Configurable death listener priority
        getServer().getPluginManager().registerEvent(EntityDeathEvent.class, new Listener() {
        }, getEventPriority("death", EventPriority.MONITOR), new EntityDeathListener(this), this, true);

        // All other non-configurable listeners
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerBucketEmptyListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
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
        getServer().getPluginManager().registerEvents(new PlayerInteractAtEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockExplodeListener(this), this);

        //getServer().getPluginManager().registerEvents(new GraveTestListener(this), this); // Test Listener
    }

    // Get priority for a type. Currently only "death" is available
    private EventPriority getEventPriority(String type, EventPriority defaultPriority) {
        String priorityStr = getConfig().getString("settings.listener-priority." + type);
        if (priorityStr == null) {
            return defaultPriority;
        }

        try {
            return EventPriority.valueOf(priorityStr.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            getLogger().warning("Invalid event priority in config for type '"
                                + type
                                + "': "
                                + priorityStr
                                + ". Defaulting to "
                                + defaultPriority);
            return defaultPriority;
        }
    }

    public void unregisterListeners() {
        HandlerList.unregisterAll(this);
    }

    private void registerRecipes() {
        if (!versionManager.isMohist) {
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
        if (getConfig().getInt("settings.debug.level", 0) < level) {
            return;
        }

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
                String debug = !integrationManager.hasMultiPaper()
                               ? "Debug:"
                               : "Debug (" + integrationManager.getMultiPaper().getLocalServerName() + "):";

                player.sendMessage(ChatColor.RED
                                   + "☠"
                                   + ChatColor.DARK_GRAY
                                   + " » "
                                   + ChatColor.RED
                                   + debug
                                   + ChatColor.RESET
                                   + " "
                                   + string);
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
            }
            else {
                FileUtil.moveFile(folderConfigFile, "outdated/config-" + configVersion);
            }

            warningMessage("Outdated config detected (v"
                           + configVersion
                           + "), current version is (v"
                           + currentConfigVersion
                           + "), renaming outdated config file.");
            saveDefaultConfig();
            reloadConfig();
        }
    }

    private void updateChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!getConfig().getBoolean("settings.update.check")) {
                    return;
                }

                final Version latestVersion = getLatestVersion();

                if (latestVersion == null) {
                    return;
                }

                if (getVersion().isOutdated(latestVersion)) {
                    getLogger().info("Update: Outdated version detected "
                                     + getVersion()
                                     + ", latest version is "
                                     + latestVersion
                                     + ", https://www.spigotmc.org/resources/"
                                     + getSpigotID()
                                     + "/");
                }
            }
        }.runTaskTimer(this, 0, 24 * 60 * 60 * 20); // run on a daily schedule
    }

    private void compatibilityChecker() {
        compatibility = new CompatibilityBlockData();

        if (versionManager.isBukkit) {
            infoMessage("Bukkit detected, some functions won't work on Bukkit, like hex codes.");
        }

        if (versionManager.isMohist) {
            infoMessage("Mohist detected, not injecting custom recipes.");
        }
    }

    public void dumpServerInfo(CommandSender commandSender) {
        if (isEnabled()) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                String serverDumpInfo = ServerUtil.getServerDumpInfo(this);
                String message = serverDumpInfo;

                if (getConfig().getString("settings.dump.method", "HASTEBIN").equalsIgnoreCase("HASTEBIN")) {
                    String response = HastebinUtil.postDataToHastebin(serverDumpInfo, true);

                    if (response != null) {
                        message = response;
                    }
                }

                if (serverDumpInfo.equals(message)) {
                    try {
                        String name = "graves-dump-" + System.currentTimeMillis() + ".txt";
                        PrintWriter printWriter = new PrintWriter(name, StandardCharsets.UTF_8);

                        printWriter.write(serverDumpInfo);
                        printWriter.close();

                        message = name;
                    }
                    catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }

                commandSender.sendMessage(ChatColor.RED
                                          + "☠"
                                          + ChatColor.DARK_GRAY
                                          + " » "
                                          + ChatColor.RESET
                                          + "Dumped: "
                                          + message);
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

    /// startregion: getConfig*
    public boolean getConfigBool(String config, @NotNull Grave grave) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList()).getBoolean(config);
    }

    public int getConfigInt(String config, @NotNull Grave grave) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList()).getInt(config);
    }

    public int getConfigInt(String config, @NotNull Grave grave, int defaultValue) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList()).getInt(config, defaultValue);
    }

    public String getConfigString(String config, @NotNull Grave grave, String defaultValue) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList()).getString(config, defaultValue);
    }

    public String getConfigString(String config, @NotNull Entity entity, String defaultValue) {
        return getConfig(config, entity.getType(), getPermissionList(entity)).getString(config, defaultValue);
    }

    public String getConfigString(String config, @NotNull Grave grave) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList()).getString(config);
    }

    public List<String> getConfigStringList(String config, @NotNull Grave grave) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList()).getStringList(config);
    }

    public double getConfigDbl(String config, @NotNull Grave grave) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList()).getDouble(config);
    }

    public boolean getConfigBool(String config, @NotNull Entity entity) {
        return getConfig(config, entity.getType(), getPermissionList(entity)).getBoolean(config);
    }

    public boolean getConfigBool(String config, @NotNull Entity entity, @Nullable List<String> permissionList) {
        return getConfig(config, entity.getType(), permissionList).getBoolean(config);
    }

    public boolean getConfigBool(String config, EntityType entityType, @Nullable List<String> permissionList) {
        return getConfig(config, entityType, permissionList).getBoolean(config);
    }

    public int getConfigInt(String config, @NotNull Entity entity, @Nullable List<String> permissionList) {
        return getConfig(config, entity.getType(), permissionList).getInt(config);
    }

    public String getConfigString(String config, @NotNull Entity entity, @Nullable List<String> permissionList) {
        return getConfig(config, entity.getType(), permissionList).getString(config);
    }

    public String getConfigString(String config, @NotNull Entity entity, @Nullable List<String> permissionList, String defaultValue) {
        return getConfig(config, entity.getType(), permissionList).getString(config, defaultValue);
    }

    public String getConfigString(String config, EntityType entityType, @Nullable List<String> permissionList) {
        return getConfig(config, entityType, permissionList).getString(config);
    }

    public List<String> getConfigStringList(String config, @NotNull Entity entity, @Nullable List<String> permissionList) {
        return getConfig(config, entity.getType(), permissionList).getStringList(config);
    }

    public ConfigurationSection getConfigSection(String s, Grave grave) {
        return getConfig(s, grave).getConfigurationSection(s);
    }
    /// endregion: getConfig*

    /// startregion: getConfig
    public ConfigurationSection getConfig(String config, @NotNull Grave grave) {
        return getConfig(config, grave.getOwnerType(), grave.getPermissionList());
    }

    public ConfigurationSection getConfig(String config, EntityType entityType, List<String> permissionList) {
        if (permissionList != null && !permissionList.isEmpty()) {
            for (String permission : permissionList) {
                String section = "settings.permission." + permission;

                ConfigurationSection configurationSection = getConfig().getConfigurationSection(section);
                if (configurationSection != null && configurationSection.contains(config, true)) {
                    return configurationSection;
                }
            }
        }

        if (entityType != null) {
            String section = "settings.entity." + entityType.name();

            ConfigurationSection configurationSection = getConfig().getConfigurationSection(section);
            if (configurationSection != null && configurationSection.contains(config, true)) {
                return configurationSection;
            }
        }

        return getConfig().getConfigurationSection("settings.default.default");
    }
    /// endregion

    private void loadResourceDefaults(FileConfiguration fileConfiguration, String resource) {
        InputStream inputStream = getResource(resource);

        if (inputStream != null) {
            fileConfiguration.addDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, Charsets.UTF_8)));
        }
    }

    private void bakeDefaults(@NotNull FileConfiguration fileConfiguration) {
        try {
            fileConfiguration.options().copyDefaults(true);
            fileConfiguration.loadFromString(fileConfiguration.saveToString());
        }
        catch (InvalidConfigurationException ignored) {
        }
    }

    public List<String> getPermissionList(Entity entity) {
        List<String> permissionList = new ArrayList<>();
        List<String> permissionListSorted = new ArrayList<>();

        if (entity instanceof Player player) {

            for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
                if (permissionAttachmentInfo.getPermission().startsWith("graves.permission.")) {
                    String permission = permissionAttachmentInfo.getPermission()
                                                                .replace("graves.permission.", "")
                                                                .toLowerCase();

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

    private @NotNull FileConfiguration getConfigFiles(@NotNull File folder) {
        FileConfiguration yamlConfiguration = new YamlConfiguration();
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
                        yamlConfiguration.addDefaults(getConfigFiles(file));
                    }
                    else {
                        FileConfiguration savedFileConfiguration = getConfigFile(file);

                        if (savedFileConfiguration != null) {
                            yamlConfiguration.addDefaults(savedFileConfiguration);
                            bakeDefaults(yamlConfiguration);
                            loadResourceDefaults(yamlConfiguration, "config" + File.separator + file.getName());
                        }
                        else {
                            warningMessage("Unable to load config " + file.getName());
                        }
                    }
                }
            }
        }

        return yamlConfiguration;
    }

    private @Nullable FileConfiguration getConfigFile(File file) {
        if (!YAMLUtil.isValidYAML(file)) {
            return null;
        }

        try {
            return YamlConfiguration.loadConfiguration(file);
        }
        catch (IllegalArgumentException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Contract(" -> new")
    public final @NotNull File getConfigFolder() {
        return new File(getDataFolder(), "config");
    }

    public final File getPluginsFolder() {
        return getDataFolder().getParentFile();
    }

    public @NotNull Version getVersion() {
        return myVersion;
    }

    public @Nullable Version getLatestVersion() {
        return UpdateUtil.getLatestVersion(getSpigotID());
    }

    @SuppressWarnings("SameReturnValue")
    public final int getSpigotID() {
        return 116202;
    }

    @SuppressWarnings("SameReturnValue")
    public final int getMetricsID() {
        return 21607;
    }
}
