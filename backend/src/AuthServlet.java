package backend.src;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Servlet for handling authentication operations (login, signup, logout)
 */
@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {
    
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
        
        String pathInfo = request.getPathInfo();
        
        // Set CORS headers
        setCORSHeaders(response);
        
        if (pathInfo == null) {
            sendErrorResponse(response, "Invalid endpoint", 400);
            return;
        }
        
        switch (pathInfo) {
            case "/check":
                checkAuthStatus(request, response);
                break;
            case "/logout":
                logout(request, response);
                break;
            default:
                sendErrorResponse(response, "Endpoint not found", 404);
                break;
        }
    }
    
    /**
     * Handle POST requests
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        // Set CORS headers
        setCORSHeaders(response);
        
        if (pathInfo == null) {
            sendErrorResponse(response, "Invalid endpoint", 400);
            return;
        }
        
        switch (pathInfo) {
            case "/login":
                login(request, response);
                break;
            case "/signup":
                signup(request, response);
                break;
            default:
                sendErrorResponse(response, "Endpoint not found", 404);
                break;
        }
    }
    
    /**
     * Handle OPTIONS requests for CORS
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        setCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    /**
     * Handle user login
     */
    private void login(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse JSON request body
            JsonObject requestData = parseRequestBody(request);
            
            if (requestData == null) {
                sendErrorResponse(response, "Invalid JSON data", 400);
                return;
            }
            
            String email = getJsonString(requestData, "email");
            String password = getJsonString(requestData, "password");
            
            // Validate input
            if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
                sendErrorResponse(response, "Email and password are required", 400);
                return;
            }
            
            // Authenticate user
            UserDAO.User user = userDAO.authenticateUser(email.trim(), password);
            
            if (user != null) {
                // Create session
                HttpSession session = request.getSession(true);
                session.setAttribute("userId", user.getId());
                session.setAttribute("userName", user.getName());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("userRole", user.getRole());
                session.setMaxInactiveInterval(3600); // 1 hour session timeout
                
                // Send success response
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "Login successful");
                
                JsonObject userData = new JsonObject();
                userData.addProperty("id", user.getId());
                userData.addProperty("name", user.getName());
                userData.addProperty("email", user.getEmail());
                userData.addProperty("role", user.getRole());
                
                responseData.add("user", userData);
                
                sendSuccessResponse(response, responseData);
                
            } else {
                sendErrorResponse(response, "Invalid email or password", 401);
            }
            
        } catch (Exception e) {
            System.err.println("Error in login: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Handle user signup
     */
    private void signup(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse JSON request body
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
            
            // Validate input
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
            
            // Check if email already exists
            if (userDAO.emailExists(email.trim())) {
                sendErrorResponse(response, "Email already registered", 409);
                return;
            }
            
            // Register user
            boolean success = userDAO.registerUser(name.trim(), email.trim(), password, role.toLowerCase());
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "Account created successfully");
                
                sendSuccessResponse(response, responseData);
                
            } else {
                sendErrorResponse(response, "Failed to create account", 500);
            }
            
        } catch (Exception e) {
            System.err.println("Error in signup: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Check authentication status
     */
    private void checkAuthStatus(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("userId") != null) {
            JsonObject responseData = new JsonObject();
            responseData.addProperty("authenticated", true);
            
            JsonObject userData = new JsonObject();
            userData.addProperty("id", (Integer) session.getAttribute("userId"));
            userData.addProperty("name", (String) session.getAttribute("userName"));
            userData.addProperty("email", (String) session.getAttribute("userEmail"));
            userData.addProperty("role", (String) session.getAttribute("userRole"));
            
            responseData.add("user", userData);
            
            sendSuccessResponse(response, responseData);
        } else {
            JsonObject responseData = new JsonObject();
            responseData.addProperty("authenticated", false);
            responseData.addProperty("message", "Not authenticated");
            
            sendSuccessResponse(response, responseData);
        }
    }
    
    /**
     * Handle logout
     */
    private void logout(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            session.invalidate();
        }
        
        JsonObject responseData = new JsonObject();
        responseData.addProperty("success", true);
        responseData.addProperty("message", "Logged out successfully");
        
        sendSuccessResponse(response, responseData);
    }
    
    /**
     * Set CORS headers
     */
    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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
