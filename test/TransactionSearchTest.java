import dao.TransactionDAO;
import model.PageResult;
import model.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * TransactionSearchTest - JUnit tests for paginated transaction search and filtering in TransactionDAO.
 *
 * STAGE 14 — JUNIT TESTING
 * -------------------------
 * Tests transaction filtering by type, amount range, and limit/offset pagination.
 */
public class TransactionSearchTest {

    private TransactionDAO transactionDAO;
    private long testAccountNo;

    @Before
    public void setUp() throws Exception {
        transactionDAO = new TransactionDAO();
        TestDBHelper.cleanupTestData();

        int custId = TestDBHelper.insertTestCustomer(
            "Search TX User", TestDBHelper.testEmail("search_tx"), TestDBHelper.testPhone(50), "pass", "Addr"
        );
        testAccountNo = TestDBHelper.insertTestAccount(custId, "SAVINGS", 10000.00, "ACTIVE");

        // Populate test transactions
        transactionDAO.depositAmount(testAccountNo, 1000.00); // DEPOSIT 1000
        transactionDAO.depositAmount(testAccountNo, 2000.00); // DEPOSIT 2000
        transactionDAO.withdrawAmount(testAccountNo, 500.00);  // WITHDRAWAL 500
    }

    @After
    public void tearDown() {
        TestDBHelper.cleanupTestData();
    }

    @Test
    public void shouldReturnPaginatedTransactionsForAccount() {
        PageResult<Transaction> page1 = transactionDAO.getPaginatedTransactionsForAccount(
            testAccountNo, 1, 2, null, null, null, null, null
        );

        assertEquals("Total transactions should be 3", 3, page1.getTotalRecords());
        assertEquals("Page 1 with size 2 should return 2 records", 2, page1.getRecords().size());
        assertEquals("Total pages should be 2", 2, page1.getTotalPages());
        assertTrue("Page 1 should have next page", page1.hasNext());
    }

    @Test
    public void shouldFilterTransactionsByType() {
        PageResult<Transaction> deposits = transactionDAO.getPaginatedTransactionsForAccount(
            testAccountNo, 1, 10, "DEPOSIT", null, null, null, null
        );

        assertEquals("Should return 2 deposit transactions", 2, deposits.getTotalRecords());
        for (Transaction tx : deposits.getRecords()) {
            assertEquals("DEPOSIT", tx.getTransactionType());
        }

        PageResult<Transaction> withdrawals = transactionDAO.getPaginatedTransactionsForAccount(
            testAccountNo, 1, 10, "WITHDRAWAL", null, null, null, null
        );

        assertEquals("Should return 1 withdrawal transaction", 1, withdrawals.getTotalRecords());
    }

    @Test
    public void shouldFilterTransactionsByAmountRange() {
        // Filter transactions with amount >= 1500
        PageResult<Transaction> highAmountTxs = transactionDAO.getPaginatedTransactionsForAccount(
            testAccountNo, 1, 10, null, 1500.00, null, null, null
        );

        assertEquals("Should return 1 transaction (2000.00)", 1, highAmountTxs.getTotalRecords());
        assertEquals(2000.00, highAmountTxs.getRecords().get(0).getAmount(), 0.01);
    }
}
