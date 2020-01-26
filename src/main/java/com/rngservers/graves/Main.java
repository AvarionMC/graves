package com.rngservers.graves;

import com.rngservers.graves.commands.Graves;
import com.rngservers.graves.data.DataManager;
import com.rngservers.graves.events.Events;
import com.rngservers.graves.grave.GraveManager;
import com.rngservers.graves.grave.Messages;
import com.rngservers.graves.gui.GUIManager;
import com.rngservers.graves.hooks.Vault;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    GraveManager graveManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Vault vault = new Vault();
        if (!vault.setupEconomy()) {
            vault = null;
        }

        DataManager data = new DataManager(this);
        Messages messages = new Messages(this);
        graveManager = new GraveManager(this, data, messages);
        GUIManager guiManager = new GUIManager(this, graveManager, vault);

        this.getCommand("graves").setExecutor(new Graves(this, data, graveManager, guiManager, messages));
        this.getServer().getPluginManager().registerEvents(new Events(this, graveManager, guiManager, messages), this);
    }

    @Override
    public void onDisable() {
        graveManager.removeHolograms();
        graveManager.closeGraves();
        graveManager.saveGraves();
    }
}
