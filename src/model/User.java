package model;

/**
 * User - Base model class representing a generic user in the system.
 * 
 * OOP CONCEPT — INHERITANCE:
 * This class serves as the superclass for Customer and Admin. It encapsulates
 * fields and methods common to all users (like ID and password), allowing
 * sub-classes to inherit and reuse them.
 */
public class User {
    protected int id;
    protected String password;

    public User() {
    }

    public User(int id, String password) {
        this.id = id;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
