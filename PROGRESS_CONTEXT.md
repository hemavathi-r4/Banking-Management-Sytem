# 📈 PROGRESS CONTEXT — Banking Management System

**Date:** July 3, 2026 (Friday)  
**Stage Completed:** Stage 3 — Customer Login & Authentication  
**Tech Stack:** Core Java, JDBC, MySQL, Console Application

---

## 📅 Day 1 — June 30, 2026 (Monday)

### Stage 1 — Project Setup & Database Foundation ✅

**What was done:**
- Created the entire project folder structure with organized packages.
- Wrote the SQL script (`bank_management.sql`) that creates 4 tables: `customers`, `accounts`, `transactions`, `admins`.
- Built `DBConnection.java` — a centralized JDBC utility class that provides database connections.
- Created `Main.java` — the application entry point with a database connection test and a main menu loop (with placeholder stubs).

**Files created on Day 1:**

| File | Location | Purpose |
|------|----------|---------|
| `bank_management.sql` | `sql/` | SQL script to create the database and all tables |
| `DBConnection.java` | `src/database/` | JDBC connection utility — stores DB URL, username, password; provides `getConnection()` |
| `Main.java` | `src/` | Application entry point — tests DB on startup, displays main menu in a loop |

---

## 📅 Day 2 — July 1, 2026 (Tuesday)

### Stage 2 — Customer Registration ✅

**Goal:** Allow new customers to register via the console. Their data is saved to the `customers` table in MySQL. Duplicate emails and phone numbers are detected and rejected.

**Files created/modified on Day 2:**
- `src/model/Customer.java` (New POJO representation of customer records)
- `src/dao/CustomerDAO.java` (New DAO class handling registration INSERT and checks)
- `src/menu/RegistrationMenu.java` (New registration console UI)
- `src/Main.java` (Modified to import and wire option 1 to `RegistrationMenu`)

---

## 📅 Day 3 — July 3, 2026 (Friday)

### Stage 3 — Customer Login & Authentication ✅

**Goal:** Implement Customer Login so that registered customers can authenticate using their email and password. Upon successful login, display a Customer Dashboard menu with options for account details, deposits, withdrawals, fund transfers, and statements (stubs for now), along with a functional logout option.

---

### 📁 File 1: `src/dao/CustomerDAO.java` — MODIFIED

**What changed:**
- Added a `loginCustomer(String email, String password)` method to query the database.
- Runs `SELECT * FROM customers WHERE email = ? AND password = ?` safely via `PreparedStatement`.
- Returns a fully-populated `Customer` object if a matching row is found, or `null` if credentials are invalid.
- Catches `SQLException` locally to prevent the app from crashing in case of database connectivity issues.

---

### 📁 File 2: `src/menu/LoginMenu.java` — NEW

**What it is:**  
A console menu class that handles the user interface for customer login.

**What's inside:**
- Prompts the user for email and password.
- Trims and validates inputs (making sure email and password are not empty).
- Calls `customerDAO.loginCustomer(email, password)`.
- If login succeeds → redirects to `CustomerMenu` to display the customer dashboard.
- If login fails → displays `[ERROR] Invalid email or password` and returns to the main menu.

---

### 📁 File 3: `src/menu/CustomerMenu.java` — NEW

**What it is:**  
The post-login dashboard console UI.

**What's inside:**
- Welcomes the customer by name: `"Welcome, [customer name]!"`.
- Displays a dashboard menu loop:
  1. View Account Details (Stub — Stage 4)
  2. Deposit (Stub — Stage 5)
  3. Withdraw (Stub — Stage 6)
  4. Fund Transfer (Stub — Stage 7)
  5. Mini Statement (Stub — Stage 8)
  6. Logout (exits loop, returning to LoginMenu and back to Main Menu)

---

### 📁 File 4: `src/Main.java` — MODIFIED

**What changed:**
- Imported `menu.LoginMenu`.
- Instantiated `LoginMenu loginMenu = new LoginMenu();` next to `RegistrationMenu`.
- Wired Option `2` ("Customer Login") to `loginMenu.showLoginForm(scanner);` replacing the Stage 2 stub message.

---

## 🏗️ Architecture Flow — How Login Works End-to-End

