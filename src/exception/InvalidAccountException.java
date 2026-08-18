package exception;

/**
 * InvalidAccountException - Custom checked exception for fund transfers and account lookups.
 * 
 * This is thrown when a customer attempts to perform an operation on a target account
 * that does not exist in the database or is not in an ACTIVE status.
 */
public class InvalidAccountException extends Exception {
    private static final long serialVersionUID = 1L;

    private final long invalidAccountNo;

    /**
     * Constructor for InvalidAccountException.
     * 
     * @param invalidAccountNo the account number that was invalid
     * @param message          descriptive error message
     */
    public InvalidAccountException(long invalidAccountNo, String message) {
        super(message);
        this.invalidAccountNo = invalidAccountNo;
    }

    public long getInvalidAccountNo() {
        return invalidAccountNo;
    }
}
