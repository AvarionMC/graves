package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.data.BlockData;
import com.ranull.graves.data.ChunkData;
import com.ranull.graves.data.EntityData;
import com.ranull.graves.data.HologramData;
import com.ranull.graves.type.Grave;
import com.ranull.graves.util.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;

public final class DataManager {
    private final Graves plugin;
    private Type type;
    private String url;
    private Connection connection;

    public DataManager(Graves plugin) {
        this.plugin = plugin;
        this.type = DataManager.Type.SQLITE;

        loadType(type);
        load();
    }

    private void load() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            loadTables();
            loadGraveMap();
            loadBlockMap();
            loadEntityMap("armorstand", EntityData.Type.ARMOR_STAND);
            loadEntityMap("itemframe", EntityData.Type.ITEM_FRAME);
            loadHologramMap();

            if (plugin.getIntegrationManager().hasFurnitureLib()) {
                loadEntityDataMap("furniturelib", EntityData.Type.FURNITURELIB);
            }

            if (plugin.getIntegrationManager().hasFurnitureEngine()) {
                loadEntityDataMap("furnitureengine", EntityData.Type.FURNITUREENGINE);
            }

            if (plugin.getIntegrationManager().hasItemsAdder()) {
                loadEntityDataMap("itemsadder", EntityData.Type.ITEMSADDER);
            }

            if (plugin.getIntegrationManager().hasOraxen()) {
                loadEntityDataMap("oraxen", EntityData.Type.ORAXEN);
            }

