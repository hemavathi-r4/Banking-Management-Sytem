package model;

/**
 * AccountStatistics - DTO for the Account Statistics report.
 *
 * STAGE 17 — BANKING REPORTS & ANALYTICS
 * ---------------------------------------
 * Holds a breakdown of account counts and balances grouped by
 * account type (SAVINGS / CURRENT) and account status (ACTIVE / FROZEN / CLOSED).
 *
 * Populated by ReportsDAO.getAccountStatistics().
 */
public class AccountStatistics {

    private final String accountType;    // "SAVINGS" or "CURRENT"
    private final String status;         // "ACTIVE", "FROZEN", or "CLOSED"
    private final int    count;          // number of accounts in this group
    private final double totalBalance;   // sum of balances in this group
    private final double avgBalance;     // average balance in this group
    private final double minBalance;     // minimum balance in this group
    private final double maxBalance;     // maximum balance in this group

    public AccountStatistics(String accountType, String status, int count,
                             double totalBalance, double avgBalance,
                             double minBalance, double maxBalance) {
        this.accountType  = accountType;
        this.status       = status;
        this.count        = count;
        this.totalBalance = totalBalance;
        this.avgBalance   = avgBalance;
        this.minBalance   = minBalance;
        this.maxBalance   = maxBalance;
    }

    public String getAccountType()  { return accountType; }
    public String getStatus()       { return status; }
    public int    getCount()        { return count; }
    public double getTotalBalance() { return totalBalance; }
    public double getAvgBalance()   { return avgBalance; }
    public double getMinBalance()   { return minBalance; }
    public double getMaxBalance()   { return maxBalance; }
}
