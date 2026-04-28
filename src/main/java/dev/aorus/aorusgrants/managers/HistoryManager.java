package dev.aorus.aorusgrants.managers;

import dev.aorus.aorusgrants.AorusGrants;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages the grant history log stored in plugins/AorusGrants/history.yml.
 *
 * Structure per entry (under root key "history"):
 *   history:
 *     <uuid-random-key>:
 *       targetUuid:   "..."
 *       targetName:   "..."
 *       staffUuid:    "..."   (or "CONSOLE")
 *       staffName:    "..."
 *       group:        "..."
 *       duration:     "Permanent" | "3d 2h 10m"
 *       permanent:    true|false
 *       timestamp:    1234567890   (epoch seconds)
 *       expiresAt:    1234567890   (epoch seconds, 0 if permanent)
 *
 * This class ONLY appends entries and reads them.
 * No refactoring of any other class is required.
 */
public class HistoryManager {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                             .withZone(ZoneId.systemDefault());

    private final AorusGrants plugin;
    private final File historyFile;
    private YamlConfiguration yaml;

    public HistoryManager(AorusGrants plugin) {
        this.plugin      = plugin;
        this.historyFile = new File(plugin.getDataFolder(), "history.yml");
        load();
    }

    // ─────────────────────────────────────────────────────────
    //   LOAD / SAVE
    // ─────────────────────────────────────────────────────────

