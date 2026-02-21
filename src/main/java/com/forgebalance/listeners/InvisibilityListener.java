package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.potion.PotionEffectType;

public class InvisibilityListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public InvisibilityListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeathMessage(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isEnabled("invisibility.enabled")
                || !plugin.getConfigManager().getBoolean("invisibility.hide-killer-name", true)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (!killer.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return;
        }
        event.setDeathMessage(ChatColor.GRAY + event.getEntity().getName() + " was slain by Unknown");
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfigManager().isEnabled("invisibility.enabled")
                || !plugin.getConfigManager().getBoolean("invisibility.anonymous-invis", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return;
        }
        event.setFormat("Unknown: %2$s");
    }
}
