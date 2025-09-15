/**
 * Enhanced ECDH client for secure key exchange with TLS-enabled backend
 * Updated with HTTPS support and certificate handling
 */
const crypto = window.crypto.subtle;

// Use HTTPS for security endpoints
const getSecureBaseURL = () => {
  const environment = process.env.REACT_APP_ENVIRONMENT;
  
  if (environment === 'production') {
    return process.env.REACT_APP_BASE_URL;
  }
  
  // Always use HTTPS for security endpoints in development
  return process.env.REACT_APP_SECURE_BASE_URL || 'https://localhost:8443';
};

const baseURL = getSecureBaseURL();

// Utility functions for Base64 conversion
export const arrayBufferToBase64 = (buffer) => {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
};

const securityContext = {
  sessionId: null,
  sharedSecret: null,
  aesKey: null,
  initialized: false
};

export const base64ToArrayBuffer = (base64) => {
  try {
    // Make sure the base64 string is properly padded
    const paddedBase64 = base64.replace(/=+$/, '');
    const paddingNeeded = paddedBase64.length % 4;
    const correctlyPaddedBase64 = paddingNeeded > 0 
      ? paddedBase64 + '='.repeat(4 - paddingNeeded) 
      : paddedBase64;
    
    // Decode the base64 string
    const binaryString = atob(correctlyPaddedBase64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
  } catch (error) {
    console.error('Base64 decoding error:', error);
    console.error('Problematic base64 string:', base64);
    throw error;
  }
};

/**
 * Creates a client signature for authentication
 * Uses HMAC-SHA256 to sign a timestamp with the client secret
 */
const createClientSignature = async (timestamp) => {
  try {
    // Get the client ID and secret from window (set by auth-setup.js)
    const clientId = window.CLIENT_ID || 'default-client-id';
    
    // Generate a signature using the current timestamp
    const encoder = new TextEncoder();
    const data = encoder.encode(`${clientId}:${timestamp}`);
    
    // Use a static key for development purposes
    // In production, this should be securely managed
    const devKey = encoder.encode('street-med-client-authentication-key');
    
    // Import the key for HMAC signing
    const key = await crypto.importKey(
      'raw',
      devKey,
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    );
    
    // Create the signature
    const signature = await crypto.sign('HMAC', key, data);
    
    // Return as Base64
    return arrayBufferToBase64(signature);
  } catch (error) {
    console.error('Error creating client signature:', error);
    return null;
  }
};

/**
 * Handle certificate errors in development
 */
const handleCertificateError = (error) => {
  if (process.env.REACT_APP_ENVIRONMENT === 'development' && 
      process.env.REACT_APP_ALLOW_SELF_SIGNED_CERT === 'true') {
    
    console.warn('Self-signed certificate detected. Opening certificate acceptance page...');
    
    // Dispatch event to show certificate helper
    window.dispatchEvent(new CustomEvent('certificate-error', { 
      detail: { 
        url: baseURL,
        error: error.message 
      }
    }));
    
    return {
      success: false,
      error: 'Please accept the self-signed certificate and try again',
      requiresCertAcceptance: true
    };
  }
  
  throw error;
};

/**
 * Performs the ECDH key exchange with the TLS-enabled backend
 */
export const performKeyExchange = async () => {
  console.log('Starting ECDH key exchange with TLS-enabled backend...');
  console.log('Using base URL:', baseURL);
  
  try {
    // Step 1: Generate client key pair
    console.log('Generating client key pair...');
    const keyPair = await crypto.generateKey(
      {
        name: 'ECDH',
        namedCurve: 'P-256',
      },
      true,
      ['deriveKey', 'deriveBits']
    );
    
    // Step 2: Prepare client authentication
    const timestamp = Date.now().toString();
    const clientId = window.CLIENT_ID || 'default-client-id';
    const signature = await createClientSignature(timestamp);
    
    console.log('Prepared client authentication');
    
    // Step 3: Initiate handshake with server over HTTPS
    console.log('Requesting server public key over HTTPS...');
    
    // Prepare headers with client authentication
    const headers = {
      'X-Client-ID': clientId,
      'X-Timestamp': timestamp,
      'Origin': window.location.origin
    };
    
    // Add signature if available
    if (signature) {
      headers['X-Signature'] = signature;
      console.log('Added client authentication signature');
    } else {
      console.warn('Client authentication signature unavailable');
    }
    
    try {
      const response = await fetch(`${baseURL}/api/security/initiate-handshake`, {
        method: 'GET',
        headers: headers,
        credentials: 'include',
        mode: 'cors'
      });
      
      console.log('Handshake response status:', response.status);
      
      if (!response.ok) {
        // Check if it's a certificate error
        if (response.status === 0 || response.type === 'opaque') {
          return handleCertificateError(new Error('Certificate validation failed'));
        }
        throw new Error(`Server handshake failed: ${response.status}`);
      }
      
      const data = await response.json();
      if (!data || !data.sessionId || !data.serverPublicKey) {
        throw new Error('Invalid response format from server');
      }
      
      const sessionId = data.sessionId;
      const serverPublicKeyBase64 = data.serverPublicKey;
      
      console.log(`Received session ID: ${sessionId}`);
      console.log('Received server public key');
      
      // Step 4: Import server's public key
      const serverPublicKeyBytes = base64ToArrayBuffer(serverPublicKeyBase64);
      const serverPublicKey = await crypto.importKey(
        'spki',
        serverPublicKeyBytes,
        {
          name: 'ECDH',
          namedCurve: 'P-256',
        },
        true,
        []
      );
      
      // Step 5: Export client's public key
      const clientPublicKeyBytes = await crypto.exportKey('spki', keyPair.publicKey);
      const clientPublicKeyBase64 = arrayBufferToBase64(clientPublicKeyBytes);
      
      // Step 6: Complete handshake with server
      console.log('Completing handshake with server...');
      
      // Generate new timestamp and signature for this request
      const completeTimestamp = Date.now().toString();
      const completeSignature = await createClientSignature(completeTimestamp);
      
      // Include client authentication in the complete-handshake request
      const completeHeaders = {
        'Content-Type': 'application/json',
        'X-Client-ID': clientId,
        'X-Timestamp': completeTimestamp,
        'Origin': window.location.origin
      };
      
      if (completeSignature) {
        completeHeaders['X-Signature'] = completeSignature;
      }
      
      const completeResponse = await fetch(`${baseURL}/api/security/complete-handshake`, {
        method: 'POST',
        headers: completeHeaders,
        body: JSON.stringify({
          sessionId,
          clientPublicKey: clientPublicKeyBase64,
        }),
        credentials: 'include',
        mode: 'cors'
      });
      
      if (!completeResponse.ok) {
        throw new Error(`Handshake completion failed: ${completeResponse.status}`);
      }
      
      // Step 7: Derive shared secret
      console.log('Deriving shared secret...');
      const sharedSecret = await crypto.deriveBits(
        {
          name: 'ECDH',
          public: serverPublicKey,
        },
        keyPair.privateKey,
        256
      );
      
      // Convert shared secret to hex for logging
      const bytes = new Uint8Array(sharedSecret);
      const hexSharedSecret = Array.from(bytes)
        .map(b => b.toString(16).padStart(2, '0'))
        .join('');
      
      console.log('ECDH key exchange completed successfully over HTTPS!');
      console.log(`Session ID: ${sessionId}`);
      console.log(`Shared Secret (hex): ${hexSharedSecret.substring(0, 10)}...`); // Only show part for security
      
      // Initialize the AES key for encryption/decryption
      const aesKey = await initializeAESKey(sharedSecret);
      
      // Set up the security context
      securityContext.sessionId = sessionId;
      securityContext.sharedSecret = sharedSecret;
      securityContext.aesKey = aesKey;
      securityContext.initialized = true;
      
      // Store session ID in localStorage for persistence
      localStorage.setItem('ecdh_session_id', sessionId);
      
      return {
        success: true,
        sessionId,
        sharedSecret
      };
    } catch (error) {
      console.error('Fetch error during key exchange:', error);
      
      // Check for certificate errors
      if (error.message.includes('Failed to fetch') || 
          error.message.includes('NetworkError') ||
          error.message.includes('ERR_CERT_AUTHORITY_INVALID')) {
        return handleCertificateError(error);
      }
      
      throw error;
    }
  } catch (error) {
    console.error('ECDH key exchange failed:', error);
    
    // Check for certificate-related errors
    if (error.message.includes('self signed certificate') || 
        error.message.includes('certificate') ||
        error.message.includes('ERR_CERT_AUTHORITY_INVALID')) {
      return handleCertificateError(error);
    }
    
    return {
      success: false,
      error: error.message
    };
  }
};

/**
 * Initialize AES key from the shared secret
 */
export const initializeAESKey = async (sharedSecret) => {
  if (!sharedSecret) {
    throw new Error('No shared secret available');
  }
  
  // Compute SHA-256 hash of the shared secret to match the server's key derivation
  const hashBuffer = await crypto.digest('SHA-256', sharedSecret);
  
  // Import the hashed shared secret as an AES-GCM key
  const aesKey = await crypto.importKey(
    'raw',
    hashBuffer,
    {
      name: 'AES-GCM',
      length: 256
    },
    false, // non-extractable
    ['encrypt', 'decrypt']
  );
  
  return aesKey;
};

/**
 * Encrypt data using AES-GCM
 */
export const encrypt = async (data) => {
  if (!securityContext.initialized || !securityContext.aesKey) {
    throw new Error('Security context not initialized');
  }
  
  try {
    // Generate random IV
    const iv = window.crypto.getRandomValues(new Uint8Array(12));
    
    // Encrypt data
    const encodedData = new TextEncoder().encode(data);
    const encryptedData = await crypto.encrypt(
      {
        name: 'AES-GCM',
        iv
      },
      securityContext.aesKey,
      encodedData
    );
    
    // Combine IV and encrypted data
    const result = new Uint8Array(iv.byteLength + encryptedData.byteLength);
    result.set(iv, 0);
    result.set(new Uint8Array(encryptedData), iv.byteLength);
    
    // Convert to Base64
    return arrayBufferToBase64(result);
  } catch (error) {
    console.error('Encryption failed:', error);
    throw error;
  }
};

/**
 * Decrypt data using AES-GCM
 */
export const decrypt = async (encryptedData) => {
  if (!securityContext.initialized || !securityContext.aesKey) {
    throw new Error('Security context not initialized');
  }
  
  try {
    // First check if this is actually an error response in plain JSON
    // (This happens when session expires)
    try {
      const errorObj = JSON.parse(encryptedData);
      if (errorObj.status === "error" || errorObj.error) {
        // This is a plain JSON error response, not encrypted data
        throw new Error('Session expired or invalid. Please refresh the page.');
      }
    } catch (jsonError) {
      // Not valid JSON, continue with normal decryption
    }
    
    // Decode Base64
    const encryptedBytes = base64ToArrayBuffer(encryptedData);
    
    // Extract IV (first 12 bytes)
    const iv = encryptedBytes.slice(0, 12);
    
    // Extract ciphertext
    const ciphertext = encryptedBytes.slice(12);
    
    // Decrypt data
    const decryptedData = await crypto.decrypt(
      {
        name: 'AES-GCM',
        iv
      },
      securityContext.aesKey,
      ciphertext
    );
    
    // Convert to string
    return new TextDecoder().decode(decryptedData);
  } catch (error) {
    // If there's an error due to session expiration, throw a user-friendly message
    if (error.message.includes('atob') || error.message.includes('Session expired')) {
      throw new Error('Your session has expired. Please refresh the page to continue.');
    }
    console.error('Decryption failed:', error);
    throw error;
  }
};

/**
 * Get the current session ID
 */
export const getSessionId = () => {
  return securityContext.sessionId || localStorage.getItem('ecdh_session_id');
};

/**
 * Check if security is initialized
 */
export const isInitialized = () => {
  return securityContext.initialized;
};

/**
 * Restore session from localStorage
 */
export const restoreSession = () => {
  const storedSessionId = localStorage.getItem('ecdh_session_id');
  if (storedSessionId && !securityContext.initialized) {
    console.log('Found stored session ID, but keys need to be re-established');
    // In a real implementation, you might want to re-establish the session
    // For now, we'll just clear it
    localStorage.removeItem('ecdh_session_id');
  }
};

/**
 * Clear the security context
 */
export const clearSecurityContext = () => {
  securityContext.sessionId = null;
  securityContext.sharedSecret = null;
  securityContext.aesKey = null;
  securityContext.initialized = false;
  localStorage.removeItem('ecdh_session_id');
};

/**
 * Alternative initialization that bypasses ECDH (for development only)
 * Should NOT be used when TLS is enabled
 */
export const bypassKeyExchangeForDevelopment = () => {
  if (process.env.NODE_ENV !== 'development') {
    console.error('Bypass method should only be used in development!');
    return false;
  }
  
  if (process.env.REACT_APP_USE_TLS === 'true') {
    console.error('Cannot bypass key exchange when TLS is enabled!');
    return false;
  }
  
  console.warn('BYPASSING SECURITY - DO NOT USE IN PRODUCTION');
  securityContext.sessionId = 'dev-session-' + Date.now();
  securityContext.initialized = true;
  return true;
};

/**
 * Performs an encrypted API call with client authentication over HTTPS
 */
export const secureApiCall = async (url, method, data) => {
  if (!isInitialized()) {
    throw new Error('Security context not initialized. Cannot make secure API call.');
  }
  
  try {
    // Ensure we're using HTTPS
    const secureUrl = url.startsWith('http') ? url : `${baseURL}${url}`;
    if (!secureUrl.startsWith('https') && process.env.REACT_APP_ENVIRONMENT !== 'development') {
      throw new Error('Secure API calls must use HTTPS');
    }
    
    // Add client authentication
    const timestamp = Date.now().toString();
    const clientId = window.CLIENT_ID || 'default-client-id';
    const signature = await createClientSignature(timestamp);
    
    // Encrypt the request data if we have data and an encryption key
    let payload;
    if (data) {
      if (securityContext.aesKey) {
        payload = await encrypt(JSON.stringify(data));
      } else {
        // Fallback for development bypass
        payload = JSON.stringify(data);
      }
    }
    
    // Prepare headers with client authentication and session ID
    const headers = {
      'Content-Type': securityContext.aesKey ? 'text/plain' : 'application/json',
      'X-Session-ID': getSessionId(),
      'X-Client-ID': clientId,
      'X-Timestamp': timestamp,
      'Origin': window.location.origin
    };
    
    if (signature) {
      headers['X-Signature'] = signature;
    }
    
    // Make the request with the headers
    const response = await fetch(secureUrl, {
      method: method,
      headers: headers,
      body: payload,
      credentials: 'include',
      mode: 'cors'
    });
    
    if (!response.ok) {
      throw new Error(`API call failed with status: ${response.status}`);
    }
    
    // Get the response
    const responseText = await response.text();
    
    // If we have an encryption key, decrypt the response
    if (securityContext.aesKey && responseText) {
      try {
        const decryptedResponse = await decrypt(responseText);
        return JSON.parse(decryptedResponse);
      } catch (decryptError) {
        console.warn('Failed to decrypt response, trying to parse as plain JSON:', decryptError);
        return JSON.parse(responseText);
      }
    } else {
      // For development bypass or empty responses
      if (!responseText) return {};
      return JSON.parse(responseText);
    }
  } catch (error) {
    console.error('Secure API call failed:', error);
    
    // Handle certificate errors
    if (error.message.includes('Failed to fetch') || 
        error.message.includes('NetworkError')) {
      return handleCertificateError(error);
    }
    
    throw error;
  }
};

// Initialize session restoration on module load
restoreSession();