package com.ranull.graves.type;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.HashMap;
import java.util.Map;

public class Graveyard {
    private final String name;
    private final World world;
    private final Graveyard.Type type;
    private final Map<Location, BlockFace> graveLocationMap;
    private Location spawnLocation;
    private String title;
    private String description;
    private boolean isPublic;

    public Graveyard(String name, World world, Graveyard.Type type) {
        this.name = name;
        this.world = world;
        this.type = type;
        this.graveLocationMap = new HashMap<>();
    }

    public String getKey() {
        return type.name().toLowerCase() + "|" + world.getName() + "|" + name;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public Graveyard.Type getType() {
        return type;
    }

    public void addGraveLocation(Location location, BlockFace blockFace) {
        graveLocationMap.put(location, blockFace);
    }

    public void removeGraveLocation(Location location) {
        graveLocationMap.remove(location);
    }

    public boolean hasGraveLocation(Location location) {
        return graveLocationMap.containsKey(location);
    }

    public Map<Location, BlockFace> getGraveLocationMap() {
        return graveLocationMap;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public enum PRIORITY {
        TOWNY_TOWN,
        TOWNY_NATION,
        TOWNY_PUBLIC,
        FACTIONS_FACTION,
        FACTIONS_ALLY,
        FACTIONS_PUBLIC,
        WORLDGUARD_OWNER,
        WORLDGUARD_MEMBER,
        WORLDGUARD_PUBLIC
    }

    public enum Type {
        WORLDGUARD,
        TOWNY,
        FACTIONS
    }
}
