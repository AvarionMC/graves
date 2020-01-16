package com.rngservers.graves;

import com.rngservers.graves.commands.Graves;
import com.rngservers.graves.data.DataManager;
import com.rngservers.graves.events.Events;
import com.rngservers.graves.grave.GraveManager;
import com.rngservers.graves.grave.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    GraveManager graveManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        DataManager data = new DataManager(this);
        Messages messages = new Messages(this);
        graveManager = new GraveManager(this, data, messages);

        this.getCommand("graves").setExecutor(new Graves(this, data, graveManager));
        this.getServer().getPluginManager().registerEvents(new Events(this, graveManager, messages), this);
    }

    @Override
    public void onDisable() {
        graveManager.closeGraves();
        graveManager.saveGraves();
    }
}
