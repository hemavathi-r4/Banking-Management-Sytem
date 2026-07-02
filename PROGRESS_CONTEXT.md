# 📈 PROGRESS CONTEXT — Banking Management System

**Date:** July 1, 2026 (Tuesday)  
**Stage Completed:** Stage 2 — Customer Registration  
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

---

### 📁 File 1: `src/model/Customer.java` — NEW

**What it is:**  
A model (POJO) class that represents a single bank customer. Each instance of this class holds one customer's data.

**Why it exists:**  
Every row in the `customers` database table maps to one `Customer` object in Java. Instead of passing around 6 separate strings (name, email, phone, etc.), we bundle them into one clean object. This is how professional Java applications handle data.

**What's inside:**

| Element | Details |
|---------|---------|
| **Fields (6)** | `customerId`, `name`, `email`, `phone`, `password`, `address` — all `private` |
| **Constructor 1** | No-argument — creates an empty object, set values later with setters |
| **Constructor 2** | `(name, email, phone, password, address)` — used during registration (no ID yet, because MySQL auto-generates it) |
| **Constructor 3** | `(customerId, name, email, phone, password, address)` — used when reading data from the database (ID is known) |
| **Getters & Setters** | One getter and one setter for each of the 6 fields |
| **`toString()`** | Returns a formatted string for easy printing/debugging. Intentionally excludes the password for security |

**OOP Concept Demonstrated:**  
**Encapsulation** — All fields are `private`. No other class can directly access or modify them. Access is controlled through public getter/setter methods. This protects data integrity and allows us to add validation logic in setters later if needed.

**Database Mapping:**
```
Customer.java Field  →  customers Table Column
─────────────────────────────────────────────
customerId           →  customer_id (INT, PK, AUTO_INCREMENT)
name                 →  name (VARCHAR 100, NOT NULL)
email                →  email (VARCHAR 100, NOT NULL, UNIQUE)
phone                →  phone (VARCHAR 15, NOT NULL, UNIQUE)
password             →  password (VARCHAR 255, NOT NULL)
address              →  address (VARCHAR 255, nullable)
```

---

### 📁 File 2: `src/dao/CustomerDAO.java` — NEW

**What it is:**  
A Data Access Object (DAO) class that handles all database operations related to customers.

**Why it exists:**  
The DAO pattern separates database logic from the rest of the application. The menu class doesn't need to know SQL — it just calls `customerDAO.registerCustomer(customer)`. If we ever switch from MySQL to PostgreSQL, we only change the DAO files — nothing else.

**What's inside:**

| Method | What It Does | SQL Used |
|--------|--------------|----------|
| `registerCustomer(Customer)` | Validates uniqueness, then INSERTs customer into DB | `INSERT INTO customers (name, email, phone, password, address) VALUES (?, ?, ?, ?, ?)` |
| `emailExists(String email)` | Checks if an email is already registered | `SELECT COUNT(*) FROM customers WHERE email = ?` |
| `phoneExists(String phone)` | Checks if a phone number is already registered | `SELECT COUNT(*) FROM customers WHERE phone = ?` |

**How `registerCustomer()` works step-by-step:**
1. Calls `emailExists()` — if the email is taken, prints an error and returns `false`.
2. Calls `phoneExists()` — if the phone is taken, prints an error and returns `false`.
3. If both are unique, opens a database connection using `DBConnection.getConnection()`.
4. Creates a `PreparedStatement` with the INSERT query.
5. Sets the 5 placeholder values (`?`) using `setString()`.
6. Calls `executeUpdate()` which runs the INSERT and returns the number of rows affected.
7. If 1 row was inserted → prints success message and returns `true`.
8. If a `SQLException` occurs → catches it, prints a friendly error, and returns `false` (app does NOT crash).

**Key Technical Decisions:**

| Decision | Why |
|----------|-----|
| **`PreparedStatement` over `Statement`** | Prevents SQL injection attacks. `PreparedStatement` escapes special characters in user input automatically. Also more performant because the DB can cache the query plan. |
| **`try-with-resources`** | Java auto-closes the `Connection` and `PreparedStatement` when the `try` block ends, even if an exception occurs. This prevents resource leaks (unclosed database connections). |
| **`SQLException` catch blocks** | Every database method catches `SQLException` and prints a user-friendly error. The application never terminates unexpectedly due to a database error. |
| **Duplicate checks before INSERT** | We check email and phone separately so we can give the user a specific error message ("email already exists" vs "phone already exists") instead of a generic database error. |

---

### 📁 File 3: `src/menu/RegistrationMenu.java` — NEW

**What it is:**  
A console menu class that handles the user interface for customer registration.

**Why it exists:**  
Keeps UI/input-output logic separate from business logic and database logic. This follows the **Separation of Concerns** principle:
- `Main.java` → controls the main menu loop
- `RegistrationMenu.java` → handles registration input/output
- `CustomerDAO.java` → handles database operations
- `Customer.java` → holds customer data

**What's inside:**

| Element | Details |
|---------|---------|
| **Field** | `CustomerDAO customerDAO` — instance created in the constructor |
| **Constructor** | Initializes `customerDAO = new CustomerDAO()` |
| **`showRegistrationForm(Scanner)`** | Displays the registration form, collects input, validates, creates a `Customer` object, calls DAO |

