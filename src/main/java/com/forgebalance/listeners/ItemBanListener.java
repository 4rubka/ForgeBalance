package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemBanListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public ItemBanListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (isBannedMaterial(item.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This item is banned: " + item.getType().name());
            return;
        }
        sanitizeItem(item, player, false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        ItemStack consumable = event.getConsumable();
        if (consumable != null && isBannedMaterial(consumable.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This ammo is banned: " + consumable.getType().name());
            return;
        }

        ItemStack weapon = event.getBow();
        if (weapon != null && isBannedMaterial(weapon.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This weapon is banned: " + weapon.getType().name());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        ItemStack consumed = event.getItem();
        if (consumed == null) {
            return;
        }
        if (isBannedMaterial(consumed.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This item is banned: " + consumed.getType().name());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        if (isBannedMaterial(item.getType())) {
            event.setCancelled(true);
            return;
        }

        if (item.getType() == Material.MACE
                && plugin.getConfigManager().getBoolean("items.allow-one-mace", true)
                && !plugin.getConfigManager().getBoolean("mace.ban-mace", true)) {
            int maceCount = countMaterial(player.getInventory(), Material.MACE);
            if (maceCount >= 1) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Only one mace is allowed.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (current != null && isBannedMaterial(current.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This item is banned: " + current.getType().name());
            return;
        }
        if (cursor != null && isBannedMaterial(cursor.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This item is banned: " + cursor.getType().name());
            return;
        }

        sanitizeItem(current, player, false);
        sanitizeItem(cursor, player, false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        for (ItemStack item : player.getInventory().getContents()) {
            sanitizeItem(item, player, false);
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            sanitizeItem(armor, player, false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeldItem(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        ItemStack next = player.getInventory().getItem(event.getNewSlot());
        if (next != null && isBannedMaterial(next.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This item is banned: " + next.getType().name());
            return;
        }
        sanitizeItem(next, player, true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!plugin.getConfigManager().isEnabled("enchantments.enabled")) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("enchantments.block-breach-swapping", true)) {
            return;
        }
        if (hasBreach(event.getMainHandItem()) || hasBreach(event.getOffHandItem())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Breach swapping is blocked.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!plugin.getConfigManager().isEnabled("enchantments.enabled")) {
            return;
        }
        if (!(event.getEnchanter() instanceof Player)) {
            return;
        }
        Player player = event.getEnchanter();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        Set<String> banned = getBannedEnchantments();
        boolean blockBreachSwap = plugin.getConfigManager().getBoolean("enchantments.block-breach-swapping", true);

        List<Enchantment> keys = new ArrayList<>(event.getEnchantsToAdd().keySet());
        for (Enchantment enchantment : keys) {
            String key = enchantment.getKey().getKey();

            if (banned.contains(key) && !(blockBreachSwap && "breach".equals(key))) {
                event.getEnchantsToAdd().remove(enchantment);
                continue;
            }

            int level = event.getEnchantsToAdd().getOrDefault(enchantment, 0);
            int limit = getLimitForKey(key);
            if (limit > 0 && level > limit) {
                event.getEnchantsToAdd().put(enchantment, limit);
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        sanitizeItem(result, null, false);
        event.setResult(result);
    }

    private boolean isBannedMaterial(Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }
        if (!plugin.getConfigManager().isEnabled("items.enabled")) {
            return false;
        }

        // Feature-specific toggles take precedence over static list values.
        if (material == Material.TIPPED_ARROW) {
            return plugin.getConfigManager().getBoolean("items.ban-tipped-arrows", true);
        }
        if (material == Material.END_CRYSTAL) {
            return plugin.getConfigManager().getBoolean("items.ban-crystals", false);
        }
        if (material == Material.ENDER_PEARL) {
            return plugin.getConfigManager().getBoolean("items.ban-pearls-global", false);
        }
        if (material == Material.ELYTRA) {
            return plugin.getConfigManager().getBoolean("items.ban-elytra-item", false);
        }
        if (material == Material.CROSSBOW) {
            return plugin.getConfigManager().getBoolean("items.ban-crossbows", false);
        }
        if (material == Material.TRIDENT) {
            return plugin.getConfigManager().getBoolean("items.ban-tridents", false);
        }
        if (material == Material.TOTEM_OF_UNDYING) {
            return plugin.getConfigManager().getBoolean("items.ban-totems", false);
        }

        if (isNetheriteMaterial(material)) {
            return plugin.getConfigManager().getBoolean("items.ban-netherite", true);
        }

        List<String> bannedItems = plugin.getConfigManager().getStringList("items.banned-items");
        for (String name : bannedItems) {
            Material banned = Material.matchMaterial(name);
            if (banned == material) {
                return true;
            }
        }
        if (material == Material.MACE && plugin.getConfigManager().getBoolean("mace.ban-mace", true)) {
            return true;
        }
        return false;
    }

    private boolean isNetheriteMaterial(Material material) {
        return material == Material.NETHERITE_HELMET
                || material == Material.NETHERITE_CHESTPLATE
                || material == Material.NETHERITE_LEGGINGS
                || material == Material.NETHERITE_BOOTS
                || material == Material.NETHERITE_SWORD
                || material == Material.NETHERITE_PICKAXE
                || material == Material.NETHERITE_AXE
                || material == Material.NETHERITE_SHOVEL
                || material == Material.NETHERITE_HOE;
    }

    private int countMaterial(PlayerInventory inventory, Material material) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean hasBreach(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        for (Enchantment enchantment : meta.getEnchants().keySet()) {
            NamespacedKey key = enchantment.getKey();
            if ("breach".equals(key.getKey())) {
                return true;
            }
        }
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
            for (Enchantment enchantment : storageMeta.getStoredEnchants().keySet()) {
                if ("breach".equals(enchantment.getKey().getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> getBannedEnchantments() {
        Set<String> banned = new HashSet<>();
        for (String value : plugin.getConfigManager().getStringList("enchantments.banned-enchantments")) {
            if (value != null && !value.isEmpty()) {
                banned.add(value.toLowerCase());
            }
        }
        return banned;
    }

    private int getLimitForKey(String key) {
        ConfigurationSection enchantments = plugin.getConfig().getConfigurationSection("enchantments");
        if (enchantments == null) {
            return 0;
        }
        if ("protection".equals(key)) {
            return enchantments.getInt("protection-limit", 4);
        }
        if ("sharpness".equals(key)) {
            return enchantments.getInt("sharpness-limit", 5);
        }
        if ("efficiency".equals(key)) {
            return enchantments.getInt("efficiency-limit", 5);
        }
        if ("unbreaking".equals(key)) {
            return enchantments.getInt("unbreaking-limit", 3);
        }
        if ("fortune".equals(key)) {
            return enchantments.getInt("fortune-limit", 3);
        }
        if ("looting".equals(key)) {
            return enchantments.getInt("looting-limit", 3);
        }
        if ("power".equals(key)) {
            return enchantments.getInt("power-limit", 5);
        }
        if ("punch".equals(key)) {
            return enchantments.getInt("punch-limit", 2);
        }
        if ("flame".equals(key)) {
            return enchantments.getInt("flame-limit", 1);
        }
        if ("infinity".equals(key)) {
            return enchantments.getInt("infinity-limit", 1);
        }
        return 0;
    }

    private void sanitizeItem(ItemStack item, Player owner, boolean notify) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (!plugin.getConfigManager().isEnabled("enchantments.enabled")) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        Set<String> bannedEnchantments = getBannedEnchantments();
        boolean blockBreachSwap = plugin.getConfigManager().getBoolean("enchantments.block-breach-swapping", true);

        boolean changed = sanitizeEnchantMap(meta.getEnchants(), (enchantment, level) -> {
            int limit = getLimitForKey(enchantment.getKey().getKey());
            if (limit > 0 && level > limit) {
                meta.removeEnchant(enchantment);
                meta.addEnchant(enchantment, limit, true);
                return true;
            }
            if (bannedEnchantments.contains(enchantment.getKey().getKey())
                    && !("breach".equals(enchantment.getKey().getKey()) && blockBreachSwap)) {
                meta.removeEnchant(enchantment);
                return true;
            }
            return false;
        });

        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
            changed |= sanitizeEnchantMap(storageMeta.getStoredEnchants(), (enchantment, level) -> {
                int limit = getLimitForKey(enchantment.getKey().getKey());
                if (limit > 0 && level > limit) {
                    storageMeta.removeStoredEnchant(enchantment);
                    storageMeta.addStoredEnchant(enchantment, limit, true);
                    return true;
                }
                if (bannedEnchantments.contains(enchantment.getKey().getKey())
                        && !("breach".equals(enchantment.getKey().getKey()) && blockBreachSwap)) {
                    storageMeta.removeStoredEnchant(enchantment);
                    return true;
                }
                return false;
            });
        }

        if (changed) {
            item.setItemMeta(meta);
            if (notify && owner != null) {
                owner.sendMessage(ChatColor.YELLOW + "Illegal enchantments were adjusted.");
            }
        }
    }

    private boolean sanitizeEnchantMap(Map<Enchantment, Integer> enchantMap, EnchantmentSanitizer sanitizer) {
        if (enchantMap == null || enchantMap.isEmpty()) {
            return false;
        }
        boolean changed = false;
        List<Map.Entry<Enchantment, Integer>> entries = new ArrayList<>(enchantMap.entrySet());
        for (Map.Entry<Enchantment, Integer> entry : entries) {
            if (sanitizer.apply(entry.getKey(), entry.getValue())) {
                changed = true;
            }
        }
        return changed;
    }

    @FunctionalInterface
    private interface EnchantmentSanitizer {
        boolean apply(Enchantment enchantment, int level);
    }
}
