import dao.TransactionDAO;
import exception.AccountFrozenException;
import exception.InsufficientFundsException;
import exception.InvalidAccountException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * TransactionDAOTest - JUnit tests for TransactionDAO deposit, withdrawal, and transfer logic.
 *
 * STAGE 12 — JUNIT TESTING
 * -------------------------
 * Tests run against the real MySQL database using isolated test accounts.
 * Every test cleans up after itself using TestDBHelper.
 *
 * WHAT WE TEST:
 *   - Deposit: success, balance increase, negative/zero rejection
 *   - Withdrawal: success, balance decrease, InsufficientFundsException, frozen account
 *   - Transfer: success, sender/receiver balance changes, same-account rejection,
 *               insufficient funds, non-existent destination
 */
public class TransactionDAOTest {

    private TransactionDAO transactionDAO;

    // Reusable test accounts created in @Before
    private int  testCustomerId1;
    private int  testCustomerId2;
    private long testAccountA;  // Active account with 5000.00
    private long testAccountB;  // Active account with 2000.00 (transfer target)
    private long frozenAccount; // Frozen account for frozen-check tests

    @Before
    public void setUp() throws Exception {
        transactionDAO = new TransactionDAO();
        TestDBHelper.cleanupTestData();

        // Create two test customers
        testCustomerId1 = TestDBHelper.insertTestCustomer(
            "TX User One", TestDBHelper.testEmail("tx_user1"),
            TestDBHelper.testPhone(30), "pass1", "Addr1"
        );
        testCustomerId2 = TestDBHelper.insertTestCustomer(
            "TX User Two", TestDBHelper.testEmail("tx_user2"),
            TestDBHelper.testPhone(31), "pass2", "Addr2"
        );

        // Create test accounts:
        // Account A — ACTIVE, balance 5000
        testAccountA   = TestDBHelper.insertTestAccount(testCustomerId1, "SAVINGS",  5000.00, "ACTIVE");
        // Account B — ACTIVE, balance 2000
        testAccountB   = TestDBHelper.insertTestAccount(testCustomerId2, "SAVINGS",  2000.00, "ACTIVE");
        // Frozen account — cannot deposit, withdraw, or transfer
        frozenAccount  = TestDBHelper.insertTestAccount(testCustomerId1, "SAVINGS",  1000.00, "FROZEN");
    }

    @After
    public void tearDown() {
        TestDBHelper.cleanupTestData();
    }

    // ==================================================================
    // DEPOSIT TESTS
    // ==================================================================

    /**
     * A deposit of a valid positive amount into an ACTIVE account should succeed.
     * The returned value should be true.
     */
    @Test
    public void shouldDepositAmountSuccessfully() throws Exception {
        boolean result = transactionDAO.depositAmount(testAccountA, 500.00);
        assertTrue("Valid deposit should return true", result);
    }

    /**
     * After depositing Rs. 500 into an account with Rs. 5000,
     * the balance must be exactly Rs. 5500.
     */
    @Test
    public void shouldIncreaseBalanceAfterDeposit() throws Exception {
        double beforeDeposit = transactionDAO.getUpdatedBalance(testAccountA);
        double depositAmount  = 500.00;

        transactionDAO.depositAmount(testAccountA, depositAmount);

        double afterDeposit = transactionDAO.getUpdatedBalance(testAccountA);
        assertEquals(
            "Balance after deposit should be (before + depositAmount)",
            beforeDeposit + depositAmount, afterDeposit, 0.01
        );
    }

    /**
     * Multiple deposits should accumulate correctly.
     * Starting balance: 5000. Deposit 1000, then 250.
     * Expected final: 6250.
     */
    @Test
    public void shouldAccumulateMultipleDeposits() throws Exception {
        transactionDAO.depositAmount(testAccountA, 1000.00);
        transactionDAO.depositAmount(testAccountA, 250.00);

        double finalBalance = transactionDAO.getUpdatedBalance(testAccountA);
        assertEquals("Multiple deposits should accumulate correctly", 6250.00, finalBalance, 0.01);
    }

