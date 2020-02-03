package com.rngservers.graves.recipe;

import com.rngservers.graves.Main;
import com.rngservers.graves.grave.GraveManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.List;

public class RecipeManager {
    private Main plugin;
    private GraveManager graveManager;

    public RecipeManager(Main plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    public void loadRecipes() {
        Boolean graveToken = plugin.getConfig().getBoolean("settings.graveToken");
        Boolean graveTokenRecipeEnabled = plugin.getConfig().getBoolean("settings.graveTokenRecipeEnabled");
        if (graveToken && graveTokenRecipeEnabled) {
            graveTokenRecipe();
        }
    }

    public void unloadRecipes() {
        Material tokenMaterial = Material.matchMaterial(plugin.getConfig().getString("settings.graveTokenItem"));
        if (tokenMaterial != null) {
            Iterator<Recipe> recipes = plugin.getServer().recipeIterator();
            while (recipes.hasNext()) {
                Recipe recipe = recipes.next();
                if (recipe != null) {
                    ItemStack item = recipe.getResult();
                    if (item.hasItemMeta()) {
                        if (hasRecipeData(item)) {
                            recipes.remove();
                        }
                    }
                }
            }
        }
    }

    public void graveTokenRecipe() {
        Material tokenMaterial = Material.matchMaterial(plugin.getConfig().getString("settings.graveTokenItem"));
        if (tokenMaterial != null) {
            ItemStack item = graveManager.getGraveToken();

            NamespacedKey key = new NamespacedKey(plugin, "grave_token");
            ShapedRecipe recipe = new ShapedRecipe(key, item);

            recipe.shape("ABC", "DEF", "GHI");

            List<String> lines = plugin.getConfig().getStringList("settings.graveTokenRecipe");
            Integer recipeKey = 1;
            for (String string : lines.get(0).split(" ")) {
                Material material = Material.matchMaterial(string);
                if (material != null) {
                    recipe.setIngredient(getChar(recipeKey), material);
                }
                recipeKey++;
            }
            for (String string : lines.get(1).split(" ")) {
                Material material = Material.matchMaterial(string);
                if (material != null) {
                    recipe.setIngredient(getChar(recipeKey), material);
                }
                recipeKey++;
            }
            for (String string : lines.get(2).split(" ")) {
                Material material = Material.matchMaterial(string);
                if (material != null) {
                    recipe.setIngredient(getChar(recipeKey), material);
                }
                recipeKey++;
            }
            plugin.getServer().addRecipe(recipe);
        }
    }

    public Boolean hasRecipeData(ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "gravesRecipe");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }

    public char getChar(Integer count) {
        switch (count) {
            case 1:
                return 'A';
            case 2:
                return 'B';
            case 3:
                return 'C';
            case 4:
                return 'D';
            case 5:
                return 'E';
            case 6:
                return 'F';
            case 7:
                return 'G';
            case 8:
                return 'H';
            case 9:
                return 'I';
            default:
                return '*';
        }
    }
}
