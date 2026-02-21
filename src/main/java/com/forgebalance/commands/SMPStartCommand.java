package com.forgebalance.commands;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SMPStartCommand implements CommandExecutor {

    private final ForgeBalancePlugin plugin;

    public SMPStartCommand(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("forgebalance.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (!plugin.getConfigManager().getBoolean("smp-start.enabled", true)) {
            sender.sendMessage(ChatColor.RED + "SMP start system is disabled in config.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <start [seconds]|stop|status>");
            sendStatus(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if ("start".equals(sub)) {
            int graceSeconds = plugin.getConfigManager().getInt("smp-start.grace-period", 300);
            if (args.length >= 2) {
                try {
                    graceSeconds = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
                    return true;
                }
            }
            plugin.startGracePeriod(graceSeconds);
            sender.sendMessage(ChatColor.GREEN + "Started grace period for " + graceSeconds + " seconds.");
            return true;
        }

        if ("stop".equals(sub)) {
            plugin.endGracePeriod(true);
            sender.sendMessage(ChatColor.GREEN + "Grace period stopped.");
            return true;
        }

        if ("status".equals(sub)) {
            sendStatus(sender);
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <start [seconds]|stop|status>");
        return true;
    }

    private void sendStatus(CommandSender sender) {
        if (plugin.isGracePeriodActive()) {
            sender.sendMessage(ChatColor.GREEN + "Grace period active: "
                    + plugin.getGraceRemainingSeconds() + "s remaining.");
            return;
        }
        sender.sendMessage(ChatColor.RED + "Grace period is not active.");
    }
}
