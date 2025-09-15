import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './CertificateHelper.css';

const CertificateHelper = () => {
  const [showHelper, setShowHelper] = useState(false);
  const [tlsStatus, setTlsStatus] = useState(null);
  const [isChecking, setIsChecking] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  
  useEffect(() => {
    if (process.env.REACT_APP_ENVIRONMENT === 'development' && 
        process.env.REACT_APP_USE_TLS === 'true') {
      checkTLSConnection();
    }
    
    // Listen for certificate errors from axios
    const handleCertError = (event) => {
      console.log('Certificate error event received:', event.detail);
      setShowHelper(true);
    };
    
    window.addEventListener('certificate-error', handleCertError);
    
    return () => {
      window.removeEventListener('certificate-error', handleCertError);
    };
  }, []);
  
  const checkTLSConnection = async () => {
    setIsChecking(true);
    try {
      const certCheckUrl = process.env.REACT_APP_CERT_CHECK_URL || 
                          'https://localhost:8443/api/test/tls/status';
      
      const response = await axios.get(certCheckUrl, {
        timeout: 5000,
        validateStatus: () => true // Accept any status code
      });
      
      if (response.status === 200) {
        setTlsStatus(response.data);
        setShowHelper(false);
        console.log('TLS connection successful:', response.data);
      } else {
        setShowHelper(true);
      }
    } catch (error) {
      console.error('TLS connection check failed:', error);
      
      if (error.code === 'ERR_CERT_AUTHORITY_INVALID' || 
          error.code === 'ECONNREFUSED' ||
          error.message?.includes('self signed certificate') ||
          error.message?.includes('Network Error')) {
        setShowHelper(true);
      }
    } finally {
      setIsChecking(false);
    }
  };
  
  const acceptCertificate = () => {
    const certUrl = process.env.REACT_APP_CERT_CHECK_URL || 
                   'https://localhost:8443/api/test/tls/status';
    
    // Open in new tab to accept certificate
    const newWindow = window.open(certUrl, '_blank');
    
    // Show instructions
    alert(
      'A new tab has been opened.\n\n' +
      '1. You may see a security warning - this is expected for local development\n' +
      '2. Click "Advanced" or "Show Details"\n' +
      '3. Click "Proceed to localhost (unsafe)" or "Accept the Risk and Continue"\n' +
      '4. Once you see the TLS status page, close that tab and click "Check Connection" here'
    );
    
    // Focus back on the original window
    if (newWindow) {
      setTimeout(() => {
        window.focus();
      }, 100);
    }
    
    setRetryCount(retryCount + 1);
  };
  
  const handleRecheck = () => {
    setShowHelper(false);
    setTimeout(() => {
      checkTLSConnection();
    }, 500);
  };
  
  const dismissHelper = () => {
    setShowHelper(false);
    // Store dismissal in session storage
    sessionStorage.setItem('tls-helper-dismissed', 'true');
  };
  
  // Don't show in production or if dismissed
  if (process.env.REACT_APP_ENVIRONMENT !== 'development' || 
      sessionStorage.getItem('tls-helper-dismissed') === 'true') {
    return null;
  }
  
  if (!showHelper) return null;
  
  return (
    <div className="certificate-helper">
      <div className="certificate-helper-content">
        <div className="certificate-helper-icon">⚠️</div>
        
        <div className="certificate-helper-text">
          <h3>Secure Connection Setup Required</h3>
          <p>
            Your backend is using HTTPS with a self-signed certificate. 
            This is normal for local development but requires one-time acceptance.
          </p>
          
          {tlsStatus && (
            <div className="tls-status">
              <strong>Current Status:</strong> {tlsStatus.message || 'Unknown'}
            </div>
          )}
        </div>
        
        <div className="certificate-helper-actions">
          <button 
            onClick={acceptCertificate}
            className="cert-button cert-button-primary"
            disabled={isChecking}
          >
            Accept Certificate
          </button>
          
          <button 
            onClick={handleRecheck}
            className="cert-button cert-button-secondary"
            disabled={isChecking}
          >
            {isChecking ? 'Checking...' : 'Check Connection'}
          </button>
          
          <button 
            onClick={dismissHelper}
            className="cert-button cert-button-text"
          >
            Dismiss
          </button>
        </div>
        
        {retryCount > 0 && (
          <div className="certificate-helper-hint">
            <p>
              <strong>Still seeing this message?</strong> Make sure you:
            </p>
            <ol>
              <li>Accepted the certificate in the new tab</li>
              <li>The backend server is running on port 8443</li>
              <li>Clicked "Check Connection" after accepting</li>
            </ol>
            <p>
              Alternative: Visit{' '}
              <a 
                href="https://localhost:8443" 
                target="_blank" 
                rel="noopener noreferrer"
              >
                https://localhost:8443
              </a>{' '}
              directly and accept the certificate.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default CertificateHelper;