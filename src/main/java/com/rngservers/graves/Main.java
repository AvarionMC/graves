package com.rngservers.graves;

import com.rngservers.graves.grave.GraveManager;
import com.rngservers.graves.commands.Graves;
import com.rngservers.graves.data.DataManager;
import com.rngservers.graves.events.Events;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    GraveManager graveManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        DataManager data = new DataManager(this);
        graveManager = new GraveManager(this, data);

        this.getCommand("graves").setExecutor(new Graves(this, data));
        this.getServer().getPluginManager().registerEvents(new Events(this, graveManager), this);
    }

    @Override
    public void onDisable() {
        graveManager.saveGraves();
    }
}
