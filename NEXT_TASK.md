# 📌 NEXT TASK — Stage 9: Administrator Operations Module

**Planned Date:** July 16, 2026 (Thursday)  
**Prerequisite:** Stage 8 must be tested and working (Mini Statement Functionality)

---

## 🎯 Goal

Implement the Administrator Module to allow administrative users to manage customers, accounts, and view global bank statistics. 
When an administrator logs in with username `admin` and password `admin123` via option "3. Admin Login" in the Main Menu, they should land on the Admin Dashboard console menu:
1. View Customers
2. Search Customer
3. Delete Customer
4. Freeze Account
5. Activate Account
6. View Transactions
7. Statistics
8. Logout

---

## 📝 Tasks

### Task 1 — Create `AdminDAO.java` in `src/dao/`

Implement data access operations for administrative features:
- `public boolean authenticateAdmin(String username, String password)`: Queries `admins` table to verify credentials.
- `public List<Customer> getAllCustomers()`: SELECTs and returns all rows from the `customers` table.
- `public Customer searchCustomer(String searchTerm)`: Searches for a customer by email, phone, or ID.
- `public boolean deleteCustomer(int customerId)`: DELETEs customer by ID (ON DELETE CASCADE will handle accounts).
- `public boolean updateAccountStatus(long accountNo, String status)`: UPDATEs the `status` column in `accounts` (e.g., to `'FROZEN'` or `'ACTIVE'`).
- `public List<Transaction> getAllTransactions()`: SELECTs all transactions in the bank.
- `public Map<String, Object> getBankStatistics()`: Retrieves global statistics (e.g., total customers, total bank deposits/balance, total transactions count, count of SAVINGS/CURRENT accounts).

### Task 2 — Create `AdminMenu.java` in `src/menu/`

Create the presentation layer for admin operations:
- Displays login prompt collecting admin username and password.
- Coordinates the Admin Dashboard choice loop.
- Displays neatly formatted outputs (e.g., a table for customers, a summary list for statistics, a table of all system transactions).
- Prompts for account freeze/activation and calls the appropriate DAO method.

### Task 3 — Update `Main.java`

- Import `menu.AdminMenu` and wire switch option `3` ("Admin Login") to call the admin login flow.

---

## 📂 Files to Create / Modify

| File | Action | Purpose |
|------|--------|---------|
| `src/dao/AdminDAO.java` | NEW | Administrative database operations |
| `src/menu/AdminMenu.java` | NEW | Admin dashboard presentation and prompt loop |
| `src/Main.java` | MODIFY | Wire Option 3 to `AdminMenu` login |

---

## ✅ Definition of Done

- [ ] Admin login successfully checks the `admins` database table.
- [ ] Admin can view a clean table of all customers.
- [ ] Search works dynamically by ID, email, or phone.
- [ ] Freeze and activate updates `accounts.status` properly (e.g. status value changes in the DB).
- [ ] Statistics display aggregates (total balance, total accounts, total customers).
- [ ] Delete customer deletes both customer and their accounts cascade.
- [ ] Admin logout returns cleanly to the Main Menu.
