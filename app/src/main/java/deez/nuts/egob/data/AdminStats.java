package deez.nuts.egob.data;

public class AdminStats {
    public int totalRequests;
    public int activeRequests;
    public int pendingVerifications;
    public int completedToday;
    public int completedTotal;
    public int rejectedTotal;
    public int citizens;
    public double compliancePercent;

    public String complianceText() {
        return String.format(java.util.Locale.US, "%.1f%%", compliancePercent);
    }
}
