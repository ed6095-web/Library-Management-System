package backend.src;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Loan operations
 * Handles database operations related to loans (borrowing, returning, fines)
 */
public class LoanDAO {
    
    private Connection connection;
    private static final int LOAN_PERIOD_DAYS = 30; // Default loan period
    private static final double FINE_PER_DAY = 1.0; // Fine amount per overdue day
    
    /**
     * Constructor - initializes database connection
     */
    public LoanDAO() {
        try {
            this.connection = DBConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("Error initializing LoanDAO: " + e.getMessage());
        }
    }
    
    /**
     * Borrow a book
     * @param userId user ID
     * @param bookId book ID
     * @return true if book borrowed successfully, false otherwise
     */
    public boolean borrowBook(int userId, int bookId) {
        String loanQuery = "INSERT INTO loans (user_id, book_id, loan_date, due_date, status) VALUES (?, ?, ?, ?, 'active')";
        
        try {
            // Check if book is available
            BookDAO bookDAO = new BookDAO();
            BookDAO.Book book = bookDAO.getBookById(bookId);
            
            if (book == null || book.getAvailableCopies() <= 0) {
                System.out.println("Book not available for borrowing: ID " + bookId);
                return false;
            }
            
            // Check if user already has this book
            if (hasActiveBookLoan(userId, bookId)) {
                System.out.println("User already has an active loan for this book");
                return false;
            }
            
            LocalDate loanDate = LocalDate.now();
            LocalDate dueDate = loanDate.plusDays(LOAN_PERIOD_DAYS);
            
            try (PreparedStatement stmt = connection.prepareStatement(loanQuery)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, bookId);
                stmt.setDate(3, Date.valueOf(loanDate));
                stmt.setDate(4, Date.valueOf(dueDate));
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Update book availability
                    if (bookDAO.updateBookAvailability(bookId, true)) {
                        DBConnection.commitTransaction();
                        System.out.println("Book borrowed successfully - User: " + userId + ", Book: " + bookId);
                        return true;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error borrowing book: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Return a book
     * @param loanId loan ID
     * @return true if book returned successfully, false otherwise
     */
    public boolean returnBook(int loanId) {
        String returnQuery = "UPDATE loans SET return_date = ?, status = 'returned' WHERE id = ? AND status = 'active'";
        
        try {
            Loan loan = getLoanById(loanId);
            if (loan == null || !loan.getStatus().equals("active")) {
                System.out.println("Active loan not found: ID " + loanId);
                return false;
            }
            
            LocalDate returnDate = LocalDate.now();
            
            try (PreparedStatement stmt = connection.prepareStatement(returnQuery)) {
                stmt.setDate(1, Date.valueOf(returnDate));
                stmt.setInt(2, loanId);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Update book availability
                    BookDAO bookDAO = new BookDAO();
                    if (bookDAO.updateBookAvailability(loan.getBookId(), false)) {
                        
                        // Check for overdue and create fine if necessary
                        if (returnDate.isAfter(loan.getDueDate().toLocalDate())) {
                            long overdueDays = returnDate.toEpochDay() - loan.getDueDate().toLocalDate().toEpochDay();
                            double fineAmount = overdueDays * FINE_PER_DAY;
                            createFine(loanId, loan.getUserId(), fineAmount, "Book overdue by " + overdueDays + " days - $" + FINE_PER_DAY + " per day");
                        }
                        
                        DBConnection.commitTransaction();
                        System.out.println("Book returned successfully - Loan ID: " + loanId);
                        return true;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error returning book: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Get loan by ID
     * @param loanId loan ID
     * @return Loan object if found, null otherwise
     */
    public Loan getLoanById(int loanId) {
        String query = "SELECT * FROM loans WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, loanId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Loan loan = new Loan();
                loan.setId(rs.getInt("id"));
                loan.setUserId(rs.getInt("user_id"));
                loan.setBookId(rs.getInt("book_id"));
                loan.setLoanDate(rs.getDate("loan_date"));
                loan.setDueDate(rs.getDate("due_date"));
                loan.setReturnDate(rs.getDate("return_date"));
                loan.setStatus(rs.getString("status"));
                loan.setCreatedAt(rs.getTimestamp("created_at"));
                
                return loan;
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving loan by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get loans by user ID
     * @param userId user ID
     * @return List of loans for the user
     */
    public List<LoanDetails> getLoansByUserId(int userId) {
        List<LoanDetails> loans = new ArrayList<>();
        String query = "SELECT l.*, b.title, b.author, u.name as user_name " +
                      "FROM loans l " +
                      "JOIN books b ON l.book_id = b.id " +
                      "JOIN users u ON l.user_id = u.id " +
                      "WHERE l.user_id = ? " +
                      "ORDER BY l.created_at DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                LoanDetails loan = new LoanDetails();
                loan.setId(rs.getInt("id"));
                loan.setUserId(rs.getInt("user_id"));
                loan.setBookId(rs.getInt("book_id"));
                loan.setUserName(rs.getString("user_name"));
                loan.setBookTitle(rs.getString("title"));
                loan.setBookAuthor(rs.getString("author"));
                loan.setLoanDate(rs.getDate("loan_date"));
                loan.setDueDate(rs.getDate("due_date"));
                loan.setReturnDate(rs.getDate("return_date"));
                loan.setStatus(rs.getString("status"));
                loan.setCreatedAt(rs.getTimestamp("created_at"));
                
                loans.add(loan);
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving loans by user ID: " + e.getMessage());
        }
        
        return loans;
    }
    
    /**
     * Get all loans (Admin view)
     * @return List of all loans with details
     */
    public List<LoanDetails> getAllLoans() {
        List<LoanDetails> loans = new ArrayList<>();
        String query = "SELECT l.*, b.title, b.author, u.name as user_name " +
                      "FROM loans l " +
                      "JOIN books b ON l.book_id = b.id " +
                      "JOIN users u ON l.user_id = u.id " +
                      "ORDER BY l.created_at DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                LoanDetails loan = new LoanDetails();
                loan.setId(rs.getInt("id"));
                loan.setUserId(rs.getInt("user_id"));
                loan.setBookId(rs.getInt("book_id"));
                loan.setUserName(rs.getString("user_name"));
                loan.setBookTitle(rs.getString("title"));
                loan.setBookAuthor(rs.getString("author"));
                loan.setLoanDate(rs.getDate("loan_date"));
                loan.setDueDate(rs.getDate("due_date"));
                loan.setReturnDate(rs.getDate("return_date"));
                loan.setStatus(rs.getString("status"));
                loan.setCreatedAt(rs.getTimestamp("created_at"));
                
                loans.add(loan);
            }
            
            System.out.println("Retrieved " + loans.size() + " loans from database");
            
        } catch (SQLException e) {
            System.err.println("Error retrieving all loans: " + e.getMessage());
        }
        
        return loans;
    }
    
    /**
     * Get active loans
     * @return List of active loans
     */
    public List<LoanDetails> getActiveLoans() {
        List<LoanDetails> loans = new ArrayList<>();
        String query = "SELECT l.*, b.title, b.author, u.name as user_name " +
                      "FROM loans l " +
                      "JOIN books b ON l.book_id = b.id " +
                      "JOIN users u ON l.user_id = u.id " +
                      "WHERE l.status = 'active' " +
                      "ORDER BY l.due_date ASC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                LoanDetails loan = new LoanDetails();
                loan.setId(rs.getInt("id"));
                loan.setUserId(rs.getInt("user_id"));
                loan.setBookId(rs.getInt("book_id"));
                loan.setUserName(rs.getString("user_name"));
                loan.setBookTitle(rs.getString("title"));
                loan.setBookAuthor(rs.getString("author"));
                loan.setLoanDate(rs.getDate("loan_date"));
                loan.setDueDate(rs.getDate("due_date"));
                loan.setReturnDate(rs.getDate("return_date"));
                loan.setStatus(rs.getString("status"));
                loan.setCreatedAt(rs.getTimestamp("created_at"));
                
                loans.add(loan);
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving active loans: " + e.getMessage());
        }
        
        return loans;
    }
    
    /**
     * Get overdue loans
     * @return List of overdue loans
     */
    public List<LoanDetails> getOverdueLoans() {
        List<LoanDetails> loans = new ArrayList<>();
        String query = "SELECT l.*, b.title, b.author, u.name as user_name " +
                      "FROM loans l " +
                      "JOIN books b ON l.book_id = b.id " +
                      "JOIN users u ON l.user_id = u.id " +
                      "WHERE l.status = 'active' AND l.due_date < CURDATE() " +
                      "ORDER BY l.due_date ASC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                LoanDetails loan = new LoanDetails();
                loan.setId(rs.getInt("id"));
                loan.setUserId(rs.getInt("user_id"));
                loan.setBookId(rs.getInt("book_id"));
                loan.setUserName(rs.getString("user_name"));
                loan.setBookTitle(rs.getString("title"));
                loan.setBookAuthor(rs.getString("author"));
                loan.setLoanDate(rs.getDate("loan_date"));
                loan.setDueDate(rs.getDate("due_date"));
                loan.setReturnDate(rs.getDate("return_date"));
                loan.setStatus(rs.getString("status"));
                loan.setCreatedAt(rs.getTimestamp("created_at"));
                
                loans.add(loan);
            }
            
            // Update status to overdue
            updateOverdueStatus();
            
        } catch (SQLException e) {
            System.err.println("Error retrieving overdue loans: " + e.getMessage());
        }
        
        return loans;
    }
    
    /**
     * Update overdue loan status
     */
    private void updateOverdueStatus() {
        String query = "UPDATE loans SET status = 'overdue' WHERE status = 'active' AND due_date < CURDATE()";
        
        try (Statement stmt = connection.createStatement()) {
            int updatedRows = stmt.executeUpdate(query);
            if (updatedRows > 0) {
                DBConnection.commitTransaction();
                System.out.println("Updated " + updatedRows + " loans to overdue status");
            }
        } catch (SQLException e) {
            System.err.println("Error updating overdue status: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
    }
    
    /**
     * Check if user has active loan for a book
     * @param userId user ID
     * @param bookId book ID
     * @return true if user has active loan for the book, false otherwise
     */
    private boolean hasActiveBookLoan(int userId, int bookId) {
        String query = "SELECT COUNT(*) FROM loans WHERE user_id = ? AND book_id = ? AND status = 'active'";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking active book loan: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Create fine for overdue book
     * @param loanId loan ID
     * @param userId user ID
     * @param amount fine amount
     * @param reason fine reason
     * @return true if fine created successfully, false otherwise
     */
    public boolean createFine(int loanId, int userId, double amount, String reason) {
        String query = "INSERT INTO fines (loan_id, user_id, amount, reason, paid) VALUES (?, ?, ?, ?, FALSE)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, loanId);
            stmt.setInt(2, userId);
            stmt.setDouble(3, amount);
            stmt.setString(4, reason);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Fine created - User: " + userId + ", Amount: $" + amount);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating fine: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get fines by user ID
     * @param userId user ID
     * @return List of fines for the user
     */
    public List<Fine> getFinesByUserId(int userId) {
        List<Fine> fines = new ArrayList<>();
        String query = "SELECT * FROM fines WHERE user_id = ? ORDER BY created_at DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Fine fine = new Fine();
                fine.setId(rs.getInt("id"));
                fine.setLoanId(rs.getInt("loan_id"));
                fine.setUserId(rs.getInt("user_id"));
                fine.setAmount(rs.getDouble("amount"));
                fine.setReason(rs.getString("reason"));
                fine.setPaid(rs.getBoolean("paid"));
                fine.setCreatedAt(rs.getTimestamp("created_at"));
                
                fines.add(fine);
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving fines by user ID: " + e.getMessage());
        }
        
        return fines;
    }
    
    /**
     * Pay fine
     * @param fineId fine ID
     * @return true if payment successful, false otherwise
     */
    public boolean payFine(int fineId) {
        String query = "UPDATE fines SET paid = TRUE WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, fineId);
            
            int rowsAffected = stmt.executeUpdate();
            DBConnection.commitTransaction();
            
            if (rowsAffected > 0) {
                System.out.println("Fine paid successfully: ID " + fineId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error paying fine: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Get loan statistics
     * @return LoanStats object with loan counts
     */
    public LoanStats getLoanStats() {
        LoanStats stats = new LoanStats();
        
        try {
            // Total loans
            String totalQuery = "SELECT COUNT(*) FROM loans";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(totalQuery)) {
                if (rs.next()) {
                    stats.setTotalLoans(rs.getInt(1));
                }
            }
            
            // Active loans
            String activeQuery = "SELECT COUNT(*) FROM loans WHERE status = 'active'";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(activeQuery)) {
                if (rs.next()) {
                    stats.setActiveLoans(rs.getInt(1));
                }
            }
            
            // Overdue loans
            String overdueQuery = "SELECT COUNT(*) FROM loans WHERE status = 'active' AND due_date < CURDATE()";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(overdueQuery)) {
                if (rs.next()) {
                    stats.setOverdueLoans(rs.getInt(1));
                }
            }
            
            // Total fines
            String finesQuery = "SELECT SUM(amount) FROM fines WHERE paid = FALSE";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(finesQuery)) {
                if (rs.next()) {
                    stats.setTotalOutstandingFines(rs.getDouble(1));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting loan statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Inner class for Loan entity
     */
    public static class Loan {
        private int id;
        private int userId;
        private int bookId;
        private Date loanDate;
        private Date dueDate;
        private Date returnDate;
        private String status;
        private Timestamp createdAt;
        
        // Constructors
        public Loan() {}
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public int getBookId() { return bookId; }
        public void setBookId(int bookId) { this.bookId = bookId; }
        
        public Date getLoanDate() { return loanDate; }
        public void setLoanDate(Date loanDate) { this.loanDate = loanDate; }
        
        public Date getDueDate() { return dueDate; }
        public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
        
        public Date getReturnDate() { return returnDate; }
        public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * Inner class for Loan Details with joined information
     */
    public static class LoanDetails extends Loan {
        private String userName;
        private String bookTitle;
        private String bookAuthor;
        
        // Getters and Setters
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        
        public String getBookTitle() { return bookTitle; }
        public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
        
        public String getBookAuthor() { return bookAuthor; }
        public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }
    }
    
    /**
     * Inner class for Fine entity
     */
    public static class Fine {
        private int id;
        private int loanId;
        private int userId;
        private double amount;
        private String reason;
        private boolean paid;
        private Timestamp createdAt;
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public int getLoanId() { return loanId; }
        public void setLoanId(int loanId) { this.loanId = loanId; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }
        
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * Inner class for Loan Statistics
     */
    public static class LoanStats {
        private int totalLoans;
        private int activeLoans;
        private int overdueLoans;
        private double totalOutstandingFines;
        
        // Getters and Setters
        public int getTotalLoans() { return totalLoans; }
        public void setTotalLoans(int totalLoans) { this.totalLoans = totalLoans; }
        
        public int getActiveLoans() { return activeLoans; }
        public void setActiveLoans(int activeLoans) { this.activeLoans = activeLoans; }
        
        public int getOverdueLoans() { return overdueLoans; }
        public void setOverdueLoans(int overdueLoans) { this.overdueLoans = overdueLoans; }
        
        public double getTotalOutstandingFines() { return totalOutstandingFines; }
        public void setTotalOutstandingFines(double totalOutstandingFines) { this.totalOutstandingFines = totalOutstandingFines; }
    }
}
