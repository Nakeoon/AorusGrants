package dev.aorus.aorusgrants.gui;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.managers.ConfigManager;
import dev.aorus.aorusgrants.model.GrantSession;
import dev.aorus.aorusgrants.utils.ColorUtil;
import dev.aorus.aorusgrants.utils.ItemBuilder;
import dev.aorus.aorusgrants.utils.MenuUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MENU 3 — DURATION (54 slots)
 *
 * Layout (4 items per row):
 *   Días    (Verde)    → slots 10,11,12,13
 *   Semanas (Amarillo) → slots 19,20,21,22
 *   Meses   (Naranja)  → slots 28,29,30,31
 *   Permanente         → slot 25
 *   Confirmar          → slot 26
 *   Nav: ← (48 deco) | redstone (49 back) | → (50 deco)
 */
public class DurationMenu {

    private static final int PERM_SLOT    = 24;
    private static final int CONFIRM_SLOT = 25;
    private static final int NAV_PREV     = 48;
    private static final int NAV_BACK     = 49;
    private static final int NAV_NEXT     = 50;

    private static final int[] CORNERS = {0, 8, 45, 53};

    private final AorusGrants plugin;

    public DurationMenu(AorusGrants plugin) {
        this.plugin = plugin;
    }

    public void open(Player staff, GrantSession session) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("duration-menu");
        if (cfg == null) return;

        String title = cfg.getString("title", "&8» &bSeleccionar Duración &8«");
        int size     = cfg.getInt("size", 54);

        Inventory inv = MenuUtils.createInventory(title, size);

        // Levers on corners
        for (int corner : CORNERS) {
            inv.setItem(corner, new ItemBuilder(Material.LEVER).name(" ").hideFlags().build());
        }

        buildSection(inv, cfg.getConfigurationSection("days"));
        buildSection(inv, cfg.getConfigurationSection("weeks"));
        buildSection(inv, cfg.getConfigurationSection("months"));
        placePermanent(inv, cfg);
        refreshConfirmButton(inv, cfg, session);
        placeNavBar(inv);

