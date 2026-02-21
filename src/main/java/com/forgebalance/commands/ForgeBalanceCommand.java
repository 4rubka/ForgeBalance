package com.forgebalance.commands;

import com.forgebalance.ForgeBalancePlugin;
import com.forgebalance.gui.AdminMenu;
import com.forgebalance.managers.MonitoringManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class ForgeBalanceCommand implements CommandExecutor {

    private final ForgeBalancePlugin plugin;

    public ForgeBalanceCommand(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("forgebalance.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                if (plugin.getConfigManager().getBoolean("menu.enabled", true)) {
                    AdminMenu.open((Player) sender, plugin);
                    return true;
                }
            }
            sendStatus(sender);
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <status|reload|menu|stats [player]>");
            return true;
        }

        String sub = args[0].toLowerCase();
        if ("reload".equals(sub)) {
            plugin.getConfigManager().reload();
            plugin.getCooldownManager().clearAll();
            plugin.getCombatManager().clearAll();
            plugin.getMonitoringManager().clearAll();
            if (plugin.getConfigManager().isEnabled("recipes.enabled")) {
                plugin.getRecipeManager().registerCustomRecipes();
            }
            sender.sendMessage(ChatColor.GREEN + "ForgeBalance config reloaded.");
            return true;
        }
        if ("menu".equals(sub)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can open menu.");
                return true;
            }
            if (!plugin.getConfigManager().getBoolean("menu.enabled", true)) {
                sender.sendMessage(ChatColor.RED + "Menu is disabled in config (menu.enabled=false).");
                return true;
            }
            AdminMenu.open((Player) sender, plugin);
            return true;
        }

        if ("status".equals(sub)) {
            sendStatus(sender);
            return true;
        }
        if ("stats".equals(sub)) {
            sendStats(sender, args);
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <status|reload|menu|stats [player]>");
        return true;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "ForgeBalance status:");
        sender.sendMessage(ChatColor.GRAY + "- Grace active: "
                + (plugin.isGracePeriodActive() ? "yes (" + plugin.getGraceRemainingSeconds() + "s)" : "no"));
        sender.sendMessage(ChatColor.GRAY + "- Damage module: "
                + (plugin.getConfigManager().isEnabled("damage.enabled") ? "on" : "off"));
        sender.sendMessage(ChatColor.GRAY + "- Cooldowns module: "
                + (plugin.getConfigManager().isEnabled("cooldowns.enabled") ? "on" : "off"));
        sender.sendMessage(ChatColor.GRAY + "- Combat module: "
                + (plugin.getConfigManager().isEnabled("combat.enabled") ? "on" : "off"));
    }

    private void sendStats(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found online: " + args[1]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage from console: /forgebalance stats <player>");
            return;
        }

        MonitoringManager.HitStatsSnapshot stats = plugin.getMonitoringManager().getHitStatsSnapshot(target.getUniqueId());
        sender.sendMessage(ChatColor.AQUA + "Combat stats for " + target.getName() + ":");
        sender.sendMessage(ChatColor.GRAY + "- Hits given: " + ChatColor.WHITE + stats.getHitsGiven());
        sender.sendMessage(ChatColor.GRAY + "- Hits taken: " + ChatColor.WHITE + stats.getHitsTaken());
        sender.sendMessage(ChatColor.GRAY + "- Damage given: " + ChatColor.WHITE + String.format(Locale.US, "%.1f", stats.getDamageGiven()));
        sender.sendMessage(ChatColor.GRAY + "- Damage taken: " + ChatColor.WHITE + String.format(Locale.US, "%.1f", stats.getDamageTaken()));
    }
}
