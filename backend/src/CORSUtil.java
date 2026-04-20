package backend.src;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * CORS utility class for handling Cross-Origin requests
 */
public class CORSUtil {
    
    // Allowed origins
    private static final String[] ALLOWED_ORIGINS = {
        "https://library-management-system-mu-one.vercel.app",
        "http://localhost:3000",
        "http://localhost:8080",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:8080"
    };
    
    /**
     * Set CORS headers for the response
     * Properly handles credentials for session management
     * @param request HTTP request (can be null for fallback cases)
     * @param response HTTP response
     */
    public static void setCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        // Get origin from request, handle null request gracefully
        String origin = (request != null) ? request.getHeader("Origin") : null;
        
        // Always check origin and set specific allowed origin (not wildcard when using credentials)
        if (origin != null && isOriginAllowed(origin)) {
            // Origin is allowed - use it specifically (required when credentials mode is 'include')
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else if (origin != null) {
            // Origin provided but not in whitelist - only allow wildcard (no credentials)
            response.setHeader("Access-Control-Allow-Origin", "*");
        } else {
            // No origin header or null request - allow all (safe for same-origin or preflight)
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        
        // Allow credentials, methods, and headers
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, X-Requested-With, X-CSRF-Token, Cookie");
        response.setHeader("Access-Control-Expose-Headers", "Content-Length, Content-Type, Set-Cookie");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }
    
    /**
     * Check if the origin is in the allowed list
     * @param origin the origin to check
     * @return true if allowed, false otherwise
     */
    private static boolean isOriginAllowed(String origin) {
        if (origin == null) {
            return false;
        }
        
        for (String allowedOrigin : ALLOWED_ORIGINS) {
            if (allowedOrigin.equalsIgnoreCase(origin)) {
                return true;
            }
        }
        return false;
    }
}
