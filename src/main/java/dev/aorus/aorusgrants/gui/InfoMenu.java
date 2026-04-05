package dev.aorus.aorusgrants.gui;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.managers.LPExecutor;
import dev.aorus.aorusgrants.model.GrantSession;
import dev.aorus.aorusgrants.utils.ColorUtil;
import dev.aorus.aorusgrants.utils.ItemBuilder;
import dev.aorus.aorusgrants.utils.MenuUtils;
import dev.aorus.aorusgrants.utils.SkullUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * MENU — INFO (54 slots)
 *
 * Layout:
 *   Slot 4   → Player head (Adventure API lore — correct HEX prefix display)
 *   Slots 0,8,45,53 → Levers
 *   Slots contenido → player's groups
 *   Slot 49  → Redstone back
 */
public class InfoMenu {

    private static final String SEP       = "&8&m------------------------------";
    private static final int    HEAD_SLOT = 4;
    private static final int    BACK_SLOT = 49;
    private static final int[]  CORNERS   = {0, 8, 45, 53};

    private static final List<Integer> CONTENT_SLOTS = List.of(
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    );

    private final AorusGrants plugin;

    public InfoMenu(AorusGrants plugin) {
        this.plugin = plugin;
    }

    public void open(Player staff, GrantSession session) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("info-menu");

        String title    = (cfg != null) ? cfg.getString("title", "&8» &bPlayer Groups &8«")
                                        : "&8» &bPlayer Groups &8«";
        int    size     = (cfg != null) ? cfg.getInt("size", 54) : 54;
        int    backSlot = (cfg != null) ? cfg.getInt("back-button.slot", BACK_SLOT) : BACK_SLOT;

        List<Integer> groupSlots = (cfg != null && !cfg.getIntegerList("group-slots").isEmpty())
                ? cfg.getIntegerList("group-slots") : CONTENT_SLOTS;

        final int            finalBack  = backSlot;
        final List<Integer>  finalSlots = groupSlots;
        final String         targetName = session.getTargetName();

        // Fetch prefix + groups async, build on main thread
        plugin.getLpExecutor().getPlayerPrefix(session.getTargetUUID())
              .thenAccept((String prefix) ->
                  plugin.getLpExecutor().getPlayerGroups(session.getTargetUUID())
                        .thenAccept((List<LPExecutor.GroupInfo> groups) ->
                            plugin.getServer().getScheduler().runTask(plugin, () -> {

                                Inventory inv = MenuUtils.createInventory(title, size);

                                // Levers on corners
                                for (int corner : CORNERS) {
                                    inv.setItem(corner, new ItemBuilder(Material.LEVER)
                                            .name(" ").hideFlags().build());
                                }

                                // Back button
                                inv.setItem(finalBack, new ItemBuilder(Material.REDSTONE)
                                    .name("&c◀ Back")
                                    .lore(SEP, "&7Return to main menu.", SEP)
                                    .build());

                                // ── Player head — Adventure API lore ──────────────
                                // Build group list for lore (group names only, plain)
                                StringBuilder sbGroups = new StringBuilder();
                                for (LPExecutor.GroupInfo g : groups) {
                                    if (sbGroups.length() > 0) sbGroups.append(", ");
                                    sbGroups.append(capitalize(g.name()));
                                }
                                String groupListStr = sbGroups.length() > 0
                                        ? sbGroups.toString() : "None";

                                // LP prefix: § → & so legacyAmpersand serializer handles it
                                String prefixForAdventure = prefix.isEmpty()
                                        ? "&7None"
                                        : ColorUtil.sectionToAmpersand(prefix).stripTrailing();

                                ItemStack headItem = SkullUtils.getPlayerHead(
                                        targetName, targetName, new ArrayList<>());
                                ItemMeta headMeta = headItem.getItemMeta();
                                if (headMeta != null) {
                                    var ser = plugin.getLpExecutor().getAdventureSerializer();

                                    // Display name — no italic
                                    Component headName = ser.deserialize("&b" + targetName)
                                            .decoration(TextDecoration.ITALIC, false);

                                    // Lore lines — each de-italicized, prefix renders HEX correctly
                                    List<Component> loreComps = new ArrayList<>();
                                    loreComps.add(ser.deserialize(SEP)
                                            .decoration(TextDecoration.ITALIC, false));
                                    loreComps.add(ser.deserialize("&7Player: &f" + targetName)
                                            .decoration(TextDecoration.ITALIC, false));
                                    loreComps.add(ser.deserialize("&7Prefix: " + prefixForAdventure)
                                            .decoration(TextDecoration.ITALIC, false));
                                    loreComps.add(ser.deserialize("&7Groups: &f" + groupListStr)
                                            .decoration(TextDecoration.ITALIC, false));
                                    loreComps.add(ser.deserialize(SEP)
                                            .decoration(TextDecoration.ITALIC, false));

                                    headMeta.displayName(headName);
                                    headMeta.lore(loreComps);
                                    headItem.setItemMeta(headMeta);
                                }
                                inv.setItem(HEAD_SLOT, headItem);

                                // ── Player's groups ────────────────────────────────
                                if (groups.isEmpty()) {
                                    int slot = finalSlots.isEmpty() ? 31 : finalSlots.get(0);
                                    inv.setItem(slot, new ItemBuilder(Material.BARRIER)
                                        .name("&cNo Groups")
                                        .lore(SEP, "&7This player has no groups.", SEP)
                                        .build());
                                } else {
                                    for (int i = 0; i < Math.min(groups.size(), finalSlots.size()); i++) {
                                        inv.setItem(finalSlots.get(i), buildGroupItem(groups.get(i)));
                                    }
                                }

                                staff.openInventory(inv);
                            })
                        )
              );
    }

    // ─────────────────────────────────────────────────────────
    //   GROUP ITEM — display name via Adventure (HEX-safe prefix)
    // ─────────────────────────────────────────────────────────

    private ItemStack buildGroupItem(LPExecutor.GroupInfo info) {
        String displayName = capitalize(info.name());

        // § → & so the ampersand serializer handles HEX correctly
        String prefixRaw = info.prefix().stripTrailing();
        String prefixForAdventure = prefixRaw.isEmpty()
                ? "&7" + displayName
                : ColorUtil.sectionToAmpersand(prefixRaw);

        // Non-prefix lore lines (plain strings, go through ItemBuilder → ColorUtil.color())
        List<String> plainLore = new ArrayList<>();
        plainLore.add(SEP);
        plainLore.add("&7Group:  &f" + displayName);
        plainLore.add("&7Type:   " + (info.temporal() ? "&eTemporary" : "&aPermanent"));
        if (info.temporal()) plainLore.add("&7Duration: &f" + info.duration());
        plainLore.add(SEP);

        // Build item — set display name via Adventure, lore via legacy (no prefix in lore = safe)
        ItemStack item = new ItemBuilder(Material.BOOK).lore(plainLore).build();
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            var ser = plugin.getLpExecutor().getAdventureSerializer();
            Component nameComp = ser.deserialize(prefixForAdventure)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(nameComp);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isInfoMenu(String title) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("info-menu");
        if (cfg == null) return false;
        return ColorUtil.color(cfg.getString("title", "")).equals(title);
    }

    public int getBackSlot() {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("info-menu");
        return (cfg != null) ? cfg.getInt("back-button.slot", BACK_SLOT) : BACK_SLOT;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
