package dao;

import database.DBConnection;
import model.Customer;
import model.Transaction;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminDAO - Data Access Object for Admin-related database operations.
 */
public class AdminDAO {

    /**
     * Authenticates administrator credentials using BCrypt hash verification.
     *
     * STAGE 11 — SECURITY CHANGE:
     * Previously: SELECT 1 FROM admins WHERE username = ? AND password = ?
     *             (Plain-text comparison inside SQL — insecure)
     *
     * Now:        SELECT password FROM admins WHERE username = ?
     *             Then: PasswordUtil.verifyPassword(inputPassword, storedHash)
     *             (Hash verification happens in Java — never in SQL)
     *
     * GENERIC ERROR RESPONSE:
     * Both "wrong username" and "wrong password" produce the same false result.
     * The UI shows: "Invalid admin username or password." — not revealing which failed.
     *
     * @param username the input admin username
     * @param password the plain-text admin password entered at login
     * @return true if credentials are valid, false otherwise
     */
    public boolean authenticateAdmin(String username, String password) {
        // Guard: reject empty inputs without touching the database
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return false;
        }

        // Fetch the stored BCrypt hash by username only
        String sql = "SELECT password FROM admins WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    // STAGE 11: Verify password using BCrypt — never compare plain text
                    return PasswordUtil.verifyPassword(password, storedHash);
                }
                // Username not found — return false (same as wrong password)
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Admin authentication could not be completed.");
            return false;
        }
        return false;
    }

    /**
     * Retrieves all registered customers from the database.
     * 
     * @return List of Customer objects
     */
    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY customer_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Customer customer = new Customer(
                    rs.getInt("customer_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("password"),
                    rs.getString("address")
                );
                customers.add(customer);
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not retrieve customer list: " + e.getMessage());
        }

        return customers;
    }

    /**
     * Searches for customers matching a search term by ID, name, email, or phone.
     * 
     * @param searchTerm the search string
     * @return List of Customer objects matching the criteria
     */
    public List<Customer> searchCustomers(String searchTerm) {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE customer_id = ? OR name LIKE ? OR email LIKE ? OR phone LIKE ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int searchId = -1;
            try {
                searchId = Integer.parseInt(searchTerm);
            } catch (NumberFormatException ignored) {}

            pstmt.setInt(1, searchId);
            pstmt.setString(2, "%" + searchTerm + "%");
            pstmt.setString(3, "%" + searchTerm + "%");
            pstmt.setString(4, "%" + searchTerm + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Customer customer = new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("password"),
                        rs.getString("address")
                    );
                    customers.add(customer);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Customer search failed: " + e.getMessage());
        }

        return customers;
    }

    /**
     * Deletes a customer and all their accounts and transactions atomically.
     * 
     * @param customerId the customer ID to delete
     * @return true if deletion succeeded
     */
    public boolean deleteCustomer(int customerId) {
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction block

            // Step 1: Find all accounts for the customer
            List<Long> accountNumbers = new ArrayList<>();
            String getAccsSql = "SELECT account_no FROM accounts WHERE customer_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(getAccsSql)) {
                pstmt.setInt(1, customerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        accountNumbers.add(rs.getLong("account_no"));
                    }
                }
            }

            // Step 2: Delete transactions referencing those accounts
            String deleteTxSql = "DELETE FROM transactions WHERE from_account = ? OR to_account = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteTxSql)) {
                for (long accNo : accountNumbers) {
                    pstmt.setLong(1, accNo);
                    pstmt.setLong(2, accNo);
                    pstmt.executeUpdate();
                }
            }

            // Step 3: Delete the accounts
            String deleteAccsSql = "DELETE FROM accounts WHERE customer_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteAccsSql)) {
                pstmt.setInt(1, customerId);
                pstmt.executeUpdate();
            }

            // Step 4: Delete the customer record
            String deleteCustSql = "DELETE FROM customers WHERE customer_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteCustSql)) {
                pstmt.setInt(1, customerId);
                int rowsDeleted = pstmt.executeUpdate();
                
                if (rowsDeleted == 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit(); // Save changes permanently
            return true;

        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to delete customer: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
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

    /**
     * Updates account status (e.g. FROZEN or ACTIVE).
     * 
     * @param accountNo the account number to update
     * @param status    the new status ('ACTIVE', 'FROZEN', 'CLOSED')
     * @return true if status was updated successfully
     */
    public boolean updateAccountStatus(long accountNo, String status) {
        String sql = "UPDATE accounts SET status = ? WHERE account_no = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setLong(2, accountNo);

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.out.println("[ERROR] Account status update failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a specific account number exists in the system.
     * 
     * @param accountNo the account number
     * @return true if the account exists
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
            return false;
        }
    }

    /**
     * Retrieves all system transactions.
     * 
     * @return List of Transaction objects sorted by time descending
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY transaction_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

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
                transactions.add(tx);
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not retrieve transactions log: " + e.getMessage());
        }

        return transactions;
    }

    /**
     * Computes global bank statistics.
     * 
     * @return Map containing statistics key-value pairs
     */
    public Map<String, Object> getBankStatistics() {
        Map<String, Object> stats = new HashMap<>();

        String customersCountSql = "SELECT COUNT(*) FROM customers";
        String accountsCountSql   = "SELECT COUNT(*) FROM accounts";
        String totalBalanceSql    = "SELECT SUM(balance) FROM accounts";
        String totalTxCountSql    = "SELECT COUNT(*) FROM transactions";
        String savingsCountSql    = "SELECT COUNT(*) FROM accounts WHERE account_type = 'SAVINGS'";
        String currentCountSql    = "SELECT COUNT(*) FROM accounts WHERE account_type = 'CURRENT'";

        try (Connection conn = DBConnection.getConnection()) {

            // Get total customers
            try (PreparedStatement pstmt = conn.prepareStatement(customersCountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) stats.put("totalCustomers", rs.getInt(1));
            }

            // Get total accounts
            try (PreparedStatement pstmt = conn.prepareStatement(accountsCountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) stats.put("totalAccounts", rs.getInt(1));
            }

            // Get total balance
            try (PreparedStatement pstmt = conn.prepareStatement(totalBalanceSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) stats.put("totalBalance", rs.getDouble(1));
            }

            // Get total transactions
            try (PreparedStatement pstmt = conn.prepareStatement(totalTxCountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) stats.put("totalTransactions", rs.getInt(1));
            }

            // Get SAVINGS count
            try (PreparedStatement pstmt = conn.prepareStatement(savingsCountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) stats.put("savingsCount", rs.getInt(1));
            }

            // Get CURRENT count
            try (PreparedStatement pstmt = conn.prepareStatement(currentCountSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) stats.put("currentCount", rs.getInt(1));
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not compute bank statistics: " + e.getMessage());
        }

        return stats;
    }
}
