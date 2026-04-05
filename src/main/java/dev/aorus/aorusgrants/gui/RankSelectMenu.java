package dev.aorus.aorusgrants.gui;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.managers.LPExecutor;
import dev.aorus.aorusgrants.model.GrantSession;
import dev.aorus.aorusgrants.model.GroupData;
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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * MENU 2 — RANK SELECT (54 slots)
 *
 * Layout:
 *   Slot 4          → Player head (Adventure API lore)
 *   Corners 0,8,45,53 → Levers
 *   Content slots   → LP groups (weight DESC), display name = Adventure prefix
 *   Nav: 48 (prev)  | 49 (back) | 50 (next)
 */
public class RankSelectMenu {

    private static final String SEP = "&8&m------------------------------";

    private static final int HEAD_SLOT = 4;

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

    public RankSelectMenu(AorusGrants plugin) {
        this.plugin = plugin;
    }

    public void open(Player staff, GrantSession session) {
        open(staff, session, 0);
    }

    public void open(Player staff, GrantSession session, int page) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("rank-select-menu");
        if (cfg == null) return;

        String title = cfg.getString("title", "&8» &bSelect Rank &8«");
        int    size  = cfg.getInt("size", 54);

        plugin.getGroupManager().getLPGroupsSortedAsync().thenAccept(groups ->
            plugin.getLpExecutor().getPlayerGroups(session.getTargetUUID())
                  .thenAccept((List<LPExecutor.GroupInfo> playerGroups) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {

                    Inventory inv = MenuUtils.createInventory(title, size);

                    // Levers
                    for (int corner : CORNERS) {
                        inv.setItem(corner, new ItemBuilder(Material.LEVER).name(" ").hideFlags().build());
                    }

                    // ── Player head at slot 4 — Adventure API lore ────
                    // Group list: plain names (no prefix colors in this line)
                    StringBuilder sbGroups = new StringBuilder();
                    for (LPExecutor.GroupInfo g : playerGroups) {
                        if (sbGroups.length() > 0) sbGroups.append(", ");
                        sbGroups.append(ColorUtil.stripColor(g.prefix()).stripTrailing().isEmpty()
                                ? g.name()
                                : ColorUtil.stripColor(g.prefix()).stripTrailing());
                    }
                    String groupListStr = sbGroups.length() > 0 ? sbGroups.toString() : "None";

                    // Player's current prefix (highest-priority group's prefix)
                    String playerPrefixRaw = playerGroups.stream()
                            .filter(g -> !g.prefix().isEmpty())
                            .map(LPExecutor.GroupInfo::prefix)
                            .findFirst().orElse("");
                    String playerPrefixForAdventure = playerPrefixRaw.isEmpty()
                            ? "&7None"
                            : ColorUtil.sectionToAmpersand(playerPrefixRaw).stripTrailing();

                    ItemStack headItem = SkullUtils.getPlayerHead(
                            session.getTargetName(), session.getTargetName(), new ArrayList<>());
                    ItemMeta headMeta = headItem.getItemMeta();
                    if (headMeta != null) {
                        var ser = plugin.getLpExecutor().getAdventureSerializer();

                        Component headName = ser.deserialize("&f" + session.getTargetName())
                                .decoration(TextDecoration.ITALIC, false);

                        List<Component> loreComps = new ArrayList<>();
                        loreComps.add(ser.deserialize(SEP)
                                .decoration(TextDecoration.ITALIC, false));
                        loreComps.add(ser.deserialize("&7Player: &f" + session.getTargetName())
                                .decoration(TextDecoration.ITALIC, false));
                        loreComps.add(ser.deserialize("&7Rank:   " + playerPrefixForAdventure)
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

                    // ── Paginated group list ──────────────────────────
                    int totalPages  = Math.max(1, (int) Math.ceil((double) groups.size() / ITEMS_PER_PAGE));
                    int currentPage = Math.max(0, Math.min(page, totalPages - 1));
                    int startIdx    = currentPage * ITEMS_PER_PAGE;
                    int endIdx      = Math.min(startIdx + ITEMS_PER_PAGE, groups.size());

                    List<GroupData> pageGroups = groups.subList(startIdx, endIdx);
                    for (int i = 0; i < pageGroups.size(); i++) {
                        inv.setItem(CONTENT_SLOTS.get(i), buildGroupItem(pageGroups.get(i)));
                    }

                    placeNavBar(inv, currentPage, totalPages);
                    session.setCurrentPage(currentPage);
                    staff.openInventory(inv);
                })
            )
        );
    }

    // ─────────────────────────────────────────────────────────
    //   GROUP ITEM — display name via Adventure (HEX-safe prefix)
    // ─────────────────────────────────────────────────────────

    private ItemStack buildGroupItem(GroupData group) {
        // § → & so Adventure handles HEX correctly
        String prefixRaw = group.getPrefix().stripTrailing();
        String prefixForAdventure = prefixRaw.isEmpty()
                ? "&f" + group.getDisplayName()
                : ColorUtil.sectionToAmpersand(prefixRaw);

        // Plain lore — no LP prefix here, safe to go through ItemBuilder
        List<String> plainLore = Arrays.asList(
            SEP,
            "&7Group:  &f" + group.getDisplayName(),
            "&7Weight: &f" + group.getWeight(),
            "&7Type:   " + group.getTypeDisplay(),
            SEP,
            "",
            "&eClick to select."
        );

        // Resolve base item (custom or WHITE_WOOL)
        ItemStack item;
        if (plugin.getGroupItemStorage().hasCustomItem(group.getId())) {
            ItemStack custom = plugin.getGroupItemStorage().getItem(group.getId(), "");
            item = (custom != null) ? custom.clone() : new ItemStack(Material.WHITE_WOOL);
        } else {
            item = new ItemStack(Material.WHITE_WOOL);
        }

        // Apply display name via Adventure (respects HEX), lore via legacy
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

    // ─────────────────────────────────────────────────────────
    //   NAV BAR
    // ─────────────────────────────────────────────────────────

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

    public boolean isRankSelectMenu(String title) {
        ConfigurationSection cfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("rank-select-menu");
        if (cfg == null) return false;
        return ColorUtil.color(cfg.getString("title", "")).equals(title);
    }

    public void resolveClick(Player staff, GrantSession session, int slot,
                              Consumer<GroupData> onGroupSelected,
                              Runnable onBack, Runnable onPrev, Runnable onNext) {

        for (int c : CORNERS) if (c == slot) return;
        if (slot == HEAD_SLOT) return;

        if (slot == NAV_BACK) { onBack.run(); return; }
        if (slot == NAV_PREV) { onPrev.run(); return; }
        if (slot == NAV_NEXT) { onNext.run(); return; }

        int idx = CONTENT_SLOTS.indexOf(slot);
        if (idx < 0) return;

        int globalIdx = session.getCurrentPage() * ITEMS_PER_PAGE + idx;

        plugin.getGroupManager().getLPGroupsSortedAsync().thenAccept(groups -> {
            if (globalIdx < groups.size()) {
                GroupData group = groups.get(globalIdx);
                plugin.getServer().getScheduler().runTask(plugin, () -> onGroupSelected.accept(group));
            }
        });
    }

    public int getNavBackSlot() { return NAV_BACK; }
    public int getNavPrevSlot() { return NAV_PREV; }
    public int getNavNextSlot() { return NAV_NEXT; }
}
