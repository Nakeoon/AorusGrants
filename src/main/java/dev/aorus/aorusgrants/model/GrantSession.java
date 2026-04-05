package dev.aorus.aorusgrants.model;

import java.util.UUID;

/**
 * Holds the state of a grant operation while the staff member navigates the menus.
 */
public class GrantSession {

    public enum Action {
        PROMOTE, DEMOTE
    }

    private final UUID staffUUID;
    private final UUID targetUUID;
    private final String targetName;
    private Action action;
    private String selectedGroup;
    private long createdAt;

    // Duration tracking (in minutes)
    private long totalMinutes = 0;
    private boolean permanent = false;

    // For duration display
    private int addedDays = 0;
    private int addedWeeks = 0;
    private int addedMonths = 0;

    // Pagination state
    private int currentPage = 0;

    public GrantSession(UUID staffUUID, UUID targetUUID, String targetName) {
        this.staffUUID = staffUUID;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.createdAt = System.currentTimeMillis();
    }

    // ── Duration builders ──────────────────────────────────────

    public void addDays(int days) {
        this.permanent = false;
        this.addedDays += days;
        this.totalMinutes += (long) days * 24 * 60;
    }

    public void addWeeks(int weeks) {
        this.permanent = false;
        this.addedWeeks += weeks;
        this.totalMinutes += (long) weeks * 7 * 24 * 60;
    }

    public void addMonths(int months) {
        this.permanent = false;
        this.addedMonths += months;
        this.totalMinutes += (long) months * 30 * 24 * 60;
    }

    public void setPermanent() {
        this.permanent = true;
        this.totalMinutes = 0;
        this.addedDays = 0;
        this.addedWeeks = 0;
        this.addedMonths = 0;
    }

    /**
     * Returns a human-readable summary of the selected duration.
     */
    public String getDurationDisplay() {
        if (permanent) return "Permanent";
        if (totalMinutes == 0) return "Not set";

        StringBuilder sb = new StringBuilder();
        if (addedMonths > 0) sb.append(addedMonths).append(" month(s)\n");
        if (addedWeeks > 0) sb.append("+ ").append(addedWeeks).append(" week(s)\n");
        if (addedDays > 0) sb.append("+ ").append(addedDays).append(" day(s)");
        return sb.toString().trim();
    }

    public boolean hasDuration() {
        return permanent || totalMinutes > 0;
    }

    // ── Getters / Setters ──────────────────────────────────────

    public UUID getStaffUUID() { return staffUUID; }

    public UUID getTargetUUID() { return targetUUID; }

    public String getTargetName() { return targetName; }

    public Action getAction() { return action; }

    public void setAction(Action action) { this.action = action; }

    public String getSelectedGroup() { return selectedGroup; }

    public void setSelectedGroup(String selectedGroup) { this.selectedGroup = selectedGroup; }

    public long getTotalMinutes() { return totalMinutes; }

    public boolean isPermanent() { return permanent; }

    public long getCreatedAt() { return createdAt; }

    public void refresh() { this.createdAt = System.currentTimeMillis(); }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int page) { this.currentPage = page; }

}
