/**
 * Environment Configuration
 * Determines API base URL based on environment
 */

const getApiBaseUrl = () => {
  // Check if we're in production (Vercel environment)
  if (typeof window !== 'undefined' && window.location.hostname !== 'localhost') {
    // Get the API URL from environment variable or use a default
    const apiUrl = localStorage.getItem('apiBaseUrl') || window.__API_BASE_URL__;
    
    if (apiUrl) {
      return apiUrl;
    }
    
    // Fallback: assume backend is deployed to similar domain structure
    // e.g., if frontend is on myapp.vercel.app, backend might be on myapp-backend.railway.app
    console.warn('API_BASE_URL not configured. Please set it in browser console: localStorage.setItem("apiBaseUrl", "YOUR_BACKEND_URL")');
    return 'https://your-backend-url.railway.app/api'; // Replace with your backend URL
  }
  
  // Local development
  return 'http://localhost:8080/api';
};

const API_BASE_URL = getApiBaseUrl();
