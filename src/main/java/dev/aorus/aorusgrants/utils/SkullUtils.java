package dev.aorus.aorusgrants.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.stream.Collectors;

public class SkullUtils {

    /**
     * Create a player head ItemStack with name and lore.
     */
    public static ItemStack getPlayerHead(String playerName, String displayName, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(ColorUtil.color(displayName));

            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream().map(ColorUtil::color).collect(Collectors.toList()));
            }

            skull.setItemMeta(meta);
        }

        return skull;
    }
}
