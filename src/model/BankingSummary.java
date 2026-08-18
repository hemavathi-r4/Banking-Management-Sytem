package model;

/**
 * BankingSummary - DTO for the Overall Banking Summary report.
 *
 * STAGE 17 — BANKING REPORTS & ANALYTICS
 * ---------------------------------------
 * Holds all aggregated metrics for the complete bank summary report.
 * Populated by ReportsDAO and displayed by ReportsMenu.
 */
public class BankingSummary {

    private final int    totalCustomers;
    private final int    totalAccounts;
    private final double totalBalance;
    private final int    totalTransactions;
    private final int    savingsAccounts;
    private final int    currentAccounts;
    private final int    activeAccounts;
    private final int    frozenAccounts;
    private final int    closedAccounts;
    private final double avgBalancePerAccount;
    private final double totalDeposited;
    private final double totalWithdrawn;
    private final double totalTransferred;

    public BankingSummary(int totalCustomers, int totalAccounts, double totalBalance,
                          int totalTransactions, int savingsAccounts, int currentAccounts,
                          int activeAccounts, int frozenAccounts, int closedAccounts,
                          double avgBalancePerAccount, double totalDeposited,
                          double totalWithdrawn, double totalTransferred) {
        this.totalCustomers      = totalCustomers;
        this.totalAccounts       = totalAccounts;
        this.totalBalance        = totalBalance;
        this.totalTransactions   = totalTransactions;
        this.savingsAccounts     = savingsAccounts;
        this.currentAccounts     = currentAccounts;
        this.activeAccounts      = activeAccounts;
        this.frozenAccounts      = frozenAccounts;
        this.closedAccounts      = closedAccounts;
        this.avgBalancePerAccount = avgBalancePerAccount;
        this.totalDeposited      = totalDeposited;
        this.totalWithdrawn      = totalWithdrawn;
        this.totalTransferred    = totalTransferred;
    }

    public int    getTotalCustomers()      { return totalCustomers; }
    public int    getTotalAccounts()       { return totalAccounts; }
    public double getTotalBalance()        { return totalBalance; }
    public int    getTotalTransactions()   { return totalTransactions; }
    public int    getSavingsAccounts()     { return savingsAccounts; }
    public int    getCurrentAccounts()     { return currentAccounts; }
    public int    getActiveAccounts()      { return activeAccounts; }
    public int    getFrozenAccounts()      { return frozenAccounts; }
    public int    getClosedAccounts()      { return closedAccounts; }
    public double getAvgBalancePerAccount(){ return avgBalancePerAccount; }
    public double getTotalDeposited()      { return totalDeposited; }
    public double getTotalWithdrawn()      { return totalWithdrawn; }
    public double getTotalTransferred()    { return totalTransferred; }
}
