package com.forgebalance.listeners;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UtilityListener implements Listener {

    private final ForgeBalancePlugin plugin;
    private final Set<UUID> pendingSpectator = new HashSet<>();

    public UtilityListener(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onOnePlayerSleep(PlayerBedEnterEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.one-player-sleep.enabled", true)) {
            return;
        }
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        World world = event.getPlayer().getWorld();
        long time = world.getTime();
        boolean isNight = time >= 12541 && time <= 23458;
        if (plugin.getConfigManager().getBoolean("utility.one-player-sleep.require-night-time", true) && !isNight) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            world.setTime(0L);
            if (plugin.getConfigManager().getBoolean("utility.one-player-sleep.skip-thunder", true)) {
                world.setStorm(false);
                world.setThundering(false);
            }
            world.setWeatherDuration(0);
            Bukkit.broadcastMessage(ChatColor.YELLOW + event.getPlayer().getName() + " skipped the night.");
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.stop-items-despawn.enabled", false)) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("utility.stop-items-despawn.whitelist-important-items-only", true)) {
            event.setCancelled(true);
            return;
        }

        Item itemEntity = event.getEntity();
        ItemStack item = itemEntity.getItemStack();
        if (item == null) {
            return;
        }
        Material type = item.getType();
        if (type == Material.NETHER_STAR
                || type == Material.HEART_OF_THE_SEA
                || type == Material.ENCHANTED_GOLDEN_APPLE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.dimension-control.enabled", false)) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        World.Environment target = event.getTo().getWorld() == null
                ? event.getPlayer().getWorld().getEnvironment()
                : event.getTo().getWorld().getEnvironment();

        if (target == World.Environment.NORMAL
                && !plugin.getConfigManager().getBoolean("utility.dimension-control.overworld-enabled", true)) {
            event.setCancelled(true);
        } else if (target == World.Environment.NETHER
                && !plugin.getConfigManager().getBoolean("utility.dimension-control.nether-enabled", true)) {
            event.setCancelled(true);
        } else if (target == World.Environment.THE_END
                && !plugin.getConfigManager().getBoolean("utility.dimension-control.end-enabled", true)) {
            event.setCancelled(true);
        }

        if (event.isCancelled()) {
            event.getPlayer().sendMessage(ChatColor.RED + "That dimension is disabled.");
        }
    }

    @EventHandler
    public void onFirstJoin(PlayerJoinEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.first-join-kit.enabled", false)) {
            return;
        }
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore() || !plugin.getConfigManager().getBoolean("utility.first-join-kit.give-once", true);
        if (!firstJoin) {
            return;
        }

        if (plugin.getConfigManager().getBoolean("utility.first-join-kit.clear-default-items", false)) {
            player.getInventory().clear();
        }

        List<String> configured = plugin.getConfigManager().getStringList("utility.first-join-kit.items");
        if (configured.isEmpty()) {
            player.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1));
            player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
            player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
        } else {
            for (String line : configured) {
                ItemStack parsed = parseItem(line);
                if (parsed != null) {
                    player.getInventory().addItem(parsed);
                }
            }
        }

        if (plugin.getConfigManager().getBoolean("utility.first-join-kit.announce", false)) {
            Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " received first-join kit.");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.spectator-after-death.enabled", false)) {
            return;
        }
        Player player = event.getEntity();
        pendingSpectator.add(player.getUniqueId());
        if (plugin.getConfigManager().getBoolean("utility.spectator-after-death.keep-inventory", false)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
        }
        if (plugin.getConfigManager().getBoolean("utility.spectator-after-death.auto-respawn", false)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    player.spigot().respawn();
                } catch (Throwable ignored) {
                    // Older forks may not support direct respawn call.
                }
            });
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingSpectator.remove(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
    }

    @EventHandler(ignoreCancelled = true)
    public void onVillagerTransform(EntityTransformEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.villager-protection.enabled", false)) {
            return;
        }
        if (!plugin.getConfigManager().getBoolean("utility.villager-protection.prevent-zombie-convert", false)) {
            return;
        }
        if (event.getEntityType() == EntityType.VILLAGER && event.getTransformReason() == EntityTransformEvent.TransformReason.INFECTION) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onXpSpawn(EntitySpawnEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.clumps.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof ExperienceOrb)) {
            return;
        }
        ExperienceOrb newOrb = (ExperienceOrb) event.getEntity();
        double radius = plugin.getConfigManager().getDouble("utility.clumps.merge-radius", 4.0D);
        Location location = newOrb.getLocation();
        for (Entity nearby : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (!(nearby instanceof ExperienceOrb) || nearby.getUniqueId().equals(newOrb.getUniqueId())) {
                continue;
            }
            ExperienceOrb orb = (ExperienceOrb) nearby;
            orb.setExperience(orb.getExperience() + newOrb.getExperience());
            newOrb.remove();
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.performance.merge-item-stacks", false)) {
            return;
        }
        Item spawned = event.getEntity();
        ItemStack spawnedStack = spawned.getItemStack();
        if (spawnedStack == null || spawnedStack.getType() == Material.AIR) {
            return;
        }

        double radius = plugin.getConfigManager().getDouble("utility.performance.merge-radius", 3.5D);
        for (Entity nearby : spawned.getWorld().getNearbyEntities(spawned.getLocation(), radius, radius, radius)) {
            if (!(nearby instanceof Item) || nearby.getUniqueId().equals(spawned.getUniqueId())) {
                continue;
            }

            Item other = (Item) nearby;
            ItemStack otherStack = other.getItemStack();
            if (otherStack == null || !otherStack.isSimilar(spawnedStack)) {
                continue;
            }

            int maxStack = otherStack.getMaxStackSize();
            int movable = Math.min(spawnedStack.getAmount(), maxStack - otherStack.getAmount());
            if (movable <= 0) {
                continue;
            }

            otherStack.setAmount(otherStack.getAmount() + movable);
            other.setItemStack(otherStack);
            spawnedStack.setAmount(spawnedStack.getAmount() - movable);

            if (spawnedStack.getAmount() <= 0) {
                spawned.remove();
                return;
            }
            spawned.setItemStack(spawnedStack);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.string-duper-revival.enabled", false)) {
            return;
        }
        handleStringDuper(event.getBlocks());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!plugin.getConfigManager().getBoolean("utility.string-duper-revival.enabled", false)) {
            return;
        }
        handleStringDuper(event.getBlocks());
    }

    private ItemStack parseItem(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        String[] split = line.split(":");
        Material material = Material.matchMaterial(split[0]);
        if (material == null) {
            return null;
        }
        int amount = 1;
        if (split.length > 1) {
            try {
                amount = Integer.parseInt(split[1]);
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        amount = Math.max(1, Math.min(64, amount));
        return new ItemStack(material, amount);
    }

    private void handleStringDuper(List<Block> movedBlocks) {
        if (movedBlocks == null || movedBlocks.isEmpty()) {
            return;
        }
        boolean checkNeighbors = plugin.getConfigManager().getBoolean("utility.string-duper-revival.check-neighbors", true);
        int dropAmount = Math.max(1, plugin.getConfigManager().getInt("utility.string-duper-revival.drop-per-trigger", 1));

        Set<String> droppedLocations = new HashSet<>();
        for (Block moved : movedBlocks) {
            dropStringIfTripwire(moved, dropAmount, droppedLocations);
            if (!checkNeighbors) {
                continue;
            }
            for (BlockFace face : new BlockFace[]{
                    BlockFace.UP,
                    BlockFace.DOWN,
                    BlockFace.NORTH,
                    BlockFace.SOUTH,
                    BlockFace.EAST,
                    BlockFace.WEST
            }) {
                dropStringIfTripwire(moved.getRelative(face), dropAmount, droppedLocations);
            }
        }
    }

    private void dropStringIfTripwire(Block block, int amount, Set<String> droppedLocations) {
        if (block == null) {
            return;
        }
        Material type = block.getType();
        if (type != Material.TRIPWIRE && type != Material.TRIPWIRE_HOOK) {
            return;
        }
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        if (!droppedLocations.add(key)) {
            return;
        }
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), new ItemStack(Material.STRING, amount));
    }
}
