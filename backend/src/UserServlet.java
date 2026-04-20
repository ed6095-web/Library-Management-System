package backend.src;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Servlet for handling user management operations (Admin only)
 */
@WebServlet("/users/*")
public class UserServlet extends HttpServlet {
    
    private UserDAO userDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        gson = new Gson();
    }
    
    /**
     * Handle GET requests
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check authentication and admin role
        if (!isAuthenticated(request)) {
            sendErrorResponse(response, "Authentication required", 401);
            return;
        }
        
        if (!isAdmin(request)) {
            sendErrorResponse(response, "Admin access required", 403);
            return;
        }
        
        String pathInfo = request.getPathInfo();
        
        setCORSHeaders(response);
        
        if (pathInfo == null) {
            // Get all users
            getAllUsers(request, response);
            return;
        }
        
        switch (pathInfo) {
            case "/stats":
                getUserStats(request, response);
                break;
            default:
                if (pathInfo.startsWith("/")) {
                    try {
                        int userId = Integer.parseInt(pathInfo.substring(1));
                        getUserById(request, response, userId);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(response, "Invalid user ID", 400);
                    }
                } else {
                    sendErrorResponse(response, "Endpoint not found", 404);
                }
                break;
        }
    }
    
    /**
     * Handle POST requests
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check authentication and admin role
        if (!isAuthenticated(request) || !isAdmin(request)) {
            sendErrorResponse(response, "Admin access required", 403);
            return;
        }
        
        setCORSHeaders(response);
        createUser(request, response);
    }
    
    /**
     * Handle PUT requests
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check authentication and admin role
        if (!isAuthenticated(request) || !isAdmin(request)) {
            sendErrorResponse(response, "Admin access required", 403);
            return;
        }
        
        setCORSHeaders(response);
        
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/")) {
            try {
                int userId = Integer.parseInt(pathInfo.substring(1));
                updateUser(request, response, userId);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, "Invalid user ID", 400);
            }
        } else {
            sendErrorResponse(response, "User ID required", 400);
        }
    }
    
    /**
     * Handle DELETE requests
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check authentication and admin role
        if (!isAuthenticated(request) || !isAdmin(request)) {
            sendErrorResponse(response, "Admin access required", 403);
            return;
        }
        
        setCORSHeaders(response);
        
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/")) {
            try {
                int userId = Integer.parseInt(pathInfo.substring(1));
                
                // Prevent admin from deleting themselves
                HttpSession session = request.getSession(false);
                int currentUserId = (Integer) session.getAttribute("userId");
                
                if (userId == currentUserId) {
                    sendErrorResponse(response, "Cannot delete your own account", 400);
                    return;
                }
                
                deleteUser(request, response, userId);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, "Invalid user ID", 400);
            }
        } else {
            sendErrorResponse(response, "User ID required", 400);
        }
    }
    
    /**
     * Handle OPTIONS requests for CORS
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        CORSUtil.setCORSHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    /**
     * Get all users
     */
    private void getAllUsers(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            List<UserDAO.User> users = userDAO.getAllUsers();
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("users", gson.toJsonTree(users));
            responseData.addProperty("total", users.size());
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting all users: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve users", 500);
        }
    }
    
    /**
     * Get user by ID
     */
    private void getUserById(HttpServletRequest request, HttpServletResponse response, int userId) 
            throws IOException {
        
        try {
            UserDAO.User user = userDAO.getUserById(userId);
            
            if (user != null) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.add("user", gson.toJsonTree(user));
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "User not found", 404);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve user", 500);
        }
    }
    
    /**
     * Create new user (Admin function)
     */
    private void createUser(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            JsonObject requestData = parseRequestBody(request);
            
            if (requestData == null) {
                sendErrorResponse(response, "Invalid JSON data", 400);
                return;
            }
            
            String name = getJsonString(requestData, "name");
            String email = getJsonString(requestData, "email");
            String password = getJsonString(requestData, "password");
            String role = getJsonString(requestData, "role");
            
            // Set default role if not provided
            if (role == null || role.trim().isEmpty()) {
                role = "user";
            }
            
            // Validate required fields
            if (name == null || email == null || password == null || 
                name.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
                sendErrorResponse(response, "Name, email, and password are required", 400);
                return;
            }
            
            // Validate email format
            if (!isValidEmail(email.trim())) {
                sendErrorResponse(response, "Invalid email format", 400);
                return;
            }
            
            // Validate password strength
            if (password.length() < 6) {
                sendErrorResponse(response, "Password must be at least 6 characters long", 400);
                return;
            }
            
            // Validate role
            if (!role.toLowerCase().equals("admin") && !role.toLowerCase().equals("user")) {
                sendErrorResponse(response, "Role must be 'admin' or 'user'", 400);
                return;
            }
            
            // Check if email already exists
            if (userDAO.emailExists(email.trim())) {
                sendErrorResponse(response, "Email already registered", 409);
                return;
            }
            
            boolean success = userDAO.registerUser(name.trim(), email.trim(), password, role.toLowerCase());
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "User created successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to create user", 500);
            }
            
        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Update user
     */
    private void updateUser(HttpServletRequest request, HttpServletResponse response, int userId) 
            throws IOException {
        
        try {
            JsonObject requestData = parseRequestBody(request);
            
            if (requestData == null) {
                sendErrorResponse(response, "Invalid JSON data", 400);
                return;
            }
            
            String name = getJsonString(requestData, "name");
            String email = getJsonString(requestData, "email");
            String role = getJsonString(requestData, "role");
            
            // Validate required fields
            if (name == null || email == null || role == null || 
                name.trim().isEmpty() || email.trim().isEmpty() || role.trim().isEmpty()) {
                sendErrorResponse(response, "Name, email, and role are required", 400);
                return;
            }
            
            // Validate email format
            if (!isValidEmail(email.trim())) {
                sendErrorResponse(response, "Invalid email format", 400);
                return;
            }
            
            // Validate role
            if (!role.toLowerCase().equals("admin") && !role.toLowerCase().equals("user")) {
                sendErrorResponse(response, "Role must be 'admin' or 'user'", 400);
                return;
            }
            
            // Check if trying to change admin's own role to user
            HttpSession session = request.getSession(false);
            int currentUserId = (Integer) session.getAttribute("userId");
            
            if (userId == currentUserId && role.toLowerCase().equals("user")) {
                sendErrorResponse(response, "Cannot change your own role from admin to user", 400);
                return;
            }
            
            boolean success = userDAO.updateUser(userId, name.trim(), email.trim(), role.toLowerCase());
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "User updated successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to update user or user not found", 404);
            }
            
        } catch (Exception e) {
            System.err.println("Error updating user: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Delete user
     */
    private void deleteUser(HttpServletRequest request, HttpServletResponse response, int userId) 
            throws IOException {
        
        try {
            boolean success = userDAO.deleteUser(userId);
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "User deleted successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to delete user or user not found", 404);
            }
            
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Get user statistics
     */
    private void getUserStats(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            UserDAO.UserStats stats = userDAO.getUserStats();
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("stats", gson.toJsonTree(stats));
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting user statistics: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve statistics", 500);
        }
    }
    
    /**
     * Check if user is authenticated
     */
    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("userId") != null;
    }
    
    /**
     * Check if user is admin
     */
    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String role = (String) session.getAttribute("userRole");
            return "admin".equals(role);
        }
        return false;
    }
    
    /**
     * Set CORS headers
     */
    private void setCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        CORSUtil.setCORSHeaders(request, response);
    }
    
    private void setCORSHeaders(HttpServletResponse response) {
        // Legacy method - should not be used, but kept for compatibility
        CORSUtil.setCORSHeaders(null, response);
    }
    
    /**
     * Parse JSON request body
     */
    private JsonObject parseRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        String line;
        
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }
        
        try {
            return gson.fromJson(buffer.toString(), JsonObject.class);
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get string value from JSON object
     */
    private String getJsonString(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }
    
    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Send success response
     */
    private void sendSuccessResponse(HttpServletResponse response, JsonObject data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(data));
            out.flush();
        }
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        
        JsonObject errorData = new JsonObject();
        errorData.addProperty("success", false);
        errorData.addProperty("error", message);
        
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(errorData));
            out.flush();
        }
    }
}
