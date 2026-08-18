import dao.ReportsDAO;
import dao.TransactionDAO;
import exception.AccountFrozenException;
import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import model.AccountStatistics;
import model.BankingSummary;
import model.TopAccountRow;
import model.TransactionSummaryRow;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ReportsDAOTest — JUnit 4 tests for Stage 17 Banking Reports & Analytics.
 *
 * STAGE 17 — BANKING REPORTS & ANALYTICS
 * ----------------------------------------
 * Verifies that all ReportsDAO methods return correct, non-null results
 * against isolated test data created by TestDBHelper.
 *
 * Test setup:
 *   - 2 customers with 2 accounts each (SAVINGS + CURRENT, various statuses)
 *   - Several deposit, withdrawal, and transfer transactions
 *
 * Each test validates structural correctness (not null, count > 0, field constraints)
 * and some tests validate specific computed values.
 */
public class ReportsDAOTest {

    private static final ReportsDAO    reportsDAO = new ReportsDAO();
    private static final TransactionDAO txDAO     = new TransactionDAO();

    private long accountA;
    private long accountB;

    @Before
    public void setUp() throws Exception {
        TestDBHelper.cleanupTestData();

        // Customer 1: has SAVINGS (ACTIVE) and CURRENT (FROZEN)
        int cust1 = TestDBHelper.insertTestCustomer(
            "Reports Test User1",
            TestDBHelper.testEmail("rpt_user1"),
            TestDBHelper.testPhone(8800),
            "Pass123", "123 Test St"
        );
        accountA = TestDBHelper.insertTestAccount(cust1, "SAVINGS", 5000.00, "ACTIVE");
        TestDBHelper.insertTestAccount(cust1, "CURRENT", 1000.00, "FROZEN");

        // Customer 2: has SAVINGS (ACTIVE) and CURRENT (ACTIVE)
        int cust2 = TestDBHelper.insertTestCustomer(
            "Reports Test User2",
            TestDBHelper.testEmail("rpt_user2"),
            TestDBHelper.testPhone(8801),
            "Pass123", "456 Test Ave"
        );
        accountB = TestDBHelper.insertTestAccount(cust2, "SAVINGS",  2000.00, "ACTIVE");
        TestDBHelper.insertTestAccount(cust2, "CURRENT", 3000.00, "ACTIVE");

        assertTrue("Account A must be created", accountA > 0);
        assertTrue("Account B must be created", accountB > 0);

        // Create some transactions
        txDAO.depositAmount(accountA, 500.00);           // DEPOSIT to accountA
        txDAO.depositAmount(accountB, 300.00);           // DEPOSIT to accountB
        txDAO.withdrawAmount(accountA, 200.00);          // WITHDRAWAL from accountA
        txDAO.transferAmount(accountA, accountB, 100.00, "JUnit Report Test Transfer");  // TRANSFER
    }

    @After
    public void tearDown() {
        TestDBHelper.cleanupTestData();
    }

    // --------------------------------------------------
    // Test 1: Overall Banking Summary — not null, all fields >= 0
    // --------------------------------------------------
    @Test
    public void testGetOverallBankingSummary_notNull() {
        BankingSummary summary = reportsDAO.getOverallBankingSummary();

        assertNotNull("BankingSummary must not be null", summary);
        assertTrue("Total customers must be >= 0",     summary.getTotalCustomers()    >= 0);
        assertTrue("Total accounts must be >= 0",      summary.getTotalAccounts()     >= 0);
        assertTrue("Total balance must be >= 0",       summary.getTotalBalance()      >= 0);
        assertTrue("Total transactions must be >= 0",  summary.getTotalTransactions() >= 0);
        assertTrue("Savings count must be >= 0",       summary.getSavingsAccounts()   >= 0);
        assertTrue("Current count must be >= 0",       summary.getCurrentAccounts()   >= 0);
        assertTrue("Active count must be >= 0",        summary.getActiveAccounts()    >= 0);
        assertTrue("Frozen count must be >= 0",        summary.getFrozenAccounts()    >= 0);
    }

