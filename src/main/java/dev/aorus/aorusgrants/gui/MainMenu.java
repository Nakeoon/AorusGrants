package dev.aorus.aorusgrants.gui;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.model.GrantSession;
import dev.aorus.aorusgrants.utils.ColorUtil;
import dev.aorus.aorusgrants.utils.ItemBuilder;
import dev.aorus.aorusgrants.utils.MenuUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MENU 1 — MAIN MENU (27 slots)
 *
 *   Slot 13 → BOOK  (center — click opens InfoMenu)
 *   Slot 11 → PROMOTE (green wool)
 *   Slot 15 → DEMOTE  (red wool)
 *
 *   No levers. No head. No info button — book IS the info button.
 */
public class MainMenu {

    private final AorusGrants plugin;

    public MainMenu(AorusGrants plugin) {
        this.plugin = plugin;
    }

    public void open(Player staff, GrantSession session) {
        ConfigurationSection cfg = plugin.getConfigManager()
                .getMenusConfig().getConfigurationSection("main-menu");
        if (cfg == null) return;

        String title = cfg.getString("title", "&8» &bAorusGrants &8«");
        int    size  = cfg.getInt("size", 27);

        Inventory inv = MenuUtils.createInventory(title, size);

        String targetName = session.getTargetName();

        // ── Center book — clicking opens the player's group info ──
        ConfigurationSection bookCfg = cfg.getConfigurationSection("player-book");
        int    bookSlot = (bookCfg != null) ? bookCfg.getInt("slot", 13) : 13;
        String bookName = ((bookCfg != null)
                ? bookCfg.getString("name", "&b{player}") : "&b{player}")
                .replace("{player}", targetName);

        List<String> bookLore = ((bookCfg != null)
                ? bookCfg.getStringList("lore")
                : List.of(
                    "&8&m------------------------------",
                    "&7Player: &f{player}",
                    "",
                    "&eClick to view groups.",
                    "&8&m------------------------------"))
                .stream()
                .map((String l) -> l.replace("{player}", targetName))
                .collect(Collectors.toList());

        inv.setItem(bookSlot, new ItemBuilder(Material.BOOK)
                .name(bookName).lore(bookLore).build());

        // ── Promote / Demote buttons ───────────────────────────
        placeButton(inv, cfg.getConfigurationSection("promote-button"), targetName);
        placeButton(inv, cfg.getConfigurationSection("demote-button"),  targetName);

        staff.openInventory(inv);
    }

    private void placeButton(Inventory inv, ConfigurationSection section, String targetName) {
        if (section == null) return;
        int    slot = section.getInt("slot");
        String mat  = section.getString("material", "WHITE_WOOL");
        String name = section.getString("name", "").replace("{player}", targetName);

        List<String> lore = section.getStringList("lore").stream()
                .map((String l) -> l.replace("{player}", targetName))
                .collect(Collectors.toList());

        try {
            inv.setItem(slot, new ItemBuilder(Material.valueOf(mat))
                    .name(name).lore(lore).build());
        } catch (IllegalArgumentException ignored) {}
    }

    public boolean isMainMenu(String title) {
        ConfigurationSection cfg = plugin.getConfigManager()
                .getMenusConfig().getConfigurationSection("main-menu");
        if (cfg == null) return false;
        return ColorUtil.color(cfg.getString("title", "")).equals(title);
    }

    /** Slot of the center book — used by MenuListener to detect book clicks. */
    public int getBookSlot() {
        ConfigurationSection cfg = plugin.getConfigManager()
                .getMenusConfig().getConfigurationSection("main-menu");
        ConfigurationSection bookCfg = (cfg != null) ? cfg.getConfigurationSection("player-book") : null;
        return (bookCfg != null) ? bookCfg.getInt("slot", 13) : 13;
    }
}
