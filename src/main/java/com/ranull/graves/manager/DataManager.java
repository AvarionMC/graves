package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.inventory.Grave;
import com.ranull.graves.util.ClassUtil;
import com.ranull.graves.util.InventoryUtil;
import com.ranull.graves.util.LocationUtil;
import com.ranull.graves.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.sql.*;
import java.util.*;

public final class DataManager {
    private final Graves plugin;
    private final Map<UUID, Grave> uuidGraveMap;
    private final Map<String, ChunkData> chunkDataMap;
    private Type type;
    private String url;
    private Connection connection;

    public DataManager(Graves plugin, Type type) {
        this.plugin = plugin;
        this.type = type;
        this.uuidGraveMap = new HashMap<>();
        this.chunkDataMap = new HashMap<>();

        loadType(type);
        load();
    }

    private void load() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            loadTables();
            loadGraveMap();
            loadBlockMap();
            loadHologramMap();

            if (plugin.hasFurnitureLib()) {
                loadEntityDataMap("furniturelib", EntityData.Type.FURNITURELIB);
            }

            if (plugin.hasFurnitureEngine()) {
                loadEntityDataMap("furnitureengine", EntityData.Type.FURNITUREENGINE);
            }

            if (plugin.hasItemsAdder()) {
                loadEntityDataMap("itemsadder", EntityData.Type.ITEMSADDER);
            }

