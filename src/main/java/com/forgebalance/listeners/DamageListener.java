package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class DamageListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public DamageListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfigManager().isEnabled("damage.enabled")) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        Entity targetEntity = event.getEntity();
        if (!(targetEntity instanceof LivingEntity)) {
            return;
        }

        if (targetEntity instanceof Player
                && !plugin.getConfigManager().getBoolean("pvp.enabled", true)) {
            event.setCancelled(true);
            return;
        }

        if (targetEntity instanceof Player
                && plugin.isGracePeriodActive()
                && plugin.getConfigManager().getBoolean("smp-start.disable-pvp", true)) {
            event.setCancelled(true);
            return;
        }

        double damage = event.getDamage();
        double globalMultiplier = plugin.getConfigManager().getBoolean("damage.use-global-multiplier", true)
                ? plugin.getConfigManager().getDouble("damage.global-multiplier", 1.0D)
                : 1.0D;
        damage *= globalMultiplier;

        if (targetEntity instanceof Player) {
            if (plugin.getConfigManager().getBoolean("damage.use-pvp-multiplier", true)) {
                damage *= plugin.getConfigManager().getDouble("damage.pvp-multiplier", 1.0D);
            }
        } else {
            if (plugin.getConfigManager().getBoolean("damage.use-pve-multiplier", true)) {
                damage *= plugin.getConfigManager().getDouble("damage.pve-multiplier", 1.0D);
            }
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon != null && weapon.getType() != Material.AIR
                && plugin.getConfigManager().getBoolean("damage.enable-custom-damage-table", true)) {
            ConfigurationSection customDamage = plugin.getConfig().getConfigurationSection("damage.custom-damage");
            if (customDamage != null) {
                String materialName = weapon.getType().name();
                if (customDamage.contains(materialName)) {
                    double base = customDamage.getDouble(materialName, event.getDamage());
                    damage = base * globalMultiplier;
                    if (targetEntity instanceof Player && plugin.getConfigManager().getBoolean("damage.use-pvp-multiplier", true)) {
                        damage *= plugin.getConfigManager().getDouble("damage.pvp-multiplier", 1.0D);
                    } else if (!(targetEntity instanceof Player) && plugin.getConfigManager().getBoolean("damage.use-pve-multiplier", true)) {
                        damage *= plugin.getConfigManager().getDouble("damage.pve-multiplier", 1.0D);
                    }
                }
            }
        }

        event.setDamage(Math.max(0D, damage));
    }

    private Player resolveAttacker(Entity damager) {
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
}
