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
     * @param request HTTP request
     * @param response HTTP response
     */
    public static void setCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        
        // Check if origin is allowed
        if (origin != null && isOriginAllowed(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            // Default to allowing from any origin (permissive for public APIs)
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, X-Requested-With");
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
        for (String allowedOrigin : ALLOWED_ORIGINS) {
            if (allowedOrigin.equalsIgnoreCase(origin)) {
                return true;
            }
        }
        return false;
    }
}
