package model;

/**
 * TopAccountRow - DTO for a single row in the Top Active Accounts report.
 *
 * STAGE 17 — BANKING REPORTS & ANALYTICS
 * ---------------------------------------
 * Represents one account ranked by its total transaction count.
 * Populated by ReportsDAO.getTopActiveAccounts().
 */
public class TopAccountRow {

    private final long   accountNo;         // the account number
    private final int    customerId;        // the owning customer's ID
    private final String accountType;       // "SAVINGS" or "CURRENT"
    private final String status;            // "ACTIVE", "FROZEN", or "CLOSED"
    private final double currentBalance;    // current balance
    private final int    transactionCount;  // total number of transactions involving this account
    private final double totalVolume;       // total monetary volume (sum of all amounts)
    private final int    rank;              // rank position (1 = most active)

    public TopAccountRow(long accountNo, int customerId, String accountType,
                         String status, double currentBalance,
                         int transactionCount, double totalVolume, int rank) {
        this.accountNo        = accountNo;
        this.customerId       = customerId;
        this.accountType      = accountType;
        this.status           = status;
        this.currentBalance   = currentBalance;
        this.transactionCount = transactionCount;
        this.totalVolume      = totalVolume;
        this.rank             = rank;
    }

    public long   getAccountNo()        { return accountNo; }
    public int    getCustomerId()       { return customerId; }
    public String getAccountType()      { return accountType; }
    public String getStatus()           { return status; }
    public double getCurrentBalance()   { return currentBalance; }
    public int    getTransactionCount() { return transactionCount; }
    public double getTotalVolume()      { return totalVolume; }
    public int    getRank()             { return rank; }
}