    // --------------------------------------------------
    // Test 2: Overall Banking Summary — test data counts reflected
    // --------------------------------------------------
    @Test
    public void testGetOverallBankingSummary_countsIncludeTestData() {
        BankingSummary summary = reportsDAO.getOverallBankingSummary();

        assertNotNull(summary);
        // We added 4 transactions (2 deposits, 1 withdrawal, 1 transfer)
        // The total may include pre-existing data, but must be >= 4
        assertTrue("Total transactions should be at least 4 from test setup",
            summary.getTotalTransactions() >= 4);

        // We added 2 customers and 4 accounts
        assertTrue("Total customers must include our 2 test customers",
            summary.getTotalCustomers() >= 2);
        assertTrue("Total accounts must include our 4 test accounts",
            summary.getTotalAccounts()  >= 4);
    }

    // --------------------------------------------------
    // Test 3: Transaction type summary — list is not empty
    // --------------------------------------------------
    @Test
    public void testGetTransactionTypeSummary_notEmpty() {
        List<TransactionSummaryRow> rows = reportsDAO.getTransactionTypeSummary();

        assertNotNull("Transaction summary list must not be null", rows);
        assertFalse("Transaction summary list must not be empty", rows.isEmpty());
    }

    // --------------------------------------------------
    // Test 4: Transaction type summary — all fields valid
    // --------------------------------------------------
    @Test
    public void testGetTransactionTypeSummary_fieldsValid() {
        List<TransactionSummaryRow> rows = reportsDAO.getTransactionTypeSummary();

        for (TransactionSummaryRow row : rows) {
            assertNotNull("Transaction type must not be null", row.getTransactionType());
            assertTrue("Count must be > 0",          row.getCount()       > 0);
            assertTrue("Total amount must be > 0",   row.getTotalAmount() > 0);
            assertTrue("Avg amount must be > 0",     row.getAvgAmount()   > 0);
            assertTrue("Min amount must be > 0",     row.getMinAmount()   > 0);
            assertTrue("Max >= Min",                 row.getMaxAmount()   >= row.getMinAmount());
        }
    }

    // --------------------------------------------------
    // Test 5: Daily transaction report — today returns results
    // --------------------------------------------------
    @Test
    public void testGetDailyTransactionReport_todayHasResults() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<TransactionSummaryRow> rows = reportsDAO.getDailyTransactionReport(today, today);

