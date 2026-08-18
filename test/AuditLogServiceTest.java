import model.AuditLog;
import model.PageResult;
import service.AuditLogService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * AuditLogServiceTest - JUnit tests for AuditLogService and AuditLogDAO.
 *
 * STAGE 13 & 14 — JUNIT TESTING
 * -----------------------------
 * Tests audit log insertion, success/failure event logging, user-specific log retrieval,
 * and filtering by action/status.
 */
public class AuditLogServiceTest {

    private AuditLogService auditLogService;
    private int testCustId;
    private String testUserEmail;

    @Before
    public void setUp() {
        auditLogService = new AuditLogService();
        TestDBHelper.cleanupTestData();

        testUserEmail = TestDBHelper.testEmail("audit_user");
        testCustId = TestDBHelper.insertTestCustomer(
            "Audit User", testUserEmail, TestDBHelper.testPhone(40), "pass123", "Street"
        );
    }

    @After
    public void tearDown() {
        TestDBHelper.cleanupTestData();
    }

    @Test
    public void shouldLogSuccessEventSuccessfully() {
        boolean logged = auditLogService.logSuccess(
            testCustId, testUserEmail, "LOGIN", "Customer logged in"
        );
        assertTrue("Success log insertion should return true", logged);

        PageResult<AuditLog> pageResult = auditLogService.getLogsByUserId(testCustId, 1, 10);
        assertFalse("Logs should not be empty for test user", pageResult.getRecords().isEmpty());

        AuditLog log = pageResult.getRecords().get(0);
        assertEquals("LOGIN", log.getAction());
        assertEquals("SUCCESS", log.getStatus());
        assertEquals(testUserEmail, log.getUsername());
    }

    @Test
    public void shouldLogFailureEventSuccessfully() {
        boolean logged = auditLogService.logFailure(
            testCustId, testUserEmail, "WITHDRAWAL", "Withdrawal failed due to insufficient funds"
        );
        assertTrue("Failure log insertion should return true", logged);

        PageResult<AuditLog> pageResult = auditLogService.getPaginatedLogs(1, 10, testUserEmail, "WITHDRAWAL", "FAILURE");
        assertFalse("Filtered logs should return recorded failure", pageResult.getRecords().isEmpty());

        AuditLog log = pageResult.getRecords().get(0);
        assertEquals("WITHDRAWAL", log.getAction());
        assertEquals("FAILURE", log.getStatus());
    }

    @Test
    public void shouldFilterLogsByActionAndStatus() {
        auditLogService.logSuccess(testCustId, testUserEmail, "DEPOSIT", "Deposited 500");
        auditLogService.logFailure(testCustId, testUserEmail, "TRANSFER", "Transfer failed");

        PageResult<AuditLog> deposits = auditLogService.getPaginatedLogs(1, 10, testUserEmail, "DEPOSIT", "SUCCESS");
        assertEquals("Should find 1 deposit log", 1, deposits.getTotalRecords());

        PageResult<AuditLog> transfers = auditLogService.getPaginatedLogs(1, 10, testUserEmail, "TRANSFER", "FAILURE");
        assertEquals("Should find 1 transfer failure log", 1, transfers.getTotalRecords());
    }

    @Test
    public void shouldHandleNullUserIdForAnonymousLogs() {
        boolean logged = auditLogService.logFailure(
            null, "ANONYMOUS_USER", "LOGIN", "Login attempt with unknown email"
        );
        assertTrue("Anonymous log should be accepted", logged);
    }
}
