/**
 * Environment Configuration
 * Determines API base URL based on environment
 */

const DEFAULT_PRODUCTION_API_URL = 'https://library-management-system-0oiu.onrender.com/api';

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

const isPrivateNetworkHost = (hostname) => {
  if (!hostname) {
    return false;
  }

  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return true;
  }

  if (/^192\.168\./.test(hostname)) {
    return true;
  }

  if (/^10\./.test(hostname)) {
    return true;
  }

  const match = hostname.match(/^172\.(\d{1,3})\./);
  if (match) {
    const secondOctet = Number(match[1]);
    return secondOctet >= 16 && secondOctet <= 31;
  }

  return false;
};

const computeApiBaseUrl = () => {
  const hostname = (typeof window !== 'undefined' && window.location && window.location.hostname) ? window.location.hostname : '';
  const origin = (typeof window !== 'undefined' && window.location && window.location.origin) ? window.location.origin : '';

  // 1. User override in localStorage
  const storedUrl = (typeof localStorage !== 'undefined') ? localStorage.getItem('apiBaseUrl') : '';
  const normalizedStoredUrl = normalizeApiUrl(storedUrl);
  if (normalizedStoredUrl) {
    return normalizedStoredUrl;
  }

  // 2. Runtime-injected URL from api-init.js
  const injectedUrl = normalizeApiUrl(window.__API_BASE_URL__ || '');
  if (injectedUrl) {
    return injectedUrl;
  }

  // 3. Optional URL from html attribute
  const htmlElement = document.documentElement;
  const domUrl = normalizeApiUrl(htmlElement ? htmlElement.getAttribute('data-api-url') : '');
  if (domUrl) {
    return domUrl;
  }

  // 4. Local or LAN testing: use same origin backend
  if (isPrivateNetworkHost(hostname)) {
    return `${origin}/api`;
  }

  // 5. Production fallback
  return DEFAULT_PRODUCTION_API_URL;
};

const getApiBaseUrl = () => computeApiBaseUrl();

let API_BASE_URL = getApiBaseUrl();

// Export for use in other scripts
window.API_BASE_URL = API_BASE_URL;
window.getApiBaseUrl = getApiBaseUrl;
window.setApiBaseUrl = (url) => {
  const normalized = normalizeApiUrl(url);
  if (!normalized) {
    console.warn('Ignoring invalid API URL. Expected a valid URL like https://your-backend.com/api');
    return;
  }

  localStorage.setItem('apiBaseUrl', normalized);
  window.API_BASE_URL = normalized;
  API_BASE_URL = normalized;
  location.reload();
};

console.log('API Base URL configured as:', API_BASE_URL || 'NOT SET - check environment variables');
