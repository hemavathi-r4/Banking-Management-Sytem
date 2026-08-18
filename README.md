# 🏦 Banking Management System (BMS)

A robust, console-based Banking Management System built in Core Java using JDBC and MySQL. This system follows enterprise-grade software design principles, showcasing Object-Oriented Programming (OOP) concepts, the Data Access Object (DAO) pattern, custom exception handling, and atomic SQL transactions.

---

## 🌟 Key Features

The project is structured into modular layers spanning across functional requirements:

1. **Database & Connection Foundation**: Centralized JDBC configuration with connection pooling diagnostics and safety features.
2. **Customer Registration**: Secure customer onboarding with check constraints to reject duplicate email/phone numbers.
3. **Authentication & Session Management**: Post-login Customer Dashboard powered by credential-matching checks.
4. **Account Management**: Supports opening multiple accounts (e.g., SAVINGS or CURRENT) per customer with minimum balance enforcement (Rs. 1,000).
5. **Deposits & Balance Management**: Processes deposits (Min Rs. 500) and executes real-time database updates.
6. **Withdrawals with Custom Exceptions**: Safe withdrawals with instant validation of funds. Rejects operations on insufficient balance using custom exceptions.
7. **Atomic Fund Transfers**: Facilitates transfers between customers. Runs balance debit, credit, and transaction logs inside an atomic SQL transaction block (commit/rollback).
8. **Mini Statement generation**: Generates real-time reports of the last 5 transactions (deposits, withdrawals, and transfers).
9. **Administrator Dashboard**: Includes tools to search, list, or delete customer accounts (cascading all records atomically), toggle account status (ACTIVE/FROZEN/CLOSED), view global bank metrics, and audit the entire ledger.
10. **Exception Enforcement & Polish**: System-wide hardening which blocks deposits, withdrawals, and transfers on FROZEN/CLOSED accounts and sanitizes user input formats.

---

## 🛠️ Tech Stack & Dependencies

* **Language**: Java (JDK 8 or higher)
* **Database**: MySQL Server (8.0+)
* **Driver**: MySQL Connector/J driver (`mysql-connector-j-8.0.33.jar`)
* **Interface**: Interactive Command-Line Console

---

## 📂 Project Structure

```
BMS/
├── bin/                          # Compiled Java class files
├── lib/                          # External libraries
│   └── mysql-connector-j-8.0.33.jar # JDBC Driver Dependency
├── sql/                          # Database scripts
│   └── bank_management.sql       # Database schema and setup script
├── src/                          # Source code
│   ├── database/
│   │   └── DBConnection.java     # JDBC Connection manager
│   ├── exception/
│   │   ├── AccountFrozenException.java      # Checked exception for restricted status
│   │   ├── InsufficientFundsException.java  # Checked exception for overdraft check
│   │   └── InvalidAccountException.java     # Checked exception for missing accounts
│   ├── model/
│   │   ├── User.java             # Base user model
│   │   ├── Customer.java         # Customer entity (extends User)
│   │   ├── Admin.java            # Admin entity (extends User)
│   │   ├── Account.java          # Account model mapping
│   │   └── Transaction.java      # Transaction model mapping
│   ├── dao/
│   │   ├── CustomerDAO.java      # Customer DB queries
│   │   ├── AccountDAO.java       # Account DB queries
│   │   ├── AdminDAO.java         # Admin DB queries
│   │   └── TransactionDAO.java   # Financial transaction operations (Atomic)
│   ├── menu/
│   │   ├── RegistrationMenu.java # User registration UI
│   │   ├── LoginMenu.java        # User authentication UI
│   │   ├── CustomerMenu.java     # Customer actions dashboard
│   │   └── AdminMenu.java        # Admin actions dashboard
│   └── Main.java                 # Entry point of application
├── .gitignore                    # Git file exclusions
├── INTERVIEW_EXPLANATION.md       # 5-minute interview talking points
└── README.md                     # Project documentation (this file)
```

---

## ⚙️ Setup & Installation

### 1. Database Configuration
1. Open your MySQL client (CLI or Workbench).
2. Execute the setup SQL script located in the `sql` directory:
   ```sql
   SOURCE sql/bank_management.sql;
   ```
   *This script automatically creates the database `bank_management` along with the tables `customers`, `accounts`, `transactions`, and `admins`.*

