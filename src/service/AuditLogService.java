package service;

import dao.AuditLogDAO;
import model.AuditLog;
import model.PageResult;

/**
 * AuditLogService - Service layer for audit logging operations.
 *
 * STAGE 13 & 14 — AUDIT LOGGING & SERVICE LAYER
 * ---------------------------------------------
 * Centralizes audit trail generation and querying across the application.
 * Prevents duplicating database logging logic inside menus or controllers.
 */
public class AuditLogService {

    private final AuditLogDAO auditLogDAO;

    public AuditLogService() {
        this.auditLogDAO = new AuditLogDAO();
    }

    public AuditLogService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    /**
     * Records an audit log entry for a successful action.
     */
    public boolean logSuccess(Integer userId, String username, String action, String description) {
        AuditLog log = new AuditLog(userId, username, action, description, "SUCCESS");
        return auditLogDAO.insertLog(log);
    }

    /**
     * Records an audit log entry for a failed action.
     */
    public boolean logFailure(Integer userId, String username, String action, String description) {
        AuditLog log = new AuditLog(userId, username, action, description, "FAILURE");
        return auditLogDAO.insertLog(log);
    }

    /**
     * Fetches paginated and filtered audit logs for admin inspection.
     */
    public PageResult<AuditLog> getPaginatedLogs(int page, int pageSize, String usernameFilter,
                                                 String actionFilter, String statusFilter) {
        return auditLogDAO.getPaginatedLogs(page, pageSize, usernameFilter, actionFilter, statusFilter);
    }

    /**
     * Fetches audit logs specific to a given user ID.
     */
    public PageResult<AuditLog> getLogsByUserId(int userId, int page, int pageSize) {
        return auditLogDAO.getLogsByUserId(userId, page, pageSize);
    }
}
