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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Servlet for handling book operations (CRUD, search, categories)
 */
@WebServlet("/books/*")
public class BookServlet extends HttpServlet {
    
    private BookDAO bookDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
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
            // Get all books
            getAllBooks(request, response);
            return;
        }
        
        switch (pathInfo) {
            case "/search":
                searchBooks(request, response);
                break;
            case "/categories":
                getCategories(request, response);
                break;
            case "/stats":
                getBookStats(request, response);
                break;
            default:
                if (pathInfo.startsWith("/")) {
                    try {
                        int bookId = Integer.parseInt(pathInfo.substring(1));
                        getBookById(request, response, bookId);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(response, "Invalid book ID", 400);
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
        addBook(request, response);
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
                int bookId = Integer.parseInt(pathInfo.substring(1));
                updateBook(request, response, bookId);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, "Invalid book ID", 400);
            }
        } else {
            sendErrorResponse(response, "Book ID required", 400);
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
                int bookId = Integer.parseInt(pathInfo.substring(1));
                deleteBook(request, response, bookId);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, "Invalid book ID", 400);
            }
        } else {
            sendErrorResponse(response, "Book ID required", 400);
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
     * Get all books
     */
    private void getAllBooks(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            String category = request.getParameter("category");
            List<BookDAO.Book> books;
            
            if (category != null && !category.trim().isEmpty()) {
                books = bookDAO.getBooksByCategory(category.trim());
            } else {
                books = bookDAO.getAllBooks();
            }
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("books", gson.toJsonTree(books));
            responseData.addProperty("total", books.size());
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting books: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve books", 500);
        }
    }
    
    /**
     * Get book by ID
     */
    private void getBookById(HttpServletRequest request, HttpServletResponse response, int bookId) 
            throws IOException {
        
        try {
            BookDAO.Book book = bookDAO.getBookById(bookId);
            
            if (book != null) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.add("book", gson.toJsonTree(book));
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Book not found", 404);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting book by ID: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve book", 500);
        }
    }
    
    /**
     * Search books
     */
    private void searchBooks(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            String searchTerm = request.getParameter("q");
            
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                sendErrorResponse(response, "Search term is required", 400);
                return;
            }
            
            List<BookDAO.Book> books = bookDAO.searchBooks(searchTerm.trim());
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("books", gson.toJsonTree(books));
            responseData.addProperty("total", books.size());
            responseData.addProperty("searchTerm", searchTerm.trim());
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error searching books: " + e.getMessage());
            sendErrorResponse(response, "Failed to search books", 500);
        }
    }
    
    /**
     * Get all categories
     */
    private void getCategories(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            List<String> categories = bookDAO.getAllCategories();
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("categories", gson.toJsonTree(categories));
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting categories: " + e.getMessage());
            sendErrorResponse(response, "Failed to retrieve categories", 500);
        }
    }
    
    /**
     * Add new book
     */
    private void addBook(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            JsonObject requestData = parseRequestBody(request);
            
            if (requestData == null) {
                sendErrorResponse(response, "Invalid JSON data", 400);
                return;
            }
            
            String title = getJsonString(requestData, "title");
            String author = getJsonString(requestData, "author");
            String category = getJsonString(requestData, "category");
            String isbn = getJsonString(requestData, "isbn");
            int totalCopies = getJsonInt(requestData, "totalCopies", 1);
            
            // Validate required fields
            if (title == null || author == null || category == null || 
                title.trim().isEmpty() || author.trim().isEmpty() || category.trim().isEmpty()) {
                sendErrorResponse(response, "Title, author, and category are required", 400);
                return;
            }
            
            // Validate total copies
            if (totalCopies < 1) {
                sendErrorResponse(response, "Total copies must be at least 1", 400);
                return;
            }
            
            // Check for duplicate ISBN if provided
            if (isbn != null && !isbn.trim().isEmpty() && bookDAO.isbnExists(isbn.trim())) {
                sendErrorResponse(response, "ISBN already exists", 409);
                return;
            }
            
            boolean success = bookDAO.addBook(title.trim(), author.trim(), category.trim(), 
                                            isbn != null ? isbn.trim() : null, totalCopies);
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "Book added successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to add book", 500);
            }
            
        } catch (Exception e) {
            System.err.println("Error adding book: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Update book
     */
    private void updateBook(HttpServletRequest request, HttpServletResponse response, int bookId) 
            throws IOException {
        
        try {
            JsonObject requestData = parseRequestBody(request);
            
            if (requestData == null) {
                sendErrorResponse(response, "Invalid JSON data", 400);
                return;
            }
            
            String title = getJsonString(requestData, "title");
            String author = getJsonString(requestData, "author");
            String category = getJsonString(requestData, "category");
            String isbn = getJsonString(requestData, "isbn");
            int totalCopies = getJsonInt(requestData, "totalCopies", 1);
            
            // Validate required fields
            if (title == null || author == null || category == null || 
                title.trim().isEmpty() || author.trim().isEmpty() || category.trim().isEmpty()) {
                sendErrorResponse(response, "Title, author, and category are required", 400);
                return;
            }
            
            // Validate total copies
            if (totalCopies < 1) {
                sendErrorResponse(response, "Total copies must be at least 1", 400);
                return;
            }
            
            boolean success = bookDAO.updateBook(bookId, title.trim(), author.trim(), 
                                               category.trim(), isbn != null ? isbn.trim() : null, totalCopies);
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "Book updated successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to update book or book not found", 404);
            }
            
        } catch (Exception e) {
            System.err.println("Error updating book: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Delete book
     */
    private void deleteBook(HttpServletRequest request, HttpServletResponse response, int bookId) 
            throws IOException {
        
        try {
            boolean success = bookDAO.deleteBook(bookId);
            
            if (success) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("success", true);
                responseData.addProperty("message", "Book deleted successfully");
                
                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, "Failed to delete book or book not found", 404);
            }
            
        } catch (Exception e) {
            System.err.println("Error deleting book: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    /**
     * Get book statistics
     */
    private void getBookStats(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            BookDAO.BookStats stats = bookDAO.getBookStats();
            
            JsonObject responseData = new JsonObject();
            responseData.addProperty("success", true);
            responseData.add("stats", gson.toJsonTree(stats));
            
            sendSuccessResponse(response, responseData);
            
        } catch (Exception e) {
            System.err.println("Error getting book statistics: " + e.getMessage());
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
