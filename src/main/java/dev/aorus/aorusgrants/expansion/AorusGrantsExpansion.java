package dev.aorus.aorusgrants.expansion;

import dev.aorus.aorusgrants.AorusGrants;
import dev.aorus.aorusgrants.managers.LPExecutor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class AorusGrantsExpansion extends PlaceholderExpansion {

    private final AorusGrants plugin;

    public AorusGrantsExpansion(AorusGrants plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "aorusgrants";
    }

    @Override
    public String getAuthor() {
        return "Aorus";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean requiresCachePlayer() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        String[] args = params.split("_");
        if (args.length == 0) return "";

        return switch (args[0].toLowerCase()) {
            case "rank" -> getPrimaryRank(player);
            case "ranks" -> getAllRanks(player);
            case "rank_expire" -> getRankExpire(player);
            case "rank_time" -> getRankTimeRemaining(player);
            default -> null;
        };
    }

    private String getPrimaryRank(OfflinePlayer player) {
        if (!player.isOnline()) {
            return getRankSync(player.getUniqueId());
        }
        Player p = player.getPlayer();
        if (p == null) return "";

        return plugin.getLpExecutor().getPlayerGroups(p.getUniqueId())
                .thenApply(list -> list.isEmpty() ? "None" : list.get(0).name())
                .join();
    }

    private String getRankSync(java.util.UUID uuid) {
        try {
            return plugin.getLpExecutor().getPlayerGroups(uuid)
                    .thenApply(list -> list.isEmpty() ? "None" : list.get(0).name())
                    .get();
        } catch (Exception e) {
            return "None";
        }
    }

    private String getAllRanks(OfflinePlayer player) {
        return plugin.getLpExecutor().getPlayerGroups(player.getUniqueId())
                .thenApply(list -> list.isEmpty() ? "None" : list.stream()
                        .map(g -> g.name())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("None"))
                .join();
    }

    private String getRankExpire(OfflinePlayer player) {
        return plugin.getLpExecutor().getPlayerGroups(player.getUniqueId())
                .thenApply(list -> list.isEmpty() ? "No temp ranks" : list.stream()
                        .filter(g -> g.temporal())
                        .map(LPExecutor.GroupInfo::expiry)
                        .findFirst()
                        .orElse("Permanent"))
                .join();
    }

    private String getRankTimeRemaining(OfflinePlayer player) {
        return getRankExpire(player);
    }
}