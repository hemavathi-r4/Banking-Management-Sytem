# 📌 NEXT TASK — Stage 3: Customer Login & Authentication

**Planned Date:** July 2, 2026 (Wednesday)  
**Prerequisite:** Stage 2 must be tested and working (Customer Registration)

---

## 🎯 Goal

Implement Customer Login so that registered customers can authenticate using their **email** and **password**. After successful login, display a **Customer Dashboard** menu with placeholder options for future features.

---

## 📝 Tasks

### Task 1 — Add `loginCustomer()` method to `CustomerDAO.java`

- Add a new method: `loginCustomer(String email, String password)`
- Use a `SELECT` query with `PreparedStatement` to find a customer matching both email and password
- Return a `Customer` object if credentials match, or `null` if they don't
- Handle `SQLException` gracefully — don't crash the app

**Expected SQL:**
```sql
SELECT * FROM customers WHERE email = ? AND password = ?
```

---

### Task 2 — Create `CustomerMenu.java` in `src/menu/`

- This is the **Customer Dashboard** — shown only after successful login
- Display a menu with these options (most will be stubs for now):
  1. View Account Details *(stub — Stage 4)*
  2. Deposit *(stub — Stage 5)*
  3. Withdraw *(stub — Stage 6)*
  4. Fund Transfer *(stub — Stage 7)*
  5. Mini Statement *(stub — Stage 8)*
  6. Logout *(functional — returns to main menu)*
- Accept the logged-in `Customer` object and `Scanner` as parameters
- Show a welcome message: `"Welcome, [customer name]!"`

---

### Task 3 — Create `LoginMenu.java` in `src/menu/`

- Handle the login user interface
- Prompt the user for email and password
- Call `customerDAO.loginCustomer(email, password)`
- If login succeeds → call `CustomerMenu` to show the dashboard
- If login fails → show error message: `"Invalid email or password"`
- Return to main menu after logout or failed login

---

### Task 4 — Wire Login to `Main.java`

- Connect main menu option `2` ("Customer Login") to `LoginMenu`
- Replace the current stub message with actual login flow
- Minimal changes to Main.java (same approach as Stage 2)

---

## 📂 Files to Create / Modify

| File | Action | Purpose |
|------|--------|---------|
| `src/dao/CustomerDAO.java` | MODIFY | Add `loginCustomer(String email, String password)` method |
| `src/menu/LoginMenu.java` | NEW | Console UI for login — collects email & password |
| `src/menu/CustomerMenu.java` | NEW | Customer Dashboard — post-login menu with stub options |
| `src/Main.java` | MODIFY | Wire option 2 to `LoginMenu` |

---

## 🏗️ Expected Flow

```
Main Menu → User selects "2. Customer Login"
    │
    ▼
LoginMenu → Prompts for email and password
    │
    ▼
CustomerDAO.loginCustomer(email, password)
    │
    ├── Match found → Returns Customer object
    │       │
    │       ▼
    │   CustomerMenu → Shows dashboard with options
    │       │
    │       └── User selects "6. Logout" → Back to Main Menu
    │
    └── No match → Prints "[ERROR] Invalid email or password"
                → Back to Main Menu
```

---

## ✅ Definition of Done

- [ ] `loginCustomer()` correctly validates credentials against the database
- [ ] Invalid login shows a clear error message (no crash)
- [ ] Successful login shows the Customer Dashboard with the customer's name
- [ ] Logout returns to the main menu cleanly
- [ ] All existing code (Stage 1 & 2) remains untouched and functional
- [ ] Code is well-commented and beginner-friendly

---

## 🚫 What NOT to Implement in Stage 3

- ❌ Account creation
- ❌ Deposit / Withdrawal / Transfer logic
- ❌ Transaction history
- ❌ Admin login

Only implement **Customer Login**, the **Customer Dashboard** (with stubs), and wire it to the main menu.

---

> ⚠️ **Do NOT start Stage 3 until Stage 2 is fully tested and confirmed working.**
