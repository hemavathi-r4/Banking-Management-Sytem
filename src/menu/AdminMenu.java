package menu;

import dao.AdminDAO;
import dao.TransactionDAO;
import model.AuditLog;
import model.Customer;
import model.PageResult;
import model.Transaction;
import service.AuditLogService;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * AdminMenu - Handles the Administrator Dashboard and interactions.
 */
public class AdminMenu {

    private final AdminDAO        adminDAO        = new AdminDAO();
    private final TransactionDAO  transactionDAO  = new TransactionDAO();
    private final AuditLogService auditLogService = new AuditLogService();

    /**
     * Prompts the administrator for credentials and authenticates.
     * 
     * @param scanner the shared Scanner for console inputs
     */
    public void showLoginForm(Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("               ADMIN LOGIN");
        System.out.println("------------------------------------------");
        System.out.print("  Enter Username : ");
        String username = scanner.nextLine().trim();
        System.out.print("  Enter Password : ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("\n  [ERROR] Fields cannot be empty. Returning to Main Menu.\n");
            auditLogService.logFailure(null, username.isEmpty() ? "ANONYMOUS_ADMIN" : username, "LOGIN", "Admin login failed: Empty fields");
            return;
        }

        boolean authenticated = adminDAO.authenticateAdmin(username, password);

        if (authenticated) {
            auditLogService.logSuccess(null, username, "LOGIN", "Admin logged in successfully");
            System.out.println("\n  ✓ Authentication successful! Logging into Admin Dashboard...");
            showDashboard(username, scanner);
        } else {
            auditLogService.logFailure(null, username, "LOGIN", "Admin login failed: Invalid credentials");
            System.out.println("\n  [ERROR] Invalid admin username or password.\n");
        }
    }