            if (plugin.getIntegrationManager().hasPlayerNPC()) {
                loadEntityDataMap("playernpc", EntityData.Type.PLAYERNPC);
                plugin.getIntegrationManager().getPlayerNPC().createCorpses();
            }
        });
    }

    private void loadTables() {
        setupGraveTable();
        setupBlockTable();
        setupHologramTable();
        setupEntityTable("armorstand");
        setupEntityTable("itemframe");

        if (plugin.getIntegrationManager().hasFurnitureLib()) {
            setupEntityTable("furniturelib");
        }

        if (plugin.getIntegrationManager().hasFurnitureEngine()) {
            setupEntityTable("furnitureengine");
        }

        if (plugin.getIntegrationManager().hasItemsAdder()) {
            setupEntityTable("itemsadder");
        }

        if (plugin.getIntegrationManager().hasOraxen()) {
            setupEntityTable("oraxen");
        }

        if (plugin.getIntegrationManager().hasPlayerNPC()) {
            setupEntityTable("playernpc");
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
            migrateRootDataSubData();

            this.url = "jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "data" + File.separator + "data.db";

            ClassUtil.loadClass("org.sqlite.JDBC");
            executeUpdate("PRAGMA journal_mode=" + plugin.getConfig()
                    .getString("settings.storage.sqlite.journal-mode", "WAL").toUpperCase() + ";");
            executeUpdate("PRAGMA synchronous=" + plugin.getConfig()
                    .getString("settings.storage.sqlite.synchronous", "OFF").toUpperCase() + ";");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void migrateRootDataSubData() {
        new File(plugin.getDataFolder(), "data").mkdirs();

        File[] files = plugin.getDataFolder().listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("data.db")) {
                    FileUtil.moveFile(file, "data" + File.separator + file.getName());
                }
            }
        }
    }

    public boolean hasChunkData(Location location) {
        return plugin.getCacheManager().getChunkMap().containsKey(LocationUtil.chunkToString(location));
    }

    public ChunkData getChunkData(Location location) {
        String chunkString = LocationUtil.chunkToString(location);
        ChunkData chunkData;

        if (plugin.getCacheManager().getChunkMap().containsKey(chunkString)) {
            chunkData = plugin.getCacheManager().getChunkMap().get(chunkString);
        } else {
            chunkData = new ChunkData(location);

            plugin.getCacheManager().getChunkMap().put(chunkString, chunkData);
        }

        return chunkData;
    }

    public void removeChunkData(ChunkData chunkData) {
        plugin.getCacheManager().getChunkMap().remove(LocationUtil.chunkToString(chunkData.getLocation()));
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
                "owner_texture_signature VARCHAR(255),\n" +
                "killer_type VARCHAR(255),\n" +
                "killer_name VARCHAR(255),\n" +
                "killer_name_display VARCHAR(255),\n" +
                "killer_uuid VARCHAR(255),\n" +
                "location_death VARCHAR(255),\n" +
                "yaw FLOAT(16),\n" +
                "pitch FLOAT(16),\n" +
                "inventory TEXT,\n" +
                "equipment TEXT,\n" +
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

        if (!columnList.contains("owner_texture_signature")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN owner_texture_signature VARCHAR(255);");
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

        if (!columnList.contains("equipment")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN equipment TEXT;");
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
                "uuid_entity VARCHAR(255),\n" +
                "uuid_grave VARCHAR(255),\n" +
                "line INT(16));");

        List<String> columnList = getColumnList(name);

        if (!columnList.contains("location")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN location VARCHAR(255);");
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

    private void setupEntityTable(String name) {
        executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (" +
                "location VARCHAR(255),\n" +
                "uuid_entity VARCHAR(255),\n" +
                "uuid_grave VARCHAR(255));");

        List<String> columnList = getColumnList(name);

        if (!columnList.contains("location")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN location VARCHAR(255);");
        }

        if (!columnList.contains("uuid_entity")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN uuid_entity VARCHAR(255);");
        }

        if (!columnList.contains("uuid_grave")) {
            executeUpdate("ALTER TABLE " + name + " ADD COLUMN uuid_grave VARCHAR(255);");
        }
    }

    private void loadGraveMap() {
        plugin.getCacheManager().getGraveMap().clear();

        ResultSet resultSet = executeQuery("SELECT * FROM grave;");

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    Grave grave = resultSetToGrave(resultSet);

                    if (grave != null) {
                        plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave);
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

    private void loadEntityMap(String table, EntityData.Type type) {
        ResultSet resultSet = executeQuery("SELECT * FROM " + table + ";");

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    Location location = null;

                    if (resultSet.getString("location") != null) {
                        location = LocationUtil.stringToLocation(resultSet.getString("location"));
                    } else if (resultSet.getString("chunk") != null) {
                        location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                    }

                    if (location != null) {
                        UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                        UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));

                        getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                    }
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
                    Location location = null;

                    if (resultSet.getString("location") != null) {
                        location = LocationUtil.stringToLocation(resultSet.getString("location"));
                    } else if (resultSet.getString("chunk") != null) {
                        location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                    }

                    if (location != null) {
                        UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                        UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                        int line = resultSet.getInt("line");

                        getChunkData(location).addEntityData(new HologramData(location, uuidEntity, uuidGrave, line));
                    }
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
                    Location location = null;

                    if (resultSet.getString("location") != null) {
                        location = LocationUtil.stringToLocation(resultSet.getString("location"));
                    } else if (resultSet.getString("chunk") != null) {
                        location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                    }
                    if (location != null) {
                        UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                        UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));

                        getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                    }
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

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("INSERT INTO block (location, uuid_grave, replace_material, replace_data) " +
                        "VALUES (" + location + ", " + uuidGrave + ", " + replaceMaterial + ", " + replaceData + ");"));
    }

    public void removeBlockData(Location location) {
        getChunkData(location).removeBlockData(location);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("DELETE FROM block WHERE location = '"
                        + LocationUtil.locationToString(location) + "';"));
    }

    public void addHologramData(HologramData hologramData) {
        getChunkData(hologramData.getLocation()).addEntityData(hologramData);

        String location = "'" + LocationUtil.locationToString(hologramData.getLocation()) + "'";
        String uuidEntity = "'" + hologramData.getUUIDEntity() + "'";
        String uuidGrave = "'" + hologramData.getUUIDGrave() + "'";
        int line = hologramData.getLine();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("INSERT INTO hologram (location, uuid_entity, uuid_grave, line) VALUES ("
                        + location + ", " + uuidEntity + ", " + uuidGrave + ", " + line + ");"));
    }

    public void removeHologramData(List<EntityData> entityDataList) {
        try {
            Statement statement = connection.createStatement();

            for (EntityData hologramData : entityDataList) {
                getChunkData(hologramData.getLocation()).removeEntityData(hologramData);
                statement.addBatch("DELETE FROM hologram WHERE uuid_entity = '"
                        + hologramData.getUUIDEntity() + "';");
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> executeBatch(statement));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void addEntityData(EntityData entityData) {
        getChunkData(entityData.getLocation()).addEntityData(entityData);

        String table = entityDataTypeTable(entityData.getType());

        if (table != null) {
            String location = "'" + LocationUtil.locationToString(entityData.getLocation()) + "'";
            String uuidEntity = "'" + entityData.getUUIDEntity() + "'";
            String uuidGrave = "'" + entityData.getUUIDGrave() + "'";

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                    executeUpdate("INSERT INTO " + table + " (location, uuid_entity, uuid_grave) VALUES ("
                            + location + ", " + uuidEntity + ", " + uuidGrave + ");"));
        }
    }

    public void removeEntityData(EntityData entityData) {
        removeEntityData(Collections.singletonList(entityData));
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
                    plugin.debugMessage("Removing " + table + " for grave "
                            + entityData.getUUIDGrave(), 1);
                }
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> executeBatch(statement));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public String entityDataTypeTable(EntityData.Type type) {
        switch (type) {
            case ARMOR_STAND:
                return "armorstand";
            case ITEM_FRAME:
                return "itemframe";
            case HOLOGRAM:
                return "hologram";
            case FURNITURELIB:
                return "furniturelib";
            case FURNITUREENGINE:
                return "furnitureengine";
            case ITEMSADDER:
                return "itemsadder";
            case ORAXEN:
                return "oraxen";
            case PLAYERNPC:
                return "playernpc";
            default:
                return type.name().toLowerCase().replace("_", "");
        }
    }

    public void addGrave(Grave grave) {
        plugin.getCacheManager().getGraveMap().put(grave.getUUID(), grave);

        String uuid = grave.getUUID() != null ? "'" + grave.getUUID() + "'" : "NULL";
        String ownerType = grave.getOwnerType() != null ? "'" + grave.getOwnerType() + "'" : "NULL";
        String ownerName = grave.getOwnerName() != null ? "'" + grave.getOwnerName()
                .replace("'", "''") + "'" : "NULL";
        String ownerNameDisplay = grave.getOwnerNameDisplay() != null ? "'" + grave.getOwnerNameDisplay()
                .replace("'", "''") + "'" : "NULL";
        String ownerUUID = grave.getOwnerUUID() != null ? "'" + grave.getOwnerUUID() + "'" : "NULL";
        String ownerTexture = grave.getOwnerTexture() != null ? "'" + grave.getOwnerTexture()
                .replace("'", "''") + "'" : "NULL";
        String ownerTextureSignature = grave.getOwnerTextureSignature() != null ? "'" + grave.getOwnerTextureSignature()
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
        String equipment = "'" + Base64Util.objectToBase64(grave.getEquipmentMap()) + "'";
        String permissions = grave.getPermissionList() != null && !grave.getPermissionList().isEmpty()
                ? "'" + StringUtils.join(grave.getPermissionList(), "|") + "'" : "NULL";
        int protection = grave.getProtection() ? 1 : 0;
        int experience = grave.getExperience();
        long timeAlive = grave.getTimeAlive();
        long timeProtection = grave.getTimeProtection();
        long timeCreation = grave.getTimeCreation();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                executeUpdate("INSERT INTO grave (uuid, owner_type, owner_name, owner_name_display, owner_uuid,"
                        + " owner_texture, owner_texture_signature, killer_type, killer_name, killer_name_display,"
                        + " killer_uuid, location_death, yaw, pitch, inventory, equipment, experience, protection, time_alive,"
                        + "time_protection, time_creation, permissions) VALUES (" + uuid + ", " + ownerType + ", "
                        + ownerName + ", " + ownerNameDisplay + ", " + ownerUUID + ", " + ownerTexture + ", "
                        + ownerTextureSignature + ", " + killerType + ", " + killerName + ", " + killerNameDisplay + ", "
                        + killerUUID + ", " + locationDeath + ", " + yaw + ", " + pitch + ", " + inventory + ", "
                        + equipment + ", " + experience + ", " + protection + ", " + timeAlive + ", "
                        + timeProtection + ", " + timeCreation + ", " + permissions + ");"));
    }

    public void removeGrave(Grave grave) {
        removeGrave(grave.getUUID());
    }

    public void removeGrave(UUID uuid) {
        plugin.getCacheManager().getGraveMap().remove(uuid);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("DELETE FROM grave WHERE uuid = '" + uuid + "';");
        });
    }

    public void updateGrave(Grave grave, String column, String string) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            executeUpdate("UPDATE grave SET " + column + " = '" + string + "' WHERE uuid = '"
                    + grave.getUUID() + "';");
        });
    }

    public Grave resultSetToGrave(ResultSet resultSet) {
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
            grave.setOwnerTextureSignature(resultSet.getString("owner_texture_signature") != null
                    ? resultSet.getString("owner_texture_signature") : null);
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

            if (resultSet.getString("equipment") != null) {
                Map<EquipmentSlot, ItemStack> equipmentMap = (Map<EquipmentSlot, ItemStack>) Base64Util
                        .base64ToObject(resultSet.getString("equipment"));

                grave.setEquipmentMap(equipmentMap != null ? equipmentMap : new HashMap<>());
            }

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

    public void closeConnection() {
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
