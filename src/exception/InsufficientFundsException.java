package exception;

/**
 * InsufficientFundsException - Custom checked exception for withdrawal failures.
 *
 * WHAT IS A CUSTOM EXCEPTION?
 * Java allows you to create your own exception classes by extending Exception
 * (checked) or RuntimeException (unchecked).
 *
 * WHY CREATE A CUSTOM EXCEPTION INSTEAD OF A SIMPLE if-RETURN?
 * 1. CLARITY: The exception name itself communicates exactly what went wrong.
 *    A plain 'return false' gives no information about WHY the method failed.
 * 2. RICH DATA: Custom exceptions can carry extra fields (amountRequested,
 *    availableBalance) that plain boolean returns cannot.
 * 3. CLEAN SEPARATION: The DAO (TransactionDAO) deals with the business rule
 *    violation, while the UI (CustomerMenu) decides how to display it.
 *    Neither layer is mixed into the other.
 * 4. INDUSTRY PRACTICE: Real banking and enterprise systems use domain-specific
 *    exceptions for all business rule violations.
 *
 * WHY EXTEND Exception (NOT RuntimeException)?
 * Extending Exception creates a CHECKED exception — Java forces every caller
 * of withdrawAmount() to either catch it or declare 'throws InsufficientFundsException'
 * in its method signature. This makes the failure case impossible to ignore
 * accidentally, which is exactly what we want for a financial operation.
 *
 * Extending RuntimeException would make it unchecked — callers could forget
 * to handle it and the program would crash at runtime instead of compile time.
 */
public class InsufficientFundsException extends Exception {

    // --------------------------------------------------
    // Fields — carry extra context about the failure
    // --------------------------------------------------
    private final double amountRequested;  // The amount the customer tried to withdraw
    private final double availableBalance; // The actual balance in the account at time of attempt

    // --------------------------------------------------
    // Constructor
    // --------------------------------------------------
    /**
     * Creates an InsufficientFundsException with the requested and available amounts.
     *
     * Calls super(message) to set the exception's message — this is what
     * Exception.getMessage() returns, and what appears in stack traces.
     *
     * We build a descriptive message here so that any code that catches this
     * exception and calls e.getMessage() gets a useful, human-readable string.
     *
     * @param amountRequested  the amount the customer attempted to withdraw
     * @param availableBalance the balance available in the account
     */
    public InsufficientFundsException(double amountRequested, double availableBalance) {
        super(String.format(
            "Insufficient funds. Requested: Rs. %,.2f, Available: Rs. %,.2f",
            amountRequested, availableBalance
        ));
        this.amountRequested  = amountRequested;
        this.availableBalance = availableBalance;
    }

    // --------------------------------------------------
    // Getters — allow the catcher to access specific values
    // --------------------------------------------------
    // CustomerMenu uses these to display formatted amounts separately,
    // rather than parsing the message string.

    public double getAmountRequested() {
        return amountRequested;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }
}
