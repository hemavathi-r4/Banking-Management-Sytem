# 📌 NEXT TASK — Stage 10: System Refactoring, Polish, and Detailed README Documentation

**Planned Date:** July 16, 2026 (Wednesday)  
**Prerequisite:** Stage 9 must be tested and working (Administrator Operations Module)

---

## 🎯 Goal

Perform a final polish pass over the entire Banking Management System. Refactor code for consistency, add input validation guards across all modules (e.g., block transactions on FROZEN accounts), and create a comprehensive `README.md` file suitable for GitHub.

---

## 📝 Tasks

### Task 1 — Account Status Enforcement

Currently, frozen accounts can still perform deposits, withdrawals, and transfers. Add status checks:
- In `TransactionDAO.depositAmount()`: Before processing, query the account status. If `FROZEN` or `CLOSED`, reject the operation with a clear message.
- In `TransactionDAO.withdrawAmount()`: Same check before withdrawal.
- In `TransactionDAO.transferAmount()`: Check both source and destination account statuses.
- Display user-friendly error messages in `CustomerMenu.java` when operations are blocked.

### Task 2 — Input Validation Hardening

Review and strengthen input validation across all menus:
- Ensure email format validation during registration (basic `@` and `.` check).
- Ensure phone number validation (digits only, length check).
- Trim and sanitize all user inputs consistently.
- Handle edge cases like empty strings, extremely long inputs, and special characters.

### Task 3 — Code Consistency & Cleanup

- Ensure all DAO methods follow the same resource management pattern (try-with-resources or manual finally blocks).
- Verify all `finally` blocks restore `autoCommit` and close connections.
- Add missing Javadoc comments to any undocumented methods.
- Remove any dead code or unused imports.

### Task 4 — Create `README.md`

Write a professional, GitHub-ready `README.md` at the project root containing:
- Project title and description
- Features list (all 9 stages summarized)
- Technology stack
- Prerequisites (JDK, MySQL, JDBC driver)
- Setup instructions (database, configuration, compilation, running)
- Project structure diagram
- Screenshots / sample console output
- OOP concepts demonstrated
- Future enhancements section
- License and author information

### Task 5 — Final Compilation & Smoke Test

- Compile all files with `javac`.
- Run through the complete application flow (register → login → all 5 customer options → admin login → all 7 admin options).
- Verify no runtime errors or unexpected behavior.

---

## 📂 Files to Create / Modify

| File | Action | Purpose |
|------|--------|---------|
| `src/dao/TransactionDAO.java` | MODIFY | Add account status checks before transactions |
| `src/menu/CustomerMenu.java` | MODIFY | Handle frozen account error messages |
| `src/menu/RegistrationMenu.java` | MODIFY | Strengthen email/phone validation |
| `README.md` | NEW | Comprehensive GitHub documentation |

---

## ✅ Definition of Done

- [ ] Frozen accounts cannot perform deposit, withdrawal, or transfer operations.
- [ ] Email and phone validation added during registration.
- [ ] All DAO methods have consistent resource management patterns.
- [ ] All public methods have Javadoc comments.
- [ ] `README.md` is complete and professional.
- [ ] Full application compiles and runs end-to-end without errors.
