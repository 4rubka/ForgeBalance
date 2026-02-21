package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import com.forgebalance.managers.MonitoringManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MonitoringListener implements Listener {

    private final ForgeBalancePlugin plugin;
    private long lastHealthObjectiveCleanupMs;

    public MonitoringListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatMonitor(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        Entity target = event.getEntity();
        if (target instanceof Player && plugin.getConfigManager().getBoolean("combat.track-hit-statistics", false)) {
            plugin.getMonitoringManager().recordPvPHit(attacker, (Player) target, event.getFinalDamage());
        }

        if (target instanceof LivingEntity
                && plugin.getConfigManager().getBoolean("utility.health-indicators.builtin.enabled", false)) {
            sendBuiltInHealthIndicator(attacker, (LivingEntity) target, event.getFinalDamage());
        }

        cleanupHealthObjectives(false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.anti-xray.enabled", false)) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        Block block = event.getBlock();
        MonitoringManager.XrayAlert alert = plugin.getMonitoringManager().recordMining(
                player,
                block.getType(),
                block.getY()
        );
        if (alert == null) {
            return;
        }

        if (plugin.getConfigManager().getBoolean("utility.anti-xray.alert-on-suspicious-mining", true)) {
            notifyStaff(ChatColor.RED + "[ForgeBalance] Suspicious mining: "
                    + ChatColor.YELLOW + alert.getPlayerName()
                    + ChatColor.GRAY + " ratio="
                    + ChatColor.WHITE + String.format("%.2f", alert.getRatio() * 100D) + "%"
                    + ChatColor.GRAY + " ores="
                    + ChatColor.WHITE + alert.getOreBreaks() + "/" + alert.getTotalBreaks()
                    + ChatColor.GRAY + " y="
                    + ChatColor.WHITE + alert.getYLevel()
                    + ChatColor.GRAY + " material="
                    + ChatColor.WHITE + alert.getMinedMaterial());
        }

        if (plugin.getConfigManager().getBoolean("utility.anti-xray.punish-enabled", false)) {
            String command = plugin.getConfigManager().getString("utility.anti-xray.punish-command", "");
            if (!command.isEmpty()) {
                String parsed = command
                        .replace("%player%", alert.getPlayerName())
                        .replace("%ratio%", String.format("%.2f", alert.getRatio() * 100D))
                        .replace("%ores%", String.valueOf(alert.getOreBreaks()))
                        .replace("%total%", String.valueOf(alert.getTotalBreaks()));
                ConsoleCommandSender console = Bukkit.getConsoleSender();
                Bukkit.dispatchCommand(console, parsed);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().getBoolean("utility.anti-xaero-minimap.enabled", false)
                && plugin.getConfigManager().getBoolean("utility.anti-xaero-minimap.check-on-join", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkPlayerChannels(player), 40L);
        }
        cleanupHealthObjectives(true);
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.anti-xaero-minimap.enabled", false)) {
            return;
        }
        String channel = event.getChannel();
        if (!isXaeroChannel(channel)) {
            return;
        }
        handleXaeroDetection(event.getPlayer(), channel);
    }

    private void checkPlayerChannels(Player player) {
        if (!player.isOnline()) {
            return;
        }
        Set<String> channels = player.getListeningPluginChannels();
        for (String channel : channels) {
            if (isXaeroChannel(channel)) {
                handleXaeroDetection(player, channel);
                return;
            }
        }
    }

    private void handleXaeroDetection(Player player, String channel) {
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        boolean firstDetection = plugin.getMonitoringManager().markXaeroDetected(player.getUniqueId());
        if (!firstDetection) {
            return;
        }

        String mode = plugin.getConfigManager()
                .getString("utility.anti-xaero-minimap.punish-mode", "WARN")
                .toUpperCase(Locale.ROOT);
        String warnMessage = color(plugin.getConfigManager().getString(
                "utility.anti-xaero-minimap.warn-message",
                "&cXaero Minimap/WorldMap is not allowed on this server."
        ));
        String kickMessage = color(plugin.getConfigManager().getString(
                "utility.anti-xaero-minimap.kick-message",
                "&cDisallowed minimap mod detected."
        ));

        if (plugin.getConfigManager().getBoolean("utility.anti-xaero-minimap.notify-staff", true)) {
            notifyStaff(ChatColor.RED + "[ForgeBalance] Xaero channel detected for "
                    + ChatColor.YELLOW + player.getName()
                    + ChatColor.GRAY + " (" + channel + ")");
        }

        if (plugin.getConfigManager().getBoolean("utility.anti-xaero-minimap.block-channel-registration", true)
                && !"KICK".equals(mode)) {
            player.sendMessage(ChatColor.YELLOW + "Client channel usage is blocked by server policy.");
        }

        if ("KICK".equals(mode)) {
            player.kickPlayer(kickMessage);
            return;
        }

        player.sendMessage(warnMessage);
    }

    private void sendBuiltInHealthIndicator(Player attacker, LivingEntity target, double finalDamage) {
        boolean showHearts = plugin.getConfigManager().getBoolean("utility.health-indicators.builtin.show-hearts", true);
        double remaining = Math.max(0D, target.getHealth() - Math.max(0D, finalDamage));
        double max = Math.max(1D, target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                ? target.getMaxHealth()
                : target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());

        String value = showHearts
                ? String.format(Locale.US, "%.1f❤", remaining / 2D)
                : String.format(Locale.US, "%.1f/%.1f", remaining, max);

        String format = plugin.getConfigManager().getString(
                "utility.health-indicators.builtin.actionbar-format",
                "&c%target% &7-> &f%health%"
        );
        String parsed = color(format
                .replace("%target%", target.getName())
                .replace("%health%", value));

        sendActionBar(attacker, parsed);
    }

    private void sendActionBar(Player player, String message) {
        try {
            Method sendActionBar = player.getClass().getMethod("sendActionBar", String.class);
            sendActionBar.invoke(player, message);
        } catch (Throwable ignored) {
            player.sendMessage(message);
        }
    }

    private void cleanupHealthObjectives(boolean force) {
        boolean disableThirdParty = plugin.getConfigManager().getBoolean("utility.health-indicators.disable-third-party", false);
        boolean blockExternal = plugin.getConfigManager().getBoolean("utility.health-indicators.block-external", false);
        if (!disableThirdParty && !blockExternal) {
            return;
        }

        int intervalSeconds = Math.max(5, plugin.getConfigManager().getInt("utility.health-indicators.cleanup-interval-seconds", 30));
        long now = System.currentTimeMillis();
        if (!force && (now - lastHealthObjectiveCleanupMs) < intervalSeconds * 1000L) {
            return;
        }
        lastHealthObjectiveCleanupMs = now;

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            return;
        }

        cleanupScoreboard(scoreboardManager.getMainScoreboard());
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player online : onlinePlayers) {
            cleanupScoreboard(online.getScoreboard());
        }
    }

    private void cleanupScoreboard(Scoreboard scoreboard) {
        if (scoreboard == null) {
            return;
        }
        List<Objective> objectives = new ArrayList<>(scoreboard.getObjectives());
        for (Objective objective : objectives) {
            String name = objective.getName() == null ? "" : objective.getName().toLowerCase(Locale.ROOT);
            String criteria = String.valueOf(objective.getCriteria()).toLowerCase(Locale.ROOT);
            if (name.contains("health") || criteria.contains("health")) {
                objective.unregister();
            }
        }
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        return null;
    }

    private boolean isXaeroChannel(String channel) {
        if (channel == null) {
            return false;
        }
        String normalized = channel.toLowerCase(Locale.ROOT);
        return normalized.contains("xaero");
    }

    private void notifyStaff(String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("forgebalance.admin")) {
                online.sendMessage(message);
            }
        }
        plugin.getLogger().warning(ChatColor.stripColor(message));
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
