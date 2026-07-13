import database.DBConnection;
import menu.LoginMenu;
import menu.RegistrationMenu;
import menu.AdminMenu;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Main - Entry point of the Banking Management System.
 *
 * For Stage 1, this class:
 *   1. Tests the database connection.
 *   2. Displays the main menu (skeleton — options will be wired in later stages).
 *
 * HOW TO RUN:
 *   1. Make sure MySQL is running and you've executed the SQL script.
 *   2. Make sure mysql-connector-j.jar is added to your project libraries.
 *   3. Run this file.
 */
public class Main {

    public static void main(String[] args) {

        // --------------------------------------------------
        // Step 1: Test the database connection at startup
        // --------------------------------------------------
        System.out.println("==========================================");
        System.out.println("   BANKING MANAGEMENT SYSTEM");
        System.out.println("==========================================");
        System.out.println();

        if (!testDatabaseConnection()) {
            System.out.println("[ERROR] Cannot connect to the database.");
            System.out.println("Please check:");
            System.out.println("  1. MySQL server is running.");
            System.out.println("  2. Database 'bank_management' exists (run the SQL script).");
            System.out.println("  3. Username/password in DBConnection.java are correct.");
            System.out.println("  4. mysql-connector-j.jar is added to the project.");
            System.out.println();
            System.out.println("Exiting...");
            return;  // Stop the program — no point continuing without a database
        }

        System.out.println("[OK] Database connection successful!");
        System.out.println();

        // --------------------------------------------------
        // Step 2: Show the main menu in a loop
        // --------------------------------------------------
        Scanner scanner = new Scanner(System.in);
        RegistrationMenu registrationMenu = new RegistrationMenu();  // Stage 2: Registration menu
        LoginMenu loginMenu = new LoginMenu();                        // Stage 3: Login menu
        AdminMenu adminMenu = new AdminMenu();                        // Stage 9: Admin menu
        boolean running = true;

        while (running) {
            printMainMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    registrationMenu.showRegistrationForm(scanner);  // Stage 2: Customer Registration
                    break;
                case "2":
                    loginMenu.showLoginForm(scanner);  // Stage 3: Customer Login
                    break;
                case "3":
                    adminMenu.showLoginForm(scanner);
                    break;
                case "4":
                    System.out.println("\nThank you for using the Banking Management System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please enter 1, 2, 3, or 4.\n");
            }
        }

        scanner.close();
    }

    /**
     * Prints the main menu to the console.
     */
    private static void printMainMenu() {
        System.out.println("------------------------------------------");
        System.out.println("              MAIN MENU");
        System.out.println("------------------------------------------");
        System.out.println("  1. Register");
        System.out.println("  2. Customer Login");
        System.out.println("  3. Admin Login");
        System.out.println("  4. Exit");
        System.out.println("------------------------------------------");
    }

    /**
     * Attempts to connect to the database to verify that the setup is correct.
     *
     * HOW try-with-resources WORKS:
     * The connection is declared inside try(...). Java automatically closes it
     * when the block finishes, even if an exception occurs. This prevents
     * resource leaks (open connections that never get closed).
     *
     * @return true if connection is successful, false otherwise
     */
    private static boolean testDatabaseConnection() {
        try (Connection conn = DBConnection.getConnection()) {
            // If we reach this line, the connection was successful.
            // conn.isValid(2) checks if the connection is alive within 2 seconds.
            return conn != null && conn.isValid(2);
        } catch (SQLException e) {
            // Print the error so the user knows what went wrong
            System.out.println("[DB ERROR] " + e.getMessage());
            return false;
        }
    }
}
