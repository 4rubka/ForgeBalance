package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;

public class MaceListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public MaceListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMaceDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != Material.MACE) {
            return;
        }
        if (!plugin.getConfigManager().isEnabled("mace.enabled")) {
            return;
        }
        if (plugin.getConfigManager().getBoolean("mace.ban-mace", true)
                && !plugin.getConfigManager().hasBypass(attacker)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "Mace is banned.");
            return;
        }

        if (plugin.getConfigManager().isEnabled("cooldowns.enabled")) {
            if (plugin.getConfigManager().getBoolean("cooldowns.mace-cooldown-enabled", true)) {
                int maceCooldown = plugin.getConfigManager().getInt("cooldowns.mace-cooldown", 100);
                boolean allowed = plugin.getCooldownManager().checkAndApply(
                        attacker,
                        "mace",
                        maceCooldown,
                        "Mace",
                        event
                );
                if (!allowed) {
                    return;
                }
                attacker.setCooldown(Material.MACE, Math.max(0, maceCooldown));
            }
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (plugin.getConfigManager().getBoolean("mace.stun-shield", true) && victim.isBlocking()) {
            int stunDuration = plugin.getConfigManager().getInt("mace.shield-stun-duration", 20);
            victim.setCooldown(Material.SHIELD, Math.max(0, stunDuration));
            victim.sendMessage(ChatColor.RED + "Your shield was stunned by a mace hit.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWindChargeUse(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.WIND_CHARGE) {
            return;
        }
        if (!plugin.getConfigManager().isEnabled("cooldowns.enabled")) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("cooldowns.wind-charge-cooldown-enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        int cooldown = plugin.getConfigManager().getInt("cooldowns.wind-charge-cooldown", 20);
        boolean allowed = plugin.getCooldownManager().checkAndApply(
                player,
                "wind_charge",
                cooldown,
                "Wind Charge",
                event
        );
        if (allowed) {
            player.setCooldown(Material.WIND_CHARGE, Math.max(0, cooldown));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRiptide(PlayerRiptideEvent event) {
        if (!plugin.getConfigManager().isEnabled("cooldowns.enabled")) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("cooldowns.riptide-cooldown-enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        int cooldown = plugin.getConfigManager().getInt("cooldowns.riptide-cooldown", 100);
        boolean allowed = plugin.getCooldownManager().checkAndApply(
                player,
                "riptide",
                cooldown,
                "Riptide",
                event
        );
        if (allowed) {
            player.setCooldown(Material.TRIDENT, Math.max(0, cooldown));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGapConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getConfigManager().isEnabled("cooldowns.enabled")) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("cooldowns.gap-cooldown-enabled", true)) {
            return;
        }
        Material consumed = event.getItem().getType();
        if (consumed != Material.GOLDEN_APPLE && consumed != Material.ENCHANTED_GOLDEN_APPLE) {
            return;
        }

        Player player = event.getPlayer();
        int cooldown = plugin.getConfigManager().getInt("cooldowns.gap-cooldown", 100);
        boolean allowed = plugin.getCooldownManager().checkAndApply(
                player,
                "gap",
                cooldown,
                "Golden Apple",
                event
        );
        if (allowed) {
            player.setCooldown(consumed, Math.max(0, cooldown));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShieldBlock(EntityDamageByEntityEvent event) {
        if (!plugin.getConfigManager().isEnabled("cooldowns.enabled")) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("cooldowns.shield-cooldown-enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player defender = (Player) event.getEntity();
        if (!defender.isBlocking()) {
            return;
        }
        int shieldCooldown = plugin.getConfigManager().getInt("cooldowns.shield-cooldown", 100);
        if (shieldCooldown > 0) {
            defender.setCooldown(Material.SHIELD, shieldCooldown);
        }
    }
}
