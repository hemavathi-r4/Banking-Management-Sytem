-- ============================================================
-- Banking Management System — Stage 13 & 14 Migration Script
-- ============================================================
-- Run this script in MySQL to create the audit_logs table
-- and performance indexes for search, filtering & pagination.
-- ============================================================

USE bank_management;

-- ============================================================
-- Table: audit_logs
-- Records security, authentication, administrative, and banking operations.
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NULL,                         -- Customer or Admin ID (if applicable)
    username    VARCHAR(100) NULL,                -- User identifier (email/username)
    action      VARCHAR(50)  NOT NULL,            -- e.g. LOGIN, LOGOUT, DEPOSIT, WITHDRAWAL, TRANSFER
    description VARCHAR(255) NULL,                -- Context/details of the operation
    status      VARCHAR(20)  NOT NULL,            -- SUCCESS or FAILURE
    timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES customers(customer_id)
        ON DELETE SET NULL
);

-- ============================================================
-- Database Indexes for Performance (Stage 14)
-- Accelerates search, filtering, sorting, and pagination queries.
-- ============================================================

-- Indexes on transactions table
CREATE INDEX idx_transactions_from_acc ON transactions(from_account);
CREATE INDEX idx_transactions_to_acc   ON transactions(to_account);
CREATE INDEX idx_transactions_type     ON transactions(transaction_type);
CREATE INDEX idx_transactions_time     ON transactions(transaction_time);

-- Indexes on accounts table
CREATE INDEX idx_accounts_cust_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status  ON accounts(status);

-- Indexes on customers table
CREATE INDEX idx_customers_name  ON customers(name);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_phone ON customers(phone);

-- Indexes on audit_logs table
CREATE INDEX idx_audit_logs_user_id   ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_username  ON audit_logs(username);
CREATE INDEX idx_audit_logs_action    ON audit_logs(action);
CREATE INDEX idx_audit_logs_status    ON audit_logs(status);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

-- Done!
