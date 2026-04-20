/**
 * API Configuration Initialization
 * This script auto-detects and configures the backend API URL
 */

(function initApiConfig() {
    const hostname = window.location.hostname;
    const isProduction = hostname !== 'localhost' && hostname !== '127.0.0.1';
    
    if (isProduction) {
        console.log('🔧 Initializing production API configuration...');
        
        // Check if already set in localStorage
        const stored = localStorage.getItem('apiBaseUrl');
        if (stored) {
            window.__API_BASE_URL__ = stored;
            console.log('✓ Using stored API URL:', stored);
            return;
        }
        
        // Try to detect backend from known domains
        const possibleBackendUrls = [
            'https://library-management-system-0oiu.onrender.com',
            'https://library-management-backend.onrender.com',
            'https://your-backend.railway.app',
            'https://your-backend.render.com',
        ];
        
        console.log('🔍 Detecting backend URL...');
        
        // Try each backend URL
        for (const baseUrl of possibleBackendUrls) {
            const testUrl = baseUrl + '/api/books';
            
            // Use a timeout to avoid hanging
            const timeoutId = setTimeout(() => {
                console.log('⏱️ Timeout checking:', baseUrl);
            }, 3000);
            
            fetch(testUrl, { 
                method: 'GET',
                mode: 'cors',
                headers: { 'Content-Type': 'application/json' }
            })
                .then(response => {
                    clearTimeout(timeoutId);
                    if (response.ok || response.status === 401) {
                        // Backend found (401 means server is there, just auth failed)
                        const apiUrl = baseUrl + '/api';
                        localStorage.setItem('apiBaseUrl', apiUrl);
                        window.__API_BASE_URL__ = apiUrl;
                        console.log('✓ Backend found at:', apiUrl);
                    }
                })
                .catch(error => {
                    clearTimeout(timeoutId);
                    console.log('✗ Backend not found at:', baseUrl);
                });
        }
        
        // Set a fallback message if nothing is found
        setTimeout(() => {
            if (!window.__API_BASE_URL__) {
                console.warn('⚠️ No backend detected. To manually set backend URL, run:');
                console.warn('localStorage.setItem("apiBaseUrl", "YOUR_BACKEND_URL/api")');
                console.warn('window.location.reload();');
            }
        }, 5000);
    }
})();
