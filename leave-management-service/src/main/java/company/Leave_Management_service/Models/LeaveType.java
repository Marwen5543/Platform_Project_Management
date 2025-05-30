package company.Leave_Management_service.Models;

public enum LeaveType {
    VACATION("Vacation"),
    SICK("Sick Leave"),
    PERSONAL("Personal Leave"),
    MATERNITY("Maternity Leave"),
    PATERNITY("Paternity Leave"),
    BEREAVEMENT("Bereavement Leave");

    private final String displayName;

    LeaveType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
