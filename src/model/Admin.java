package model;

/**
 * Admin - Model class representing an administrator, extending User.
 * 
 * OOP CONCEPT — INHERITANCE:
 * Admin extends User, inheriting the common fields id (mapping to admin_id)
 * and password, while adding admin-specific fields like username.
 */
public class Admin extends User {
    private String username;

    public Admin() {
        super();
    }

    public Admin(int adminId, String username, String password) {
        super(adminId, password);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Admin {" +
                " adminId=" + getId() +
                ", username='" + username + '\'' +
                " }";
    }
}
