package com.forgebalance.managers;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final ForgeBalancePlugin plugin;
    private final Map<UUID, Long> combatTaggedUntil = new ConcurrentHashMap<>();
    private final Set<UUID> combatLogPenalties = ConcurrentHashMap.newKeySet();

    public CombatManager(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    public void tagPlayer(Player player) {
        int tagSeconds = plugin.getConfigManager().getInt("combat.combat-tag-time", 10);
        if (tagSeconds <= 0) {
            return;
        }
        long until = System.currentTimeMillis() + (tagSeconds * 1000L);
        combatTaggedUntil.put(player.getUniqueId(), until);
    }

    public void tagPlayers(Player a, Player b) {
        tagPlayer(a);
        tagPlayer(b);
    }

    public boolean isInCombat(Player player) {
        Long until = combatTaggedUntil.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            combatTaggedUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public long getRemainingSeconds(Player player) {
        Long until = combatTaggedUntil.get(player.getUniqueId());
        if (until == null) {
            return 0L;
        }
        long remaining = Math.max(0L, until - System.currentTimeMillis());
        return remaining / 1000L;
    }

    public void clearTag(Player player) {
        combatTaggedUntil.remove(player.getUniqueId());
    }

    public void markCombatLogPenalty(UUID uuid) {
        combatLogPenalties.add(uuid);
    }

    public boolean consumeCombatLogPenalty(UUID uuid) {
        return combatLogPenalties.remove(uuid);
    }

    public void clearCombatLogPenalty(UUID uuid) {
        combatLogPenalties.remove(uuid);
    }

    public void clearAll() {
        combatTaggedUntil.clear();
        combatLogPenalties.clear();
    }
}
