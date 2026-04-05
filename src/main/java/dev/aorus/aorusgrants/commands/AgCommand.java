package dev.aorus.aorusgrants.commands;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.gui.HistoryMenu;
import dev.aorus.aorusgrants.gui.MainMenu;
import dev.aorus.aorusgrants.managers.ConfigManager;
import dev.aorus.aorusgrants.model.GrantSession;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgCommand implements CommandExecutor, TabCompleter {

    private final AorusGrants plugin;
    private final MainMenu    mainMenu;
    private final HistoryMenu historyMenu;

    public AgCommand(AorusGrants plugin) {
        this.plugin      = plugin;
        this.mainMenu    = new MainMenu(plugin);
        this.historyMenu = new HistoryMenu(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();

        if (!(sender instanceof Player staff)) {
            sender.sendMessage(ConfigManager.color("&cOnly players can use this command."));
            return true;
        }

        if (!staff.hasPermission("aorusgrants.use")) {
            staff.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(staff);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ── /ag help ──────────────────────────────────────────
        if (sub.equals("help")) {
            sendHelp(staff);
            return true;
        }

        // ── /ag reload (admin only) ───────────────────────────
        if (sub.equals("reload") && staff.hasPermission("aorusgrants.admin")) {
            plugin.getConfigManager().reload();
            plugin.getGroupManager().reload();
            plugin.getGroupItemStorage().reload();
            staff.sendMessage(prefix + ConfigManager.color("&aConfiguration reloaded."));
            return true;
        }

        // ── /ag history <player> ──────────────────────────────
        if (sub.equals("history")) {
            if (!staff.hasPermission("aorusgrants.admin")) {
                staff.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                staff.sendMessage(prefix + ConfigManager.color("&eUsage: &f/ag history <player>"));
                return true;
            }
            openHistoryForPlayer(staff, args[1]);
            return true;
        }

        // ── /ag <player> — open grant menu ───────────────────
        openGrantMenu(staff, args[0]);
        return true;
    }

    // ─────────────────────────────────────────────────────────
    //   GRANT MENU  (online + offline player support)
    //
    //   Resolution order:
    //   1. Bukkit online player (exact match, instant)
    //   2. Bukkit offline cache (hasPlayedBefore, instant)
    //   3. LuckPerms async UUID lookup (covers LP-only users)
    //   4. Direct UUID parse (if staff typed a raw UUID)
    // ─────────────────────────────────────────────────────────

    private void openGrantMenu(Player staff, String targetInput) {
        String prefix = plugin.getConfigManager().getPrefix();

        // 1. Online player
        Player online = Bukkit.getPlayerExact(targetInput);
        if (online != null) {
            openSession(staff, online.getUniqueId(), online.getName());
            return;
        }

        // 2. Bukkit offline cache
        @SuppressWarnings("deprecation")
        OfflinePlayer cached = Bukkit.getOfflinePlayer(targetInput);
        if (cached.hasPlayedBefore() || cached.getName() != null) {
            String name = cached.getName() != null ? cached.getName() : targetInput;
            openSession(staff, cached.getUniqueId(), name);
            return;
        }

        // 3. LP async lookup
        staff.sendMessage(prefix + ConfigManager.color(
                "&7Looking up &f" + targetInput + "&7 via LuckPerms..."));

        plugin.getLuckPerms().getUserManager().lookupUniqueId(targetInput).thenAccept(uuid -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (uuid == null) {
                    // 4. Try raw UUID input
                    UUID parsed = tryParseUUID(targetInput);
                    if (parsed == null) {
                        staff.sendMessage(prefix + plugin.getConfigManager()
                                .getMessage("player-not-found")
                                .replace("{player}", targetInput));
                        return;
                    }
                    openSession(staff, parsed, targetInput);
                    return;
                }
                // Resolve username from LP then open
                plugin.getLuckPerms().getUserManager().lookupUsername(uuid).thenAccept(resolvedName -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        String displayName = resolvedName != null ? resolvedName : targetInput;
                        openSession(staff, uuid, displayName);
                    });
                });
            });
        });
    }

    private void openSession(Player staff, UUID targetUuid, String targetName) {
        GrantSession session = plugin.getSessionManager().createSession(
                staff.getUniqueId(), targetUuid, targetName);
        mainMenu.open(staff, session);
    }

    // ─────────────────────────────────────────────────────────
    //   HISTORY MENU  (same offline resolution as grant menu)
    // ─────────────────────────────────────────────────────────

    private void openHistoryForPlayer(Player staff, String targetInput) {
        String prefix = plugin.getConfigManager().getPrefix();

        // 1. Online
        Player online = Bukkit.getPlayerExact(targetInput);
        if (online != null) {
            historyMenu.open(staff, online.getUniqueId(), online.getName(), 0);
            return;
        }

        // 2. Bukkit cache
        @SuppressWarnings("deprecation")
        OfflinePlayer cached = Bukkit.getOfflinePlayer(targetInput);
        if (cached.hasPlayedBefore() || cached.getName() != null) {
            String name = cached.getName() != null ? cached.getName() : targetInput;
            historyMenu.open(staff, cached.getUniqueId(), name, 0);
            return;
        }

        // 3. LP async lookup
        staff.sendMessage(prefix + ConfigManager.color(
                "&7Looking up &f" + targetInput + "&7 via LuckPerms..."));

        plugin.getLuckPerms().getUserManager().lookupUniqueId(targetInput).thenAccept(uuid -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (uuid == null) {
                    staff.sendMessage(prefix + plugin.getConfigManager()
                            .getMessage("player-not-found")
                            .replace("{player}", targetInput));
                    return;
                }
                plugin.getLuckPerms().getUserManager().lookupUsername(uuid).thenAccept(resolvedName -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        String displayName = resolvedName != null ? resolvedName : targetInput;
                        historyMenu.open(staff, uuid, displayName, 0);
                    });
                });
            });
        });
    }

    // ─────────────────────────────────────────────────────────
    //   HELP
    // ─────────────────────────────────────────────────────────

    private void sendHelp(Player staff) {
        boolean isAdmin = staff.hasPermission("aorusgrants.admin");
        staff.sendMessage(ConfigManager.color("&8&m            &r"));
        staff.sendMessage(ConfigManager.color(" &bAorusGrants &7Help"));
        staff.sendMessage(ConfigManager.color("&8&m            &r"));
        staff.sendMessage(ConfigManager.color("&e/ag <player>              &7→ Open grant menu"));
        staff.sendMessage(ConfigManager.color("&e/ag help                  &7→ View this help"));
        if (isAdmin) {
            staff.sendMessage(ConfigManager.color(""));
            staff.sendMessage(ConfigManager.color(" &c Admin:"));
            staff.sendMessage(ConfigManager.color("&e/ag history <player>      &7→ View grant history"));
            staff.sendMessage(ConfigManager.color("&e/agadmin setitem <group>  &7→ Set rank item"));
            staff.sendMessage(ConfigManager.color("&e/agadmin reload           &7→ Reload configuration"));
        }
        staff.sendMessage(ConfigManager.color("&8&m            &r"));
    }

    // ─────────────────────────────────────────────────────────
    //   TAB COMPLETE
    // ─────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) completions.add(p.getName());
            }
            if ("help".startsWith(partial))    completions.add("help");
            if ("history".startsWith(partial) && sender.hasPermission("aorusgrants.admin"))
                completions.add("history");
            if ("reload".startsWith(partial)  && sender.hasPermission("aorusgrants.admin"))
                completions.add("reload");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("history")
                   && sender.hasPermission("aorusgrants.admin")) {
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) completions.add(p.getName());
            }
        }

        return completions;
    }

    // ─────────────────────────────────────────────────────────
    //   UTIL
    // ─────────────────────────────────────────────────────────

    private static UUID tryParseUUID(String s) {
        try { return UUID.fromString(s); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    /** Called from AorusGrants.java to register this HistoryMenu in MenuListener. */
    public HistoryMenu getHistoryMenu() { return historyMenu; }
}
