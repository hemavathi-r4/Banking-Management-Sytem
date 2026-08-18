package dao;

import database.DBConnection;
import exception.AccountFrozenException;
import exception.InsufficientFundsException;
import exception.InvalidAccountException;

import model.PageResult;
import model.Transaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * TransactionDAO - Data Access Object for Transaction-related database operations.
 *
 * This class handles all SQL operations that involve money movement:
 *   - Stage 5: Depositing money into an account.
 *   - Stage 6: Withdrawing money from an account (throws InsufficientFundsException).
 *   - Fetching the latest balance after any transaction.
 *
 * IMPORTANT CONCEPT — DATABASE TRANSACTIONS (not bank transactions):
 * A "database transaction" means a group of SQL statements that must ALL succeed
 * or ALL fail together. This ensures data consistency (atomicity).
 *
 * For a deposit, two SQL statements must run as one atomic unit:
 *   1. UPDATE accounts SET balance = balance + ? WHERE account_no = ?
 *   2. INSERT INTO transactions (...) VALUES (...)
 *
 * If only the first runs and the second fails, the balance changes but no record
 * is logged — this is a data integrity problem. By wrapping both in a database
 * transaction, we guarantee they both succeed or neither does.
 *
 * HOW WE CONTROL TRANSACTIONS IN JDBC:
 *   conn.setAutoCommit(false) — turns off auto-save after each statement.
 *   conn.commit()             — saves all pending statements permanently.
 *   conn.rollback()           — cancels all pending statements on error.
 *
 * STAGE 16 — CONCURRENT SAFETY UPGRADE (SELECT ... FOR UPDATE):
 * ---------------------------------------------------------------
 * Previous design had a TOCTOU (Time-of-Check Time-of-Use) race condition:
 *   1. Thread A reads balance = 10,000 (sufficient for 8,000 withdrawal).
 *   2. Thread B reads balance = 10,000 (sufficient for 8,000 withdrawal).
 *   3. Both threads proceed — resulting balance = -6,000 (overdraft!).
 *
 * Fix: All balance/status checks are now moved INSIDE the JDBC transaction block
 * using:  SELECT balance, status FROM accounts WHERE account_no = ? FOR UPDATE
 *
 * This acquires a row-level exclusive lock on the account row, forcing any
 * concurrent transaction that also tries to lock the same row to WAIT until
 * this transaction either commits or rolls back. This eliminates the race window.
 *
 * Isolation level set to READ_COMMITTED:
 *   - Prevents dirty reads (reading uncommitted data from another transaction).
 *   - Sufficient for row-level locking with FOR UPDATE — no need for SERIALIZABLE.
 *   - Avoids unnecessary gap locks that REPEATABLE_READ or SERIALIZABLE would add.
 */
public class TransactionDAO {

