/**
 * API Configuration Initialization
 * This script auto-detects and configures the backend API URL
 */

(function initApiConfig() {
    const hostname = window.location.hostname;
    const isProduction = hostname !== 'localhost' && hostname !== '127.0.0.1';
    const normalizeApiUrl = (url) => {
        if (!url || typeof url !== 'string') {
            return '';
        }

        const cleaned = url.trim().replace(/\/+$/, '');
        if (!cleaned) {
            return '';
        }

        return cleaned.endsWith('/api') ? cleaned : `${cleaned}/api`;
    };
    const applyApiUrl = (apiUrl, persist = false) => {
        const normalized = normalizeApiUrl(apiUrl);
        if (!normalized) {
            return false;
        }

        window.__API_BASE_URL__ = normalized;
        window.API_BASE_URL = normalized;

        if (persist) {
            localStorage.setItem('apiBaseUrl', normalized);
        }

        return true;
    };
    
    if (isProduction) {
        console.log('🔧 Initializing production API configuration...');

        // Try to detect backend from known domains
        const possibleBackendUrls = [
            'https://library-management-system-0oiu.onrender.com',
            'https://library-management-backend.onrender.com',
            'https://your-backend.railway.app',
            'https://your-backend.render.com',
        ];

        // Check if already set in localStorage
        const stored = normalizeApiUrl(localStorage.getItem('apiBaseUrl'));
        if (stored) {
            applyApiUrl(stored, true);
            console.log('✓ Using stored API URL:', stored);
        }

        // Apply the best known default immediately so first API call doesn't race detection.
        if (!stored) {
            applyApiUrl(possibleBackendUrls[0], false);
        }
        
        console.log('🔍 Detecting backend URL...');

        // Detect a reachable backend in the background and cache it.
        (async () => {
            const candidateBaseUrls = stored
                ? [stored.replace(/\/api$/, ''), ...possibleBackendUrls]
                : [...possibleBackendUrls];
            const seen = new Set();

            for (const baseUrl of candidateBaseUrls) {
                if (!baseUrl || seen.has(baseUrl)) {
                    continue;
                }

                seen.add(baseUrl);
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 3500);

                try {
                    const testUrl = `${baseUrl}/api/books`;
                    const response = await fetch(testUrl, {
                        method: 'GET',
                        mode: 'cors',
                        headers: { 'Content-Type': 'application/json' },
                        signal: controller.signal,
                    });

                    clearTimeout(timeoutId);

                    if (response.ok || response.status === 401) {
                        applyApiUrl(baseUrl, true);
                        console.log('✓ Backend found at:', window.__API_BASE_URL__);
                        return;
                    }

                    console.log('✗ Backend responded with unexpected status:', baseUrl, response.status);
                } catch (error) {
                    clearTimeout(timeoutId);
                    console.log('✗ Backend not found at:', baseUrl);
                }
            }

            console.warn('⚠️ No backend detected. To manually set backend URL, run:');
            console.warn('localStorage.setItem("apiBaseUrl", "YOUR_BACKEND_URL/api")');
            console.warn('window.location.reload();');
        })();
    }
})();
