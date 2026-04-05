package dev.aorus.aorusgrants.gui;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.model.GrantSession;
import dev.aorus.aorusgrants.utils.ColorUtil;
import dev.aorus.aorusgrants.utils.ItemBuilder;
import dev.aorus.aorusgrants.utils.MenuUtils;
import dev.aorus.aorusgrants.utils.SkullUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MENU 4 — CONFIRM (27 slots)
 * Layout:
 *   Slot 13 → Player Head (center)
 *   Slot 11 → Confirm (Green Wool)
 *   Slot 15 → Cancel  (Red Wool)
 *   NO palancas, NO filler
 */
public class ConfirmMenu {

    private final AorusGrants plugin;

    public ConfirmMenu(AorusGrants plugin) {
        this.plugin = plugin;
    }

    public void open(Player staff, GrantSession session) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("confirm-menu");
        if (cfg == null) return;

        String title = cfg.getString("title", "&8» &bConfirmar &8«");
        int size = cfg.getInt("size", 27);

        Inventory inv = MenuUtils.createInventory(title, size);

        String targetName = session.getTargetName();
        String group    = session.getSelectedGroup() != null ? session.getSelectedGroup() : "N/A";
        String action   = session.getAction() != null ? session.getAction().name() : "N/A";
        String duration = session.getDurationDisplay();

        // ── Player head ────────────────────────────────────────
        ConfigurationSection headCfg = cfg.getConfigurationSection("player-head");
        int headSlot = headCfg != null ? headCfg.getInt("slot", 13) : 13;
        String headName = (headCfg != null ? headCfg.getString("name", "&b{player}") : "&b{player}")
                .replace("{player}", targetName);
        List<String> headLore = (headCfg != null ? headCfg.getStringList("lore") : List.of(
            "&8&m------------------------------",
            "&7Jugador: &f{player}",
            "&7Grupo: &f{group}",
            "&7Acción: &f{action}",
            "&7Duración: &f{duration}",
            "&8&m------------------------------"
        )).stream()
            .map((String l) -> l.replace("{player}", targetName)
                       .replace("{group}", group)
                       .replace("{action}", action)
                       .replace("{duration}", duration))
            .collect(java.util.stream.Collectors.toList());

        inv.setItem(headSlot, SkullUtils.getPlayerHead(targetName, headName, headLore));

        // ── Confirm button ─────────────────────────────────────
        ConfigurationSection confirmCfg = cfg.getConfigurationSection("confirm-button");
        int confirmSlot = confirmCfg != null ? confirmCfg.getInt("slot", 11) : 11;
        String confirmName = confirmCfg != null ? confirmCfg.getString("name", "&a&l✔ CONFIRMAR") : "&a&l✔ CONFIRMAR";
        List<String> confirmLore = confirmCfg != null ? confirmCfg.getStringList("lore") : List.of("&7Click para ejecutar.");
        String confirmMat = confirmCfg != null ? confirmCfg.getString("material", "GREEN_WOOL") : "GREEN_WOOL";

        try {
            inv.setItem(confirmSlot, new ItemBuilder(Material.valueOf(confirmMat))
                .name(confirmName).lore(confirmLore).build());
        } catch (IllegalArgumentException e) {
            inv.setItem(confirmSlot, new ItemBuilder(Material.GREEN_WOOL)
                .name(confirmName).lore(confirmLore).build());
        }

        // ── Cancel button ──────────────────────────────────────
        ConfigurationSection cancelCfg = cfg.getConfigurationSection("cancel-button");
        int cancelSlot = cancelCfg != null ? cancelCfg.getInt("slot", 15) : 15;
        String cancelName = cancelCfg != null ? cancelCfg.getString("name", "&c&l✘ CANCELAR") : "&c&l✘ CANCELAR";
        List<String> cancelLore = cancelCfg != null ? cancelCfg.getStringList("lore") : List.of("&7Click para cancelar.");
        String cancelMat = cancelCfg != null ? cancelCfg.getString("material", "RED_WOOL") : "RED_WOOL";

        try {
            inv.setItem(cancelSlot, new ItemBuilder(Material.valueOf(cancelMat))
                .name(cancelName).lore(cancelLore).build());
        } catch (IllegalArgumentException e) {
            inv.setItem(cancelSlot, new ItemBuilder(Material.RED_WOOL)
                .name(cancelName).lore(cancelLore).build());
        }

        staff.openInventory(inv);
    }

    public boolean isConfirmMenu(String title) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("confirm-menu");
        if (cfg == null) return false;
        return ColorUtil.color(cfg.getString("title", "")).equals(title);
    }

    public int getConfirmSlot() {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("confirm-menu");
        ConfigurationSection c = cfg != null ? cfg.getConfigurationSection("confirm-button") : null;
        return c != null ? c.getInt("slot", 11) : 11;
    }

    public int getCancelSlot() {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("confirm-menu");
        ConfigurationSection c = cfg != null ? cfg.getConfigurationSection("cancel-button") : null;
        return c != null ? c.getInt("slot", 15) : 15;
    }
}
