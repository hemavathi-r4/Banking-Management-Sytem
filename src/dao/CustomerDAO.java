package dao;

import database.DBConnection;
import model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * CustomerDAO - Data Access Object for Customer-related database operations.
 *
 * WHAT IS A DAO?
 * DAO stands for Data Access Object. It's a design pattern where we put all
 * database-related code (SQL queries, inserts, updates) inside a dedicated class.
 * This keeps our code organized — the model (Customer.java) only holds data,
 * while the DAO handles all the database work.
 *
 * WHY USE PreparedStatement INSTEAD OF Statement?
 * 1. SECURITY: PreparedStatement prevents SQL injection attacks.
 *    SQL injection is when a hacker types SQL code into an input field
 *    to manipulate your database. PreparedStatement escapes special characters.
 * 2. PERFORMANCE: The database can reuse the compiled query plan.
 * 3. READABILITY: Cleaner code with '?' placeholders instead of string concatenation.
 */
public class CustomerDAO {

    // --------------------------------------------------
    // Method 1: Register a new customer
    // --------------------------------------------------
    /**
     * Registers a new customer in the database.
     *
     * Steps:
     *   1. Check if the email already exists → show error if it does.
     *   2. Check if the phone already exists → show error if it does.
     *   3. If both are unique, insert the customer into the 'customers' table.
     *
     * @param customer the Customer object containing registration details
     * @return true if registration is successful, false otherwise
     */
    public boolean registerCustomer(Customer customer) {

        // Step 1: Check for duplicate email
        if (emailExists(customer.getEmail())) {
            System.out.println("\n[ERROR] This email is already registered: " + customer.getEmail());
            System.out.println("        Please use a different email address.\n");
            return false;
        }

        // Step 2: Check for duplicate phone
        if (phoneExists(customer.getPhone())) {
            System.out.println("\n[ERROR] This phone number is already registered: " + customer.getPhone());
            System.out.println("        Please use a different phone number.\n");
            return false;
        }

        // Step 3: Insert the customer into the database
        // The '?' marks are placeholders — we fill them in safely using setString().
        String sql = "INSERT INTO customers (name, email, phone, password, address) VALUES (?, ?, ?, ?, ?)";

        // try-with-resources: Connection and PreparedStatement are auto-closed
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Fill in the placeholders with actual values
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getEmail());
            pstmt.setString(3, customer.getPhone());
            pstmt.setString(4, customer.getPassword());
            pstmt.setString(5, customer.getAddress());

            // executeUpdate() is used for INSERT, UPDATE, DELETE queries.
            // It returns the number of rows affected.
            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("\n==========================================");
                System.out.println("  ✓ Registration Successful!");
                System.out.println("==========================================");
                System.out.println("  Welcome, " + customer.getName() + "!");
                System.out.println("  You can now log in with your email.");
                System.out.println("==========================================\n");
                return true;
            }

        } catch (SQLException e) {
            // Handle any SQL errors gracefully — don't crash the application
            System.out.println("\n[ERROR] Registration failed due to a database error.");
            System.out.println("        Details: " + e.getMessage());
            System.out.println("        Please try again later.\n");
        }

        return false;
    }

    // --------------------------------------------------
    // Method 2: Check if an email already exists
    // --------------------------------------------------
    /**
     * Checks whether the given email is already registered in the database.
     *
     * HOW IT WORKS:
     * We run a SELECT COUNT(*) query. If the count is greater than 0,
     * it means someone already registered with this email.
     *
     * @param email the email address to check
     * @return true if the email already exists, false otherwise
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM customers WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            // executeQuery() is used for SELECT queries.
            // It returns a ResultSet — a table of results.
            try (ResultSet rs = pstmt.executeQuery()) {
                // Move to the first (and only) row of results
                if (rs.next()) {
                    // getInt(1) gets the value of the first column (the COUNT)
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not check email: " + e.getMessage());
        }

        // If something went wrong, return false (we'll catch the error during insert)
        return false;
    }

    // --------------------------------------------------
    // Method 3: Check if a phone number already exists
    // --------------------------------------------------
    /**
     * Checks whether the given phone number is already registered in the database.
     *
     * Works the same way as emailExists() — uses SELECT COUNT(*) to check.
     *
     * @param phone the phone number to check
     * @return true if the phone number already exists, false otherwise
     */
    public boolean phoneExists(String phone) {
        String sql = "SELECT COUNT(*) FROM customers WHERE phone = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, phone);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not check phone: " + e.getMessage());
        }

        return false;
    }
}
