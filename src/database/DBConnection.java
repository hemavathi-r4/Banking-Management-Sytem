package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection - Utility class to manage the MySQL database connection.
 *
 * WHY A SEPARATE CLASS?
 * ---------------------
 * Instead of writing connection code in every file, we keep it in one place.
 * If the database URL or credentials change, we only update this one file.
 * This is called the "Single Responsibility Principle" — each class does one job.
 *
 * HOW JDBC WORKS (simplified):
 * 1. Java loads the MySQL driver (a .jar file you add to your project).
 * 2. DriverManager.getConnection() uses the URL, username, and password
 *    to open a connection to your MySQL server.
 * 3. You use that Connection object to run SQL queries.
 * 4. When done, you close the connection to free resources.
 */
public class DBConnection {

    // --------------------------------------------------
    // Database configuration — update these if needed
    // --------------------------------------------------
    private static final String URL      = "jdbc:mysql://localhost:3306/bank_management";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Hemavathi@04";  // 

    /**
     * Opens and returns a new connection to the MySQL database.
     *
     * IMPORTANT: Every time you call this method, a NEW connection is created.
     * Always close the connection after use (preferably with try-with-resources).
     *
     * Example usage:
     *   try (Connection conn = DBConnection.getConnection()) {
     *       // use conn to run queries
     *   }
     *   // connection is automatically closed here
     *
     * @return a live Connection to the bank_management database
     * @throws SQLException if the connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
