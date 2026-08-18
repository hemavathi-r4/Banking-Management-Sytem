-- ============================================================
-- Banking Management System — Stage 15, 16 & 17 Migration Script
-- ============================================================
-- SAFE TO RUN ON EXISTING DATABASE.
-- Does NOT drop or truncate any existing tables.
-- Does NOT delete or modify any existing data.
-- Uses IF NOT EXISTS / IF EXISTS guards throughout.
-- ============================================================

USE bank_management;

-- ============================================================
-- STAGE 15 — DATABASE CONSTRAINTS
-- Adding CHECK constraints for business rule enforcement at DB level.
-- These complement application-layer validation (defense in depth).
-- Requires MySQL 8.0.16+ for CHECK constraint enforcement.
-- ============================================================

-- Constraint: Account balance must never be negative.
-- Prevents data corruption at the database level even if application logic has a bug.
ALTER TABLE accounts
    ADD CONSTRAINT IF NOT EXISTS chk_accounts_balance_non_negative
    CHECK (balance >= 0);

-- Constraint: account_type must be one of the two allowed values.
ALTER TABLE accounts
    ADD CONSTRAINT IF NOT EXISTS chk_accounts_account_type
    CHECK (account_type IN ('SAVINGS', 'CURRENT'));

-- Constraint: status must be a valid state value.
ALTER TABLE accounts
    ADD CONSTRAINT IF NOT EXISTS chk_accounts_status
    CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'));

-- Constraint: Transaction amount must always be positive.
-- A zero or negative transaction amount is meaningless in a banking context.
ALTER TABLE transactions
    ADD CONSTRAINT IF NOT EXISTS chk_transactions_amount_positive
    CHECK (amount > 0);

-- ============================================================
-- STAGE 15 — ADDITIONAL INDEXES
-- Supplements the Stage 14 indexes with columns needed for
-- Stage 17 report GROUP BY / ORDER BY / WHERE queries.
-- Uses CREATE INDEX IF NOT EXISTS (MySQL 8.0+ syntax) to avoid
-- duplicate index errors on re-run.
-- ============================================================

-- Index on accounts.account_type
-- Used by: Account statistics GROUP BY account_type in ReportsDAO.
CREATE INDEX IF NOT EXISTS idx_accounts_type
    ON accounts(account_type);

-- Index on transactions.amount
-- Used by: amount range filter queries in Stage 14 pagination.
CREATE INDEX IF NOT EXISTS idx_transactions_amount
    ON transactions(amount);

-- Composite index on (transaction_type, transaction_time)
-- Used by: Daily/Monthly transaction reports — filters both type AND date together.
-- A composite index is more efficient than two separate single-column indexes
-- when both columns appear in the WHERE clause together.
CREATE INDEX IF NOT EXISTS idx_transactions_type_time
    ON transactions(transaction_type, transaction_time);

-- ============================================================
-- EXPLAIN ANALYSIS NOTES (Development Analysis Only)
-- The following queries were analyzed using EXPLAIN during development.
-- These are NOT executed at runtime — included here for documentation.
--
-- QUERY 1: Paginated transactions for an account (Stage 14)
--   EXPLAIN SELECT * FROM transactions WHERE from_account = 1001 OR to_account = 1001
--           ORDER BY transaction_time DESC LIMIT 10 OFFSET 0;
--   Result: Uses idx_transactions_from_acc and idx_transactions_to_acc (index merge)
--
-- QUERY 2: Daily report
--   EXPLAIN SELECT COUNT(*), SUM(amount), transaction_type FROM transactions
--           WHERE DATE(transaction_time) = '2026-08-18' GROUP BY transaction_type;
--   Result: Uses idx_transactions_type_time (composite index)
--
-- QUERY 3: Top active accounts
--   EXPLAIN SELECT from_account, COUNT(*) AS tx_count FROM transactions
--           GROUP BY from_account ORDER BY tx_count DESC LIMIT 10;
--   Result: Uses idx_transactions_from_acc
-- ============================================================

-- Done! Stage 15 migration complete.
