package model;

/**
 * AuditLog - Model class representing an audit trail log entry.
 *
 * STAGE 13 — AUDIT LOGGING
 * ------------------------
 * Maps to the 'audit_logs' table in MySQL.
 * Records important security, authentication, account, and financial operations
 * for compliance, traceability, and operational auditing.
 *
 * Database Table Reference:
 * ┌──────────────┬───────────────────┬──────────────────────────────────────┐
 * │ Column       │ Type              │ Notes                                │
 * ├──────────────┼───────────────────┼──────────────────────────────────────┤
 * │ log_id       │ INT (PK, AI)      │ Auto-generated                       │
 * │ user_id      │ INT (FK, Nullable)│ References customers/admins          │
 * │ username     │ VARCHAR(100)      │ Email or Username                    │
 * │ action       │ VARCHAR(50)       │ e.g. LOGIN, DEPOSIT, WITHDRAWAL      │
 * │ description  │ VARCHAR(255)      │ Event context (no sensitive data)    │
 * │ status       │ VARCHAR(20)       │ SUCCESS or FAILURE                   │
 * │ timestamp    │ TIMESTAMP         │ Auto-generated timestamp             │
 * └──────────────┴───────────────────┴──────────────────────────────────────┘
 */
public class AuditLog {

    private int     logId;
    private Integer userId;      // Use Integer object to allow null
    private String  username;
    private String  action;
    private String  description;
    private String  status;      // "SUCCESS" or "FAILURE"
    private String  timestamp;

    // Constructors
    public AuditLog() {
    }

    /**
     * Constructor for creating a new AuditLog object before saving to database.
     */
    public AuditLog(Integer userId, String username, String action, String description, String status) {
        this.userId      = userId;
        this.username    = username;
        this.action      = action;
        this.description = description;
        this.status      = status;
    }

    /**
     * Full constructor for reconstructing an AuditLog object from ResultSet.
     */
    public AuditLog(int logId, Integer userId, String username, String action, String description, String status, String timestamp) {
        this.logId       = logId;
        this.userId      = userId;
        this.username    = username;
        this.action      = action;
        this.description = description;
        this.status      = status;
        this.timestamp   = timestamp;
    }

    // Getters and Setters
    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AuditLog {" +
                " logId=" + logId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", action='" + action + '\'' +
                ", status='" + status + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", description='" + description + '\'' +
                " }";
    }
}
