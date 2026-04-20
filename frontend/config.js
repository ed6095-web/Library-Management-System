/**
 * Environment Configuration
 * Determines API base URL based on environment
 */

const getApiBaseUrl = () => {
  // Check if we're in production (Vercel environment)
  if (typeof window !== 'undefined' && window.location.hostname !== 'localhost') {
    // 1. Check localStorage (user set value)
    const storedUrl = localStorage.getItem('apiBaseUrl');
    if (storedUrl) {
      return storedUrl;
    }
    
    // 2. Check for injected global variable from build
    if (window.__API_BASE_URL__) {
      return window.__API_BASE_URL__;
    }
    
    // 3. Try to get from DOM data attribute
    const htmlElement = document.documentElement;
    if (htmlElement && htmlElement.getAttribute('data-api-url')) {
      return htmlElement.getAttribute('data-api-url');
    }
    
    // 4. Default - should not reach here if env var is set
    console.warn('API_BASE_URL not configured in production. Configure it via:');
    console.warn('1. Set NEXT_PUBLIC_API_URL environment variable in Vercel');
    console.warn('2. Or set in browser console: localStorage.setItem("apiBaseUrl", "YOUR_BACKEND_URL")');
    return '';
  }
  
  // Local development
  return 'http://localhost:8080/api';
};

let API_BASE_URL = getApiBaseUrl();

// Export for use in other scripts
window.API_BASE_URL = API_BASE_URL;
window.setApiBaseUrl = (url) => {
  localStorage.setItem('apiBaseUrl', url);
  window.API_BASE_URL = url;
  API_BASE_URL = url;
  location.reload();
};

console.log('API Base URL configured as:', API_BASE_URL || 'NOT SET - check environment variables');
