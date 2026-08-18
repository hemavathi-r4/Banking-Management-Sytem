import dao.TransactionDAO;
import exception.AccountFrozenException;
import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * TransactionRollbackTest — JUnit 4 tests for Stage 16 transaction atomicity and rollback.
 *
 * STAGE 16 — ADVANCED TRANSACTION MANAGEMENT
 * -------------------------------------------
 * These tests verify that:
 *   1. A failed withdrawal does NOT alter the account balance.
 *   2. A failed transfer due to insufficient funds does NOT deduct from source
 *      AND does NOT credit the destination.
 *   3. A failed transfer due to invalid destination does not alter source balance.
 *   4. A transfer to a non-existent destination throws InvalidAccountException
 *      and leaves the source balance unchanged.
 *   5. Concurrent-style sequential withdrawals (simulated) do not produce overdraft.
 *
 * WHY REAL DB?
 * All tests run against the real MySQL test database (same approach as Stage 12 tests).
 * TestDBHelper creates isolated test data using the @bmstest.internal email domain,
 * which is automatically cleaned up in @After.
 *
 * STAGE 16 SPECIFIC:
 * The key improvement tested here is that balance checks now happen INSIDE
 * the JDBC transaction using SELECT ... FOR UPDATE, ensuring:
 *   - The balance we read when deciding to proceed is the same one we deduct from.
 *   - InsufficientFundsException is thrown after rollback — no partial changes.
 */
public class TransactionRollbackTest {

    private static final TransactionDAO txDAO = new TransactionDAO();

    private int    customerId;
    private long   accountNo;

    @Before
    public void setUp() {
        TestDBHelper.cleanupTestData();

        // Create a test customer with a test account holding 1,000.00
        customerId = TestDBHelper.insertTestCustomer(
            "Rollback Test User",
            TestDBHelper.testEmail("rollbackuser"),
            TestDBHelper.testPhone(9900),
            "TestPass123",
            "Test Address"
        );

        assertTrue("Test customer should be created", customerId > 0);

        accountNo = TestDBHelper.insertTestAccount(customerId, "SAVINGS", 1000.00, "ACTIVE");
        assertTrue("Test account should be created", accountNo > 0);
    }

    @After
    public void tearDown() {
        TestDBHelper.cleanupTestData();
    }

    // --------------------------------------------------
    // Test 1: Insufficient Funds — balance unchanged after exception
    // --------------------------------------------------
    /**
     * Verifying that InsufficientFundsException is thrown when amount > balance,
     * and that the balance remains exactly the same as before the attempt.
     *
     * STAGE 16: The balance check now happens inside the transaction with
     * SELECT ... FOR UPDATE, so rollback is guaranteed before exception is thrown.
     */
    @Test
    public void testWithdrawInsufficientFunds_balanceUnchanged() throws AccountFrozenException {
        double initialBalance = txDAO.getUpdatedBalance(accountNo);
        assertEquals("Initial balance should be 1000.00", 1000.00, initialBalance, 0.001);

        try {
            // Attempt to withdraw more than the balance — should fail
            txDAO.withdrawAmount(accountNo, 5000.00);
            fail("Expected InsufficientFundsException was not thrown");
        } catch (InsufficientFundsException e) {
            // Expected — verify the balance is still exactly the same
            double afterBalance = txDAO.getUpdatedBalance(accountNo);
            assertEquals(
                "Balance must NOT change after a failed withdrawal",
                initialBalance, afterBalance, 0.001
            );
        }
    }

