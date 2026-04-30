package dev.aorus.aorusgrants.managers;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final AorusGrants plugin;
    private FileConfiguration menusConfig;
    private Map<String, Map<String, String>> messages;

    public ConfigManager(AorusGrants plugin) {
        this.plugin = plugin;
        loadMenusConfig();
        loadMessages();
    }

    private void loadMenusConfig() {
        File menusFile = new File(plugin.getDataFolder(), "menus.yml");
        if (!menusFile.exists()) {
            plugin.saveResource("menus.yml", false);
        }
        menusConfig = YamlConfiguration.loadConfiguration(menusFile);
    }

    private void loadMessages() {
        messages = new HashMap<>();
        
        Map<String, String> es = new HashMap<>();
        es.put("prefix", "&8[&bAorusGrants&8] &r");
        es.put("no-permission", "&cNo tienes permiso para hacer eso.");
        es.put("player-not-found", "&cJugador no encontrado: &e{player}");
        es.put("grant-success", "&aRango &e{group} &aconcedido a &e{player}&a.");
        es.put("demote-success", "&aRango eliminado de &e{player}&a.");
        es.put("no-groups", "&cEste jugador no tiene rangos para eliminar.");
        es.put("session-expired", "&cTu sesión ha expirado. Abre el menú de nuevo.");
        es.put("luckperms-not-found", "&cLuckPerms no está instalado o habilitado.");
        es.put("notify-offline", "&e&l¡Recibiste rangos mientras estabas offline!");
        es.put("duration-select", "&7Selecciona la duración:");
        
        Map<String, String> en = new HashMap<>();
        en.put("prefix", "&8[&bAorusGrants&8] &r");
        en.put("no-permission", "&cYou don't have permission to do that.");
        en.put("player-not-found", "&cPlayer not found: &e{player}");
        en.put("grant-success", "&aSuccessfully granted &e{group} &ato &e{player}&a.");
        en.put("demote-success", "&aSuccessfully removed rank from &e{player}&a.");
        en.put("no-groups", "&cThis player has no ranks to remove.");
        en.put("session-expired", "&cYour session has expired. Please reopen the menu.");
        en.put("luckperms-not-found", "&cLuckPerms is not installed or enabled.");
        en.put("notify-offline", "&e&lYou received ranks while offline!");
        en.put("duration-select", "&7Select duration:");
        
        messages.put("es", es);
        messages.put("en", en);
    }

    public void reload() {
        plugin.reloadConfig();
        loadMenusConfig();
        loadMessages();
    }

    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getMenusConfig() {
        return menusConfig;
    }

    public String getPrefix() {
        String lang = plugin.getConfig().getString("language", "es");
        return color(getMessage("prefix", lang));
    }

    public String getMessage(String key) {
        String lang = plugin.getConfig().getString("language", "es");
        return getMessage(key, lang);
    }
    
    public String getMessage(String key, String lang) {
        Map<String, String> langMessages = messages.getOrDefault(lang, messages.get("es"));
        String msg = langMessages.getOrDefault(key, key);
        return color(msg);
    }

    public int getSessionTimeout() {
        return plugin.getConfig().getInt("session-timeout", 120);
    }

    public String getLanguage(org.bukkit.entity.Player player) {
        String lang = plugin.getConfig().getString("language", "es");
        String playerLang = plugin.getConfig().getString("player-languages." + player.getUniqueId(), "");
        return playerLang.isEmpty() ? lang : playerLang;
    }

    public void setPlayerLanguage(org.bukkit.entity.Player player, String lang) {
        if (messages.containsKey(lang)) {
            plugin.getConfig().set("player-languages." + player.getUniqueId(), lang);
            plugin.saveConfig();
        }
    }

    public static String color(String text) {
        return ColorUtil.color(text);
    }
}
