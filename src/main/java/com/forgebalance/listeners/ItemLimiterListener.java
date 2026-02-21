package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemLimiterListener implements Listener {

    private final ForgeBalancePlugin plugin;

    public ItemLimiterListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isItemLimiterEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        sanitizePlayerInventory(player, false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!isItemLimiterEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        ItemStack stack = event.getItem().getItemStack();
        if (stack == null) {
            return;
        }

        if (isBlockShulkerNests() && hasNestedShulker(stack)) {
            event.setCancelled(true);
            sendLimiterMessage(player, "Nested shulker boxes are blocked.");
            return;
        }

        if (isBlockOverstack()) {
            clampStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isItemLimiterEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped == null) {
            return;
        }
        if (isBlockShulkerNests() && hasNestedShulker(dropped)) {
            event.setCancelled(true);
            sendLimiterMessage(player, "Nested shulker boxes are blocked.");
            return;
        }
        if (isBlockOverstack()) {
            clampStack(dropped);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isItemLimiterEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (isBlockOverstack()) {
            if (clampStack(current) | clampStack(cursor)) {
                player.updateInventory();
            }
        }

        if (!isBlockShulkerNests()) {
            return;
        }

        if (isShulkerTargetInventory(event.getView().getTopInventory(), event.getClickedInventory())) {
            if (isShulkerItem(cursor) || (event.isShiftClick() && isShulkerItem(current))) {
                event.setCancelled(true);
                sendLimiterMessage(player, "Shulker boxes inside shulker boxes are blocked.");
                return;
            }
        }

        if (hasNestedShulker(cursor) || hasNestedShulker(current)) {
            event.setCancelled(true);
            sendLimiterMessage(player, "Nested shulker boxes are blocked.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isItemLimiterEnabled() || !isBlockShulkerNests()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (plugin.getConfigManager().hasBypass(player)) {
            return;
        }
        ItemStack oldCursor = event.getOldCursor();
        if (isShulkerTargetInventory(event.getView().getTopInventory(), event.getView().getTopInventory())
                && isShulkerItem(oldCursor)) {
            event.setCancelled(true);
            sendLimiterMessage(player, "Shulker boxes inside shulker boxes are blocked.");
        }
    }

    private void sanitizePlayerInventory(Player player, boolean notify) {
        PlayerInventory inventory = player.getInventory();
        boolean changed = false;

        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (sanitizeStack(item)) {
                changed = true;
            }
        }

        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (sanitizeStack(item)) {
                changed = true;
            }
        }

        ItemStack offhand = inventory.getItemInOffHand();
        if (sanitizeStack(offhand)) {
            changed = true;
        }

        if (changed) {
            player.updateInventory();
            if (notify) {
                sendLimiterMessage(player, "Item limiter adjusted illegal items in your inventory.");
            }
        }
    }

    private boolean sanitizeStack(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        boolean changed = false;
        if (isBlockOverstack()) {
            changed |= clampStack(stack);
        }
        if (isBlockShulkerNests()) {
            changed |= stripNestedShulkerItems(stack);
        }
        return changed;
    }

    private boolean clampStack(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        int maxAllowed = Math.max(1, plugin.getConfigManager().getInt("items.item-limiter.max-stack-override", 64));
        if (stack.getAmount() <= maxAllowed) {
            return false;
        }
        stack.setAmount(maxAllowed);
        return true;
    }

    private boolean stripNestedShulkerItems(ItemStack stack) {
        if (stack == null || !isShulkerItem(stack)) {
            return false;
        }
        ItemMeta itemMeta = stack.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return false;
        }
        BlockStateMeta meta = (BlockStateMeta) itemMeta;
        if (!(meta.getBlockState() instanceof ShulkerBox)) {
            return false;
        }
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        ItemStack[] stored = box.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < stored.length; i++) {
            ItemStack nested = stored[i];
            if (isShulkerItem(nested)) {
                stored[i] = null;
                changed = true;
            } else if (nested != null && isBlockOverstack()) {
                changed |= clampStack(nested);
            }
        }
        if (!changed) {
            return false;
        }
        box.getInventory().setContents(stored);
        meta.setBlockState(box);
        stack.setItemMeta(meta);
        return true;
    }

    private boolean hasNestedShulker(ItemStack stack) {
        if (stack == null || !isShulkerItem(stack)) {
            return false;
        }
        ItemMeta itemMeta = stack.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return false;
        }
        BlockStateMeta meta = (BlockStateMeta) itemMeta;
        if (!(meta.getBlockState() instanceof ShulkerBox)) {
            return false;
        }
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        for (ItemStack nested : box.getInventory().getContents()) {
            if (isShulkerItem(nested)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShulkerItem(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Material material = stack.getType();
        return material != null && material.name().endsWith("SHULKER_BOX");
    }

    private boolean isShulkerTargetInventory(Inventory top, Inventory clicked) {
        return top != null && clicked != null && top.getType() == InventoryType.SHULKER_BOX && clicked.equals(top);
    }

    private boolean isItemLimiterEnabled() {
        return plugin.getConfigManager().getBoolean("items.item-limiter.enabled", false);
    }

    private boolean isBlockOverstack() {
        return plugin.getConfigManager().getBoolean("items.item-limiter.block-overstack", false);
    }

    private boolean isBlockShulkerNests() {
        return plugin.getConfigManager().getBoolean("items.item-limiter.block-shulker-nests", false);
    }

    private void sendLimiterMessage(Player player, String message) {
        if (plugin.getConfigManager().getBoolean("items.item-limiter.notify-player", true)) {
            player.sendMessage(ChatColor.RED + message);
        }
    }
}
