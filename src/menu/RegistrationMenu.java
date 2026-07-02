package menu;

import dao.CustomerDAO;
import model.Customer;

import java.util.Scanner;

/**
 * RegistrationMenu - Handles the console UI for Customer Registration.
 *
 * WHY A SEPARATE CLASS FOR THE MENU?
 * ----------------------------------
 * We could put all this code inside Main.java, but that would make Main.java
 * very long and hard to maintain. By putting the registration menu in its own
 * class, we follow the "Separation of Concerns" principle:
 *   - Main.java      → controls the main menu loop
 *   - RegistrationMenu → handles registration input/output
 *   - CustomerDAO     → handles database operations
 *   - Customer        → holds customer data
 *
 * Each class has ONE clear responsibility.
 */
public class RegistrationMenu {

    // We create one instance of CustomerDAO to use for database operations.
    // This is created once and reused — no need to create a new one each time.
    private CustomerDAO customerDAO;

    /**
     * Constructor — initializes the CustomerDAO.
     */
    public RegistrationMenu() {
        this.customerDAO = new CustomerDAO();
    }

    // --------------------------------------------------
    // Main method: Show the registration form
    // --------------------------------------------------
    /**
     * Displays the registration form and collects user input.
     *
     * WHY DO WE PASS THE Scanner IN?
     * We don't create a new Scanner here because Main.java already has one.
     * Creating multiple Scanners on System.in can cause issues.
     * So we share the same Scanner across the application.
     *
     * @param scanner the shared Scanner for reading console input
     */
    public void showRegistrationForm(Scanner scanner) {
        System.out.println("\n==========================================");
        System.out.println("         CUSTOMER REGISTRATION");
        System.out.println("==========================================\n");

        // --------------------------------------------------
        // Collect input from the user
        // --------------------------------------------------

        // Name
        System.out.print("Enter your name       : ");
        String name = scanner.nextLine().trim();

        // Email
        System.out.print("Enter your email      : ");
        String email = scanner.nextLine().trim();

        // Phone
        System.out.print("Enter your phone      : ");
        String phone = scanner.nextLine().trim();

        // Password
        System.out.print("Enter your password   : ");
        String password = scanner.nextLine().trim();

        // Address
        System.out.print("Enter your address    : ");
        String address = scanner.nextLine().trim();

        // --------------------------------------------------
        // Basic input validation
        // --------------------------------------------------
        // We check that required fields are not empty before proceeding.
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            System.out.println("\n[ERROR] Name, Email, Phone, and Password are required fields.");
            System.out.println("        Registration cancelled.\n");
            return;
        }

        // --------------------------------------------------
        // Create a Customer object and register
        // --------------------------------------------------
        // We use the constructor that doesn't need customerId,
        // because the database will auto-generate it.
        Customer newCustomer = new Customer(name, email, phone, password, address);

        // Call the DAO to save the customer to the database.
        // The DAO handles duplicate checks and SQL operations.
        customerDAO.registerCustomer(newCustomer);
    }
}