**How `showRegistrationForm()` works:**
1. Prints a "CUSTOMER REGISTRATION" header.
2. Prompts the user for: Name, Email, Phone, Password, Address.
3. Reads each input using `scanner.nextLine().trim()`.
4. **Validates:** If name, email, phone, or password is empty → prints error and returns (address is optional).
5. Creates a `Customer` object using the registration constructor (no ID).
6. Calls `customerDAO.registerCustomer(newCustomer)` which handles the database work.

**Why Scanner is passed as a parameter (not created inside):**  
Creating multiple `Scanner` objects on `System.in` causes bugs in Java. The single `Scanner` created in `Main.java` is shared across all menu classes by passing it as a method parameter.

---

### 📁 File 4: `src/Main.java` — MODIFIED (minimal changes)

**What changed (only 3 lines):**

```diff
 import database.DBConnection;
+import menu.RegistrationMenu;

 ...

 Scanner scanner = new Scanner(System.in);
+RegistrationMenu registrationMenu = new RegistrationMenu();
 boolean running = true;

 ...

 case "1":
-    System.out.println("\n>> Registration will be implemented in Stage 3.\n");
+    registrationMenu.showRegistrationForm(scanner);
     break;
```

**Why so few changes:**  
Because we followed modular design. All the real logic lives in `RegistrationMenu` and `CustomerDAO`. `Main.java` only needs to know that a `RegistrationMenu` exists and how to call it. This is the benefit of separation of concerns.

---

## 🏗️ Architecture Flow — How Registration Works End-to-End

```
User runs the application
        │
        ▼
Main.java tests DB connection → [OK] Database connection successful!
        │
        ▼
Main menu displays → User selects "1. Register"
        │
        ▼
Main.java calls → registrationMenu.showRegistrationForm(scanner)
        │
        ▼
RegistrationMenu prompts for: Name, Email, Phone, Password, Address
        │
        ▼
Input validation → Are required fields filled? (If no → error & return)
        │
        ▼
Creates → new Customer(name, email, phone, password, address)
        │
        ▼
Calls → customerDAO.registerCustomer(customer)
        │
        ├──→ emailExists(email)?  → Yes → "[ERROR] Email already registered" → return false
        │
        ├──→ phoneExists(phone)?  → Yes → "[ERROR] Phone already registered" → return false
        │
        └──→ Both unique? → INSERT INTO customers → "✓ Registration Successful!" → return true
                │
                ▼
        Back to Main Menu (loop continues)
```

---

## 📂 Current Folder Structure After Stage 2

```
BMS/
├── src/
│   ├── model/
│   │   └── Customer.java          ← NEW (Stage 2)
│   ├── dao/
│   │   └── CustomerDAO.java       ← NEW (Stage 2)
│   ├── database/
│   │   └── DBConnection.java      ← Stage 1 (unchanged)
│   ├── exception/                 ← (empty — future stages)
│   ├── menu/
│   │   └── RegistrationMenu.java  ← NEW (Stage 2)
│   └── Main.java                  ← MODIFIED (3 lines added)
├── sql/
│   └── bank_management.sql        ← Stage 1 (unchanged)
├── lib/                           ← (mysql-connector-j.jar)
├── PROJECT_CONTEXT.md
├── PROGRESS_CONTEXT.md            ← This file
└── NEXT_TASK.md
```

---

## 🧠 OOP & Design Concepts Used in Stage 2

| Concept | Where It's Applied |
|---------|-------------------|
| **Encapsulation** | `Customer.java` — all fields are `private`, accessed via public getters/setters |
| **DAO Pattern** | `CustomerDAO.java` — all database logic isolated in one class |
| **Separation of Concerns** | Each class has one job: Model holds data, DAO talks to DB, Menu handles UI, Main controls flow |
| **Single Responsibility Principle** | No class does more than its one defined purpose |
| **Defensive Programming** | Input validation in RegistrationMenu, duplicate checks in DAO, SQLException handling everywhere |

---

## 📊 Overall Progress Tracker

| Stage | Description | Status | Date |
|-------|-------------|--------|------|
| 1 | Project setup, DB schema, DB connection | ✅ Complete | June 30, 2026 |
| 2 | Customer Registration (model, DAO, menu) | ✅ Complete | July 1, 2026 |
| 3 | Customer Login & Authentication | ⬜ Not started | — |
| 4 | Account Creation & Balance Checking | ⬜ Not started | — |
| 5 | Deposit Functionality | ⬜ Not started | — |
| 6 | Withdrawal with Custom Exceptions | ⬜ Not started | — |
| 7 | Fund Transfer with SQL Transactions | ⬜ Not started | — |
| 8 | Transaction History & Mini Statement | ⬜ Not started | — |
| 9 | Admin Module | ⬜ Not started | — |
| 10 | Code Polish, Documentation, README | ⬜ Not started | — |

---

## 💡 Suggested Git Commit Message for Stage 2

```
feat: implement customer registration (Stage 2)

- Add Customer model class (model/Customer.java)
- Add CustomerDAO with registration and duplicate checks (dao/CustomerDAO.java)
- Add RegistrationMenu for console registration UI (menu/RegistrationMenu.java)
- Wire registration to main menu option 1 (Main.java)
```
