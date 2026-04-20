package backend.src;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.DefaultServlet;

/**
 * Main class to start the Library Management System server
 * Uses Jetty embedded server to host the servlets
 */
public class Main {
    
    private static final int SERVER_PORT = 8080;
    private static final String CONTEXT_PATH = "/api";
    
    public static void main(String[] args) {
        System.out.println("Starting Library Management System Server...");
        
        try {
            // Test database connection first
            if (!DBConnection.testConnection()) {
                System.err.println("Failed to connect to database. Please check your database configuration.");
                System.err.println("Make sure MySQL is running and the database 'library_management' exists.");
                System.exit(1);
            }
            
            // Initialize database schema and seed data
            DatabaseInitializer.initialize();
            
            // Create Jetty server
            Server server = new Server(SERVER_PORT);
            
            // Create servlet context handler
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            
            // Add servlets to context
            addServlets(context);
            
            // Add static file handler for frontend
            addStaticFileHandler(context);
            
            // Set handler to server
            server.setHandler(context);
            
            // Add shutdown hook
            addShutdownHook(server);
            
            // Start server
            server.start();
            
            System.out.println("=".repeat(60));
            System.out.println("🚀 Library Management System Server Started Successfully!");
            System.out.println("=".repeat(60));
            System.out.println("📡 Server running on: http://localhost:" + SERVER_PORT);
            System.out.println("🔗 API Base URL: http://localhost:" + SERVER_PORT + CONTEXT_PATH);
            System.out.println("📁 Frontend URL: http://localhost:" + SERVER_PORT + "/frontend/");
            System.out.println();
            System.out.println("🔐 Default Admin Login:");
            System.out.println("   Email: ed6095@srmist.edu.in");
            System.out.println("   Password: admin123");
            System.out.println();
            System.out.println("👤 Sample User Login:");
            System.out.println("   Email: eashandarsh77@gmail.com");
            System.out.println("   Password: user123");
            System.out.println();
            System.out.println("📊 Available API Endpoints:");
            System.out.println("   Authentication: " + CONTEXT_PATH + "/auth/*");
            System.out.println("   Books: " + CONTEXT_PATH + "/books/*");
            System.out.println("   Loans: " + CONTEXT_PATH + "/loans/*");
            System.out.println("   Users: " + CONTEXT_PATH + "/users/*");
            System.out.println("=".repeat(60));
            System.out.println("Press Ctrl+C to stop the server");
            
            // Keep server running
            server.join();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Add all servlets to the context handler
     */
    private static void addServlets(ServletContextHandler context) {
        // Authentication servlet
        ServletHolder authServlet = new ServletHolder(new AuthServlet());
        context.addServlet(authServlet, CONTEXT_PATH + "/auth/*");
        
        // Book servlet
        ServletHolder bookServlet = new ServletHolder(new BookServlet());
        context.addServlet(bookServlet, CONTEXT_PATH + "/books/*");
        
        // Loan servlet
        ServletHolder loanServlet = new ServletHolder(new LoanServlet());
        context.addServlet(loanServlet, CONTEXT_PATH + "/loans/*");
        
        // User servlet
        ServletHolder userServlet = new ServletHolder(new UserServlet());
        context.addServlet(userServlet, CONTEXT_PATH + "/users/*");
        
        System.out.println("✅ All servlets registered successfully");
    }
    
    /**
     * Add static file handler for serving frontend files
     */
    private static void addStaticFileHandler(ServletContextHandler context) {
        // Serve static files (HTML, CSS, JS) from frontend directory
        ServletHolder staticServlet = new ServletHolder("static", DefaultServlet.class);
        staticServlet.setInitParameter("resourceBase", "./frontend");
        staticServlet.setInitParameter("dirAllowed", "true");
        staticServlet.setInitParameter("pathInfoOnly", "true");
        context.addServlet(staticServlet, "/frontend/*");
        
        // Serve root index.html
        ServletHolder rootServlet = new ServletHolder("root", DefaultServlet.class);
        rootServlet.setInitParameter("resourceBase", "./frontend");
        rootServlet.setInitParameter("welcomeServlets", "false");
        rootServlet.setInitParameter("redirectWelcome", "true");
        rootServlet.setInitParameter("welcomeFiles", "index.html");
        context.addServlet(rootServlet, "/");
        
        System.out.println("✅ Static file handlers configured");
    }
    
    /**
     * Add shutdown hook for graceful server shutdown
     */
    private static void addShutdownHook(Server server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down Library Management System Server...");
            
            try {
                // Stop server
                server.stop();
                System.out.println("✅ Server stopped successfully");
                
                // Close database connection
                DBConnection.closeConnection();
                System.out.println("✅ Database connection closed");
                
                System.out.println("👋 Library Management System Server shutdown complete");
                
            } catch (Exception e) {
                System.err.println("❌ Error during server shutdown: " + e.getMessage());
            }
        }));
    }
    
    /**
     * Display system information and requirements
     */
    private static void displaySystemInfo() {
        System.out.println("\n📋 System Information:");
        System.out.println("   Java Version: " + System.getProperty("java.version"));
        System.out.println("   Operating System: " + System.getProperty("os.name"));
        System.out.println("   Database: MySQL (via JDBC)");
        System.out.println("   Server: Jetty Embedded");
        System.out.println();
        
        System.out.println("📋 Prerequisites:");
        System.out.println("   ✓ Java 8 or higher");
        System.out.println("   ✓ MySQL Server running");
        System.out.println("   ✓ Database 'library_management' created");
        System.out.println("   ✓ Required JAR dependencies in classpath:");
        System.out.println("     - mysql-connector-java-8.0.33.jar");
        System.out.println("     - gson-2.8.9.jar");
        System.out.println("     - jetty-server-9.4.44.jar");
        System.out.println("     - jetty-servlet-9.4.44.jar");
        System.out.println("     - servlet-api-3.1.0.jar");
    }
    
    /**
     * Validate system requirements
     */
    private static boolean validateSystemRequirements() {
        System.out.println("🔍 Validating system requirements...");
        
        // Check Java version
        String javaVersion = System.getProperty("java.version");
        System.out.println("   Java Version: " + javaVersion);
        
        // Check if required classes are available
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("   ✅ MySQL JDBC Driver found");
        } catch (ClassNotFoundException e) {
            System.err.println("   ❌ MySQL JDBC Driver not found");
            return false;
        }
        
        try {
            Class.forName("com.google.gson.Gson");
            System.out.println("   ✅ Gson library found");
        } catch (ClassNotFoundException e) {
            System.err.println("   ❌ Gson library not found");
            return false;
        }
        
        try {
            Class.forName("org.eclipse.jetty.server.Server");
            System.out.println("   ✅ Jetty server found");
        } catch (ClassNotFoundException e) {
            System.err.println("   ❌ Jetty server not found");
            return false;
        }
        
        System.out.println("✅ All system requirements validated");
        return true;
    }
    
    /**
     * Alternative main method with system validation
     */
    public static void startWithValidation(String[] args) {
        displaySystemInfo();
        
        if (!validateSystemRequirements()) {
            System.err.println("❌ System requirements not met. Please install missing dependencies.");
            System.exit(1);
        }
        
        main(args);
    }
}