        assertNotNull("Daily report list must not be null", rows);
        // We created transactions in setUp today, so there should be rows
        assertFalse("Daily report for today must not be empty (transactions just created)", rows.isEmpty());
    }

    // --------------------------------------------------
    // Test 6: Daily transaction report — period field is populated
    // --------------------------------------------------
    @Test
    public void testGetDailyTransactionReport_periodFieldPopulated() {
        List<TransactionSummaryRow> rows = reportsDAO.getDailyTransactionReport(null, null);

        assertNotNull(rows);
        for (TransactionSummaryRow row : rows) {
            assertNotNull("Period (date) must not be null in daily report", row.getPeriod());
            assertTrue("Period must be in YYYY-MM-DD format",
                row.getPeriod().matches("\\d{4}-\\d{2}-\\d{2}"));
        }
    }

    // --------------------------------------------------
    // Test 7: Monthly report — period field format
    // --------------------------------------------------
    @Test
    public void testGetMonthlyTransactionReport_periodFieldFormat() {
        int currentYear = LocalDate.now().getYear();
        List<TransactionSummaryRow> rows = reportsDAO.getMonthlyTransactionReport(currentYear);

        assertNotNull(rows);
        assertFalse("Monthly report for current year must not be empty", rows.isEmpty());

        for (TransactionSummaryRow row : rows) {
            assertNotNull("Period must not be null in monthly report", row.getPeriod());
            assertTrue("Period must be in YYYY-MM format",
                row.getPeriod().matches("\\d{4}-\\d{2}"));
        }
    }

    // --------------------------------------------------
    // Test 8: Monthly report — year=0 returns all data
    // --------------------------------------------------
    @Test
    public void testGetMonthlyTransactionReport_yearZeroReturnsAllData() {
        List<TransactionSummaryRow> allRows   = reportsDAO.getMonthlyTransactionReport(0);
        List<TransactionSummaryRow> yearRows  = reportsDAO.getMonthlyTransactionReport(
            LocalDate.now().getYear()
        );

        assertNotNull(allRows);
        // All years should have at least as many rows as current year
        assertTrue("All-years report should have >= current year rows",
            allRows.size() >= yearRows.size());
    }

    // --------------------------------------------------
    // Test 9: Account statistics — valid groups returned
    // --------------------------------------------------
    @Test
    public void testGetAccountStatistics_validGroups() {
        List<AccountStatistics> rows = reportsDAO.getAccountStatistics();

        assertNotNull("Account statistics list must not be null", rows);
        assertFalse("Account statistics list must not be empty", rows.isEmpty());

        for (AccountStatistics row : rows) {
            assertNotNull("Account type must not be null", row.getAccountType());
            assertNotNull("Status must not be null",       row.getStatus());
            assertTrue("Count must be > 0",                row.getCount()        > 0);
            assertTrue("Total balance must be >= 0",       row.getTotalBalance() >= 0);
            assertTrue("Min balance must be >= 0",         row.getMinBalance()   >= 0);
            assertTrue("Max >= Min",                       row.getMaxBalance()   >= row.getMinBalance());
        }
    }

    // --------------------------------------------------
    // Test 10: Account statistics — test data accounts included
    // --------------------------------------------------
    @Test
    public void testGetAccountStatistics_testDataPresent() {
        List<AccountStatistics> rows = reportsDAO.getAccountStatistics();

        assertNotNull(rows);

        // We created SAVINGS/ACTIVE, SAVINGS/ACTIVE, CURRENT/FROZEN, CURRENT/ACTIVE
        // so at least SAVINGS/ACTIVE and CURRENT/ACTIVE groups must exist
        boolean hasSavingsActive  = false;
        boolean hasCurrentFrozen  = false;

        for (AccountStatistics row : rows) {
            if ("SAVINGS".equals(row.getAccountType()) && "ACTIVE".equals(row.getStatus())) {
                hasSavingsActive = true;
            }
            if ("CURRENT".equals(row.getAccountType()) && "FROZEN".equals(row.getStatus())) {
                hasCurrentFrozen = true;
            }
        }

        assertTrue("SAVINGS/ACTIVE group must exist", hasSavingsActive);
        assertTrue("CURRENT/FROZEN group must exist", hasCurrentFrozen);
    }

    // --------------------------------------------------
    // Test 11: Top active accounts — ranked correctly
    // --------------------------------------------------
    @Test
    public void testGetTopActiveAccounts_rankedCorrectly() {
        List<TopAccountRow> rows = reportsDAO.getTopActiveAccounts(10);

        assertNotNull("Top accounts list must not be null", rows);
        assertFalse("Top accounts list must not be empty", rows.isEmpty());

        // Verify ranks are sequential starting from 1
        for (int i = 0; i < rows.size(); i++) {
            assertEquals("Rank at position " + i + " should be " + (i + 1),
                i + 1, rows.get(i).getRank());
        }

        // Verify descending order by transaction count
        for (int i = 0; i < rows.size() - 1; i++) {
            assertTrue("Transaction count should be descending",
                rows.get(i).getTransactionCount() >= rows.get(i + 1).getTransactionCount());
        }
    }

    // --------------------------------------------------
    // Test 12: Top active accounts — all required fields non-null
    // --------------------------------------------------
    @Test
    public void testGetTopActiveAccounts_allFieldsValid() {
        List<TopAccountRow> rows = reportsDAO.getTopActiveAccounts(5);

        assertNotNull(rows);

        for (TopAccountRow row : rows) {
            assertTrue("Account no must be > 0",          row.getAccountNo()        > 0);
            assertTrue("Customer ID must be > 0",         row.getCustomerId()       > 0);
            assertNotNull("Account type must not be null", row.getAccountType());
            assertNotNull("Status must not be null",       row.getStatus());
            assertTrue("Current balance must be >= 0",    row.getCurrentBalance()   >= 0);
            assertTrue("Transaction count must be > 0",   row.getTransactionCount() > 0);
            assertTrue("Total volume must be > 0",        row.getTotalVolume()      > 0);
            assertTrue("Rank must be > 0",                row.getRank()             > 0);
        }
    }
}
