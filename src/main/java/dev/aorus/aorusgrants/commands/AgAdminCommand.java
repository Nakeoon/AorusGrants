package dev.aorus.aorusgrants.commands;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.gui.HistoryMenu;
import dev.aorus.aorusgrants.managers.ConfigManager;
import dev.aorus.aorusgrants.managers.HistoryManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /agadmin setitem <grupo>
 * Sets the display item for a group to whatever the staff has in hand.
 * Saves: material, custom model data, name, lore → group-items.yml
 */
public class AgAdminCommand implements CommandExecutor, TabCompleter {

    private final AorusGrants plugin;

    public AgAdminCommand(AorusGrants plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();

        if (!(sender instanceof Player staff)) {
            sender.sendMessage(ConfigManager.color("&cSolo jugadores pueden usar este comando."));
            return true;
        }

        if (!staff.hasPermission("aorusgrants.admin")) {
            staff.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(staff, prefix);
            return true;
        }

        // ── /agadmin setitem <grupo> ───────────────────────────
        if (args[0].equalsIgnoreCase("setitem")) {
            if (args.length < 2) {
                staff.sendMessage(prefix + ConfigManager.color("&eUso: /agadmin setitem <grupo>"));
                return true;
            }

            String groupId = args[1].toLowerCase();

            // Verify group exists in LP
            if (!plugin.getGroupManager().existsInLP(groupId)) {
                staff.sendMessage(prefix + ConfigManager.color(
                    "&cEl grupo &e" + groupId + "&c no existe en LuckPerms."));
                return true;
            }

            ItemStack hand = staff.getInventory().getItemInMainHand();

            if (hand.getType().isAir()) {
                staff.sendMessage(prefix + ConfigManager.color(
                    "&cDebes tener un item en la mano."));
                return true;
            }

            plugin.getGroupItemStorage().saveItem(groupId, hand);

            staff.sendMessage(prefix + ConfigManager.color(
                "&aItem del grupo &e" + groupId + "&a actualizado a &f"
                + hand.getType().name()
                + (hand.getItemMeta() != null && hand.getItemMeta().hasCustomModelData()
                    ? " &7(CMD: " + hand.getItemMeta().getCustomModelData() + ")" : "")
                + "&a."));
            return true;
        }

        // ── /agadmin reload ────────────────────────────────────
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().reload();
            plugin.getGroupManager().reload();
            plugin.getGroupItemStorage().reload();
            staff.sendMessage(prefix + ConfigManager.color("&aConfiguración recargada."));
            return true;
        }

        // ── /agadmin stafflog ─────────────────────────────────
        if (args[0].equalsIgnoreCase("stafflog")) {
            String targetStaff = args.length >= 2 ? args[1] : staff.getName();
            var history = plugin.getHistoryManager().getStaffHistoryByName(targetStaff);

            if (history.isEmpty()) {
                staff.sendMessage(prefix + ConfigManager.color(
                    "&cNo se encontraron acciones para &e" + targetStaff));
                return true;
            }

            new HistoryMenu(plugin).open(staff, staff.getUniqueId(), targetStaff + " (Staff Log)", 0);
            return true;
        }

        sendAdminHelp(staff, prefix);
        return true;
    }

    private void sendAdminHelp(Player staff, String prefix) {
        staff.sendMessage(ConfigManager.color("&8&m            &r"));
        staff.sendMessage(ConfigManager.color("&b AorusGrants &7Admin"));
        staff.sendMessage(ConfigManager.color("&8&m            &r"));
        staff.sendMessage(ConfigManager.color("&e/agadmin setitem <grupo> &7→ Asignar item del grupo"));
        staff.sendMessage(ConfigManager.color("&e/agadmin stafflog [staff] &7→ Ver historial de staff"));
        staff.sendMessage(ConfigManager.color("&e/agadmin reload &7→ Recargar configs"));
        staff.sendMessage(ConfigManager.color("&8&m            &r"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String sub : List.of("setitem", "reload")) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setitem")) {
            String partial = args[1].toLowerCase();
            plugin.getLuckPerms().getGroupManager().getLoadedGroups().forEach(g -> {
                if (g.getName().startsWith(partial)) completions.add(g.getName());
            });
        }

        return completions;
    }
}
