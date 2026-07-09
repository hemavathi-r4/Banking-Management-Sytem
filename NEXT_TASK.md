# 📌 NEXT TASK — Stage 6: Withdrawal with Custom Exception Handling

**Planned Date:** July 12, 2026 (Saturday)  
**Prerequisite:** Stage 5 must be tested and working (Deposit Functionality)

---

## 🎯 Goal

Implement withdrawal functionality for logged-in customers. When a customer selects "3. Withdraw" from the Customer Dashboard, the system should:
1. Fetch the customer's accounts and display them.
2. Prompt the customer to select which account to withdraw from.
3. Prompt for a withdrawal amount (minimum Rs. 500).
4. Validate that the account has sufficient funds.
5. If insufficient: throw and catch a **custom exception** (`InsufficientFundsException`) — do NOT use a plain `if` check return.
6. If sufficient: Update the account balance and log the transaction atomically.
7. Display a success confirmation with the updated balance.

---

## 📝 Tasks

### Task 1 — Create `InsufficientFundsException.java` in `src/exception/`

- Create a **custom checked exception** class extending `Exception`.
- Fields:
  - `private double amountRequested`
  - `private double availableBalance`
- Implement a constructor:
  - `InsufficientFundsException(double amountRequested, double availableBalance)`
  - Sets both fields and passes a descriptive message to `super()`:
    - e.g. `"Insufficient funds. Requested: Rs. X.XX, Available: Rs. X.XX"`
- Implement getters for both fields.
- Override `getMessage()` if needed for a clean display.

---

### Task 2 — Extend `TransactionDAO.java` in `src/dao/`

- Add a new method:
  - `withdrawAmount(long accountNo, double amount) throws InsufficientFundsException`
    - **Step 1:** Fetch the current balance using `getUpdatedBalance(accountNo)`.
    - **Step 2:** If `balance < amount`, throw `new InsufficientFundsException(amount, balance)`.
    - **Step 3:** If sufficient, run two SQL statements atomically (same pattern as `depositAmount`):
      - `UPDATE accounts SET balance = balance - ? WHERE account_no = ?`
      - `INSERT INTO transactions (from_account, transaction_type, amount, remarks) VALUES (?, 'WITHDRAWAL', ?, 'Self withdrawal')`
    - **Step 4:** `conn.commit()` on success, `conn.rollback()` on any `SQLException`.
    - **Step 5:** Always restore `conn.setAutoCommit(true)` and close in a `finally` block.

---

### Task 3 — Update `CustomerMenu.java` in `src/menu/`

- Modify option `3` ("Withdraw") to call a new private `withdraw(customer, scanner)` method.
- The `withdraw()` method must:
  - Fetch accounts. If none: display message and return.
  - Display numbered account list (same format as deposit).
  - Validate account selection (same loop as deposit).
  - Prompt for withdrawal amount (minimum Rs. 500, same loop as deposit).
  - Call `transactionDAO.withdrawAmount(accountNo, amount)` inside a **try-catch** that catches `InsufficientFundsException`:
    - On `InsufficientFundsException`: display:
      ```
      [!] Withdrawal Failed: Insufficient funds.
          Requested : Rs. X,XXX.XX
          Available : Rs. X,XXX.XX
      ```
    - On success: fetch updated balance, display the `✓ Withdrawal Successful!` banner.

---

## 📂 Files to Create / Modify

| File | Action | Purpose |
|------|--------|---------|
| `src/exception/InsufficientFundsException.java` | NEW | Custom checked exception for insufficient balance |
| `src/dao/TransactionDAO.java` | MODIFY | Add `withdrawAmount()` method |
| `src/menu/CustomerMenu.java` | MODIFY | Implement Withdraw flow for option 3 |

---

## 🏗️ Expected Flow

```
Customer Dashboard → User selects "3. Withdraw"
    │
    ▼
accountDAO.getAccountsByCustomerId(customerId)
    │
    ├── No accounts ──► "No accounts found." ──► Return to Dashboard
    │
    └── Accounts found ──► Display numbered list
                           ──► Prompt: "Select account (1/2/...): "
                           [Validate selection]
                               │
                               ▼
                           Prompt: "Enter amount to withdraw (Min Rs. 500): "
                           [Validate: numeric, >= 500]
                               │
                               ▼
                           transactionDAO.withdrawAmount(accountNo, amount)
                               │
                               ├── getUpdatedBalance(accountNo)
                               │       │
                               │   balance < amount?
                               │       │
                               │       └── YES ──► throw InsufficientFundsException
                               │                        │
                               │                        ▼
                               │               CustomerMenu catches it
                               │               Displays:
                               │                 "[!] Insufficient funds."
                               │                 "Requested: Rs. X,XXX.XX"
                               │                 "Available: Rs. X,XXX.XX"
                               │                        │
                               │                        ▼
                               │               Return to Dashboard
                               │
                               └── balance >= amount ──► Atomic:
                                       UPDATE accounts SET balance = balance - ?
                                       INSERT INTO transactions ('WITHDRAWAL', ...)
                                       conn.commit()
                                           │
                                           ▼
                                   getUpdatedBalance(accountNo)
                                           │
                                           ▼
                                   "✓ Withdrawal Successful!"
                                   "Amount Withdrawn : Rs. X,XXX.XX"
                                   "Updated Balance  : Rs. X,XXX.XX"
                                           │
                                           ▼
                                   Return to Dashboard
```

---

## ✅ Definition of Done

- [ ] `InsufficientFundsException` is a custom checked exception in the `exception` package.
- [ ] `withdrawAmount()` throws `InsufficientFundsException` (does NOT use a plain `return false` for insufficient funds).
- [ ] Withdrawal updates the `accounts.balance` column correctly (decrements, not increments).
- [ ] Each withdrawal is recorded in the `transactions` table with `transaction_type = 'WITHDRAWAL'`.
- [ ] Balance update and transaction log insert are atomic (commit/rollback).
- [ ] `CustomerMenu.withdraw()` catches `InsufficientFundsException` and displays a clear, detailed error.
- [ ] Minimum withdrawal amount of Rs. 500 is enforced with looped validation.
- [ ] Updated balance is fetched from the database and shown on success.
- [ ] All DB resources are safely released.

---

## 🚫 What NOT to Implement in Stage 6

- ❌ Fund Transfer (Stage 7)
- ❌ Transaction History display (Stage 8)
- ❌ Admin operations (Stage 9)
- ❌ Account freezing or status checks (the `status` column exists in DB but enforcement is a polish task)

---

> ⚠️ **Do NOT start Stage 6 until Stage 5 is fully tested and confirmed working.**
