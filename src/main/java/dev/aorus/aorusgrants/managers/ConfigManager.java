package dev.aorus.aorusgrants.managers;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final AorusGrants plugin;
    private FileConfiguration menusConfig;

    public ConfigManager(AorusGrants plugin) {
        this.plugin = plugin;
        loadMenusConfig();
    }

    private void loadMenusConfig() {
        File menusFile = new File(plugin.getDataFolder(), "menus.yml");
        if (!menusFile.exists()) {
            plugin.saveResource("menus.yml", false);
        }
        menusConfig = YamlConfiguration.loadConfiguration(menusFile);
    }

    public void reload() {
        plugin.reloadConfig();
        loadMenusConfig();
    }

    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getMenusConfig() {
        return menusConfig;
    }

    // ── Convenience helpers ────────────────────────────────────

    public String getPrefix() {
        return color(plugin.getConfig().getString("messages.prefix", "&8[&bAorusGrants&8] &r"));
    }

    public String getMessage(String key) {
        return color(plugin.getConfig().getString("messages." + key, "&cMessage not found: " + key));
    }

    public int getSessionTimeout() {
        return plugin.getConfig().getInt("session-timeout", 120);
    }

    public static String color(String text) {
        return ColorUtil.color(text);
    }
}