    // --------------------------------------------------
    // Helper: Get the status of an account (used externally)
    // --------------------------------------------------
    /**
     * Fetches the current status of the given account (ACTIVE, FROZEN, or CLOSED).
     * NOTE: This method is used for lightweight status-only checks outside transactions.
     * Inside money-movement transactions, we use SELECT ... FOR UPDATE instead.
     *
     * @param accountNo the account number to check
     * @return the status string, or null if not found
     */
    public String getAccountStatus(long accountNo) {
        String sql = "SELECT status FROM accounts WHERE account_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, accountNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not fetch account status: " + e.getMessage());
        }
        return null;
    }

    // --------------------------------------------------
    // Method 1: Deposit money into an account
    // --------------------------------------------------
    /**
     * Deposits the given amount into the specified account.
     *
     * This method executes the following SQL statements inside a single database transaction:
     *   Step A: SELECT balance, status FROM accounts WHERE account_no = ? FOR UPDATE
     *           (acquires row lock — prevents concurrent modification during this transaction)
     *   Step B: UPDATE accounts SET balance = balance + ? WHERE account_no = ?
     *   Step C: INSERT INTO transactions (to_account, transaction_type, amount, remarks)
     *           VALUES (?, 'DEPOSIT', ?, 'Self deposit')
     *
     * STAGE 16 CHANGE: The status check is now done INSIDE the transaction using
     * SELECT ... FOR UPDATE, which acquires a row-level lock. This prevents a
     * concurrent withdrawal from reading the account status between our check and update.
     *
     * @param accountNo the account number to deposit into
     * @param amount    the amount to deposit
     * @return true if all SQL statements succeeded and were committed, false otherwise
     * @throws AccountFrozenException if the account is not ACTIVE
     */
    public boolean depositAmount(long accountNo, double amount) throws AccountFrozenException {

        String lockSql    = "SELECT balance, status FROM accounts WHERE account_no = ? FOR UPDATE";
        String updateSql  = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
        String insertSql  = "INSERT INTO transactions (to_account, transaction_type, amount, remarks) " +
                            "VALUES (?, 'DEPOSIT', ?, 'Self deposit')";

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();

            // STAGE 16: Set READ_COMMITTED isolation level.
            // Prevents dirty reads while avoiding unnecessary gap locks.
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);

            // STEP 1 (STAGE 16): Acquire row-level lock on account row.
            // This SELECT ... FOR UPDATE will block any other transaction trying
            // to lock or update this account row until we commit or rollback.
            String status;
            try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                lockStmt.setLong(1, accountNo);
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("\n[ERROR] Account not found. Deposit aborted.\n");
                        conn.rollback();
                        return false;
                    }
                    status = rs.getString("status");
                }
            }

            // STEP 2: Now that we hold the lock, check the status safely.
            if (!"ACTIVE".equals(status)) {
                conn.rollback();
                throw new AccountFrozenException(accountNo, status);
            }

            // STEP 3: Update the account balance (row is still locked by our FOR UPDATE)
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, amount);
                updateStmt.setLong(2, accountNo);
                updateStmt.executeUpdate();
            }

            // STEP 4: Log the deposit to the transactions table
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, accountNo);
                insertStmt.setDouble(2, amount);
                insertStmt.executeUpdate();
            }

            // STEP 5: Commit — permanently save all changes and RELEASE the row lock
            conn.commit();
            return true;

        } catch (AccountFrozenException e) {
            // Re-throw the domain exception — the caller handles it
            throw e;

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Deposit failed due to a database error.");
            System.out.println("        Details: " + e.getMessage());
            System.out.println("        Your balance has NOT been changed.\n");

            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("[ERROR] Rollback also failed: " + rollbackEx.getMessage());
            }

            return false;

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException closeEx) {
                System.out.println("[ERROR] Could not close connection: " + closeEx.getMessage());
            }
        }
    }

    // --------------------------------------------------
    // Method 2: Get the latest balance of an account
    // --------------------------------------------------
    /**
     * Fetches the current balance for the given account number from the database.
     *
     * WHY DO WE FETCH THE BALANCE AGAIN AFTER DEPOSIT?
     * After a successful deposit, we could just add the amount to our local variable.
     * But fetching from the DB gives us the ground-truth balance — useful if
     * concurrent operations could change the balance between our two steps.
     * It also confirms the UPDATE was saved correctly.
     *
     * SQL: SELECT balance FROM accounts WHERE account_no = ?
     *
     * @param accountNo the account number to look up
     * @return the current balance as a double, or -1.0 if the account is not found or an error occurs
     */
    public double getUpdatedBalance(long accountNo) {
        String sql = "SELECT balance FROM accounts WHERE account_no = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, accountNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not fetch updated balance: " + e.getMessage());
        }

        return -1.0;
    }

    // --------------------------------------------------
    // Method 3: Withdraw money from an account
    // --------------------------------------------------
    /**
     * Withdraws the given amount from the specified account.
     *
     * STAGE 16 CHANGE — TOCTOU Race Condition Fixed:
     * ------------------------------------------------
     * Previously, balance and status were checked BEFORE opening the JDBC transaction:
     *   getAccountStatus()    ← read-only, no lock
     *   getUpdatedBalance()   ← read-only, no lock
     *   ... [gap here where another thread can withdraw] ...
     *   conn.setAutoCommit(false)
     *   UPDATE balance ...
     *
     * Two concurrent withdrawals could both pass the balance check, then both
     * decrement — causing an overdraft.
     *
     * Now: All checks are done INSIDE the transaction using SELECT ... FOR UPDATE:
     *   conn.setAutoCommit(false)
     *   SELECT balance, status ... FOR UPDATE  ← acquires exclusive row lock
     *   [check status → throw AccountFrozenException if needed]
     *   [check balance → throw InsufficientFundsException if needed]
     *   UPDATE balance ...
     *   INSERT transaction log ...
     *   conn.commit()  ← row lock released here
     *
     * @param accountNo the account number to withdraw from
     * @param amount    the amount to withdraw
     * @return true if the withdrawal succeeded and was committed
     * @throws InsufficientFundsException if the account balance is less than the amount
     * @throws AccountFrozenException     if the account is not ACTIVE
     */
    public boolean withdrawAmount(long accountNo, double amount)
            throws InsufficientFundsException, AccountFrozenException {

        String lockSql   = "SELECT balance, status FROM accounts WHERE account_no = ? FOR UPDATE";
        String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_no = ?";
        String insertSql = "INSERT INTO transactions (from_account, transaction_type, amount, remarks) " +
                           "VALUES (?, 'WITHDRAWAL', ?, 'Self withdrawal')";

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);

            // STEP 1 (STAGE 16): Acquire exclusive row lock + read balance and status atomically.
            // No other transaction can modify this row until we commit or rollback.
            double currentBalance;
            String status;
            try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                lockStmt.setLong(1, accountNo);
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        System.out.println("\n[ERROR] Account not found. Withdrawal aborted.\n");
                        return false;
                    }
                    currentBalance = rs.getDouble("balance");
                    status         = rs.getString("status");
                }
            }

            // STEP 2: Status check — inside the lock, so status cannot change concurrently.
            if (!"ACTIVE".equals(status)) {
                conn.rollback();
                throw new AccountFrozenException(accountNo, status);
            }

            // STEP 3: Funds check — inside the lock, guaranteeing the balance we read is current.
            if (currentBalance < amount) {
                conn.rollback();
                throw new InsufficientFundsException(amount, currentBalance);
            }

            // STEP 4: Decrement the account balance
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, amount);
                updateStmt.setLong(2, accountNo);
                updateStmt.executeUpdate();
            }

            // STEP 5: Log the withdrawal to the transactions table
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, accountNo);
                insertStmt.setDouble(2, amount);
                insertStmt.executeUpdate();
            }

            // STEP 6: Commit — save changes and release row lock
            conn.commit();
            return true;

        } catch (InsufficientFundsException | AccountFrozenException e) {
            // Domain exceptions are re-thrown after rollback (already done above)
            throw e;

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Withdrawal failed due to a database error.");
            System.out.println("        Details: " + e.getMessage());
            System.out.println("        Your balance has NOT been changed.\n");

            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("[ERROR] Rollback also failed: " + rollbackEx.getMessage());
            }

            return false;

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException closeEx) {
                System.out.println("[ERROR] Could not close connection: " + closeEx.getMessage());
            }
        }
    }

    // --------------------------------------------------
    // Method 4: Check if an account exists
    // --------------------------------------------------
    /**
     * Checks if the specified account number exists in the accounts table.
     *
     * @param accountNo the account number to check
     * @return true if the account exists, false otherwise
     */
    public boolean accountExists(long accountNo) {
        String sql = "SELECT 1 FROM accounts WHERE account_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, accountNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error checking if account exists: " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------
    // Method 5: Fund Transfer
    // --------------------------------------------------
    /**
     * Transfers money from one account to another atomically.
     *
     * STAGE 16 CHANGE — TOCTOU Race Condition Fixed:
     * ------------------------------------------------
     * All pre-checks (status, balance, account existence) are now moved INSIDE
     * the JDBC transaction and performed via SELECT ... FOR UPDATE.
     *
     * DEADLOCK PREVENTION via Consistent Lock Ordering:
     * When two concurrent transfers involve the same two accounts in opposite directions,
     * they can deadlock if they acquire locks in different orders:
     *   Thread A locks account 1001, then waits for 1002.
     *   Thread B locks account 1002, then waits for 1001. → DEADLOCK.
     *
     * Fix: Always lock the lower account_no first. By locking in a deterministic
     * order (Math.min first, Math.max second), concurrent transfers can never deadlock.
     *
     * This method runs the following SQL statements inside a single database transaction:
     *   Step A: Lock accounts in deterministic order using SELECT ... FOR UPDATE
     *   Step B: Validate status, balance, and destination existence
     *   Step C: UPDATE accounts SET balance = balance - ? WHERE account_no = ? (from)
     *   Step D: UPDATE accounts SET balance = balance + ? WHERE account_no = ? (to)
     *   Step E: INSERT INTO transactions (from_account, to_account, transaction_type, amount, remarks)
     *
     * @param fromAccountNo the source account number
     * @param toAccountNo   the destination account number
     * @param amount        the amount to transfer
     * @param remarks       custom remarks for the transfer
     * @return true if the transfer was successful
     * @throws InsufficientFundsException if the source account balance is less than the amount
     * @throws InvalidAccountException    if the target account does not exist
     * @throws AccountFrozenException     if the source account is not ACTIVE
     */
    public boolean transferAmount(long fromAccountNo, long toAccountNo, double amount, String remarks)
            throws InsufficientFundsException, InvalidAccountException, AccountFrozenException {

        // Validate: source and destination cannot be the same account.
        // Done outside the transaction — no DB access needed.
        if (fromAccountNo == toAccountNo) {
            throw new InvalidAccountException(toAccountNo,
                "Destination account cannot be the same as the source account.");
        }

        // DEADLOCK PREVENTION: determine lock acquisition order.
        // Always lock lower account_no first, higher second.
        long firstLock  = Math.min(fromAccountNo, toAccountNo);
        long secondLock = Math.max(fromAccountNo, toAccountNo);

        String lockSql   = "SELECT account_no, balance, status FROM accounts WHERE account_no = ? FOR UPDATE";
        String debitSql  = "UPDATE accounts SET balance = balance - ? WHERE account_no = ?";
        String creditSql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
        String insertSql = "INSERT INTO transactions " +
                           "(from_account, to_account, transaction_type, amount, remarks) " +
                           "VALUES (?, ?, 'TRANSFER', ?, ?)";

        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);

            // STEP 1 (STAGE 16): Lock both account rows in deterministic order.
            // Row 1: lower account_no
            double fromBalance = -1;
            String fromStatus  = null;
            boolean toFound    = false;

            try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                lockStmt.setLong(1, firstLock);
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new InvalidAccountException(firstLock,
                            "Account number " + firstLock + " does not exist.");
                    }
                    long lockedNo = rs.getLong("account_no");
                    if (lockedNo == fromAccountNo) {
                        fromBalance = rs.getDouble("balance");
                        fromStatus  = rs.getString("status");
                    } else {
                        // firstLock is toAccountNo
                        toFound = true;
                    }
                }
            }

            // Row 2: higher account_no
            try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                lockStmt.setLong(1, secondLock);
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new InvalidAccountException(secondLock,
                            "Account number " + secondLock + " does not exist.");
                    }
                    long lockedNo = rs.getLong("account_no");
                    if (lockedNo == fromAccountNo) {
                        fromBalance = rs.getDouble("balance");
                        fromStatus  = rs.getString("status");
                    } else {
                        toFound = true;
                    }
                }
            }

            // STEP 2: Validate source account status (inside lock)
            if (fromStatus == null || !"ACTIVE".equals(fromStatus)) {
                conn.rollback();
                throw new AccountFrozenException(fromAccountNo,
                    fromStatus != null ? fromStatus : "UNKNOWN");
            }

            // STEP 3: Validate source account has sufficient funds (inside lock)
            if (fromBalance < amount) {
                conn.rollback();
                throw new InsufficientFundsException(amount, fromBalance);
            }

            // STEP 4: Debit the source account
            try (PreparedStatement debitStmt = conn.prepareStatement(debitSql)) {
                debitStmt.setDouble(1, amount);
                debitStmt.setLong(2, fromAccountNo);
                debitStmt.executeUpdate();
            }

            // STEP 5: Credit the destination account
            try (PreparedStatement creditStmt = conn.prepareStatement(creditSql)) {
                creditStmt.setDouble(1, amount);
                creditStmt.setLong(2, toAccountNo);
                creditStmt.executeUpdate();
            }

            // STEP 6: Log the transaction record
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, fromAccountNo);
                insertStmt.setLong(2, toAccountNo);
                insertStmt.setDouble(3, amount);
                insertStmt.setString(4, (remarks == null || remarks.trim().isEmpty())
                    ? "Fund Transfer" : remarks.trim());
                insertStmt.executeUpdate();
            }

            // STEP 7: Commit — saves all changes and releases both row locks
            conn.commit();
            return true;

        } catch (InsufficientFundsException | InvalidAccountException | AccountFrozenException e) {
            // Domain exceptions are re-thrown after rollback (already done above)
            throw e;

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Fund transfer failed due to a database error.");
            System.out.println("        Details: " + e.getMessage());
            System.out.println("        No money has been moved.\n");

            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("[ERROR] Rollback failed: " + rollbackEx.getMessage());
            }

            return false;

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException closeEx) {
                System.out.println("[ERROR] Could not close connection: " + closeEx.getMessage());
            }
        }
    }

    // --------------------------------------------------
    // Method 6: Get Mini Statement (Latest 5 Transactions)
    // --------------------------------------------------
    /**
     * Retrieves the latest 5 transactions associated with the given account number.
     *
     * @param accountNo the account number to retrieve transactions for
     * @return a List of Transaction objects sorted by transaction_time descending
     */
    public List<Transaction> getMiniStatement(long accountNo) {
        List<Transaction> miniStatement = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE from_account = ? OR to_account = ? " +
                     "ORDER BY transaction_time DESC LIMIT 5";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, accountNo);
            pstmt.setLong(2, accountNo);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction tx = new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getLong("from_account"),
                        rs.getLong("to_account"),
                        rs.getString("transaction_type"),
                        rs.getDouble("amount"),
                        rs.getString("transaction_time"),
                        rs.getString("remarks")
                    );
                    miniStatement.add(tx);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Could not fetch transaction history: " + e.getMessage());
        }

        return miniStatement;
    }

    // --------------------------------------------------
    // Method 7: Paginated & Filtered Transaction History (Stage 14)
    // --------------------------------------------------
    /**
     * Fetches paginated and filtered transactions associated with a given account (or all accounts if accountNo <= 0).
     *
     * Supports filtering by:
     *   - transactionType ("DEPOSIT", "WITHDRAWAL", "TRANSFER", or null/ALL)
     *   - minAmount / maxAmount range
     *   - startDate / endDate (YYYY-MM-DD string format)
     *
     * @param accountNo    account number to query (or 0 for all accounts)
     * @param page         1-indexed page number
     * @param pageSize     number of records per page
     * @param typeFilter   transaction type filter
     * @param minAmount    minimum transaction amount filter
     * @param maxAmount    maximum transaction amount filter
     * @param startDate    start date filter (YYYY-MM-DD)
     * @param endDate      end date filter (YYYY-MM-DD)
     * @return PageResult of matching Transaction records
     */
    public PageResult<Transaction> getPaginatedTransactionsForAccount(long accountNo, int page, int pageSize,
                                                                      String typeFilter, Double minAmount,
                                                                      Double maxAmount, String startDate, String endDate) {
        int validPage     = Math.max(1, page);
        int validPageSize = Math.max(1, pageSize);
        int offset        = (validPage - 1) * validPageSize;

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (accountNo > 0) {
            whereClause.append(" AND (from_account = ? OR to_account = ?)");
            params.add(accountNo);
            params.add(accountNo);
        }

        if (typeFilter != null && !typeFilter.trim().isEmpty() && !typeFilter.equalsIgnoreCase("ALL")) {
            whereClause.append(" AND transaction_type = ?");
            params.add(typeFilter.trim().toUpperCase());
        }

        if (minAmount != null && minAmount >= 0) {
            whereClause.append(" AND amount >= ?");
            params.add(minAmount);
        }

        if (maxAmount != null && maxAmount >= 0) {
            whereClause.append(" AND amount <= ?");
            params.add(maxAmount);
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            whereClause.append(" AND transaction_time >= ?");
            params.add(startDate.trim() + " 00:00:00");
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            whereClause.append(" AND transaction_time <= ?");
            params.add(endDate.trim() + " 23:59:59");
        }

        String countSql  = "SELECT COUNT(*) FROM transactions" + whereClause;
        String selectSql = "SELECT * FROM transactions" + whereClause +
                           " ORDER BY transaction_time DESC, transaction_id DESC LIMIT ? OFFSET ?";

        long totalRecords = 0;
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {

            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    countStmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        totalRecords = rs.getLong(1);
                    }
                }
            }

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                int paramIdx = 1;
                for (Object p : params) {
                    selectStmt.setObject(paramIdx++, p);
                }
                selectStmt.setInt(paramIdx++, validPageSize);
                selectStmt.setInt(paramIdx, offset);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        Transaction tx = new Transaction(
                            rs.getInt("transaction_id"),
                            rs.getLong("from_account"),
                            rs.getLong("to_account"),
                            rs.getString("transaction_type"),
                            rs.getDouble("amount"),
                            rs.getString("transaction_time"),
                            rs.getString("remarks")
                        );
                        transactions.add(tx);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Error fetching paginated transactions: " + e.getMessage());
        }

        return new PageResult<>(transactions, validPage, validPageSize, totalRecords);
    }
}
