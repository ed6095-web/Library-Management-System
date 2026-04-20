/**
 * Environment Configuration
 * Determines API base URL based on environment
 */

const getApiBaseUrl = () => {
  // Check if we're in production (Vercel environment)
  if (typeof window !== 'undefined' && window.location.hostname !== 'localhost') {
    // Try to get API URL from multiple sources
    // 1. First check localStorage (user set value)
    const storedUrl = localStorage.getItem('apiBaseUrl');
    if (storedUrl) {
      return storedUrl;
    }
    
    // 2. Check for injected global variable from Vercel env
    if (window.__API_BASE_URL__) {
      return window.__API_BASE_URL__;
    }
    
    // 3. Try to get from DOM data attribute (set in index.html)
    const htmlElement = document.documentElement;
    if (htmlElement && htmlElement.getAttribute('data-api-url')) {
      return htmlElement.getAttribute('data-api-url');
    }
    
    // 4. Default fallback
    console.warn('API_BASE_URL not configured. Please set it in browser console: localStorage.setItem("apiBaseUrl", "YOUR_BACKEND_URL")');
    return 'https://your-backend-url.railway.app/api'; // Replace with your backend URL
  }
  
  // Local development
  return 'http://localhost:8080/api';
};

const API_BASE_URL = getApiBaseUrl();

// Export for use in other scripts
window.API_BASE_URL = API_BASE_URL;

console.log('API Base URL configured as:', API_BASE_URL);
