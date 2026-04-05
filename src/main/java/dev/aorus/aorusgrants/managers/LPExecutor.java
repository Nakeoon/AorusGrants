package dev.aorus.aorusgrants.managers;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.model.GrantSession;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.query.QueryOptions;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LPExecutor {

    private final AorusGrants plugin;
    private final LuckPerms lp;

    /**
     * LuckPerms stores prefixes already translated to §-codes (legacy section sign format).
     * This serializer reads §-encoded strings and produces Adventure Components correctly,
     * including full HEX color support when LP uses &#RRGGBB notation internally.
     *
     * Use this for ANY string that comes from LP (prefixes, group names with color).
     * Also safe for our own &-encoded strings because we translate & → § first via
     * ColorUtil.color() before passing to deserialize().
     */
    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    public LPExecutor(AorusGrants plugin) {
        this.plugin = plugin;
        this.lp = plugin.getLuckPerms();
    }

    /**
     * Returns the shared Adventure serializer used by all menus.
     * Input: strings with & color codes (e.g. "&aHello" or the raw LP prefix).
     * LP prefixes that already contain § must be pre-converted with
     * ColorUtil.sectionToAmpersand() before passing here.
     */
    public LegacyComponentSerializer getAdventureSerializer() {
        return SERIALIZER;
    }

    // ─────────────────────────────────────────────────────────
    //   GRANT
    // ─────────────────────────────────────────────────────────

    public CompletableFuture<Boolean> executeGrant(GrantSession session) {
        return lp.getUserManager().loadUser(session.getTargetUUID()).thenApply(user -> {
            if (user == null) return false;

            InheritanceNode node;
            if (session.isPermanent()) {
                // Equivalent to: lp user <name> parent add <group>
                node = InheritanceNode.builder(session.getSelectedGroup())
                        .value(true)
                        .build();
            } else {
                // Equivalent to: lp user <name> parent addtemp <group> <duration>
                Instant expiry = Instant.now().plus(Duration.ofMinutes(session.getTotalMinutes()));
                node = InheritanceNode.builder(session.getSelectedGroup())
                        .value(true)
                        .expiry(expiry)
                        .build();
            }

            // data().add() = parent add — NEVER removes other groups (unlike set)
            user.data().add(node);
            lp.getUserManager().saveUser(user);
            plugin.getLogger().info("[Grant] " + session.getTargetName()
                    + " -> " + session.getSelectedGroup()
                    + " | Duration: " + session.getDurationDisplay()
                    + " | By: " + session.getStaffUUID());
            return true;
        });
    }

    // ─────────────────────────────────────────────────────────
    //   REMOVE specific group
    // ─────────────────────────────────────────────────────────

    public CompletableFuture<Boolean> removeGroup(UUID targetUUID, String groupName) {
        return lp.getUserManager().loadUser(targetUUID).thenApply(user -> {
            if (user == null) return false;

            Set<Node> toRemove = user.data().toCollection().stream()
                    .filter(n -> n instanceof InheritanceNode)
                    .filter(n -> ((InheritanceNode) n).getGroupName().equalsIgnoreCase(groupName))
                    .collect(Collectors.toSet());

            if (toRemove.isEmpty()) return false;
            toRemove.forEach(user.data()::remove);
            lp.getUserManager().saveUser(user);
            plugin.getLogger().info("[RemoveGroup] " + targetUUID + " removed group: " + groupName);
            return true;
        });
    }

    // ─────────────────────────────────────────────────────────
    //   GET player groups — clean names + prefixes
    // ─────────────────────────────────────────────────────────

    public CompletableFuture<List<GroupInfo>> getPlayerGroups(UUID playerUUID) {
        return lp.getUserManager().loadUser(playerUUID).thenApply(user -> {
            if (user == null) return Collections.<GroupInfo>emptyList();

            return user.data().toCollection().stream()
                    .filter(n -> n instanceof InheritanceNode)
                    .map(n -> (InheritanceNode) n)
                    .map(n -> {
                        String groupName  = n.getGroupName();
                        String prefix     = getGroupPrefix(groupName);
                        boolean temporal  = n.hasExpiry();
                        String duration   = temporal ? formatExpiry(n.getExpiry()) : "Permanente";
                        // Fetch LP weight for sorting
                        Group g = lp.getGroupManager().getGroup(groupName.toLowerCase());
                        int weight = (g != null) ? g.getWeight().orElse(0) : 0;
                        return new GroupInfo(groupName, prefix, temporal, duration, weight);
                    })
                    // Highest weight first — matches the order the player sees their rank
                    .sorted(Comparator.comparingInt(GroupInfo::weight).reversed())
                    .collect(Collectors.toList());
        });
    }

    // ─────────────────────────────────────────────────────────
    //   GET player prefix
    // ─────────────────────────────────────────────────────────

    public CompletableFuture<String> getPlayerPrefix(UUID playerUUID) {
        return lp.getUserManager().loadUser(playerUUID).thenApply(user -> {
            if (user == null) return "";
            String p = user.getCachedData().getMetaData(QueryOptions.nonContextual()).getPrefix();
            return p != null ? p : "";
        });
    }

    // ─────────────────────────────────────────────────────────
    //   Get group prefix — reads PrefixNode directly to avoid
    //   getCachedData context issues that produce raw color codes
    // ─────────────────────────────────────────────────────────

    public String getGroupPrefix(String groupName) {
        Group group = lp.getGroupManager().getGroup(groupName.toLowerCase());
        if (group == null) return "";

        // Strategy 1: try cached metadata with nonContextual query (most reliable)
        String cached = group.getCachedData().getMetaData(QueryOptions.nonContextual()).getPrefix();
        if (cached != null && !cached.isEmpty()) return cached;

        // Strategy 2: read PrefixNode directly from the group's data
        // This handles cases where the server context isn't set up (offline/test)
        return group.data().toCollection().stream()
                .filter(n -> n.getType() == NodeType.PREFIX)
                .map(n -> (PrefixNode) n)
                .max(Comparator.comparingInt(PrefixNode::getPriority))
                .map(PrefixNode::getMetaValue)
                .orElse("");
    }

    // ─────────────────────────────────────────────────────────
    //   Helpers
    // ─────────────────────────────────────────────────────────

    private String formatExpiry(Instant expiry) {
        long diff = expiry.getEpochSecond() - (System.currentTimeMillis() / 1000L);
        if (diff <= 0) return "Expirado";

        long days  = diff / 86400;
        long hours = (diff % 86400) / 3600;
        long mins  = (diff % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days  > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (mins  > 0 || sb.isEmpty()) sb.append(Math.max(1, mins)).append("m");
        return sb.toString().trim();
    }

    /** DTO: clean group name, LP prefix, temporal flag, duration string, LP weight. */
    public record GroupInfo(String name, String prefix, boolean temporal, String duration, int weight) {
        public String expiry() { return duration; }
    }
}
