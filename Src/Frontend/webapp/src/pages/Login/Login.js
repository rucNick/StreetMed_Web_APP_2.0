import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { encrypt, decrypt, getSessionId, isInitialized } from "../../security/ecdhClient";
import { secureAxios } from "../../config/axiosConfig";
import "../../css/Login/Login.css";
import SessionErrorModal from '../../components/SessionErrorModal';

const Login = ({ onLoginSuccess }) => {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [showSessionErrorModal, setShowSessionErrorModal] = useState(false);

  const baseURL = process.env.REACT_APP_SECURE_BASE_URL || process.env.REACT_APP_BASE_URL;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage(""); // Clear any previous messages

    try {
      if (isInitialized()) {
        console.log("Using secure login with encryption over HTTPS");

        const loginData = { username, password };
        const encryptedData = await encrypt(JSON.stringify(loginData));

        // Use secureAxios for HTTPS connection
        const response = await fetch(`${baseURL}/api/auth/login`, {
          method: "POST",
          headers: {
            "Content-Type": "text/plain",
            "X-Session-ID": getSessionId(),
          },
          body: encryptedData,
          credentials: 'include',
          mode: 'cors'
        });

        // Check for network/TLS errors
        if (!response.ok) {
          if (response.status === 500) {
            setMessage("Server error. Your session may have expired.");
            setShowSessionErrorModal(true);
            return;
          } else if (response.status === 403) {
            // Check if it's an HTTPS requirement error
            const responseText = await response.text();
            try {
              const errorData = JSON.parse(responseText);
              if (errorData.httpsRequired) {
                setMessage("Secure connection required. Please ensure you're using HTTPS.");
                // Redirect to HTTPS if not already
                if (window.location.protocol !== 'https:') {
                  window.location.href = window.location.href.replace('http:', 'https:');
                }
                return;
              }
            } catch {
              // Not JSON, continue with normal error handling
            }
          }
        }

        try {
          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (data.authenticated || data.status === "success") {
            setMessage("Login success!");
            console.log("User info:", data);

            // Store user data in both localStorage and sessionStorage for security
            const userData = {
              username: data.username,
              userId: data.userId,
              role: data.role,
              authToken: data.authToken,
              volunteerSubRole: data.volunteerSubRole
            };
            
            localStorage.setItem("auth_user", JSON.stringify(userData));
            sessionStorage.setItem("auth_user", JSON.stringify(userData));

            onLoginSuccess(userData);
            navigate("/");
          } else {
            throw new Error(data.message || "Login failed");
          }
        } catch (decryptError) {
          // Handle session expiration or decryption errors
          console.error("Decryption error:", decryptError);
          if (decryptError.message.includes('atob') || 
              decryptError.message.includes('session') ||
              decryptError.message.includes('decode')) {
            setMessage("Your session has expired.");
            setShowSessionErrorModal(true);
          } else {
            throw decryptError;
          }
        }
      } else {
        // Fallback: Use secureAxios for non-encrypted login over HTTPS
        console.log("Using secure HTTPS login without encryption");
        
        try {
          const response = await secureAxios.post('/api/auth/login', {
            username,
            password
          });

          if (response.data.authenticated || response.data.status === "success") {
            setMessage("Login success!");
            
            const userData = {
              username: response.data.username,
              userId: response.data.userId,
              role: response.data.role,
              authToken: response.data.authToken,
              volunteerSubRole: response.data.volunteerSubRole
            };
            
            localStorage.setItem("auth_user", JSON.stringify(userData));
            sessionStorage.setItem("auth_user", JSON.stringify(userData));

            onLoginSuccess(userData);
            navigate("/");
          } else {
            setMessage(response.data.message || "Login failed");
          }
        } catch (axiosError) {
          if (axiosError.code === 'ERR_CERT_AUTHORITY_INVALID') {
            setMessage("Certificate error. Please accept the certificate and try again.");
            window.dispatchEvent(new CustomEvent('certificate-error', { 
              detail: { url: baseURL }
            }));
          } else {
            setMessage(axiosError.response?.data?.message || "Login failed");
          }
        }
      }
    } catch (error) {
      console.error("Login error:", error);
      
      // Check if it's a certificate or session-related error
      if (error.message.includes('Failed to fetch') || 
          error.message.includes('NetworkError')) {
        setMessage("Connection error. Please check your connection and certificate.");
        window.dispatchEvent(new CustomEvent('certificate-error', { 
          detail: { url: baseURL }
        }));
      } else if (error.message.includes('session') || 
          error.message.includes('atob') || 
          error.message.includes('decode')) {
        setMessage("Session error. Please refresh the page.");
        setShowSessionErrorModal(true);
      } else {
        setMessage("Login error: " + error.message);
      }
    }
  };

  const handleResetPasswordClick = (e) => {
    e.preventDefault();
    navigate("/reset_password");
  };

  const handleSignUpClick = (e) => {
    e.preventDefault();
    navigate("/register");
  };

  const handleGoBack = (e) => {
    e.preventDefault();
    navigate("/");
  };

  return (
    <div className="page-container">
      <header className="site-header">
        <div className="logo-container">
          <img src="/Untitled.png" alt="Site Logo" className="logo" />
          <h2 className="site-title">Street Med Go</h2>
        </div>
        <button className="go-back-btn" onClick={handleGoBack}>
          Go Back
        </button>
      </header>

      <div className="login-container">
        <div className="login-card">
          <h1 className="login-title">Login</h1>
          
          <form onSubmit={handleSubmit}>
            <div className="input-group">
              <label htmlFor="email">Email or UserName</label>
              <input
                type="text"
                id="email"
                name="email"
                placeholder="Email or UserName"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>

            <div className="input-group">
              <label htmlFor="password">Password</label>
              <input
                type="password"
                id="password"
                name="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <a href="#!" className="forgot-password" onClick={handleResetPasswordClick}>
              Forgot Password?
            </a>

            {message && (
              <div className="message" style={{ color: "red", textAlign: "center", fontSize: "0.9rem", marginBottom: "1rem" }}>
                {message}
              </div>
            )}

            <button type="submit" className="login-btn login-btn-login">
              Log in
            </button>

            <div className="divider">
              <span>or</span>
            </div>

            <button type="button" className="login-btn login-btn-signup" onClick={handleSignUpClick}>
              Sign up
            </button>
          </form>
        </div>
      </div>
      
      {/* Session Error Modal */}
      <SessionErrorModal 
        isOpen={showSessionErrorModal}
        onClose={() => setShowSessionErrorModal(false)}
      />
    </div>
  );
};

export default Login;