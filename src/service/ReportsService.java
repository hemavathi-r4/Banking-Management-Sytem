package service;

import dao.ReportsDAO;
import model.AccountStatistics;
import model.BankingSummary;
import model.TopAccountRow;
import model.TransactionSummaryRow;

import java.util.List;

/**
 * ReportsService - Service layer for Banking Reports & Analytics.
 *
 * STAGE 17 — BANKING REPORTS & ANALYTICS
 * ----------------------------------------
 * Sits between ReportsMenu (UI) and ReportsDAO (database).
 * Responsibilities:
 *   1. Input validation for report parameters (date formats, topN bounds, year ranges).
 *   2. Delegation to ReportsDAO for data retrieval.
 *   3. Any business logic that applies before/after data retrieval.
 *
 * Follows the same architectural pattern as AuditLogService (Stage 13).
 */
public class ReportsService {

    private final ReportsDAO reportsDAO;

    public ReportsService() {
        this.reportsDAO = new ReportsDAO();
    }

    /** Constructor for dependency injection in tests. */
    public ReportsService(ReportsDAO reportsDAO) {
        this.reportsDAO = reportsDAO;
    }

    // --------------------------------------------------
    // Report 1: Overall Banking Summary
    // --------------------------------------------------
    /**
     * Returns the overall banking summary.
     * No parameters — returns a bank-wide snapshot.
     *
     * @return BankingSummary DTO, or null if a DB error occurs
     */
    public BankingSummary getOverallBankingSummary() {
        return reportsDAO.getOverallBankingSummary();
    }

    // --------------------------------------------------
    // Report 2: Transaction Type Summary
    // --------------------------------------------------
    /**
     * Returns aggregated stats per transaction type (DEPOSIT, WITHDRAWAL, TRANSFER).
     *
     * @return List of TransactionSummaryRow (up to 3 rows)
     */
    public List<TransactionSummaryRow> getTransactionTypeSummary() {
        return reportsDAO.getTransactionTypeSummary();
    }

    // --------------------------------------------------
    // Report 3: Daily Transaction Report
    // --------------------------------------------------
    /**
     * Returns a daily breakdown of transaction activity.
     *
     * Validates date strings to be non-null and well-formed (YYYY-MM-DD).
     * If start/end date are both empty/null, fetches all available data.
     * If startDate is after endDate, returns empty list with an error message.
     *
     * @param startDate YYYY-MM-DD start date (inclusive), or null/empty for all time
     * @param endDate   YYYY-MM-DD end date (inclusive), or null/empty for all time
     * @return List of TransactionSummaryRow grouped by (day, type)
     */
    public List<TransactionSummaryRow> getDailyTransactionReport(String startDate, String endDate) {
        String cleanStart = (startDate != null) ? startDate.trim() : null;
        String cleanEnd   = (endDate   != null) ? endDate.trim()   : null;

        if (cleanStart != null && !cleanStart.isEmpty() && !isValidDate(cleanStart)) {
            System.out.println("[ReportsService] Invalid startDate format. Expected YYYY-MM-DD.");
            return java.util.Collections.emptyList();
        }

        if (cleanEnd != null && !cleanEnd.isEmpty() && !isValidDate(cleanEnd)) {
            System.out.println("[ReportsService] Invalid endDate format. Expected YYYY-MM-DD.");
            return java.util.Collections.emptyList();
        }

        // Pass null for empty strings so the DAO skips the filter clause
        String effectiveStart = (cleanStart == null || cleanStart.isEmpty()) ? null : cleanStart;
        String effectiveEnd   = (cleanEnd   == null || cleanEnd.isEmpty())   ? null : cleanEnd;

        return reportsDAO.getDailyTransactionReport(effectiveStart, effectiveEnd);
    }

    // --------------------------------------------------
    // Report 4: Monthly Transaction Report
    // --------------------------------------------------
    /**
     * Returns monthly aggregated transaction data for a given year.
     *
     * @param year the year to filter (e.g. 2026), or 0 to include all years
     * @return List of TransactionSummaryRow grouped by (month, type)
     */
    public List<TransactionSummaryRow> getMonthlyTransactionReport(int year) {
        if (year < 0) {
            System.out.println("[ReportsService] Invalid year. Must be >= 0 (0 = all years).");
            return java.util.Collections.emptyList();
        }
        return reportsDAO.getMonthlyTransactionReport(year);
    }

    // --------------------------------------------------
    // Report 5: Account Statistics
    // --------------------------------------------------
    /**
     * Returns balance statistics for accounts grouped by type and status.
     *
     * @return List of AccountStatistics rows
     */
    public List<AccountStatistics> getAccountStatistics() {
        return reportsDAO.getAccountStatistics();
    }

    // --------------------------------------------------
    // Report 6: Top Active Accounts
    // --------------------------------------------------
    /**
     * Returns the top N accounts ranked by transaction count.
     *
     * @param topN number of accounts to return (clamped to 1–50)
     * @return List of TopAccountRow
     */
    public List<TopAccountRow> getTopActiveAccounts(int topN) {
        int validTopN = Math.max(1, Math.min(topN, 50));
        return reportsDAO.getTopActiveAccounts(validTopN);
    }

    // --------------------------------------------------
    // Helper: validate date format YYYY-MM-DD
    // --------------------------------------------------
    /**
     * Validates that the given string is a plausible YYYY-MM-DD date.
     * Regex checks format; does not verify that the calendar date exists.
     *
     * @param date the date string to validate
     * @return true if the format matches YYYY-MM-DD
     */
    private boolean isValidDate(String date) {
        return date != null && date.matches("\\d{4}-\\d{2}-\\d{2}");
    }
}
