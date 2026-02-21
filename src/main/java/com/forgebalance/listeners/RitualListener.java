package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RitualListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public RitualListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRitualUse(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().getBoolean("rituals.enabled", true)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.LODESTONE) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !plugin.getRecipeManager().isWardenHeart(item)) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 30, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0));
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 40, 0.5, 1.0, 0.5, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.0F);
        player.sendMessage(ChatColor.DARK_AQUA + "Ritual completed. Deep dark energy flows through you.");
    }
}
