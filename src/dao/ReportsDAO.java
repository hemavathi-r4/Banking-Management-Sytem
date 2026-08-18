package dao;

import database.DBConnection;
import model.AccountStatistics;
import model.BankingSummary;
import model.TopAccountRow;
import model.TransactionSummaryRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportsDAO - Data Access Object for Banking Reports & Analytics.
 *
 * STAGE 17 — BANKING REPORTS & ANALYTICS
 * ----------------------------------------
 * All SQL in this class uses aggregation functions (COUNT, SUM, AVG, MIN, MAX)
 * and GROUP BY to compute analytical reports from the existing transaction,
 * account, and customer tables. No new tables are required.
 *
 * Reports available:
 *   1. Overall Banking Summary    — Full snapshot of bank health
 *   2. Transaction Type Summary   — Breakdown by DEPOSIT / WITHDRAWAL / TRANSFER
 *   3. Daily Transaction Report   — Aggregated by calendar day
 *   4. Monthly Transaction Report — Aggregated by YYYY-MM
 *   5. Account Statistics         — Grouped by account_type and status
 *   6. Top Active Accounts        — Ranked by total transaction count
 *
 * All methods use PreparedStatement (parameterized queries) to prevent SQL injection.
 * Date parameters are validated at the service layer before reaching this DAO.
 */
public class ReportsDAO {

