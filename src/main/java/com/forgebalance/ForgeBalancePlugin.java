package com.forgebalance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import com.forgebalance.commands.*;
import com.forgebalance.listeners.*;
import com.forgebalance.managers.*;

/**
 * ForgeBalance - Main plugin class for SMP PvP balance and cooldown controls
 * Supports Paper/Spigot 1.21.10 - 1.21.11
 */
public class ForgeBalancePlugin extends JavaPlugin {
    
    private static ForgeBalancePlugin instance;
    private FileConfiguration config;
    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private CombatManager combatManager;
    private RecipeManager recipeManager;
    private MonitoringManager monitoringManager;
    private boolean gracePeriodActive;
    private long graceEndMillis;
    private BukkitTask graceCountdownTask;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        config = getConfig();
        
        // Initialize managers
        configManager = new ConfigManager(this);
        cooldownManager = new CooldownManager(this);
        combatManager = new CombatManager(this);
        recipeManager = new RecipeManager(this);
        monitoringManager = new MonitoringManager(this);
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerListeners();
        
        // Register custom recipes
        if (configManager.isEnabled("recipes.enabled")) {
            recipeManager.registerCustomRecipes();
        }
        
        // Register cooldowns
        if (configManager.isEnabled("cooldowns.enabled")) {
            cooldownManager.registerCooldowns();
        }
        
        getLogger().info("ForgeBalance v1.0.0 enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        if (graceCountdownTask != null) {
            graceCountdownTask.cancel();
            graceCountdownTask = null;
        }
        getLogger().info("ForgeBalance disabled!");
    }
    
    private void registerCommands() {
        // Main commands are optional in plugin.yml at runtime; guard nulls.
        registerCommand("smpstart", new SMPStartCommand(this));
        registerCommand("forgebalance", new ForgeBalanceCommand(this));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing in plugin.yml");
            return;
        }
        command.setExecutor(executor);
    }
    
    private void registerListeners() {
        // Register all listeners once; each module gates itself through config checks.
        Bukkit.getPluginManager().registerEvents(new DamageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemBanListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EffectBanListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InvisibilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WardenListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RitualListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CraftListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MaceListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockBanListener(this), this);
        Bukkit.getPluginManager().registerEvents(new UtilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MonitoringListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemLimiterListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }
    
    public static ForgeBalancePlugin getInstance() {
        return instance;
    }
    
    public FileConfiguration getPluginConfig() {
        return config;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public CombatManager getCombatManager() {
        return combatManager;
    }
    
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public MonitoringManager getMonitoringManager() {
        return monitoringManager;
    }

    public boolean isGracePeriodActive() {
        if (!gracePeriodActive) {
            return false;
        }
        if (System.currentTimeMillis() >= graceEndMillis) {
            endGracePeriod(true);
            return false;
        }
        return true;
    }

    public long getGraceRemainingSeconds() {
        if (!isGracePeriodActive()) {
            return 0L;
        }
        long remainingMs = Math.max(0L, graceEndMillis - System.currentTimeMillis());
        return remainingMs / 1000L;
    }

    public void startGracePeriod(int seconds) {
        if (seconds <= 0) {
            endGracePeriod(false);
            return;
        }
        gracePeriodActive = true;
        graceEndMillis = System.currentTimeMillis() + (seconds * 1000L);
        if (graceCountdownTask != null) {
            graceCountdownTask.cancel();
        }
        graceCountdownTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!gracePeriodActive) {
                return;
            }
            long remaining = getGraceRemainingSeconds();
            if (remaining <= 0) {
                endGracePeriod(true);
                return;
            }
            if (!configManager.getBoolean("smp-start.show-countdown", true)) {
                return;
            }
            if (remaining <= 10 || remaining % 60 == 0) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Grace period: " + ChatColor.WHITE + remaining + "s");
            }
        }, 20L, 20L);

        Bukkit.broadcastMessage(ChatColor.GREEN + "Grace period started for " + seconds + " seconds.");
    }

    public void endGracePeriod(boolean announce) {
        gracePeriodActive = false;
        graceEndMillis = 0L;
        if (graceCountdownTask != null) {
            graceCountdownTask.cancel();
            graceCountdownTask = null;
        }
        if (announce) {
            Bukkit.broadcastMessage(ChatColor.RED + "Grace period has ended.");
        }
    }
}