```
User runs the application
        │
        ▼
Main menu displays → User selects "2. Customer Login"
        │
        ▼
Main.java calls → loginMenu.showLoginForm(scanner)
        │
        ▼
LoginMenu prompts for: Email and Password
        │
        ▼
Input validation → Are email/password filled? (If no → error & return)
        │
        ▼
Calls → customerDAO.loginCustomer(email, password)
        │
        ├──→ If match found → Returns Customer object
        │         │
        │         ▼
        │    LoginMenu redirects to → customerMenu.showDashboard(customer, scanner)
        │         │
        │         ▼
        │    Customer dashboard shows options:
        │      - Options 1-5: Print stub message
        │      - Option 6: "Logging out..." → Exits loop and returns to Main Menu
        │
        └──→ If no match → Prints "[ERROR] Invalid email or password"
                  │
                  ▼
             Returns to Main Menu
```

---

## 📂 Current Folder Structure After Stage 3

```
BMS/
├── src/
│   ├── model/
│   │   └── Customer.java          ← Stage 2
│   ├── dao/
│   │   └── CustomerDAO.java       ← MODIFIED (Stage 3)
│   ├── database/
│   │   └── DBConnection.java      ← Stage 1 (unchanged)
│   ├── exception/                 ← (empty — future stages)
│   ├── menu/
│   │   ├── RegistrationMenu.java  ← Stage 2
│   │   ├── LoginMenu.java         ← NEW (Stage 3)
│   │   └── CustomerMenu.java      ← NEW (Stage 3)
│   └── Main.java                  ← MODIFIED (Stage 3)
├── sql/
│   └── bank_management.sql        ← Stage 1
├── lib/                           ← (contains mysql-connector-j-8.0.33.jar)
├── PROJECT_CONTEXT.md
├── PROGRESS_CONTEXT.md            ← This file
└── NEXT_TASK.md
```

---

## 🧠 OOP & Design Concepts Used in Stage 3

| Concept | Where It's Applied |
|---------|-------------------|
| **Encapsulation** | `Customer` objects instantiated from the DB encapsulate user data, which is passed securely between menus. |
| **DAO Pattern** | Login query resides inside `CustomerDAO.java`. The LoginMenu only expects a Customer object or null back. |
| **Separation of Concerns** | Logic is separated: UI menu classes manage input/output formatting, DAO manages connection & execution, Model encapsulates data. |
| **Single Responsibility** | LoginMenu controls credential input, CustomerMenu controls dashboard navigation, CustomerDAO controls database authentication. |
| **Defensive Programming** | Parameter binding protects against SQL Injection, input validation prevents blank login attempts, exception handling handles DB errors gracefully. |

---

## 📊 Overall Progress Tracker

| Stage | Description | Status | Date |
|-------|-------------|--------|------|
| 1 | Project setup, DB schema, DB connection | ✅ Complete | June 30, 2026 |
| 2 | Customer Registration (model, DAO, menu) | ✅ Complete | July 1, 2026 |
| 3 | Customer Login & Authentication | ✅ Complete | July 3, 2026 |
| 4 | Account Creation & Balance Checking | ⬜ Not started | — |
| 5 | Deposit Functionality | ⬜ Not started | — |
| 6 | Withdrawal with Custom Exceptions | ⬜ Not started | — |
| 7 | Fund Transfer with SQL Transactions | ⬜ Not started | — |
| 8 | Transaction History & Mini Statement | ⬜ Not started | — |
| 9 | Admin Module | ⬜ Not started | — |
| 10 | Code Polish, Documentation, README | ⬜ Not started | — |

---

## 💡 Suggested Git Commit Message for Stage 3

```
feat: implement administrator operations module with OOP inheritance (Stage 9)

- Create User.java base superclass with common id/password fields (OOP Inheritance)
- Create Admin.java model extending User with username field
- Refactor Customer.java to extend User, delegating customerId/password to inherited fields
- Create AdminDAO.java with authenticate, CRUD customers, account status, transactions, statistics
- Create AdminMenu.java with login prompt and 8-option admin dashboard
- Update Main.java: wire Option 3 to AdminMenu.showLoginForm()
- deleteCustomer() uses atomic JDBC transactions to cascade-delete transactions, accounts, and customer
```

---

## 📅 Stage 11 — Security Upgrade (August 18, 2026)

### Goal
Harden authentication and database security without breaking the existing functionality.

### Key Changes

**1. Password Hashing (BCrypt)**
- Added `jbcrypt-0.4.jar` to `lib/`
- Created `src/util/PasswordUtil.java` — wraps `BCrypt.hashpw()` (hash) and `BCrypt.checkpw()` (verify) with cost factor 12
- Modified `CustomerDAO.registerCustomer()`: hashes the password with `PasswordUtil.hashPassword()` before inserting into the database
- Modified `CustomerDAO.loginCustomer()`: changed SQL from `WHERE email=? AND password=?` to `WHERE email=?`, then verifies hash in Java with `PasswordUtil.verifyPassword()`
- Modified `AdminDAO.authenticateAdmin()`: same pattern — fetch by username, verify hash in Java
- Created `sql/stage11_migration.sql`: updates default admin password to BCrypt hash, documents customer migration approach

