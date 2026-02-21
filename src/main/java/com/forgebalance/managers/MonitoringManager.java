package com.forgebalance.managers;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MonitoringManager {

    private final ForgeBalancePlugin plugin;
    private final Map<UUID, HitStats> hitStatsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, MiningWindow> miningWindowsByPlayer = new ConcurrentHashMap<>();
    private final Set<UUID> xaeroDetectedPlayers = ConcurrentHashMap.newKeySet();

    public MonitoringManager(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    public void clearAll() {
        hitStatsByPlayer.clear();
        miningWindowsByPlayer.clear();
        xaeroDetectedPlayers.clear();
    }

    public void clearPlayer(UUID uuid) {
        hitStatsByPlayer.remove(uuid);
        miningWindowsByPlayer.remove(uuid);
        xaeroDetectedPlayers.remove(uuid);
    }

    public void recordPvPHit(Player attacker, Player victim, double finalDamage) {
        HitStats attackerStats = hitStatsByPlayer.computeIfAbsent(attacker.getUniqueId(), ignored -> new HitStats());
        HitStats victimStats = hitStatsByPlayer.computeIfAbsent(victim.getUniqueId(), ignored -> new HitStats());

        attackerStats.hitsGiven++;
        attackerStats.damageGiven += Math.max(0D, finalDamage);
        attackerStats.lastEventAt = System.currentTimeMillis();

        victimStats.hitsTaken++;
        victimStats.damageTaken += Math.max(0D, finalDamage);
        victimStats.lastEventAt = attackerStats.lastEventAt;
    }

    public HitStatsSnapshot getHitStatsSnapshot(UUID uuid) {
        HitStats stats = hitStatsByPlayer.get(uuid);
        if (stats == null) {
            return new HitStatsSnapshot(0, 0, 0D, 0D, 0L);
        }
        return new HitStatsSnapshot(
                stats.hitsGiven,
                stats.hitsTaken,
                stats.damageGiven,
                stats.damageTaken,
                stats.lastEventAt
        );
    }

    public XrayAlert recordMining(Player player, Material minedType, int yLevel) {
        if (!plugin.getConfigManager().getBoolean("utility.anti-xray.enabled", false)) {
            return null;
        }

        MiningWindow window = miningWindowsByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new MiningWindow());
        long now = System.currentTimeMillis();
        boolean ore = isMonitoredOre(minedType);

        window.samples.addLast(new MiningSample(now, ore));
        window.totalBreaks++;
        if (ore) {
            window.oreBreaks++;
        }

        int windowSeconds = Math.max(10, plugin.getConfigManager().getInt("utility.anti-xray.window-seconds", 300));
        pruneOldSamples(window, now, windowSeconds * 1000L);

        int minOreBreaks = Math.max(1, plugin.getConfigManager().getInt("utility.anti-xray.min-ore-breaks", 10));
        int minTotalBreaks = Math.max(10, plugin.getConfigManager().getInt("utility.anti-xray.min-total-breaks", 120));
        double ratioThreshold = Math.max(0.01D, plugin.getConfigManager().getDouble("utility.anti-xray.ratio-threshold", 0.08D));
        int cooldownSeconds = Math.max(1, plugin.getConfigManager().getInt("utility.anti-xray.alert-cooldown-seconds", 120));

        if (window.oreBreaks < minOreBreaks || window.totalBreaks < minTotalBreaks) {
            return null;
        }

        double ratio = window.totalBreaks == 0 ? 0D : (window.oreBreaks / (double) window.totalBreaks);
        if (ratio < ratioThreshold) {
            return null;
        }

        if ((now - window.lastAlertAt) < (cooldownSeconds * 1000L)) {
            return null;
        }

        window.lastAlertAt = now;
        window.alertCount++;
        return new XrayAlert(
                player.getUniqueId(),
                player.getName(),
                minedType.name(),
                yLevel,
                window.oreBreaks,
                window.totalBreaks,
                ratio,
                window.alertCount
        );
    }

    public boolean markXaeroDetected(UUID uuid) {
        return xaeroDetectedPlayers.add(uuid);
    }

    public boolean wasXaeroDetected(UUID uuid) {
        return xaeroDetectedPlayers.contains(uuid);
    }

    private void pruneOldSamples(MiningWindow window, long now, long maxAgeMs) {
        while (!window.samples.isEmpty()) {
            MiningSample oldest = window.samples.peekFirst();
            if (oldest == null || (now - oldest.timestamp) <= maxAgeMs) {
                break;
            }
            window.samples.removeFirst();
            window.totalBreaks = Math.max(0, window.totalBreaks - 1);
            if (oldest.ore) {
                window.oreBreaks = Math.max(0, window.oreBreaks - 1);
            }
        }
    }

    private boolean isMonitoredOre(Material material) {
        List<String> configured = plugin.getConfigManager().getStringList("utility.anti-xray.monitored-ores");
        if (configured.isEmpty()) {
            return defaultOreSet().contains(material);
        }

        for (String oreName : configured) {
            Material ore = Material.matchMaterial(oreName);
            if (ore != null && ore == material) {
                return true;
            }
        }
        return false;
    }

    private Set<Material> defaultOreSet() {
        Set<Material> defaultSet = new HashSet<>();
        addIfPresent(defaultSet, "DIAMOND_ORE");
        addIfPresent(defaultSet, "DEEPSLATE_DIAMOND_ORE");
        addIfPresent(defaultSet, "EMERALD_ORE");
        addIfPresent(defaultSet, "DEEPSLATE_EMERALD_ORE");
        addIfPresent(defaultSet, "ANCIENT_DEBRIS");
        addIfPresent(defaultSet, "GOLD_ORE");
        addIfPresent(defaultSet, "DEEPSLATE_GOLD_ORE");
        addIfPresent(defaultSet, "REDSTONE_ORE");
        addIfPresent(defaultSet, "DEEPSLATE_REDSTONE_ORE");
        return defaultSet;
    }

    private void addIfPresent(Set<Material> set, String materialName) {
        Material material = Material.matchMaterial(materialName);
        if (material != null) {
            set.add(material);
        }
    }

    private static final class HitStats {
        private long hitsGiven;
        private long hitsTaken;
        private double damageGiven;
        private double damageTaken;
        private long lastEventAt;
    }

    private static final class MiningWindow {
        private final Deque<MiningSample> samples = new ArrayDeque<>();
        private int oreBreaks;
        private int totalBreaks;
        private int alertCount;
        private long lastAlertAt;
    }

    private static final class MiningSample {
        private final long timestamp;
        private final boolean ore;

        private MiningSample(long timestamp, boolean ore) {
            this.timestamp = timestamp;
            this.ore = ore;
        }
    }

    public static final class HitStatsSnapshot {
        private final long hitsGiven;
        private final long hitsTaken;
        private final double damageGiven;
        private final double damageTaken;
        private final long lastEventAt;

        public HitStatsSnapshot(long hitsGiven, long hitsTaken, double damageGiven, double damageTaken, long lastEventAt) {
            this.hitsGiven = hitsGiven;
            this.hitsTaken = hitsTaken;
            this.damageGiven = damageGiven;
            this.damageTaken = damageTaken;
            this.lastEventAt = lastEventAt;
        }

        public long getHitsGiven() {
            return hitsGiven;
        }

        public long getHitsTaken() {
            return hitsTaken;
        }

        public double getDamageGiven() {
            return damageGiven;
        }

        public double getDamageTaken() {
            return damageTaken;
        }

        public long getLastEventAt() {
            return lastEventAt;
        }
    }

    public static final class XrayAlert {
        private final UUID playerUuid;
        private final String playerName;
        private final String minedMaterial;
        private final int yLevel;
        private final int oreBreaks;
        private final int totalBreaks;
        private final double ratio;
        private final int alertCount;

        public XrayAlert(
                UUID playerUuid,
                String playerName,
                String minedMaterial,
                int yLevel,
                int oreBreaks,
                int totalBreaks,
                double ratio,
                int alertCount
        ) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.minedMaterial = minedMaterial;
            this.yLevel = yLevel;
            this.oreBreaks = oreBreaks;
            this.totalBreaks = totalBreaks;
            this.ratio = ratio;
            this.alertCount = alertCount;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getMinedMaterial() {
            return minedMaterial;
        }

        public int getYLevel() {
            return yLevel;
        }

        public int getOreBreaks() {
            return oreBreaks;
        }

        public int getTotalBreaks() {
            return totalBreaks;
        }

        public double getRatio() {
            return ratio;
        }

        public int getAlertCount() {
            return alertCount;
        }
    }
}
