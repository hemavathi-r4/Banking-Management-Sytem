package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * PasswordUtil - Utility class for secure password hashing and verification.
 *
 * STAGE 11 — SECURITY UPGRADE
 * ----------------------------
 * Previously, passwords were stored as plain text in the database.
 * This class introduces BCrypt hashing so passwords are NEVER stored or
 * compared as plain text.
 *
 * WHY BCRYPT?
 * -----------
 * BCrypt is a password hashing function specifically designed for storing
 * passwords securely. It has several key properties:
 *
 * 1. ONE-WAY: You cannot reverse a BCrypt hash back to the original password.
 *    This means even if attackers get the database, they cannot read passwords.
 *
 * 2. SALTED: BCrypt automatically generates a random salt for every hash.
 *    The same password "abc123" will produce a DIFFERENT hash each time
 *    it is hashed. This prevents rainbow table attacks.
 *
 * 3. SLOW BY DESIGN: BCrypt has a configurable "work factor" (cost).
 *    A cost of 12 means 2^12 = 4096 iterations of hashing, making brute-force
 *    attacks extremely expensive computationally.
 *
 * HOW THE HASH LOOKS:
 * -------------------
 * "$2a$12$randomSalt22chars.hashedPasswordHere"
 *  ----  --  --------  ----------------------
 *  algo  cost  salt (22 chars)   hash (31 chars)
 *
 * Total length: 60 characters — fits in VARCHAR(255) easily.
 *
 * USAGE:
 * ------
 * Registration:  String hash = PasswordUtil.hashPassword(enteredPassword);
 *                // Store 'hash' in database, NOT the original password
 *
 * Login:         boolean valid = PasswordUtil.verifyPassword(enteredPassword, storedHash);
 *                // verifyPassword() re-hashes enteredPassword with same salt and compares
 *
 * LIBRARY USED: jBCrypt-0.4.jar by Damien Miller
 */
public class PasswordUtil {

    /**
     * The BCrypt work factor (cost).
     *
     * Cost 12 is a good balance between security and performance:
     * - High enough to slow down brute-force attackers significantly
     * - Fast enough for normal users to not notice any delay (~200–300ms)
     *
     * Increasing this by 1 doubles the hashing time.
     */
    private static final int BCRYPT_COST = 12;

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * WHEN TO USE: During customer REGISTRATION, before inserting into the database.
     *
     * HOW IT WORKS:
     *   1. BCrypt.gensalt(BCRYPT_COST) generates a random salt with the given cost factor.
     *   2. BCrypt.hashpw(plainText, salt) combines the password and salt, runs 2^12 rounds
     *      of Blowfish encryption, and returns the full 60-character hash string.
     *   3. The salt is embedded in the hash string, so you don't need to store it separately.
     *
     * @param plainTextPassword the raw password entered by the user (never store this)
     * @return a 60-character BCrypt hash string (safe to store in the database)
     * @throws IllegalArgumentException if plainTextPassword is null or empty
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        // gensalt() generates a new random salt each time — same password → different hash
        String salt = BCrypt.gensalt(BCRYPT_COST);
        return BCrypt.hashpw(plainTextPassword, salt);
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * WHEN TO USE: During customer LOGIN, after retrieving the stored hash from the database.
     *
     * HOW IT WORKS:
     *   1. BCrypt.checkpw() extracts the salt from the stored hash.
     *   2. It rehashes the candidate password with the same salt.
     *   3. It compares the result to the stored hash using a constant-time comparison
     *      (resistant to timing attacks).
     *
     * SECURITY NOTE:
     * This method never exposes the plain-text password to the database or logs.
     * The comparison is done entirely in Java memory.
     *
     * @param plainTextPassword the password entered by the user at login
     * @param storedHash        the BCrypt hash retrieved from the database
     * @return true if the password matches the stored hash, false otherwise
     */
    public static boolean verifyPassword(String plainTextPassword, String storedHash) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            return false;
        }
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // This can happen if storedHash is not a valid BCrypt hash format
            // (e.g., a legacy plain-text password that hasn't been migrated yet)
            return false;
        }
    }
}
