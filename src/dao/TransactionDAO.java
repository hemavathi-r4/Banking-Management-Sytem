package dao;

import database.DBConnection;
import exception.InsufficientFundsException;
import exception.InvalidAccountException;

import model.Transaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * TransactionDAO - Data Access Object for Transaction-related database operations.
 *
 * This class handles all SQL operations that involve money movement:
 *   - Stage 5: Depositing money into an account.
 *   - Stage 6: Withdrawing money from an account (throws InsufficientFundsException).
 *   - Fetching the latest balance after any transaction.
 *
 * IMPORTANT CONCEPT — DATABASE TRANSACTIONS (not bank transactions):
 * A "database transaction" means a group of SQL statements that must ALL succeed
 * or ALL fail together. This ensures data consistency (atomicity).
 *
 * For a deposit, two SQL statements must run as one atomic unit:
 *   1. UPDATE accounts SET balance = balance + ? WHERE account_no = ?
 *   2. INSERT INTO transactions (...) VALUES (...)
 *
 * If only the first runs and the second fails, the balance changes but no record
 * is logged — this is a data integrity problem. By wrapping both in a database
 * transaction, we guarantee they both succeed or neither does.
 *
 * HOW WE CONTROL TRANSACTIONS IN JDBC:
 *   conn.setAutoCommit(false) — turns off auto-save after each statement.
 *   conn.commit()             — saves all pending statements permanently.
 *   conn.rollback()           — cancels all pending statements on error.
 */
public class TransactionDAO {

    // --------------------------------------------------
    // Method 1: Deposit money into an account
    // --------------------------------------------------
    /**
     * Deposits the given amount into the specified account.
     *
     * This method executes TWO SQL statements inside a single database transaction:
     *   Step A: UPDATE accounts SET balance = balance + ? WHERE account_no = ?
     *   Step B: INSERT INTO transactions (to_account, transaction_type, amount, remarks)
     *           VALUES (?, 'DEPOSIT', ?, 'Self deposit')
     *
     * If Step A succeeds but Step B fails, conn.rollback() reverses Step A.
     * Both changes are only permanently saved when conn.commit() is called.
     *
     * WHY balance = balance + amount (not just SET balance = amount)?
     * Because we don't want to replace the balance — we want to ADD to it.
     * MySQL's "SET balance = balance + ?" reads the current value and adds on top.
     *
     * @param accountNo the account number to deposit into
     * @param amount    the amount to deposit
     * @return true if both SQL statements succeeded and were committed, false otherwise
     */
    public boolean depositAmount(long accountNo, double amount) {

        String updateSql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
        String insertSql = "INSERT INTO transactions (to_account, transaction_type, amount, remarks) " +
                           "VALUES (?, 'DEPOSIT', ?, 'Self deposit')";

        // We need a single Connection object shared across both statements
        // so both can be committed or rolled back together.
        // We cannot use separate try-with-resources blocks for this — they would
        // create separate connections, which can't be part of the same transaction.
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();

            // STEP 1: Turn off auto-commit
            // By default, JDBC commits (saves) every SQL statement immediately.
            // We disable this so we can control when to commit manually.
            conn.setAutoCommit(false);

            // STEP 2: Update the account balance
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, amount);
                updateStmt.setLong(2, accountNo);

                int rowsUpdated = updateStmt.executeUpdate();

