package dev.aorus.aorusgrants.managers;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.model.GroupData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GroupManager {

    private final AorusGrants plugin;
    // Optional config overrides (type classification only — material is always WHITE_WOOL default now)
    private final Map<String, GroupData> configOverrides = new LinkedHashMap<>();

    public GroupManager(AorusGrants plugin) {
        this.plugin = plugin;
        loadConfigOverrides();
    }

    private void loadConfigOverrides() {
        configOverrides.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("groups");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".display-name", key);
            String prefix      = section.getString(key + ".prefix", "");
            int weight         = section.getInt(key + ".weight", 0);
            String typeStr     = section.getString(key + ".type", "DEFAULT");

            // Material from config only used if no custom item is set via /agadmin setitem
            // Default is always WHITE_WOOL — custom items stored in GroupItemStorage
            String materialStr = section.getString(key + ".material", "WHITE_WOOL");

            GroupData.GroupType type;
            try { type = GroupData.GroupType.valueOf(typeStr.toUpperCase()); }
            catch (IllegalArgumentException e) { type = GroupData.GroupType.DEFAULT; }

            Material material;
            try { material = Material.valueOf(materialStr.toUpperCase()); }
            catch (IllegalArgumentException e) { material = Material.WHITE_WOOL; }

            configOverrides.put(key.toLowerCase(), new GroupData(key, displayName, prefix, weight, type, material));
        }
        plugin.getLogger().info("[GroupManager] Loaded " + configOverrides.size() + " config overrides.");
    }

    public void reload() {
        loadConfigOverrides();
    }

    /**
     * Fetch ALL groups from LP, sorted by weight DESC.
     * Item = WHITE_WOOL by default. If /agadmin setitem was used, that item takes priority.
     * Name of item in GUI = LP prefix of the group.
     */
    public CompletableFuture<List<GroupData>> getLPGroupsSortedAsync() {
        return CompletableFuture.supplyAsync(() ->
            plugin.getLuckPerms().getGroupManager().getLoadedGroups().stream()
                .map(this::buildGroupData)
                .sorted(Comparator.comparingInt(GroupData::getWeight).reversed())
                .collect(Collectors.toList())
        );
    }

    /**
     * Build GroupData for a LP group.
     * - name   = item display name → LP prefix of group (e.g. "[Admin]")
     * - material = WHITE_WOOL (unless overridden by /agadmin setitem, handled at render time)
     * - type   = from config.yml override if present, else DEFAULT
     */
    public GroupData buildGroupData(Group lpGroup) {
        String id       = lpGroup.getName().toLowerCase();
        int lpWeight    = lpGroup.getWeight().orElse(0);

        // Always read prefix from LP live
        String lpPrefix = "";
        var meta = lpGroup.getCachedData().getMetaData(QueryOptions.nonContextual());
        if (meta.getPrefix() != null) lpPrefix = meta.getPrefix();

        // Apply config override for type/displayName if present
        if (configOverrides.containsKey(id)) {
            GroupData override = configOverrides.get(id);
            String prefix = lpPrefix.isEmpty() ? override.getPrefix() : lpPrefix;
            String displayName = override.getDisplayName();
            return new GroupData(id, displayName, prefix, lpWeight, override.getType(), Material.WHITE_WOOL);
        }

        // Auto-detected group (not in config) — all defaults
        String displayName = capitalize(id);
        String prefix = lpPrefix.isEmpty() ? "&7" + displayName + " &r" : lpPrefix;
        return new GroupData(id, displayName, prefix, lpWeight, GroupData.GroupType.DEFAULT, Material.WHITE_WOOL);
    }

    public Map<String, GroupData> getGroups() {
        return Collections.unmodifiableMap(configOverrides);
    }

    public GroupData getGroup(String id) {
        GroupData cfg = configOverrides.get(id.toLowerCase());
        if (cfg != null) return cfg;
        Group lpGroup = plugin.getLuckPerms().getGroupManager().getGroup(id.toLowerCase());
        return lpGroup != null ? buildGroupData(lpGroup) : null;
    }

    public boolean existsInLP(String id) {
        return plugin.getLuckPerms().getGroupManager().getGroup(id.toLowerCase()) != null;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
