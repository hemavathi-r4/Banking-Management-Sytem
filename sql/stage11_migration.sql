-- ============================================================
-- Stage 11 Migration Script — Password Hashing Security Upgrade
-- ============================================================
-- Run this script in MySQL AFTER deploying Stage 11 changes.
--
-- PURPOSE:
--   1. Confirms the password columns can hold BCrypt hashes (60 chars each).
--   2. Updates the default admin account password to a BCrypt hash.
--   3. Provides guidance on handling existing plain-text customer passwords.
--
-- IMPORTANT: This script is SAFE — it does NOT drop any tables or delete data.
-- ============================================================

USE bank_management;

-- ============================================================
-- Step 1: Ensure password columns are wide enough for BCrypt hashes
-- ============================================================
-- BCrypt hashes are exactly 60 characters long.
-- Both columns are already VARCHAR(255) per the original schema,
-- but we re-declare them to document the intent explicitly.
-- If your columns were smaller, this ALTER would resize them.
-- ============================================================

ALTER TABLE customers MODIFY password VARCHAR(255) NOT NULL;
ALTER TABLE admins    MODIFY password VARCHAR(255) NOT NULL;

-- ============================================================
-- Step 2: Update the default admin password to a BCrypt hash
-- ============================================================
-- The original admin record was inserted with plain text: 'admin123'
-- We replace it with the BCrypt hash of 'admin123' (cost factor 12).
--
-- Hash for 'admin123':  $2a$12$Ov7WjkYFNGjY3V.yrOqHAuLqeQhH6v.INEdOp0r4hHWOxZqXlVBMq
--
-- After running this script, log in with:
--   Username: admin
--   Password: admin123   (the application will BCrypt-verify this)
-- ============================================================

UPDATE admins
SET    password = '$2a$12$Ov7WjkYFNGjY3V.yrOqHAuLqeQhH6v.INEdOp0r4hHWOxZqXlVBMq'
WHERE  username = 'admin';

-- Verify the update applied
SELECT admin_id, username,
       SUBSTRING(password, 1, 7) AS hash_prefix,
       LENGTH(password)          AS hash_length
FROM   admins;

-- ============================================================
-- Step 3: Existing plain-text CUSTOMER passwords
-- ============================================================
-- NOTE: BCrypt is a one-way hash — there is NO way to automatically
-- convert existing plain-text passwords to BCrypt hashes without knowing
-- the original passwords.
--
-- OPTIONS for existing customers (choose one):
--
-- OPTION A (Recommended for development / fresh install):
--   Clear all existing test customer data and re-register:
--     DELETE FROM transactions;
--     DELETE FROM accounts;
--     DELETE FROM customers;
--   Then re-register customers through the application — passwords will
--   be hashed automatically.
--
-- OPTION B (For production with real users):
--   Force a password reset for all customers on their next login.
--   This requires building a "forgot password" / reset flow (future stage).
--   In the meantime, existing customers with plain-text passwords will
--   fail login because PasswordUtil.verifyPassword() will not match a
--   plain-text stored value against a BCrypt-format check.
--
-- For this project (development / educational), OPTION A is the clean approach.
-- ============================================================

-- ============================================================
-- Step 4: (Optional) Clear existing customer test data for clean restart
-- ============================================================
-- Uncomment these lines ONLY if you want to reset test customers:
--
-- DELETE FROM transactions;
-- DELETE FROM accounts;
-- DELETE FROM customers;
--
-- After uncommenting and running, re-register all customers through the app.
-- ============================================================

-- ============================================================
-- Done! Your database is now ready for Stage 11 BCrypt security.
-- ============================================================
