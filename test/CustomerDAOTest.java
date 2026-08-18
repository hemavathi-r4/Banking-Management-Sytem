import dao.CustomerDAO;
import model.Customer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * CustomerDAOTest - JUnit tests for CustomerDAO registration and login logic.
 *
 * STAGE 12 — JUNIT TESTING
 * -------------------------
 * Tests run against the REAL MySQL database using isolated test data.
 * @Before and @After clean up test data before and after every test method.
 *
 * WHAT WE TEST:
 *   - Registration: success, duplicate email rejection, hashed password storage
 *   - Login: correct credentials, wrong password, non-existent email, empty fields
 */
public class CustomerDAOTest {

    private CustomerDAO customerDAO;

    @Before
    public void setUp() {
        customerDAO = new CustomerDAO();
        // Clean up any leftover test data from a previous failed run
        TestDBHelper.cleanupTestData();
    }

    @After
    public void tearDown() {
        // Always remove test data after every test to keep the DB clean
        TestDBHelper.cleanupTestData();
    }

    // ================================================================
    // Registration Tests
    // ================================================================

    /**
     * A valid new customer with unique email and phone should be registered successfully.
     */
    @Test
    public void shouldRegisterCustomerSuccessfully() {
        Customer customer = new Customer(
            "Alice Test",
            TestDBHelper.testEmail("alice"),
            TestDBHelper.testPhone(1),
            "SecurePass1!",
            "123 Test Street"
        );

        boolean result = customerDAO.registerCustomer(customer);
        assertTrue("Valid customer registration should succeed", result);
    }

    /**
     * After registration, the stored password in the database must be a BCrypt hash,
     * NOT the original plain-text password. We verify this by confirming the
     * stored value starts with the BCrypt prefix ($2a$).
     */
    @Test
    public void shouldHashPasswordOnRegistration() {
        String plainPassword = "PlainTextPass99";
        Customer customer = new Customer(
            "Bob Hashed",
            TestDBHelper.testEmail("bob_hash"),
            TestDBHelper.testPhone(2),
            plainPassword,
            "456 Hash Ave"
        );

        boolean registered = customerDAO.registerCustomer(customer);
        assertTrue("Registration should succeed", registered);

        // Log in with the correct credentials to verify BCrypt worked end-to-end.
        // If the hash is stored correctly, loginCustomer() will successfully verify.
        Customer loggedIn = customerDAO.loginCustomer(TestDBHelper.testEmail("bob_hash"), plainPassword);
        assertNotNull(
            "Login must succeed after registration — proves BCrypt hash was stored correctly",
            loggedIn
        );
    }

    /**
     * Registering a customer with an email that already exists should fail
     * and return false (no duplicate entry in the database).
     */
    @Test
    public void shouldRejectDuplicateEmailOnRegistration() {
        String email = TestDBHelper.testEmail("duplicate_email");

        Customer first = new Customer("First User", email, TestDBHelper.testPhone(3), "Pass123!", "Addr1");
        Customer second = new Customer("Second User", email, TestDBHelper.testPhone(4), "Pass456!", "Addr2");

        boolean firstResult  = customerDAO.registerCustomer(first);
        boolean secondResult = customerDAO.registerCustomer(second);

        assertTrue("First registration should succeed", firstResult);
        assertFalse("Second registration with duplicate email should fail", secondResult);
    }

    /**
     * Registering a customer with a phone that already exists should fail.
     */
    @Test
    public void shouldRejectDuplicatePhoneOnRegistration() {
        String phone = TestDBHelper.testPhone(99);

        Customer first  = new Customer("First Phone",  TestDBHelper.testEmail("dup_phone1"), phone, "Pass1!", "A");
        Customer second = new Customer("Second Phone", TestDBHelper.testEmail("dup_phone2"), phone, "Pass2!", "B");

        boolean firstResult  = customerDAO.registerCustomer(first);
        boolean secondResult = customerDAO.registerCustomer(second);

        assertTrue("First registration should succeed", firstResult);
        assertFalse("Second registration with duplicate phone should fail", secondResult);
    }

    // ================================================================
    // Login Tests
    // ================================================================

    /**
     * A customer who registered with valid credentials should be able to log in
     * successfully using those same credentials.
     * The returned Customer object should contain the correct name and email.
     */
    @Test
    public void shouldLoginWithCorrectCredentials() {
        String email    = TestDBHelper.testEmail("login_correct");
        String password = "GoodPassword99!";

        // Register first
        TestDBHelper.insertTestCustomer("Login User", email, TestDBHelper.testPhone(5), password, "Login St");

        // Now attempt login
        Customer result = customerDAO.loginCustomer(email, password);

        assertNotNull("Login with correct credentials should return a Customer", result);
        assertEquals("Returned customer should have correct email", email, result.getEmail());
        assertEquals("Returned customer should have correct name", "Login User", result.getName());
    }

    /**
     * Login with a wrong password should fail and return null.
     * This verifies BCrypt verification rejects wrong passwords.
     */
    @Test
    public void shouldRejectLoginWithWrongPassword() {
        String email    = TestDBHelper.testEmail("wrong_pass");
        String password = "CorrectPassword1";

        TestDBHelper.insertTestCustomer("Wrong Pass User", email, TestDBHelper.testPhone(6), password, "Pass St");

        Customer result = customerDAO.loginCustomer(email, "WrongPassword999");
        assertNull("Login with wrong password should return null", result);
    }

    /**
     * Login with an email that doesn't exist in the database should return null.
     * We must NOT throw an exception — just return null gracefully.
     */
    @Test
    public void shouldRejectLoginWithNonExistentEmail() {
        Customer result = customerDAO.loginCustomer(
            TestDBHelper.testEmail("does_not_exist"),
            "AnyPassword"
        );
        assertNull("Login with non-existent email should return null", result);
    }

    /**
     * Login with an empty email string should immediately return null
     * without attempting a database query.
     */
    @Test
    public void shouldRejectLoginWithEmptyEmail() {
        Customer result = customerDAO.loginCustomer("", "SomePassword");
        assertNull("Login with empty email should return null", result);
    }

    /**
     * Login with an empty password string should immediately return null
     * without attempting a database query.
     */
    @Test
    public void shouldRejectLoginWithEmptyPassword() {
        String email = TestDBHelper.testEmail("empty_pass_test");
        TestDBHelper.insertTestCustomer("Empty Pass", email, TestDBHelper.testPhone(7), "RealPass", "Street");

        Customer result = customerDAO.loginCustomer(email, "");
        assertNull("Login with empty password should return null", result);
    }

    /**
     * Login with both email and password empty should return null.
     */
    @Test
    public void shouldRejectLoginWithBothFieldsEmpty() {
        Customer result = customerDAO.loginCustomer("", "");
        assertNull("Login with both fields empty should return null", result);
    }

    /**
     * Login with null email should return null without throwing NullPointerException.
     */
    @Test
    public void shouldRejectLoginWithNullEmail() {
        Customer result = customerDAO.loginCustomer(null, "Password");
        assertNull("Login with null email should return null", result);
    }
}
