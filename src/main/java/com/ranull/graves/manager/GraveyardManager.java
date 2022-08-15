package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Graveyard;
import com.ranull.graves.util.BlockFaceUtil;
import com.ranull.graves.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GraveyardManager {
    private final Graves plugin;
    private final Map<String, Graveyard> graveyardMap;
    private final Map<UUID, Graveyard> modifyingGraveyardMap;

    public GraveyardManager(Graves plugin) {
        this.plugin = plugin;
        this.graveyardMap = new HashMap<>();
        this.modifyingGraveyardMap = new HashMap<>();
    }

    public void unload() {
        for (Map.Entry<UUID, Graveyard> entry : modifyingGraveyardMap.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());

            if (player != null) {
                stopModifyingGraveyard(player);
            }
        }
    }

    public Graveyard getGraveyardByKey(String key) {
        return graveyardMap.get(key);
    }

    public Graveyard createGraveyard(Location location, String name, World world, Graveyard.Type type) {
        Graveyard graveyard = new Graveyard(name, world, type);

        graveyard.setSpawnLocation(location);
        graveyardMap.put(graveyard.getKey(), graveyard);

        return graveyard;
    }

    public void addLocationInGraveyard(Player player, Location location, Graveyard graveyard) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!graveyard.hasGraveLocation(location)) {
                BlockFace blockFace = BlockFaceUtil.getYawBlockFace(player.getLocation().getYaw()).getOppositeFace();

                graveyard.addGraveLocation(location, blockFace);
                previewLocation(player, location, blockFace);
                player.sendMessage("set block in graveyard");
            }
        });
    }

    public void removeLocationInGraveyard(Player player, Location location, Graveyard graveyard) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (graveyard.hasGraveLocation(location)) {
                graveyard.removeGraveLocation(location);
                refreshLocation(player, location);
                player.sendMessage("remove block in graveyard");
            }
        });
    }

    public Map<Location, BlockFace> getGraveyardFreeSpaces(Graveyard graveyard) {
        Map<Location, BlockFace> locationMap = new HashMap<>(graveyard.getGraveLocationMap());

        locationMap.entrySet().removeAll(getGraveyardUsedSpaces(graveyard).entrySet());

        return locationMap;
    }

    public Map<Location, BlockFace> getGraveyardUsedSpaces(Graveyard graveyard) {
        Map<Location, BlockFace> locationMap = new HashMap<>();

        for (Map.Entry<Location, BlockFace> entry : graveyard.getGraveLocationMap().entrySet()) {
            if (plugin.getBlockManager().getGraveFromBlock(entry.getKey().getBlock()) != null) {
                locationMap.put(entry.getKey(), entry.getValue());
            }
        }

        return locationMap;
    }

    public boolean isModifyingGraveyard(Player player) {
        return modifyingGraveyardMap.containsKey(player.getUniqueId());
    }

    public Graveyard getModifyingGraveyard(Player player) {
        return modifyingGraveyardMap.get(player.getUniqueId());
    }

    public void startModifyingGraveyard(Player player, Graveyard graveyard) {
        if (isModifyingGraveyard(player)) {
            stopModifyingGraveyard(player);
        }

        modifyingGraveyardMap.put(player.getUniqueId(), graveyard);

        for (Map.Entry<Location, BlockFace> entry : graveyard.getGraveLocationMap().entrySet()) {
            previewLocation(player, entry.getKey(), entry.getValue());
        }

        player.sendMessage("starting modifying graveyard " + graveyard.getName());
    }

    public void stopModifyingGraveyard(Player player) {
        Graveyard graveyard = getModifyingGraveyard(player);

        if (graveyard != null) {
            modifyingGraveyardMap.remove(player.getUniqueId());

            for (Location location : graveyard.getGraveLocationMap().keySet()) {
                refreshLocation(player, location);
            }

            player.sendMessage("stop modifying graveyard " + graveyard.getName());
            // TODO SAVE GRAVEYARD
        }
    }

    public boolean isLocationInGraveyard(Location location, Graveyard graveyard) {
        switch (graveyard.getType()) {
            case WORLDGUARD:
                return plugin.getIntegrationManager().getWorldGuard() != null
                        && plugin.getIntegrationManager().getWorldGuard().isInsideRegion(location, graveyard.getName());
            case TOWNY:
                return plugin.getIntegrationManager().hasTowny()
                        && plugin.getIntegrationManager().getTowny().isInsidePlot(location, graveyard.getName());
            default:
                return false;
        }
    }

    public Graveyard getClosestGraveyard(Location location, Entity entity) {
        Map<Location, Graveyard> locationGraveyardMap = new HashMap<>();

        for (Graveyard graveyard : graveyardMap.values()) {
            if (graveyard.getSpawnLocation() != null) {
                switch (graveyard.getType()) {
                    case WORLDGUARD:
                        if (graveyard.isPublic() || (!(entity instanceof Player)
                                || (plugin.getIntegrationManager().getWorldGuard() != null
                                && plugin.getIntegrationManager().getWorldGuard()
                                .isMember(graveyard.getName(), (Player) entity)))) {
                            locationGraveyardMap.put(graveyard.getSpawnLocation(), graveyard);
                        }

                        break;
                    case TOWNY:
                        if (graveyard.isPublic() || (!(entity instanceof Player)
                                || (plugin.getIntegrationManager().hasTowny()
                                && plugin.getIntegrationManager().getTowny()
                                .isResident(graveyard.getName(), (Player) entity)))) {
                            locationGraveyardMap.put(graveyard.getSpawnLocation(), graveyard);
                        }

                        break;
                }
            }
        }

        return !locationGraveyardMap.isEmpty() ? locationGraveyardMap.get(LocationUtil
                .getClosestLocation(location, new ArrayList<>(locationGraveyardMap.keySet()))) : null;
    }

    private void previewLocation(Player player, Location location, BlockFace blockFace) {
        if (plugin.getIntegrationManager().hasProtocolLib()) {
            plugin.getIntegrationManager().getProtocolLib().setBlock(location.getBlock(), Material.PLAYER_HEAD, player);
        }
    }

    private void refreshLocation(Player player, Location location) {
        if (plugin.getIntegrationManager().hasProtocolLib()) {
            plugin.getIntegrationManager().getProtocolLib().refreshBlock(location.getBlock(), player);
        }
    }
}