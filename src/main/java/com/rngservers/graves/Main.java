package com.rngservers.graves;

import com.rngservers.graves.commands.Graves;
import com.rngservers.graves.data.DataManager;
import com.rngservers.graves.events.Events;
import com.rngservers.graves.grave.GraveManager;
import com.rngservers.graves.messages.Messages;
import com.rngservers.graves.gui.GUIManager;
import com.rngservers.graves.hooks.Vault;
import com.rngservers.graves.recipe.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    GraveManager graveManager;
    RecipeManager recipeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Vault vault = new Vault();
        if (!vault.setupEconomy()) {
            vault = null;
        }

        Messages messages = new Messages(this);
        DataManager data = new DataManager(this);
        graveManager = new GraveManager(this, data, messages);

        recipeManager = new RecipeManager(this, graveManager);
        recipeManager.loadRecipes();

        GUIManager guiManager = new GUIManager(this, graveManager, vault);

        this.getCommand("graves").setExecutor(new Graves(this, data, graveManager, guiManager, recipeManager, messages));
        this.getServer().getPluginManager().registerEvents(new Events(this, graveManager, guiManager, messages), this);
    }

    @Override
    public void onDisable() {
        graveManager.removeHolograms();
        graveManager.closeGraves();
        graveManager.saveGraves();
        recipeManager.unloadRecipes();
    }
}
