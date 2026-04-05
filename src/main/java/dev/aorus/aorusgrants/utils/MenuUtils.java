package dev.aorus.aorusgrants.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

public class MenuUtils {

    public static final int[] CORNER_SLOTS_54 = {0, 8, 45, 53};
    public static final int[] CORNER_SLOTS_27 = {0, 8, 18, 26};
    public static final Material LEVER = Material.LEVER;

    /**
     * Create a plain inventory with NO filler, NO corners.
     * Menus are minimalista — only functional items shown.
     */
    public static Inventory createInventory(String title, int size) {
        return Bukkit.createInventory(null, size, ColorUtil.color(title));
    }

    /**
     * Place levers on corners only for menus that want them (54-slot main navigation menus).
     * Pass size=54 or size=27 to get the right corners.
     */
    public static void placeCorners(Inventory inv, int size) {
        int[] slots = (size == 27) ? CORNER_SLOTS_27 : CORNER_SLOTS_54;
        for (int s : slots) {
            inv.setItem(s, new ItemBuilder(LEVER).name(" ").hideFlags().build());
        }
    }

    /**
     * Create inventory with corner levers (for main menu / rank select).
     */
    public static Inventory createWithCorners(String title, int size) {
        Inventory inv = createInventory(title, size);
        placeCorners(inv, size);
        return inv;
    }

    /** Legacy alias used by old code — just creates plain inventory. */
    public static Inventory createBorderedInventory(String title, int size) {
        return createInventory(title, size);
    }

    public static boolean isCornerSlot(int slot, int size) {
        int[] slots = (size == 27) ? CORNER_SLOTS_27 : CORNER_SLOTS_54;
        for (int s : slots) if (s == slot) return true;
        return false;
    }
}
