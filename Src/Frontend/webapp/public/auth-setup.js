(function() {
  function setupClientAuth() {
    try {
      // Check if running in local development
      const isLocalDev = window.location.hostname === 'localhost' || 
                         window.location.hostname.includes('127.0.0.1');
      
      if (isLocalDev) {
        // For local development, use a standard client identifier
        console.log('Running in development environment, using development client credentials');
        window.CLIENT_ID = 'street-med-frontend-local';
        window.CLIENT_SECRET = 'local-development-secret';
        return;
      }
      
      // For production, use environment variables
      console.log('Running in production environment, using production client credentials');
      window.CLIENT_ID = window.REACT_APP_CLIENT_ID || 'street-med-frontend-prod';
    } catch (error) {
      console.warn('Error setting up client authentication:', error);
      // Use fallback values
      window.CLIENT_ID = 'street-med-frontend-fallback';
      window.CLIENT_SECRET = 'fallback-client-secret';
    }
  }
  
  // Execute immediately
  setupClientAuth();
})();