**2. SQL Injection (Confirmed Already Safe)**
- Verified all 4 DAO classes (`CustomerDAO`, `AccountDAO`, `AdminDAO`, `TransactionDAO`) — all 100% use `PreparedStatement` with `?` placeholders. No string concatenation vulnerabilities found.

**3. Sensitive Data Handling**
- Plain-text passwords never logged or included in error messages
- Authentication failures return a generic message ("Invalid username or password.") regardless of cause
- DB error details not forwarded to users during authentication

**4. Input Validation Improvements**
- `CustomerDAO.loginCustomer()`: Added null/empty guard — rejects without a DB call
- `AdminDAO.authenticateAdmin()`: Added null/empty guard — rejects without a DB call

### Files Created in Stage 11
| File | Purpose |
|------|---------|
| `src/util/PasswordUtil.java` | BCrypt hashing utility |
| `sql/stage11_migration.sql` | DB migration script for BCrypt hashes |
| `lib/jbcrypt-0.4.jar` | BCrypt library |

### Files Modified in Stage 11
| File | Change |
|------|--------|
| `src/dao/CustomerDAO.java` | Hash on register, BCrypt-verify on login |
| `src/dao/AdminDAO.java` | BCrypt-verify on authenticate |

---

## 📅 Stage 12 — JUnit Testing (August 18, 2026)

### Goal
Add meaningful JUnit 4 tests that verify actual business behavior, not just method execution.

### Test Infrastructure
- Added `lib/junit-4.13.2.jar` and `lib/hamcrest-core-1.3.jar`
- Created `test/TestDBHelper.java` — manages isolated test data lifecycle (create in @Before, clean in @After)
- Tests run against the real MySQL DB using isolated data with `@bmstest.internal` email domain
- Created `compile_and_run_tests.bat` — one-click compile + run script for Windows

### Test Classes Created

**`test/PasswordUtilTest.java`** — 8 tests
- Hash generation, BCrypt prefix validation, salt randomness, correct/wrong/null/empty verification, exception on null/empty input

**`test/CustomerDAOTest.java`** — 9 tests
- Registration success, duplicate email rejection, duplicate phone rejection, hash storage verification, login with correct credentials, wrong password, non-existent email, empty email, empty password

**`test/AccountDAOTest.java`** — 6 tests
- SAVINGS account creation, CURRENT account creation, retrieval after creation, multi-account retrieval, empty list for no accounts, empty list for non-existent customer

**`test/TransactionDAOTest.java`** — 23 tests
- Deposit success, balance increase, multiple deposits accumulate, frozen account deposit rejection
- Withdrawal success, balance decrease, InsufficientFundsException with correct amounts, exact-balance withdrawal, frozen account withdrawal rejection
- Transfer success, sender balance decrease, receiver balance increase, insufficient funds (with atomicity check), self-transfer rejection, non-existent destination rejection, frozen sender rejection, accountExists true/false

### Test Results
```
JUnit version 4.13.2
OK (46 tests)   Time: 30.639s
```

**All 46 tests passed on first run. ✅**

---

## 📊 Overall Progress Tracker (Updated)

| Stage | Description | Status | Date |
|-------|-------------|--------|------|
| 1 | Project setup, DB schema, DB connection | ✅ Complete | June 30, 2026 |
| 2 | Customer Registration (model, DAO, menu) | ✅ Complete | July 1, 2026 |
| 3 | Customer Login & Authentication | ✅ Complete | July 3, 2026 |
| 4 | Account Creation & Balance Checking | ✅ Complete | July 10, 2026 |
| 5 | Deposit Functionality | ✅ Complete | — |
| 6 | Withdrawal with Custom Exceptions | ✅ Complete | — |
| 7 | Fund Transfer with SQL Transactions | ✅ Complete | — |
| 8 | Transaction History & Mini Statement | ✅ Complete | — |
| 9 | Admin Module | ✅ Complete | — |
| 10 | Code Polish, Documentation, README | ✅ Complete | — |
| 11 | Security: BCrypt Hashing, SQL Injection Review | ✅ Complete | August 18, 2026 |
| 12 | JUnit Testing (46 tests, all passing) | ✅ Complete | August 18, 2026 |
