package backend.src;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Book operations
 * Handles database operations related to books (CRUD, search, availability)
 */
public class BookDAO {
    
    private Connection connection;
    
    /**
     * Constructor - initializes database connection
     */
    public BookDAO() {
        try {
            this.connection = DBConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("Error initializing BookDAO: " + e.getMessage());
        }
    }
    
    /**
     * Add new book to library
     * @param title book title
     * @param author book author
     * @param category book category
     * @param isbn book ISBN
     * @param totalCopies total number of copies
     * @return true if book added successfully, false otherwise
     */
    public boolean addBook(String title, String author, String category, String isbn, int totalCopies) {
        String query = "INSERT INTO books (title, author, category, isbn, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.setString(3, category);
            stmt.setString(4, isbn);
            stmt.setInt(5, totalCopies);
            stmt.setInt(6, totalCopies); // Initially all copies are available
            
            int rowsAffected = stmt.executeUpdate();
            DBConnection.commitTransaction();
            
            if (rowsAffected > 0) {
                System.out.println("Book added successfully: " + title);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding book: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Get all books
     * @return List of all books
     */
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books ORDER BY title ASC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setCategory(rs.getString("category"));
                book.setIsbn(rs.getString("isbn"));
                book.setTotalCopies(rs.getInt("total_copies"));
                book.setAvailableCopies(rs.getInt("available_copies"));
                book.setAvailability(rs.getBoolean("availability"));
                book.setCreatedAt(rs.getTimestamp("created_at"));
                
                books.add(book);
            }
            
            System.out.println("Retrieved " + books.size() + " books from database");
            
        } catch (SQLException e) {
            System.err.println("Error retrieving all books: " + e.getMessage());
        }
        
