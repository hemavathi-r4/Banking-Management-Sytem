# 📌 NEXT TASK — Stage 5: Deposit Functionality

**Planned Date:** July 11, 2026 (Friday)  
**Prerequisite:** Stage 4 must be tested and working (Account Creation & Balance Checking)

---

## 🎯 Goal

Implement deposit functionality for logged-in customers. When a customer selects "2. Deposit" from the Customer Dashboard, the system should:
1. Fetch the customer's accounts and display them.
2. Prompt the customer to select which account to deposit into.
3. Prompt for a deposit amount (minimum Rs. 500).
4. Update the account balance in the database.
5. Log the transaction to the `transactions` table.
6. Display a success confirmation with the updated balance.

---

## 📝 Tasks

### Task 1 — Create `Transaction.java` in `src/model/`

- Create a POJO class representing a transaction matching the `transactions` database table.
- Fields:
  - `private int transactionId`
  - `private long fromAccount`
  - `private long toAccount`
  - `private String transactionType`
  - `private double amount`
  - `private String transactionTime`
  - `private String remarks`
- Implement default constructor, constructor for deposit (no `transactionId`, no `fromAccount`), and full constructor (for DB loading).
- Implement getters, setters, and `toString()`.

---

### Task 2 — Create `TransactionDAO.java` in `src/dao/`

- Implement data access logic for transactions:
  - `depositAmount(long accountNo, double amount)`: Updates the account balance using:
    - `UPDATE accounts SET balance = balance + ? WHERE account_no = ?`
    - After successful update, insert a row into `transactions`:
      - `INSERT INTO transactions (to_account, transaction_type, amount, remarks) VALUES (?, 'DEPOSIT', ?, ?)`
    - Both SQL operations must be wrapped in a **single database transaction** (use `conn.setAutoCommit(false)` + `conn.commit()` / `conn.rollback()`).
    - Handle `SQLException` gracefully.
  - `getUpdatedBalance(long accountNo)`: Fetches and returns the latest balance for a given account:
    - `SELECT balance FROM accounts WHERE account_no = ?`

---

### Task 3 — Update `CustomerMenu.java` in `src/menu/`

- Instantiate `TransactionDAO` in `CustomerMenu`.
- Modify option `2` ("Deposit") to:
  - Fetch accounts for the logged-in customer using `accountDAO.getAccountsByCustomerId()`.
  - If no accounts exist:
    - Display: `"You do not have any accounts to deposit into. Please open an account first."`
    - Return to dashboard.
  - If accounts exist:
    - Display numbered list of accounts (Account No + Type).
    - Prompt the customer to select which account to deposit into.
    - Validate selection (must be a valid number in range).
    - Prompt for deposit amount: `"Enter amount to deposit (Min Rs. 500): "`
    - Validate: amount must be a valid number and `>= 500`.
    - Call `transactionDAO.depositAmount(accountNo, amount)`.
    - Fetch updated balance using `transactionDAO.getUpdatedBalance(accountNo)`.
    - Display success confirmation with new balance.

---

## 📂 Files to Create / Modify

| File | Action | Purpose |
|------|--------|---------|
| `src/model/Transaction.java` | NEW | Model representing the transactions table |
| `src/dao/TransactionDAO.java` | NEW | DB logic for deposit update and transaction log insert |
| `src/menu/CustomerMenu.java` | MODIFY | Implement Deposit flow for option 2 |

---

## 🏗️ Expected Flow

```
Customer Dashboard → User selects "2. Deposit"
    │
    ▼
accountDAO.getAccountsByCustomerId(customerId)
    │
    ├── No accounts ──► "No accounts found. Open an account first."
    │                   ──► Return to Dashboard
    │
    └── Accounts found ──► Display numbered list of accounts
                           ──► Prompt: "Select account number (1/2/...): "
                           [Validate selection]
                               │
                               ▼
                           Prompt: "Enter amount to deposit (Min Rs. 500): "
                           [Validate: numeric, >= 500]
                               │
                               ▼
                           transactionDAO.depositAmount(accountNo, amount)
                               │
                               ├── UPDATE accounts SET balance = balance + ? WHERE account_no = ?
                               └── INSERT INTO transactions (to_account, type, amount, remarks)
                               [Both inside a single DB transaction with commit/rollback]
                               │
                               ▼
                           transactionDAO.getUpdatedBalance(accountNo)
                               │
                               ▼
                           "✓ Deposit Successful! New Balance: Rs. X,XXX.XX"
                               │
                               ▼
                           Return to Dashboard
```

---

## ✅ Definition of Done

- [ ] `Transaction` model and `TransactionDAO` classes are successfully created and structured.
- [ ] Deposit updates the `accounts.balance` column correctly in the database.
- [ ] Each deposit is recorded as a row in the `transactions` table with `transaction_type = 'DEPOSIT'`.
- [ ] Both the balance update and the transaction insert succeed atomically — if one fails, the other is rolled back.
- [ ] Minimum deposit amount of Rs. 500 is enforced with looped validation.
- [ ] Updated balance is fetched from the database and shown to the customer after deposit.
- [ ] All database resources (Connections, Statements, ResultSets) are safely released using `try-with-resources`.

---

## 🚫 What NOT to Implement in Stage 5

- ❌ Withdrawal functionality (Stage 6)
- ❌ Fund Transfer (Stage 7)
- ❌ Transaction History display (Stage 8)
- ❌ Admin operations (Stage 9)

---

> ⚠️ **Do NOT start Stage 5 until Stage 4 is fully tested and confirmed working.**
