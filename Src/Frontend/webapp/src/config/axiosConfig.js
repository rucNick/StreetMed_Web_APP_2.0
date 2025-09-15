import axios from 'axios';

// Determine which base URL to use based on environment and requirements
const getBaseURL = (requireSecure = false) => {
  const environment = process.env.REACT_APP_ENVIRONMENT;
  const useTLS = process.env.REACT_APP_USE_TLS === 'true';
  
  // In production, always use HTTPS
  if (environment === 'production') {
    return process.env.REACT_APP_BASE_URL;
  }
  
  // In development, check if secure connection is required
  if (requireSecure || useTLS) {
    return process.env.REACT_APP_SECURE_BASE_URL || 'https://localhost:8443';
  }
  
  // Default to HTTP in development for non-secure endpoints
  return process.env.REACT_APP_BASE_URL_HTTP || 'http://localhost:8080';
};

// Helper to get auth headers
const getAuthHeaders = () => {
  const authData = sessionStorage.getItem('auth_user');
  if (authData) {
    try {
      const user = JSON.parse(authData);
      return {
        'Admin-Username': user.username,
        'Authentication-Status': 'true',
        'X-User-Role': user.role,
        'X-User-Id': user.userId
      };
    } catch (e) {
      console.error('Error parsing auth data:', e);
    }
  }
  return {};
};

// Create default axios instance (can use both HTTP and HTTPS)
const axiosInstance = axios.create({
  baseURL: getBaseURL(),
  timeout: parseInt(process.env.REACT_APP_REQUEST_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  withCredentials: process.env.REACT_APP_WITH_CREDENTIALS === 'true'
});

// Create secure axios instance (always uses HTTPS)
export const secureAxios = axios.create({
  baseURL: getBaseURL(true), // Force secure URL
  timeout: parseInt(process.env.REACT_APP_REQUEST_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  withCredentials: true
});

// Create public axios instance (can use HTTP in development)
export const publicAxios = axios.create({
  baseURL: getBaseURL(false),
  timeout: parseInt(process.env.REACT_APP_REQUEST_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  withCredentials: false
});

// Request interceptor for default instance
axiosInstance.interceptors.request.use(
  (config) => {
    // Add auth headers if available
    const authHeaders = getAuthHeaders();
    config.headers = { ...config.headers, ...authHeaders };
    
    // Log request in debug mode
    if (process.env.REACT_APP_DEBUG_MODE === 'true') {
      console.log('Request:', config.method?.toUpperCase(), config.url);
    }
    
    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Request interceptor for secure instance
secureAxios.interceptors.request.use(
  (config) => {
    // Add auth headers
    const authHeaders = getAuthHeaders();
    config.headers = { ...config.headers, ...authHeaders };
    
    // Ensure we're using HTTPS
    if (config.url && !config.url.startsWith('https')) {
      console.warn('Secure axios instance being used with non-HTTPS URL:', config.url);
    }
    
    if (process.env.REACT_APP_DEBUG_MODE === 'true') {
      console.log('Secure Request:', config.method?.toUpperCase(), config.url);
    }
    
    return config;
  },
  (error) => {
    console.error('Secure request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
const responseInterceptor = (response) => {
  if (process.env.REACT_APP_DEBUG_MODE === 'true') {
    console.log('Response:', response.status, response.config.url);
  }
  return response;
};

const errorInterceptor = async (error) => {
  if (process.env.REACT_APP_DEBUG_MODE === 'true') {
    console.error('Response error:', error);
  }
  
  // Handle certificate errors
  if (error.code === 'ERR_CERT_AUTHORITY_INVALID' || 
      error.message?.includes('self signed certificate')) {
    console.error('Certificate error detected. Please accept the self-signed certificate.');
    
    // Only show certificate helper in development
    if (process.env.REACT_APP_ENVIRONMENT === 'development') {
      // Dispatch custom event to show certificate helper
      window.dispatchEvent(new CustomEvent('certificate-error', { 
        detail: { url: error.config?.baseURL || error.config?.url }
      }));
    }
    
    return Promise.reject(new Error('Certificate validation failed. Please accept the certificate and try again.'));
  }
  
  // Handle connection refused (server might be down)
  if (error.code === 'ECONNREFUSED') {
    console.error('Connection refused. Is the backend server running?');
    
    // In development, try fallback from HTTPS to HTTP for non-admin endpoints
    if (process.env.REACT_APP_ENVIRONMENT === 'development' && 
        error.config?.baseURL?.includes('https://localhost:8443') &&
        !error.config?.url?.includes('/admin/')) {
      
      console.log('Attempting HTTP fallback for non-admin endpoint...');
      const httpConfig = {
        ...error.config,
        baseURL: process.env.REACT_APP_BASE_URL_HTTP || 'http://localhost:8080'
      };
      
      try {
        return await axios.request(httpConfig);
      } catch (fallbackError) {
        console.error('HTTP fallback also failed:', fallbackError);
      }
    }
    
    return Promise.reject(new Error('Cannot connect to server. Please ensure the backend is running.'));
  }
  
  // Handle 403 HTTPS required errors
  if (error.response?.status === 403 && 
      error.response?.data?.httpsRequired === true) {
    console.error('HTTPS required for this endpoint');
    
    // Retry with secure axios
    if (!error.config?.baseURL?.includes('https')) {
      console.log('Retrying with HTTPS...');
      const secureConfig = {
        ...error.config,
        baseURL: process.env.REACT_APP_SECURE_BASE_URL || 'https://localhost:8443'
      };
      
      try {
        return await secureAxios.request(secureConfig);
      } catch (retryError) {
        console.error('HTTPS retry failed:', retryError);
      }
    }
  }
  
  // Handle authentication errors
  if (error.response?.status === 401) {
    console.error('Authentication failed');
    // Could dispatch an event to trigger re-login
    window.dispatchEvent(new CustomEvent('auth-error', { detail: error.response }));
  }
  
  return Promise.reject(error);
};

// Apply response interceptors
axiosInstance.interceptors.response.use(responseInterceptor, errorInterceptor);
secureAxios.interceptors.response.use(responseInterceptor, errorInterceptor);
publicAxios.interceptors.response.use(responseInterceptor, errorInterceptor);

// Export helper functions
export const setAuthToken = (token) => {
  if (token) {
    axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    secureAxios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete axiosInstance.defaults.headers.common['Authorization'];
    delete secureAxios.defaults.headers.common['Authorization'];
  }
};

export const clearAuthToken = () => {
  delete axiosInstance.defaults.headers.common['Authorization'];
  delete secureAxios.defaults.headers.common['Authorization'];
};

// Export function to check TLS status
export const checkTLSConnection = async () => {
  try {
    const response = await secureAxios.get('/api/test/tls/status');
    return { success: true, data: response.data };
  } catch (error) {
    return { success: false, error: error.message };
  }
};

// Default export
export default axiosInstance;