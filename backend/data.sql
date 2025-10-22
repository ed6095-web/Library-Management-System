-- Sample Data for Library Management System

USE library_management;

-- Insert sample users
INSERT INTO users (name, email, password, role) VALUES
('Admin User', 'admin@library.com', 'admin123', 'admin'),
('John Doe', 'john.doe@email.com', 'user123', 'user'),
('Jane Smith', 'jane.smith@email.com', 'user123', 'user'),
('Alice Johnson', 'alice.johnson@email.com', 'user123', 'user'),
('Bob Wilson', 'bob.wilson@email.com', 'user123', 'user'),
('Carol Brown', 'carol.brown@email.com', 'user123', 'user'),
('David Lee', 'david.lee@email.com', 'user123', 'user'),
('Emma Davis', 'emma.davis@email.com', 'user123', 'user');

-- Insert sample books
INSERT INTO books (title, author, category, isbn, total_copies, available_copies) VALUES
('To Kill a Mockingbird', 'Harper Lee', 'Fiction', '978-0-06-112008-4', 3, 2),
('1984', 'George Orwell', 'Fiction', '978-0-452-28423-4', 2, 1),
('Pride and Prejudice', 'Jane Austen', 'Romance', '978-0-14-143951-8', 2, 2),
('The Great Gatsby', 'F. Scott Fitzgerald', 'Fiction', '978-0-7432-7356-5', 4, 3),
('Harry Potter and the Sorcerer\'s Stone', 'J.K. Rowling', 'Fantasy', '978-0-439-70818-8', 5, 4),
('The Catcher in the Rye', 'J.D. Salinger', 'Fiction', '978-0-316-76948-0', 2, 2),
('Lord of the Rings', 'J.R.R. Tolkien', 'Fantasy', '978-0-544-00341-5', 3, 2),
('The Hobbit', 'J.R.R. Tolkien', 'Fantasy', '978-0-547-92822-7', 3, 3),
('Dune', 'Frank Herbert', 'Science Fiction', '978-0-441-17271-9', 2, 1),
('Foundation', 'Isaac Asimov', 'Science Fiction', '978-0-553-29335-0', 2, 2),
('The Hitchhiker\'s Guide to the Galaxy', 'Douglas Adams', 'Science Fiction', '978-0-345-39180-3', 3, 3),
('Murder on the Orient Express', 'Agatha Christie', 'Mystery', '978-0-06-207350-4', 2, 2),
('The Da Vinci Code', 'Dan Brown', 'Mystery', '978-0-307-47427-5', 3, 2),
('Gone Girl', 'Gillian Flynn', 'Thriller', '978-0-307-58836-4', 2, 1),
('The Girl with the Dragon Tattoo', 'Stieg Larsson', 'Thriller', '978-0-307-45454-1', 2, 2),
('Sapiens', 'Yuval Noah Harari', 'Non-Fiction', '978-0-06-231609-7', 3, 3),
('Educated', 'Tara Westover', 'Biography', '978-0-399-59050-4', 2, 1),
('Becoming', 'Michelle Obama', 'Biography', '978-1-524-76313-8', 3, 2),
('The Alchemist', 'Paulo Coelho', 'Philosophy', '978-0-06-231500-7', 4, 4),
('Think and Grow Rich', 'Napoleon Hill', 'Self-Help', '978-1-585-42433-4', 2, 2);

-- Insert sample loans (some active, some returned)
INSERT INTO loans (user_id, book_id, loan_date, due_date, return_date, status) VALUES
-- Active loans
(2, 1, '2025-08-15', '2025-09-14', NULL, 'active'),
(3, 2, '2025-08-20', '2025-09-19', NULL, 'active'),
(4, 4, '2025-08-25', '2025-09-24', NULL, 'active'),
(5, 5, '2025-08-28', '2025-09-27', NULL, 'active'),
(6, 9, '2025-08-30', '2025-09-29', NULL, 'active'),
-- Overdue loans
(7, 13, '2025-07-20', '2025-08-19', NULL, 'overdue'),
(8, 14, '2025-07-25', '2025-08-24', NULL, 'overdue'),
(2, 17, '2025-07-30', '2025-08-29', NULL, 'overdue'),
-- Returned loans
(3, 7, '2025-07-01', '2025-07-31', '2025-07-28', 'returned'),
(4, 8, '2025-07-05', '2025-08-04', '2025-08-01', 'returned'),
(5, 10, '2025-07-10', '2025-08-09', '2025-08-05', 'returned'),
(6, 11, '2025-07-15', '2025-08-14', '2025-08-10', 'returned'),
(7, 12, '2025-06-20', '2025-07-20', '2025-07-18', 'returned'),
(8, 15, '2025-06-25', '2025-07-25', '2025-07-20', 'returned'),
(2, 16, '2025-06-30', '2025-07-30', '2025-07-25', 'returned');

-- Insert sample fines for overdue books
INSERT INTO fines (loan_id, user_id, amount, reason, paid) VALUES
(6, 7, 12.00, 'Book overdue by 12 days - $1 per day', FALSE),
(7, 8, 7.00, 'Book overdue by 7 days - $1 per day', FALSE),
(8, 2, 2.00, 'Book overdue by 2 days - $1 per day', FALSE);

-- Update book availability based on active loans
UPDATE books SET available_copies = total_copies - (
    SELECT COUNT(*) FROM loans 
    WHERE loans.book_id = books.id AND loans.status = 'active'
);
