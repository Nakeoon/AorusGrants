package dev.aorus.aorusgrants.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null) {
            List<String> colored = lines.stream()
                    .map(ColorUtil::color)
                    .collect(Collectors.toList());
            meta.setLore(colored);
        }
        return this;
    }

    public ItemBuilder appendLore(String... lines) {
        if (meta != null) {
            List<String> existing = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            for (String line : lines) {
                existing.add(ColorUtil.color(line));
            }
            meta.setLore(existing);
        }
        return this;
    }

    public ItemBuilder hideFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    // ── Static helpers ─────────────────────────────────────────

    public static ItemStack filler(Material material) {
        return new ItemBuilder(material).name(" ").hideFlags().build();
    }

    public static ItemStack filler() {
        return filler(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    }
}
