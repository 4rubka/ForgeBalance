package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CraftListener implements Listener {

    private final ForgeBalancePlugin plugin;
    private final Map<UUID, Map<Material, Integer>> craftedByPlayer = new ConcurrentHashMap<>();

    public CraftListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!plugin.getConfigManager().isEnabled("one-craft.enabled")) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        ItemStack result = event.getRecipe() == null ? null : event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("one-craft.limited-crafts");
        if (section == null) {
            return;
        }
        String materialName = result.getType().name();
        if (!section.contains(materialName)) {
            return;
        }

        int maxAllowed = section.getInt(materialName, 1);
        int craftAmount = Math.max(1, result.getAmount());

        Map<Material, Integer> playerCounts = craftedByPlayer.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new ConcurrentHashMap<>()
        );
        int craftedSoFar = playerCounts.getOrDefault(result.getType(), 0);
        if (craftedSoFar + craftAmount > maxAllowed) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Craft limit reached for " + result.getType().name()
                    + " (" + maxAllowed + ").");
            return;
        }

        playerCounts.put(result.getType(), craftedSoFar + craftAmount);
    }
}
