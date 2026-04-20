package backend.src;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Database Initializer - Creates tables and seeds sample data on startup
 */
public class DatabaseInitializer {
    
    /**
     * Initialize database schema and seed data
     */
    public static void initialize() {
        System.out.println("Initializing database schema...");
        
        try {
            Connection conn = DBConnection.getConnection();
            Statement stmt = conn.createStatement();
            
            // Create tables
            createTables(stmt);
            
            // Seed sample data
            seedSampleData(stmt);
            
            conn.commit();
            System.out.println("Database initialization completed successfully!");
            
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
            try {
                DBConnection.rollbackTransaction();
            } catch (Exception rollbackError) {
                System.err.println("Error rolling back transaction: " + rollbackError.getMessage());
            }
        }
    }
    
    /**
     * Create all required tables
     */
    private static void createTables(Statement stmt) throws SQLException {
        System.out.println("Creating tables...");
        
        // Create users table
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(100) NOT NULL," +
                "email VARCHAR(100) UNIQUE NOT NULL," +
                "password VARCHAR(255) NOT NULL," +
                "role VARCHAR(50) DEFAULT 'user'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        stmt.execute(createUsersTable);
        System.out.println("✓ Users table created");
        
        // Create books table
        String createBooksTable = "CREATE TABLE IF NOT EXISTS books (" +
                "id SERIAL PRIMARY KEY," +
                "title VARCHAR(200) NOT NULL," +
                "author VARCHAR(100) NOT NULL," +
                "category VARCHAR(50) NOT NULL," +
                "isbn VARCHAR(20) UNIQUE," +
                "availability BOOLEAN DEFAULT TRUE," +
                "total_copies INT DEFAULT 1," +
                "available_copies INT DEFAULT 1," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        stmt.execute(createBooksTable);
        System.out.println("✓ Books table created");
        
        // Create loans table
        String createLoansTable = "CREATE TABLE IF NOT EXISTS loans (" +
                "id SERIAL PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "book_id INT NOT NULL," +
                "loan_date DATE NOT NULL," +
                "due_date DATE NOT NULL," +
                "return_date DATE," +
                "status VARCHAR(50) DEFAULT 'active'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE" +
                ")";
        stmt.execute(createLoansTable);
        System.out.println("✓ Loans table created");
        
        // Create fines table
        String createFinesTable = "CREATE TABLE IF NOT EXISTS fines (" +
                "id SERIAL PRIMARY KEY," +
                "loan_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "amount DECIMAL(10,2) NOT NULL," +
                "reason VARCHAR(255)," +
                "paid BOOLEAN DEFAULT FALSE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")";
        stmt.execute(createFinesTable);
        System.out.println("✓ Fines table created");
    }
    
    /**
     * Seed sample data into database
     */
    private static void seedSampleData(Statement stmt) throws SQLException {
        System.out.println("Seeding sample data...");
        
        // Insert sample users
        String insertUsers = "INSERT INTO users (name, email, password, role) VALUES " +
                "('Admin User', 'admin@library.com', 'admin123', 'admin')," +
                "('John Doe', 'john.doe@email.com', 'user123', 'user')," +
                "('Jane Smith', 'jane.smith@email.com', 'user123', 'user')," +
                "('Alice Johnson', 'alice.johnson@email.com', 'user123', 'user')," +
                "('Bob Wilson', 'bob.wilson@email.com', 'user123', 'user')," +
                "('Carol Brown', 'carol.brown@email.com', 'user123', 'user')," +
                "('David Lee', 'david.lee@email.com', 'user123', 'user')," +
                "('Emma Davis', 'emma.davis@email.com', 'user123', 'user') " +
                "ON CONFLICT (email) DO NOTHING";
        stmt.execute(insertUsers);
        System.out.println("✓ Sample users inserted");
        
        // Insert sample books
        String insertBooks = "INSERT INTO books (title, author, category, isbn, total_copies, available_copies) VALUES " +
                "('To Kill a Mockingbird', 'Harper Lee', 'Fiction', '978-0-06-112008-4', 3, 2)," +
                "('1984', 'George Orwell', 'Fiction', '978-0-452-28423-4', 2, 1)," +
                "('Pride and Prejudice', 'Jane Austen', 'Romance', '978-0-14-143951-8', 2, 2)," +
                "('The Great Gatsby', 'F. Scott Fitzgerald', 'Fiction', '978-0-7432-7356-5', 4, 3)," +
                "('Harry Potter and the Sorcerer''s Stone', 'J.K. Rowling', 'Fantasy', '978-0-439-70818-8', 5, 4)," +
                "('The Catcher in the Rye', 'J.D. Salinger', 'Fiction', '978-0-316-76948-0', 2, 2)," +
                "('Lord of the Rings', 'J.R.R. Tolkien', 'Fantasy', '978-0-544-00341-5', 3, 2)," +
                "('The Hobbit', 'J.R.R. Tolkien', 'Fantasy', '978-0-547-92822-7', 3, 3)," +
                "('Dune', 'Frank Herbert', 'Science Fiction', '978-0-441-17271-9', 2, 1)," +
                "('Foundation', 'Isaac Asimov', 'Science Fiction', '978-0-553-29335-0', 2, 2)," +
                "('The Hitchhiker''s Guide to the Galaxy', 'Douglas Adams', 'Science Fiction', '978-0-345-39180-3', 3, 3)," +
                "('Murder on the Orient Express', 'Agatha Christie', 'Mystery', '978-0-06-207350-4', 2, 2)," +
                "('The Da Vinci Code', 'Dan Brown', 'Mystery', '978-0-307-47427-5', 3, 2)," +
                "('Gone Girl', 'Gillian Flynn', 'Thriller', '978-0-307-58836-4', 2, 1)," +
                "('The Girl with the Dragon Tattoo', 'Stieg Larsson', 'Thriller', '978-0-307-45454-1', 2, 2)," +
                "('Sapiens', 'Yuval Noah Harari', 'Non-Fiction', '978-0-06-231609-7', 3, 3)," +
                "('Educated', 'Tara Westover', 'Biography', '978-0-399-59050-4', 2, 1)," +
                "('Becoming', 'Michelle Obama', 'Biography', '978-1-524-76313-8', 3, 2)," +
                "('The Alchemist', 'Paulo Coelho', 'Philosophy', '978-0-06-231500-7', 4, 4)," +
                "('Think and Grow Rich', 'Napoleon Hill', 'Self-Help', '978-1-585-42433-4', 2, 2) " +
                "ON CONFLICT (isbn) DO NOTHING";
        stmt.execute(insertBooks);
        System.out.println("✓ Sample books inserted");
        
        // Insert sample loans
        String insertLoans = "INSERT INTO loans (user_id, book_id, loan_date, due_date, return_date, status) VALUES " +
                "(2, 1, '2025-08-15', '2025-09-14', NULL, 'active')," +
                "(3, 2, '2025-08-20', '2025-09-19', NULL, 'active')," +
                "(4, 4, '2025-08-25', '2025-09-24', NULL, 'active')," +
                "(5, 5, '2025-08-28', '2025-09-27', NULL, 'active')," +
                "(6, 9, '2025-08-30', '2025-09-29', NULL, 'active') " +
                "ON CONFLICT DO NOTHING";
        stmt.execute(insertLoans);
        System.out.println("✓ Sample loans inserted");
    }
}