3. Open [DBConnection.java](file:///c:/Users/Hemavathi/Desktop/BMS/src/database/DBConnection.java) and verify/update the database configuration constants:
   ```java
   private static final String URL      = "jdbc:mysql://localhost:3306/bank_management";
   private static final String USERNAME = "root";
   private static final String PASSWORD = "YOUR_PASSWORD"; // Update this with your MySQL password
   ```

### 2. Compilation
Compile all Java source files into the `bin/` directory by referencing the JDBC library in the classpath:

**On Windows (PowerShell / Command Prompt)**:
```powershell
javac -cp "lib/mysql-connector-j-8.0.33.jar" -d bin src/database/*.java src/model/*.java src/exception/*.java src/dao/*.java src/menu/*.java src/Main.java
```

### 3. Running the Project
Execute the compiled `Main` class with the JDBC driver in the classpath:

**On Windows (PowerShell / Command Prompt)**:
```powershell
java -cp "bin;lib/mysql-connector-j-8.0.33.jar" Main
```

---

## 🧩 OOP and Software Engineering Best Practices

* **Encapsulation**: Used throughout entities (like `Customer`, `Account`, `Transaction`) by declaring fields `private` and exposing public getter/setter methods.
* **Inheritance**: Implemented a core `User` model, from which `Customer` and `Admin` extend to share identity properties.
* **DAO Design Pattern**: Decoupled domain business entities from SQL access layers (via `CustomerDAO`, `AccountDAO`, `AdminDAO`, `TransactionDAO`), keeping database work modular.
* **Custom Checked Exceptions**: Defined `InsufficientFundsException`, `InvalidAccountException`, and `AccountFrozenException` to handle runtime operational blocks cleanly.
* **Atomic Transactions**: Ensured money transfers and onboarding cleanups run within JDBC transaction boundaries (`setAutoCommit(false)`, `commit()`, and `rollback()`) to guarantee data integrity.
* **Input Sanitization**: Validates email format patterns and digits/length checks for phone numbers prior to database ingestion.

---

## 🔒 Stage 11 — Security Upgrade

### Password Hashing (BCrypt)

Passwords are **never stored as plain text**. Stage 11 introduced BCrypt-based password hashing:

* **Library used**: `jbcrypt-0.4.jar` (Damien Miller's jBCrypt, placed in `lib/`)
* **New utility**: `src/util/PasswordUtil.java` — wraps `BCrypt.hashpw()` and `BCrypt.checkpw()`
* **Cost factor**: 12 (2¹² = 4096 hashing rounds — slow for attackers, imperceptible for users)
* **Salt**: Generated automatically per password — same password produces a different hash each time

**Registration flow (with hashing):**
```
User enters password → Validate → PasswordUtil.hashPassword() → Store hash in DB
```

**Login flow (with BCrypt verify):**
```
User enters password → Fetch stored hash by email → PasswordUtil.verifyPassword() → Authenticate
```

### SQL Injection Prevention

All DAO classes already used `PreparedStatement` with `?` placeholders from Stages 1–10.
No dynamic string concatenation exists in any database query. This was verified and confirmed during Stage 11.

### Sensitive Data Handling

* Plain-text passwords are **never logged** or exposed in error messages
* Authentication errors always return a **generic message** (`"Invalid username or password."`) — whether the email doesn't exist or the password is wrong — to prevent user enumeration
* Database error details are not forwarded to the user in authentication paths
* Hardcoded DB credentials are confined to `DBConnection.java` (documented limitation)

### Input Validation in Authentication

* Empty email/password rejected immediately — no DB call made
* Null inputs handled gracefully without `NullPointerException`
* Admin login rejects empty username/password before querying

### Database Changes

Run `sql/stage11_migration.sql` after deploying Stage 11 to:
1. Update the default admin password to a BCrypt hash of `admin123`
2. Confirm `VARCHAR(255)` column widths (already sufficient for 60-char BCrypt hashes)

> **Note**: Existing plain-text customer passwords cannot be automatically migrated (BCrypt is one-way). See the migration script for options.

---

## 🧪 Stage 12 — JUnit Testing

JUnit 4 test coverage was added for all key business logic in the service/DAO layer.

### Test Setup

**Dependencies added** (in `lib/`):
* `junit-4.13.2.jar` — JUnit 4 testing framework
* `hamcrest-core-1.3.jar` — Required by JUnit 4

**Test approach**: Tests run against the **real MySQL database** using isolated test data that is created before each test and cleaned up after. No mock framework is needed.

### Test Classes

| File | Tests |
|------|-------|
| `test/PasswordUtilTest.java` | 8 tests — BCrypt hash generation, salt randomness, correct/wrong/null/empty verification |
| `test/CustomerDAOTest.java` | 9 tests — Registration, duplicate detection, hash storage, login success/failure/empty |
| `test/AccountDAOTest.java` | 6 tests — Account creation (SAVINGS/CURRENT), retrieval, empty-list behavior |
| `test/TransactionDAOTest.java` | 23 tests — Deposit/withdrawal/transfer success, balance verification, exception assertions |

**Total: 46 tests — all passing ✅**

### Running the Tests

```powershell
# Compile + run all tests
compile_and_run_tests.bat

# Or manually:
javac -cp "lib\mysql-connector-j-8.0.33.jar;lib\jbcrypt-0.4.jar;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar" -d bin src\database\*.java src\model\*.java src\exception\*.java src\util\*.java src\dao\*.java src\menu\*.java src\Main.java
javac -cp "bin;lib\mysql-connector-j-8.0.33.jar;lib\jbcrypt-0.4.jar;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar" -d bin test\TestDBHelper.java test\PasswordUtilTest.java test\CustomerDAOTest.java test\AccountDAOTest.java test\TransactionDAOTest.java
java -cp "bin;lib\mysql-connector-j-8.0.33.jar;lib\jbcrypt-0.4.jar;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar" org.junit.runner.JUnitCore PasswordUtilTest CustomerDAOTest AccountDAOTest TransactionDAOTest
```

### Important Test Scenarios

| Scenario | Method |
|----------|--------|
| Balance increases correctly after deposit | `shouldIncreaseBalanceAfterDeposit()` |
| Balance decreases correctly after withdrawal | `shouldDecreaseBalanceAfterWithdrawal()` |
| InsufficientFundsException thrown on overdraft | `shouldRejectWithdrawalWhenBalanceIsInsufficient()` |
| Sender/receiver balances both correct after transfer | `shouldDecreaseSenderBalanceAfterTransfer()` + `shouldIncreaseReceiverBalanceAfterTransfer()` |
| Self-transfer rejected | `shouldRejectTransferToSameAccount()` |
| Frozen account blocks all operations | `shouldThrowAccountFrozenExceptionOnDeposit/Withdrawal/Transfer` |
| Wrong password rejected | `shouldRejectLoginWithWrongPassword()` |
| BCrypt hash stored on registration | `shouldHashPasswordOnRegistration()` |
