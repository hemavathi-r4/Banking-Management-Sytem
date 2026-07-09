package menu;

import dao.CustomerDAO;
import model.Customer;
import java.util.Scanner;

/**
 * LoginMenu - Handles the console UI for Customer Login.
 *
 * This class displays the login form, collects customer credentials (email and password),
 * calls the CustomerDAO to authenticate, and displays either the customer dashboard or an error.
 */
public class LoginMenu {

    private CustomerDAO customerDAO;
    private CustomerMenu customerMenu;

    /**
     * Constructor — initializes CustomerDAO and CustomerMenu.
     */
    public LoginMenu() {
        this.customerDAO = new CustomerDAO();
        this.customerMenu = new CustomerMenu();
    }

    /**
     * Displays the login form and processes customer authentication.
     *
     * @param scanner the shared Scanner for reading console input
     */
    public void showLoginForm(Scanner scanner) {
        System.out.println("\n==========================================");
        System.out.println("              CUSTOMER LOGIN");
        System.out.println("==========================================\n");

        System.out.print("Enter your email      : ");
        String email = scanner.nextLine().trim();

        System.out.print("Enter your password   : ");
        String password = scanner.nextLine().trim();

        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            System.out.println("\n[ERROR] Email and Password are required fields.");
            System.out.println("        Login cancelled.\n");
            return;
        }

        // Attempt login via CustomerDAO
        Customer customer = customerDAO.loginCustomer(email, password);

        if (customer != null) {
            // Login successful -> show dashboard
            customerMenu.showDashboard(customer, scanner);
        } else {
            // Login failed -> print error
            System.out.println("\n[ERROR] Invalid email or password\n");
        }
    }
}