    // ============================================================
    // Report 1: Overall Banking Summary
    // ============================================================
    /**
     * Returns a comprehensive snapshot of the bank's financial state.
     *
     * Uses two separate queries:
     *   Query A: Account-level aggregates (balance totals, type/status counts)
     *   Query B: Transaction-level aggregates (per type volumes)
     *
     * Both queries use SQL aggregate functions and return a single row each,
     * making them very efficient regardless of database size.
     *
     * @return BankingSummary DTO with all bank-wide metrics, or null on DB error
     */
    public BankingSummary getOverallBankingSummary() {

        // Query A: Account and customer aggregates in a single UNION ALL query
        // (reuses the same pattern as the Stage 15 optimized getBankStatistics())
        String accountSql =
            "SELECT 'totalCustomers'  AS metric, CAST(COUNT(*) AS DECIMAL) AS value FROM customers " +
            "UNION ALL " +
            "SELECT 'totalAccounts',  CAST(COUNT(*) AS DECIMAL) FROM accounts " +
            "UNION ALL " +
            "SELECT 'totalBalance',   COALESCE(SUM(balance), 0) FROM accounts " +
            "UNION ALL " +
            "SELECT 'avgBalance',     COALESCE(AVG(balance), 0) FROM accounts " +
            "UNION ALL " +
            "SELECT 'savingsCount',   CAST(COUNT(*) AS DECIMAL) FROM accounts WHERE account_type = 'SAVINGS' " +
            "UNION ALL " +
            "SELECT 'currentCount',   CAST(COUNT(*) AS DECIMAL) FROM accounts WHERE account_type = 'CURRENT' " +
            "UNION ALL " +
            "SELECT 'activeCount',    CAST(COUNT(*) AS DECIMAL) FROM accounts WHERE status = 'ACTIVE' " +
            "UNION ALL " +
            "SELECT 'frozenCount',    CAST(COUNT(*) AS DECIMAL) FROM accounts WHERE status = 'FROZEN' " +
            "UNION ALL " +
            "SELECT 'closedCount',    CAST(COUNT(*) AS DECIMAL) FROM accounts WHERE status = 'CLOSED'";

        // Query B: Transaction volume by type
        // GROUP BY transaction_type produces 3 rows (DEPOSIT, WITHDRAWAL, TRANSFER)
        // each with the sum of amounts. We only need the three totals here.
        String txSql =
            "SELECT transaction_type, " +
            "       CAST(COUNT(*) AS DECIMAL) AS tx_count, " +
            "       COALESCE(SUM(amount), 0)  AS tx_total " +
            "FROM transactions " +
            "GROUP BY transaction_type";

        int    totalCustomers = 0, totalAccounts = 0;
        int    savingsCount = 0, currentCount = 0;
        int    activeCount = 0, frozenCount = 0, closedCount = 0;
        double totalBalance = 0, avgBalance = 0;
        double totalDeposited = 0, totalWithdrawn = 0, totalTransferred = 0;
        int    totalTransactions = 0;

        try (Connection conn = DBConnection.getConnection()) {

            // Run Query A
            try (PreparedStatement pstmt = conn.prepareStatement(accountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String metric = rs.getString("metric");
                    double value  = rs.getDouble("value");
                    switch (metric) {
                        case "totalCustomers": totalCustomers = (int) value; break;
                        case "totalAccounts":  totalAccounts  = (int) value; break;
                        case "totalBalance":   totalBalance   = value;       break;
                        case "avgBalance":     avgBalance     = value;       break;
                        case "savingsCount":   savingsCount   = (int) value; break;
                        case "currentCount":   currentCount   = (int) value; break;
                        case "activeCount":    activeCount    = (int) value; break;
                        case "frozenCount":    frozenCount    = (int) value; break;
                        case "closedCount":    closedCount    = (int) value; break;
                        default: break;
                    }
                }
            }

            // Run Query B
            try (PreparedStatement pstmt = conn.prepareStatement(txSql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String type  = rs.getString("transaction_type");
                    int    count = (int) rs.getDouble("tx_count");
                    double total = rs.getDouble("tx_total");
                    totalTransactions += count;
                    switch (type) {
                        case "DEPOSIT":    totalDeposited   = total; break;
                        case "WITHDRAWAL": totalWithdrawn   = total; break;
                        case "TRANSFER":   totalTransferred = total; break;
                        default: break;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReportsDAO] Error fetching banking summary: " + e.getMessage());
            return null;
        }

        return new BankingSummary(
            totalCustomers, totalAccounts, totalBalance, totalTransactions,
            savingsCount, currentCount,
            activeCount, frozenCount, closedCount,
            avgBalance, totalDeposited, totalWithdrawn, totalTransferred
        );
    }

    // ============================================================
    // Report 2: Transaction Type Summary (overall, no date filter)
    // ============================================================
    /**
     * Returns a summary row for each transaction type: DEPOSIT, WITHDRAWAL, TRANSFER.
     *
     * SQL: GROUP BY transaction_type with COUNT, SUM, AVG, MIN, MAX.
     * Returns up to 3 rows (one per distinct type in the data).
     *
     * @return List of TransactionSummaryRow, one per transaction type
     */
    public List<TransactionSummaryRow> getTransactionTypeSummary() {
        List<TransactionSummaryRow> rows = new ArrayList<>();

        String sql =
            "SELECT transaction_type, " +
            "       COUNT(*)     AS tx_count, " +
            "       SUM(amount)  AS total_amount, " +
            "       AVG(amount)  AS avg_amount, " +
            "       MIN(amount)  AS min_amount, " +
            "       MAX(amount)  AS max_amount " +
            "FROM transactions " +
            "GROUP BY transaction_type " +
            "ORDER BY FIELD(transaction_type, 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                rows.add(new TransactionSummaryRow(
                    rs.getString("transaction_type"),
                    rs.getInt("tx_count"),
                    rs.getDouble("total_amount"),
                    rs.getDouble("avg_amount"),
                    rs.getDouble("min_amount"),
                    rs.getDouble("max_amount")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[ReportsDAO] Error fetching transaction type summary: " + e.getMessage());
        }

        return rows;
    }

    // ============================================================
    // Report 3: Daily Transaction Report
    // ============================================================
    /**
     * Returns a per-day aggregated transaction report for a given date range.
     *
     * SQL: GROUP BY DATE(transaction_time) — produces one row per calendar day.
     * Each row contains a count and total per transaction_type for that day.
     *
     * WHY DATE(transaction_time)?
     * The transaction_time column is DATETIME. DATE() extracts only the date part,
     * allowing grouping by day regardless of the time component.
     * The composite index idx_transactions_type_time (Stage 15) helps this query.
     *
     * @param startDate  YYYY-MM-DD format start date (inclusive), or null for all time
     * @param endDate    YYYY-MM-DD format end date (inclusive), or null for all time
     * @return List of TransactionSummaryRow, one per (day, type) combination
     */
    public List<TransactionSummaryRow> getDailyTransactionReport(String startDate, String endDate) {
        List<TransactionSummaryRow> rows = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT DATE(transaction_time) AS period, " +
            "       transaction_type, " +
            "       COUNT(*)    AS tx_count, " +
            "       SUM(amount) AS total_amount, " +
            "       AVG(amount) AS avg_amount, " +
            "       MIN(amount) AS min_amount, " +
            "       MAX(amount) AS max_amount " +
            "FROM transactions " +
            "WHERE 1=1 "
        );

        List<String> params = new ArrayList<>();

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append("AND DATE(transaction_time) >= ? ");
            params.add(startDate.trim());
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append("AND DATE(transaction_time) <= ? ");
            params.add(endDate.trim());
        }

