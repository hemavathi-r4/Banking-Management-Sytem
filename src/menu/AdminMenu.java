package menu;

import dao.AdminDAO;
import model.Customer;
import model.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * AdminMenu - Handles the Administrator Dashboard and interactions.
 */
public class AdminMenu {

    private final AdminDAO adminDAO = new AdminDAO();

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
            return;
        }

        boolean authenticated = adminDAO.authenticateAdmin(username, password);

        if (authenticated) {
            System.out.println("\n  ✓ Authentication successful! Logging into Admin Dashboard...");
            showDashboard(scanner);
        } else {
            System.out.println("\n  [ERROR] Invalid admin username or password.\n");
        }
    }

    /**
     * Drives the admin choice loop dashboard.
     * 
     * @param scanner the shared Scanner for inputs
     */
    private void showDashboard(Scanner scanner) {
        boolean loggedIn = true;

        while (loggedIn) {
            printAdminDashboardMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    viewCustomers();
                    break;
                case "2":
                    searchCustomer(scanner);
                    break;
                case "3":
                    deleteCustomer(scanner);
                    break;
                case "4":
                    freezeAccount(scanner);
                    break;
                case "5":
                    activateAccount(scanner);
                    break;
                case "6":
                    viewTransactions();
                    break;
                case "7":
                    viewStatistics();
                    break;
                case "8":
                    System.out.println("\nLogging out... Returning to Main Menu.\n");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please enter 1-8.\n");
            }
        }
    }

    private void printAdminDashboardMenu() {
        System.out.println("------------------------------------------");
        System.out.println("            ADMINISTRATOR DASHBOARD");
        System.out.println("------------------------------------------");
        System.out.println("  1. View All Customers");
        System.out.println("  2. Search Customer");
        System.out.println("  3. Delete Customer");
        System.out.println("  4. Freeze Account");
        System.out.println("  5. Activate Account");
        System.out.println("  6. View All Transactions");
        System.out.println("  7. View Bank Statistics");
        System.out.println("  8. Logout");
        System.out.println("------------------------------------------");
    }

    private void viewCustomers() {
        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.println("                                      REGISTERED CUSTOMERS");
        System.out.println("----------------------------------------------------------------------------------------------------");

        List<Customer> customers = adminDAO.getAllCustomers();

        if (customers.isEmpty()) {
            System.out.println("  No registered customers in the system.\n");
            return;
        }

        System.out.printf("  %-7s | %-20s | %-25s | %-15s | %-20s%n",
                "Cust ID", "Name", "Email", "Phone", "Address");
        System.out.println("  --------------------------------------------------------------------------------------------------");

        for (Customer c : customers) {
            System.out.printf("  %-7d | %-20s | %-25s | %-15s | %-20s%n",
                    c.getCustomerId(),
                    c.getName(),
                    c.getEmail(),
                    c.getPhone(),
                    (c.getAddress() == null || c.getAddress().isEmpty() ? "N/A" : c.getAddress()));
        }
        System.out.println("----------------------------------------------------------------------------------------------------\n");
    }

    private void searchCustomer(Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("               SEARCH CUSTOMER");
        System.out.println("------------------------------------------");
        System.out.print("  Enter search query (ID, Name, Email, or Phone): ");
        String query = scanner.nextLine().trim();

        if (query.isEmpty()) {
            System.out.println("  [!] Query cannot be empty.\n");
            return;
        }

        List<Customer> customers = adminDAO.searchCustomers(query);

        if (customers.isEmpty()) {
            System.out.println("\n  [!] No matching customers found.\n");
            return;
        }

        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.printf("  %-7s | %-20s | %-25s | %-15s | %-20s%n",
                "Cust ID", "Name", "Email", "Phone", "Address");
        System.out.println("  --------------------------------------------------------------------------------------------------");

        for (Customer c : customers) {
            System.out.printf("  %-7d | %-20s | %-25s | %-15s | %-20s%n",
                    c.getCustomerId(),
                    c.getName(),
                    c.getEmail(),
                    c.getPhone(),
                    (c.getAddress() == null || c.getAddress().isEmpty() ? "N/A" : c.getAddress()));
        }
        System.out.println("----------------------------------------------------------------------------------------------------\n");
    }

    private void deleteCustomer(Scanner scanner) {
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
            System.out.println("\n==========================================");
            System.out.println("  ✓ Customer & Accounts Deleted successfully!");
            System.out.println("==========================================\n");
        } else {
            System.out.println("\n  [!] Deletion failed. Customer ID may not exist.\n");
        }
    }

    private void freezeAccount(Scanner scanner) {
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
            System.out.println("\n==========================================");
            System.out.println("  ✓ Account " + accountNo + " is now FROZEN.");
            System.out.println("==========================================\n");
        } else {
            System.out.println("  [!] Operation failed.\n");
        }
    }

    private void activateAccount(Scanner scanner) {
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
            System.out.println("\n==========================================");
            System.out.println("  ✓ Account " + accountNo + " is now ACTIVE.");
            System.out.println("==========================================\n");
        } else {
            System.out.println("  [!] Operation failed.\n");
        }
    }

    private void viewTransactions() {
        System.out.println("\n------------------------------------------------------------------------------------------------------------------------");
        System.out.println("                                                 GLOBAL SYSTEM TRANSACTIONS LOG");
        System.out.println("------------------------------------------------------------------------------------------------------------------------");

        List<Transaction> transactions = adminDAO.getAllTransactions();

        if (transactions.isEmpty()) {
            System.out.println("  No transactions recorded in the bank database.\n");
            return;
        }

        System.out.printf("  %-6s | %-19s | %-12s | %-12s | %-12s | %-12s | %-30s%n",
                "TX ID", "Date & Time", "Type", "Amount", "From Account", "To Account", "Remarks");
        System.out.println("  ----------------------------------------------------------------------------------------------------------------------");

        for (Transaction tx : transactions) {
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
        System.out.println("------------------------------------------------------------------------------------------------------------------------\n");
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
}
