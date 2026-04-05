package dev.aorus.aorusgrants.listeners;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.gui.*;
import dev.aorus.aorusgrants.managers.ConfigManager;
import dev.aorus.aorusgrants.model.GrantSession;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import java.util.UUID;

public class MenuListener implements Listener {

    private final AorusGrants plugin;
    private final MainMenu      mainMenu;
    private final RankSelectMenu rankSelectMenu;
    private final DurationMenu  durationMenu;
    private final ConfirmMenu   confirmMenu;
    private final InfoMenu      infoMenu;
    private final DemoteMenu    demoteMenu;
    private final HistoryMenu   historyMenu;   // NEW

    public MenuListener(AorusGrants plugin) {
        this.plugin         = plugin;
        this.mainMenu       = new MainMenu(plugin);
        this.rankSelectMenu = new RankSelectMenu(plugin);
        this.durationMenu   = new DurationMenu(plugin);
        this.confirmMenu    = new ConfirmMenu(plugin);
        this.infoMenu       = new InfoMenu(plugin);
        this.demoteMenu     = new DemoteMenu(plugin);
        this.historyMenu    = new HistoryMenu(plugin);   // NEW
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player staff)) return;
        if (e.getClickedInventory() == null) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        String title = e.getView().getTitle();
        int    slot  = e.getSlot();

        // ── ONLY cancel if this is one of our menus ────────────
        // Cancelling before this check would block ALL inventory
        // interactions on the server (chests, crafting, etc.)
        boolean isOurMenu = historyMenu.isHistoryMenu(title)
                || mainMenu.isMainMenu(title)
                || rankSelectMenu.isRankSelectMenu(title)
                || durationMenu.isDurationMenu(title)
                || confirmMenu.isConfirmMenu(title)
                || infoMenu.isInfoMenu(title)
                || demoteMenu.isDemoteMenu(title);

        if (!isOurMenu) return;

        e.setCancelled(true);
        playClickSound(staff);

        // History menu does NOT require a GrantSession — check it first
        if (historyMenu.isHistoryMenu(title)) {
            handleHistory(staff, slot);
            return;
        }

        GrantSession session = plugin.getSessionManager().getSession(staff.getUniqueId());
        if (session == null) return;

        if      (mainMenu.isMainMenu(title))              handleMainMenu(staff, session, slot);
        else if (rankSelectMenu.isRankSelectMenu(title))  handleRankSelect(staff, session, slot);
        else if (durationMenu.isDurationMenu(title))      handleDuration(staff, session, slot);
        else if (confirmMenu.isConfirmMenu(title))        handleConfirm(staff, session, slot);
        else if (infoMenu.isInfoMenu(title))              handleInfo(staff, session, slot);
        else if (demoteMenu.isDemoteMenu(title))          handleDemote(staff, session, slot);
    }

    // ─────────────────────────────────────────────────────────
    //   MAIN MENU (27 slots)
    //   Slot 13 (book)  → opens InfoMenu
    //   Slot 11 (green) → Promote
    //   Slot 15 (red)   → Demote
    // ─────────────────────────────────────────────────────────
    private void handleMainMenu(Player staff, GrantSession session, int slot) {
        var menusCfg = plugin.getConfigManager().getMenusConfig()
                .getConfigurationSection("main-menu");
        if (menusCfg == null) return;

        int bookSlot    = mainMenu.getBookSlot();
        int promoteSlot = menusCfg.getInt("promote-button.slot", 11);
        int demoteSlot  = menusCfg.getInt("demote-button.slot",  15);

        if (slot == bookSlot) {
            // Book center → open player's group info
            infoMenu.open(staff, session);

        } else if (slot == promoteSlot) {
            session.setAction(GrantSession.Action.PROMOTE);
            rankSelectMenu.open(staff, session, 0);

        } else if (slot == demoteSlot) {
            session.setAction(GrantSession.Action.DEMOTE);
            demoteMenu.open(staff, session, 0);
        }
    }

    // ─────────────────────────────────────────────────────────
    //   RANK SELECT (54 slots, paginado)
    // ─────────────────────────────────────────────────────────
    private void handleRankSelect(Player staff, GrantSession session, int slot) {
        rankSelectMenu.resolveClick(
            staff, session, slot,
            group -> {
                session.setSelectedGroup(group.getId());
                durationMenu.open(staff, session);
            },
            () -> mainMenu.open(staff, session),
            () -> rankSelectMenu.open(staff, session, session.getCurrentPage() - 1),
            () -> rankSelectMenu.open(staff, session, session.getCurrentPage() + 1)
        );
    }

    // ─────────────────────────────────────────────────────────
    //   DURATION MENU
    // ─────────────────────────────────────────────────────────
    private void handleDuration(Player staff, GrantSession session, int slot) {
        int confirmSlot = durationMenu.getConfirmSlot();
        int backSlot    = durationMenu.getBackSlot();
        int prevSlot    = durationMenu.getNavPrevSlot();
        int nextSlot    = durationMenu.getNavNextSlot();

        if (slot == prevSlot || slot == nextSlot) return;

        if (slot == backSlot) {
            rankSelectMenu.open(staff, session, session.getCurrentPage());
            return;
        }

        if (slot == confirmSlot) {
            if (!session.hasDuration()) {
                staff.sendMessage(plugin.getConfigManager().getPrefix()
                        + ConfigManager.color("&cPlease select a duration first."));
                return;
            }
            confirmMenu.open(staff, session);
            return;
        }

        int[] timeData = durationMenu.getTimeClickData(slot);
        if (timeData != null) {
            switch (timeData[0]) {
                case 0 -> session.addDays(timeData[1]);
                case 1 -> session.addWeeks(timeData[1]);
                case 2 -> session.addMonths(timeData[1]);
                case 3 -> session.setPermanent();
            }
            var durCfg = durationMenu.getConfig();
            if (durCfg != null) {
                durationMenu.refreshConfirmButton(staff.getOpenInventory().getTopInventory(), durCfg, session);
            }
            staff.sendMessage(plugin.getConfigManager().getPrefix()
                    + ConfigManager.color("&7Duración: &f"
                    + session.getDurationDisplay().replace("\n", " ")));
        }
    }

    // ─────────────────────────────────────────────────────────
    //   CONFIRM MENU (27 slots)
    // ─────────────────────────────────────────────────────────
    private void handleConfirm(Player staff, GrantSession session, int slot) {
        int confirmSlot = confirmMenu.getConfirmSlot();
        int cancelSlot  = confirmMenu.getCancelSlot();

        if (slot == cancelSlot) {
            mainMenu.open(staff, session);
            return;
        }

        if (slot == confirmSlot) {
            staff.closeInventory();

            // Snapshot session data before async (session removed after)
            final String  targetName   = session.getTargetName();
            final UUID    targetUuid   = session.getTargetUUID();
            final String  groupName    = session.getSelectedGroup() != null ? session.getSelectedGroup() : "";
            final String  duration     = session.getDurationDisplay();
            final boolean isPermanent  = session.isPermanent();
            final long    totalMinutes = session.getTotalMinutes();
            final String  staffName    = staff.getName();
            final UUID    staffUuid    = staff.getUniqueId();

            plugin.getLpExecutor().executeGrant(session).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        // ── Success sound ─────────────────────────────
                        playSuccessSound(staff);

                        // ── Record in history.yml ─────────────────────
                        plugin.getHistoryManager().recordGrant(
                                targetUuid, targetName,
                                staffUuid,  staffName,
                                groupName,
                                duration, isPermanent, totalMinutes
                        );

                        // ── Broadcast to staff ────────────────────────
                        String broadcast = plugin.getConfigManager().getPrefix()
                                + ConfigManager.color("&e" + staffName + " &7granted &b" + groupName
                                + " &7to &e" + targetName + " &7(&f" + duration + "&7)");
                        broadcastToAdmins(broadcast);
                    } else {
                        staff.sendMessage(plugin.getConfigManager().getPrefix()
                                + ConfigManager.color("&cFailed to execute grant."));
                    }
                });
            });
            plugin.getSessionManager().removeSession(staff.getUniqueId());
        }
    }

    // ─────────────────────────────────────────────────────────
    //   INFO MENU (54 slots)
    // ─────────────────────────────────────────────────────────
    private void handleInfo(Player staff, GrantSession session, int slot) {
        // Ignore corner levers
        if (slot == 0 || slot == 8 || slot == 45 || slot == 53) return;

        if (slot == infoMenu.getBackSlot()) {
            mainMenu.open(staff, session);
        }
    }

    // ─────────────────────────────────────────────────────────
    //   DEMOTE MENU (54 slots, paginado)
    // ─────────────────────────────────────────────────────────
    private void handleDemote(Player staff, GrantSession session, int slot) {
        // Ignore corner levers
        if (slot == 0 || slot == 8 || slot == 45 || slot == 53) return;

        int backSlot = demoteMenu.getBackSlot();
        int prevSlot = demoteMenu.getPrevSlot();
        int nextSlot = demoteMenu.getNextSlot();

        if (slot == backSlot) {
            mainMenu.open(staff, session);
            return;
        }
        if (slot == prevSlot) {
            demoteMenu.open(staff, session, demoteMenu.getCurrentPage() - 1);
            return;
        }
        if (slot == nextSlot) {
            demoteMenu.open(staff, session, demoteMenu.getCurrentPage() + 1);
            return;
        }

        String groupName = demoteMenu.getGroupAtSlot(slot);
        if (groupName == null) return;

        staff.closeInventory();

        // Snapshot before async
        final String targetNameD = session.getTargetName();
        final String staffNameD  = staff.getName();
        final String groupRemoved = groupName;

        plugin.getLpExecutor().removeGroup(session.getTargetUUID(), groupName).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    // ── Success sound ─────────────────────────────
                    playSuccessSound(staff);

                    // Broadcast to admins
                    String broadcast = plugin.getConfigManager().getPrefix()
                            + ConfigManager.color("&e" + staffNameD + " &7removed &b" + groupRemoved
                            + " &7from &e" + targetNameD);
                    broadcastToAdmins(broadcast);
                } else {
                    staff.sendMessage(plugin.getConfigManager().getPrefix()
                            + ConfigManager.color("&cCould not remove group &e" + groupRemoved + "&c."));
                }
            });
        });

        plugin.getSessionManager().removeSession(staff.getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Sessions persist within timeout window
    }

    // ─────────────────────────────────────────────────────────
    //   HISTORY MENU  (no GrantSession required)
    // ─────────────────────────────────────────────────────────
    private void handleHistory(Player staff, int slot) {
        java.util.UUID viewedTarget = historyMenu.getViewedTarget();
        if (viewedTarget == null) return;

        // Ignore corner levers
        if (slot == 0 || slot == 8 || slot == 45 || slot == 53) return;

        int backSlot = historyMenu.getNavBackSlot();
        int prevSlot = historyMenu.getNavPrevSlot();
        int nextSlot = historyMenu.getNavNextSlot();

        if (slot == backSlot) {
            staff.closeInventory();
            return;
        }
        if (slot == prevSlot) {
            int prev = historyMenu.getCurrentPage() - 1;
            if (prev >= 0) historyMenu.open(staff, viewedTarget, resolveHistoryName(viewedTarget), prev);
            return;
        }
        if (slot == nextSlot) {
            historyMenu.open(staff, viewedTarget, resolveHistoryName(viewedTarget), historyMenu.getCurrentPage() + 1);
            return;
        }
        // Content slots are read-only entries
    }

    /** Resolves the display name for a UUID when paginating history. */
    private String resolveHistoryName(java.util.UUID uuid) {
        var entries = plugin.getHistoryManager().getHistory(uuid);
        if (!entries.isEmpty()) return entries.get(0).targetName();
        var offline = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString().substring(0, 8);
    }

    // ─────────────────────────────────────────────────────────
    //   SOUND EFFECTS
    // ─────────────────────────────────────────────────────────

    /** Subtle click feedback on every valid menu interaction. */
    private void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
    }

    /** Satisfying confirmation sound when a grant or demote succeeds. */
    private void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.0f);
    }

    // ─────────────────────────────────────────────────────────
    //   BROADCAST to all online players with aorusgrants.admin
    //   This logs actions visibly in-game to staff members.
    // ─────────────────────────────────────────────────────────
    private void broadcastToAdmins(String message) {
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("aorusgrants.admin"))
                .forEach(p -> p.sendMessage(message));
        // Also log to console
        plugin.getLogger().info("[Action] " + org.bukkit.ChatColor.stripColor(message));
    }

}
