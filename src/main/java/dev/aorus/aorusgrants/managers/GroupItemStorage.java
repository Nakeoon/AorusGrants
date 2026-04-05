package dev.aorus.aorusgrants.managers;

import dev.aorus.aorusgrants.AorusGrants;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists custom display items for groups set via /agadmin setitem <grupo>.
 * Saves to plugins/AorusGrants/group-items.yml.
 *
 * Stored per group:
 *   material, custom-model-data, name, lore
 */
public class GroupItemStorage {

    private final AorusGrants plugin;
    private final File file;
    private FileConfiguration config;

    // In-memory cache: groupId (lowercase) → custom ItemStack
    private final Map<String, ItemStack> cache = new HashMap<>();

    public GroupItemStorage(AorusGrants plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "group-items.yml");
        load();
    }

    // ─────────────────────────────────────────────────────────
    //   Load
    // ─────────────────────────────────────────────────────────

    public void load() {
        cache.clear();
        if (!file.exists()) return;

        config = YamlConfiguration.loadConfiguration(file);

        for (String groupId : config.getKeys(false)) {
            String matStr = config.getString(groupId + ".material", "WHITE_WOOL");
            int cmd = config.getInt(groupId + ".custom-model-data", 0);
            String name = config.getString(groupId + ".name", null);

            Material mat;
            try { mat = Material.valueOf(matStr.toUpperCase()); }
            catch (IllegalArgumentException e) { mat = Material.WHITE_WOOL; }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (cmd > 0) meta.setCustomModelData(cmd);
                if (name != null) meta.setDisplayName(name);

                if (config.isList(groupId + ".lore")) {
                    meta.setLore(config.getStringList(groupId + ".lore"));
                }
                item.setItemMeta(meta);
            }

            cache.put(groupId.toLowerCase(), item);
        }

        plugin.getLogger().info("[GroupItemStorage] Loaded " + cache.size() + " custom group items.");
    }

    // ─────────────────────────────────────────────────────────
    //   Save
    // ─────────────────────────────────────────────────────────

    public void saveItem(String groupId, ItemStack item) {
        groupId = groupId.toLowerCase();
        cache.put(groupId, item.clone());

        if (config == null) config = YamlConfiguration.loadConfiguration(file);

        ItemMeta meta = item.getItemMeta();

        config.set(groupId + ".material", item.getType().name());

        int cmd = (meta != null && meta.hasCustomModelData()) ? meta.getCustomModelData() : 0;
        config.set(groupId + ".custom-model-data", cmd);

        String displayName = (meta != null && meta.hasDisplayName()) ? meta.getDisplayName() : null;
        config.set(groupId + ".name", displayName);

        if (meta != null && meta.hasLore()) {
            config.set(groupId + ".lore", meta.getLore());
        } else {
            config.set(groupId + ".lore", null);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[GroupItemStorage] Failed to save group-items.yml: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    //   Get
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the custom ItemStack for a group, or null if no custom item is set.
     * The returned stack has the group's prefix as display name if a name isn't already set.
     */
    public ItemStack getItem(String groupId, String prefixFallback) {
        ItemStack base = cache.get(groupId.toLowerCase());
        if (base == null) return null;

        ItemStack result = base.clone();
        ItemMeta meta = result.getItemMeta();
        // If saved without a name, use the prefix
        if (meta != null && !meta.hasDisplayName() && prefixFallback != null) {
            meta.setDisplayName(prefixFallback);
            result.setItemMeta(meta);
        }
        return result;
    }

    public boolean hasCustomItem(String groupId) {
        return cache.containsKey(groupId.toLowerCase());
    }

    public void reload() {
        load();
    }
}
