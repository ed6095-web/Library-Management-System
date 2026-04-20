/**
 * Library Management System - Main JavaScript File
 * Handles authentication, API calls, and common functionality
 */

// Configuration - API_BASE_URL is defined in config.js
const SESSION_STORAGE_KEY = 'library_user';

// Global state
let currentUser = null;

// ============================================
// Utility Functions
// ============================================

/**
 * Make API calls to the backend
 * @param {string} endpoint - API endpoint (without base URL)
 * @param {string} method - HTTP method (GET, POST, PUT, DELETE)
 * @param {object} data - Request body data
 * @returns {Promise<object>} API response
 */
async function api(endpoint, method = 'GET', data = null) {
    const runtimeApiBaseUrl = typeof window.getApiBaseUrl === 'function'
        ? window.getApiBaseUrl()
        : (window.API_BASE_URL || API_BASE_URL || '');
    const normalizedBaseUrl = (runtimeApiBaseUrl || '').replace(/\/+$/, '');

    if (!normalizedBaseUrl) {
        throw new Error('API URL is not configured. Set it with localStorage.setItem("apiBaseUrl", "https://your-backend-url/api") and reload.');
    }

    const url = `${normalizedBaseUrl}${endpoint}`;
    
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include', // Include cookies for session management
    };
    
    if (data && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(data);
    }
    
    try {
        const response = await fetch(url, options);
        const isJson = (response.headers.get('content-type') || '').includes('application/json');
        const result = isJson ? await response.json() : null;
        
        if (!response.ok) {
            const fallbackMessage = `HTTP error! status: ${response.status}`;
            throw new Error((result && result.error) ? result.error : fallbackMessage);
        }
        
        return result || {};
    } catch (error) {
        console.error('API Error:', error);

        const isNetworkError = error instanceof TypeError && /fetch/i.test(error.message || '');
        if (isNetworkError) {
            throw new Error(`Failed to fetch from ${url}. Check backend URL, HTTPS/HTTP mismatch, and CORS settings.`);
        }

        throw error;
    }
}

/**
 * Show loading overlay
 */
function showLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.style.display = 'flex';
    }
}

/**
 * Hide loading overlay
 */
function hideLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.style.display = 'none';
    }
}

/**
 * Show message to user
 * @param {string} message - Message text
 * @param {string} type - Message type (success, error, info)
 */
function showMessage(message, type = 'info') {
    const container = document.getElementById('messageContainer');
    if (!container) return;
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    messageDiv.textContent = message;
    
    container.appendChild(messageDiv);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (messageDiv.parentNode) {
            messageDiv.parentNode.removeChild(messageDiv);
        }
    }, 5000);
}

/**
 * Clear all messages
 */
function clearMessages() {
    const container = document.getElementById('messageContainer');
    if (container) {
        container.innerHTML = '';
    }
}

/**
 * Format date for display
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date
 */
function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString();
}

/**
 * Format date and time for display
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date and time
 */
function formatDateTime(dateString) {
    return new Date(dateString).toLocaleString();
}

/**
 * Escape HTML to prevent XSS
 * @param {string} text - Text to escape
 * @returns {string} Escaped HTML
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Debounce function to limit API calls
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 * @returns {Function} Debounced function
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// ============================================
// Authentication Functions
// ============================================

/**
 * Initialize authentication on page load
 */
async function initAuth() {
    // Check if we're on the login page
    if (document.body.classList.contains('login-page')) {
        setupLoginPage();
        return;
    }
    
    // For other pages, check authentication status
    const isAuthenticated = await checkAuth();
    if (!isAuthenticated) {
        window.location.href = 'index.html';
        return;
    }
    
    // Setup common page elements
    setupCommonElements();
}

/**
 * Setup login page functionality
 */
function setupLoginPage() {
    const loginForm = document.getElementById('loginForm');
    const signupForm = document.getElementById('signupForm');
    
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }
    
    if (signupForm) {
        signupForm.addEventListener('submit', handleSignup);
        
        // Add event listener for role change to show/hide admin email note
        const signupRole = document.getElementById('signupRole');
        if (signupRole) {
            signupRole.addEventListener('change', function() {
                const adminEmailNote = document.getElementById('adminEmailNote');
                if (adminEmailNote) {
                    adminEmailNote.style.display = this.value === 'admin' ? 'block' : 'none';
                }
            });
        }
    }
}

/**
 * Handle login form submission
 * @param {Event} e - Form submit event
 */