                if (rowsUpdated == 0) {
                    // No rows were updated — the account number doesn't exist
                    System.out.println("\n[ERROR] Account not found. Deposit aborted.\n");
                    conn.rollback(); // Nothing to roll back, but good practice
                    return false;
                }
            }

            // STEP 3: Log the deposit to the transactions table
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, accountNo);
                insertStmt.setDouble(2, amount);
                insertStmt.executeUpdate();
            }

            // STEP 4: Commit — permanently save both changes to the database
            conn.commit();
            return true;

        } catch (SQLException e) {
            // Something went wrong — roll back any partial changes
            System.out.println("\n[ERROR] Deposit failed due to a database error.");
            System.out.println("        Details: " + e.getMessage());
            System.out.println("        Your balance has NOT been changed.\n");

            try {
                if (conn != null) {
                    conn.rollback(); // Undo any partial changes made before the error
                }
            } catch (SQLException rollbackEx) {
                System.out.println("[ERROR] Rollback also failed: " + rollbackEx.getMessage());
            }

            return false;

        } finally {
            // STEP 5: Restore auto-commit and close the connection
            // The 'finally' block always runs, whether the try succeeded or an exception was thrown.
            // This ensures the connection is always cleaned up and auto-commit is restored
            // so future operations on this connection behave normally.
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Restore default behaviour
                    conn.close();             // Return connection to the pool / close it
                }
            } catch (SQLException closeEx) {
                System.out.println("[ERROR] Could not close connection: " + closeEx.getMessage());
            }
        }
    }

    // --------------------------------------------------
    // Method 2: Get the latest balance of an account
    // --------------------------------------------------
    /**
     * Fetches the current balance for the given account number from the database.
     *
     * WHY DO WE FETCH THE BALANCE AGAIN AFTER DEPOSIT?
     * After a successful deposit, we could just add the amount to our local variable.
     * But fetching from the DB gives us the ground-truth balance — useful if
     * concurrent operations could change the balance between our two steps.
     * It also confirms the UPDATE was saved correctly.
     *
     * SQL: SELECT balance FROM accounts WHERE account_no = ?
     *
     * @param accountNo the account number to look up
     * @return the current balance as a double, or -1.0 if the account is not found or an error occurs
     */
    public double getUpdatedBalance(long accountNo) {
        String sql = "SELECT balance FROM accounts WHERE account_no = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, accountNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // getDouble("balance") reads the DECIMAL(15,2) column as a Java double
                    return rs.getDouble("balance");
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not fetch updated balance: " + e.getMessage());
        }

        // Return -1.0 as a sentinel value meaning "balance could not be fetched"
        // The caller should check for this value before displaying it.
        return -1.0;
    }

    // --------------------------------------------------
    // Method 3: Withdraw money from an account
    // --------------------------------------------------
    /**
     * Withdraws the given amount from the specified account.
     *
     * This method introduces a KEY DIFFERENCE from depositAmount():
     * Before touching the database, it checks the account balance first.
     * If funds are insufficient, it throws InsufficientFundsException —
     * a custom checked exception — BEFORE any SQL runs. No rollback needed.
     *
     * WHY throw an exception instead of returning false?
     * - The method signature 'throws InsufficientFundsException' forces every caller
     *   to explicitly handle this failure case at compile time.
     * - The exception carries amountRequested and availableBalance fields, giving
     *   the UI (CustomerMenu) rich data to display a helpful error message.
     * - A plain 'return false' gives zero information about WHY it failed.
     *
     * If funds ARE sufficient, this method executes TWO SQL statements atomically:
     *   Step A: UPDATE accounts SET balance = balance - ? WHERE account_no = ?
     *   Step B: INSERT INTO transactions (from_account, transaction_type, amount, remarks)
     *           VALUES (?, 'WITHDRAWAL', ?, 'Self withdrawal')
     *
     * Note the key SQL difference vs. deposit:
     *   DEPOSIT:    SET balance = balance + ?   (add to account)
     *   WITHDRAWAL: SET balance = balance - ?   (subtract from account)
     *   DEPOSIT logs to: to_account column (money came IN to the account)
     *   WITHDRAWAL logs to: from_account column (money went OUT of the account)
     *
     * @param accountNo the account number to withdraw from
     * @param amount    the amount to withdraw
     * @return true if the withdrawal succeeded and was committed
     * @throws InsufficientFundsException if the account balance is less than the amount
     */
    public boolean withdrawAmount(long accountNo, double amount) throws InsufficientFundsException {

        // ------------------------------------------
        // PRE-CHECK: Verify sufficient funds BEFORE opening a transaction
        // ------------------------------------------
        // We fetch the balance first using getUpdatedBalance() — a separate,
        // read-only SELECT query. This keeps the balance check outside the
        // transaction scope, which is fine: if the balance check passes but
        // the UPDATE fails, the rollback will undo it safely.
        double currentBalance = getUpdatedBalance(accountNo);

        if (currentBalance < amount) {
            // Not enough funds — throw the custom exception.
            // Execution jumps immediately to the catch block in CustomerMenu.
            // No SQL has been run yet, so no rollback is needed.
            throw new InsufficientFundsException(amount, currentBalance);
        }

        // ------------------------------------------
        // SQL: Atomic balance decrement + transaction log
        // ------------------------------------------
        String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_no = ?";
        String insertSql = "INSERT INTO transactions (from_account, transaction_type, amount, remarks) " +
                           "VALUES (?, 'WITHDRAWAL', ?, 'Self withdrawal')";

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin atomic transaction

            // STEP 1: Decrement the account balance
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, amount);
                updateStmt.setLong(2, accountNo);

                int rowsUpdated = updateStmt.executeUpdate();

                if (rowsUpdated == 0) {
                    System.out.println("\n[ERROR] Account not found. Withdrawal aborted.\n");
                    conn.rollback();
                    return false;
                }
            }

            // STEP 2: Log the withdrawal to the transactions table
            // Note: 'from_account' is used here (not 'to_account') because
            //       money is leaving the account, not arriving.
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, accountNo);
                insertStmt.setDouble(2, amount);
                insertStmt.executeUpdate();
            }

            // STEP 3: Commit — permanently save both changes
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Withdrawal failed due to a database error.");
            System.out.println("        Details: " + e.getMessage());
            System.out.println("        Your balance has NOT been changed.\n");

            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("[ERROR] Rollback also failed: " + rollbackEx.getMessage());
            }

            return false;

        } finally {
            // Always restore auto-commit and close — same pattern as depositAmount()
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException closeEx) {
                System.out.println("[ERROR] Could not close connection: " + closeEx.getMessage());
            }
        }
    }

    // --------------------------------------------------
    // Method 4: Check if an account exists
    // --------------------------------------------------
    /**
     * Checks if the specified account number exists in the accounts table.
     *
     * @param accountNo the account number to check
     * @return true if the account exists, false otherwise
     */
    public boolean accountExists(long accountNo) {
        String sql = "SELECT 1 FROM accounts WHERE account_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, accountNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error checking if account exists: " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------
    // Method 5: Fund Transfer
    // --------------------------------------------------
    /**
     * Transfers money from one account to another atomically.
     *
     * This method runs THREE SQL statements inside a single database transaction:
     *   Step A: UPDATE accounts SET balance = balance - ? WHERE account_no = ? (from account)
     *   Step B: UPDATE accounts SET balance = balance + ? WHERE account_no = ? (to account)
     *   Step C: INSERT INTO transactions (from_account, to_account, transaction_type, amount, remarks)
     *           VALUES (?, ?, 'TRANSFER', ?, ?)
     *
     * If any step fails, the transaction is rolled back and the balances remain unchanged.
     *
     * @param fromAccountNo the source account number
     * @param toAccountNo   the destination account number
     * @param amount        the amount to transfer
     * @param remarks       custom remarks for the transfer
     * @return true if the transfer was successful
     * @throws InsufficientFundsException if the source account balance is less than the amount
     * @throws InvalidAccountException     if the target account does not exist
     */
    public boolean transferAmount(long fromAccountNo, long toAccountNo, double amount, String remarks)
            throws InsufficientFundsException, InvalidAccountException {

        // 1. Validate destination account is not the same as source account
        if (fromAccountNo == toAccountNo) {
            throw new InvalidAccountException(toAccountNo, "Destination account cannot be the same as the source account.");
        }

        // 2. Pre-check: Verify source account has sufficient funds
        double fromBalance = getUpdatedBalance(fromAccountNo);
        if (fromBalance < amount) {
            throw new InsufficientFundsException(amount, fromBalance);
        }

        // 3. Pre-check: Verify destination account exists
        if (!accountExists(toAccountNo)) {
            throw new InvalidAccountException(toAccountNo, "Destination account number " + toAccountNo + " does not exist.");
        }

        String debitSql = "UPDATE accounts SET balance = balance - ? WHERE account_no = ?";
        String creditSql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
        String insertSql = "INSERT INTO transactions (from_account, to_account, transaction_type, amount, remarks) " +
                           "VALUES (?, ?, 'TRANSFER', ?, ?)";

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction block

            // STEP 1: Debit the source account
            try (PreparedStatement debitStmt = conn.prepareStatement(debitSql)) {
                debitStmt.setDouble(1, amount);
                debitStmt.setLong(2, fromAccountNo);
                debitStmt.executeUpdate();
            }

            // STEP 2: Credit the destination account
            try (PreparedStatement creditStmt = conn.prepareStatement(creditSql)) {
                creditStmt.setDouble(1, amount);
                creditStmt.setLong(2, toAccountNo);
                creditStmt.executeUpdate();
            }

            // STEP 3: Log the transaction
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, fromAccountNo);
                insertStmt.setLong(2, toAccountNo);
                insertStmt.setDouble(3, amount);
                // Default to 'Fund Transfer' if remarks are empty or null
                insertStmt.setString(4, (remarks == null || remarks.trim().isEmpty()) ? "Fund Transfer" : remarks.trim());
                insertStmt.executeUpdate();
            }

            // Commit transaction
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Fund transfer failed due to a database error.");
            System.out.println("        Details: " + e.getMessage());
            System.out.println("        No money has been moved.\n");

            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.out.println("[ERROR] Rollback failed: " + rollbackEx.getMessage());
            }

            return false;

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException closeEx) {
                System.out.println("[ERROR] Could not close connection: " + closeEx.getMessage());
            }
        }
    }

    // --------------------------------------------------
    // Method 6: Get Mini Statement (Latest 5 Transactions)
    // --------------------------------------------------
    /**
     * Retrieves the latest 5 transactions associated with the given account number.
     *
     * @param accountNo the account number to retrieve transactions for
     * @return a List of Transaction objects sorted by transaction_time descending
     */
    public List<Transaction> getMiniStatement(long accountNo) {
        List<Transaction> miniStatement = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE from_account = ? OR to_account = ? " +
                     "ORDER BY transaction_time DESC LIMIT 5";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, accountNo);
            pstmt.setLong(2, accountNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction tx = new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getLong("from_account"),
                        rs.getLong("to_account"),
                        rs.getString("transaction_type"),
                        rs.getDouble("amount"),
                        rs.getString("transaction_time"),
                        rs.getString("remarks")
                    );
                    miniStatement.add(tx);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not fetch transaction history: " + e.getMessage());
        }

        return miniStatement;
    }
}
