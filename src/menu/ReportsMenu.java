package menu;

import model.AccountStatistics;
import model.BankingSummary;
import model.TopAccountRow;
import model.TransactionSummaryRow;
import service.ReportsService;

import java.util.List;
import java.util.Scanner;

/**
 * ReportsMenu - Console UI for Banking Reports & Analytics (Stage 17).
 *
 * STAGE 17 — BANKING REPORTS & ANALYTICS
 * ----------------------------------------
 * Accessible only from the Admin Dashboard (option 10).
 * Customers have NO access to this menu — admin-only functionality.
 *
 * Available reports:
 *   1. Overall Banking Summary   — Bank-wide financial snapshot
 *   2. Transaction Type Summary  — Breakdown by DEPOSIT/WITHDRAWAL/TRANSFER
 *   3. Daily Transaction Report  — Activity grouped by calendar day
 *   4. Monthly Transaction Report — Activity grouped by month
 *   5. Account Statistics        — Accounts grouped by type and status
 *   6. Top Active Accounts       — Ranked by transaction count
 *   7. Back to Admin Dashboard
 *
 * Design follows the same scanner-loop pattern as AdminMenu and CustomerMenu.
 */
public class ReportsMenu {

    private final ReportsService reportsService = new ReportsService();

    /**
     * Displays the reports sub-menu and handles admin navigation.
     *
     * @param scanner the shared Scanner for console input
     */
    public void showReportsMenu(Scanner scanner) {
        boolean inReports = true;

        while (inReports) {
            printReportsMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": showOverallBankingSummary();                   break;
                case "2": showTransactionTypeSummary();                  break;
                case "3": showDailyTransactionReport(scanner);           break;
                case "4": showMonthlyTransactionReport(scanner);         break;
                case "5": showAccountStatistics();                       break;
                case "6": showTopActiveAccounts(scanner);                break;
                case "7":
                    System.out.println("\n  Returning to Admin Dashboard...\n");
                    inReports = false;
                    break;
                default:
                    System.out.println("\n  [!] Invalid choice. Please enter 1–7.\n");
            }
        }
    }

    // --------------------------------------------------
    private void printReportsMenu() {
        System.out.println("\n==========================================");
        System.out.println("       BANKING REPORTS & ANALYTICS");
        System.out.println("==========================================");
        System.out.println("  1. Overall Banking Summary");
        System.out.println("  2. Transaction Type Summary");
        System.out.println("  3. Daily Transaction Report");
        System.out.println("  4. Monthly Transaction Report");
        System.out.println("  5. Account Statistics");
        System.out.println("  6. Top Active Accounts");
        System.out.println("  7. Back to Admin Dashboard");
        System.out.println("==========================================");
    }

    // --------------------------------------------------
    // Report 1: Overall Banking Summary
    // --------------------------------------------------
    private void showOverallBankingSummary() {
        System.out.println("\n------------------------------------------");
        System.out.println("         OVERALL BANKING SUMMARY");
        System.out.println("------------------------------------------");

        BankingSummary summary = reportsService.getOverallBankingSummary();

        if (summary == null) {
            System.out.println("  [ERROR] Could not retrieve banking summary.\n");
            return;
        }

        System.out.println("  CUSTOMERS & ACCOUNTS");
        System.out.printf("  %-28s : %d%n",  "Total Customers",         summary.getTotalCustomers());
        System.out.printf("  %-28s : %d%n",  "Total Accounts",          summary.getTotalAccounts());
        System.out.printf("  %-28s : Rs. %,.2f%n", "Total Bank Balance", summary.getTotalBalance());
        System.out.printf("  %-28s : Rs. %,.2f%n", "Avg Balance/Account", summary.getAvgBalancePerAccount());

        System.out.println();
        System.out.println("  ACCOUNT BREAKDOWN BY TYPE");
        System.out.printf("  %-28s : %d%n", "Savings Accounts",        summary.getSavingsAccounts());
        System.out.printf("  %-28s : %d%n", "Current Accounts",        summary.getCurrentAccounts());

        System.out.println();
        System.out.println("  ACCOUNT BREAKDOWN BY STATUS");
        System.out.printf("  %-28s : %d%n", "Active Accounts",         summary.getActiveAccounts());
        System.out.printf("  %-28s : %d%n", "Frozen Accounts",         summary.getFrozenAccounts());
        System.out.printf("  %-28s : %d%n", "Closed Accounts",         summary.getClosedAccounts());

        System.out.println();
        System.out.println("  TRANSACTION VOLUMES");
        System.out.printf("  %-28s : %d%n",       "Total Transactions",  summary.getTotalTransactions());
        System.out.printf("  %-28s : Rs. %,.2f%n", "Total Deposited",    summary.getTotalDeposited());
        System.out.printf("  %-28s : Rs. %,.2f%n", "Total Withdrawn",    summary.getTotalWithdrawn());
        System.out.printf("  %-28s : Rs. %,.2f%n", "Total Transferred",  summary.getTotalTransferred());
        System.out.println("------------------------------------------\n");
    }

    // --------------------------------------------------
    // Report 2: Transaction Type Summary
    // --------------------------------------------------
    private void showTransactionTypeSummary() {
        System.out.println("\n------------------------------------------");
        System.out.println("       TRANSACTION TYPE SUMMARY");
        System.out.println("------------------------------------------");

        List<TransactionSummaryRow> rows = reportsService.getTransactionTypeSummary();

        if (rows.isEmpty()) {
            System.out.println("  No transaction data available.\n");
            return;
        }

        System.out.printf("  %-12s %7s %16s %14s %14s %14s%n",
            "TYPE", "COUNT", "TOTAL (Rs.)", "AVG (Rs.)", "MIN (Rs.)", "MAX (Rs.)");
        System.out.println("  " + "-".repeat(80));

        for (TransactionSummaryRow row : rows) {
            System.out.printf("  %-12s %7d %,16.2f %,14.2f %,14.2f %,14.2f%n",
                row.getTransactionType(),
                row.getCount(),
                row.getTotalAmount(),
                row.getAvgAmount(),
                row.getMinAmount(),
                row.getMaxAmount()
            );
        }
        System.out.println("------------------------------------------\n");
    }

    // --------------------------------------------------
    // Report 3: Daily Transaction Report
    // --------------------------------------------------
    private void showDailyTransactionReport(Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("       DAILY TRANSACTION REPORT");
        System.out.println("------------------------------------------");
        System.out.println("  Enter date range (YYYY-MM-DD). Leave blank for all dates.");
        System.out.print("  Start Date (or ENTER to skip): ");
        String startDate = scanner.nextLine().trim();
        System.out.print("  End Date   (or ENTER to skip): ");
        String endDate = scanner.nextLine().trim();

        List<TransactionSummaryRow> rows = reportsService.getDailyTransactionReport(
            startDate.isEmpty() ? null : startDate,
            endDate.isEmpty()   ? null : endDate
        );

        if (rows.isEmpty()) {
            System.out.println("  No transactions found for the selected date range.\n");
            return;
        }

        System.out.printf("%n  %-12s %-12s %7s %14s %12s%n",
            "DATE", "TYPE", "COUNT", "TOTAL (Rs.)", "AVG (Rs.)");
        System.out.println("  " + "-".repeat(65));

        String lastPeriod = null;
        for (TransactionSummaryRow row : rows) {
            String period = row.getPeriod();
            // Print a blank separator line when the date changes (grouping effect)
            if (lastPeriod != null && !lastPeriod.equals(period)) {
                System.out.println();
            }
            lastPeriod = period;

            System.out.printf("  %-12s %-12s %7d %,14.2f %,12.2f%n",
                period,
                row.getTransactionType(),
                row.getCount(),
                row.getTotalAmount(),
                row.getAvgAmount()
            );
        }
        System.out.println("------------------------------------------\n");
    }

    // --------------------------------------------------
    // Report 4: Monthly Transaction Report
    // --------------------------------------------------
    private void showMonthlyTransactionReport(Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("      MONTHLY TRANSACTION REPORT");
        System.out.println("------------------------------------------");
        System.out.print("  Enter year (e.g. 2026) or 0 for all years: ");
        String yearInput = scanner.nextLine().trim();

        int year = 0;
        try {
            year = Integer.parseInt(yearInput);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid year input. Showing all years.");
        }

        List<TransactionSummaryRow> rows = reportsService.getMonthlyTransactionReport(year);

        if (rows.isEmpty()) {
            System.out.println("  No transaction data found.\n");
            return;
        }

        System.out.printf("%n  %-10s %-12s %7s %14s %12s%n",
            "MONTH", "TYPE", "COUNT", "TOTAL (Rs.)", "AVG (Rs.)");
        System.out.println("  " + "-".repeat(62));

        String lastPeriod = null;
        for (TransactionSummaryRow row : rows) {
            String period = row.getPeriod();
            if (lastPeriod != null && !lastPeriod.equals(period)) {
                System.out.println();
            }
            lastPeriod = period;

            System.out.printf("  %-10s %-12s %7d %,14.2f %,12.2f%n",
                period,
                row.getTransactionType(),
                row.getCount(),
                row.getTotalAmount(),
                row.getAvgAmount()
            );
        }
        System.out.println("------------------------------------------\n");
    }

    // --------------------------------------------------
    // Report 5: Account Statistics
    // --------------------------------------------------
    private void showAccountStatistics() {
        System.out.println("\n------------------------------------------");
        System.out.println("           ACCOUNT STATISTICS");
        System.out.println("------------------------------------------");

        List<AccountStatistics> rows = reportsService.getAccountStatistics();

        if (rows.isEmpty()) {
            System.out.println("  No account data available.\n");
            return;
        }

        System.out.printf("  %-10s %-8s %6s %14s %12s %12s %12s%n",
            "TYPE", "STATUS", "COUNT", "TOTAL (Rs.)", "AVG (Rs.)", "MIN (Rs.)", "MAX (Rs.)");
        System.out.println("  " + "-".repeat(80));

        String lastType = null;
        for (AccountStatistics row : rows) {
            // Print a blank separator line when account type changes
            if (lastType != null && !lastType.equals(row.getAccountType())) {
                System.out.println();
            }
            lastType = row.getAccountType();

            System.out.printf("  %-10s %-8s %6d %,14.2f %,12.2f %,12.2f %,12.2f%n",
                row.getAccountType(),
                row.getStatus(),
                row.getCount(),
                row.getTotalBalance(),
                row.getAvgBalance(),
                row.getMinBalance(),
                row.getMaxBalance()
            );
        }
        System.out.println("------------------------------------------\n");
    }

    // --------------------------------------------------
    // Report 6: Top Active Accounts
    // --------------------------------------------------
    private void showTopActiveAccounts(Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("         TOP ACTIVE ACCOUNTS");
        System.out.println("------------------------------------------");
        System.out.print("  How many top accounts to show? (1–50, default 10): ");
        String topNInput = scanner.nextLine().trim();

        int topN = 10;
        if (!topNInput.isEmpty()) {
            try {
                topN = Integer.parseInt(topNInput);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input. Showing top 10.");
            }
        }

        List<TopAccountRow> rows = reportsService.getTopActiveAccounts(topN);

        if (rows.isEmpty()) {
            System.out.println("  No transaction data available.\n");
            return;
        }

        System.out.printf("%n  %4s %10s %5s %-10s %-8s %14s %7s %14s%n",
            "RANK", "ACCOUNT", "CUST", "TYPE", "STATUS", "BALANCE (Rs.)", "TX CNT", "VOLUME (Rs.)");
        System.out.println("  " + "-".repeat(90));

        for (TopAccountRow row : rows) {
            System.out.printf("  %4d %10d %5d %-10s %-8s %,14.2f %7d %,14.2f%n",
                row.getRank(),
                row.getAccountNo(),
                row.getCustomerId(),
                row.getAccountType(),
                row.getStatus(),
                row.getCurrentBalance(),
                row.getTransactionCount(),
                row.getTotalVolume()
            );
        }
        System.out.println("------------------------------------------\n");
    }
}
