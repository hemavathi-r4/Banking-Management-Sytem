package menu;

import dao.AccountDAO;
import dao.TransactionDAO;
import model.Account;
import model.Customer;

import java.util.List;
import java.util.Scanner;

/**
 * CustomerMenu - Handles the Customer Dashboard console UI after successful login.
 *
 * Stage 3: Basic dashboard with stub options.
 * Stage 4: Implements "1. View Account Details" — fetches accounts, displays them,
 *           or prompts to open a SAVINGS/CURRENT account with min deposit Rs. 1000.
 * Stage 5: Implements "2. Deposit" — account selection, amount validation (min Rs. 500),
 *           atomic balance update + transaction log via TransactionDAO.
 *
 * Future stages will implement options 3–5:
 *   3. Withdraw     (Stage 6)
 *   4. Fund Transfer (Stage 7)
 *   5. Mini Statement (Stage 8)
 */
public class CustomerMenu {

    // DAO instances — instantiated once and reused across all dashboard interactions
    private final AccountDAO     accountDAO     = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    /**
     * Displays the customer dashboard and handles user interaction.
     *
     * @param customer the logged-in Customer object
     * @param scanner  the shared Scanner for reading console input
     */
    public void showDashboard(Customer customer, Scanner scanner) {
        boolean loggedIn = true;

        while (loggedIn) {
            printDashboardMenu(customer.getName());
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    viewAccountDetails(customer, scanner);
                    break;
                case "2":
                    deposit(customer, scanner);
                    break;
                case "3":
                    System.out.println("\n>> Withdraw will be implemented in Stage 6.\n");
                    break;
                case "4":
                    System.out.println("\n>> Fund Transfer will be implemented in Stage 7.\n");
                    break;
                case "5":
                    System.out.println("\n>> Mini Statement will be implemented in Stage 8.\n");
                    break;
                case "6":
                    System.out.println("\nLogging out... Returning to Main Menu.\n");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please enter 1, 2, 3, 4, 5, or 6.\n");
            }
        }
    }

    // --------------------------------------------------
    // Stage 4: View Account Details
    // --------------------------------------------------
    /**
     * Handles the "View Account Details" flow.
     *
     * Flow:
     *   1. Fetch all accounts belonging to this customer from the DB.
     *   2a. If accounts are found → display each account's details.
     *   2b. If no accounts found → offer to open a new account.
     *       - Prompt for account type (SAVINGS / CURRENT).
     *       - Prompt for initial deposit (minimum Rs. 1000).
     *       - Validate inputs and call AccountDAO.createAccount().
     *
     * @param customer the logged-in Customer object
     * @param scanner  the shared Scanner for reading console input
     */
    private void viewAccountDetails(Customer customer, Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("          VIEW ACCOUNT DETAILS");
        System.out.println("------------------------------------------");

        // Step 1: Fetch accounts for this customer from the database
        List<Account> accounts = accountDAO.getAccountsByCustomerId(customer.getCustomerId());

        // Step 2a: Customer already has one or more accounts — display them
        if (!accounts.isEmpty()) {
            System.out.println("  Accounts registered under your profile:\n");

            for (Account acc : accounts) {
                System.out.println("  ------------------------------------------");
                System.out.println("  Account Number : " + acc.getAccountNo());
                System.out.println("  Account Type   : " + acc.getAccountType());
                System.out.printf( "  Balance        : Rs. %,.2f%n", acc.getBalance());
                System.out.println("  Status         : " + acc.getStatus());
            }

            System.out.println("  ------------------------------------------\n");
            return;
        }

        // Step 2b: Customer has no accounts yet
        System.out.println("  You do not have a bank account registered.");
        System.out.print("  Would you like to open a new account? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();

        if (!response.equals("yes")) {
            System.out.println("\n  Returning to dashboard...\n");
            return;
        }

        // --- Account Type Selection ---
        String accountType = "";
        while (true) {
            System.out.print("  Enter account type (SAVINGS/CURRENT): ");
            accountType = scanner.nextLine().trim().toUpperCase();

            if (accountType.equals("SAVINGS") || accountType.equals("CURRENT")) {
                break; // Valid input — exit the loop
            }

            System.out.println("  [!] Invalid type. Please enter SAVINGS or CURRENT.");
        }

        // --- Initial Deposit Validation ---
        double initialDeposit = 0;
        while (true) {
            System.out.print("  Enter initial deposit amount (Min Rs. 1000): ");
            String depositInput = scanner.nextLine().trim();

            try {
                initialDeposit = Double.parseDouble(depositInput);

                if (initialDeposit < 1000) {
                    System.out.println("  [!] Minimum initial deposit is Rs. 1000. Please try again.");
                } else {
                    break; // Valid deposit — exit the loop
                }

            } catch (NumberFormatException e) {
                // The user typed something that is not a valid number
                System.out.println("  [!] Invalid amount. Please enter a numeric value (e.g. 5000).");
            }
        }

        // --- Create the Account ---
        // Status defaults to 'ACTIVE' for all newly opened accounts
        Account newAccount = new Account(
            customer.getCustomerId(),
            accountType,
            initialDeposit,
            "ACTIVE"
        );

        accountDAO.createAccount(newAccount);
    }

    // --------------------------------------------------
    // Stage 5: Deposit
    // --------------------------------------------------
    /**
     * Handles the "Deposit" flow.
     *
     * Flow:
     *   1. Fetch all accounts for this customer.
     *   2a. If no accounts → tell user to open one first.
     *   2b. If accounts exist → display numbered list → user picks one.
     *   3. Prompt for deposit amount (minimum Rs. 500), validate in a loop.
     *   4. Call transactionDAO.depositAmount() → atomically:
     *        - UPDATE accounts SET balance = balance + amount
     *        - INSERT INTO transactions (DEPOSIT log)
     *   5. Fetch updated balance and display success confirmation.
     *
     * @param customer the logged-in Customer object
     * @param scanner  the shared Scanner for reading console input
     */
    private void deposit(Customer customer, Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("               DEPOSIT");
        System.out.println("------------------------------------------");

        // Step 1: Fetch customer's accounts
        List<Account> accounts = accountDAO.getAccountsByCustomerId(customer.getCustomerId());

        // Step 2a: No accounts — cannot deposit
        if (accounts.isEmpty()) {
            System.out.println("  You do not have any accounts to deposit into.");
            System.out.println("  Please open an account first (Option 1).\n");
            return;
        }

        // Step 2b: Display numbered list of available accounts
        System.out.println("  Select the account to deposit into:\n");
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            System.out.printf("  [%d] Account No: %d  |  Type: %s  |  Balance: Rs. %,.2f%n",
                    i + 1, acc.getAccountNo(), acc.getAccountType(), acc.getBalance());
        }
        System.out.println();

        // Step 3: Account selection with validation
        int selectedIndex = -1;
        while (true) {
            System.out.print("  Enter choice (1" + (accounts.size() > 1 ? "-" + accounts.size() : "") + "): ");
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= accounts.size()) {
                    selectedIndex = choice - 1; // Convert to 0-based index
                    break;
                } else {
                    System.out.println("  [!] Please enter a number between 1 and " + accounts.size() + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input. Please enter a number.");
            }
        }

        Account selectedAccount = accounts.get(selectedIndex);
        long accountNo = selectedAccount.getAccountNo();

        // Step 4: Deposit amount validation
        double depositAmount = 0;
        while (true) {
            System.out.print("  Enter amount to deposit (Min Rs. 500): ");
            String amtInput = scanner.nextLine().trim();

            try {
                depositAmount = Double.parseDouble(amtInput);

                if (depositAmount < 500) {
                    System.out.println("  [!] Minimum deposit amount is Rs. 500. Please try again.");
                } else {
                    break; // Valid amount — exit loop
                }

            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid amount. Please enter a numeric value (e.g. 2000).");
            }
        }

        // Step 5: Execute the deposit (atomic: balance update + transaction log)
        boolean success = transactionDAO.depositAmount(accountNo, depositAmount);

        if (success) {
            // Step 6: Fetch the refreshed balance from the DB to confirm
            double newBalance = transactionDAO.getUpdatedBalance(accountNo);

            System.out.println("\n==========================================");
            System.out.println("  ✓ Deposit Successful!");
            System.out.println("==========================================");
            System.out.printf( "  Amount Deposited  : Rs. %,.2f%n", depositAmount);
            System.out.println("  Account Number    : " + accountNo);
            if (newBalance >= 0) {
                System.out.printf("  Updated Balance   : Rs. %,.2f%n", newBalance);
            }
            System.out.println("==========================================\n");
        }
        // If deposit failed, TransactionDAO already printed the error message.
    }

    // --------------------------------------------------
    // Dashboard Menu Printer
    // --------------------------------------------------
    /**
     * Prints the Customer Dashboard header and options.
     *
     * @param customerName the name of the logged-in customer
     */
    private void printDashboardMenu(String customerName) {
        System.out.println("------------------------------------------");
        System.out.println("            CUSTOMER DASHBOARD");
        System.out.println("------------------------------------------");
        System.out.println("  Welcome, " + customerName + "!");
        System.out.println("------------------------------------------");
        System.out.println("  1. View Account Details");
        System.out.println("  2. Deposit");
        System.out.println("  3. Withdraw");
        System.out.println("  4. Fund Transfer");
        System.out.println("  5. Mini Statement");
        System.out.println("  6. Logout");
        System.out.println("------------------------------------------");
    }
}