    /**
     * Depositing into a FROZEN account should throw AccountFrozenException.
     * The balance must remain unchanged.
     */
    @Test
    public void shouldThrowAccountFrozenExceptionOnDepositToFrozenAccount() {
        assertThrows(
            "Depositing to a frozen account should throw AccountFrozenException",
            AccountFrozenException.class,
            () -> transactionDAO.depositAmount(frozenAccount, 100.00)
        );
    }

    /**
     * getUpdatedBalance() for a non-existent account should return -1.0
     * (the sentinel value defined in the method).
     */
    @Test
    public void shouldReturnNegativeOneForNonExistentAccount() {
        double balance = transactionDAO.getUpdatedBalance(-99999L);
        assertEquals("Non-existent account should return -1.0", -1.0, balance, 0.001);
    }

    // ==================================================================
    // WITHDRAWAL TESTS
    // ==================================================================

    /**
     * Withdrawing a valid amount from an ACTIVE account with sufficient funds should succeed.
     */
    @Test
    public void shouldWithdrawAmountSuccessfully() throws Exception {
        boolean result = transactionDAO.withdrawAmount(testAccountA, 1000.00);
        assertTrue("Valid withdrawal should return true", result);
    }

    /**
     * After withdrawing Rs. 1000 from an account with Rs. 5000,
     * the balance must be exactly Rs. 4000.
     */
    @Test
    public void shouldDecreaseBalanceAfterWithdrawal() throws Exception {
        double beforeWithdrawal = transactionDAO.getUpdatedBalance(testAccountA);
        double withdrawalAmount = 1000.00;

        transactionDAO.withdrawAmount(testAccountA, withdrawalAmount);

        double afterWithdrawal = transactionDAO.getUpdatedBalance(testAccountA);
        assertEquals(
            "Balance after withdrawal should be (before - withdrawalAmount)",
            beforeWithdrawal - withdrawalAmount, afterWithdrawal, 0.01
        );
    }

    /**
     * Withdrawing MORE than the available balance must throw InsufficientFundsException.
     * The exception should carry the correct amounts.
     */
    @Test
    public void shouldRejectWithdrawalWhenBalanceIsInsufficient() {
        InsufficientFundsException ex = assertThrows(
            "Withdrawal exceeding balance must throw InsufficientFundsException",
            InsufficientFundsException.class,
            () -> transactionDAO.withdrawAmount(testAccountA, 99999.00)
        );

        assertEquals("Exception should carry the requested amount",
            99999.00, ex.getAmountRequested(), 0.01);
        assertEquals("Exception should carry the available balance",
            5000.00, ex.getAvailableBalance(), 0.01);
    }

    /**
     * Withdrawing exactly the full available balance should succeed (zero remaining).
     */
    @Test
    public void shouldAllowWithdrawalOfExactAvailableBalance() throws Exception {
        double balance = transactionDAO.getUpdatedBalance(testAccountA); // 5000.00
        boolean result = transactionDAO.withdrawAmount(testAccountA, balance);

        assertTrue("Withdrawal of exact balance should succeed", result);
        assertEquals("Balance after full withdrawal should be 0",
            0.00, transactionDAO.getUpdatedBalance(testAccountA), 0.01);
    }

    /**
     * Withdrawing from a FROZEN account should throw AccountFrozenException.
     */
    @Test
    public void shouldThrowAccountFrozenExceptionOnWithdrawalFromFrozenAccount() {
        assertThrows(
            "Withdrawal from a frozen account should throw AccountFrozenException",
            AccountFrozenException.class,
            () -> transactionDAO.withdrawAmount(frozenAccount, 100.00)
        );
    }

    // ==================================================================
    // FUND TRANSFER TESTS
    // ==================================================================

    /**
     * A valid transfer between two ACTIVE accounts with sufficient funds should succeed.
     */
    @Test
    public void shouldTransferAmountSuccessfully() throws Exception {
        boolean result = transactionDAO.transferAmount(testAccountA, testAccountB, 500.00, "Test Transfer");
        assertTrue("Valid transfer should return true", result);
    }