        staff.openInventory(inv);
    }

    private void buildSection(Inventory inv, ConfigurationSection section) {
        if (section == null) return;

        String mat          = section.getString("material", "WHITE_WOOL");
        String nameTemplate = section.getString("name-template", "{amount}");
        List<String> loreTpl = section.getStringList("lore-template");

        Material material;
        try { material = Material.valueOf(mat); }
        catch (IllegalArgumentException e) { material = Material.WHITE_WOOL; }

        for (Map<?, ?> entry : section.getMapList("slots")) {
            int slot   = (int) entry.get("slot");
            int amount = (int) entry.get("amount");

            String itemName = nameTemplate.replace("{amount}", String.valueOf(amount));

            // Explicit (String line) type avoids capture-of-extends-Object errors
            List<String> lore = loreTpl.stream()
                    .map((String line) -> line.replace("{amount}", String.valueOf(amount)))
                    .collect(Collectors.toList());

            inv.setItem(slot, new ItemBuilder(material).name(itemName).lore(lore).build());
        }
    }

    private void placePermanent(Inventory inv, ConfigurationSection cfg) {
        ConfigurationSection permCfg = cfg.getConfigurationSection("permanent");
        int    slot = (permCfg != null) ? permCfg.getInt("slot", PERM_SLOT) : PERM_SLOT;
        String mat  = (permCfg != null) ? permCfg.getString("material", "RED_WOOL") : "RED_WOOL";
        String name = (permCfg != null) ? permCfg.getString("name", "&c&lPERMANENTE") : "&c&lPERMANENTE";
        List<String> lore = (permCfg != null)
                ? permCfg.getStringList("lore")
                : List.of("&8&m------------------------------", "&7Sobrescribe toda duración.", "&8&m------------------------------");

        Material material;
        try { material = Material.valueOf(mat); }
        catch (IllegalArgumentException e) { material = Material.RED_WOOL; }

        inv.setItem(slot, new ItemBuilder(material).name(name).lore(lore).build());
    }

    public void refreshConfirmButton(Inventory inv, ConfigurationSection cfg, GrantSession session) {
        ConfigurationSection confirmCfg = (cfg != null) ? cfg.getConfigurationSection("confirm-button") : null;
        int    slot = (confirmCfg != null) ? confirmCfg.getInt("slot", CONFIRM_SLOT) : CONFIRM_SLOT;
        String mat  = (confirmCfg != null) ? confirmCfg.getString("material", "NETHER_STAR") : "NETHER_STAR";
        String name = (confirmCfg != null) ? confirmCfg.getString("name", "&a&lCONFIRMAR") : "&a&lCONFIRMAR";

        List<String> rawLore = (confirmCfg != null)
                ? confirmCfg.getStringList("lore")
                : List.of("&8&m------------------------------", "&7Duración actual:", "&f{duration}", "", "&aClick para continuar.", "&8&m------------------------------");

        String durationDisplay = session.getDurationDisplay();
        List<String> lore = rawLore.stream()
                .map((String line) -> line.replace("{duration}", durationDisplay))
                .collect(Collectors.toList());

        Material material;
        try { material = Material.valueOf(mat); }
        catch (IllegalArgumentException e) { material = Material.NETHER_STAR; }

        inv.setItem(slot, new ItemBuilder(material).name(name).lore(lore).build());
    }

    private void placeNavBar(Inventory inv) {
        inv.setItem(NAV_PREV, new ItemBuilder(Material.ARROW)
            .name("&8«").lore("&8&m------------------------------", "&8No previous page.", "&8&m------------------------------").build());
        inv.setItem(NAV_BACK, new ItemBuilder(Material.REDSTONE)
            .name("&c◀ Back").lore("&8&m------------------------------", "&7Return to rank selection.", "&8&m------------------------------").build());
        inv.setItem(NAV_NEXT, new ItemBuilder(Material.ARROW)
            .name("&8»").lore("&8&m------------------------------", "&8No next page.", "&8&m------------------------------").build());
    }

    public boolean isDurationMenu(String title) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("duration-menu");
        if (cfg == null) return false;
        return ColorUtil.color(cfg.getString("title", "")).equals(title);
    }

    /**
     * Returns [type, amount]: 0=days, 1=weeks, 2=months, 3=permanent.
     * Returns null if slot is not a time button.
     */
    public int[] getTimeClickData(int slot) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("duration-menu");
        if (cfg == null) return null;

        ConfigurationSection perm = cfg.getConfigurationSection("permanent");
        int permSlot = (perm != null) ? perm.getInt("slot", PERM_SLOT) : PERM_SLOT;
        if (slot == permSlot) return new int[]{3, 0};

        String[] sections = {"days", "weeks", "months"};
        for (int t = 0; t < sections.length; t++) {
            ConfigurationSection sec = cfg.getConfigurationSection(sections[t]);
            if (sec == null) continue;
            for (Map<?, ?> entry : sec.getMapList("slots")) {
                if ((int) entry.get("slot") == slot) return new int[]{t, (int) entry.get("amount")};
            }
        }
        return null;
    }

    public int getConfirmSlot() {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("duration-menu");
        if (cfg == null) return CONFIRM_SLOT;
        ConfigurationSection c = cfg.getConfigurationSection("confirm-button");
        return (c != null) ? c.getInt("slot", CONFIRM_SLOT) : CONFIRM_SLOT;
    }

    public int getBackSlot()    { return NAV_BACK; }
    public int getNavPrevSlot() { return NAV_PREV; }
    public int getNavNextSlot() { return NAV_NEXT; }

    public ConfigurationSection getConfig() {
        return plugin.getConfigManager().getMenusConfig().getConfigurationSection("duration-menu");
    }
}