    private void load() {
        if (!historyFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                historyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("[HistoryManager] Could not create history.yml: " + e.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(historyFile);
    }

    private void save() {
        try {
            yaml.save(historyFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[HistoryManager] Could not save history.yml: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    //   RECORD A GRANT
    // ─────────────────────────────────────────────────────────

    /**
     * Records a successful grant.
     * Call this right after LPExecutor.executeGrant() returns true.
     *
     * @param targetUuid   UUID of the player who received the rank
     * @param targetName   Display name of the target player
     * @param staffUuid    UUID of the staff who executed the grant
     * @param staffName    Name of the staff (use "CONSOLE" for console)
     * @param group        Group name that was granted
     * @param durationDisplay  Human-readable duration (from session.getDurationDisplay())
     * @param permanent    Whether the grant is permanent
     * @param totalMinutes Total minutes of the grant (0 if permanent)
     */
    public void recordGrant(UUID targetUuid, String targetName,
                            UUID staffUuid, String staffName,
                            String group,
                            String durationDisplay, boolean permanent,
                            long totalMinutes) {

        // Unique key per entry: timestamp + random suffix to avoid collisions
        String key = "history." + targetUuid.toString().replace("-", "")
                   + "_" + System.currentTimeMillis();

        long now      = Instant.now().getEpochSecond();
        long expiresAt = permanent ? 0L : (now + totalMinutes * 60L);

        yaml.set(key + ".targetUuid",  targetUuid.toString());
        yaml.set(key + ".targetName",  targetName);
        yaml.set(key + ".staffUuid",   staffUuid != null ? staffUuid.toString() : "CONSOLE");
        yaml.set(key + ".staffName",   staffName);
        yaml.set(key + ".group",       group);
        yaml.set(key + ".duration",    durationDisplay);
        yaml.set(key + ".permanent",   permanent);
        yaml.set(key + ".timestamp",   now);
        yaml.set(key + ".expiresAt",   expiresAt);

        save();

        plugin.getLogger().info("[History] Recorded grant: " + staffName
                + " -> " + group + " -> " + targetName
                + " | " + (permanent ? "Permanent" : durationDisplay));
    }

    // ─────────────────────────────────────────────────────────
    //   QUERY HISTORY FOR A PLAYER
    // ─────────────────────────────────────────────────────────

    /**
     * Returns all history entries for a given target UUID,
     * sorted newest-first.
     */
    public List<HistoryEntry> getHistory(UUID targetUuid) {
        List<HistoryEntry> result = new ArrayList<>();

        var section = yaml.getConfigurationSection("history");
        if (section == null) return result;

        String targetStr = targetUuid.toString();

        for (String key : section.getKeys(false)) {
            String path = "history." + key;
            String storedTarget = yaml.getString(path + ".targetUuid", "");
            if (!storedTarget.equals(targetStr)) continue;

            result.add(new HistoryEntry(
                storedTarget,
                yaml.getString(path  + ".targetName",  "Unknown"),
                yaml.getString(path  + ".staffUuid",   "CONSOLE"),
                yaml.getString(path  + ".staffName",   "Unknown"),
                yaml.getString(path  + ".group",       "?"),
                yaml.getString(path  + ".duration",    "?"),
                yaml.getBoolean(path + ".permanent",   false),
                yaml.getLong(path    + ".timestamp",   0L),
                yaml.getLong(path    + ".expiresAt",   0L)
            ));
        }

        // Sort newest first
        result.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        return result;
    }

    /**
     * Returns all history entries made by a given staff UUID,
     * sorted newest-first.
     */
    public List<HistoryEntry> getStaffHistory(UUID staffUuid) {
        List<HistoryEntry> result = new ArrayList<>();

        var section = yaml.getConfigurationSection("history");
        if (section == null) return result;

        String staffStr = staffUuid.toString();

        for (String key : section.getKeys(false)) {
            String path = "history." + key;
            String storedStaff = yaml.getString(path + ".staffUuid", "");
            if (!storedStaff.equals(staffStr)) continue;

            result.add(new HistoryEntry(
                yaml.getString(path + ".targetUuid", ""),
                yaml.getString(path + ".targetName", "Unknown"),
                storedStaff,
                yaml.getString(path + ".staffName", "Unknown"),
                yaml.getString(path + ".group", "?"),
                yaml.getString(path + ".duration", "?"),
                yaml.getBoolean(path + ".permanent", false),
                yaml.getLong(path + ".timestamp", 0L),
                yaml.getLong(path + ".expiresAt", 0L)
            ));
        }

        result.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        return result;
    }

    /**
     * Returns all history entries made by a given staff name (case-insensitive).
     */
    public List<HistoryEntry> getStaffHistoryByName(String staffName) {
        List<HistoryEntry> result = new ArrayList<>();
        if (staffName == null || staffName.isBlank()) return result;

        var section = yaml.getConfigurationSection("history");
        if (section == null) return result;

        String search = staffName.toLowerCase();

        for (String key : section.getKeys(false)) {
            String path = "history." + key;
            String storedStaff = yaml.getString(path + ".staffName", "");
            if (!storedStaff.toLowerCase().equals(search)) continue;

            result.add(new HistoryEntry(
                yaml.getString(path + ".targetUuid", ""),
                yaml.getString(path + ".targetName", "Unknown"),
                yaml.getString(path + ".staffUuid", "CONSOLE"),
                storedStaff,
                yaml.getString(path + ".group", "?"),
                yaml.getString(path + ".duration", "?"),
                yaml.getBoolean(path + ".permanent", false),
                yaml.getLong(path + ".timestamp", 0L),
                yaml.getLong(path + ".expiresAt", 0L)
            ));
        }

        result.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        return result;
    }

    // ─────────────────────────────────────────────────────────
    //   DTO
    // ─────────────────────────────────────────────────────────

    /**
     * Immutable snapshot of a single history entry.
     */
    public record HistoryEntry(
            String targetUuid,
            String targetName,
            String staffUuid,
            String staffName,
            String group,
            String duration,
            boolean permanent,
            long timestamp,
            long expiresAt
    ) {
        /** Formatted date string (dd/MM/yyyy HH:mm). */
        public String formattedDate() {
            return DATE_FMT.format(Instant.ofEpochSecond(timestamp));
        }

        /**
         * Whether this grant is currently active.
         * Permanent grants are always active.
         * Temporary grants are active if expiresAt > now.
         */
        public boolean isActive() {
            if (permanent) return true;
            return expiresAt > Instant.now().getEpochSecond();
        }

        /** Human-readable remaining time, or "Expired" / "Permanent". */
        public String statusDisplay() {
            if (permanent) return "&aPermanent";
            long remaining = expiresAt - Instant.now().getEpochSecond();
            if (remaining <= 0) return "&cExpired";
            long d = remaining / 86400;
            long h = (remaining % 86400) / 3600;
            long m = (remaining % 3600) / 60;
            StringBuilder sb = new StringBuilder("&aActive &7(");
            if (d > 0) sb.append(d).append("d ");
            if (h > 0) sb.append(h).append("h ");
            if (m > 0 || (d == 0 && h == 0)) sb.append(Math.max(1, m)).append("m");
            sb.append("&7)");
            return sb.toString().trim();
        }
    }
}
