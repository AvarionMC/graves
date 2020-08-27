package com.ranull.graves;

import com.ranull.graves.commands.GravesCommand;
import com.ranull.graves.listeners.Events;
import com.ranull.graves.hooks.VaultHook;
import com.ranull.graves.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Graves extends JavaPlugin {
    GraveManager graveManager;
    GUIManager guiManager;
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

        guiManager = new GUIManager(this, graveManager, vaultHook);

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

    public GUIManager getGuiManager() {
        return guiManager;
    }
}
