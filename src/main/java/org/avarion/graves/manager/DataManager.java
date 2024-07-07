package org.avarion.graves.manager;

import org.apache.commons.lang3.StringUtils;
import org.avarion.graves.Graves;
import org.avarion.graves.data.BlockData;
import org.avarion.graves.data.ChunkData;
import org.avarion.graves.data.EntityData;
import org.avarion.graves.data.HologramData;
import org.avarion.graves.type.Grave;
import org.avarion.graves.util.*;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.*;

public final class DataManager {

    private final Graves plugin;
    private Type type = DataManager.Type.SQLITE;
    private String url;
    private Connection connection;

    public DataManager(Graves plugin) {
        this.plugin = plugin;

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
                loadEntityMap("furniturelib", EntityData.Type.FURNITURELIB);
            }

            if (plugin.getIntegrationManager().hasFurnitureEngine()) {
                loadEntityMap("furnitureengine", EntityData.Type.FURNITUREENGINE);
            }

            if (plugin.getIntegrationManager().hasItemsAdder()) {
                loadEntityMap("itemsadder", EntityData.Type.ITEMSADDER);
            }

            if (plugin.getIntegrationManager().hasOraxen()) {
                loadEntityMap("oraxen", EntityData.Type.ORAXEN);
            }

            if (plugin.getIntegrationManager().hasPlayerNPC()) {
                loadEntityMap("playernpc", EntityData.Type.PLAYERNPC);
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
        }
        else {
            migrateRootDataSubData();

            this.url = "jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "data" + File.separator + "data.db";

            ClassUtil.loadClass("org.sqlite.JDBC");
            executeUpdate("PRAGMA journal_mode=" + plugin.getConfig()
                                                         .getString("settings.storage.sqlite.journal-mode", "WAL")
                                                         .toUpperCase() + ";");
            executeUpdate("PRAGMA synchronous=" + plugin.getConfig()
                                                        .getString("settings.storage.sqlite.synchronous", "OFF")
                                                        .toUpperCase() + ";");
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
        return CacheManager.chunkMap.containsKey(LocationUtil.chunkToString(location));
    }

    public ChunkData getChunkData(Location location) {
        String chunkString = LocationUtil.chunkToString(location);
        ChunkData chunkData;

        if (CacheManager.chunkMap.containsKey(chunkString)) {
            chunkData = CacheManager.chunkMap.get(chunkString);
        }
        else {
            chunkData = new ChunkData(location);

            CacheManager.chunkMap.put(chunkString, chunkData);
        }

        return chunkData;
    }

    public void removeChunkData(@NotNull ChunkData chunkData) {
        CacheManager.chunkMap.remove(LocationUtil.chunkToString(chunkData.getLocation()));
    }

