package dev.aorus.aorusgrants.gui;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.managers.HistoryManager;
import dev.aorus.aorusgrants.utils.ColorUtil;
import dev.aorus.aorusgrants.utils.ItemBuilder;
import dev.aorus.aorusgrants.utils.MenuUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MENU — HISTORY (54 slots, paginated)
 *
 * Layout:
 *   Corners 0, 8, 45, 53 → Levers
 *   Content slots → BOOK (active) or PAPER (expired) per history entry, newest first
 *   Slot 48 → « Previous
 *   Slot 49 → Redstone back
 *   Slot 50 → Next »
 *
 * Item display name = LP prefix of the group (via Adventure API — HEX-safe).
 */
public class HistoryMenu {

    public static final String TITLE = "&8» &eGrant History &8«";

    private static final String SEP = "&8&m------------------------------";

    private static final List<Integer> CONTENT_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.size();

    private static final int[] CORNERS = {0, 8, 45, 53};
    private static final int NAV_PREV  = 48;
    private static final int NAV_BACK  = 49;
    private static final int NAV_NEXT  = 50;

    private final AorusGrants plugin;

    private int  currentPage  = 0;
    private UUID viewedTarget = null;

    public HistoryMenu(AorusGrants plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────
    //   OPEN
    // ─────────────────────────────────────────────────────────

    public void open(Player staff, UUID targetUuid, String targetName, int page) {
        this.viewedTarget = targetUuid;

        List<HistoryManager.HistoryEntry> entries =
                plugin.getHistoryManager().getHistory(targetUuid);

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        int currentP   = Math.max(0, Math.min(page, totalPages - 1));
        this.currentPage = currentP;

        Inventory inv = MenuUtils.createInventory(ColorUtil.color(TITLE), 54);

        // Levers on corners
        for (int corner : CORNERS) {
            inv.setItem(corner, new ItemBuilder(Material.LEVER).name(" ").hideFlags().build());
        }

        if (entries.isEmpty()) {
            inv.setItem(31, new ItemBuilder(Material.BARRIER)
                .name("&cNo history found")
                .lore(SEP, "&7No grants recorded for &f" + targetName + "&7.", SEP)
                .build());
        } else {
            int startIdx = currentP * ITEMS_PER_PAGE;
            int endIdx   = Math.min(startIdx + ITEMS_PER_PAGE, entries.size());
            List<HistoryManager.HistoryEntry> pageEntries = entries.subList(startIdx, endIdx);

            for (int i = 0; i < pageEntries.size(); i++) {
                inv.setItem(CONTENT_SLOTS.get(i), buildEntryItem(pageEntries.get(i)));
            }
        }

        placeNavBar(inv, currentP, totalPages, targetName, entries.size());
        staff.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────
    //   ENTRY ITEM — display name = real LP prefix via Adventure
    // ─────────────────────────────────────────────────────────

    private ItemStack buildEntryItem(HistoryManager.HistoryEntry entry) {
        // Resolve the real LP prefix of the group so HEX colors show correctly
        String rawPrefix = plugin.getLpExecutor().getGroupPrefix(entry.group());

        // § → & for Adventure's ampersand serializer
        String prefixForAdventure = (rawPrefix == null || rawPrefix.isBlank())
                ? "&f" + entry.group()   // fallback: plain group name
                : ColorUtil.sectionToAmpersand(rawPrefix).stripTrailing();

        // Plain lore — no LP prefix here, safe for legacy string approach
        List<String> plainLore = new ArrayList<>();
        plainLore.add(SEP);
        plainLore.add("&7Granted by: &f" + entry.staffName());
        plainLore.add("&7Date:       &f" + entry.formattedDate());
        plainLore.add("&7Duration:   &f" + entry.duration());
        plainLore.add("&7Status:     " + entry.statusDisplay());
        plainLore.add(SEP);

        // BOOK = active, PAPER = expired
        Material mat = entry.isActive() ? Material.BOOK : Material.PAPER;

        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            var ser = plugin.getLpExecutor().getAdventureSerializer();

            // Display name via Adventure — respects HEX, no italic
            Component nameComp = ser.deserialize(prefixForAdventure)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(nameComp);

            // Lore via legacy (no prefix data — safe)
            meta.setLore(plainLore.stream().map(ColorUtil::color).collect(Collectors.toList()));

            item.setItemMeta(meta);
        }
        return item;
    }

    // ─────────────────────────────────────────────────────────
    //   NAV BAR
    // ─────────────────────────────────────────────────────────

    private void placeNavBar(Inventory inv, int page, int totalPages,
                              String targetName, int totalEntries) {
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
            .lore(SEP, "&7History for &f" + targetName,
                  "&7Total entries: &f" + totalEntries, SEP)
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

    // ─────────────────────────────────────────────────────────
    //   MENU IDENTIFICATION
    // ─────────────────────────────────────────────────────────

    public boolean isHistoryMenu(String title) {
        return ColorUtil.color(TITLE).equals(title);
    }

    // ─────────────────────────────────────────────────────────
    //   STATE ACCESSORS
    // ─────────────────────────────────────────────────────────

    public int  getCurrentPage()  { return currentPage;  }
    public UUID getViewedTarget() { return viewedTarget; }
    public int  getNavBackSlot()  { return NAV_BACK;     }
    public int  getNavPrevSlot()  { return NAV_PREV;     }
    public int  getNavNextSlot()  { return NAV_NEXT;     }
}
