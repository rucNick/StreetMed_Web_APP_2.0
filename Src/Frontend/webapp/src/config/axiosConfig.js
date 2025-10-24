import axios from 'axios';

// Determine which base URL to use based on environment and requirements
const getBaseURL = (requireSecure = false) => {
  const environment = process.env.REACT_APP_ENVIRONMENT;
  
  // In production, always use HTTPS
  if (environment === 'production') {
    return process.env.REACT_APP_BASE_URL;
  }
  
  // In development, check if secure connection is required
  if (requireSecure) {
    return process.env.REACT_APP_SECURE_BASE_URL || 'https://localhost:8443';
  }
  
  // Default to HTTP in development for easier setup
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
        'X-Auth-Token': user.authToken || ''
      };
    } catch (e) {
      console.error('Error parsing auth data:', e);
    }
  }
  return {};
};

// Create default axios instance (uses HTTP in dev, HTTPS in prod)
const axiosInstance = axios.create({
  baseURL: getBaseURL(false),
  timeout: parseInt(process.env.REACT_APP_REQUEST_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  withCredentials: process.env.REACT_APP_WITH_CREDENTIALS === 'true'
});

// Create secure axios instance (for admin/sensitive endpoints)
export const secureAxios = axios.create({
  baseURL: getBaseURL(true),
  timeout: parseInt(process.env.REACT_APP_REQUEST_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  withCredentials: true
});

// Create public axios instance (always uses HTTP in dev)
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
    
    // Fix relative URLs
    if (config.url && !config.url.startsWith('http')) {
      config.url = `${config.baseURL}${config.url}`;
    }
    
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
    const authData = sessionStorage.getItem('auth_user');
    if (authData) {
      try {
        const user = JSON.parse(authData);
        
        // Add authentication headers
        if (user.username) {
          config.headers['Admin-Username'] = user.username;
          config.headers['Authentication-Status'] = 'true';
        }
        
        if (user.authToken) {
          config.headers['X-Auth-Token'] = user.authToken;
        }
      } catch (e) {
        console.error('Error setting auth headers:', e);
      }
    }
    
    // Fix relative URLs - IMPORTANT FIX
    if (config.url && !config.url.startsWith('http')) {
      // For development, route auth endpoints to HTTP unless explicitly requiring HTTPS
      const isDev = process.env.REACT_APP_ENVIRONMENT === 'development';
      const isAdminEndpoint = config.url.includes('/admin');
      
      if (isDev && !isAdminEndpoint) {
        // Use HTTP for regular auth endpoints in development
        config.url = `http://localhost:8080${config.url}`;
      } else {
        // Use HTTPS for admin or production
        config.url = `${config.baseURL}${config.url}`;
      }
    }
    
    if (process.env.REACT_APP_DEBUG_MODE === 'true') {
      console.log('Secure Request:', config.method?.toUpperCase(), config.url);
      console.log('Headers:', config.headers);
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
    
    if (process.env.REACT_APP_ENVIRONMENT === 'development') {
      // Try to open certificate acceptance page
      window.open('https://localhost:8443/api/test/tls/status', '_blank');
      
      window.dispatchEvent(new CustomEvent('certificate-error', { 
        detail: { 
          url: error.config?.baseURL || error.config?.url,
          message: 'Please accept the self-signed certificate in the new tab and try again.'
        }
      }));
    }
    
    return Promise.reject(new Error('Certificate validation failed. Please accept the certificate and try again.'));
  }
  
  // Handle connection refused
  if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
    console.error('Connection failed. Checking server availability...');
    
    // In development, try fallback from HTTPS to HTTP
    if (process.env.REACT_APP_ENVIRONMENT === 'development' && 
        error.config?.url?.includes('https://localhost:8443')) {
      
      console.log('Attempting HTTP fallback...');
      const httpUrl = error.config.url.replace('https://localhost:8443', 'http://localhost:8080');
      
      try {
        const httpConfig = {
          ...error.config,
          url: httpUrl,
          baseURL: 'http://localhost:8080'
        };
        
        // Use the default axios instance for the retry
        return await axios.request(httpConfig);
      } catch (fallbackError) {
        console.error('HTTP fallback also failed:', fallbackError);
        return Promise.reject(new Error('Server is not available on both HTTP and HTTPS ports. Please ensure the backend is running.'));
      }
    }
    
    return Promise.reject(new Error('Cannot connect to server. Please ensure the backend is running.'));
  }
  
  // Handle 403 HTTPS required errors
  if (error.response?.status === 403 && 
      error.response?.data?.httpsRequired === true) {
    console.error('HTTPS required for this endpoint');
    
    // Retry with HTTPS
    if (!error.config?.url?.includes('https')) {
      console.log('Retrying with HTTPS...');
      const httpsUrl = error.config.url.replace('http://localhost:8080', 'https://localhost:8443');
      
      try {
        const httpsConfig = {
          ...error.config,
          url: httpsUrl,
          baseURL: 'https://localhost:8443'
        };
        
        return await secureAxios.request(httpsConfig);
      } catch (retryError) {
        console.error('HTTPS retry failed:', retryError);
      }
    }
  }
  
  // Handle authentication errors
  if (error.response?.status === 401) {
    console.error('Authentication failed');
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

// Export function to check server availability
export const checkServerConnection = async () => {
  try {
    // Try HTTP first (easier in development)
    const httpResponse = await axios.get('http://localhost:8080/health', { timeout: 5000 });
    console.log('Server available on HTTP');
    return { available: true, protocol: 'http', port: 8080 };
  } catch (httpError) {
    try {
      // Try HTTPS
      const httpsResponse = await axios.get('https://localhost:8443/health', { timeout: 5000 });
      console.log('Server available on HTTPS');
      return { available: true, protocol: 'https', port: 8443 };
    } catch (httpsError) {
      console.error('Server not available on either HTTP or HTTPS');
      return { available: false, error: 'Server not running' };
    }
  }
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