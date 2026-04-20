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
 * Servlet for handling loan operations (borrowing, returning, fines)
 */
@WebServlet("/loans/*")
public class LoanServlet extends HttpServlet {
    
    private LoanDAO loanDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        loanDAO = new LoanDAO();
        gson = new Gson();
    }
    
    /**
     * Handle GET requests
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Set CORS headers FIRST, before any auth checks
        setCORSHeaders(request, response);
        
        // Check authentication
        if (!isAuthenticated(request)) {
            sendErrorResponse(response, "Authentication required", 401);
            return;
        }
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null) {
            // Get loans based on user role
            if (isAdmin(request)) {
                getAllLoans(request, response);
            } else {
                getUserLoans(request, response);
            }
            return;
        }
        
        switch (pathInfo) {
            case "/active":
                getActiveLoans(request, response);
                break;
            case "/overdue":
                getOverdueLoans(request, response);
                break;
            case "/stats":
                getLoanStats(request, response);
                break;
            case "/fines":
                getUserFines(request, response);
                break;
            default:
                if (pathInfo.startsWith("/")) {
                    try {
                        int loanId = Integer.parseInt(pathInfo.substring(1));
                        getLoanById(request, response, loanId);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(response, "Invalid loan ID", 400);
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
        
        // Set CORS headers FIRST, before any auth checks
        setCORSHeaders(request, response);
        
        // Check authentication
        if (!isAuthenticated(request)) {
            sendErrorResponse(response, "Authentication required", 401);
            return;
        }
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null) {
            sendErrorResponse(response, "Invalid endpoint", 400);
            return;
        }
        
        switch (pathInfo) {
            case "/borrow":
                borrowBook(request, response);
                break;
            case "/return":
                returnBook(request, response);
                break;
            case "/pay-fine":
                payFine(request, response);
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
        CORSUtil.setCORSHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    /**
     * Get all loans (Admin only)
     */
    private void getAllLoans(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        if (!isAdmin(request)) {
            sendErrorResponse(response, "Admin access required", 403);
            return;
        }
        
        try {
            List<LoanDAO.LoanDetails> loans = loanDAO.getAllLoans();
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("loans", gson.toJsonTree(loans));
            responseData.addProperty("total", loans.size());
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting all loans: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve loans", 500);
        }
    }
    
    /**
     * Get user's loans
     */
    private void getUserLoans(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            HttpSession session = request.getSession(false);
            int userId = (Integer) session.getAttribute("userId");
            
            List<LoanDAO.LoanDetails> loans = loanDAO.getLoansByUserId(userId);
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("loans", gson.toJsonTree(loans));
            responseData.addProperty("total", loans.size());
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting user loans: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve loans", 500);
        }
    }
    
    /**
     * Get loan by ID
     */
    private void getLoanById(HttpServletRequest request, HttpServletResponse response, int loanId) 
            throws IOException {
        
        try {
            LoanDAO.Loan loan = loanDAO.getLoanById(loanId);
            
            if (loan != null) {
                // Check if user has permission to view this loan
                HttpSession session = request.getSession(false);
                int userId = (Integer) session.getAttribute("userId");
                String userRole = (String) session.getAttribute("userRole");
                
                if (!"admin".equals(userRole) && loan.getUserId() != userId) {
                    sendErrorResponse(response, "Access denied", 403);
                    return;
                }
                
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.add("loan", gson.toJsonTree(loan));
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Loan not found", 404);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting loan by ID: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve loan", 500);
        }
    }
    
    /**
     * Get active loans
     */
    private void getActiveLoans(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            List<LoanDAO.LoanDetails> loans;
            
            if (isAdmin(request)) {
                loans = loanDAO.getActiveLoans();
            } else {
                HttpSession session = request.getSession(false);
                int userId = (Integer) session.getAttribute("userId");
                loans = loanDAO.getLoansByUserId(userId);
                // Filter only active loans
                loans.removeIf(loan -> !"active".equals(loan.getStatus()));
            }
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("loans", gson.toJsonTree(loans));
            responseData.addProperty("total", loans.size());
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting active loans: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve active loans", 500);
        }
    }
    
    /**
     * Get overdue loans
     */
    private void getOverdueLoans(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        if (!isAdmin(request)) {
            sendErrorResponse(response, "Admin access required", 403);
            return;
        }
        
        try {
            List<LoanDAO.LoanDetails> loans = loanDAO.getOverdueLoans();
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("loans", gson.toJsonTree(loans));
            responseData.addProperty("total", loans.size());
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting overdue loans: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve overdue loans", 500);
        }
    }
    
    /**
     * Borrow book
     */
    private void borrowBook(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            JsonObject requestData = parseRequestBody(request);
            
            if (requestData == null) {
                sendErrorResponse(response, "Invalid JSON data", 400);
                return;
            }
            
            int bookId = getJsonInt(requestData, "bookId", -1);
            
            if (bookId == -1) {
                sendErrorResponse(response, "Book ID is required", 400);
                return;
            }
            
            HttpSession session = request.getSession(false);
            int userId = (Integer) session.getAttribute("userId");
            
            boolean success = loanDAO.borrowBook(userId, bookId);
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "Book borrowed successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to borrow book. Book may not be available or already borrowed by you.", 400);
            }
            
        } catch (Exception e) {
            System.err.println("Error borrowing book: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Return book
     */
    private void returnBook(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            JsonObject requestData = parseRequestBody(request);
            
            if (requestData == null) {
                sendErrorResponse(response, "Invalid JSON data", 400);
                return;
            }
            
            int loanId = getJsonInt(requestData, "loanId", -1);
            
            if (loanId == -1) {
                sendErrorResponse(response, "Loan ID is required", 400);
                return;
            }
            
            // Check if user owns this loan or is admin
            LoanDAO.Loan loan = loanDAO.getLoanById(loanId);
            if (loan == null) {
                sendErrorResponse(response, "Loan not found", 404);
                return;
            }
            
            HttpSession session = request.getSession(false);
            int userId = (Integer) session.getAttribute("userId");
            String userRole = (String) session.getAttribute("userRole");
            
            if (!"admin".equals(userRole) && loan.getUserId() != userId) {
                sendErrorResponse(response, "Access denied", 403);
                return;
            }
            
            boolean success = loanDAO.returnBook(loanId);
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "Book returned successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to return book. Loan may already be returned.", 400);
            }
            
        } catch (Exception e) {
            System.err.println("Error returning book: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Get user fines
     */
    private void getUserFines(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            HttpSession session = request.getSession(false);
            int userId = (Integer) session.getAttribute("userId");
            
            List<LoanDAO.Fine> fines = loanDAO.getFinesByUserId(userId);
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("fines", gson.toJsonTree(fines));
            responseData.addProperty("total", fines.size());
            
            // Calculate total unpaid fines
            double totalUnpaid = fines.stream()
                    .filter(fine -> !fine.isPaid())
                    .mapToDouble(LoanDAO.Fine::getAmount)
                    .sum();
            
            responseData.addProperty("totalUnpaid", totalUnpaid);
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting user fines: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve fines", 500);
        }
    }
    
    /**
     * Pay fine
     */
    private void payFine(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            JsonObject requestData = parseRequestBody(request);
            
            if (requestData == null) {
                sendErrorResponse(response, "Invalid JSON data", 400);
                return;
            }
            
            int fineId = getJsonInt(requestData, "fineId", -1);
            
            if (fineId == -1) {
                sendErrorResponse(response, "Fine ID is required", 400);
                return;
            }
            
            boolean success = loanDAO.payFine(fineId);
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "Fine paid successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to pay fine or fine not found", 404);
            }
            
        } catch (Exception e) {
            System.err.println("Error paying fine: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Get loan statistics
     */
    private void getLoanStats(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        if (!isAdmin(request)) {
            sendErrorResponse(response, "Admin access required", 403);
            return;
        }
        
        try {
            LoanDAO.LoanStats stats = loanDAO.getLoanStats();
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("stats", gson.toJsonTree(stats));
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting loan statistics: " + e.getMessage());
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
     * Get integer value from JSON object with default
     */
    private int getJsonInt(JsonObject json, String key, int defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsInt();
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
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