async function handleLogin(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const loginData = {
        email: formData.get('email'),
        password: formData.get('password')
    };
    
    if (!loginData.email || !loginData.password) {
        showMessage('Please fill in all fields', 'error');
        return;
    }
    
    showLoading();
    clearMessages();
    
    try {
        const response = await api('/auth/login', 'POST', loginData);
        
        if (response.success) {
            // Store user data in session storage
            sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(response.user));
            currentUser = response.user;
            
            showMessage('Login successful!', 'success');
            
            // Redirect to dashboard after short delay
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 1000);
        }
    } catch (error) {
        showMessage(error.message || 'Login failed', 'error');
    } finally {
        hideLoading();
    }
}

/**
 * Handle signup form submission
 * @param {Event} e - Form submit event
 */
async function handleSignup(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const signupData = {
        name: formData.get('name'),
        email: formData.get('email'),
        password: formData.get('password'),
        role: formData.get('role') || 'user'
    };
    
    // Validation
    if (!signupData.name || !signupData.email || !signupData.password) {
        showMessage('Please fill in all fields', 'error');
        return;
    }
    
    if (signupData.password.length < 6) {
        showMessage('Password must be at least 6 characters long', 'error');
        return;
    }
    
    if (!isValidEmail(signupData.email)) {
        showMessage('Please enter a valid email address', 'error');
        return;
    }
    
    // Validate admin email domain
    if (signupData.role === 'admin' && !isValidAdminEmail(signupData.email)) {
        showMessage('Admin accounts require an email ending with @srmist.edu.in (e.g., ed6095@srmist.edu.in)', 'error');
        return;
    }
    
    showLoading();
    clearMessages();
    
    try {
        const response = await api('/auth/signup', 'POST', signupData);
        
        if (response.success) {
            showMessage('Account created successfully! Please login.', 'success');
            
            // Switch to login form after short delay
            setTimeout(() => {
                document.getElementById('signupForm').classList.remove('active');
                document.getElementById('loginForm').classList.add('active');
                
                // Pre-fill email in login form
                document.getElementById('loginEmail').value = signupData.email;
            }, 1500);
        }
    } catch (error) {
        showMessage(error.message || 'Signup failed', 'error');
    } finally {
        hideLoading();
    }
}

/**
 * Check if user is authenticated
 * @returns {Promise<boolean>} Authentication status
 */
async function checkAuth() {
    try {
        // First check session storage (most important for client-side auth)
        const storedUser = sessionStorage.getItem(SESSION_STORAGE_KEY);
        if (storedUser) {
            try {
                currentUser = JSON.parse(storedUser);
                console.log('✓ Using stored session:', currentUser.email);
                
                // In background, verify with server (non-blocking)
                // Don't fail if this check fails - trust the stored session
                api('/auth/check')
                    .then(response => {
                        if (response.authenticated) {
                            console.log('✓ Server verified session');
                        } else {
                            console.warn('⚠️ Server doesn\'t recognize session, but client has stored data');
                        }
                    })
                    .catch(error => {
                        console.warn('⚠️ Could not verify with server:', error.message);
                    });
                
                return true;
            } catch (e) {
                console.error('Error parsing stored user:', e);
                sessionStorage.removeItem(SESSION_STORAGE_KEY);
            }
        }
        
        // If no stored user, try to get session from server
        console.log('🔍 No stored session, checking with server...');
        const response = await api('/auth/check');
        
        if (response.authenticated) {
            currentUser = response.user;
            sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(response.user));
            console.log('✓ Server session verified');
            return true;
        } else {
            currentUser = null;
            sessionStorage.removeItem(SESSION_STORAGE_KEY);
            console.log('✗ Not authenticated');
            return false;
        }
    } catch (error) {
        console.error('Auth check error:', error);
        
        // Last resort: check if there's stored user data and use it
        const storedUser = sessionStorage.getItem(SESSION_STORAGE_KEY);
        if (storedUser) {
            console.warn('⚠️ Server check failed, but using stored session');
            try {
                currentUser = JSON.parse(storedUser);
                return true;
            } catch (e) {
                console.error('Error parsing stored user:', e);
            }
        }
        
        currentUser = null;
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
        return false;
    }
}

/**
 * Get current user data
 * @returns {object|null} Current user or null
 */
function getCurrentUser() {
    return currentUser;
}

/**
 * Logout user
 */
async function logout() {
    showLoading();
    
    try {
        await api('/auth/logout');
    } catch (error) {
        console.error('Logout error:', error);
    }
    
    // Clear local storage and redirect
    currentUser = null;
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    
    showMessage('Logged out successfully', 'success');
    
    setTimeout(() => {
        window.location.href = 'index.html';
    }, 1000);
}

/**
 * Validate email format
 * @param {string} email - Email to validate
 * @returns {boolean} Is valid email
 */
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * Validate admin email domain
 * @param {string} email - Email to validate
 * @returns {boolean} Is valid admin email (ends with @srmist.edu.in)
 */
