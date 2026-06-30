-- ============================================
-- Banking Management System - Database Setup
-- ============================================
-- Run this script in MySQL to create the
-- database and all required tables.
-- ============================================

-- Step 1: Create the database
CREATE DATABASE IF NOT EXISTS bank_management;

-- Step 2: Switch to the new database
USE bank_management;

-- ============================================
-- Table: customers
-- Stores all registered customer information.
-- ============================================
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    phone       VARCHAR(15)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Table: accounts
-- Each customer can have one or more accounts.
-- account_type: 'SAVINGS' or 'CURRENT'
-- status: 'ACTIVE', 'FROZEN', or 'CLOSED'
-- ============================================
CREATE TABLE IF NOT EXISTS accounts (
    account_no   BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id  INT NOT NULL,
    account_type VARCHAR(20)    NOT NULL,
    balance      DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status       VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',

    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE CASCADE
);

-- Start account numbers from 1001 for realism
ALTER TABLE accounts AUTO_INCREMENT = 1001;

-- ============================================
-- Table: transactions
-- Records every deposit, withdrawal, and transfer.
-- transaction_type: 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER'
-- For deposits:    from_account is NULL
-- For withdrawals: to_account is NULL
-- For transfers:   both from_account and to_account are set
-- ============================================
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   INT AUTO_INCREMENT PRIMARY KEY,
    from_account     BIGINT,
    to_account       BIGINT,
    transaction_type VARCHAR(20)    NOT NULL,
    amount           DECIMAL(15, 2) NOT NULL,
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks          VARCHAR(255),

    FOREIGN KEY (from_account) REFERENCES accounts(account_no),
    FOREIGN KEY (to_account)   REFERENCES accounts(account_no)
);

-- ============================================
-- Table: admins
-- Stores admin credentials for system management.
-- ============================================
CREATE TABLE IF NOT EXISTS admins (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- ============================================
-- Insert a default admin so you can log in
-- Username: admin   Password: admin123
-- ============================================
INSERT INTO admins (username, password)
VALUES ('admin', 'admin123');

-- ============================================
-- Done! Your database is ready.
-- ============================================
