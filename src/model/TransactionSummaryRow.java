package model;

/**
 * TransactionSummaryRow - DTO for a single row in the transaction summary report.
 *
 * STAGE 17 — BANKING REPORTS & ANALYTICS
 * ---------------------------------------
 * Each row represents aggregated data for one transaction type (DEPOSIT,
 * WITHDRAWAL, or TRANSFER) over any given date range.
 *
 * Populated by ReportsDAO.getTransactionSummary() and
 * ReportsDAO.getDailyTransactionReport() / getMonthlyTransactionReport().
 */
public class TransactionSummaryRow {

    private final String transactionType;  // DEPOSIT, WITHDRAWAL, or TRANSFER
    private final int    count;            // number of transactions of this type
    private final double totalAmount;      // sum of all amounts
    private final double avgAmount;        // average amount per transaction
    private final double minAmount;        // smallest single transaction
    private final double maxAmount;        // largest single transaction
    private final String period;           // date or month label (optional, for time-based reports)

    /**
     * Full constructor — used by type + period reports.
     */
    public TransactionSummaryRow(String transactionType, int count, double totalAmount,
                                 double avgAmount, double minAmount, double maxAmount, String period) {
        this.transactionType = transactionType;
        this.count           = count;
        this.totalAmount     = totalAmount;
        this.avgAmount       = avgAmount;
        this.minAmount       = minAmount;
        this.maxAmount       = maxAmount;
        this.period          = period;
    }

    /**
     * Constructor without period — used for overall type summary (no date grouping).
     */
    public TransactionSummaryRow(String transactionType, int count, double totalAmount,
                                 double avgAmount, double minAmount, double maxAmount) {
        this(transactionType, count, totalAmount, avgAmount, minAmount, maxAmount, null);
    }

    public String getTransactionType() { return transactionType; }
    public int    getCount()           { return count; }
    public double getTotalAmount()     { return totalAmount; }
    public double getAvgAmount()       { return avgAmount; }
    public double getMinAmount()       { return minAmount; }
    public double getMaxAmount()       { return maxAmount; }
    public String getPeriod()          { return period; }
}