        sql.append("GROUP BY DATE(transaction_time), transaction_type ");
        sql.append("ORDER BY DATE(transaction_time) DESC, " +
                   "FIELD(transaction_type, 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER')");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TransactionSummaryRow(
                        rs.getString("transaction_type"),
                        rs.getInt("tx_count"),
                        rs.getDouble("total_amount"),
                        rs.getDouble("avg_amount"),
                        rs.getDouble("min_amount"),
                        rs.getDouble("max_amount"),
                        rs.getString("period")  // e.g., "2026-08-18"
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReportsDAO] Error fetching daily report: " + e.getMessage());
        }

        return rows;
    }

    // ============================================================
    // Report 4: Monthly Transaction Report
    // ============================================================
    /**
     * Returns a per-month aggregated transaction report for a given year (or all years).
     *
     * SQL: GROUP BY YEAR(transaction_time), MONTH(transaction_time), transaction_type.
     * Period label is formatted as "YYYY-MM" using DATE_FORMAT().
     *
     * @param year  the 4-digit year to filter by (e.g. 2026), or 0 for all years
     * @return List of TransactionSummaryRow, one per (month, type) combination
     */
    public List<TransactionSummaryRow> getMonthlyTransactionReport(int year) {
        List<TransactionSummaryRow> rows = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT DATE_FORMAT(transaction_time, '%Y-%m') AS period, " +
            "       transaction_type, " +
            "       COUNT(*)    AS tx_count, " +
            "       SUM(amount) AS total_amount, " +
            "       AVG(amount) AS avg_amount, " +
            "       MIN(amount) AS min_amount, " +
            "       MAX(amount) AS max_amount " +
            "FROM transactions " +
            "WHERE 1=1 "
        );

        boolean filterYear = (year > 0);
        if (filterYear) {
            sql.append("AND YEAR(transaction_time) = ? ");
        }

        sql.append("GROUP BY DATE_FORMAT(transaction_time, '%Y-%m'), transaction_type ");
        sql.append("ORDER BY period DESC, " +
                   "FIELD(transaction_type, 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER')");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            if (filterYear) {
                pstmt.setInt(1, year);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TransactionSummaryRow(
                        rs.getString("transaction_type"),
                        rs.getInt("tx_count"),
                        rs.getDouble("total_amount"),
                        rs.getDouble("avg_amount"),
                        rs.getDouble("min_amount"),
                        rs.getDouble("max_amount"),
                        rs.getString("period")   // e.g., "2026-08"
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReportsDAO] Error fetching monthly report: " + e.getMessage());
        }

        return rows;
    }

    // ============================================================
    // Report 5: Account Statistics
    // ============================================================
    /**
     * Returns account statistics grouped by account_type and status.
     *
     * SQL: GROUP BY account_type, status — produces up to 6 rows
     * (2 types × 3 statuses). Each row includes balance aggregates.
     *
     * The composite index idx_accounts_type (Stage 15) assists this query.
     *
     * @return List of AccountStatistics rows, one per (type, status) group
     */
    public List<AccountStatistics> getAccountStatistics() {
        List<AccountStatistics> rows = new ArrayList<>();

        String sql =
            "SELECT account_type, " +
            "       status, " +
            "       COUNT(*)          AS acct_count, " +
            "       COALESCE(SUM(balance), 0)  AS total_balance, " +
            "       COALESCE(AVG(balance), 0)  AS avg_balance, " +
            "       COALESCE(MIN(balance), 0)  AS min_balance, " +
            "       COALESCE(MAX(balance), 0)  AS max_balance " +
            "FROM accounts " +
            "GROUP BY account_type, status " +
            "ORDER BY account_type ASC, status ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                rows.add(new AccountStatistics(
                    rs.getString("account_type"),
                    rs.getString("status"),
                    rs.getInt("acct_count"),
                    rs.getDouble("total_balance"),
                    rs.getDouble("avg_balance"),
                    rs.getDouble("min_balance"),
                    rs.getDouble("max_balance")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[ReportsDAO] Error fetching account statistics: " + e.getMessage());
        }

        return rows;
    }

    // ============================================================
    // Report 6: Top Active Accounts
    // ============================================================
    /**
     * Returns the top N accounts ranked by their total transaction count.
     *
     * SQL: JOINs transactions with accounts; counts all transactions where
     * the account appears as either from_account OR to_account.
     * Also sums total monetary volume for additional insight.
     *
     * Uses a subquery with ROW_NUMBER() or a simple ORDER BY + LIMIT.
     * Since ROW_NUMBER() requires MySQL 8.0+, we compute rank in Java.
     *
     * Index used: idx_transactions_from_acc and idx_transactions_to_acc (Stage 14).
     *
     * @param topN  the number of top accounts to return (e.g., 5 or 10)
     * @return List of TopAccountRow, ranked from most to least active
     */
    public List<TopAccountRow> getTopActiveAccounts(int topN) {
        List<TopAccountRow> rows = new ArrayList<>();

        // This query combines from_account and to_account participation using UNION ALL
        // inside a subquery, then joins back to accounts for metadata.
        // COALESCE ensures accounts with no transactions still appear (but they won't
        // here because we INNER JOIN on transactions).
        String sql =
            "SELECT a.account_no, " +
            "       a.customer_id, " +
            "       a.account_type, " +
            "       a.status, " +
            "       a.balance          AS current_balance, " +
            "       COUNT(t.tx_id)     AS tx_count, " +
            "       SUM(t.tx_amount)   AS total_volume " +
            "FROM accounts a " +
            "JOIN ( " +
            "    SELECT from_account AS acct_no, transaction_id AS tx_id, amount AS tx_amount " +
            "    FROM transactions WHERE from_account IS NOT NULL " +
            "    UNION ALL " +
            "    SELECT to_account,   transaction_id, amount " +
            "    FROM transactions WHERE to_account IS NOT NULL " +
            ") t ON t.acct_no = a.account_no " +
            "GROUP BY a.account_no, a.customer_id, a.account_type, a.status, a.balance " +
            "ORDER BY tx_count DESC, total_volume DESC " +
            "LIMIT ?";

        int validTopN = Math.max(1, Math.min(topN, 50)); // cap at 50 for safety

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, validTopN);

            try (ResultSet rs = pstmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    rows.add(new TopAccountRow(
                        rs.getLong("account_no"),
                        rs.getInt("customer_id"),
                        rs.getString("account_type"),
                        rs.getString("status"),
                        rs.getDouble("current_balance"),
                        rs.getInt("tx_count"),
                        rs.getDouble("total_volume"),
                        rank++
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ReportsDAO] Error fetching top active accounts: " + e.getMessage());
        }

        return rows;
    }
}
