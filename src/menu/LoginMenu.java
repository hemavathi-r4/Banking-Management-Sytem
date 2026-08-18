package menu;

import dao.CustomerDAO;
import model.Customer;
import service.AuditLogService;
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
    private AuditLogService auditLogService;

    /**
     * Constructor — initializes CustomerDAO, CustomerMenu, and AuditLogService.
     */
    public LoginMenu() {
        this.customerDAO = new CustomerDAO();
        this.customerMenu = new CustomerMenu();
        this.auditLogService = new AuditLogService();
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
            auditLogService.logFailure(null, email.isEmpty() ? "ANONYMOUS" : email, "LOGIN", "Login cancelled: Empty fields");
            return;
        }

        // Attempt login via CustomerDAO
        Customer customer = customerDAO.loginCustomer(email, password);

        if (customer != null) {
            // Audit log: LOGIN - SUCCESS
            auditLogService.logSuccess(customer.getCustomerId(), customer.getEmail(), "LOGIN", "Customer logged in successfully");
            // Login successful -> show dashboard
            customerMenu.showDashboard(customer, scanner);
        } else {
            // Audit log: LOGIN - FAILURE
            auditLogService.logFailure(null, email, "LOGIN", "Failed login attempt: Invalid credentials");
            // Login failed -> print error
            System.out.println("\n[ERROR] Invalid email or password\n");
        }
    }
}
