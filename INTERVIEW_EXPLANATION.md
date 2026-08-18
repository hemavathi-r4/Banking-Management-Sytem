# 🎙️ 5-Minute Project Interview Explanation
## Banking Management System (BMS)

Here is a structured, comprehensive walkthrough designed for technical interviews to explain the architecture, design choices, challenges, and implementation details of this project in approximately 5 minutes.

---

### 1. Introduction (0 to 1 Minute)
> *"I designed and built a console-based Banking Management System in Java using JDBC and MySQL. The goal of this project was to implement a robust, enterprise-grade backend system from scratch. I wanted to demonstrate proper object-oriented principles, decouple the database operations from the application logic, and ensure strict data integrity for financial transactions. The system allows customers to register, open savings or current accounts, make deposits, withdraw funds under custom business rules, and transfer money between accounts. It also features a fully functional administrator module to manage users, check global statistics, and audit transactions."*

---

### 2. Architecture & Design Patterns (1 to 2 Minutes)
> *"To keep the codebase maintainable and clean, I avoided writing monolithic scripts and instead structured the project using a layered architecture with the **Data Access Object (DAO)** pattern:
>
> 1. **Model Layer (POJOs/Entities)**: Represents our database tables as objects like `Customer`, `Account`, and `Transaction`. I implemented inheritance here—having both `Customer` and `Admin` extend a base `User` class to share credentials and identifier properties.
> 2. **DAO Layer (Data Access Objects)**: Houses all raw SQL queries and JDBC execution blocks. The menu classes never talk to the database directly; they call the DAOs (`CustomerDAO`, `AccountDAO`, `AdminDAO`, `TransactionDAO`), which return model objects or lists.
> 3. **UI/Menu Layer**: Manages user interaction through terminal menus. It reads console inputs, performs client-side validation, and catches business exceptions thrown by the DAOs to display friendly error messages.
> 4. **Utility Layer**: Features a centralized `DBConnection` helper class that provides database connection objects. This ensures that database details are configurable in a single location."*

---

### 3. Key Technical Implementations (2 to 3.5 Minutes)
> *"Two key technical aspects of this project are **custom checked exceptions** and **atomic transaction management**:
>
> * **Checked Exceptions**: I created three custom exceptions: `InsufficientFundsException`, `InvalidAccountException`, and `AccountFrozenException`. For instance, when a customer attempts to withdraw money, the DAO first runs validation. If the requested amount exceeds the balance, it throws an `InsufficientFundsException`. This forces the compiler to require the UI layer to catch it, passing along context details like amount requested vs available balance so the user gets a helpful error.
>
> * **Atomic Database Transactions (JDBC Atomicity)**: In bank transfers or customer deletions, multiple SQL queries must run as one single unit of work. For instance, in `transferAmount()`, a debit query must run on the source account, a credit query on the target account, and an insert query in the transactions log. 
> To prevent partial updates—where a debit succeeds but a credit fails—I turned off auto-commit with `conn.setAutoCommit(false)`. If all statements execute successfully, `conn.commit()` is called. If any statement throws an `SQLException`, the code enters a catch block that executes `conn.rollback()`, ensuring the bank's ledger remains consistent. Finally, inside a `finally` block, I restore auto-commit and return the connection to prevent resource leaks."*

---

### 4. Technical Challenges & Resolutions (3.5 to 4.5 Minutes)
> *"During development, I faced a few interesting technical challenges:
>
> 1. **Connection and Resource Leaks**: In the initial stages, leaving database statements open caused connection pools to exhaust. I resolved this by applying Java's **try-with-resources** statement for standard queries, which guarantees that JDBC `Connection`, `PreparedStatement`, and `ResultSet` objects are automatically closed when exiting the block, even if exceptions are thrown.
> 2. **Cascading Deletions during Admin Cleanup**: When an administrator deletes a customer, SQL foreign key constraints would normally fail if the customer has accounts or transaction histories. I solved this by wrapping the deletion sequence in a single database transaction. The DAO queries for all account numbers under that customer, deletes matching records in the `transactions` table first, deletes the customer's accounts in the `accounts` table second, and finally deletes the customer from the `customers` table before committing.
> 3. **Status Checks on Frozen Accounts**: I implemented status-check validations. If an account is `FROZEN` or `CLOSED`, we throw an `AccountFrozenException` during deposit, withdrawal, or transfer attempts. In this stage, I wired these checks into the UI layer, catching these exceptions in `CustomerMenu` to block operations while keeping the system stable."*

---

### 5. Summary & Learning Outcomes (4.5 to 5 Minutes)
> *"Building this project deepened my understanding of database transactions and backend system design. I learned how to cleanly handle complex database schemas in Java, write defensive code using custom checked exceptions, and protect SQL queries against Injection attacks using `PreparedStatement`. It represents a solid foundation for building secure and scalable Java applications."*
