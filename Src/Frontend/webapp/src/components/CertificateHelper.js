import React, { useState, useEffect, useCallback } from 'react';
import './CertificateHelper.css';

/**
 * CertificateHelper - Handles self-signed certificate acceptance inline
 * Uses an iframe to let users accept the certificate without leaving the page
 */
const CertificateHelper = ({ onCertificateAccepted }) => {
  const [status, setStatus] = useState('checking'); // 'checking' | 'needs-acceptance' | 'accepting' | 'accepted' | 'hidden'
  const [showIframe, setShowIframe] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  
  const certCheckUrl = process.env.REACT_APP_CERT_CHECK_URL || 
                       'https://localhost:8443/api/test/tls/status';
  
  const checkTLSConnection = useCallback(async () => {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      
      const response = await fetch(certCheckUrl, {
        method: 'GET',
        signal: controller.signal,
        mode: 'cors',
        credentials: 'include'
      });
      
      clearTimeout(timeoutId);
      
      if (response.ok) {
        console.log('TLS connection successful');
        setStatus('accepted');
        onCertificateAccepted?.();
        return true;
      }
    } catch (error) {
      console.log('TLS check failed:', error.message);
      // Certificate not accepted or server not reachable
      if (error.name === 'AbortError') {
        console.log('TLS check timed out');
      }
    }
    return false;
  }, [certCheckUrl, onCertificateAccepted]);
  
  useEffect(() => {
    // Only run in development with TLS enabled
    if (process.env.REACT_APP_ENVIRONMENT !== 'development' || 
        process.env.REACT_APP_USE_TLS !== 'true') {
      setStatus('hidden');
      return;
    }
    
    // Check if already dismissed this session
    if (sessionStorage.getItem('tls-cert-accepted') === 'true') {
      setStatus('hidden');
      return;
    }
    
    checkTLSConnection().then(success => {
      if (!success) {
        setStatus('needs-acceptance');
      }
    });
  }, [checkTLSConnection]);
  
  // Poll for certificate acceptance when iframe is shown
  useEffect(() => {
    if (!showIframe) return;
    
    const pollInterval = setInterval(async () => {
      const success = await checkTLSConnection();
      if (success) {
        setShowIframe(false);
        sessionStorage.setItem('tls-cert-accepted', 'true');
        clearInterval(pollInterval);
      }
    }, 2000);
    
    return () => clearInterval(pollInterval);
  }, [showIframe, checkTLSConnection]);
  
  const handleAcceptCertificate = () => {
    setStatus('accepting');
    setShowIframe(true);
    setRetryCount(prev => prev + 1);
  };
  
  const handleIframeLoad = () => {
    // The iframe loaded - certificate might be accepted now
    // We'll verify with the polling mechanism
    console.log('Certificate iframe loaded');
  };
  
  const handleDismiss = () => {
    setStatus('hidden');
    sessionStorage.setItem('tls-helper-dismissed', 'true');
  };
  
  const handleRetryCheck = async () => {
    setStatus('checking');
    const success = await checkTLSConnection();
    if (!success) {
      setStatus('needs-acceptance');
    }
  };
  
  // Don't render in production or if hidden
  if (status === 'hidden' || status === 'accepted') {
    return null;
  }
  
  return (
    <div className="certificate-helper-overlay">
      <div className="certificate-helper-modal">
        {/* Header */}
        <div className="cert-modal-header">
          <div className="cert-modal-icon">🔐</div>
          <h2>Secure Connection Setup</h2>
        </div>
        
        {/* Content based on status */}
        {status === 'checking' && (
          <div className="cert-modal-content">
            <div className="cert-spinner"></div>
            <p>Checking secure connection...</p>
          </div>
        )}
        
        {status === 'needs-acceptance' && !showIframe && (
          <div className="cert-modal-content">
            <p className="cert-description">
              This application uses HTTPS with a development certificate. 
              Your browser needs to trust this certificate to establish a secure connection.
            </p>
            
            <div className="cert-steps">
              <div className="cert-step">
                <span className="step-number">1</span>
                <span>Click the button below to load the certificate</span>
              </div>
              <div className="cert-step">
                <span className="step-number">2</span>
                <span>In the frame, click "Advanced" → "Proceed to localhost"</span>
              </div>
              <div className="cert-step">
                <span className="step-number">3</span>
                <span>The app will automatically continue once accepted</span>
              </div>
            </div>
            
            <div className="cert-modal-actions">
              <button 
                className="cert-btn cert-btn-primary"
                onClick={handleAcceptCertificate}
              >
                Accept Certificate
              </button>
              <button 
                className="cert-btn cert-btn-secondary"
                onClick={handleDismiss}
              >
                Skip (Limited Functionality)
              </button>
            </div>
          </div>
        )}
        
        {(status === 'accepting' || showIframe) && (
          <div className="cert-modal-content">
            <p className="cert-iframe-instruction">
              <strong>Accept the certificate in the frame below:</strong>
              <br />
              Click "Advanced" → "Proceed to localhost (unsafe)"
            </p>
            
            <div className="cert-iframe-container">
              <iframe
                src={certCheckUrl.replace('/status', '/cert-test')}
                title="Certificate Acceptance"
                onLoad={handleIframeLoad}
                className="cert-iframe"
              />
            </div>
            
            <div className="cert-iframe-help">
              <p>
                <strong>Don't see the security warning?</strong> The certificate may already be accepted.
              </p>
              <div className="cert-modal-actions">
                <button 
                  className="cert-btn cert-btn-primary"
                  onClick={handleRetryCheck}
                >
                  Check Connection
                </button>
                <button 
                  className="cert-btn cert-btn-link"
                  onClick={() => window.open(certCheckUrl, '_blank')}
                >
                  Open in New Tab
                </button>
              </div>
            </div>
            
            {retryCount > 1 && (
              <div className="cert-troubleshooting">
                <strong>Still having trouble?</strong>
                <ul>
                  <li>Make sure the backend is running on port 8443</li>
                  <li>Try opening <a href={certCheckUrl} target="_blank" rel="noopener noreferrer">{certCheckUrl}</a> directly</li>
                  <li>Check if another application is using port 8443</li>
                </ul>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default CertificateHelper;