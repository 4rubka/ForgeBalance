package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class CombatListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public CombatListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfigManager().isEnabled("combat.enabled")) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("pvp.enabled", true)) {
            return;
        }
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        if (plugin.getConfigManager().hasBypass(attacker) || plugin.getConfigManager().hasBypass(victim)) {
            return;
        }

        if (plugin.getConfigManager().getBoolean("combat.no-naked-killing", false) && isNaked(victim)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "Naked killing is disabled.");
            return;
        }

        plugin.getCombatManager().tagPlayers(attacker, victim);
    }

    @EventHandler(ignoreCancelled = true)
    public void onElytraToggle(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!plugin.getConfigManager().isEnabled("combat.enabled")
                || !plugin.getConfigManager().getBoolean("combat.anti-elytra", true)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        if (plugin.getCombatManager().isInCombat(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use Elytra while in combat.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRestock(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        if (!plugin.getConfigManager().isEnabled("combat.enabled")) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        if (!plugin.getCombatManager().isInCombat(player)) {
            return;
        }

        InventoryType type = event.getInventory().getType();
        if (type == InventoryType.PLAYER || type == InventoryType.CRAFTING || type == InventoryType.CREATIVE) {
            return;
        }

        if (type == InventoryType.ENDER_CHEST
                && plugin.getConfigManager().getBoolean("combat.prevent-ender-chest-open", false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Ender chest is blocked while in combat.");
            return;
        }

        if (plugin.getConfigManager().getBoolean("combat.anti-restock", true)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Restocking is blocked while in combat.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombatInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isEnabled("combat.enabled")) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        Material type = item.getType();
        boolean inCombat = plugin.getCombatManager().isInCombat(player);

        if (type == Material.ENDER_PEARL) {
            if (plugin.getConfigManager().getBoolean("items.ban-pearls-global", false)
                    || plugin.getConfigManager().getBoolean("combat.ban-pearls", false)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Ender pearls are disabled.");
                return;
            }
            if (inCombat && plugin.getConfigManager().getBoolean("combat.anti-pearl", true)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot pearl while in combat.");
                return;
            }
            if (plugin.getConfigManager().isEnabled("cooldowns.enabled")
                    && plugin.getConfigManager().getBoolean("cooldowns.pearl-cooldown-enabled", false)) {
                int cooldown = plugin.getConfigManager().getInt("cooldowns.pearl-cooldown", 80);
                if (plugin.getCooldownManager().checkAndApply(player, "pearl", cooldown, "Pearl", event)) {
                    player.setCooldown(Material.ENDER_PEARL, Math.max(0, cooldown));
                }
            }
            return;
        }

        if (type == Material.END_CRYSTAL
                && inCombat
                && (plugin.getConfigManager().getBoolean("combat.ban-crystals", false)
                || plugin.getConfigManager().getBoolean("items.ban-crystals", false))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "End crystals are disabled in combat.");
            return;
        }

        if (type == Material.WIND_CHARGE
                && inCombat
                && plugin.getConfigManager().getBoolean("combat.prevent-wind-charge", false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Wind Charge is blocked while in combat.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombatConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getConfigManager().isEnabled("combat.enabled")) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        if (!plugin.getCombatManager().isInCombat(player)) {
            return;
        }
        Material consumed = event.getItem().getType();
        if ((consumed == Material.GOLDEN_APPLE || consumed == Material.ENCHANTED_GOLDEN_APPLE)
                && plugin.getConfigManager().getBoolean("combat.disable-gapple-while-tagged", false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Gapples are blocked while in combat.");
            return;
        }
        if (consumed == Material.CHORUS_FRUIT
                && plugin.getConfigManager().getBoolean("combat.prevent-chorus-fruit", false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Chorus fruit is blocked while in combat.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombatRiptide(PlayerRiptideEvent event) {
        if (!plugin.getConfigManager().isEnabled("combat.enabled")) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("combat.prevent-riptide-escape", false)) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        if (plugin.getCombatManager().isInCombat(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Riptide is blocked while in combat.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!plugin.getConfigManager().isEnabled("combat.enabled")
                || !plugin.getConfigManager().getBoolean("combat.disable-shield-swap", false)) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        if (!plugin.getCombatManager().isInCombat(player)) {
            return;
        }
        ItemStack main = event.getMainHandItem();
        ItemStack off = event.getOffHandItem();
        if ((main != null && main.getType() == Material.SHIELD) || (off != null && off.getType() == Material.SHIELD)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Shield swapping is blocked while in combat.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        boolean inCombat = plugin.getCombatManager().isInCombat(player);
        if (inCombat
                && plugin.getConfigManager().isEnabled("combat.enabled")
                && plugin.getConfigManager().getBoolean("combat.combat-log-punish", false)
                && !plugin.getConfigManager().hasBypass(player)) {
            plugin.getCombatManager().markCombatLogPenalty(player.getUniqueId());
            if (plugin.getConfigManager().getBoolean("combat.broadcast-tag-status", false)) {
                org.bukkit.Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " combat-logged and will be punished.");
            }
        }
        plugin.getCombatManager().clearTag(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.getCombatManager().clearTag(event.getEntity());
    }

    private boolean isNaked(Player player) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }
}
