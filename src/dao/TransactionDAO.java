package dao;

import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * TransactionDAO - Data Access Object for Transaction-related database operations.
 *
 * This class handles all SQL operations that involve money movement:
 *   - Depositing money into an account.
 *   - Fetching the latest balance after a transaction.
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
}