    /**
     * Drives the admin choice loop dashboard.
     * 
     * @param adminUsername the logged-in admin username
     * @param scanner the shared Scanner for inputs
     */
    private void showDashboard(String adminUsername, Scanner scanner) {
        boolean loggedIn = true;

        while (loggedIn) {
            printAdminDashboardMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    viewCustomers(scanner);
                    break;
                case "2":
                    searchCustomer(scanner);
                    break;
                case "3":
                    deleteCustomer(adminUsername, scanner);
                    break;
                case "4":
                    freezeAccount(adminUsername, scanner);
                    break;
                case "5":
                    activateAccount(adminUsername, scanner);
                    break;
                case "6":
                    viewTransactions(scanner);
                    break;
                case "7":
                    viewStatistics();
                    break;
                case "8":
                    viewAuditLogs(scanner);
                    break;
                case "9":
                    auditLogService.logSuccess(null, adminUsername, "LOGOUT", "Admin logged out");
                    System.out.println("\nLogging out... Returning to Main Menu.\n");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please enter 1-9.\n");
            }
        }
    }

    private void printAdminDashboardMenu() {
        System.out.println("------------------------------------------");
        System.out.println("            ADMINISTRATOR DASHBOARD");
        System.out.println("------------------------------------------");
        System.out.println("  1. View All Customers (Paginated)");
        System.out.println("  2. Search Customer");
        System.out.println("  3. Delete Customer");
        System.out.println("  4. Freeze Account");
        System.out.println("  5. Activate Account");
        System.out.println("  6. View All Transactions (Paginated)");
        System.out.println("  7. View Bank Statistics");
        System.out.println("  8. View System Audit Logs");
        System.out.println("  9. Logout");
        System.out.println("------------------------------------------");
    }

    private void viewCustomers(Scanner scanner) {
        int page = 1;
        int pageSize = 5;
        String search = null;

        boolean viewing = true;
        while (viewing) {
            PageResult<Customer> pageResult = adminDAO.getPaginatedCustomers(page, pageSize, search);

            System.out.println("\n----------------------------------------------------------------------------------------------------");
            System.out.printf("  REGISTERED CUSTOMERS  (Page %d of %d, Total: %d)%n",
                    pageResult.getCurrentPage(), Math.max(1, pageResult.getTotalPages()), pageResult.getTotalRecords());
            if (search != null && !search.isEmpty()) {
                System.out.printf("  [Active Filter -> Search: '%s']%n", search);
            }
            System.out.println("----------------------------------------------------------------------------------------------------");

            if (pageResult.getRecords().isEmpty()) {
                System.out.println("  No registered customers found.\n");
            } else {
                System.out.printf("  %-7s | %-20s | %-25s | %-15s | %-20s%n",
                        "Cust ID", "Name", "Email", "Phone", "Address");
                System.out.println("  --------------------------------------------------------------------------------------------------");

                for (Customer c : pageResult.getRecords()) {
                    System.out.printf("  %-7d | %-20s | %-25s | %-15s | %-20s%n",
                            c.getCustomerId(),
                            c.getName(),
                            c.getEmail(),
                            c.getPhone(),
                            (c.getAddress() == null || c.getAddress().isEmpty() ? "N/A" : c.getAddress()));
                }
            }
            System.out.println("----------------------------------------------------------------------------------------------------");
            System.out.println("  Controls: [N] Next Page | [P] Previous Page | [S] Search | [C] Clear Search | [B] Back");
            System.out.print("  Choice: ");
            String action = scanner.nextLine().trim().toUpperCase();

            switch (action) {
                case "N":
                    if (pageResult.hasNext()) page++;
                    else System.out.println("  [!] Already on the last page.");
                    break;
                case "P":
                    if (pageResult.hasPrevious()) page--;
                    else System.out.println("  [!] Already on the first page.");
                    break;
                case "S":
                    System.out.print("  Enter search query (ID, Name, Email, or Phone): ");
                    search = scanner.nextLine().trim();
                    page = 1;
                    break;
                case "C":
                    search = null;
                    page = 1;
                    System.out.println("  ✓ Search cleared.");
                    break;
                case "B":
                    viewing = false;
                    break;
                default:
                    System.out.println("  [!] Invalid choice. Enter N, P, S, C, or B.");
            }
        }
    }

    private void searchCustomer(Scanner scanner) {
        viewCustomers(scanner); // Reuses the paginated customer search view cleanly
    }

    private void deleteCustomer(String adminUsername, Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("               DELETE CUSTOMER");
        System.out.println("------------------------------------------");
        System.out.print("  Enter Customer ID to delete: ");
        String idInput = scanner.nextLine().trim();

        int customerId;
        try {
            customerId = Integer.parseInt(idInput);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid Customer ID. Must be a number.\n");
            return;
        }

        System.out.print("  WARNING: Deleting this customer will delete all associated accounts/transactions.\n" +
                           "  Are you sure you want to proceed? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("yes")) {
            System.out.println("\n  Deletion cancelled.\n");
            return;
        }

        boolean success = adminDAO.deleteCustomer(customerId);

        if (success) {
            auditLogService.logSuccess(null, adminUsername, "CUSTOMER_DELETED", "Admin deleted customer ID #" + customerId);
            System.out.println("\n==========================================");
            System.out.println("  ✓ Customer & Accounts Deleted successfully!");
            System.out.println("==========================================\n");
        } else {
            auditLogService.logFailure(null, adminUsername, "CUSTOMER_DELETED", "Admin failed to delete customer ID #" + customerId);
            System.out.println("\n  [!] Deletion failed. Customer ID may not exist.\n");
        }
    }

    private void freezeAccount(String adminUsername, Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("               FREEZE ACCOUNT");
        System.out.println("------------------------------------------");
        System.out.print("  Enter Account Number to freeze: ");
        String accInput = scanner.nextLine().trim();

        long accountNo;
        try {
            accountNo = Long.parseLong(accInput);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid Account Number. Must be numeric.\n");
            return;
        }

        if (!adminDAO.accountExists(accountNo)) {
            System.out.println("  [!] Account number " + accountNo + " does not exist in the system.\n");
            return;
        }

        boolean success = adminDAO.updateAccountStatus(accountNo, "FROZEN");

        if (success) {
            auditLogService.logSuccess(null, adminUsername, "ACCOUNT_FROZEN", "Admin froze account #" + accountNo);
            System.out.println("\n==========================================");
            System.out.println("  ✓ Account " + accountNo + " is now FROZEN.");
            System.out.println("==========================================\n");
        } else {
            auditLogService.logFailure(null, adminUsername, "ACCOUNT_FROZEN", "Admin failed to freeze account #" + accountNo);
            System.out.println("  [!] Operation failed.\n");
        }
    }

    private void activateAccount(String adminUsername, Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("              ACTIVATE ACCOUNT");
        System.out.println("------------------------------------------");
        System.out.print("  Enter Account Number to activate: ");
        String accInput = scanner.nextLine().trim();

        long accountNo;
        try {
            accountNo = Long.parseLong(accInput);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid Account Number. Must be numeric.\n");
            return;
        }

        if (!adminDAO.accountExists(accountNo)) {
            System.out.println("  [!] Account number " + accountNo + " does not exist in the system.\n");
            return;
        }

        boolean success = adminDAO.updateAccountStatus(accountNo, "ACTIVE");

        if (success) {
            auditLogService.logSuccess(null, adminUsername, "ACCOUNT_ACTIVATED", "Admin activated account #" + accountNo);
            System.out.println("\n==========================================");
            System.out.println("  ✓ Account " + accountNo + " is now ACTIVE.");
            System.out.println("==========================================\n");
        } else {
            auditLogService.logFailure(null, adminUsername, "ACCOUNT_ACTIVATED", "Admin failed to activate account #" + accountNo);
            System.out.println("  [!] Operation failed.\n");
        }
    }

    private void viewTransactions(Scanner scanner) {
        int page = 1;
        int pageSize = 10;
        String typeFilter = null;

        boolean viewing = true;
        while (viewing) {
            PageResult<Transaction> pageResult = transactionDAO.getPaginatedTransactionsForAccount(
                0, page, pageSize, typeFilter, null, null, null, null
            );

            System.out.println("\n------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("                                 GLOBAL SYSTEM TRANSACTIONS LOG  (Page %d of %d, Total: %d)%n",
                    pageResult.getCurrentPage(), Math.max(1, pageResult.getTotalPages()), pageResult.getTotalRecords());
            System.out.println("------------------------------------------------------------------------------------------------------------------------");

            if (pageResult.getRecords().isEmpty()) {
                System.out.println("  No transactions recorded in the bank database.\n");
            } else {
                System.out.printf("  %-6s | %-19s | %-12s | %-12s | %-12s | %-12s | %-30s%n",
                        "TX ID", "Date & Time", "Type", "Amount", "From Account", "To Account", "Remarks");
                System.out.println("  ----------------------------------------------------------------------------------------------------------------------");

                for (Transaction tx : pageResult.getRecords()) {
                    String fromStr = tx.getFromAccount() == 0 ? "N/A" : String.valueOf(tx.getFromAccount());
                    String toStr   = tx.getToAccount() == 0 ? "N/A" : String.valueOf(tx.getToAccount());
                    String amtStr  = String.format("Rs. %,.2f", tx.getAmount());

                    System.out.printf("  %-6d | %-19s | %-12s | %-12s | %-12s | %-12s | %-30s%n",
                            tx.getTransactionId(),
                            tx.getTransactionTime(),
                            tx.getTransactionType(),
                            amtStr,
                            fromStr,
                            toStr,
                            tx.getRemarks());
                }
            }
            System.out.println("------------------------------------------------------------------------------------------------------------------------");
            System.out.println("  Controls: [N] Next Page | [P] Previous Page | [F] Filter Type | [C] Clear Filter | [B] Back");
            System.out.print("  Choice: ");
            String action = scanner.nextLine().trim().toUpperCase();

            switch (action) {
                case "N":
                    if (pageResult.hasNext()) page++;
                    else System.out.println("  [!] Already on the last page.");
                    break;
                case "P":
                    if (pageResult.hasPrevious()) page--;
                    else System.out.println("  [!] Already on the first page.");
                    break;
                case "F":
                    System.out.print("  Enter type filter (DEPOSIT/WITHDRAWAL/TRANSFER or ALL): ");
                    String tf = scanner.nextLine().trim();
                    typeFilter = (tf.isEmpty() || tf.equalsIgnoreCase("ALL")) ? null : tf;
                    page = 1;
                    break;
                case "C":
                    typeFilter = null;
                    page = 1;
                    System.out.println("  ✓ Filter cleared.");
                    break;
                case "B":
                    viewing = false;
                    break;
                default:
                    System.out.println("  [!] Invalid choice. Enter N, P, F, C, or B.");
            }
        }
    }

    private void viewStatistics() {
        System.out.println("\n------------------------------------------");
        System.out.println("               BANK STATISTICS");
        System.out.println("------------------------------------------");

        Map<String, Object> stats = adminDAO.getBankStatistics();

        if (stats.isEmpty()) {
            System.out.println("  Failed to fetch bank statistics.\n");
            return;
        }

        int totalCust = (int) stats.getOrDefault("totalCustomers", 0);
        int totalAccs = (int) stats.getOrDefault("totalAccounts", 0);
        int totalTxs  = (int) stats.getOrDefault("totalTransactions", 0);
        int savings   = (int) stats.getOrDefault("savingsCount", 0);
        int currents  = (int) stats.getOrDefault("currentCount", 0);
        double bal    = (double) stats.getOrDefault("totalBalance", 0.0);

        System.out.printf( "  Total Customers      : %d%n", totalCust);
        System.out.printf( "  Total Accounts       : %d (%d Savings | %d Current)%n", totalAccs, savings, currents);
        System.out.printf( "  Total Bank Deposits  : Rs. %,.2f%n", bal);
        System.out.printf( "  Total Transactions   : %d%n", totalTxs);
        System.out.println("------------------------------------------\n");
    }

    // --------------------------------------------------
    // Method: Admin Audit Log Viewer (Stage 13 & Stage 14)
    // --------------------------------------------------
    /**
     * Interactive console UI for administrators to inspect system audit logs
     * with database-level searching, filtering, and pagination.
     *
     * @param scanner the shared Scanner
     */
    private void viewAuditLogs(Scanner scanner) {
        int page = 1;
        int pageSize = 10;
        String userFilter = null;
        String actionFilter = null;
        String statusFilter = null;

        boolean viewing = true;
        while (viewing) {
            PageResult<AuditLog> pageResult = auditLogService.getPaginatedLogs(
                page, pageSize, userFilter, actionFilter, statusFilter
            );

            System.out.println("\n------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("                                       SYSTEM AUDIT TRAIL LOGS  (Page %d of %d, Total: %d)%n",
                    pageResult.getCurrentPage(), Math.max(1, pageResult.getTotalPages()), pageResult.getTotalRecords());
            if (userFilter != null || actionFilter != null || statusFilter != null) {
                System.out.printf("  [Active Filters -> User: %s | Action: %s | Status: %s]%n",
                        userFilter == null ? "ALL" : userFilter,
                        actionFilter == null ? "ALL" : actionFilter,
                        statusFilter == null ? "ALL" : statusFilter);
            }
            System.out.println("------------------------------------------------------------------------------------------------------------------------");

            if (pageResult.getRecords().isEmpty()) {
                System.out.println("  No matching audit logs found.\n");
            } else {
                System.out.printf("  %-6s | %-19s | %-20s | %-18s | %-9s | %-35s%n",
                        "Log ID", "Timestamp", "User / Email", "Action", "Status", "Description");
                System.out.println("  ----------------------------------------------------------------------------------------------------------------------");

                for (AuditLog log : pageResult.getRecords()) {
                    String userStr = (log.getUsername() != null && !log.getUsername().isEmpty())
                            ? log.getUsername()
                            : (log.getUserId() != null ? "ID#" + log.getUserId() : "SYSTEM");

                    System.out.printf("  %-6d | %-19s | %-20s | %-18s | %-9s | %-35s%n",
                            log.getLogId(),
                            log.getTimestamp(),
                            userStr,
                            log.getAction(),
                            log.getStatus(),
                            log.getDescription());
                }
            }
            System.out.println("------------------------------------------------------------------------------------------------------------------------");
            System.out.println("  Controls: [N] Next Page | [P] Previous Page | [F] Filter | [C] Clear Filters | [B] Back");
            System.out.print("  Choice: ");
            String action = scanner.nextLine().trim().toUpperCase();

            switch (action) {
                case "N":
                    if (pageResult.hasNext()) page++;
                    else System.out.println("  [!] Already on the last page.");
                    break;
                case "P":
                    if (pageResult.hasPrevious()) page--;
                    else System.out.println("  [!] Already on the first page.");
                    break;
                case "F":
                    System.out.print("  Enter User ID/Username filter (or press Enter to skip): ");
                    String uf = scanner.nextLine().trim();
                    userFilter = uf.isEmpty() ? null : uf;

                    System.out.print("  Enter Action filter (e.g. LOGIN, DEPOSIT, WITHDRAWAL, TRANSFER or ALL): ");
                    String af = scanner.nextLine().trim();
                    actionFilter = (af.isEmpty() || af.equalsIgnoreCase("ALL")) ? null : af;

                    System.out.print("  Enter Status filter (SUCCESS, FAILURE or ALL): ");
                    String sf = scanner.nextLine().trim();
                    statusFilter = (sf.isEmpty() || sf.equalsIgnoreCase("ALL")) ? null : sf;

                    page = 1; // Reset to page 1 on new filter
                    break;
                case "C":
                    userFilter = null;
                    actionFilter = null;
                    statusFilter = null;
                    page = 1;
                    System.out.println("  ✓ Filters cleared.");
                    break;
                case "B":
                    viewing = false;
                    break;
                default:
                    System.out.println("  [!] Invalid choice. Enter N, P, F, C, or B.");
            }
        }
    }
}
