package dev.aorus.aorusgrants.utils;

/**
 * Utility class for translating color codes.
 *
 * IMPORTANT: LuckPerms already stores prefixes with § (section sign).
 * We must NOT double-encode them. Use renderPrefix() when rendering
 * LP prefixes that already contain §, and color() only for raw &-strings.
 *
 * For Adventure API: use sectionToAmpersand() to convert LP § prefixes
 * to & before passing to LegacyComponentSerializer.legacyAmpersand().
 */
public final class ColorUtil {

    private ColorUtil() {}

    /**
     * Translate '&' color codes to §.
     * Only use this on strings that haven't been processed by LP yet.
     */
    public static String color(String text) {
        if (text == null) return "";
        return text.replace("&", "\u00A7");
    }

    /**
     * Strips formatting for clean plain text.
     * Removes both § and & color sequences.
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)[§&][0-9a-fk-or]", "");
    }

    /**
     * Ensures a prefix string is rendered correctly in Minecraft item names
     * via legacy setDisplayName(). LP prefixes already contain §.
     *   - If string already has § codes: return as-is
     *   - If string only has & codes: translate to §
     */
    public static String renderPrefix(String prefix) {
        if (prefix == null) return "";
        if (prefix.contains("\u00A7")) return prefix;
        return prefix.replace("&", "\u00A7");
    }

    /**
     * Converts § (section sign) back to & ampersand codes.
     *
     * REQUIRED before passing an LP prefix to Adventure's
     * LegacyComponentSerializer.legacyAmpersand().deserialize().
     * LP stores prefixes as §-encoded; the ampersand serializer expects &-codes.
     */
    public static String sectionToAmpersand(String text) {
        if (text == null) return "";
        return text.replace("\u00A7", "&");
    }

    public static String stripTrailing(String text) {
        if (text == null) return "";
        int len = text.length();
        if (len == 0) return text;
        while (len > 0 && (text.charAt(len - 1) == ' ' || text.charAt(len - 1) == '\u00A7')) {
            len--;
        }
        return len == text.length() ? text : text.substring(0, len);
    }
}