            if (plugin.hasOraxen()) {
                loadEntityDataMap("oraxen", EntityData.Type.ORAXEN);
            }
        });
    }

    private void loadTables() {
        setupGraveTable();
        setupBlockTable();
        setupHologramTable();

        if (plugin.hasFurnitureLib()) {
            setupFurnitureIntegrationTable("furniturelib");
        }

        if (plugin.hasFurnitureEngine()) {
            setupFurnitureIntegrationTable("furnitureengine");
        }

        if (plugin.hasItemsAdder()) {
            setupFurnitureIntegrationTable("itemsadder");
        }

        if (plugin.hasOraxen()) {
            setupFurnitureIntegrationTable("oraxen");
        }
    }

    public void reload() {
        reload(type);
    }

    public void reload(Type type) {
        closeConnection();
        loadType(type);
        load();
    }

    public void loadType(Type type) {
        this.type = type;

        if (type == Type.MYSQL) {  // TODO MYSQL
            this.url = null;

            ClassUtil.loadClass("com.mysql.jdbc.Driver");
        } else {
            this.url = "jdbc:sqlite:" + plugin.getDataFolder() + File.separatorChar + "data.db";

            ClassUtil.loadClass("org.sqlite.JDBC");
            executeUpdate("PRAGMA journal_mode=" + plugin.getConfig()
                    .getString("settings.storage.sqlite.journal-mode", "WAL").toUpperCase() + ";");
            executeUpdate("PRAGMA synchronous=" + plugin.getConfig()
                    .getString("settings.storage.sqlite.synchronous", "OFF").toUpperCase() + ";");
        }
    }

    public void onDisable() {
        closeConnection();
    }

    public Map<UUID, Grave> getGraveMap() {
        return uuidGraveMap;
    }

    public boolean hasChunkData(Location location) {
        return chunkDataMap.containsKey(LocationUtil.chunkToString(location));
    }

    public Map<String, ChunkData> getChunkDataMap() {
        return chunkDataMap;
    }

    public ChunkData getChunkData(Location location) {
        String chunkString = LocationUtil.chunkToString(location);
        ChunkData chunkData;

        if (chunkDataMap.containsKey(chunkString)) {
            chunkData = chunkDataMap.get(chunkString);
        } else {
            chunkData = new ChunkData(location);

            chunkDataMap.put(chunkString, chunkData);
        }

        return chunkData;
    }

    public List<String> getColumnList(String tableName) {
        List<String> columnList = new ArrayList<>();
        ResultSet resultSet;

        if (type == Type.MYSQL) {
            resultSet = null; // TODO MYSQL
        } else {
            resultSet = executeQuery("PRAGMA table_info(" + tableName + ");");
        }

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    columnList.add(resultSet.getString("name"));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }

        return columnList;
    }

    public void setupGraveTable() {
        String name = "grave";

        executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                "uuid VARCHAR(255) UNIQUE,\n" +
                "owner_type VARCHAR(255),\n" +
                "owner_name VARCHAR(255),\n" +
                "owner_name_display VARCHAR(255),\n" +
                "owner_uuid VARCHAR(255),\n" +
                "owner_texture VARCHAR(255),\n" +
                "killer_type VARCHAR(255),\n" +
                "killer_name VARCHAR(255),\n" +
                "killer_name_display VARCHAR(255),\n" +
                "killer_uuid VARCHAR(255),\n" +
                "location_death VARCHAR(255),\n" +
                "yaw FLOAT(16),\n" +
                "pitch FLOAT(16),\n" +
                "inventory TEXT,\n" +
                "experience INT(16),\n" +
                "protection INT(1),\n" +
                "time_alive INT(16),\n" +
                "time_protection INT(11),\n" +
                "time_creation INT(11),\n" +
                "permissions TEXT);");

        List<String> columnList = getColumnList(name);

        if (!columnList.contains("uuid")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN uuid VARCHAR(255) UNIQUE;");
        }

        if (!columnList.contains("owner_type")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN owner_type VARCHAR(255);");
        }

        if (!columnList.contains("owner_name")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN owner_name VARCHAR(255);");
        }

        if (!columnList.contains("owner_name_display")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN owner_name_display VARCHAR(255);");
        }

        if (!columnList.contains("owner_uuid")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN owner_uuid VARCHAR(255);");
        }

        if (!columnList.contains("owner_texture")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN owner_texture VARCHAR(255);");
        }

        if (!columnList.contains("killer_type")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN killer_type VARCHAR(255);");
        }

        if (!columnList.contains("killer_name")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN killer_name VARCHAR(255);");
        }

        if (!columnList.contains("killer_name_display")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN killer_name_display VARCHAR(255);");
        }

        if (!columnList.contains("killer_uuid")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN killer_uuid VARCHAR(255);");
        }

        if (!columnList.contains("location_death")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN location_death VARCHAR(255);");
        }

        if (!columnList.contains("yaw")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN yaw FLOAT(16);");
        }

        if (!columnList.contains("pitch")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN pitch FLOAT(16);");
        }

        if (!columnList.contains("inventory")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN inventory TEXT;");
        }

        if (!columnList.contains("experience")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN experience INT(16);");
        }

        if (!columnList.contains("protection")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN protection INT(1);");
        }

        if (!columnList.contains("time_alive")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN time_alive INT(16);");
        }

        if (!columnList.contains("time_protection")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN time_protection INT(16);");
        }

        if (!columnList.contains("time_creation")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN time_creation INT(16);");
        }

        if (!columnList.contains("permissions")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN permissions TEXT;");
        }
    }

    public void setupBlockTable() {
        String name = "block";

        executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                "location VARCHAR(255),\n" +
                "uuid_grave VARCHAR(255),\n" +
                "replace_material VARCHAR(255),\n" +
                "replace_data TEXT);");

        List<String> columnList = getColumnList(name);

        if (!columnList.contains("location")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN location VARCHAR(255);");
        }

        if (!columnList.contains("uuid_grave")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN uuid_grave VARCHAR(255);");
        }

        if (!columnList.contains("replace_material")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN replace_material VARCHAR(255);");
        }

        if (!columnList.contains("replace_data")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN replace_data TEXT;");
        }
    }

    public void setupHologramTable() {
        String name = "hologram";

        executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                "chunk VARCHAR(255),\n" +
                "uuid_entity VARCHAR(255),\n" +
                "uuid_grave VARCHAR(255),\n" +
                "line INT(16));");

        List<String> columnList = getColumnList(name);

        if (!columnList.contains("chunk")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN chunk VARCHAR(255);");
        }

        if (!columnList.contains("uuid_entity")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN uuid_entity VARCHAR(255);");
        }

        if (!columnList.contains("uuid_grave")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN uuid_grave VARCHAR(255);");
        }

        if (!columnList.contains("line")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN line INT(16);");
        }
    }

    private void setupFurnitureIntegrationTable(String name) {
        executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                "chunk VARCHAR(255),\n" +
                "uuid_entity VARCHAR(255),\n" +
                "uuid_grave VARCHAR(255));");

        List<String> columnList = getColumnList(name);

        if (!columnList.contains("chunk")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN chunk VARCHAR(255);");
        }

        if (!columnList.contains("uuid_entity")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN uuid_entity VARCHAR(255);");
        }

        if (!columnList.contains("uuid_grave")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN uuid_grave VARCHAR(255);");
        }
    }

    private void loadGraveMap() {
        uuidGraveMap.clear();

        ResultSet resultSet = executeQuery("SELECT * FROM grave;");

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    Grave grave = resultSetToGrave(resultSet);

                    if (grave != null) {
                        uuidGraveMap.put(grave.getUUID(), grave);
                    }
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void loadBlockMap() {
        ResultSet resultSet = executeQuery("SELECT * FROM block;");

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    Location location = LocationUtil.stringToLocation(resultSet.getString("location"));
                    UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                    String replaceMaterial = resultSet.getString("replace_material");
                    String replaceData = resultSet.getString("replace_data");

                    getChunkData(location).addBlockData(new BlockData(location, uuidGrave,
                            replaceMaterial, replaceData));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void loadHologramMap() {
        ResultSet resultSet = executeQuery("SELECT * FROM hologram;");

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    Location location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                    UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                    UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                    int line = resultSet.getInt("line");

                    getChunkData(location).addEntityData(new HologramData(location, uuidEntity, uuidGrave, line));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void loadEntityDataMap(String table, EntityData.Type type) {
        ResultSet resultSet = executeQuery("SELECT * FROM " + table + ";");

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    Location location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                    UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                    UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));

                    getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void addBlockData(BlockData blockData) {
        getChunkData(blockData.getLocation()).addBlockData(blockData);

        String uuidGrave = blockData.getGraveUUID() != null ? "'" + blockData.getGraveUUID() + "'" : "NULL";
        String location = "'" + LocationUtil.locationToString(blockData.getLocation()) + "'";
        String replaceMaterial = blockData.getReplaceMaterial() != null ? "'"
                + blockData.getReplaceMaterial() + "'" : "NULL";
        String replaceData = blockData.getReplaceData() != null ? "'" + blockData.getReplaceData() + "'" : "NULL";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("INSERT INTO block (location, uuid_grave, replace_material, replace_data) " +
                    "VALUES (" + location + ", " + uuidGrave + ", " + replaceMaterial + ", " + replaceData + ");");
        });
    }

    public void removeBlockData(Location location) {
        getChunkData(location).removeBlockData(location);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("DELETE FROM block WHERE location = '"
                    + LocationUtil.locationToString(location) + "';");
        });
    }

    public void addHologramData(HologramData hologramData) {
        getChunkData(hologramData.getLocation()).addEntityData(hologramData);

        String chunk = "'" + LocationUtil.chunkToString(hologramData.getLocation()) + "'";
        String uuidEntity = "'" + hologramData.getUUIDEntity() + "'";
        String uuidGrave = "'" + hologramData.getUUIDGrave() + "'";
        int line = hologramData.getLine();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("INSERT INTO hologram (chunk, uuid_entity, uuid_grave, line) VALUES ("
                    + chunk + ", " + uuidEntity + ", " + uuidGrave + ", " + line + ");");
        });
    }

    public void removeHologramData(List<EntityData> entityDataList) {
        try {
            Statement statement = connection.createStatement();

            for (EntityData hologramData : entityDataList) {
                getChunkData(hologramData.getLocation()).removeEntityData(hologramData);
                statement.addBatch("DELETE FROM hologram WHERE uuid_entity = '"
                        + hologramData.getUUIDEntity() + "';");
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                executeBatch(statement);
            });
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void addEntityData(EntityData entityData) {
        getChunkData(entityData.getLocation()).addEntityData(entityData);

        String table = entityDataTypeTable(entityData.getType());

        if (table != null) {
            String chunk = "'" + LocationUtil.chunkToString(entityData.getLocation()) + "'";
            String uuidEntity = "'" + entityData.getUUIDEntity() + "'";
            String uuidGrave = "'" + entityData.getUUIDGrave() + "'";

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                executeUpdate("INSERT INTO " + table + " (chunk, uuid_entity, uuid_grave) VALUES ("
                        + chunk + ", " + uuidEntity + ", " + uuidGrave + ");");
            });
        }
    }

    public void removeEntityData(List<EntityData> entityDataList) {
        try {
            Statement statement = connection.createStatement();

            for (EntityData entityData : entityDataList) {
                getChunkData(entityData.getLocation()).removeEntityData(entityData);

                String table = entityDataTypeTable(entityData.getType());

                if (table != null) {
                    statement.addBatch("DELETE FROM " + table + " WHERE uuid_entity = '"
                            + entityData.getUUIDEntity() + "';");
                }
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                executeBatch(statement);
            });
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public String entityDataTypeTable(EntityData.Type type) {
        switch (type) {
            case HOLOGRAM: {
                return "hologram";
            }

            case FURNITURELIB: {
                return "furniturelib";
            }

            case FURNITUREENGINE: {
                return "furnitureengine";
            }

            case ITEMSADDER: {
                return "itemsadder";
            }

            case ORAXEN: {
                return "oraxen";
            }

            default: {
                return null;
            }
        }
    }

    public void addGrave(Grave grave) {
        uuidGraveMap.put(grave.getUUID(), grave);

        String uuid = grave.getUUID() != null ? "'" + grave.getUUID() + "'" : "NULL";
        String ownerType = grave.getOwnerType() != null ? "'" + grave.getOwnerType() + "'" : "NULL";
        String ownerName = grave.getOwnerName() != null ? "'" + grave.getOwnerName()
                .replace("'", "''") + "'" : "NULL";
        String ownerNameDisplay = grave.getOwnerNameDisplay() != null ? "'" + grave.getOwnerNameDisplay()
                .replace("'", "''") + "'" : "NULL";
        String ownerUUID = grave.getOwnerUUID() != null ? "'" + grave.getOwnerUUID() + "'" : "NULL";
        String ownerTexture = grave.getOwnerTexture() != null ? "'" + grave.getOwnerTexture()
                .replace("'", "''") + "'" : "NULL";
        String killerType = grave.getKillerType() != null ? "'" + grave.getKillerType() + "'" : "NULL";
        String killerName = grave.getKillerName() != null ? "'" + grave.getKillerName()
                .replace("'", "''") + "'" : "NULL";
        String killerNameDisplay = grave.getKillerNameDisplay() != null ? "'" + grave.getKillerNameDisplay()
                .replace("'", "''") + "'" : "NULL";
        String killerUUID = grave.getKillerUUID() != null ? "'" + grave.getKillerUUID() + "'" : "NULL";
        String locationDeath = grave.getLocationDeath() != null ? "'"
                + LocationUtil.locationToString(grave.getLocationDeath()) + "'" : "NULL";
        float yaw = grave.getYaw();
        float pitch = grave.getPitch();
        String inventory = "'" + InventoryUtil.inventoryToString(grave.getInventory()) + "'";
        String permissions = grave.getPermissionList() != null && !grave.getPermissionList().isEmpty()
                ? "'" + StringUtils.join(grave.getPermissionList(), "|") + "'" : "NULL";
        int protection = grave.getProtection() ? 1 : 0;
        int experience = grave.getExperience();
        long timeAlive = grave.getTimeAlive();
        long timeProtection = grave.getTimeProtection();
        long timeCreation = grave.getTimeCreation();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("INSERT INTO grave (uuid, owner_type, owner_name, owner_name_display, owner_uuid,"
                    + " owner_texture, killer_type, killer_name, killer_name_display, killer_uuid, location_death,"
                    + " yaw, pitch, inventory, experience, protection, time_alive, time_protection,"
                    + " time_creation, permissions) VALUES (" + uuid + ", " + ownerType + ", " + ownerName + ", "
                    + ownerNameDisplay + ", " + ownerUUID + ", " + ownerTexture + ", " + killerType + ", "
                    + killerName + ", " + killerNameDisplay + ", " + killerUUID + ", " + locationDeath + ", "
                    + yaw + ", " + pitch + ", " + inventory + ", " + experience + ", " + protection + ", "
                    + timeAlive + ", " + timeProtection + ", " + timeCreation + ", " + permissions + ");");
        });
    }

    public void removeGrave(Grave grave) {
        uuidGraveMap.remove(grave.getUUID());

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("DELETE FROM grave WHERE uuid = '" + grave.getUUID() + "';");
        });
    }

    public void updateGrave(Grave grave, String column, String string) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("UPDATE grave SET " + column + " = '" + string + "' WHERE uuid = '"
                    + grave.getUUID() + "';");
        });
    }

    private Grave resultSetToGrave(ResultSet resultSet) {
        try {
            Grave grave = new Grave(UUID.fromString(resultSet.getString("uuid")));

            grave.setOwnerType(resultSet.getString("owner_type") != null
                    ? EntityType.valueOf(resultSet.getString("owner_type")) : null);
            grave.setOwnerName(resultSet.getString("owner_name") != null
                    ? resultSet.getString("owner_name") : null);
            grave.setOwnerNameDisplay(resultSet.getString("owner_name_display") != null
                    ? resultSet.getString("owner_name_display") : null);
            grave.setOwnerUUID(resultSet.getString("owner_uuid") != null
                    ? UUID.fromString(resultSet.getString("owner_uuid")) : null);
            grave.setOwnerTexture(resultSet.getString("owner_texture") != null
                    ? resultSet.getString("owner_texture") : null);
            grave.setKillerType(resultSet.getString("killer_type") != null
                    ? EntityType.valueOf(resultSet.getString("killer_type")) : null);
            grave.setKillerName(resultSet.getString("killer_name") != null
                    ? resultSet.getString("killer_name") : null);
            grave.setKillerNameDisplay(resultSet.getString("killer_name_display") != null
                    ? resultSet.getString("killer_name_display") : null);
            grave.setKillerUUID(resultSet.getString("killer_uuid") != null
                    ? UUID.fromString(resultSet.getString("killer_uuid")) : null);
            grave.setLocationDeath(resultSet.getString("location_death") != null
                    ? LocationUtil.stringToLocation(resultSet.getString("location_death")) : null);
            grave.setYaw(resultSet.getFloat("yaw"));
            grave.setPitch(resultSet.getFloat("pitch"));
            grave.setExperience(resultSet.getInt("experience"));
            grave.setProtection(resultSet.getInt("protection") == 1);
            grave.setTimeAlive(resultSet.getLong("time_alive"));
            grave.setTimeProtection(resultSet.getLong("time_protection"));
            grave.setTimeCreation(resultSet.getLong("time_creation"));
            grave.setPermissionList(resultSet.getString("permissions") != null
                    ? new ArrayList<>(Arrays.asList(resultSet.getString("permissions").split("\\|"))) : null);
            grave.setInventory(InventoryUtil.stringToInventory(grave, resultSet.getString("inventory"),
                    StringUtil.parseString(plugin.getConfig("gui.grave.title", grave.getOwnerType(),
                                    grave.getPermissionList())
                            .getString("gui.grave.title"), grave.getLocationDeath(), grave, plugin), plugin));

            return grave;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    private boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException exception) {
            exception.printStackTrace();

            return false;
        }
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void closeConnection() {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void executeBatch(Statement statement) {
        if (!isConnected()) {
            connect();
        }

        try {
            statement.executeBatch();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void executeUpdate(String sql) {
        if (!isConnected()) {
            connect();
        }

        try {
            connection.createStatement().executeUpdate(sql);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private ResultSet executeQuery(String sql) {
        if (!isConnected()) {
            connect();
        }

        try {
            return connection.createStatement().executeQuery(sql);
        } catch (SQLException exception) {
            exception.printStackTrace();

            return null;
        }
    }

    public enum Type {
        SQLITE,
        MYSQL
    }
}