        return books;
    }
    
    /**
     * Get book by ID
     * @param bookId book ID
     * @return Book object if found, null otherwise
     */
    public Book getBookById(int bookId) {
        String query = "SELECT * FROM books WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setCategory(rs.getString("category"));
                book.setIsbn(rs.getString("isbn"));
                book.setTotalCopies(rs.getInt("total_copies"));
                book.setAvailableCopies(rs.getInt("available_copies"));
                book.setAvailability(rs.getBoolean("availability"));
                book.setCreatedAt(rs.getTimestamp("created_at"));
                
                return book;
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving book by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search books by title, author, or category
     * @param searchTerm search term
     * @return List of matching books
     */
    public List<Book> searchBooks(String searchTerm) {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR category LIKE ? ORDER BY title ASC";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            String searchPattern = "%" + searchTerm + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setCategory(rs.getString("category"));
                book.setIsbn(rs.getString("isbn"));
                book.setTotalCopies(rs.getInt("total_copies"));
                book.setAvailableCopies(rs.getInt("available_copies"));
                book.setAvailability(rs.getBoolean("availability"));
                book.setCreatedAt(rs.getTimestamp("created_at"));
                
                books.add(book);
            }
            
            System.out.println("Found " + books.size() + " books matching: " + searchTerm);
            
        } catch (SQLException e) {
            System.err.println("Error searching books: " + e.getMessage());
        }
        
        return books;
    }
    
    /**
     * Get books by category
     * @param category book category
     * @return List of books in the category
     */
    public List<Book> getBooksByCategory(String category) {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE category = ? ORDER BY title ASC";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, category);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setCategory(rs.getString("category"));
                book.setIsbn(rs.getString("isbn"));
                book.setTotalCopies(rs.getInt("total_copies"));
                book.setAvailableCopies(rs.getInt("available_copies"));
                book.setAvailability(rs.getBoolean("availability"));
                book.setCreatedAt(rs.getTimestamp("created_at"));
                
                books.add(book);
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving books by category: " + e.getMessage());
        }
        
        return books;
    }
    
    /**
     * Update book information
     * @param bookId book ID to update
     * @param title new title
     * @param author new author
     * @param category new category
     * @param isbn new ISBN
     * @param totalCopies new total copies
     * @return true if update successful, false otherwise
     */
    public boolean updateBook(int bookId, String title, String author, String category, String isbn, int totalCopies) {
        String query = "UPDATE books SET title = ?, author = ?, category = ?, isbn = ?, total_copies = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.setString(3, category);
            stmt.setString(4, isbn);
            stmt.setInt(5, totalCopies);
            stmt.setInt(6, bookId);
            
            int rowsAffected = stmt.executeUpdate();
            DBConnection.commitTransaction();
            
            if (rowsAffected > 0) {
                System.out.println("Book updated successfully: ID " + bookId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating book: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Delete book by ID
     * @param bookId book ID to delete
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteBook(int bookId) {
        String query = "DELETE FROM books WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            
            int rowsAffected = stmt.executeUpdate();
            DBConnection.commitTransaction();
            
            if (rowsAffected > 0) {
                System.out.println("Book deleted successfully: ID " + bookId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Update book availability when borrowed or returned
     * @param bookId book ID
     * @param borrowed true if book is being borrowed, false if returned
     * @return true if update successful, false otherwise
     */
    public boolean updateBookAvailability(int bookId, boolean borrowed) {
        String query;
        if (borrowed) {
            query = "UPDATE books SET available_copies = available_copies - 1 WHERE id = ? AND available_copies > 0";
        } else {
            query = "UPDATE books SET available_copies = available_copies + 1 WHERE id = ? AND available_copies < total_copies";
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Update availability flag
                updateAvailabilityFlag(bookId);
                DBConnection.commitTransaction();
                
                System.out.println("Book availability updated: ID " + bookId + ", Borrowed: " + borrowed);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating book availability: " + e.getMessage());
            DBConnection.rollbackTransaction();
        }
        
        return false;
    }
    
    /**
     * Update availability flag based on available copies
     * @param bookId book ID
     */
    private void updateAvailabilityFlag(int bookId) {
        String query = "UPDATE books SET availability = (available_copies > 0) WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating availability flag: " + e.getMessage());
        }
    }
    
    /**
     * Check if ISBN already exists
     * @param isbn ISBN to check
     * @return true if ISBN exists, false otherwise
     */
    public boolean isbnExists(String isbn) {
        String query = "SELECT COUNT(*) FROM books WHERE isbn = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, isbn);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking ISBN existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get all distinct categories
     * @return List of categories
     */
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String query = "SELECT DISTINCT category FROM books ORDER BY category ASC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving categories: " + e.getMessage());
        }
        
        return categories;
    }
    
    /**
     * Get book statistics
     * @return BookStats object with book counts
     */
    public BookStats getBookStats() {
        BookStats stats = new BookStats();
        
        try {
            // Total books
            String totalQuery = "SELECT COUNT(*) FROM books";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(totalQuery)) {
                if (rs.next()) {
                    stats.setTotalBooks(rs.getInt(1));
                }
            }
            
            // Available books
            String availableQuery = "SELECT COUNT(*) FROM books WHERE availability = true";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(availableQuery)) {
                if (rs.next()) {
                    stats.setAvailableBooks(rs.getInt(1));
                }
            }
            
            // Total copies
            String copiesQuery = "SELECT SUM(total_copies) FROM books";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(copiesQuery)) {
                if (rs.next()) {
                    stats.setTotalCopies(rs.getInt(1));
                }
            }
            
            // Available copies
            String availableCopiesQuery = "SELECT SUM(available_copies) FROM books";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(availableCopiesQuery)) {
                if (rs.next()) {
                    stats.setAvailableCopies(rs.getInt(1));
                }
            }
            
            // Categories count
            String categoriesQuery = "SELECT COUNT(DISTINCT category) FROM books";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(categoriesQuery)) {
                if (rs.next()) {
                    stats.setTotalCategories(rs.getInt(1));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting book statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Inner class for Book entity
     */
    public static class Book {
        private int id;
        private String title;
        private String author;
        private String category;
        private String isbn;
        private int totalCopies;
        private int availableCopies;
        private boolean availability;
        private Timestamp createdAt;
        
        // Constructors
        public Book() {}
        
        public Book(String title, String author, String category, String isbn, int totalCopies) {
            this.title = title;
            this.author = author;
            this.category = category;
            this.isbn = isbn;
            this.totalCopies = totalCopies;
            this.availableCopies = totalCopies;
            this.availability = true;
        }
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
        
        public int getTotalCopies() { return totalCopies; }
        public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
        
        public int getAvailableCopies() { return availableCopies; }
        public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
        
        public boolean isAvailability() { return availability; }
        public void setAvailability(boolean availability) { this.availability = availability; }
        
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        
        @Override
        public String toString() {
            return "Book{id=" + id + ", title='" + title + "', author='" + author + "', category='" + category + "', available=" + availability + "}";
        }
    }
    
    /**
     * Inner class for Book Statistics
     */
    public static class BookStats {
        private int totalBooks;
        private int availableBooks;
        private int totalCopies;
        private int availableCopies;
        private int totalCategories;
        
        // Getters and Setters
        public int getTotalBooks() { return totalBooks; }
        public void setTotalBooks(int totalBooks) { this.totalBooks = totalBooks; }
        
        public int getAvailableBooks() { return availableBooks; }
        public void setAvailableBooks(int availableBooks) { this.availableBooks = availableBooks; }
        
        public int getTotalCopies() { return totalCopies; }
        public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
        
        public int getAvailableCopies() { return availableCopies; }
        public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
        
        public int getTotalCategories() { return totalCategories; }
        public void setTotalCategories(int totalCategories) { this.totalCategories = totalCategories; }
    }
}
