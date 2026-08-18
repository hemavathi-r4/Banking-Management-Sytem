import dao.AdminDAO;
import model.Account;
import model.Customer;
import model.PageResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * AdminSearchTest - JUnit tests for AdminDAO paginated customer and account searching.
 *
 * STAGE 14 — JUNIT TESTING
 * -------------------------
 * Tests AdminDAO SQL search, filter, and pagination methods.
 */
public class AdminSearchTest {

    private AdminDAO adminDAO;
    private int custId1;
    private int custId2;

    @Before
    public void setUp() {
        adminDAO = new AdminDAO();
        TestDBHelper.cleanupTestData();

        custId1 = TestDBHelper.insertTestCustomer("Charlie Alpha", TestDBHelper.testEmail("charlie"), TestDBHelper.testPhone(60), "pass1", "Address 1");
        custId2 = TestDBHelper.insertTestCustomer("Delta Bravo",   TestDBHelper.testEmail("delta"),   TestDBHelper.testPhone(61), "pass2", "Address 2");

        TestDBHelper.insertTestAccount(custId1, "SAVINGS", 3000.00, "ACTIVE");
        TestDBHelper.insertTestAccount(custId2, "CURRENT", 7000.00, "FROZEN");
    }

    @After
    public void tearDown() {
        TestDBHelper.cleanupTestData();
    }

    @Test
    public void shouldReturnPaginatedCustomerSearchResults() {
        PageResult<Customer> pageResult = adminDAO.getPaginatedCustomers(1, 10, "Charlie");

        assertFalse("Search results should not be empty", pageResult.getRecords().isEmpty());
        assertEquals("Charlie Alpha", pageResult.getRecords().get(0).getName());
    }

    @Test
    public void shouldFilterAccountsByTypeAndStatus() {
        PageResult<Account> frozenAccounts = adminDAO.getPaginatedAccounts(1, 10, null, "FROZEN", null);

        assertFalse("Should find frozen account", frozenAccounts.getRecords().isEmpty());
        assertEquals("FROZEN", frozenAccounts.getRecords().get(0).getStatus());

        PageResult<Account> savingsAccounts = adminDAO.getPaginatedAccounts(1, 10, "SAVINGS", null, null);
        assertFalse("Should find SAVINGS account", savingsAccounts.getRecords().isEmpty());
        assertEquals("SAVINGS", savingsAccounts.getRecords().get(0).getAccountType());
    }

    @Test
    public void shouldHandleEmptySearchMatches() {
        PageResult<Customer> pageResult = adminDAO.getPaginatedCustomers(1, 10, "NonExistentName9999");
        assertTrue("No records should match nonexistent search", pageResult.getRecords().isEmpty());
        assertEquals(0, pageResult.getTotalRecords());
    }
}
