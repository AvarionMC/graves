package com.rngservers.graves;

import com.rngservers.graves.commands.GravesCommand;
import com.rngservers.graves.events.Events;
import com.rngservers.graves.hooks.VaultHook;
import com.rngservers.graves.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Graves extends JavaPlugin {
    GraveManager graveManager;
    RecipeManager recipeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        VaultHook vaultHook = new VaultHook();
        if (!vaultHook.setupEconomy()) {
            vaultHook = null;
        }

        MessageManager messageManager = new MessageManager(this);
        DataManager data = new DataManager(this);
        graveManager = new GraveManager(this, data, messageManager);

        recipeManager = new RecipeManager(this, graveManager);
        recipeManager.loadRecipes();

        GUIManager guiManager = new GUIManager(this, graveManager, vaultHook);

        Objects.requireNonNull(getCommand("graves")).
                setExecutor(new GravesCommand(this, data, graveManager, guiManager, recipeManager, messageManager));

        getServer().getPluginManager().registerEvents(new Events(this, graveManager, guiManager, messageManager), this);
    }

    @Override
    public void onDisable() {
        graveManager.removeHolograms();
        graveManager.closeGraves();
        graveManager.saveGraves();

        recipeManager.unloadRecipes();
    }
}
