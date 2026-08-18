package exception;

/**
 * AccountFrozenException - Custom checked exception for operations on frozen/closed accounts.
 *
 * OOP CONCEPT — DOMAIN-DRIVEN EXCEPTION:
 * This exception conveys that a transaction was rejected because the target
 * account is not in ACTIVE status (e.g., FROZEN or CLOSED). It carries the
 * account number and current status for display in the UI.
 */
public class AccountFrozenException extends Exception {
    private static final long serialVersionUID = 1L;

    private final long accountNo;
    private final String accountStatus;

    /**
     * Constructs an AccountFrozenException.
     *
     * @param accountNo     the account number that is frozen/closed
     * @param accountStatus the current status of the account (e.g. "FROZEN", "CLOSED")
     */
    public AccountFrozenException(long accountNo, String accountStatus) {
        super("Account " + accountNo + " is currently " + accountStatus + ". Operation denied.");
        this.accountNo = accountNo;
        this.accountStatus = accountStatus;
    }

    public long getAccountNo() {
        return accountNo;
    }

    public String getAccountStatus() {
        return accountStatus;
    }
}
