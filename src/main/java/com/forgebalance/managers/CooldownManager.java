package com.forgebalance.managers;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final ForgeBalancePlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerCooldowns() {
        // No bootstrap required; cooldowns are initialized on first use.
    }

    public void clearAll() {
        cooldowns.clear();
    }

    public void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
    }

    public boolean isOnCooldown(Player player, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return false;
        }
        Long endAt = playerCooldowns.get(key);
        if (endAt == null) {
            return false;
        }
        if (System.currentTimeMillis() >= endAt) {
            playerCooldowns.remove(key);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(player.getUniqueId());
            }
            return false;
        }
        return true;
    }

    public long getRemainingTicks(Player player, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0L;
        }
        Long endAt = playerCooldowns.get(key);
        if (endAt == null) {
            return 0L;
        }
        long remainingMs = Math.max(0L, endAt - System.currentTimeMillis());
        return Math.max(0L, remainingMs / 50L);
    }

    public void setCooldown(Player player, String key, int ticks) {
        if (ticks <= 0) {
            return;
        }
        long endAt = System.currentTimeMillis() + (ticks * 50L);
        cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(key, endAt);
    }

    public boolean checkAndApply(Player player, String key, int ticks, String prettyName, Cancellable event) {
        if (ticks <= 0) {
            return true;
        }
        if (plugin.isGracePeriodActive() && plugin.getConfigManager().getBoolean("cooldowns.pause-during-grace", false)) {
            return true;
        }
        if (isOnCooldown(player, key)) {
            long remainingTicks = getRemainingTicks(player, key);
            if (event != null) {
                event.setCancelled(true);
            }
            if (plugin.getConfigManager().getBoolean("cooldowns.show-messages", true)) {
                double remainingSeconds = remainingTicks / 20.0D;
                player.sendMessage(ChatColor.RED + prettyName + " cooldown: " + ChatColor.WHITE
                        + String.format("%.1f", remainingSeconds) + "s");
            }
            return false;
        }
        setCooldown(player, key, ticks);
        return true;
    }
}
