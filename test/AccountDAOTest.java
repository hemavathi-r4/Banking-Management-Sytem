import dao.AccountDAO;
import model.Account;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * AccountDAOTest - JUnit tests for AccountDAO account creation and retrieval.
 *
 * STAGE 12 — JUNIT TESTING
 * -------------------------
 * Tests run against the real MySQL database using isolated test data.
 * Every test cleans up after itself using TestDBHelper.
 *
 * WHAT WE TEST:
 *   - Account creation for a valid customer
 *   - Retrieval of accounts by customer ID
 *   - Behavior when no accounts exist for a customer
 */
public class AccountDAOTest {

    private AccountDAO accountDAO;

    // Test customer IDs — set in @Before, cleaned in @After
    private int testCustomerId;

    @Before
    public void setUp() {
        accountDAO = new AccountDAO();
        TestDBHelper.cleanupTestData();

        // Create one test customer to own test accounts
        testCustomerId = TestDBHelper.insertTestCustomer(
            "Account Test User",
            TestDBHelper.testEmail("acc_user"),
            TestDBHelper.testPhone(20),
            "TestPass123",
            "99 Account Lane"
        );
    }

    @After
    public void tearDown() {
        TestDBHelper.cleanupTestData();
    }

    // ================================================================
    // Account Creation Tests
    // ================================================================

    /**
     * Creating a SAVINGS account for a valid customer should succeed.
     * The createAccount() method should return true.
     */
    @Test
    public void shouldCreateSavingsAccountSuccessfully() {
        Account account = new Account(testCustomerId, "SAVINGS", 1000.00, "ACTIVE");
        boolean result = accountDAO.createAccount(account);
        assertTrue("Creating a SAVINGS account should succeed", result);
    }

    /**
     * Creating a CURRENT account for a valid customer should also succeed.
     */
    @Test
    public void shouldCreateCurrentAccountSuccessfully() {
        Account account = new Account(testCustomerId, "CURRENT", 5000.00, "ACTIVE");
        boolean result = accountDAO.createAccount(account);
        assertTrue("Creating a CURRENT account should succeed", result);
    }

    /**
     * After creating an account, getAccountsByCustomerId() should return
     * a non-empty list containing that account.
     *
     * We verify:
     *   1. The list is not empty
     *   2. The returned account has the correct type and balance
     */
    @Test
    public void shouldReturnAccountAfterCreation() {
        double openingBalance = 2500.00;
        Account account = new Account(testCustomerId, "SAVINGS", openingBalance, "ACTIVE");
        accountDAO.createAccount(account);

        List<Account> accounts = accountDAO.getAccountsByCustomerId(testCustomerId);

        assertFalse("Account list should not be empty after account creation", accounts.isEmpty());

        // Find the account with our balance (in case multiple accounts exist)
        Account retrieved = accounts.get(0);
        assertEquals("Account type should be SAVINGS", "SAVINGS", retrieved.getAccountType());
        assertEquals("Balance should match opening balance",
            openingBalance, retrieved.getBalance(), 0.01);
        assertEquals("Status should be ACTIVE", "ACTIVE", retrieved.getStatus());
        assertEquals("customer_id should match", testCustomerId, retrieved.getCustomerId());
    }

    /**
     * A customer with two accounts should have both returned by getAccountsByCustomerId().
     */
    @Test
    public void shouldReturnMultipleAccountsForSameCustomer() {
        accountDAO.createAccount(new Account(testCustomerId, "SAVINGS", 1000.00, "ACTIVE"));
        accountDAO.createAccount(new Account(testCustomerId, "CURRENT", 5000.00, "ACTIVE"));

        List<Account> accounts = accountDAO.getAccountsByCustomerId(testCustomerId);

        assertEquals("Customer with 2 accounts should get 2 results", 2, accounts.size());
    }

    // ================================================================
    // Account Retrieval Tests
    // ================================================================

    /**
     * getAccountsByCustomerId() should return an empty list (not null)
     * when a valid customer has no accounts yet.
     */
    @Test
    public void shouldReturnEmptyListWhenCustomerHasNoAccounts() {
        // testCustomerId exists but has no accounts created in this test
        List<Account> accounts = accountDAO.getAccountsByCustomerId(testCustomerId);

        assertNotNull("getAccountsByCustomerId() should never return null", accounts);
        assertTrue("Customer with no accounts should get an empty list", accounts.isEmpty());
    }

    /**
     * getAccountsByCustomerId() for a customer ID that doesn't exist in the DB
     * should return an empty list (not null, not an exception).
     */
    @Test
    public void shouldReturnEmptyListForNonExistentCustomer() {
        // Use an intentionally invalid customer ID
        List<Account> accounts = accountDAO.getAccountsByCustomerId(-999);

        assertNotNull("Result should not be null for non-existent customer", accounts);
        assertTrue("Non-existent customer should get empty list", accounts.isEmpty());
    }
}
