package dev.aorus.aorusgrants.listeners;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.managers.ConfigManager;
import dev.aorus.aorusgrants.managers.HistoryManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LoginNotificationListener implements Listener {

    private final AorusGrants plugin;

    public LoginNotificationListener(AorusGrants plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("notify-on-join", true)) return;
        
        Player player = e.getPlayer();
        if (player.hasPermission("aorusgrants.notify.bypass")) return;
        
        List<HistoryManager.HistoryEntry> entries = plugin.getHistoryManager().getHistory(player.getUniqueId());
        if (entries.isEmpty()) return;

        long lastPlayed = player.getLastPlayed();
        
        List<HistoryManager.HistoryEntry> recent = new ArrayList<>();
        for (HistoryManager.HistoryEntry entry : entries) {
            if (entry.timestamp() > lastPlayed && entry.isActive()) {
                recent.add(entry);
            }
        }

        if (recent.isEmpty()) return;

        String prefix = plugin.getConfigManager().getPrefix();

        player.sendMessage(ConfigManager.color(prefix + "&e&l You received ranks while offline!"));
        player.sendMessage(ConfigManager.color("&8&m                             &r"));

        for (HistoryManager.HistoryEntry entry : recent) {
            String msg = "&7- &b" + entry.group() + " &7by &e" + entry.staffName()
                    + " &7(" + entry.duration() + ")";
            player.sendMessage(ConfigManager.color(msg));
        }

        player.sendMessage(ConfigManager.color("&8&m                             &r"));
    }
}