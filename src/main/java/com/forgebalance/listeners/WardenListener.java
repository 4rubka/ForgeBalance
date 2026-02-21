package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.concurrent.ThreadLocalRandom;

public class WardenListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public WardenListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWardenDeath(EntityDeathEvent event) {
        if (!plugin.getConfigManager().isEnabled("warden.enabled")) {
            return;
        }
        if (event.getEntityType() != EntityType.WARDEN) {
            return;
        }

        double dropChance = plugin.getConfigManager().getDouble("warden.heart-drop-chance", 1.0D);
        if (dropChance <= 0D) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > Math.min(1D, dropChance)) {
            return;
        }

        event.getDrops().add(plugin.getRecipeManager().createWardenHeartItem());
    }
}