    // --------------------------------------------------
    // Test 2: Frozen account withdrawal — balance unchanged
    // --------------------------------------------------
    /**
     * Verifying AccountFrozenException is thrown for a FROZEN account,
     * and balance remains unchanged.
     */
    @Test
    public void testWithdrawFrozenAccount_balanceUnchanged() throws InsufficientFundsException {
        // Create a frozen account with balance
        long frozenAccountNo = TestDBHelper.insertTestAccount(customerId, "SAVINGS", 500.00, "FROZEN");
        assertTrue("Frozen test account should be created", frozenAccountNo > 0);

        double initialBalance = txDAO.getUpdatedBalance(frozenAccountNo);

        try {
            txDAO.withdrawAmount(frozenAccountNo, 100.00);
            fail("Expected AccountFrozenException was not thrown");
        } catch (AccountFrozenException e) {
            double afterBalance = txDAO.getUpdatedBalance(frozenAccountNo);
            assertEquals(
                "Balance must NOT change after attempted withdrawal from frozen account",
                initialBalance, afterBalance, 0.001
            );
        }
    }

    // --------------------------------------------------
    // Test 3: Transfer to self — InvalidAccountException, balance unchanged
    // --------------------------------------------------
    @Test
    public void testTransferToSameAccount_throwsInvalidAccountException() {
        double initialBalance = txDAO.getUpdatedBalance(accountNo);

        try {
            txDAO.transferAmount(accountNo, accountNo, 100.00, "Self transfer");
            fail("Expected InvalidAccountException was not thrown");
        } catch (InvalidAccountException e) {
            // Expected — verify balance unchanged
            double afterBalance = txDAO.getUpdatedBalance(accountNo);
            assertEquals("Balance must be unchanged after self-transfer attempt",
                initialBalance, afterBalance, 0.001);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    // --------------------------------------------------
    // Test 4: Transfer to non-existent account — balance unchanged
    // --------------------------------------------------
    /**
     * Verifying that transferring to an account number that doesn't exist
     * throws InvalidAccountException and leaves the source balance intact.
     *
     * STAGE 16: Both account rows are now locked inside the transaction before
     * validation. The non-existent destination causes rollback before any debit.
     */
    @Test
    public void testTransferToNonExistentAccount_sourceBalanceUnchanged() {
        double initialBalance = txDAO.getUpdatedBalance(accountNo);

        long nonExistentAccount = 9999999L; // extremely unlikely to exist

        try {
            txDAO.transferAmount(accountNo, nonExistentAccount, 500.00, "Test transfer");
            fail("Expected InvalidAccountException was not thrown");
        } catch (InvalidAccountException e) {
            // Expected
            double afterBalance = txDAO.getUpdatedBalance(accountNo);
            assertEquals(
                "Source account balance must be unchanged when destination doesn't exist",
                initialBalance, afterBalance, 0.001
            );
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // --------------------------------------------------
    // Test 5: Transfer insufficient funds — BOTH balances unchanged
    // --------------------------------------------------
    /**
     * Verifying that when a transfer fails due to insufficient funds in the source,
     * both source and destination balances are completely unchanged.
     *
     * This is the core atomicity guarantee: in the old code (pre-Stage 16),
     * the balance check happened outside the transaction. With SELECT FOR UPDATE,
     * the check and the debit are now atomic.
     */
    @Test
    public void testTransferInsufficientFunds_bothBalancesUnchanged() throws AccountFrozenException, InvalidAccountException {
        // Create a second test account to transfer to
        int customer2Id = TestDBHelper.insertTestCustomer(
            "Rollback Dest User",
            TestDBHelper.testEmail("rollbackdest"),
            TestDBHelper.testPhone(9901),
            "TestPass123",
            "Test Address 2"
        );
        long destAccountNo = TestDBHelper.insertTestAccount(customer2Id, "SAVINGS", 200.00, "ACTIVE");

        double sourceInitial = txDAO.getUpdatedBalance(accountNo);     // 1000.00
        double destInitial   = txDAO.getUpdatedBalance(destAccountNo); // 200.00

        try {
            // Try to transfer 5,000 from a 1,000-balance account — should fail
            txDAO.transferAmount(accountNo, destAccountNo, 5000.00, "Test");
            fail("Expected InsufficientFundsException was not thrown");
        } catch (InsufficientFundsException e) {
            double sourceAfter = txDAO.getUpdatedBalance(accountNo);
            double destAfter   = txDAO.getUpdatedBalance(destAccountNo);

            assertEquals("Source balance must be unchanged after failed transfer",
                sourceInitial, sourceAfter, 0.001);
            assertEquals("Destination balance must be unchanged after failed transfer",
                destInitial, destAfter, 0.001);
        }
    }

    // --------------------------------------------------
    // Test 6: Successful transfer — both balances updated correctly
    // --------------------------------------------------
    /**
     * End-to-end verification that a valid transfer correctly debits source
     * and credits destination with exact amounts.
     */
    @Test
    public void testSuccessfulTransfer_bothBalancesCorrect() throws Exception {
        int customer3Id = TestDBHelper.insertTestCustomer(
            "Transfer Success Dest",
            TestDBHelper.testEmail("transferdest"),
            TestDBHelper.testPhone(9902),
            "TestPass123",
            "Test Address 3"
        );
        long destAccountNo = TestDBHelper.insertTestAccount(customer3Id, "SAVINGS", 500.00, "ACTIVE");

        double sourceInitial = txDAO.getUpdatedBalance(accountNo);     // 1000.00
        double destInitial   = txDAO.getUpdatedBalance(destAccountNo); // 500.00

        boolean result = txDAO.transferAmount(accountNo, destAccountNo, 300.00, "JUnit transfer test");
        assertTrue("Transfer should succeed", result);

        double sourceAfter = txDAO.getUpdatedBalance(accountNo);
        double destAfter   = txDAO.getUpdatedBalance(destAccountNo);

        assertEquals("Source balance must decrease by exact transfer amount",
            sourceInitial - 300.00, sourceAfter, 0.001);
        assertEquals("Destination balance must increase by exact transfer amount",
            destInitial + 300.00, destAfter, 0.001);
    }

    // --------------------------------------------------
    // Test 7: Deposit rollback — frozen account deposit rejected
    // --------------------------------------------------
    @Test
    public void testDepositToFrozenAccount_throwsAccountFrozenException() {
        long frozenAccNo = TestDBHelper.insertTestAccount(customerId, "CURRENT", 300.00, "FROZEN");
        double initialBalance = txDAO.getUpdatedBalance(frozenAccNo);

        try {
            txDAO.depositAmount(frozenAccNo, 100.00);
            fail("Expected AccountFrozenException was not thrown");
        } catch (AccountFrozenException e) {
            double afterBalance = txDAO.getUpdatedBalance(frozenAccNo);
            assertEquals("Frozen account balance must be unchanged after deposit attempt",
                initialBalance, afterBalance, 0.001);
        }
    }

    // --------------------------------------------------
    // Test 8: Sequential withdrawals do not produce overdraft
    // --------------------------------------------------
    /**
     * Simulates the previously dangerous TOCTOU scenario with sequential operations.
     * Withdraws 600 twice from a 1000-balance account.
     * First should succeed, second should fail with InsufficientFundsException.
     * Final balance should be exactly 400 (not -200).
     */
    @Test
    public void testSequentialWithdrawals_noOverdraft() throws AccountFrozenException {
        // First withdrawal: 600 from 1000 — should succeed
        try {
            boolean result = txDAO.withdrawAmount(accountNo, 600.00);
            assertTrue("First withdrawal of 600 from 1000 should succeed", result);
        } catch (InsufficientFundsException e) {
            fail("First withdrawal should not fail: " + e.getMessage());
        }

        double midBalance = txDAO.getUpdatedBalance(accountNo);
        assertEquals("Balance after first withdrawal should be 400.00", 400.00, midBalance, 0.001);

        // Second withdrawal: 600 from 400 — should fail
        try {
            txDAO.withdrawAmount(accountNo, 600.00);
            fail("Second withdrawal of 600 from 400 should throw InsufficientFundsException");
        } catch (InsufficientFundsException e) {
            // Expected — the FOR UPDATE lock ensures we see the updated balance
        }

        double finalBalance = txDAO.getUpdatedBalance(accountNo);
        assertEquals("Final balance must be 400.00 — no overdraft",
            400.00, finalBalance, 0.001);
    }
}
