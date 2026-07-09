package menu;

import model.Customer;
import java.util.Scanner;

/**
 * CustomerMenu - Handles the Customer Dashboard console UI after successful login.
 *
 * For Stage 3, this menu acts as a landing dashboard with stub options for:
 *   1. View Account Details (Stage 4)
 *   2. Deposit (Stage 5)
 *   3. Withdraw (Stage 6)
 *   4. Fund Transfer (Stage 7)
 *   5. Mini Statement (Stage 8)
 * And a functional option:
 *   6. Logout (returns to main menu)
 */
public class CustomerMenu {

    /**
     * Displays the customer dashboard and handles user interaction.
     *
     * @param customer the logged-in Customer object
     * @param scanner the shared Scanner for reading console input
     */
    public void showDashboard(Customer customer, Scanner scanner) {
        boolean loggedIn = true;

        while (loggedIn) {
            printDashboardMenu(customer.getName());
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println("\n>> View Account Details will be implemented in Stage 4.\n");
                    break;
                case "2":
                    System.out.println("\n>> Deposit will be implemented in Stage 5.\n");
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