function isValidAdminEmail(email) {
    return email.toLowerCase().endsWith('@srmist.edu.in');
}

// ============================================
// Common Page Setup
// ============================================

/**
 * Setup common elements on authenticated pages
 */
function setupCommonElements() {
    // Setup mobile menu toggle if it exists
    setupMobileMenu();
    
    // Setup form validation
    setupFormValidation();
    
    // Setup keyboard shortcuts
    setupKeyboardShortcuts();
}

/**
 * Setup mobile menu functionality
 */
function setupMobileMenu() {
    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.querySelector('.sidebar');
    
    if (menuToggle && sidebar) {
        menuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('mobile-open');
        });
        
        // Close menu when clicking outside
        document.addEventListener('click', (e) => {
            if (!sidebar.contains(e.target) && !menuToggle.contains(e.target)) {
                sidebar.classList.remove('mobile-open');
            }
        });
    }
}

/**
 * Setup form validation
 */
function setupFormValidation() {
    // Add real-time validation to forms
    const forms = document.querySelectorAll('form');
    
    forms.forEach(form => {
        const inputs = form.querySelectorAll('input, textarea, select');
        
        inputs.forEach(input => {
            input.addEventListener('blur', validateField);
            input.addEventListener('input', debounce(validateField, 300));
        });
    });
}

/**
 * Validate individual form field
 * @param {Event} e - Input event
 */
function validateField(e) {
    const field = e.target;
    const value = field.value.trim();
    
    // Remove existing validation classes
    field.classList.remove('valid', 'invalid');
    
    // Skip validation if field is empty and not required
    if (!value && !field.required) {
        return;
    }
    
    let isValid = true;
    
    // Required field validation
    if (field.required && !value) {
        isValid = false;
    }
    
    // Email validation
    if (field.type === 'email' && value && !isValidEmail(value)) {
        isValid = false;
    }
    
    // Password validation
    if (field.type === 'password' && value && value.length < 6) {
        isValid = false;
    }
    
    // Number validation
    if (field.type === 'number' && value) {
        const num = parseFloat(value);
        const min = parseFloat(field.min);
        const max = parseFloat(field.max);
        
        if (isNaN(num) || (min && num < min) || (max && num > max)) {
            isValid = false;
        }
    }
    
    // Apply validation class
    field.classList.add(isValid ? 'valid' : 'invalid');
}

/**
 * Setup keyboard shortcuts
 */
function setupKeyboardShortcuts() {
    document.addEventListener('keydown', (e) => {
        // Escape key closes modals
        if (e.key === 'Escape') {
            const openModals = document.querySelectorAll('.modal[style*="block"]');
            openModals.forEach(modal => {
                modal.style.display = 'none';
            });
        }
        
        // Ctrl/Cmd + K for search (if search input exists)
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            const searchInput = document.querySelector('input[type="text"][placeholder*="search"], input[type="search"]');
            if (searchInput) {
                searchInput.focus();
            }
        }
    });
}

// ============================================
// Data Management Functions
// ============================================

/**
 * Cache for API responses to reduce server load
 */
const apiCache = new Map();

/**
 * Get cached data or fetch from API
 * @param {string} key - Cache key
 * @param {Function} fetchFunction - Function to fetch data
 * @param {number} ttl - Time to live in milliseconds
 * @returns {Promise} Cached or fresh data
 */
async function getCachedData(key, fetchFunction, ttl = 300000) { // 5 minutes default TTL
    const cached = apiCache.get(key);
    
    if (cached && (Date.now() - cached.timestamp < ttl)) {
        return cached.data;
    }
    
    const data = await fetchFunction();
    apiCache.set(key, {
        data,
        timestamp: Date.now()
    });
    
    return data;
}

/**
 * Clear API cache
 * @param {string} pattern - Optional pattern to clear specific keys
 */
function clearCache(pattern = null) {
    if (pattern) {
        for (const key of apiCache.keys()) {
            if (key.includes(pattern)) {
                apiCache.delete(key);
            }
        }
    } else {
        apiCache.clear();
    }
}

// ============================================
// Error Handling
// ============================================

/**
 * Global error handler
 */
window.addEventListener('error', (e) => {
    console.error('Global error:', e.error);
    
    // Don't show error messages for script loading errors
    if (e.filename) {
        return;
    }
    
    showMessage('An unexpected error occurred. Please refresh the page.', 'error');
});

/**
 * Global unhandled promise rejection handler
 */
window.addEventListener('unhandledrejection', (e) => {
    console.error('Unhandled promise rejection:', e.reason);
    
    // Prevent default browser behavior
    e.preventDefault();
    
    // Show user-friendly error message
    showMessage('A network error occurred. Please check your connection.', 'error');
});

// ============================================
// Performance Monitoring
// ============================================

