package dev.aorus.aorusgrants.model;

import org.bukkit.Material;

public class GroupData {

    public enum GroupType {
        DEFAULT, DONATOR, STAFF, HIDDEN
    }

    private final String id;
    private final String displayName;
    private final String prefix;
    private final int weight;
    private final GroupType type;
    private final Material material;

    public GroupData(String id, String displayName, String prefix, int weight, GroupType type, Material material) {
        this.id = id;
        this.displayName = displayName;
        this.prefix = prefix;
        this.weight = weight;
        this.type = type;
        this.material = material;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getPrefix() { return prefix != null ? prefix : ""; }
    public int getWeight() { return weight; }
    public GroupType getType() { return type; }
    public Material getMaterial() { return material; }

    public String getTypeDisplay() {
        return switch (type) {
            case DEFAULT -> "&7Default";
            case DONATOR -> "&bDonator";
            case STAFF -> "&cStaff";
            case HIDDEN -> "&8Hidden";
        };
    }
}
