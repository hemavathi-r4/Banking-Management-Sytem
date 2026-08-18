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
9. **Administrator Dashboard**: Includes tools to search, list, or delete customer accounts (cascading all records atomically), toggle account status (ACTIVE/FROZEN/CLOSED), view global bank metrics, view audit logs, and inspect financial analytics reports.
10. **Exception Enforcement & Polish**: System-wide hardening which blocks deposits, withdrawals, and transfers on FROZEN/CLOSED accounts and sanitizes user input formats.

---

## 🛠️ Tech Stack & Dependencies

* **Language**: Java (JDK 8 or higher)
* **Database**: MySQL Server (8.0+)
* **Driver**: MySQL Connector/J driver (`mysql-connector-j-8.0.33.jar`)
* **Security**: jBCrypt (`jbcrypt-0.4.jar`)
* **Testing**: JUnit 4 (`junit-4.13.2.jar`, `hamcrest-core-1.3.jar`)
* **Interface**: Interactive Command-Line Console

---

## 📂 Project Structure

```
BMS/
├── bin/                          # Compiled Java class files
├── lib/                          # External libraries
│   ├── mysql-connector-j-8.0.33.jar # JDBC Driver Dependency
│   ├── jbcrypt-0.4.jar           # BCrypt password hashing library
│   ├── junit-4.13.2.jar          # JUnit 4 testing framework
│   └── hamcrest-core-1.3.jar     # Hamcrest matcher library
├── sql/                          # Database scripts
│   ├── bank_management.sql       # Database schema and setup script
│   ├── stage11_migration.sql     # Security & BCrypt migration script
│   ├── stage13_14_migration.sql  # Audit log & B-tree indexes migration script
│   └── stage15_17_migration.sql  # Database constraints & composite indexes
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
│   │   ├── Transaction.java      # Transaction model mapping
│   │   ├── AuditLog.java         # Audit log entity
│   │   ├── PageResult.java       # Generic container for pagination
│   │   ├── BankingSummary.java   # Banking summary report DTO
│   │   ├── TransactionSummaryRow.java # Transaction breakdown report DTO
│   │   ├── AccountStatistics.java# Account status/type breakdown DTO
│   │   └── TopAccountRow.java    # Top active account report DTO
│   ├── dao/
│   │   ├── CustomerDAO.java      # Customer DB queries
│   │   ├── AccountDAO.java       # Account DB queries
│   │   ├── AdminDAO.java         # Admin DB queries & single-query statistics
│   │   ├── AuditLogDAO.java      # Audit logging queries
│   │   ├── TransactionDAO.java   # Financial transaction operations (FOR UPDATE row locking)
│   │   └── ReportsDAO.java       # Analytical reporting queries (SQL Aggregations)
│   ├── service/
│   │   ├── AuditLogService.java  # Audit logging service layer
│   │   └── ReportsService.java   # Banking report service layer & input validation
│   ├── menu/
│   │   ├── RegistrationMenu.java # User registration UI
│   │   ├── LoginMenu.java        # User authentication UI
│   │   ├── CustomerMenu.java     # Customer actions dashboard
│   │   ├── AdminMenu.java        # Admin actions dashboard
│   │   └── ReportsMenu.java      # Admin analytics report UI
│   └── Main.java                 # Entry point of application
├── test/                         # JUnit 4 Test Suite
│   ├── TestDBHelper.java         # Test database helper & isolation runner
│   ├── PasswordUtilTest.java     # BCrypt hashing tests
│   ├── CustomerDAOTest.java     # Customer DAO unit/integration tests
│   ├── AccountDAOTest.java      # Account DAO unit/integration tests
│   ├── TransactionDAOTest.java  # Transaction DAO tests
│   ├── AuditLogServiceTest.java # Audit log service tests
│   ├── PaginationTest.java       # Pagination unit tests
│   ├── TransactionSearchTest.java# Paginated transaction query tests
│   ├── AdminSearchTest.java      # Paginated customer/account query tests
│   ├── TransactionRollbackTest.java # Atomicity & rollback safety tests
│   └── ReportsDAOTest.java       # Reporting SQL query integration tests
├── compile_and_run_tests.bat     # Test suite compile and execute runner
├── .gitignore                    # Git file exclusions
├── INTERVIEW_EXPLANATION.md       # 5-minute interview talking points
└── README.md                     # Project documentation (this file)
```

---

## ⚡ Stage 15 — Database Optimization