/**
 * Monitor page load performance
 */
window.addEventListener('load', () => {
    // Check if Performance API is available
    if ('performance' in window) {
        const loadTime = performance.timing.loadEventEnd - performance.timing.navigationStart;
        console.log(`Page load time: ${loadTime}ms`);
        
        // Log slow pages (>3 seconds)
        if (loadTime > 3000) {
            console.warn('Slow page load detected');
        }
    }
});

/**
 * Monitor API call performance
 */
const originalFetch = window.fetch;
window.fetch = async function(...args) {
    const startTime = performance.now();
    
    try {
        const response = await originalFetch.apply(this, args);
        const endTime = performance.now();
        const duration = endTime - startTime;
        
        // Log slow API calls (>2 seconds)
        if (duration > 2000) {
            console.warn(`Slow API call: ${args[0]} took ${duration.toFixed(2)}ms`);
        }
        
        return response;
    } catch (error) {
        const endTime = performance.now();
        const duration = endTime - startTime;
        console.error(`API call failed: ${args[0]} after ${duration.toFixed(2)}ms`, error);
        throw error;
    }
};

// ============================================
// Local Storage Management
// ============================================

/**
 * Set item in localStorage with error handling
 * @param {string} key - Storage key
 * @param {any} value - Value to store
 */
function setLocalStorage(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
        console.error('Failed to set localStorage:', error);
        
        // Fallback to sessionStorage
        try {
            sessionStorage.setItem(key, JSON.stringify(value));
        } catch (sessionError) {
            console.error('Failed to set sessionStorage:', sessionError);
        }
    }
}

/**
 * Get item from localStorage with error handling
 * @param {string} key - Storage key
 * @param {any} defaultValue - Default value if key not found
 * @returns {any} Stored value or default
 */
function getLocalStorage(key, defaultValue = null) {
    try {
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : defaultValue;
    } catch (error) {
        console.error('Failed to get localStorage:', error);
        
        // Fallback to sessionStorage
        try {
            const item = sessionStorage.getItem(key);
            return item ? JSON.parse(item) : defaultValue;
        } catch (sessionError) {
            console.error('Failed to get sessionStorage:', sessionError);
            return defaultValue;
        }
    }
}

/**
 * Remove item from localStorage
 * @param {string} key - Storage key
 */
function removeLocalStorage(key) {
    try {
        localStorage.removeItem(key);
        sessionStorage.removeItem(key);
    } catch (error) {
        console.error('Failed to remove from storage:', error);
    }
}

// ============================================
// Theme Management
// ============================================

/**
 * Apply user theme preference
 */
function applyThemePreference() {
    const savedTheme = getLocalStorage('theme_preference', 'light');
    document.documentElement.setAttribute('data-theme', savedTheme);
}

/**
 * Toggle between light and dark theme
 */
function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    
    document.documentElement.setAttribute('data-theme', newTheme);
    setLocalStorage('theme_preference', newTheme);
    
    showMessage(`Switched to ${newTheme} theme`, 'info');
}

// ============================================
// Accessibility Helpers
// ============================================

/**
 * Announce content changes to screen readers
 * @param {string} message - Message to announce
 */
function announceToScreenReader(message) {
    const announcement = document.createElement('div');
    announcement.setAttribute('aria-live', 'polite');
    announcement.setAttribute('aria-atomic', 'true');
    announcement.classList.add('sr-only');
    announcement.textContent = message;
    
    document.body.appendChild(announcement);
    
    // Remove after announcement
    setTimeout(() => {
        document.body.removeChild(announcement);
    }, 1000);
}

/**
 * Focus management for modals and dynamic content
 * @param {Element} element - Element to focus
 */
function manageFocus(element) {
    if (element && typeof element.focus === 'function') {
        // Small delay to ensure element is visible
        setTimeout(() => {
            element.focus();
        }, 100);
    }
}

// ============================================
// Initialize Application
// ============================================

// Apply theme preference on page load
document.addEventListener('DOMContentLoaded', () => {
    applyThemePreference();
});

// Export functions for use in other scripts
window.librarySystem = {
    // Core functions
    api,
    showLoading,
    hideLoading,
    showMessage,
    clearMessages,
    
    // Authentication
    initAuth,
    checkAuth,
    getCurrentUser,
    logout,
    
    // Utilities
    formatDate,
    formatDateTime,
    escapeHtml,
    debounce,
    
    // Storage
    setLocalStorage,
    getLocalStorage,
    removeLocalStorage,
    
    // Cache
    getCachedData,
    clearCache,
    
    // Accessibility
    announceToScreenReader,
    manageFocus,
    
    // Theme
    toggleTheme
};

// Auto-initialize on pages that need it
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAuth);
} else {
    initAuth();
}
