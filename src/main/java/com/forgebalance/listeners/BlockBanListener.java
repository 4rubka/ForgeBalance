package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBanListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public BlockBanListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        Material type = event.getBlockPlaced().getType();
        if (type == Material.RESPAWN_ANCHOR && plugin.getConfigManager().getBoolean("items.ban-anchors", true)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Respawn anchors are banned.");
            return;
        }

        if (type == Material.TNT_MINECART && plugin.getConfigManager().getBoolean("items.ban-tnt-minecarts", true)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "TNT minecarts are banned.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.TNT_MINECART
                && plugin.getConfigManager().getBoolean("items.ban-tnt-minecarts", true)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "TNT minecarts are banned.");
            return;
        }

        if (item != null && item.getType() == Material.RESPAWN_ANCHOR
                && plugin.getConfigManager().getBoolean("items.ban-anchors", true)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Respawn anchors are banned.");
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Material clickedType = event.getClickedBlock().getType();
        if (clickedType == Material.RESPAWN_ANCHOR && plugin.getConfigManager().getBoolean("items.ban-anchors", true)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Respawn anchors are banned.");
            return;
        }

        if (!plugin.getConfigManager().getBoolean("items.ban-bed-bombing", true)) {
            return;
        }
        if (!clickedType.name().endsWith("_BED")) {
            return;
        }

        World.Environment environment = player.getWorld().getEnvironment();
        if (environment != World.Environment.NORMAL) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Bed bombing is banned.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        if (!plugin.getConfigManager().getBoolean("items.ban-tnt-minecarts", true)) {
            return;
        }
        if (event.getVehicle().getType() == EntityType.TNT_MINECART) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVillagerDamage(EntityDamageByEntityEvent event) {
        boolean legacyBan = plugin.getConfigManager().getBoolean("items.ban-killing-villagers", false);
        boolean utilityBan = plugin.getConfigManager().getBoolean("utility.villager-protection.prevent-player-kill", false);
        if (!legacyBan && !utilityBan) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        if (event.getEntityType() != EntityType.VILLAGER) {
            return;
        }
        Player player = (Player) event.getDamager();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Killing villagers is disabled.");
    }
}
