package dev.aorus.aorusgrants.gui;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.managers.LPExecutor;
import dev.aorus.aorusgrants.model.GrantSession;
import dev.aorus.aorusgrants.model.GroupData;
import dev.aorus.aorusgrants.utils.ColorUtil;
import dev.aorus.aorusgrants.utils.ItemBuilder;
import dev.aorus.aorusgrants.utils.MenuUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MENU — DEMOTE SELECT (54 slots, paginado)
 *
 * Display name del ítem usa Adventure API (igual que RankSelectMenu)
 * para que los prefijos HEX de LuckPerms se rendericen correctamente.
 */
public class DemoteMenu {

    private static final String SEP = "&8&m------------------------------";

    private static final List<Integer> CONTENT_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.size();
    private static final int[] CORNERS = {0, 8, 45, 53};
    private static final int NAV_PREV = 48;
    private static final int NAV_BACK = 49;
    private static final int NAV_NEXT = 50;

    private final AorusGrants plugin;
    private final Map<Integer, String> slotGroupMap = new HashMap<>();
    private int currentPage = 0;

    public DemoteMenu(AorusGrants plugin) {
        this.plugin = plugin;
    }

    public void open(Player staff, GrantSession session) {
        open(staff, session, 0);
    }

    public void open(Player staff, GrantSession session, int page) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("demote-menu");

        String title = (cfg != null) ? cfg.getString("title", "&8» &cRemove Rank &8«")
                                     : "&8» &cRemove Rank &8«";
        int size     = (cfg != null) ? cfg.getInt("size", 54) : 54;

        slotGroupMap.clear();

        plugin.getLpExecutor().getPlayerGroups(session.getTargetUUID()).thenAccept(groups ->
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Inventory inv = MenuUtils.createInventory(title, size);

                for (int corner : CORNERS) {
                    inv.setItem(corner, new ItemBuilder(Material.LEVER).name(" ").hideFlags().build());
                }

                int totalPages = Math.max(1, (int) Math.ceil((double) groups.size() / ITEMS_PER_PAGE));
                int cp         = Math.max(0, Math.min(page, totalPages - 1));
                currentPage    = cp;

                int startIdx = cp * ITEMS_PER_PAGE;
                int endIdx   = Math.min(startIdx + ITEMS_PER_PAGE, groups.size());

                if (groups.isEmpty()) {
                    inv.setItem(31, new ItemBuilder(Material.BARRIER)
                        .name("&cNo Groups")
                        .lore(SEP, "&7This player has no groups to remove.", SEP)
                        .build());
                } else {
                    List<LPExecutor.GroupInfo> pageGroups = groups.subList(startIdx, endIdx);
                    for (int i = 0; i < pageGroups.size(); i++) {
                        LPExecutor.GroupInfo info = pageGroups.get(i);
                        int slot = CONTENT_SLOTS.get(i);
                        slotGroupMap.put(slot, info.name());
                        inv.setItem(slot, buildGroupItem(info));
                    }
                }

                placeNavBar(inv, cp, totalPages);
                staff.openInventory(inv);
            })
        );
    }

    // ─────────────────────────────────────────────────────────
    //   ITEM — display name via Adventure API (HEX-safe)
    //   Mismo patrón que RankSelectMenu.buildGroupItem()
    // ─────────────────────────────────────────────────────────
    private ItemStack buildGroupItem(LPExecutor.GroupInfo info) {
        String displayName = capitalize(info.name());

        // § → & para que LegacyComponentSerializer.legacyAmpersand() interprete HEX
        String rawPrefix = info.prefix().stripTrailing();
        String prefixForAdventure = rawPrefix.isEmpty()
                ? "&f" + displayName
                : ColorUtil.sectionToAmpersand(rawPrefix);

        // Lore: strings sin prefix LP — seguro usar legacy ColorUtil.color()
        List<String> plainLore = new ArrayList<>();
        plainLore.add(SEP);
        plainLore.add("&7Group:  &f" + displayName);
        plainLore.add("&7Type:   " + (info.temporal() ? "&eTemporary" : "&aPermanent"));
        if (info.temporal()) plainLore.add("&7Expires: &f" + info.duration());
        plainLore.add(SEP);
        plainLore.add("");
        plainLore.add("&cClick to remove this group.");

        // Resolver material base (custom item o WHITE_WOOL)
        ItemStack item;
        if (plugin.getGroupItemStorage().hasCustomItem(info.name())) {
            ItemStack custom = plugin.getGroupItemStorage().getItem(info.name(), "");
            item = (custom != null) ? custom.clone() : new ItemStack(Material.WHITE_WOOL);
        } else {
            Material mat = Material.WHITE_WOOL;
            GroupData gd = plugin.getGroupManager().getGroup(info.name());
            if (gd != null) mat = gd.getMaterial();
            item = new ItemStack(mat);
        }

        // Aplicar display name con Adventure (respeta HEX), lore con legacy
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var ser = plugin.getLpExecutor().getAdventureSerializer();
            Component nameComp = ser.deserialize(prefixForAdventure)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(nameComp);
            meta.setLore(plainLore.stream().map(ColorUtil::color).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void placeNavBar(Inventory inv, int page, int totalPages) {
        boolean hasPrev = page > 0;
        boolean hasNext = page < totalPages - 1;

        inv.setItem(NAV_PREV, hasPrev
            ? new ItemBuilder(Material.ARROW)
                .name("&e« Previous")
                .lore(SEP, "&7Page " + (page + 1) + " / " + totalPages, SEP)
                .build()
            : new ItemBuilder(Material.ARROW)
                .name("&8« Previous")
                .lore(SEP, "&8First page.", SEP)
                .build());

        inv.setItem(NAV_BACK, new ItemBuilder(Material.REDSTONE)
            .name("&c◀ Back")
            .lore(SEP, "&7Return to main menu.", SEP)
            .build());

        inv.setItem(NAV_NEXT, hasNext
            ? new ItemBuilder(Material.ARROW)
                .name("&eNext »")
                .lore(SEP, "&7Page " + (page + 2) + " / " + totalPages, SEP)
                .build()
            : new ItemBuilder(Material.ARROW)
                .name("&8Next »")
                .lore(SEP, "&8Last page.", SEP)
                .build());
    }

    public boolean isDemoteMenu(String title) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("demote-menu");
        String cfgTitle = (cfg != null)
            ? ColorUtil.color(cfg.getString("title", "&8» &cRemove Rank &8«"))
            : ColorUtil.color("&8» &cRemove Rank &8«");
        return cfgTitle.equals(title);
    }

    public String getGroupAtSlot(int slot) { return slotGroupMap.get(slot); }
    public int getBackSlot()     { return NAV_BACK; }
    public int getPrevSlot()     { return NAV_PREV; }
    public int getNextSlot()     { return NAV_NEXT; }
    public int getCurrentPage()  { return currentPage; }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
