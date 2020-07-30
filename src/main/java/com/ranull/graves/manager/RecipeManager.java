package com.ranull.graves.manager;

import com.ranull.graves.Graves;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class RecipeManager {
    private Graves plugin;
    private GraveManager graveManager;

    public RecipeManager(Graves plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    public void loadRecipes() {
        if (plugin.getConfig().getBoolean("settings.token") &&
                plugin.getConfig().getBoolean("settings.tokenRecipeEnabled")) {
            graveTokenRecipe();
        }
    }

    public void unloadRecipes() {
        Material graveTokenMaterial = Material.matchMaterial(Objects.requireNonNull(plugin.getConfig().getString("settings.tokenItem")));
        if (graveTokenMaterial != null) {
            Iterator<Recipe> recipes = plugin.getServer().recipeIterator();
            while (recipes.hasNext()) {
                Recipe recipe = recipes.next();
                if (recipe != null) {
                    ItemStack itemStack = recipe.getResult();
                    if (itemStack.hasItemMeta()) {
                        if (hasRecipeData(itemStack)) {
                            recipes.remove();
                        }
                    }
                }
            }
        }
    }

    public void graveTokenRecipe() {
        Material graveTokenMaterial = Material.matchMaterial(Objects.requireNonNull(plugin.getConfig().getString("settings.tokenItem")));

        if (graveTokenMaterial != null) {
            ItemStack item = graveManager.getGraveToken();

            NamespacedKey key = new NamespacedKey(plugin, "grave_token");
            ShapedRecipe recipe = new ShapedRecipe(key, item);

            recipe.shape("ABC", "DEF", "GHI");

            List<String> lines = plugin.getConfig().getStringList("settings.tokenRecipe");
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

    public boolean hasRecipeData(ItemStack item) {
        return Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "gravesRecipe"), PersistentDataType.INTEGER);
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