* **Database Constraints**: `sql/stage15_17_migration.sql` adds strict SQL-level validation rules (`CHECK (balance >= 0)`, `CHECK (amount > 0)`, `CHECK (account_type IN ('SAVINGS','CURRENT'))`, and `CHECK (status IN ('ACTIVE','FROZEN','CLOSED'))`).
* **Optimized Indexes**: Added column index `accounts(account_type)`, range index `transactions(amount)`, and composite index `transactions(transaction_type, transaction_time)` for optimized `GROUP BY` and date-range queries.
* **Single-Query Statistics**: Refactored `AdminDAO.getBankStatistics()` to consolidate 6 separate database round-trips into a single `UNION ALL` aggregate query.

---

## 🔒 Stage 16 — Advanced Transaction Management & Race Condition Protection

* **TOCTOU Race Condition Fixed**: Replaced pre-transaction balance checks with `SELECT balance, status FROM accounts WHERE account_no = ? FOR UPDATE` executed **inside** the JDBC transaction.
* **Deterministic Lock Ordering**: In `transferAmount()`, account locks are acquired in order of `Math.min(fromAccount, toAccount)` then `Math.max(fromAccount, toAccount)`. This guarantees deadlock prevention during concurrent bi-directional transfers.
* **Explicit Isolation Level**: Financial operations execute under `Connection.TRANSACTION_READ_COMMITTED` for reliable concurrency without unnecessary gap locking.
* **Automated Rollback Testing**: `TransactionRollbackTest.java` verifies that failed withdrawals, frozen account attempts, and failed transfers leave both account balances completely untouched.

---

## 📈 Stage 17 — Banking Reports & Analytics

Admin dashboard includes **Banking Reports & Analytics** (Option 9) to generate SQL-aggregated financial reports:

1. **Overall Banking Summary**: Comprehensive snapshot of total customers, accounts, total bank balance, average account balance, account status breakdown, and deposit/withdrawal/transfer volume totals.
2. **Transaction Type Summary**: Grouped breakdown of total count, total monetary sum, average, min, and max amounts for `DEPOSIT`, `WITHDRAWAL`, and `TRANSFER`.
3. **Daily Transaction Report**: Per-day breakdown of transaction counts and volume over customizable date ranges (`YYYY-MM-DD`).
4. **Monthly Transaction Report**: Monthly breakdown of banking activity filtered by year (`YYYY-MM`).
5. **Account Statistics**: Account distribution breakdown by `account_type` and `status` with balance ranges.
6. **Top Active Accounts**: Ranking of the top active bank accounts by transaction volume and activity count.

---

## 🧪 Comprehensive JUnit Test Suite (Stages 12–17)

Run `compile_and_run_tests.bat` to execute the full test suite:

| Test Class | Description | Tests | Status |
|------------|-------------|-------|--------|
| `PasswordUtilTest` | BCrypt password hashing & salt verification | 8 | PASS |
| `CustomerDAOTest` | Customer registration, duplicate check & login | 9 | PASS |
| `AccountDAOTest` | Account creation & customer account lookup | 6 | PASS |
| `TransactionDAOTest` | Deposit, withdrawal, transfer & exception checks | 23 | PASS |
| `AuditLogServiceTest` | Audit logging, success/failure tracking | 4 | PASS |
| `PaginationTest` | `PageResult` container & page calculation math | 5 | PASS |
| `TransactionSearchTest` | Paginated transaction filtering & date range checks | 3 | PASS |
| `AdminSearchTest` | Paginated customer/account search queries | 3 | PASS |
| `TransactionRollbackTest` | Atomicity, rollback safety & overdraft protection | 8 | PASS |
| `ReportsDAOTest` | Banking reports SQL queries & aggregation validation | 12 | PASS |

**Total: 81 tests — 100% Passing (0 failures, 0 errors) ✅**

---

## ⚙️ Setup & Execution

### 1. Execute Database Migration Scripts
Run the migration SQL scripts in sequence inside MySQL:
```sql
SOURCE sql/bank_management.sql;
SOURCE sql/stage11_migration.sql;
SOURCE sql/stage13_14_migration.sql;
SOURCE sql/stage15_17_migration.sql;
```

### 2. Run Application
```powershell
javac -cp "lib/mysql-connector-j-8.0.33.jar;lib/jbcrypt-0.4.jar" -d bin src/database/*.java src/model/*.java src/exception/*.java src/util/*.java src/dao/*.java src/service/*.java src/menu/*.java src/Main.java
java -cp "bin;lib/mysql-connector-j-8.0.33.jar;lib/jbcrypt-0.4.jar" Main
```

### 3. Run Test Suite
```powershell
compile_and_run_tests.bat
```
