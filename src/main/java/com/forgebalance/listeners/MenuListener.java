package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import com.forgebalance.gui.AdminMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public MenuListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!AdminMenu.isMenuTitle(title)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }

        String action = meta.getPersistentDataContainer().get(
                AdminMenu.getActionKey(plugin),
                PersistentDataType.STRING
        );
        if (action != null && !action.isEmpty()) {
            handleAction(player, action);
            return;
        }

        String path = meta.getPersistentDataContainer().get(
                AdminMenu.getToggleKey(plugin),
                PersistentDataType.STRING
        );
        if (path == null || path.isEmpty()) {
            return;
        }

        boolean oldValue = plugin.getConfigManager().getBoolean(path, true);
        boolean newValue = !oldValue;
        plugin.getConfig().set(path, newValue);
        if (plugin.getConfigManager().getBoolean("settings.save-on-toggle", true)) {
            plugin.saveConfig();
        }

        // Re-apply recipe registration when toggled on or reloaded.
        if ("recipes.enabled".equals(path) || path.startsWith("recipes.")) {
            if (plugin.getConfigManager().getBoolean("recipes.enabled", true)) {
                plugin.getRecipeManager().registerCustomRecipes();
            }
        }

        if (plugin.getConfigManager().getBoolean("menu.send-toggle-message", true)) {
            player.sendMessage(ChatColor.AQUA + "[ForgeBalance] "
                    + ChatColor.GRAY + path + ": "
                    + (newValue ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        }

        Integer page = meta.getPersistentDataContainer().get(
                AdminMenu.getPageKey(plugin),
                PersistentDataType.INTEGER
        );
        int pageIndex = page == null ? AdminMenu.getPageIndexFromTitle(title) : page;
        if (plugin.getConfigManager().getBoolean("menu.keep-open-after-toggle", true)) {
            AdminMenu.populate(event.getInventory(), plugin, pageIndex);
        } else {
            player.closeInventory();
        }
    }

    private void handleAction(Player player, String action) {
        if ("close".equalsIgnoreCase(action)) {
            player.closeInventory();
            return;
        }
        if (action.startsWith("page:")) {
            String value = action.substring("page:".length());
            try {
                int page = Integer.parseInt(value);
                AdminMenu.openPage(player, plugin, page);
            } catch (NumberFormatException ignored) {
                player.sendMessage(ChatColor.RED + "Invalid menu page: " + value);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (AdminMenu.isMenuTitle(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}
