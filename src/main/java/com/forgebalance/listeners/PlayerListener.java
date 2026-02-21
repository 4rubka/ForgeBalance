package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public PlayerListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.isGracePeriodActive()) {
            player.sendMessage(ChatColor.YELLOW + "Grace period is active for "
                    + plugin.getGraceRemainingSeconds() + "s.");
        }

        if (plugin.getCombatManager().consumeCombatLogPenalty(player.getUniqueId())) {
            String punishmentMessage = ChatColor.RED + plugin.getConfigManager().getString(
                    "combat.combat-log-punish-message",
                    "You were punished for combat logging."
            );
            player.sendMessage(punishmentMessage);
            if (plugin.getConfigManager().getBoolean("combat.combat-log-kill-instantly", false)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline() && !player.isDead()) {
                        player.setHealth(0.0D);
                    }
                });
            } else {
                player.getInventory().clear();
                player.updateInventory();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getCooldownManager().clearPlayer(player.getUniqueId());
        plugin.getCombatManager().clearTag(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (plugin.getConfigManager().getBoolean("cooldowns.reset-on-death", true)) {
            plugin.getCooldownManager().clearPlayer(event.getEntity().getUniqueId());
        }
        plugin.getCombatManager().clearTag(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isGracePeriodActive()) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("smp-start.disable-block-break", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Block breaking is disabled during grace period.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!plugin.isGracePeriodActive()) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        if (plugin.getConfigManager().hasBypass(attacker)) {
            return;
        }
        if (event.getEntity() instanceof Player
                && plugin.getConfigManager().getBoolean("smp-start.disable-pvp", true)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "PvP is disabled during grace period.");
            return;
        }
        if (!(event.getEntity() instanceof Player)
                && plugin.getConfigManager().getBoolean("smp-start.disable-entity-damage", true)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "Entity damage is disabled during grace period.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!plugin.isGracePeriodActive()) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("smp-start.disable-entity-damage", true)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                || event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
        }
    }
}
