package com.forgebalance.managers;

import com.forgebalance.ForgeBalancePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final ForgeBalancePlugin plugin;

    public ConfigManager(ForgeBalancePlugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public boolean isEnabled(String path) {
        FileConfiguration config = getConfig();
        if (!config.getBoolean("settings.enabled", true)) {
            return false;
        }
        return config.getBoolean(path, true);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return getConfig().getBoolean(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return getConfig().getInt(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return getConfig().getDouble(path, defaultValue);
    }

    public String getString(String path, String defaultValue) {
        String value = getConfig().getString(path);
        return value == null ? defaultValue : value;
    }

    public List<String> getStringList(String path) {
        List<String> values = getConfig().getStringList(path);
        return values == null ? Collections.emptyList() : values;
    }

    public boolean hasBypass(Player player) {
        if (player.hasPermission("forgebalance.bypass")) {
            return true;
        }
        boolean adminHasBypass = getBoolean("settings.admin-has-bypass", false);
        return adminHasBypass && player.hasPermission("forgebalance.admin");
    }
}
