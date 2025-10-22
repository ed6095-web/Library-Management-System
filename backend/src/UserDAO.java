package backend.src;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations
 * Handles database operations related to users (login, signup, user management)
 */
public class UserDAO {
    
    private Connection connection;
    
    /**
     * Constructor - initializes database connection
     */
    public UserDAO() {
        try {
            this.connection = DBConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("Error initializing UserDAO: " + e.getMessage());
        }
    }
    
    /**
     * Authenticate user login
     * @param email user email
     * @param password user password
     * @return User object if authentication successful, null otherwise
     */
    public User authenticateUser(String email, String password) {
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                
                System.out.println("User authenticated successfully: " + email);
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Register new user
     * @param name user full name
     * @param email user email
     * @param password user password
     * @param role user role (admin/user)
     * @return true if registration successful, false otherwise
     */
    public boolean registerUser(String name, String email, String password, String role) {
        String query = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.setString(4, role.toLowerCase());
            
            int rowsAffected = stmt.executeUpdate();
            DBConnection.commitTransaction();
            
            if (rowsAffected > 0) {
                System.out.println("User registered successfully: " + email);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Get user by ID
     * @param userId user ID
     * @return User object if found, null otherwise
     */
    public User getUserById(int userId) {
        String query = "SELECT * FROM users WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get all users (Admin only)
     * @return List of all users
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY created_at DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                
                users.add(user);
            }
            
            System.out.println("Retrieved " + users.size() + " users from database");
            
        } catch (SQLException e) {
            System.err.println("Error retrieving all users: " + e.getMessage());
        }
        
        return users;
    }
    
    /**
     * Update user information
     * @param userId user ID to update
     * @param name new name
     * @param email new email
     * @param role new role
     * @return true if update successful, false otherwise
     */
    public boolean updateUser(int userId, String name, String email, String role) {
        String query = "UPDATE users SET name = ?, email = ?, role = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, role.toLowerCase());
            stmt.setInt(4, userId);
            
            int rowsAffected = stmt.executeUpdate();
            DBConnection.commitTransaction();
            
            if (rowsAffected > 0) {
                System.out.println("User updated successfully: ID " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Delete user by ID
     * @param userId user ID to delete
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteUser(int userId) {
        String query = "DELETE FROM users WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            
            int rowsAffected = stmt.executeUpdate();
            DBConnection.commitTransaction();
            
            if (rowsAffected > 0) {
                System.out.println("User deleted successfully: ID " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Check if email already exists
     * @param email email to check
     * @return true if email exists, false otherwise
     */
    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get user statistics
     * @return UserStats object with user counts
     */
    public UserStats getUserStats() {
        UserStats stats = new UserStats();
        
        try {
            // Total users
            String totalQuery = "SELECT COUNT(*) FROM users";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(totalQuery)) {
                if (rs.next()) {
                    stats.setTotalUsers(rs.getInt(1));
                }
            }
            
            // Admin count
            String adminQuery = "SELECT COUNT(*) FROM users WHERE role = 'admin'";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(adminQuery)) {
                if (rs.next()) {
                    stats.setTotalAdmins(rs.getInt(1));
                }
            }
            
            // Regular users count
            stats.setTotalRegularUsers(stats.getTotalUsers() - stats.getTotalAdmins());
            
        } catch (SQLException e) {
            System.err.println("Error getting user statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Inner class for User entity
     */
    public static class User {
        private int id;
        private String name;
        private String email;
        private String role;
        private Timestamp createdAt;
        
        // Constructors
        public User() {}
        
        public User(String name, String email, String role) {
            this.name = name;
            this.email = email;
            this.role = role;
        }
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        
        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "', email='" + email + "', role='" + role + "'}";
        }
    }
    
    /**
     * Inner class for User Statistics
     */
    public static class UserStats {
        private int totalUsers;
        private int totalAdmins;
        private int totalRegularUsers;
        
        // Getters and Setters
        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
        
        public int getTotalAdmins() { return totalAdmins; }
        public void setTotalAdmins(int totalAdmins) { this.totalAdmins = totalAdmins; }
        
        public int getTotalRegularUsers() { return totalRegularUsers; }
        public void setTotalRegularUsers(int totalRegularUsers) { this.totalRegularUsers = totalRegularUsers; }
    }
}
