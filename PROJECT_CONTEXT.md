# 📋 PROJECT CONTEXT — Banking Management System

**Last Updated:** 2026-07-01 (Tuesday)  
**Current Stage:** Stage 2 ✅ Complete  
**Tech Stack:** Core Java, JDBC, MySQL, Console Application

---

## 🏗️ Project Overview

A console-based Banking Management System built with Core Java and MySQL, designed to demonstrate OOP, JDBC, exception handling, and clean project architecture. Suitable for a college major project and software engineering portfolio.

---

## 📅 Work Done Today (July 1, 2026)

### Stage 2 — Customer Registration

| Task | Status |
|------|--------|
| Created `src/model/Customer.java` — model class with fields, constructors, getters/setters, toString() | ✅ Done |
| Created `src/dao/CustomerDAO.java` — DAO with registerCustomer(), emailExists(), phoneExists() | ✅ Done |
| Created `src/menu/RegistrationMenu.java` — console UI for registration input | ✅ Done |
| Updated `src/Main.java` — wired menu option 1 to RegistrationMenu | ✅ Done |

### Files Created / Modified Today

| File | Purpose |
|------|---------|
| `src/model/Customer.java` | Model class mapping to the `customers` table — encapsulation with private fields, 3 constructors, getters/setters, toString() |
| `src/dao/CustomerDAO.java` | Data Access Object — handles registration INSERT, duplicate email/phone checks using PreparedStatement |
| `src/menu/RegistrationMenu.java` | Console menu — collects user input (name, email, phone, password, address), validates, delegates to DAO |
| `src/Main.java` *(modified)* | Added RegistrationMenu import and wired option 1 to `showRegistrationForm()` |

---

## 📅 Previous Work (June 30, 2026)

### Stage 1 — Project Setup & Database Foundation

| Task | Status |
|------|--------|
| Created project folder structure (`src/model`, `dao`, `database`, `exception`, `menu`, `util`, `lib/`) | ✅ Done |
| Wrote SQL script (`sql/bank_management.sql`) | ✅ Done |
| Created `src/database/DBConnection.java` — centralized JDBC connection utility | ✅ Done |
| Created `src/Main.java` — entry point with DB test and main menu loop | ✅ Done |

### Database Schema

| Table | Key Details |
|-------|-------------|
| `customers` | customer_id (PK), email (UNIQUE), phone (UNIQUE), password, address, created_at |
| `accounts` | account_no (PK, starts at 1001), customer_id (FK → customers), account_type, balance, status |
| `transactions` | transaction_id (PK), from_account (FK), to_account (FK), transaction_type, amount, transaction_time, remarks |
| `admins` | admin_id (PK), username (UNIQUE), password — default: admin / admin123 |

### Key Design Decisions

- **`DBConnection` as a static utility:** All DB access goes through one class — easy to update credentials in one place (Single Responsibility Principle).
- **try-with-resources** used for auto-closing connections to prevent resource leaks.
- **Account numbers start at 1001** for realism.
- **SQL foreign keys with `ON DELETE CASCADE`** on accounts → customers, so deleting a customer cleans up their accounts.
- **DAO Pattern:** Database logic is separated from UI logic (CustomerDAO vs RegistrationMenu).
- **Shared Scanner:** A single Scanner instance is created in Main.java and passed to menu classes to avoid System.in conflicts.
- **PreparedStatement over Statement:** Used for SQL injection prevention and cleaner code.

---

## 📂 Current Folder Structure

```
BMS/
├── src/
│   ├── model/
│   │   └── Customer.java       ← Stage 2
│   ├── dao/
│   │   └── CustomerDAO.java    ← Stage 2
│   ├── database/
│   │   └── DBConnection.java
│   ├── exception/              ← (empty — future stages)
│   ├── menu/
│   │   └── RegistrationMenu.java  ← Stage 2
│   └── Main.java
├── sql/
│   └── bank_management.sql
├── lib/                        ← (mysql-connector-j.jar)
├── PROJECT_CONTEXT.md
└── NEXT_TASK.md
```

---

## ⚙️ Setup Status

- [x] Run `sql/bank_management.sql` in MySQL
- [x] Update password in `DBConnection.java` to match your MySQL root password
- [x] Download `mysql-connector-j.jar` and place in `lib/`
- [x] Open project in IntelliJ, mark `src` as Sources Root, add JAR to libraries
- [x] Verify `Main.java` runs and shows `[OK] Database connection successful!`

---

## 📊 Overall Progress

| Stage | Description | Status |
|-------|-------------|--------|
| 1 | Project setup, DB schema, DB connection | ✅ Complete |
| 2 | Customer Registration (model, DAO, menu) | ✅ Complete |
| 3 | Customer Login & Authentication | ⬜ Not started |
| 4 | Account Creation & Balance Checking | ⬜ Not started |
| 5 | Deposit Functionality | ⬜ Not started |
| 6 | Withdrawal with Custom Exceptions | ⬜ Not started |
| 7 | Fund Transfer with SQL Transactions | ⬜ Not started |
| 8 | Transaction History & Mini Statement | ⬜ Not started |
| 9 | Admin Module | ⬜ Not started |
| 10 | Code Polish, Documentation, README | ⬜ Not started |