    /**
     * After transferring Rs. 500 from A (5000) to B (2000):
     *   - A's balance must be 4500
     *   - B's balance must be 2500
     */
    @Test
    public void shouldDecreaseSenderBalanceAfterTransfer() throws Exception {
        transactionDAO.transferAmount(testAccountA, testAccountB, 500.00, "Balance Check");

        double senderBalance = transactionDAO.getUpdatedBalance(testAccountA);
        assertEquals("Sender balance must decrease by transfer amount", 4500.00, senderBalance, 0.01);
    }

    @Test
    public void shouldIncreaseReceiverBalanceAfterTransfer() throws Exception {
        transactionDAO.transferAmount(testAccountA, testAccountB, 500.00, "Balance Check");

        double receiverBalance = transactionDAO.getUpdatedBalance(testAccountB);
        assertEquals("Receiver balance must increase by transfer amount", 2500.00, receiverBalance, 0.01);
    }

    /**
     * Transferring an amount greater than the sender's balance must throw InsufficientFundsException.
     * Neither account balance should change.
     */
    @Test
    public void shouldRejectTransferWithInsufficientBalance() {
        double senderBalanceBefore   = transactionDAO.getUpdatedBalance(testAccountA);
        double receiverBalanceBefore = transactionDAO.getUpdatedBalance(testAccountB);

        assertThrows(
            "Transfer exceeding sender balance must throw InsufficientFundsException",
            InsufficientFundsException.class,
            () -> transactionDAO.transferAmount(testAccountA, testAccountB, 99999.00, "Fail Transfer")
        );

        // Verify neither balance changed (atomicity — both stay the same on failure)
        assertEquals("Sender balance must be unchanged after failed transfer",
            senderBalanceBefore, transactionDAO.getUpdatedBalance(testAccountA), 0.01);
        assertEquals("Receiver balance must be unchanged after failed transfer",
            receiverBalanceBefore, transactionDAO.getUpdatedBalance(testAccountB), 0.01);
    }

    /**
     * Transferring to the SAME account must throw InvalidAccountException.
     * Self-transfers are a business rule violation.
     */
    @Test
    public void shouldRejectTransferToSameAccount() {
        assertThrows(
            "Transfer to same account must throw InvalidAccountException",
            InvalidAccountException.class,
            () -> transactionDAO.transferAmount(testAccountA, testAccountA, 100.00, "Self")
        );
    }

    /**
     * Transferring to a non-existent destination account must throw InvalidAccountException.
     */
    @Test
    public void shouldRejectTransferToNonExistentAccount() {
        assertThrows(
            "Transfer to non-existent account must throw InvalidAccountException",
            InvalidAccountException.class,
            () -> transactionDAO.transferAmount(testAccountA, -9999L, 100.00, "Ghost Account")
        );
    }

    /**
     * Transferring FROM a frozen account must throw AccountFrozenException.
     */
    @Test
    public void shouldRejectTransferFromFrozenAccount() {
        assertThrows(
            "Transfer from frozen account must throw AccountFrozenException",
            AccountFrozenException.class,
            () -> transactionDAO.transferAmount(frozenAccount, testAccountB, 100.00, "Frozen Transfer")
        );
    }

    /**
     * getUpdatedBalance() should correctly reflect the exact balance.
     */
    @Test
    public void shouldReturnCorrectBalanceForAccount() {
        double balance = transactionDAO.getUpdatedBalance(testAccountA);
        assertEquals("getUpdatedBalance should return 5000.00 for testAccountA", 5000.00, balance, 0.01);
    }

    // ==================================================================
    // accountExists() TESTS
    // ==================================================================

    /**
     * accountExists() should return true for a valid, existing account.
     */
    @Test
    public void shouldReturnTrueForExistingAccount() {
        assertTrue("accountExists() should return true for a real account",
            transactionDAO.accountExists(testAccountA));
    }

    /**
     * accountExists() should return false for an account number that doesn't exist.
     */
    @Test
    public void shouldReturnFalseForNonExistentAccount() {
        assertFalse("accountExists() should return false for a non-existent account",
            transactionDAO.accountExists(-9999L));
    }
}
