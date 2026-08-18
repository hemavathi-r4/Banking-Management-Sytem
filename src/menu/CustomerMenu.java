package menu;

import dao.AccountDAO;
import dao.TransactionDAO;
import exception.AccountFrozenException;
import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import model.Account;
import model.Customer;
import model.PageResult;
import model.Transaction;
import service.AuditLogService;

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
 * Stage 6: Implements "3. Withdraw" — same flow as deposit but decrements balance,
 *           uses InsufficientFundsException for balance validation.
 *
 * Future stages will implement options 4–5:
 *   4. Fund Transfer (Stage 7)
 *   5. Mini Statement (Stage 8)
 */
public class CustomerMenu {

    // DAO and Service instances
    private final AccountDAO     accountDAO     = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AuditLogService auditLogService = new AuditLogService();

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
                    withdraw(customer, scanner);
                    break;
                case "4":
                    fundTransfer(customer, scanner);
                    break;
                case "5":
                    viewMiniStatement(customer, scanner);
                    break;
                case "6":
                    System.out.println("\nLogging out... Returning to Main Menu.\n");
                    auditLogService.logSuccess(customer.getCustomerId(), customer.getEmail(), "LOGOUT", "Customer logged out");
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

        boolean success = accountDAO.createAccount(newAccount);
        if (success) {
            auditLogService.logSuccess(customer.getCustomerId(), customer.getEmail(), "ACCOUNT_CREATED",
                String.format("Opened %s account with initial deposit Rs. %,.2f", accountType, initialDeposit));
        } else {
            auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "ACCOUNT_CREATED",
                "Failed to open " + accountType + " account");
        }
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
        try {
            boolean success = transactionDAO.depositAmount(accountNo, depositAmount);

            if (success) {
                // Step 6: Fetch the refreshed balance from the DB to confirm
                double newBalance = transactionDAO.getUpdatedBalance(accountNo);

                auditLogService.logSuccess(customer.getCustomerId(), customer.getEmail(), "DEPOSIT",
                    String.format("Deposited Rs. %,.2f into account #%d", depositAmount, accountNo));

                System.out.println("\n==========================================");
                System.out.println("  ✓ Deposit Successful!");
                System.out.println("==========================================");
                System.out.printf( "  Amount Deposited  : Rs. %,.2f%n", depositAmount);
                System.out.println("  Account Number    : " + accountNo);
                if (newBalance >= 0) {
                    System.out.printf("  Updated Balance   : Rs. %,.2f%n", newBalance);
                }
                System.out.println("==========================================\n");
            } else {
                auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "DEPOSIT",
                    String.format("Deposit failed for account #%d", accountNo));
            }
            // If deposit failed, TransactionDAO already printed the error message.
        } catch (AccountFrozenException e) {
            auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "DEPOSIT",
                String.format("Deposit failed on account #%d: Account frozen/closed", accountNo));

            System.out.println("\n==========================================");
            System.out.println("  [!] Deposit Failed: Account is frozen/closed.");
            System.out.println("==========================================");
            System.out.println("  Reason    : " + e.getMessage());
            System.out.println("==========================================\n");
        }
    }

    // --------------------------------------------------
    // Stage 6: Withdraw
    // --------------------------------------------------
    /**
     * Handles the "Withdraw" flow.
     *
     * Flow:
     *   1. Fetch all accounts for this customer.
     *   2a. If no accounts → tell user to open one first.
     *   2b. If accounts exist → display numbered list → user picks one.
     *   3. Prompt for withdrawal amount (minimum Rs. 500), validate in a loop.
     *   4. Call transactionDAO.withdrawAmount() inside a try-catch for InsufficientFundsException:
     *        - UPDATE accounts SET balance = balance - amount
     *        - INSERT INTO transactions (WITHDRAWAL log)
     *   5. On success: fetch updated balance and display success confirmation.
     *   6. On InsufficientFundsException: display detailed failure info.
     *
     * @param customer the logged-in Customer object
     * @param scanner  the shared Scanner for reading console input
     */
    private void withdraw(Customer customer, Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("               WITHDRAWAL");
        System.out.println("------------------------------------------");

        // Step 1: Fetch customer's accounts
        List<Account> accounts = accountDAO.getAccountsByCustomerId(customer.getCustomerId());

        // Step 2a: No accounts — cannot withdraw
        if (accounts.isEmpty()) {
            System.out.println("  You do not have any accounts to withdraw from.");
            System.out.println("  Please open an account first (Option 1).\n");
            return;
        }

        // Step 2b: Display numbered list of available accounts
        System.out.println("  Select the account to withdraw from:\n");
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

        // Step 4: Withdrawal amount validation
        double withdrawAmount = 0;
        while (true) {
            System.out.print("  Enter amount to withdraw (Min Rs. 500): ");
            String amtInput = scanner.nextLine().trim();

            try {
                withdrawAmount = Double.parseDouble(amtInput);

                if (withdrawAmount < 500) {
                    System.out.println("  [!] Minimum withdrawal amount is Rs. 500. Please try again.");
                } else {
                    break; // Valid amount — exit loop
                }

            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid amount. Please enter a numeric value (e.g. 1000).");
            }
        }

        // Step 5: Execute the withdrawal inside try-catch for custom exception
        try {
            boolean success = transactionDAO.withdrawAmount(accountNo, withdrawAmount);

            if (success) {
                // Step 6: Fetch the refreshed balance from the DB to confirm
                double newBalance = transactionDAO.getUpdatedBalance(accountNo);

                auditLogService.logSuccess(customer.getCustomerId(), customer.getEmail(), "WITHDRAWAL",
                    String.format("Withdrew Rs. %,.2f from account #%d", withdrawAmount, accountNo));

                System.out.println("\n==========================================");
                System.out.println("  ✓ Withdrawal Successful!");
                System.out.println("==========================================");
                System.out.printf( "  Amount Withdrawn  : Rs. %,.2f%n", withdrawAmount);
                System.out.println("  Account Number    : " + accountNo);
                if (newBalance >= 0) {
                    System.out.printf("  Updated Balance   : Rs. %,.2f%n", newBalance);
                }
                System.out.println("==========================================\n");
            } else {
                auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "WITHDRAWAL",
                    String.format("Withdrawal failed for account #%d", accountNo));
            }
        } catch (InsufficientFundsException e) {
            auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "WITHDRAWAL",
                String.format("Withdrawal failed on account #%d: Insufficient funds", accountNo));

            // Handle the custom exception by showing a descriptive message
            System.out.println("\n==========================================");
            System.out.println("  [!] Withdrawal Failed: Insufficient funds.");
            System.out.println("==========================================");
            System.out.printf( "  Requested : Rs. %,.2f%n", e.getAmountRequested());
            System.out.printf( "  Available : Rs. %,.2f%n", e.getAvailableBalance());
            System.out.println("==========================================\n");
        } catch (AccountFrozenException e) {
            auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "WITHDRAWAL",
                String.format("Withdrawal failed on account #%d: Account frozen/closed", accountNo));

            System.out.println("\n==========================================");
            System.out.println("  [!] Withdrawal Failed: Account is frozen/closed.");
            System.out.println("==========================================");
            System.out.println("  Reason    : " + e.getMessage());
            System.out.println("==========================================\n");
        }
    }

    // --------------------------------------------------
    // Stage 7: Fund Transfer
    // --------------------------------------------------
    /**
     * Handles the "Fund Transfer" flow.
     *
     * Flow:
     *   1. Fetch all accounts for this customer.
     *   2a. If no accounts → tell user to open one first.
     *   2b. If accounts exist → display numbered list → user picks source account.
     *   3. Prompt for target account number.
     *   4. Prompt for transfer amount (minimum Rs. 100).
     *   5. Prompt for custom remarks (optional).
     *   6. Call transactionDAO.transferAmount() inside a try-catch for:
     *        - InsufficientFundsException
     *        - InvalidAccountException
     *   7. On success: fetch updated source account balance and display success confirmation.
     *
     * @param customer the logged-in Customer object
     * @param scanner  the shared Scanner for reading console input
     */
    private void fundTransfer(Customer customer, Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("             FUND TRANSFER");
        System.out.println("------------------------------------------");

        // Step 1: Fetch customer's accounts
        List<Account> accounts = accountDAO.getAccountsByCustomerId(customer.getCustomerId());

        // Step 2a: No accounts — cannot transfer
        if (accounts.isEmpty()) {
            System.out.println("  You do not have any accounts to transfer from.");
            System.out.println("  Please open an account first (Option 1).\n");
            return;
        }

        // Step 2b: Display numbered list of available accounts
        System.out.println("  Select the source account for the transfer:\n");
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            System.out.printf("  [%d] Account No: %d  |  Type: %s  |  Balance: Rs. %,.2f%n",
                    i + 1, acc.getAccountNo(), acc.getAccountType(), acc.getBalance());
        }
        System.out.println();

        // Step 3: Source Account selection with validation
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

        Account sourceAccount = accounts.get(selectedIndex);
        long fromAccountNo = sourceAccount.getAccountNo();

        // Step 4: Prompt for target account number
        long toAccountNo = -1;
        while (true) {
            System.out.print("  Enter destination account number: ");
            String targetInput = scanner.nextLine().trim();

            try {
                toAccountNo = Long.parseLong(targetInput);
                if (toAccountNo == fromAccountNo) {
                    System.out.println("  [!] Destination account cannot be the same as the source account.");
                } else if (toAccountNo <= 0) {
                    System.out.println("  [!] Account number must be positive.");
                } else {
                    break; // Exit loop
                }
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid account number. Please enter a numeric value.");
            }
        }

        // Step 5: Transfer amount validation
        double transferAmount = 0;
        while (true) {
            System.out.print("  Enter amount to transfer (Min Rs. 100): ");
            String amtInput = scanner.nextLine().trim();

            try {
                transferAmount = Double.parseDouble(amtInput);

                if (transferAmount < 100) {
                    System.out.println("  [!] Minimum transfer amount is Rs. 100. Please try again.");
                } else {
                    break; // Valid amount — exit loop
                }

            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid amount. Please enter a numeric value (e.g. 1500).");
            }
        }

        // Step 6: Remarks prompt (optional)
        System.out.print("  Enter remarks (optional): ");
        String remarks = scanner.nextLine().trim();

        // Step 7: Execute the transfer inside try-catch for custom exceptions
        try {
            boolean success = transactionDAO.transferAmount(fromAccountNo, toAccountNo, transferAmount, remarks);

            if (success) {
                // Fetch the refreshed balance of the source account to confirm
                double newBalance = transactionDAO.getUpdatedBalance(fromAccountNo);

                auditLogService.logSuccess(customer.getCustomerId(), customer.getEmail(), "TRANSFER",
                    String.format("Transferred Rs. %,.2f from #%d to #%d", transferAmount, fromAccountNo, toAccountNo));

                System.out.println("\n==========================================");
                System.out.println("  ✓ Fund Transfer Successful!");
                System.out.println("==========================================");
                System.out.printf( "  Amount Transferred : Rs. %,.2f%n", transferAmount);
                System.out.println("  From Account       : " + fromAccountNo);
                System.out.println("  To Account         : " + toAccountNo);
                if (newBalance >= 0) {
                    System.out.printf("  Updated Balance    : Rs. %,.2f%n", newBalance);
                }
                System.out.println("==========================================\n");
            } else {
                auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "TRANSFER",
                    String.format("Transfer failed from #%d to #%d", fromAccountNo, toAccountNo));
            }
        } catch (InsufficientFundsException e) {
            auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "TRANSFER",
                String.format("Transfer failed from #%d: Insufficient funds", fromAccountNo));

            System.out.println("\n==========================================");
            System.out.println("  [!] Transfer Failed: Insufficient funds.");
            System.out.println("==========================================");
            System.out.printf( "  Requested : Rs. %,.2f%n", e.getAmountRequested());
            System.out.printf( "  Available : Rs. %,.2f%n", e.getAvailableBalance());
            System.out.println("==========================================\n");
        } catch (InvalidAccountException e) {
            auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "TRANSFER",
                String.format("Transfer failed from #%d: Invalid destination account #%d", fromAccountNo, toAccountNo));

            System.out.println("\n==========================================");
            System.out.println("  [!] Transfer Failed: Invalid account details.");
            System.out.println("==========================================");
            System.out.println("  Reason    : " + e.getMessage());
            System.out.println("==========================================\n");
        } catch (AccountFrozenException e) {
            auditLogService.logFailure(customer.getCustomerId(), customer.getEmail(), "TRANSFER",
                String.format("Transfer failed from #%d: Account frozen/closed", fromAccountNo));

            System.out.println("\n==========================================");
            System.out.println("  [!] Transfer Failed: Account is frozen/closed.");
            System.out.println("==========================================");
            System.out.println("  Reason    : " + e.getMessage());
            System.out.println("==========================================\n");
        }
    }

    // --------------------------------------------------
    // Stage 8: Mini Statement
    // --------------------------------------------------
    /**
     * Handles the "Mini Statement" flow.
     *
     * Flow:
     *   1. Fetch all accounts for this customer.
     *   2a. If no accounts → tell user to open one first.
     *   2b. If accounts exist → display numbered list → user picks account.
     *   3. Call transactionDAO.getMiniStatement() to fetch the latest 5 transactions.
     *   4. Display transactions in a formatted table on the console.
     *
     * @param customer the logged-in Customer object
     * @param scanner  the shared Scanner for reading console input
     */
    private void viewMiniStatement(Customer customer, Scanner scanner) {
        System.out.println("\n------------------------------------------");
        System.out.println("             MINI STATEMENT");
        System.out.println("------------------------------------------");

        // Step 1: Fetch customer's accounts
        List<Account> accounts = accountDAO.getAccountsByCustomerId(customer.getCustomerId());

        // Step 2a: No accounts — cannot show statement
        if (accounts.isEmpty()) {
            System.out.println("  You do not have any accounts to view statements for.");
            System.out.println("  Please open an account first (Option 1).\n");
            return;
        }

        // Step 2b: Display numbered list of available accounts
        System.out.println("  Select the account to view statement for:\n");
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

        // Stage 14 — Paginated and Filtered Transaction Viewer
        int page = 1;
        int pageSize = 5;
        String typeFilter = null;
        Double minAmt = null;
        Double maxAmt = null;

        boolean viewing = true;
        while (viewing) {
            PageResult<Transaction> pageResult = transactionDAO.getPaginatedTransactionsForAccount(
                accountNo, page, pageSize, typeFilter, minAmt, maxAmt, null, null
            );

            System.out.println("\n====================================================================================================");
            System.out.printf("  TRANSACTION HISTORY FOR ACCOUNT: %d  (Page %d of %d, Total: %d)%n",
                    accountNo, pageResult.getCurrentPage(), Math.max(1, pageResult.getTotalPages()), pageResult.getTotalRecords());
            if (typeFilter != null || minAmt != null || maxAmt != null) {
                System.out.printf("  [Active Filters -> Type: %s | Min: %s | Max: %s]%n",
                        typeFilter == null ? "ALL" : typeFilter,
                        minAmt == null ? "None" : String.format("Rs. %,.2f", minAmt),
                        maxAmt == null ? "None" : String.format("Rs. %,.2f", maxAmt));
            }
            System.out.println("====================================================================================================");

            if (pageResult.getRecords().isEmpty()) {
                System.out.println("  No matching transactions found.\n");
            } else {
                System.out.printf("  %-6s | %-19s | %-12s | %-14s | %-30s%n",
                        "TX ID", "Date & Time", "Type", "Amount", "Remarks");
                System.out.println("  --------------------------------------------------------------------------------------------------");

                for (Transaction tx : pageResult.getRecords()) {
                    String type = tx.getTransactionType();
                    String amountStr = String.format("Rs. %,.2f", tx.getAmount());
                    String description = tx.getRemarks();

                    if (type.equals("TRANSFER")) {
                        if (tx.getFromAccount() == accountNo) {
                            description = String.format("Transfer to Acc: %d (%s)", tx.getToAccount(), tx.getRemarks());
                        } else if (tx.getToAccount() == accountNo) {
                            description = String.format("Transfer from Acc: %d (%s)", tx.getFromAccount(), tx.getRemarks());
                        }
                    }

                    System.out.printf("  %-6d | %-19s | %-12s | %-14s | %-30s%n",
                            tx.getTransactionId(),
                            tx.getTransactionTime(),
                            type,
                            amountStr,
                            description);
                }
            }
            System.out.println("====================================================================================================");
            System.out.println("  Controls: [N] Next Page | [P] Previous Page | [F] Apply Filter | [C] Clear Filter | [B] Back");
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

                    System.out.print("  Enter min amount (or press Enter to skip): ");
                    String minStr = scanner.nextLine().trim();
                    try { minAmt = minStr.isEmpty() ? null : Double.parseDouble(minStr); } catch (Exception e) { minAmt = null; }

                    System.out.print("  Enter max amount (or press Enter to skip): ");
                    String maxStr = scanner.nextLine().trim();
                    try { maxAmt = maxStr.isEmpty() ? null : Double.parseDouble(maxStr); } catch (Exception e) { maxAmt = null; }

                    page = 1; // Reset to first page on filter change
                    break;
                case "C":
                    typeFilter = null;
                    minAmt = null;
                    maxAmt = null;
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
