package dev.aorus.aorusgrants.managers;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.model.GrantSession;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final AorusGrants plugin;
    private final Map<UUID, GrantSession> sessions = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public SessionManager(AorusGrants plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    private void startCleanupTask() {
        // Run every 30 seconds to clean expired sessions
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanExpired, 600L, 600L);
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        long timeoutMs = (long) plugin.getConfigManager().getSessionTimeout() * 1000L;

        sessions.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().getCreatedAt()) > timeoutMs;
            if (expired) {
                plugin.getLogger().info("Session expired for staff: " + entry.getKey());
            }
            return expired;
        });
    }

    public GrantSession createSession(UUID staffUUID, UUID targetUUID, String targetName) {
        GrantSession session = new GrantSession(staffUUID, targetUUID, targetName);
        sessions.put(staffUUID, session);
        return session;
    }

    public GrantSession getSession(UUID staffUUID) {
        GrantSession session = sessions.get(staffUUID);
        if (session == null) return null;

        long now = System.currentTimeMillis();
        long timeoutMs = (long) plugin.getConfigManager().getSessionTimeout() * 1000L;

        if ((now - session.getCreatedAt()) > timeoutMs) {
            sessions.remove(staffUUID);
            return null;
        }

        session.refresh();
        return session;
    }

    public boolean hasSession(UUID staffUUID) {
        return getSession(staffUUID) != null;
    }

    public void removeSession(UUID staffUUID) {
        sessions.remove(staffUUID);
    }

    public void clearAll() {
        sessions.clear();
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
    }
}