    public @NotNull List<String> getColumnList(String tableName) {
        List<String> columnList = new ArrayList<>();
        ResultSet resultSet;

        if (type == Type.MYSQL) {
            resultSet = null; // TODO MYSQL
        }
        else {
            resultSet = executeQuery("PRAGMA table_info(" + tableName + ");");
        }

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    columnList.add(resultSet.getString("name"));
                }
            }
            catch (SQLException exception) {
                exception.printStackTrace();
            }
        }

        return columnList;
    }

    private void createOrUpdateTable(String tableName, @NotNull Map<String, String> fields) {
        if (fields.isEmpty()) {
            return;
        }

        // 1. create the table SQL
        StringBuilder sql = new StringBuilder();

        sql.append("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName);
        sql.append(" (");

        final boolean[] isFirst = {true}; // allow modification inside lambda
        fields.forEach((key, value) -> {
            if (!isFirst[0]) {
                sql.append(", ");
            }

            sql.append(key);
            sql.append(" ");
            sql.append(value);

            isFirst[0] = false;
        });
        sql.append(");");

        // 2. Create the table if it's not there already
        executeUpdate(sql.toString());

        // 3. Verify that all fields are there
        List<String> columnList = getColumnList(tableName);
        fields.forEach((key, value) -> {
            if (columnList.contains(key)) {
                return;
            }
            executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + key + " " + value + ";");
        });
    }

    public void setupGraveTable() {
        Map<String, String> fields = new HashMap<>();

        fields.put("uuid", "VARCHAR(255) UNIQUE");
        fields.put("owner_type", "VARCHAR(255)");
        fields.put("owner_name", "VARCHAR(255)");
        fields.put("owner_name_display", "VARCHAR(255)");
        fields.put("owner_uuid", "VARCHAR(255)");
        fields.put("owner_texture", "VARCHAR(255)");
        fields.put("owner_texture_signature", "VARCHAR(255)");
        fields.put("killer_type", "VARCHAR(255)");
        fields.put("killer_name", "VARCHAR(255)");
        fields.put("killer_name_display", "VARCHAR(255)");
        fields.put("killer_uuid", "VARCHAR(255)");
        fields.put("location_death", "VARCHAR(255)");
        fields.put("yaw", "FLOAT(16)");
        fields.put("pitch", "FLOAT(16)");
        fields.put("inventory", "TEXT");
        fields.put("equipment", "TEXT");
        fields.put("experience", "INT(16)");
        fields.put("protection", "INT(1)");
        fields.put("time_alive", "INT(16)");
        fields.put("time_protection", "INT(11)");
        fields.put("time_creation", "INT(11)");
        fields.put("permissions", "TEXT");

        createOrUpdateTable("grave", fields);
    }

    public void setupBlockTable() {
        Map<String, String> fields = new HashMap<>();

        fields.put("location", "VARCHAR(255)");
        fields.put("uuid_grave", "VARCHAR(255)");
        fields.put("replace_material", "VARCHAR(255)");
        fields.put("replace_data", "TEXT");

        createOrUpdateTable("block", fields);
    }

    public void setupHologramTable() {
        Map<String, String> fields = new HashMap<>();

        fields.put("location", "VARCHAR(255)");
        fields.put("uuid_entity", "VARCHAR(255)");
        fields.put("uuid_grave", "VARCHAR(255)");
        fields.put("line", "INT(16)");

        createOrUpdateTable("hologram", fields);
    }

    private void setupEntityTable(String name) {
        Map<String, String> fields = new HashMap<>();

        fields.put("location", "VARCHAR(255)");
        fields.put("uuid_entity", "VARCHAR(255)");
        fields.put("uuid_grave", "VARCHAR(255)");

        createOrUpdateTable(name, fields);
    }

    private void loadGraveMap() {
        CacheManager.graveMap.clear();

        ResultSet resultSet = executeQuery("SELECT * FROM grave;");

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    Grave grave = resultSetToGrave(resultSet);

                    if (grave != null) {
                        CacheManager.graveMap.put(grave.getUUID(), grave);
                    }
                }
            }
            catch (SQLException exception) {
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

                    getChunkData(location).addBlockData(new BlockData(location, uuidGrave, replaceMaterial, replaceData));
                }
            }
            catch (SQLException exception) {
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
                    }
                    else if (resultSet.getString("chunk") != null) {
                        location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                    }

                    if (location != null) {
                        UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                        UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));

                        getChunkData(location).addEntityData(new EntityData(location, uuidEntity, uuidGrave, type));
                    }
                }
            }
            catch (SQLException exception) {
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
                    }
                    else if (resultSet.getString("chunk") != null) {
                        location = LocationUtil.chunkStringToLocation(resultSet.getString("chunk"));
                    }

                    if (location != null) {
                        UUID uuidEntity = UUID.fromString(resultSet.getString("uuid_entity"));
                        UUID uuidGrave = UUID.fromString(resultSet.getString("uuid_grave"));
                        int line = resultSet.getInt("line");

                        getChunkData(location).addEntityData(new HologramData(location, uuidEntity, uuidGrave, line));
                    }
                }
            }
            catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void addBlockData(@NotNull BlockData blockData) {
        getChunkData(blockData.location()).addBlockData(blockData);

        String uuidGrave = blockData.graveUUID() != null ? "'" + blockData.graveUUID() + "'" : "NULL";
        String location = "'" + LocationUtil.locationToString(blockData.location()) + "'";
        String replaceMaterial = blockData.replaceMaterial() != null ? "'" + blockData.replaceMaterial() + "'"
                                 : "NULL";
        String replaceData = blockData.replaceData() != null ? "'" + blockData.replaceData() + "'" : "NULL";

        plugin.getServer()
              .getScheduler()
              .runTaskAsynchronously(plugin, () -> executeUpdate(
                      "INSERT INTO block (location, uuid_grave, replace_material, replace_data) "
                      + "VALUES ("
                      + location
                      + ", "
                      + uuidGrave
                      + ", "
                      + replaceMaterial
                      + ", "
                      + replaceData
                      + ");"));
    }

    public void removeBlockData(Location location) {
        getChunkData(location).removeBlockData(location);

        plugin.getServer()
              .getScheduler()
              .runTaskAsynchronously(plugin, () -> executeUpdate("DELETE FROM block WHERE location = '"
                                                                 + LocationUtil.locationToString(location)
                                                                 + "';"));
    }

    public void addHologramData(@NotNull HologramData hologramData) {
        getChunkData(hologramData.getLocation()).addEntityData(hologramData);

        String location = "'" + LocationUtil.locationToString(hologramData.getLocation()) + "'";
        String uuidEntity = "'" + hologramData.getUUIDEntity() + "'";
        String uuidGrave = "'" + hologramData.getUUIDGrave() + "'";
        int line = hologramData.getLine();

        plugin.getServer()
              .getScheduler()
              .runTaskAsynchronously(plugin, () -> executeUpdate(
                      "INSERT INTO hologram (location, uuid_entity, uuid_grave, line) VALUES ("
                      + location
                      + ", "
                      + uuidEntity
                      + ", "
                      + uuidGrave
                      + ", "
                      + line
                      + ");"));
    }

    public void removeHologramData(@NotNull List<EntityData> entityDataList) {
        try {
            Statement statement = connection.createStatement();

            for (EntityData hologramData : entityDataList) {
                getChunkData(hologramData.getLocation()).removeEntityData(hologramData);
                statement.addBatch("DELETE FROM hologram WHERE uuid_entity = '" + hologramData.getUUIDEntity() + "';");
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> executeBatch(statement));
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void addEntityData(@NotNull EntityData entityData) {
        getChunkData(entityData.getLocation()).addEntityData(entityData);

        String table = entityDataTypeTable(entityData.getType());

        if (table != null) {
            String location = "'" + LocationUtil.locationToString(entityData.getLocation()) + "'";
            String uuidEntity = "'" + entityData.getUUIDEntity() + "'";
            String uuidGrave = "'" + entityData.getUUIDGrave() + "'";

            plugin.getServer()
                  .getScheduler()
                  .runTaskAsynchronously(plugin, () -> executeUpdate("INSERT INTO "
                                                                     + table
                                                                     + " (location, uuid_entity, uuid_grave) VALUES ("
                                                                     + location
                                                                     + ", "
                                                                     + uuidEntity
                                                                     + ", "
                                                                     + uuidGrave
                                                                     + ");"));
        }
    }

    public void removeEntityData(EntityData entityData) {
        removeEntityData(Collections.singletonList(entityData));
    }

    public void removeEntityData(@NotNull List<EntityData> entityDataList) {
        try {
            Statement statement = connection.createStatement();

            for (EntityData entityData : entityDataList) {
                getChunkData(entityData.getLocation()).removeEntityData(entityData);

                String table = entityDataTypeTable(entityData.getType());

                if (table != null) {
                    statement.addBatch("DELETE FROM "
                                       + table
                                       + " WHERE uuid_entity = '"
                                       + entityData.getUUIDEntity()
                                       + "';");
                    plugin.debugMessage("Removing " + table + " for grave " + entityData.getUUIDGrave(), 1);
                }
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> executeBatch(statement));
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public String entityDataTypeTable(EntityData.@NotNull Type type) {
        return switch (type) {
            case ARMOR_STAND -> "armorstand";
            case ITEM_FRAME -> "itemframe";
            case HOLOGRAM -> "hologram";
            case FURNITURELIB -> "furniturelib";
            case FURNITUREENGINE -> "furnitureengine";
            case ITEMSADDER -> "itemsadder";
            case ORAXEN -> "oraxen";
            case PLAYERNPC -> "playernpc";
            default -> type.name().toLowerCase().replace("_", "");
        };
    }

    public void addGrave(Grave grave) {
        CacheManager.graveMap.put(grave.getUUID(), grave);

        String uuid = grave.getUUID() != null ? "'" + grave.getUUID() + "'" : "NULL";
        String ownerType = grave.getOwnerType() != null ? "'" + grave.getOwnerType() + "'" : "NULL";
        String ownerName = grave.getOwnerName() != null ? "'" + grave.getOwnerName().replace("'", "''") + "'" : "NULL";
        String ownerNameDisplay = grave.getOwnerNameDisplay() != null ? "'" + grave.getOwnerNameDisplay()
                                                                                   .replace("'", "''") + "'" : "NULL";
        String ownerUUID = grave.getOwnerUUID() != null ? "'" + grave.getOwnerUUID() + "'" : "NULL";
        String ownerTexture = grave.getOwnerTexture() != null
                              ? "'" + grave.getOwnerTexture().replace("'", "''") + "'"
                              : "NULL";
        String ownerTextureSignature = grave.getOwnerTextureSignature() != null
                                       ? "'" + grave.getOwnerTextureSignature()
                                                    .replace("'", "''") + "'"
                                       : "NULL";
        String killerType = grave.getKillerType() != null ? "'" + grave.getKillerType() + "'" : "NULL";
        String killerName = grave.getKillerName() != null
                            ? "'" + grave.getKillerName().replace("'", "''") + "'"
                            : "NULL";
        String killerNameDisplay = grave.getKillerNameDisplay() != null ? "'" + grave.getKillerNameDisplay()
                                                                                     .replace("'", "''") + "'" : "NULL";
        String killerUUID = grave.getKillerUUID() != null ? "'" + grave.getKillerUUID() + "'" : "NULL";
        String locationDeath = grave.getLocationDeath() != null ? "'"
                                                                  + LocationUtil.locationToString(grave.getLocationDeath())
                                                                  + "'" : "NULL";
        float yaw = grave.getYaw();
        float pitch = grave.getPitch();
        String inventory = "'" + InventoryUtil.inventoryToString(grave.getInventory()) + "'";
        String equipment = "'" + Base64Util.objectToBase64(grave.getEquipmentMap()) + "'";
        String permissions = grave.getPermissionList() != null && !grave.getPermissionList().isEmpty() ? "'"
                                                                                                         + StringUtils.join(grave.getPermissionList(), "|")
                                                                                                         + "'" : "NULL";
        int protection = grave.getProtection() ? 1 : 0;
        int experience = grave.getExperience();
        long timeAlive = grave.getTimeAlive();
        long timeProtection = grave.getTimeProtection();
        long timeCreation = grave.getTimeCreation();

        plugin.getServer()
              .getScheduler()
              .runTaskAsynchronously(plugin, () -> executeUpdate(
                      "INSERT INTO grave (uuid, owner_type, owner_name, owner_name_display, owner_uuid,"
                      + " owner_texture, owner_texture_signature, killer_type, killer_name, killer_name_display,"
                      + " killer_uuid, location_death, yaw, pitch, inventory, equipment, experience, protection, time_alive,"
                      + "time_protection, time_creation, permissions) VALUES ("
                      + uuid
                      + ", "
                      + ownerType
                      + ", "
                      + ownerName
                      + ", "
                      + ownerNameDisplay
                      + ", "
                      + ownerUUID
                      + ", "
                      + ownerTexture
                      + ", "
                      + ownerTextureSignature
                      + ", "
                      + killerType
                      + ", "
                      + killerName
                      + ", "
                      + killerNameDisplay
                      + ", "
                      + killerUUID
                      + ", "
                      + locationDeath
                      + ", "
                      + yaw
                      + ", "
                      + pitch
                      + ", "
                      + inventory
                      + ", "
                      + equipment
                      + ", "
                      + experience
                      + ", "
                      + protection
                      + ", "
                      + timeAlive
                      + ", "
                      + timeProtection
                      + ", "
                      + timeCreation
                      + ", "
                      + permissions
                      + ");"));
    }

    public void removeGrave(@NotNull Grave grave) {
        removeGrave(grave.getUUID());
    }

    public void removeGrave(UUID uuid) {
        CacheManager.graveMap.remove(uuid);

        plugin.getServer()
              .getScheduler()
              .runTaskAsynchronously(plugin, () -> executeUpdate("DELETE FROM grave WHERE uuid = '" + uuid + "';"));
    }

    public void updateGrave(Grave grave, String column, String string) {
        plugin.getServer()
              .getScheduler()
              .runTaskAsynchronously(plugin, () -> executeUpdate("UPDATE grave SET "
                                                                 + column
                                                                 + " = '"
                                                                 + string
                                                                 + "' WHERE uuid = '"
                                                                 + grave.getUUID()
                                                                 + "';"));
    }

    @SuppressWarnings("unchecked")
    public @Nullable Grave resultSetToGrave(@NotNull ResultSet resultSet) {
        try {
            Grave grave = new Grave(UUID.fromString(resultSet.getString("uuid")));

            grave.setOwnerType(resultSet.getString("owner_type") != null
                               ? EntityType.valueOf(resultSet.getString("owner_type"))
                               : null);
            grave.setOwnerName(resultSet.getString("owner_name") != null ? resultSet.getString("owner_name") : null);
            grave.setOwnerNameDisplay(resultSet.getString("owner_name_display") != null
                                      ? resultSet.getString("owner_name_display")
                                      : null);
            grave.setOwnerUUID(resultSet.getString("owner_uuid") != null
                               ? UUID.fromString(resultSet.getString("owner_uuid"))
                               : null);
            grave.setOwnerTexture(resultSet.getString("owner_texture") != null
                                  ? resultSet.getString("owner_texture")
                                  : null);
            grave.setOwnerTextureSignature(resultSet.getString("owner_texture_signature") != null
                                           ? resultSet.getString("owner_texture_signature")
                                           : null);
            grave.setKillerType(resultSet.getString("killer_type") != null
                                ? EntityType.valueOf(resultSet.getString("killer_type"))
                                : null);
            grave.setKillerName(resultSet.getString("killer_name") != null ? resultSet.getString("killer_name") : null);
            grave.setKillerNameDisplay(resultSet.getString("killer_name_display") != null
                                       ? resultSet.getString("killer_name_display")
                                       : null);
            grave.setKillerUUID(resultSet.getString("killer_uuid") != null
                                ? UUID.fromString(resultSet.getString("killer_uuid"))
                                : null);
            grave.setLocationDeath(resultSet.getString("location_death") != null
                                   ? LocationUtil.stringToLocation(resultSet.getString("location_death"))
                                   : null);
            grave.setYaw(resultSet.getFloat("yaw"));
            grave.setPitch(resultSet.getFloat("pitch"));
            grave.setExperience(resultSet.getInt("experience"));
            grave.setProtection(resultSet.getInt("protection") == 1);
            grave.setTimeAlive(resultSet.getLong("time_alive"));
            grave.setTimeProtection(resultSet.getLong("time_protection"));
            grave.setTimeCreation(resultSet.getLong("time_creation"));
            grave.setPermissionList(resultSet.getString("permissions") != null
                                    ? new ArrayList<>(Arrays.asList(resultSet.getString("permissions").split("\\|")))
                                    : null);
            grave.setInventory(InventoryUtil.stringToInventory(grave, resultSet.getString("inventory"), StringUtil.parseString(plugin.getConfigString("gui.grave.title", grave), grave.getLocationDeath(), grave, plugin), plugin));

            if (resultSet.getString("equipment") != null) {
                Map<EquipmentSlot, ItemStack> equipmentMap = (Map<EquipmentSlot, ItemStack>) Base64Util.base64ToObject(resultSet.getString("equipment"));

                grave.setEquipmentMap(equipmentMap != null ? equipmentMap : new HashMap<>());
            }

            return grave;
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    private boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        }
        catch (SQLException exception) {
            exception.printStackTrace();

            return false;
        }
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(url);
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void closeConnection() {
        if (isConnected()) {
            try {
                connection.close();
            }
            catch (SQLException exception) {
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
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void executeUpdate(String sql) {
        if (!isConnected()) {
            connect();
        }

        try {
            Statement stmt = connection.createStatement();
            stmt.closeOnCompletion();
            stmt.executeUpdate(sql);
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private @Nullable ResultSet executeQuery(String sql) {
        if (!isConnected()) {
            connect();
        }

        try {
            Statement stmt = connection.createStatement();
            stmt.closeOnCompletion();
            return stmt.executeQuery(sql);
        }
        catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public enum Type {
        SQLITE, MYSQL
    }

}
