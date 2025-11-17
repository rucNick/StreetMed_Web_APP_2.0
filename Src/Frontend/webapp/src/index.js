import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './pages/app/App';
import reportWebVitals from './reportWebVitals';
import { performKeyExchange, initializeAESKey } from './security/ecdhClient';

// Retrieve any stored authentication state and session ID
const storedUser = localStorage.getItem('auth_user');
const storedSessionId = localStorage.getItem('ecdh_session_id');

const initializeSecurity = async () => {
  let result;
  
  // Wait a short time for auth token to be fetched
  // This allows the auth-setup.js script to complete
  const isProduction = window.location.hostname !== 'localhost' && 
                      !window.location.hostname.includes('127.0.0.1');
  const useLocalAuth = process.env.REACT_APP_USE_LOCAL_AUTH === 'true';
  
  if (isProduction || useLocalAuth) {
    await new Promise(resolve => setTimeout(resolve, 300));
  }
  
  if (storedUser && storedSessionId) {
    console.log('Authenticated user found. Reinitializing encryption session...');
    // Re-perform the key exchange to generate new keys (cannot restore non-extractable keys)
    result = await performKeyExchange();
    if (result.success) {
      localStorage.setItem('ecdh_session_id', result.sessionId);
      try {
        await initializeAESKey(result.sharedSecret);
        console.log('AES encryption key reinitialized successfully');
      } catch (error) {
        console.warn('Failed to reinitialize AES key:', error);
      }
    }
  } else {
    console.log('No authenticated user. Performing fresh key exchange...');
    result = await performKeyExchange();
    if (result.success) {
      localStorage.setItem('ecdh_session_id', result.sessionId);
      try {
        await initializeAESKey(result.sharedSecret);
        console.log('AES encryption key initialized successfully');
      } catch (error) {
        console.warn('Failed to initialize AES key:', error);
      }
    }
  }
  return result;
};

const loadingOverlay = document.createElement('div');
loadingOverlay.id = 'security-loading-overlay';
loadingOverlay.style.position = 'fixed';
loadingOverlay.style.top = '0';
loadingOverlay.style.left = '0';
loadingOverlay.style.width = '100%';
loadingOverlay.style.height = '100%';
loadingOverlay.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
loadingOverlay.style.display = 'flex';
loadingOverlay.style.justifyContent = 'center';
loadingOverlay.style.alignItems = 'center';
loadingOverlay.style.zIndex = '9999';
loadingOverlay.style.fontFamily = 'Arial, sans-serif';
loadingOverlay.innerHTML = `<div style="text-align: center;">Establishing secure connection...</div>`;
document.body.appendChild(loadingOverlay);

initializeSecurity()
  .then(result => {
    document.body.removeChild(loadingOverlay);
    const root = ReactDOM.createRoot(document.getElementById('root'));
    root.render(
      <React.StrictMode>
        <App securityInitialized={result.success} />
      </React.StrictMode>
    );
    reportWebVitals();
  })
  .catch(error => {
    console.error('Fatal error during security initialization:', error);
    if(document.body.contains(loadingOverlay)) {
      document.body.removeChild(loadingOverlay);
    }
    const errorOverlay = document.createElement('div');
    errorOverlay.style.position = 'fixed';
    errorOverlay.style.top = '0';
    errorOverlay.style.left = '0';
    errorOverlay.style.width = '100%';
    errorOverlay.style.height = '100%';
    errorOverlay.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
    errorOverlay.style.display = 'flex';
    errorOverlay.style.justifyContent = 'center';
    errorOverlay.style.alignItems = 'center';
    errorOverlay.style.zIndex = '9999';
    errorOverlay.style.fontFamily = 'Arial, sans-serif';
    errorOverlay.innerHTML = `<div style="text-align: center;">Security Error: ${error.message}. <button onclick="window.location.reload()">Retry</button></div>`;
    document.body.appendChild(errorOverlay);
    try {
      const root = ReactDOM.createRoot(document.getElementById('root'));
      root.render(
        <React.StrictMode>
          <App securityInitialized={false} />
        </React.StrictMode>
      );
    } catch(renderError) {
      console.error('Failed to render app after security error:', renderError);
    }
  });