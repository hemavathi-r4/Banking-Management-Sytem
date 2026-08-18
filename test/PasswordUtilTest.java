import org.junit.Test;
import static org.junit.Assert.*;

/**
 * PasswordUtilTest - JUnit tests for the PasswordUtil BCrypt hashing utility.
 *
 * STAGE 12 — JUNIT TESTING
 * -------------------------
 * These tests verify the correctness and security properties of the BCrypt
 * password hashing implementation introduced in Stage 11.
 *
 * Tests DO NOT require a database connection — PasswordUtil is pure Java.
 * This makes these tests true unit tests (fast, isolated, repeatable).
 */
public class PasswordUtilTest {

    // -------------------------------------------------------
    // 1. Hash generation tests
    // -------------------------------------------------------

    /**
     * A hashed password should not equal the plain-text original.
     * This confirms that actual hashing occurred.
     */
    @Test
    public void shouldHashPasswordSuccessfully() {
        String plainText = "MySecret123";
        String hash = util.PasswordUtil.hashPassword(plainText);

        assertNotNull("Hash should not be null", hash);
        assertFalse("Hash should not be empty", hash.isEmpty());
        assertNotEquals("Hash must differ from plain text", plainText, hash);
        // BCrypt hashes always start with $2a$ (or $2b$) and are 60 chars
        assertTrue("Hash should start with BCrypt prefix", hash.startsWith("$2a$") || hash.startsWith("$2b$"));
        assertEquals("BCrypt hash should be 60 characters", 60, hash.length());
    }

    /**
     * The same password hashed twice should produce DIFFERENT hashes.
     * This confirms that BCrypt generates a random salt on each call.
     * (Rainbow table attacks become infeasible because of this property.)
     */
    @Test
    public void shouldGenerateDifferentHashesForSamePassword() {
        String plainText = "SamePassword!";
        String hash1 = util.PasswordUtil.hashPassword(plainText);
        String hash2 = util.PasswordUtil.hashPassword(plainText);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(
            "Two hashes of the same password must be different (random salt)",
            hash1, hash2
        );
    }

    // -------------------------------------------------------
    // 2. Password verification tests
    // -------------------------------------------------------

    /**
     * verifyPassword() must return true when the correct plain-text password
     * is checked against its BCrypt hash.
     */
    @Test
    public void shouldVerifyCorrectPassword() {
        String plainText = "CorrectHorseBatteryStaple";
        String hash = util.PasswordUtil.hashPassword(plainText);

        boolean result = util.PasswordUtil.verifyPassword(plainText, hash);
        assertTrue("Correct password should verify successfully", result);
    }

    /**
     * verifyPassword() must return false when an incorrect password is provided,
     * even if it is very similar to the real one.
     */
    @Test
    public void shouldRejectIncorrectPassword() {
        String plainText = "CorrectPassword";
        String hash = util.PasswordUtil.hashPassword(plainText);

        boolean result = util.PasswordUtil.verifyPassword("WrongPassword", hash);
        assertFalse("Incorrect password should be rejected", result);
    }

    /**
     * verifyPassword() must return false for an empty password,
     * without throwing an exception.
     */
    @Test
    public void shouldRejectEmptyPassword() {
        String hash = util.PasswordUtil.hashPassword("SomePassword");
        boolean result = util.PasswordUtil.verifyPassword("", hash);
        assertFalse("Empty password should be rejected", result);
    }

    /**
     * verifyPassword() must return false for a null password,
     * without throwing a NullPointerException.
     */
    @Test
    public void shouldRejectNullPassword() {
        String hash = util.PasswordUtil.hashPassword("SomePassword");
        boolean result = util.PasswordUtil.verifyPassword(null, hash);
        assertFalse("Null password should be rejected gracefully", result);
    }

    /**
     * verifyPassword() must return false when a null hash is provided,
     * without throwing a NullPointerException.
     */
    @Test
    public void shouldRejectNullHash() {
        boolean result = util.PasswordUtil.verifyPassword("AnyPassword", null);
        assertFalse("Null hash should be rejected gracefully", result);
    }

    /**
     * hashPassword() must throw an exception when called with a null value,
     * rather than silently storing a null hash.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnNullInputToHash() {
        util.PasswordUtil.hashPassword(null);
    }

    /**
     * hashPassword() must throw an exception when called with an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnEmptyInputToHash() {
        util.PasswordUtil.hashPassword("");
    }
}
