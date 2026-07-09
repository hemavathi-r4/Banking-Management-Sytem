package dao;

import database.DBConnection;
import model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountDAO - Data Access Object for Account-related database operations.
 *
 * WHAT IS A DAO?
 * DAO stands for Data Access Object. It's a design pattern where all
 * database-related code (SQL queries, inserts, lookups) lives in a single,
 * dedicated class. This keeps code organised — the model (Account.java)
 * only holds data, while the DAO handles all the database work.
 *
 * WHY USE PreparedStatement INSTEAD OF Statement?
 * 1. SECURITY: PreparedStatement prevents SQL injection attacks.
 * 2. PERFORMANCE: The database can reuse the compiled query plan.
 * 3. READABILITY: Cleaner code with '?' placeholders.
 */
public class AccountDAO {

    // --------------------------------------------------
    // Method 1: Create a new bank account
    // --------------------------------------------------
    /**
     * Inserts a new account into the 'accounts' table.
     *
     * We do NOT insert account_no — MySQL auto-generates it (starting at 1001).
     * We do NOT insert status from the user — we default to 'ACTIVE' for safety.
     *
     * SQL: INSERT INTO accounts (customer_id, account_type, balance, status)
     *      VALUES (?, ?, ?, ?)
     *
     * @param account the Account object containing the new account details
     * @return true if the account was created successfully, false otherwise
     */
    public boolean createAccount(Account account) {
        String sql = "INSERT INTO accounts (customer_id, account_type, balance, status) VALUES (?, ?, ?, ?)";

        // try-with-resources: Connection and PreparedStatement are auto-closed after the block
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Fill in the '?' placeholders with actual values
            pstmt.setInt(1, account.getCustomerId());
            pstmt.setString(2, account.getAccountType());
            pstmt.setDouble(3, account.getBalance());
            pstmt.setString(4, account.getStatus());

            // executeUpdate() is used for INSERT, UPDATE, DELETE queries.
            // It returns the number of rows affected (should be 1 on success).
            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("\n==========================================");
                System.out.println("  ✓ Account Opened Successfully!");
                System.out.println("==========================================");
                System.out.println("  Account Type : " + account.getAccountType());
                System.out.printf( "  Opening Balance : Rs. %,.2f%n", account.getBalance());
                System.out.println("  Status       : ACTIVE");
                System.out.println("==========================================\n");
                return true;
            }

        } catch (SQLException e) {
            // Handle any SQL errors gracefully — don't crash the application
            System.out.println("\n[ERROR] Could not create account due to a database error.");
            System.out.println("        Details: " + e.getMessage());
            System.out.println("        Please try again later.\n");
        }

        return false;
    }

    // --------------------------------------------------
    // Method 2: Fetch all accounts for a given customer
    // --------------------------------------------------
    /**
     * Returns a list of all accounts belonging to the specified customer.
     *
     * A customer may have more than one account (e.g., one SAVINGS and one CURRENT),
     * so we return a List<Account> instead of a single Account object.
     *
     * SQL: SELECT * FROM accounts WHERE customer_id = ?
     *
     * @param customerId the ID of the customer whose accounts we want to fetch
     * @return a List of Account objects (empty list if the customer has no accounts)
     */
    public List<Account> getAccountsByCustomerId(int customerId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE customer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);

            // executeQuery() is used for SELECT queries — returns a ResultSet (table of results)
            try (ResultSet rs = pstmt.executeQuery()) {
                // Each call to rs.next() moves to the next row in the result set
                while (rs.next()) {
                    // Reconstruct an Account object from the current row using the full constructor
                    Account account = new Account(
                        rs.getLong("account_no"),
                        rs.getInt("customer_id"),
                        rs.getString("account_type"),
                        rs.getDouble("balance"),
                        rs.getString("status")
                    );
                    accounts.add(account);
                }
            }

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Could not retrieve account details.");
            System.out.println("        Details: " + e.getMessage());
        }

        // Returns an empty list (not null) if the customer has no accounts.
        // Returning an empty list is safer — callers can just check .isEmpty().
        return accounts;
    }
}
