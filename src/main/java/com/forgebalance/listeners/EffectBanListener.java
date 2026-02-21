package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class EffectBanListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public EffectBanListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEffectGain(EntityPotionEffectEvent event) {
        if (!plugin.getConfigManager().isEnabled("effects.enabled")) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null || newEffect.getType() == null || newEffect.getType().getKey() == null) {
            return;
        }

        String key = newEffect.getType().getKey().getKey().toUpperCase();
        Set<String> bypass = normalize(plugin.getConfigManager().getStringList("effects.bypass-effects"));
        if (bypass.contains(key)) {
            return;
        }

        if (isExplicitlyBlocked(newEffect.getType())) {
            event.setCancelled(true);
            if (plugin.getConfigManager().getBoolean("effects.show-block-message", true)) {
                player.sendMessage(ChatColor.RED + "Effect is blocked: " + key);
            }
            return;
        }

        Set<String> banned = normalize(plugin.getConfigManager().getStringList("effects.banned-effects"));
        if (banned.contains(key) || isLegacyAliasBanned(key, banned)) {
            event.setCancelled(true);
            if (plugin.getConfigManager().getBoolean("effects.show-block-message", true)) {
                player.sendMessage(ChatColor.RED + "Effect is banned: " + key);
            }
        }
    }

    private boolean isExplicitlyBlocked(PotionEffectType type) {
        if (type == null || type.getKey() == null) {
            return false;
        }
        String key = type.getKey().getKey();
        if ("strength".equals(key)) {
            return plugin.getConfigManager().getBoolean("effects.block-strength", true);
        }
        if ("speed".equals(key)) {
            return plugin.getConfigManager().getBoolean("effects.block-speed", false);
        }
        if ("resistance".equals(key)) {
            return plugin.getConfigManager().getBoolean("effects.block-resistance", true);
        }
        if ("fire_resistance".equals(key)) {
            return plugin.getConfigManager().getBoolean("effects.block-fire-resistance", false);
        }
        if ("regeneration".equals(key)) {
            return plugin.getConfigManager().getBoolean("effects.block-regeneration", false);
        }
        if ("jump_boost".equals(key) || type == PotionEffectType.JUMP_BOOST) {
            return plugin.getConfigManager().getBoolean("effects.block-jump-boost", false);
        }
        return false;
    }

    private Set<String> normalize(Iterable<String> values) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                normalized.add(value.toUpperCase());
            }
        }
        return normalized;
    }

    private boolean isLegacyAliasBanned(String effectKey, Set<String> banned) {
        if ("RESISTANCE".equals(effectKey) && banned.contains("DAMAGE_RESISTANCE")) {
            return true;
        }
        return false;
    }
}
