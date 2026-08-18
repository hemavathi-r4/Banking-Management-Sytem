import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * TestDBHelper - Utility class for creating and cleaning up test data in the database.
 *
 * STAGE 12 — JUNIT TESTING
 * -------------------------
 * Since this project's DAO layer is tightly coupled to JDBC (no dependency injection,
 * no mock-friendly interfaces), unit tests run against the REAL database.
 *
 * This helper class:
 *   1. Creates isolated test customers, accounts, and transactions before each test.
 *   2. Cleans up all test data after each test so tests don't interfere with each other.
 *
 * APPROACH:
 *   - Test data uses email addresses like "test_xyz@bmstest.com" to avoid collisions.
 *   - After every test (@After), all data inserted during that test is deleted.
 *   - Tests do NOT modify any pre-existing production data.
 *
 * WHY NOT USE AN IN-MEMORY DB?
 *   Switching to H2 in-memory would require significant refactoring (driver changes,
 *   SQL dialect differences). For this project level, real-DB testing is the
 *   most practical approach that preserves the existing architecture.
 */
public class TestDBHelper {

    // Unique prefix to identify test data for clean-up
    public static final String TEST_EMAIL_PREFIX = "bmstest_";
    public static final String TEST_EMAIL_DOMAIN = "@bmstest.internal";

    /**
     * Removes ALL test data from the database (transactions → accounts → customers).
     * Call this in @Before and @After to ensure a clean state.
     *
     * Deletion order respects foreign key constraints:
     *   transactions (references accounts) must be deleted before accounts.
     *   accounts (references customers) must be deleted before customers.
     */
    public static void cleanupTestData() {
        try (Connection conn = DBConnection.getConnection()) {
            // Step 1: Delete transactions referencing test accounts
            String deleteTestTxSql =
                "DELETE FROM transactions WHERE " +
                "from_account IN (SELECT account_no FROM accounts WHERE customer_id IN " +
                "  (SELECT customer_id FROM customers WHERE email LIKE ?)) " +
                "OR to_account IN (SELECT account_no FROM accounts WHERE customer_id IN " +
                "  (SELECT customer_id FROM customers WHERE email LIKE ?))";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteTestTxSql)) {
                pstmt.setString(1, "%" + TEST_EMAIL_DOMAIN);
                pstmt.setString(2, "%" + TEST_EMAIL_DOMAIN);
                pstmt.executeUpdate();
            }

            // Step 2: Delete audit logs referencing test customers/usernames
            String deleteTestAuditSql = "DELETE FROM audit_logs WHERE username LIKE ? OR user_id IN " +
                                        "(SELECT customer_id FROM customers WHERE email LIKE ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteTestAuditSql)) {
                pstmt.setString(1, "%" + TEST_EMAIL_DOMAIN);
                pstmt.setString(2, "%" + TEST_EMAIL_DOMAIN);
                pstmt.executeUpdate();
            }

            // Step 3: Delete test accounts
            String deleteTestAccsSql =
                "DELETE FROM accounts WHERE customer_id IN " +
                "(SELECT customer_id FROM customers WHERE email LIKE ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteTestAccsSql)) {
                pstmt.setString(1, "%" + TEST_EMAIL_DOMAIN);
                pstmt.executeUpdate();
            }

            // Step 4: Delete test customers
            String deleteTestCustSql = "DELETE FROM customers WHERE email LIKE ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteTestCustSql)) {
                pstmt.setString(1, "%" + TEST_EMAIL_DOMAIN);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("[TestDBHelper] Cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Inserts a test customer with a BCrypt-hashed password into the database.
     *
     * @param name     customer name
     * @param email    customer email (should use TEST_EMAIL_DOMAIN)
     * @param phone    unique phone number for this test customer
     * @param password the plain-text password (will be BCrypt hashed before insert)
     * @param address  address field
     * @return the auto-generated customer_id, or -1 if insertion fails
     */
    public static int insertTestCustomer(String name, String email, String phone,
                                         String password, String address) {
        String hashedPassword = util.PasswordUtil.hashPassword(password);
        String sql = "INSERT INTO customers (name, email, phone, password, address) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, phone);
            pstmt.setString(4, hashedPassword);
            pstmt.setString(5, address);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("[TestDBHelper] Failed to insert test customer: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Inserts a test bank account for the given customer.
     *
     * @param customerId  the customer_id of the account owner
     * @param accountType "SAVINGS" or "CURRENT"
     * @param balance     opening balance
     * @param status      "ACTIVE", "FROZEN", or "CLOSED"
     * @return the auto-generated account_no, or -1 if insertion fails
     */
    public static long insertTestAccount(int customerId, String accountType,
                                          double balance, String status) {
        String sql = "INSERT INTO accounts (customer_id, account_type, balance, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, customerId);
            pstmt.setString(2, accountType);
            pstmt.setDouble(3, balance);
            pstmt.setString(4, status);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("[TestDBHelper] Failed to insert test account: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Generates a unique test email address using a given label.
     *
     * @param label a short identifier for the test (e.g., "alice", "john1")
     * @return a unique test email like "bmstest_alice@bmstest.internal"
     */
    public static String testEmail(String label) {
        return TEST_EMAIL_PREFIX + label + TEST_EMAIL_DOMAIN;
    }

    /**
     * Generates a unique numeric phone string from a suffix number.
     * Produces 10-digit numbers starting at 9000000000 + suffix.
     *
     * @param suffix a small number to differentiate test accounts (0–9999)
     * @return a unique 13-char phone string (within 10-15 digit range)
     */
    public static String testPhone(int suffix) {
        return String.format("90%08d", suffix);
    }
}
