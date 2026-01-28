import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { encrypt, decrypt, getSessionId, isInitialized } from "../../security/ecdhClient";
import { secureAxios } from "../../config/axiosConfig";
import '../../index.css'; 
import SessionErrorModal from '../../components/SessionErrorModal';

const Login = ({ onLoginSuccess }) => {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [showSessionErrorModal, setShowSessionErrorModal] = useState(false);

  const baseURL = import.meta.env.VITE_SECURE_BASE_URL || import.meta.env.VITE_BASE_URL;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");

    try {
      if (isInitialized()) {
        console.log("Using secure login with encryption over HTTPS");
        const loginData = { username, password };
        const encryptedData = await encrypt(JSON.stringify(loginData));
        
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

        // --- UPDATED ERROR HANDLING LOGIC ---
        if (!response.ok) {
          const responseText = await response.text();
          let errorMessage = `Server error: ${response.status}`;

          // Attempt to decrypt the error response (since backend sends encrypted errors)
          try {
            const decryptedError = await decrypt(responseText);
            const errorData = JSON.parse(decryptedError);

            if (errorData.message) {
              errorMessage = errorData.message;
            }

            if (errorData.httpsRequired) {
              setMessage("Secure connection required. Redirecting...");
              if (window.location.protocol !== 'https:') {
                setTimeout(() => {
                  window.location.href = window.location.href.replace('http:', 'https:');
                }, 1500);
              }
              return;
            }
          } catch (decryptionError) {
            // If decryption fails, try parsing as plain JSON (fallback)
            try {
              const plainError = JSON.parse(responseText);
              if (plainError.message) errorMessage = plainError.message;
            } catch (jsonError) {
              console.warn("Could not parse error response", responseText);
            }
          }
          
          throw new Error(errorMessage);
        }
        // ------------------------------------

        const encryptedResponse = await response.text();
        const decryptedResponse = await decrypt(encryptedResponse);
        const data = JSON.parse(decryptedResponse);

        if (data.authenticated || data.status === "success") {
          setMessage("Login success!");
          const userData = {
            username: data.username,
            userId: data.userId,
            role: data.role,
            email: data.email || "",
            phone: data.phone || "",
            firstName: data.firstName || "",
            lastName: data.lastName || "",
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
      } else {
        // Fallback for non-secure mode (if applicable)
        const response = await secureAxios.post('/api/auth/login', { username, password });
        if (response.data.authenticated || response.data.status === "success") {
          setMessage("Login success!");
          const userData = {
            username: response.data.username,
            userId: response.data.userId,
            role: response.data.role,
            email: response.data.email || "",
            phone: response.data.phone || "",
            firstName: response.data.firstName || "",
            lastName: response.data.lastName || "",
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
      }
    } catch (error) {
      console.error("Login error:", error);
      if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
        setMessage("Connection error. Please check your connection and certificate.");
        window.dispatchEvent(new CustomEvent('certificate-error', { detail: { url: baseURL } }));
      } else if (error.message.includes('session') || error.message.includes('atob') || error.message.includes('decode')) {
        setMessage("Session error. Please refresh the page.");
        setShowSessionErrorModal(true);
      } else {
        setMessage(error.message);
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
    <div className="login-page-wrapper">
      <header className="login-site-header">
        <div className="login-logo-container">
        </div>
        <button className="login-go-back-btn" onClick={handleGoBack}>
          Go Back
        </button>
      </header>
      <div className="login-content-wrapper">
        <div className="login-card">
           <img src="/Untitled.png" alt="Logo" className="login-card-logo" />
          <h1 className="login-card-title">Log In</h1>
          <form className="login-form" onSubmit={handleSubmit}>
            <div className="input-group">
              <input
                type="text"
                id="username"
                name="username"
                placeholder="Username or Email" 
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>
            <div className="input-group">
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
            <a href="#!" className="forgot-password-link" onClick={handleResetPasswordClick}>
              Forgot Password?
            </a>
            {message && <div className="login-message">{message}</div>}
            <button type="submit" className="login-submit-btn">
              Log in
            </button>
          </form>
          <p className="signup-text">
            Don't have an account? <a href="/ eslint-disable-next-line " className="signup-link" onClick={handleSignUpClick}>Sign up here</a>.
          </p>
        </div>
      </div>
      <SessionErrorModal
        isOpen={showSessionErrorModal}
        onClose={() => setShowSessionErrorModal(false)}
      />
    </div>
  );
};

export default Login;