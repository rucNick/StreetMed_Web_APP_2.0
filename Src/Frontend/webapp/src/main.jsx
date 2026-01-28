import React, { useState, useEffect } from "react";
import ReactDOM from "react-dom/client";
import "./index.css";
import App from "./pages/app/App";
import { performKeyExchange, initializeAESKey } from "./security/ecdhClient";

// Inline CertificateGate component for pre-app certificate handling
const CertificateGate = ({ children }) => {
  const [status, setStatus] = useState("checking"); // 'checking' | 'needs-cert' | 'ready'
  const [showIframe, setShowIframe] = useState(false);

  const certCheckUrl =
    import.meta.env.VITE_CERT_CHECK_URL ||
    "https://localhost:8443/api/test/tls/status";
  const certTestUrl = certCheckUrl.replace("/status", "/cert-test");

  // Check if HTTPS connection works
  const checkConnection = async () => {
    // Skip check if TLS is disabled
    if (import.meta.env.VITE_USE_TLS !== "true") {
      return true;
    }

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);

      const response = await fetch(certCheckUrl, {
        method: "GET",
        signal: controller.signal,
        credentials: "include",
      });

      clearTimeout(timeoutId);
      return response.ok;
    } catch (error) {
      console.log("HTTPS check failed:", error?.message || error);
      return false;
    }
  };

  useEffect(() => {
    const init = async () => {
      // Check if already accepted this session
      if (sessionStorage.getItem("tls-cert-accepted") === "true") {
        const isConnected = await checkConnection();
        if (isConnected) {
          setStatus("ready");
          return;
        }
        // Certificate expired or server restarted
        sessionStorage.removeItem("tls-cert-accepted");
      }

      // Skip certificate check if TLS is disabled
      if (import.meta.env.VITE_USE_TLS !== "true") {
        setStatus("ready");
        return;
      }

      const isConnected = await checkConnection();
      if (isConnected) {
        sessionStorage.setItem("tls-cert-accepted", "true");
        setStatus("ready");
      } else {
        setStatus("needs-cert");
      }
    };

    init();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Poll for certificate acceptance
  useEffect(() => {
    if (!showIframe) return;

    const pollInterval = setInterval(async () => {
      const isConnected = await checkConnection();
      if (isConnected) {
        sessionStorage.setItem("tls-cert-accepted", "true");
        setShowIframe(false);
        setStatus("ready");
        clearInterval(pollInterval);
      }
    }, 1500);

    // Timeout after 2 minutes
    const timeout = setTimeout(() => {
      clearInterval(pollInterval);
    }, 120000);

    return () => {
      clearInterval(pollInterval);
      clearTimeout(timeout);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showIframe]);

  // Ready - render the app
  if (status === "ready") {
    return children;
  }

  // Checking connection
  if (status === "checking") {
    return (
      <div style={styles.overlay}>
        <div style={styles.modal}>
          <div style={styles.spinner}></div>
          <p style={styles.text}>Checking secure connection...</p>
        </div>
        <style>{spinnerCSS}</style>
      </div>
    );
  }

  // Needs certificate acceptance
  return (
    <div style={styles.overlay}>
      <div style={styles.modalLarge}>
        <div style={styles.header}>
          <span style={styles.icon}>🔐</span>
          <h2 style={styles.title}>Secure Connection Setup</h2>
        </div>

        <div style={styles.content}>
          {!showIframe ? (
            <>
              <p style={styles.description}>
                This application requires a secure HTTPS connection. Your browser needs to
                accept the development certificate to continue.
              </p>

              <div style={styles.steps}>
                <div style={styles.step}>
                  <span style={styles.stepNum}>1</span>
                  <span>Click "Accept Certificate" below</span>
                </div>
                <div style={styles.step}>
                  <span style={styles.stepNum}>2</span>
                  <span>In the frame, click "Advanced" → "Proceed to localhost"</span>
                </div>
                <div style={styles.step}>
                  <span style={styles.stepNum}>3</span>
                  <span>The app will automatically load once accepted</span>
                </div>
              </div>

              <div style={styles.actions}>
                <button style={styles.primaryBtn} onClick={() => setShowIframe(true)}>
                  Accept Certificate
                </button>
                <button style={styles.linkBtn} onClick={() => window.open(certTestUrl, "_blank")}>
                  Open in New Tab Instead
                </button>
              </div>
            </>
          ) : (
            <>
              <div style={styles.iframeInstruction}>
                <strong>👇 Accept the certificate below:</strong>
                <br />
                Click "Advanced" → "Proceed to localhost (unsafe)"
              </div>

              <div style={styles.iframeContainer}>
                <iframe
                  src={certTestUrl}
                  title="Certificate Acceptance"
                  style={styles.iframe}
                />
              </div>

              <div style={styles.iframeHelp}>
                <p style={styles.helpText}>
                  Once accepted, this page will automatically continue.
                </p>
                <div style={styles.actions}>
                  <button
                    style={styles.secondaryBtn}
                    onClick={async () => {
                      const isConnected = await checkConnection();
                      if (isConnected) {
                        sessionStorage.setItem("tls-cert-accepted", "true");
                        setStatus("ready");
                      }
                    }}
                  >
                    I've Accepted - Continue
                  </button>
                  <button style={styles.linkBtn} onClick={() => window.open(certTestUrl, "_blank")}>
                    Open in New Tab
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
      <style>{spinnerCSS}</style>
    </div>
  );
};

// Styles
const styles = {
  overlay: {
    position: "fixed",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    padding: "20px",
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif",
  },
  modal: {
    background: "white",
    borderRadius: "16px",
    padding: "40px",
    textAlign: "center",
    boxShadow: "0 20px 60px rgba(0, 0, 0, 0.3)",
    maxWidth: "400px",
    width: "100%",
  },
  modalLarge: {
    background: "white",
    borderRadius: "16px",
    boxShadow: "0 20px 60px rgba(0, 0, 0, 0.3)",
    maxWidth: "650px",
    width: "100%",
    maxHeight: "90vh",
    overflow: "hidden",
    display: "flex",
    flexDirection: "column",
  },
  header: {
    background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
    color: "white",
    padding: "24px 30px",
    display: "flex",
    alignItems: "center",
    gap: "16px",
  },
  icon: { fontSize: "32px" },
  title: { margin: 0, fontSize: "22px", fontWeight: 600 },
  content: { padding: "30px", overflowY: "auto" },
  description: {
    color: "#555",
    fontSize: "15px",
    lineHeight: 1.6,
    margin: "0 0 24px 0",
  },
  steps: {
    background: "#f8f9fa",
    borderRadius: "12px",
    padding: "16px 20px",
    marginBottom: "24px",
  },
  step: {
    display: "flex",
    alignItems: "center",
    gap: "14px",
    padding: "10px 0",
    borderBottom: "1px solid #e9ecef",
    fontSize: "14px",
    color: "#444",
  },
  stepNum: {
    width: "26px",
    height: "26px",
    background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
    color: "white",
    borderRadius: "50%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontWeight: 600,
    fontSize: "13px",
    flexShrink: 0,
  },
  actions: {
    display: "flex",
    gap: "12px",
    flexWrap: "wrap",
    justifyContent: "center",
  },
  primaryBtn: {
    background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
    color: "white",
    border: "none",
    padding: "14px 28px",
    borderRadius: "8px",
    fontSize: "15px",
    fontWeight: 600,
    cursor: "pointer",
    transition: "transform 0.2s, box-shadow 0.2s",
  },
  secondaryBtn: {
    background: "#e9ecef",
    color: "#495057",
    border: "none",
    padding: "12px 24px",
    borderRadius: "8px",
    fontSize: "14px",
    fontWeight: 600,
    cursor: "pointer",
  },
  linkBtn: {
    background: "transparent",
    color: "#667eea",
    border: "none",
    padding: "12px 16px",
    fontSize: "14px",
    cursor: "pointer",
    textDecoration: "underline",
  },
  iframeInstruction: {
    background: "#fff3cd",
    border: "1px solid #ffc107",
    borderRadius: "8px",
    padding: "14px 18px",
    marginBottom: "16px",
    fontSize: "14px",
    color: "#856404",
    textAlign: "center",
  },
  iframeContainer: {
    border: "2px solid #dee2e6",
    borderRadius: "8px",
    overflow: "hidden",
    marginBottom: "16px",
  },
  iframe: {
    width: "100%",
    height: "320px",
    border: "none",
    display: "block",
  },
  iframeHelp: { textAlign: "center" },
  helpText: { color: "#666", fontSize: "14px", marginBottom: "16px" },
  spinner: {
    width: "40px",
    height: "40px",
    border: "3px solid #f3f3f3",
    borderTop: "3px solid #667eea",
    borderRadius: "50%",
    animation: "spin 1s linear infinite",
    margin: "0 auto 20px",
  },
  text: { color: "#555", fontSize: "15px" },
};

const spinnerCSS = `
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

// Security initialization wrapper
const SecurityInitializer = ({ children }) => {
  const [initStatus, setInitStatus] = useState("initializing"); // 'initializing' | 'ready' | 'error'
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const initSecurity = async () => {
      try {
        const storedUser = localStorage.getItem("auth_user");
        const storedSessionId = localStorage.getItem("ecdh_session_id");

        // Small delay for auth setup
        const isProduction =
          window.location.hostname !== "localhost" &&
          !window.location.hostname.includes("127.0.0.1");

        if (isProduction || import.meta.env.VITE_USE_LOCAL_AUTH === "true") {
          await new Promise((resolve) => setTimeout(resolve, 300));
        }

        console.log(
          storedUser && storedSessionId
            ? "Authenticated user found. Reinitializing encryption session..."
            : "No authenticated user. Performing fresh key exchange..."
        );

        const result = await performKeyExchange();

        if (result.success) {
          localStorage.setItem("ecdh_session_id", result.sessionId);
          try {
            await initializeAESKey(result.sharedSecret);
            console.log("AES encryption key initialized successfully");
          } catch (error) {
            console.warn("Failed to initialize AES key:", error);
          }
          setInitStatus("ready");
        } else {
          throw new Error(result.error || "Key exchange failed");
        }
      } catch (error) {
        console.error("Security initialization error:", error);
        setErrorMessage(error?.message || String(error));
        setInitStatus("error");
      }
    };

    initSecurity();
  }, []);

  if (initStatus === "initializing") {
    return (
      <div style={styles.overlay}>
        <div style={styles.modal}>
          <div style={styles.spinner}></div>
          <p style={styles.text}>Establishing secure connection...</p>
        </div>
        <style>{spinnerCSS}</style>
      </div>
    );
  }

  if (initStatus === "error") {
    return (
      <div style={styles.overlay}>
        <div style={styles.modal}>
          <h2 style={{ ...styles.title, color: "#e74c3c", marginBottom: "16px" }}>
            Connection Error
          </h2>
          <p style={styles.text}>{errorMessage}</p>
          <div style={{ ...styles.actions, marginTop: "24px" }}>
            <button style={styles.primaryBtn} onClick={() => window.location.reload()}>
              Retry
            </button>
          </div>
        </div>
        <style>{spinnerCSS}</style>
      </div>
    );
  }

  return children;
};

// Main App wrapper
const AppWrapper = () => {
  return (
    <CertificateGate>
      <SecurityInitializer>
        <App securityInitialized={true} />
      </SecurityInitializer>
    </CertificateGate>
  );
};

// Render
ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AppWrapper />
  </React.StrictMode>
);

