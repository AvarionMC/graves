package com.ranull.graves.integration;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.ranull.graves.Graves;
import com.ranull.graves.listener.integration.towny.TownBlockTypeRegisterListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class Towny {
    private final Graves plugin;
    private final Plugin townyPlugin;
    private final TownyAPI townyAPI;
    private final TownBlockTypeRegisterListener townBlockTypeRegisterListener;
    private TownBlockType graveyardBlockType;

    public Towny(Graves plugin, Plugin townyPlugin) {
        this.plugin = plugin;
        this.townyPlugin = townyPlugin;
        this.townyAPI = TownyAPI.getInstance();
        this.townBlockTypeRegisterListener = new TownBlockTypeRegisterListener(this);

        reload();
        registerGraveyardBlockType();
        registerListeners();
    }

    public boolean isEnabled() {
        return townyPlugin.isEnabled();
    }

    public void reload() {
        graveyardBlockType = new TownBlockType("Graveyard", new TownBlockData() {
            public double getTax(Town town) {
                return plugin.getConfig().getDouble("settings.graveyard.towny.tax") + town.getPlotTax();
            }
        });
    }

    public void registerGraveyardBlockType() {
        if (!TownBlockTypeHandler.exists(graveyardBlockType.getName().toLowerCase())) {
            registerType(graveyardBlockType);
        }
    }

    public TownBlockType getGraveyardBlockType() {
        return graveyardBlockType;
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(townBlockTypeRegisterListener, plugin);
    }

    public void unregisterListeners() {
        HandlerList.unregisterAll(townBlockTypeRegisterListener);
    }

    private void registerType(TownBlockType townBlockType) {
        try {
            TownBlockTypeHandler.registerType(townBlockType);
        } catch (TownyException exception) {
            exception.printStackTrace();
        }
    }

    public boolean isTownResident(Player player, String town) {
        Resident resident = townyAPI.getResident(player);

        if (resident != null && resident.hasTown()) {
            try {
                return resident.getTown().getName().equals(town);
            } catch (NotRegisteredException ignored) {
            }
        }

        return false;
    }

    public TownBlock getGraveyardTownBlock(Location location) {
        TownBlock townBlock = townyAPI.getTownBlock(location);

        return townBlock != null && townBlock.getType() == graveyardBlockType ? townBlock : null;
    }

    public boolean hasTownPlot(Player player, String name) {
        return !getTownPlotsByName(player, name).isEmpty();
    }

    public List<TownBlock> getTownPlotsByName(Player player, String name) {
        Resident resident = townyAPI.getResident(player);

        return resident != null && resident.getTownOrNull() != null
                ? getTownPlotsByName(resident.getTownOrNull(), name) : new ArrayList<>();
    }


    public List<TownBlock> getTownPlotsByName(Location location, String name) {
        Town town = townyAPI.getTown(location);

        return town != null ? getTownPlotsByName(town, name) : new ArrayList<>();
    }

    public List<TownBlock> getTownPlotsByName(Town town, String name) {
        List<TownBlock> townBlockList = new ArrayList<>();

        for (TownBlock townBlock : town.getTownBlocks()) {
            if (townBlock.getName().equals(name)) {
                townBlockList.add(townBlock);
            }
        }

        return townBlockList;
    }

    public boolean isResident(String region, Player player) {
        return true; // TODO
    }

    public boolean isInsidePlot(Location location, String name) {
        TownBlock townBlock = townyAPI.getTownBlock(location);

        return townBlock != null && townBlock.getName().equals(name);
    }

    public boolean isLocationGraveyardBlockType(Location location) {
        TownBlock townBlock = townyAPI.getTownBlock(location);

        return townBlock != null && townBlock.getType() == graveyardBlockType;
    }
}
