package com.forgebalance.gui;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AdminMenu {

    public static final String TITLE_PREFIX = ChatColor.DARK_AQUA + "ForgeBalance Control";
    private static final int INVENTORY_SIZE = 54;
    private static final int[] CONTENT_SLOTS = new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final Map<MenuPage, List<ToggleEntry>> PAGE_TOGGLES = new EnumMap<>(MenuPage.class);

    static {
        PAGE_TOGGLES.put(MenuPage.CORE, Arrays.asList(
                toggle("settings.enabled", "Global Plugin", "Master switch for all ForgeBalance systems.", true),
                toggle("pvp.enabled", "Global PvP", "Turns all player-vs-player damage on or off.", true),
                toggle("smp-start.enabled", "SMP Start System", "Allows /smpstart grace automation.", true),
                toggle("rituals.enabled", "Ritual Engine", "Allows ritual interactions and ritual effects.", true),
                toggle("invisibility.enabled", "Invisibility QoL", "Applies anonymous invisibility behavior.", true),
                toggle("warden.enabled", "Warden Heart Drops", "Wardens can drop custom Warden Hearts.", true),
                toggle("recipes.enabled", "Custom Recipes", "Registers ForgeBalance custom recipes.", true),
                toggle("one-craft.enabled", "One-Craft Limits", "Enforces per-player craft limits.", true),
                toggle("utility.one-player-sleep.enabled", "One Player Sleep", "Night skips when one player sleeps.", true),
                toggle("utility.clumps.enabled", "XP Clumps", "Merges nearby XP orbs to reduce lag.", true),
                toggle("utility.stop-items-despawn.enabled", "No Item Despawn", "Prevents ground items from despawning.", false),
                toggle("utility.dimension-control.enabled", "Dimension Control", "Allows enabling/disabling dimensions.", false),
                toggle("utility.first-join-kit.enabled", "First Join Kit", "Gives a starter kit once per player.", false),
                toggle("utility.spectator-after-death.enabled", "Spectator After Death", "Moves players to spectator on death.", false)
        ));

        PAGE_TOGGLES.put(MenuPage.BALANCE, Arrays.asList(
                toggle("damage.enabled", "Damage Module", "Enables all damage balancing logic.", true),
                toggle("damage.use-global-multiplier", "Use Global Multiplier", "Applies global damage multiplier to all damage.", true),
                toggle("damage.use-pvp-multiplier", "Use PvP Multiplier", "Applies pvp-specific damage multiplier.", true),
                toggle("damage.use-pve-multiplier", "Use PvE Multiplier", "Applies pve-specific damage multiplier.", true),
                toggle("damage.enable-custom-damage-table", "Weapon Damage Table", "Uses custom per-weapon base damage values.", true),
                toggle("enchantments.enabled", "Enchant Rules", "Activates enchant bans and level caps.", true),
                toggle("enchantments.block-breach-swapping", "Block Breach Swap", "Stops swapping Breach weapon in/off hand.", true),
                toggle("effects.enabled", "Effect Ban Module", "Checks and blocks configured potion effects.", true),
                toggle("effects.block-strength", "Block Strength", "Blocks Strength effect from being applied.", true),
                toggle("effects.block-speed", "Block Speed", "Blocks Speed effect from being applied.", false),
                toggle("effects.block-resistance", "Block Resistance", "Blocks Resistance effect from being applied.", true),
                toggle("effects.block-fire-resistance", "Block Fire Resistance", "Blocks Fire Resistance effect from being applied.", false),
                toggle("effects.block-regeneration", "Block Regeneration", "Blocks Regeneration effect from being applied.", false),
                toggle("effects.block-jump-boost", "Block Jump Boost", "Blocks Jump Boost effect from being applied.", false),
                toggle("effects.show-block-message", "Effect Block Message", "Notifies players when effect is blocked.", true)
        ));

        PAGE_TOGGLES.put(MenuPage.COMBAT, Arrays.asList(
                toggle("combat.enabled", "Combat Module", "Enables combat tag and combat restrictions.", true),
                toggle("combat.anti-restock", "Anti Restock", "Blocks container restocking while in combat.", true),
                toggle("combat.anti-elytra", "Anti Elytra", "Blocks elytra glide toggle while tagged.", true),
                toggle("combat.anti-pearl", "Anti Pearl", "Blocks pearls while player is combat tagged.", true),
                toggle("combat.no-naked-killing", "No Naked Killing", "Cancels attacks on players with no armor.", false),
                toggle("combat.ban-crystals", "Ban Crystals", "Blocks end crystal usage for combat.", false),
                toggle("combat.ban-pearls", "Hard Ban Pearls", "Hard disables ender pearl usage globally.", false),
                toggle("combat.prevent-chorus-fruit", "Block Chorus Escape", "Blocks chorus-fruit combat escape.", false),
                toggle("combat.prevent-ender-chest-open", "Block Ender Chest", "Blocks ender chest opening in combat.", false),
                toggle("combat.prevent-wind-charge", "Block Wind Charge Escape", "Blocks wind charge movement while tagged.", false),
                toggle("combat.prevent-riptide-escape", "Block Riptide Escape", "Blocks riptide movement while tagged.", false),
                toggle("combat.disable-shield-swap", "Disable Shield Swap", "Blocks swapping shield in combat.", false),
                toggle("combat.disable-gapple-while-tagged", "Disable Gap While Tagged", "Blocks gapples during combat tag.", false),
                toggle("combat.combat-log-punish", "Combat Log Punish", "Punishes players who leave while combat tagged.", false),
                toggle("combat.track-hit-statistics", "Track Hit Statistics", "Records PvP hit counts and damage stats.", false)
        ));

        PAGE_TOGGLES.put(MenuPage.BANS, Arrays.asList(
                toggle("items.enabled", "Item Ban Module", "Enables item/material banning logic.", true),
                toggle("items.ban-tipped-arrows", "Ban Tipped Arrows", "Blocks tipped arrow usage and pickup.", true),
                toggle("items.ban-anchors", "Ban Anchors", "Blocks respawn anchors placement/use.", true),
                toggle("items.ban-bed-bombing", "Ban Bed Bombing", "Blocks bed explosions outside overworld.", true),
                toggle("items.ban-tnt-minecarts", "Ban TNT Minecarts", "Blocks TNT minecart placement/spawn.", true),
                toggle("mace.ban-mace", "Ban Mace", "Blocks all mace attacks and use.", true),
                toggle("items.allow-one-mace", "One Mace Per Player", "Limits players to a single mace.", true),
                toggle("items.ban-netherite", "Ban Netherite", "Blocks configured netherite gear/items.", true),
                toggle("items.ban-killing-villagers", "Ban Villager Killing", "Prevents players from killing villagers.", false),
                toggle("items.ban-crystals", "Ban End Crystals", "Blocks crystal placement and use.", false),
                toggle("items.ban-pearls-global", "Ban Pearls Global", "Globally bans pearl use regardless of combat.", false),
                toggle("items.ban-elytra-item", "Ban Elytra Item", "Blocks equipping/using elytra item.", false),
                toggle("items.ban-crossbows", "Ban Crossbows", "Blocks crossbow usage.", false),
                toggle("items.ban-tridents", "Ban Tridents", "Blocks trident usage.", false),
                toggle("items.ban-totems", "Ban Totems", "Blocks holding/using totems.", false),
                toggle("items.item-limiter.enabled", "Item Limiter", "Enforces overstack and shulker nesting limits.", false),
                toggle("items.item-limiter.block-overstack", "Block Overstack", "Clamps stacks above configured max size.", false),
                toggle("items.item-limiter.block-shulker-nests", "Block Shulker Nesting", "Prevents shulker boxes inside shulkers.", false)
        ));

        PAGE_TOGGLES.put(MenuPage.COOLDOWNS, Arrays.asList(
                toggle("cooldowns.enabled", "Cooldown Module", "Master switch for ForgeBalance cooldowns.", true),
                toggle("cooldowns.mace-cooldown-enabled", "Mace Cooldown", "Applies custom cooldown to mace hits.", true),
                toggle("cooldowns.shield-cooldown-enabled", "Shield Cooldown", "Applies shield cooldown after blocking.", true),
                toggle("cooldowns.wind-charge-cooldown-enabled", "Wind Charge Cooldown", "Applies cooldown to wind charge.", true),
                toggle("cooldowns.riptide-cooldown-enabled", "Riptide Cooldown", "Applies cooldown to riptide usage.", true),
                toggle("cooldowns.gap-cooldown-enabled", "Gapple Cooldown", "Applies cooldown to golden apples.", true),
                toggle("cooldowns.pearl-cooldown-enabled", "Pearl Cooldown", "Applies cooldown to ender pearls.", false),
                toggle("cooldowns.pause-during-grace", "Pause During Grace", "Pauses cooldown checks during grace period.", false),
                toggle("cooldowns.reset-on-death", "Reset On Death", "Clears active cooldowns when player dies.", true),
                toggle("cooldowns.show-messages", "Show Cooldown Messages", "Sends remaining cooldown to player.", true)
        ));

        PAGE_TOGGLES.put(MenuPage.UTILITY, Arrays.asList(
                toggle("utility.one-player-sleep.skip-thunder", "Sleep Skips Thunder", "Clears storm when one-player-sleep triggers.", true),
                toggle("utility.one-player-sleep.require-night-time", "Sleep Requires Night", "Only allow one-player-sleep at night.", true),
                toggle("utility.stop-items-despawn.whitelist-important-items-only", "Protect Important Drops Only", "Only protect configured important items from despawn.", true),
                toggle("utility.dimension-control.overworld-enabled", "Overworld Enabled", "Allows players to enter/use Overworld.", true),
                toggle("utility.dimension-control.nether-enabled", "Nether Enabled", "Allows players to enter/use Nether.", true),
                toggle("utility.dimension-control.end-enabled", "End Enabled", "Allows players to enter/use The End.", true),
                toggle("utility.villager-protection.enabled", "Villager Protection", "Protects villagers from configured causes.", false),
                toggle("utility.villager-protection.prevent-player-kill", "Stop Player Villager Kill", "Cancels direct villager kills by players.", false),
                toggle("utility.villager-protection.prevent-zombie-convert", "Stop Villager Convert", "Prevents villager zombie conversion.", false),
                toggle("utility.first-join-kit.give-once", "Kit Once", "Ensures first-join kit is given only once.", true),
                toggle("utility.first-join-kit.clear-default-items", "Clear Default Items", "Clears starter inventory before giving kit.", false),
                toggle("utility.first-join-kit.announce", "Announce First Join Kit", "Broadcasts when starter kit is granted.", false),
                toggle("utility.spectator-after-death.keep-inventory", "Spectator Keep Inventory", "Keeps inventory when set spectator on death.", false),
                toggle("utility.spectator-after-death.auto-respawn", "Spectator Auto Respawn", "Auto respawns dead players to spectator mode.", false),
                toggle("utility.anti-xaero-minimap.enabled", "Anti Xaero Minimap", "Detects Xaero channels and applies punish policy.", false),
                toggle("utility.anti-xray.enabled", "Anti X-Ray Heuristic", "Tracks suspicious ore mining ratios and alerts staff.", false),
                toggle("utility.health-indicators.disable-third-party", "Disable External HP", "Removes health objectives used by external indicators.", false),
                toggle("utility.health-indicators.builtin.enabled", "Built-in Health Indicator", "Shows target health in action bar on hit.", false),
                toggle("utility.string-duper-revival.enabled", "String Duper Revival", "Re-enables configurable string duper behavior.", false),
                toggle("utility.performance.merge-item-stacks", "Merge Item Stacks", "Merges nearby identical item entities for performance.", false)
        ));
    }

    private AdminMenu() {
    }

    public static void open(Player player, ForgeBalancePlugin plugin) {
        openPage(player, plugin, 0);
    }

    public static void openPage(Player player, ForgeBalancePlugin plugin, int pageIndex) {
        int safePage = normalizePage(pageIndex);
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, buildTitle(safePage));
        populate(inventory, plugin, safePage);
        player.openInventory(inventory);
    }

    public static void populate(Inventory inventory, ForgeBalancePlugin plugin, int pageIndex) {
        int safePage = normalizePage(pageIndex);
        inventory.clear();
        ItemStack filler = createFiller();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        renderPageTabs(inventory, safePage, plugin);
        renderToggleItems(inventory, safePage, plugin);
        renderNavigation(inventory, safePage, plugin);
    }

    public static NamespacedKey getToggleKey(ForgeBalancePlugin plugin) {
        return new NamespacedKey(plugin, "menu_toggle_path");
    }

    public static NamespacedKey getActionKey(ForgeBalancePlugin plugin) {
        return new NamespacedKey(plugin, "menu_action");
    }

    public static NamespacedKey getPageKey(ForgeBalancePlugin plugin) {
        return new NamespacedKey(plugin, "menu_page_index");
    }

    public static boolean isMenuTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public static int getPageIndexFromTitle(String title) {
        if (title == null) {
            return 0;
        }
        for (int i = 0; i < MenuPage.values().length; i++) {
            if (title.equals(buildTitle(i))) {
                return i;
            }
        }
        return 0;
    }

    private static void renderPageTabs(Inventory inventory, int currentPage, ForgeBalancePlugin plugin) {
        MenuPage[] pages = MenuPage.values();
        for (int i = 0; i < pages.length; i++) {
            int slot = i + 1;
            MenuPage page = pages[i];
            boolean active = i == currentPage;
            ItemStack item = new ItemStack(page.icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((active ? ChatColor.GREEN : ChatColor.AQUA) + page.title);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + page.description);
                lore.add(ChatColor.YELLOW + "Click to open page");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(
                        getActionKey(plugin),
                        PersistentDataType.STRING,
                        "page:" + i
                );
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
        }
    }

    private static void renderToggleItems(Inventory inventory, int currentPage, ForgeBalancePlugin plugin) {
        MenuPage page = MenuPage.values()[currentPage];
        List<ToggleEntry> toggles = PAGE_TOGGLES.getOrDefault(page, Collections.emptyList());
        int maxItems = Math.min(CONTENT_SLOTS.length, toggles.size());
        for (int i = 0; i < maxItems; i++) {
            ToggleEntry toggle = toggles.get(i);
            inventory.setItem(CONTENT_SLOTS[i], createToggleItem(toggle, currentPage, plugin));
        }
    }

    private static void renderNavigation(Inventory inventory, int currentPage, ForgeBalancePlugin plugin) {
        int previousPage = normalizePage(currentPage - 1);
        int nextPage = normalizePage(currentPage + 1);

        inventory.setItem(45, createActionItem(
                Material.ARROW,
                ChatColor.YELLOW + "Previous Page",
                Arrays.asList(ChatColor.GRAY + "Go to previous page.", ChatColor.DARK_GRAY + "Page: " + (previousPage + 1)),
                "page:" + previousPage,
                plugin
        ));
        inventory.setItem(49, createActionItem(
                Material.BARRIER,
                ChatColor.RED + "Close Menu",
                Arrays.asList(ChatColor.GRAY + "Close ForgeBalance menu."),
                "close",
                plugin
        ));
        inventory.setItem(53, createActionItem(
                Material.ARROW,
                ChatColor.YELLOW + "Next Page",
                Arrays.asList(ChatColor.GRAY + "Go to next page.", ChatColor.DARK_GRAY + "Page: " + (nextPage + 1)),
                "page:" + nextPage,
                plugin
        ));
    }

    private static ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + " ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createToggleItem(ToggleEntry toggle, int pageIndex, ForgeBalancePlugin plugin) {
        boolean enabled = plugin.getConfigManager().getBoolean(toggle.path, toggle.defaultValue);
        Material indicator = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(indicator);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + toggle.displayName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + toggle.description);
            lore.add(ChatColor.GRAY + "State: " + (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
            if (plugin.getConfigManager().getBoolean("menu.show-default-in-lore", true)) {
                lore.add(ChatColor.DARK_GRAY + "Default: " + (toggle.defaultValue ? "ON" : "OFF"));
            }
            if (plugin.getConfigManager().getBoolean("menu.show-path-in-lore", true)) {
                lore.add(ChatColor.DARK_GRAY + "Path: " + toggle.path);
            }
            lore.add(ChatColor.YELLOW + "Click to toggle");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(getToggleKey(plugin), PersistentDataType.STRING, toggle.path);
            meta.getPersistentDataContainer().set(getPageKey(plugin), PersistentDataType.INTEGER, pageIndex);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createActionItem(
            Material material,
            String name,
            List<String> lore,
            String action,
            ForgeBalancePlugin plugin
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(getActionKey(plugin), PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static int normalizePage(int pageIndex) {
        int total = MenuPage.values().length;
        if (total == 0) {
            return 0;
        }
        if (pageIndex < 0) {
            return total - 1;
        }
        if (pageIndex >= total) {
            return 0;
        }
        return pageIndex;
    }

    private static String buildTitle(int pageIndex) {
        MenuPage page = MenuPage.values()[normalizePage(pageIndex)];
        return TITLE_PREFIX + " | " + page.title;
    }

    private static ToggleEntry toggle(String path, String displayName, String description, boolean defaultValue) {
        return new ToggleEntry(path, displayName, description, defaultValue);
    }

    private enum MenuPage {
        CORE("Core", Material.NETHER_STAR, "Main systems and server-wide modules."),
        BALANCE("Balance", Material.ENCHANTED_BOOK, "Damage, effects, and enchant balancing."),
        COMBAT("Combat", Material.SHIELD, "Combat tag and anti-escape options."),
        BANS("Bans", Material.BARRIER, "Item and rule bans."),
        COOLDOWNS("Cooldowns", Material.CLOCK, "Cooldown toggles for key mechanics."),
        UTILITY("Utility", Material.COMPASS, "Extra systems and compatibility tools.");

        private final String title;
        private final Material icon;
        private final String description;

        MenuPage(String title, Material icon, String description) {
            this.title = title;
            this.icon = icon;
            this.description = description;
        }
    }

    private static final class ToggleEntry {
        private final String path;
        private final String displayName;
        private final String description;
        private final boolean defaultValue;

        private ToggleEntry(String path, String displayName, String description, boolean defaultValue) {
            this.path = path;
            this.displayName = displayName;
            this.description = description;
            this.defaultValue = defaultValue;
        }
    }
}
