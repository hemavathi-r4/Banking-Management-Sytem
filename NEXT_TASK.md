# 📌 NEXT TASK — Stage 7: Fund Transfer with SQL Transaction Atomicity

**Planned Date:** July 14, 2026 (Monday)  
**Prerequisite:** Stage 6 must be tested and working (Withdrawal Functionality)

---

## 🎯 Goal

Implement fund transfer functionality for logged-in customers. When a customer selects "4. Fund Transfer" from the Customer Dashboard, the system should:
1. Fetch the customer's accounts and display them as options for the source account.
2. Prompt the customer to select the source account.
3. Prompt for the target account number (destination).
4. Prompt for the transfer amount (minimum Rs. 100, must be positive).
5. Prompt for custom transaction remarks (optional).
6. Verify the following business rules:
   - Destination account must exist in the database.
   - Destination account must not be the same as the source account.
   - Source account must have sufficient funds.
7. Execute the transfer atomically as a single database transaction (decrements source, increments target, inserts transaction log).
8. Display success confirmation showing updated balances.

---

## 📝 Tasks

### Task 1 — Create `InvalidAccountException.java` in `src/exception/`

- Create a custom checked exception class extending `Exception`.
- Fields:
  - `private long invalidAccountNo`
- Implement a constructor:
  - `InvalidAccountException(long invalidAccountNo, String message)`
- Implement getter for the field.

### Task 2 — Extend `TransactionDAO.java` in `src/dao/`

- Add a new method:
  - `transferAmount(long fromAccountNo, long toAccountNo, double amount, String remarks) throws InsufficientFundsException, InvalidAccountException`
    - **Step 1:** Fetch current balance of `fromAccountNo`. If `balance < amount`, throw `InsufficientFundsException`.
    - **Step 2:** Check if `toAccountNo` exists in the database. If not, throw `InvalidAccountException`.
    - **Step 3:** Perform atomic operations under `conn.setAutoCommit(false)`:
      1. UPDATE `fromAccountNo` balance (subtract amount).
      2. UPDATE `toAccountNo` balance (add amount).
      3. INSERT transaction record: `INSERT INTO transactions (from_account, to_account, transaction_type, amount, remarks) VALUES (?, ?, 'TRANSFER', ?, ?)`
    - **Step 4:** `conn.commit()` on success, `conn.rollback()` on any `SQLException`.
    - **Step 5:** Always restore auto-commit and release resources.

### Task 3 — Update `CustomerMenu.java` in `src/menu/`

- Modify option `4` ("Fund Transfer") to call a new private `fundTransfer(customer, scanner)` method.
- The `fundTransfer()` method must:
  - Select source account from the list.
  - Prompt for target account number and validate it.
  - Prompt for amount (numeric, >= 100).
  - Prompt for optional remarks.
  - Call `transactionDAO.transferAmount(fromAcc, toAcc, amount, remarks)` in a `try-catch` catching:
    - `InsufficientFundsException` -> print error details.
    - `InvalidAccountException` -> print error details.
  - On success, display a success banner with new source balance.

---

## 📂 Files to Create / Modify

| File | Action | Purpose |
|------|--------|---------|
| `src/exception/InvalidAccountException.java` | NEW | Custom exception for nonexistent or inactive accounts |
| `src/dao/TransactionDAO.java` | MODIFY | Add `transferAmount()` logic with JDBC transaction control |
| `src/menu/CustomerMenu.java` | MODIFY | Implement option 4 UI flow |

---

## ✅ Definition of Done

- [ ] `InvalidAccountException` is implemented and used for invalid target accounts.
- [ ] Transfer amount is validated to be positive and >= Rs. 100.
- [ ] Transfer decrement, increment, and transaction log are executed atomically (using commit/rollback).
- [ ] In case of any exception or database failure, no funds are moved (guaranteed transaction rollback).
- [ ] User dashboard handles exceptions gracefully.
