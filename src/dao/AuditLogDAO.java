package dao;

import database.DBConnection;
import model.AuditLog;
import model.PageResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditLogDAO - Data Access Object for audit logging database operations.
 *
 * STAGE 13 & 14 — AUDIT LOGGING & PAGINATION
 * ------------------------------------------
 * Handles all database operations for system audit trails:
 *   - Inserting audit log entries safely using PreparedStatement
 *   - Fetching paginated and filtered audit logs using SQL COUNT(*) and LIMIT ? OFFSET ?
 *   - Fetching user-specific audit logs
 */
public class AuditLogDAO {

    /**
     * Inserts a new audit log entry into the 'audit_logs' table.
     *
     * @param log the AuditLog object containing event details
     * @return true if log insertion succeeded, false otherwise
     */
    public boolean insertLog(AuditLog log) {
        String sql = "INSERT INTO audit_logs (user_id, username, action, description, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (log.getUserId() != null) {
                pstmt.setInt(1, log.getUserId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }

            pstmt.setString(2, log.getUsername());
            pstmt.setString(3, log.getAction());
            pstmt.setString(4, log.getDescription());
            pstmt.setString(5, log.getStatus());

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            // Print diagnostic on stderr without disrupting user UI
            System.err.println("[AuditLogDAO] Failed to record audit log: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves paginated and filtered audit logs from MySQL.
     *
     * Uses SQL COUNT(*) for total matching records and LIMIT ? OFFSET ? for pagination.
     * All filters use parameterized queries (PreparedStatement).
     *
     * @param page           the 1-indexed page number (min 1)
     * @param pageSize       the page size (min 1)
     * @param usernameFilter filter by username (exact or partial, nullable)
     * @param actionFilter   filter by action name (nullable)
     * @param statusFilter   filter by status ("SUCCESS" / "FAILURE", nullable)
     * @return PageResult containing matching AuditLog records and pagination metadata
     */
    public PageResult<AuditLog> getPaginatedLogs(int page, int pageSize, String usernameFilter,
                                                 String actionFilter, String statusFilter) {

        int validPage     = Math.max(1, page);
        int validPageSize = Math.max(1, pageSize);
        int offset        = (validPage - 1) * validPageSize;

        // Build dynamic WHERE clause based on non-empty filters
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (usernameFilter != null && !usernameFilter.trim().isEmpty()) {
            whereClause.append(" AND (username LIKE ? OR user_id = ?)");
            String term = "%" + usernameFilter.trim() + "%";
            params.add(term);

            int searchId = -1;
            try {
                searchId = Integer.parseInt(usernameFilter.trim());
            } catch (NumberFormatException ignored) {}
            params.add(searchId);
        }

        if (actionFilter != null && !actionFilter.trim().isEmpty() && !actionFilter.equalsIgnoreCase("ALL")) {
            whereClause.append(" AND action = ?");
            params.add(actionFilter.trim().toUpperCase());
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("ALL")) {
            whereClause.append(" AND status = ?");
            params.add(statusFilter.trim().toUpperCase());
        }

        String countSql = "SELECT COUNT(*) FROM audit_logs" + whereClause;
        String selectSql = "SELECT * FROM audit_logs" + whereClause +
                           " ORDER BY log_id DESC LIMIT ? OFFSET ?";

        long totalRecords = 0;
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {

            // Step 1: Count total matching records
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    countStmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        totalRecords = rs.getLong(1);
                    }
                }
            }

            // Step 2: Fetch paginated page records
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                int paramIdx = 1;
                for (Object p : params) {
                    selectStmt.setObject(paramIdx++, p);
                }
                selectStmt.setInt(paramIdx++, validPageSize);
                selectStmt.setInt(paramIdx, offset);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        int userIdVal = rs.getInt("user_id");
                        Integer userIdObj = rs.wasNull() ? null : userIdVal;

                        AuditLog log = new AuditLog(
                            rs.getInt("log_id"),
                            userIdObj,
                            rs.getString("username"),
                            rs.getString("action"),
                            rs.getString("description"),
                            rs.getString("status"),
                            rs.getString("timestamp")
                        );
                        logs.add(log);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[AuditLogDAO] Failed to fetch audit logs: " + e.getMessage());
        }

        return new PageResult<>(logs, validPage, validPageSize, totalRecords);
    }

    /**
     * Retrieves paginated audit logs for a specific user ID.
     *
     * @param userId   the user ID
     * @param page     1-indexed page number
     * @param pageSize page size
     * @return PageResult of AuditLog records for that user
     */
    public PageResult<AuditLog> getLogsByUserId(int userId, int page, int pageSize) {
        return getPaginatedLogs(page, pageSize, String.valueOf(userId), null, null);
    }
}
