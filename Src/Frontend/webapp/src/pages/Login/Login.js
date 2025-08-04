import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { encrypt, decrypt, getSessionId, isInitialized } from "../../security/ecdhClient";
import "../../css/Login/Login.css";
import SessionErrorModal from '../../components/SessionErrorModal';

const Login = ({ onLoginSuccess }) => {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [showSessionErrorModal, setShowSessionErrorModal] = useState(false);

  const baseURL = process.env.REACT_APP_BASE_URL;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage(""); // Clear any previous messages

    try {
      if (isInitialized()) {
        console.log("Using secure login with encryption");

        const loginData = { username, password };

        const encryptedData = await encrypt(JSON.stringify(loginData));

        const response = await fetch(`${baseURL}/api/auth/login`, {
          method: "POST",
          headers: {
            "Content-Type": "text/plain",
            "X-Session-ID": getSessionId(),
          },
          body: encryptedData,
        });

        // Add this check for network errors
        if (!response.ok) {
          if (response.status === 500) {
            // Display a user-friendly message for server errors
            setMessage("Server error. Your session may have expired.");
            setShowSessionErrorModal(true);
            return;
          }
        }

        try {
          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (data.authenticated) {
            setMessage("Login success!");
            console.log("User info:", data);

            localStorage.setItem(
              "auth_user",
              JSON.stringify({
                username: data.username,
                userId: data.userId,
                role: data.role,
              })
            );

            onLoginSuccess({
              username: data.username,
              userId: data.userId,
              role: data.role,
            });
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
      }
    } catch (error) {
      console.error("Login error:", error);
      
      // Check if it's a session-related error
      if (error.message.includes('session') || 
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