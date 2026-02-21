package com.forgebalance.managers;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class RecipeManager {

    private final ForgeBalancePlugin plugin;
    private final NamespacedKey wardenHeartItemKey;
    private final NamespacedKey wardenRecipeKey;

    public RecipeManager(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
        this.wardenHeartItemKey = new NamespacedKey(plugin, "warden_heart_item");
        this.wardenRecipeKey = new NamespacedKey(plugin, "warden_heart_recipe");
    }

    public void registerCustomRecipes() {
        if (!plugin.getConfigManager().getBoolean("recipes.warden-heart-recipe.enabled", true)) {
            return;
        }

        ItemStack recipeResult = createWardenRecipeResult();
        ShapedRecipe recipe = new ShapedRecipe(wardenRecipeKey, recipeResult);

        List<String> configuredShape = plugin.getConfig().getStringList("recipes.warden-heart-recipe.shape");
        if (configuredShape.size() != 3) {
            configuredShape = Arrays.asList("GGG", "GWH", "GGG");
        }
        recipe.shape(configuredShape.get(0), configuredShape.get(1), configuredShape.get(2));

        ConfigurationSection ingredients = plugin.getConfig().getConfigurationSection("recipes.warden-heart-recipe.ingredients");
        if (ingredients != null) {
            for (String key : ingredients.getKeys(false)) {
                if (key == null || key.length() != 1) {
                    continue;
                }
                String ingredientName = ingredients.getString(key, "");
                char symbol = key.charAt(0);
                if ("WARDEN_HEART".equalsIgnoreCase(ingredientName)) {
                    recipe.setIngredient(symbol, new RecipeChoice.ExactChoice(createWardenHeartItem()));
                    continue;
                }
                Material material = Material.matchMaterial(ingredientName);
                if (material != null) {
                    recipe.setIngredient(symbol, material);
                }
            }
        }

        Bukkit.removeRecipe(wardenRecipeKey);
        Bukkit.addRecipe(recipe);
    }

    public ItemStack createWardenHeartItem() {
        String customMaterialName = plugin.getConfigManager().getString("warden.custom-heart-item", "HEART_OF_THE_SEA");
        Material material = "WARDEN_HEART".equalsIgnoreCase(customMaterialName)
                ? Material.HEART_OF_THE_SEA
                : Material.matchMaterial(customMaterialName);
        if (material == null) {
            material = Material.HEART_OF_THE_SEA;
        }

        ItemStack heart = new ItemStack(material);
        ItemMeta meta = heart.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_AQUA + "Warden Heart");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "A core infused with deep dark energy.",
                    ChatColor.GRAY + "Used in custom ritual recipes."
            ));
            meta.getPersistentDataContainer().set(wardenHeartItemKey, PersistentDataType.BYTE, (byte) 1);
            heart.setItemMeta(meta);
        }
        return heart;
    }

    public boolean isWardenHeart(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte value = meta.getPersistentDataContainer().get(wardenHeartItemKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private ItemStack createWardenRecipeResult() {
        String resultMaterialName = plugin.getConfigManager().getString(
                "recipes.warden-heart-recipe.result-material",
                "NETHER_STAR"
        );
        Material resultMaterial = Material.matchMaterial(resultMaterialName);
        if (resultMaterial == null) {
            resultMaterial = Material.NETHER_STAR;
        }

        ItemStack result = new ItemStack(resultMaterial);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Warden Sigil");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "A crafted artifact made from a Warden Heart.",
                    ChatColor.GRAY + "Use it in your custom progression."
            ));
            result.setItemMeta(meta);
        }
        return result;
    }
}
