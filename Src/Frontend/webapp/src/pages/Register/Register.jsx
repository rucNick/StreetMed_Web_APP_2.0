import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  performKeyExchange,
  encrypt,
  decrypt,
  getSessionId,
  isInitialized
} from '../../security/ecdhClient';
import { secureAxios } from '../../config/axiosConfig';
import '../../index.css'; 
import SessionErrorModal from '../../components/SessionErrorModal';

const Register = () => {
  const navigate = useNavigate();
  const [securityInitialized, setSecurityInitialized] = useState(false);
  const baseURL = import.meta.env.VITE_SECURE_BASE_URL || import.meta.env.VITE_BASE_URL;
  const [showSessionErrorModal, setShowSessionErrorModal] = useState(false);

  // --- 1. Embedded CSS for Enhanced Mobile View ---
  const embeddedCss = `
    /* Card Container */
    .signup-card-enhanced {
      background-color: rgba(255, 255, 255, 0.2);
      backdrop-filter: blur(10px);
      -webkit-backdrop-filter: blur(10px);
      border-radius: 25px;
      box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.3);
      width: 100%;
      max-width: 500px;
      padding: 40px;
      text-align: center;
      margin: 0 auto;
    }

    /* Input Groups */
    .input-group-enhanced {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 20px;
      width: 100%;
      text-align: left;
    }

    .input-label-enhanced {
      color: #ffffff;
      font-size: 18px;
      font-weight: 600;
      margin-left: 4px;
    }

    /* Inputs */
    .input-enhanced {
      width: 100%;
      padding: 14px 18px;
      font-size: 18px;
      border-radius: 25px;
      border: 1px solid rgba(255, 255, 255, 0.5); /* Softer border */
      background-color: rgba(255, 255, 255, 0.95);
      color: #333;
      box-sizing: border-box; /* Critical for alignment */
      transition: all 0.2s ease;
    }

    .input-enhanced:focus {
      outline: none;
      border-color: #003e7e;
      box-shadow: 0 0 0 3px rgba(0, 62, 126, 0.2);
    }

    /* Name Row (Split Fields) */
    .name-row-enhanced {
      display: flex;
      gap: 12px;
      width: 100%;
    }

    .input-half {
      flex: 1; /* Forces equal width */
      min-width: 0; /* CSS trick to prevent overflow in flex containers */
    }

    /* Back Button */
    .back-step-btn-enhanced {
      background: transparent;
      border: 2px solid rgba(255, 255, 255, 0.5);
      color: #ffffff;
      padding: 12px 24px;
      border-radius: 25px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      margin-top: 10px;
      transition: all 0.3s ease;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .back-step-btn-enhanced:hover {
      background-color: rgba(255, 255, 255, 0.15);
      border-color: #ffffff;
      transform: translateY(-2px);
    }

    /* --- MOBILE OPTIMIZATIONS --- */
    @media (max-width: 480px) {
      .signup-card-enhanced {
        padding: 30px 20px;
        max-width: 95%;
      }

      .signup-card-subtitle {
        font-size: 24px;
        margin-bottom: 24px;
      }

      /* Fix input font size to prevent iOS zoom */
      .input-enhanced {
        font-size: 16px; 
        padding: 12px 16px;
      }

      .input-label-enhanced {
        font-size: 16px;
      }

      /* Stack name fields on VERY small screens if needed, otherwise keep row */
      .name-row-enhanced {
        gap: 8px;
      }

      .signup-btn {
        width: 100%;
        font-size: 18px;
        padding: 12px;
      }

      /* Make back button full width for easier thumb reach */
      .back-step-btn-enhanced {
        width: 100%;
        margin-top: 16px;
      }
    }
  `;

  // --- Security Init ---
  useEffect(() => {
    const initSecurity = async () => {
      try {
        console.log("Initializing security for registration over HTTPS...");
        const result = await performKeyExchange();
        if (result.success) {
          console.log("Security initialized successfully for registration");
          setSecurityInitialized(true);
        } else if (result.requiresCertAcceptance) {
          console.warn("Certificate acceptance required");
          window.dispatchEvent(new CustomEvent('certificate-error', {
            detail: { url: baseURL }
          }));
        } else {
          console.error("Failed to initialize security:", result.error);
        }
      } catch (error) {
        console.error("Error during security initialization:", error);
        if (error.message.includes('Failed to fetch') ||
            error.message.includes('NetworkError')) {
          window.dispatchEvent(new CustomEvent('certificate-error', {
            detail: { url: baseURL }
          }));
        }
      }
    };
    initSecurity();
  }, [baseURL]);

  // --- Form State ---
  const [step, setStep] = useState(1);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  
  // Step 2 Fields
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // --- Error State ---
  const [usernameError, setUsernameError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [confirmPasswordError, setConfirmPasswordError] = useState('');
  const [emailError, setEmailError] = useState('');
  const [phoneError, setPhoneError] = useState('');
  const [nameError, setNameError] = useState('');
  const [message, setMessage] = useState('');

  // --- Validators ---
  const validateUsername = () => {
    const usernameRegex = /^[A-Za-z]+$/;
    if (!usernameRegex.test(username)) {
      setUsernameError('Usernames can only contain English letters');
      return false;
    }
    setUsernameError('');
    return true;
  };

  const validateEmail = () => {
    if (!email.trim()) {
      setEmailError(''); 
      return true;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setEmailError('The email is not formatted correctly');
      return false;
    }
    setEmailError('');
    return true;
  };

  const validatePhone = () => {
    if (!phone.trim()) {
      setPhoneError('');
      return true;
    }
    const phoneRegex = /^\d{10,11}$/;
    if (!phoneRegex.test(phone)) {
      setPhoneError('Phone number must be 10-11 digits');
      return false;
    }
    setPhoneError('');
    return true;
  };

  const validateNames = () => {
    const nameRegex = /^[A-Za-z\s]+$/;
    if (firstName.trim() && !nameRegex.test(firstName)) {
      setNameError('Names can only contain English letters');
      return false;
    }
    if (lastName.trim() && !nameRegex.test(lastName)) {
      setNameError('Names can only contain English letters');
      return false;
    }
    setNameError('');
    return true;
  };

  const validatePassword = () => {
    const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;
    if (!passwordRegex.test(password)) {
      setPasswordError('Password must be 8+ chars with letters and numbers');
      return false;
    }
    setPasswordError('');
    return true;
  };

  const validateConfirmPassword = () => {
    if (confirmPassword !== password) {
      setConfirmPasswordError('Passwords do not match');
      return false;
    }
    setConfirmPasswordError('');
    return true;
  };

  // --- Handlers ---
  const handleContinue = (e) => {
    e.preventDefault();
    if (!username.trim()) {
      setUsernameError('Username is required');
      return;
    }
    if (!validateUsername()) return;
    if (email.trim() && !validateEmail()) return;
    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');

    const isNameValid = validateNames();
    const isPhoneValid = validatePhone();
    const isPasswordValid = validatePassword();
    const isConfirmPasswordValid = validateConfirmPassword();

    if (!isNameValid || !isPhoneValid || !isPasswordValid || !isConfirmPasswordValid) {
      return;
    }

    const userData = {
      username,
      password,
      role: "CLIENT"
    };

    if (email.trim()) userData.email = email;
    if (phone.trim()) userData.phone = phone;
    if (firstName.trim()) userData.firstName = firstName;
    if (lastName.trim()) userData.lastName = lastName;

    try {
      if (securityInitialized && isInitialized()) {
        console.log("Using secure encrypted registration over HTTPS");
        const encryptedData = await encrypt(JSON.stringify(userData));
        
        const response = await fetch(`${baseURL}/api/auth/register`, {
          method: 'POST',
          headers: {
            'Content-Type': 'text/plain',
            'X-Session-ID': getSessionId()
          },
          body: encryptedData,
          credentials: 'include',
          mode: 'cors'
        });

        if (response.status === 403) {
           const responseText = await response.text();
           try {
             const errorData = JSON.parse(responseText);
             if (errorData.httpsRequired) {
               setMessage("Secure connection required. Redirecting...");
               if (window.location.protocol !== 'https:') {
                 setTimeout(() => {
                   window.location.href = window.location.href.replace('http:', 'https:');
                 }, 1500);
               }
               return;
             }
           } catch (e) {}
        }

        try {
          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (data.status === 'success') {
            setMessage('Successful registration!');
            setTimeout(() => navigate('/login'), 2000);
          } else {
             handleRegistrationError(data.message);
          }
        } catch (decryptError) {
          handleDecryptError(decryptError);
        }
      } else {
        console.log("Using secure HTTPS registration without encryption");
        const response = await secureAxios.post('/api/auth/register', userData);
        
        if (response.data.status === 'success') {
          setMessage('Successful registration!');
          setTimeout(() => navigate('/login'), 2000);
        } else {
          handleRegistrationError(response.data.message);
        }
      }
    } catch (error) {
       handleGeneralError(error);
    }
  };

  const handleRegistrationError = (msg) => {
    if (msg.includes('Username already exists')) {
      setMessage('This username is already taken.');
    } else if (msg.includes('Email already exists')) {
      setMessage('This email is already registered.');
    } else {
      setMessage(msg || 'Registration failed');
    }
  };

  const handleDecryptError = (err) => {
    console.error("Decryption error:", err);
    if (err.message.includes('atob') || err.message.includes('session')) {
      setMessage("Your session has expired. Please refresh.");
      setShowSessionErrorModal(true);
    } else {
      setMessage("Decryption failed.");
    }
  };

  const handleGeneralError = (error) => {
    console.error("Registration error:", error);
    if (error.response?.data?.message) {
      handleRegistrationError(error.response.data.message);
    } else if (error.message.includes('Failed to fetch')) {
      setMessage("Connection error. Check certificate.");
      window.dispatchEvent(new CustomEvent('certificate-error', { detail: { url: baseURL } }));
    } else {
      setMessage("Registration failed. Please try again.");
    }
  };

  const handleGoBack = () => {
    navigate('/');
  };

  return (
    <div className="signup-page-wrapper">
      <style>{embeddedCss}</style>
      
      <div className="background-image-layer"></div>

      <header className="login-site-header">
        <div className="login-logo-container"></div>
        <button className="go-back-btn" onClick={handleGoBack}>
          Go Back
        </button>
      </header>

      <div className="signup-container">
        <div className="signup-card-enhanced">

          <img src="/Untitled.png" alt="Logo" className="signup-card-logo" />
          <h2 className="signup-card-subtitle">Create Your Account</h2>

          {/* --- STEP 1 --- */}
          {step === 1 && (
            <form onSubmit={handleContinue} style={{display: 'flex', flexDirection: 'column'}}>
              <div className="input-group-enhanced">
                <input
                  type="text"
                  placeholder="Username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="input-enhanced"
                  required
                />
                {usernameError && <p className="error-text">{usernameError}</p>}
              </div>

              <div className="input-group-enhanced">
                <input
                  type="email"
                  placeholder="Email Address (Optional)"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="input-enhanced"
                />
                {emailError && <p className="error-text">{emailError}</p>}
              </div>

              <button type="submit" className="signup-btn" style={{marginTop: '10px'}}>
                Continue
              </button>
            </form>
          )}

          {/* --- STEP 2 --- */}
          {step === 2 && (
            <form onSubmit={handleSubmit} style={{display: 'flex', flexDirection: 'column'}}>
              
              {/* Full Name */}
              <div className="input-group-enhanced">
                <label className="input-label-enhanced">Full Name (Optional)</label>
                <div className="name-row-enhanced">
                  <div className="input-half">
                    <input
                      type="text"
                      placeholder="First Name"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      className="input-enhanced"
                    />
                  </div>
                  <div className="input-half">
                    <input
                      type="text"
                      placeholder="Last Name"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      className="input-enhanced"
                    />
                  </div>
                </div>
                {nameError && <p className="error-text">{nameError}</p>}
              </div>

              {/* Phone */}
              <div className="input-group-enhanced">
                <label className="input-label-enhanced">Phone Number (Optional)</label>
                <input
                  type="tel"
                  placeholder="Enter phone number"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="input-enhanced"
                />
                {phoneError && <p className="error-text">{phoneError}</p>}
              </div>

              {/* Password */}
              <div className="input-group-enhanced">
                <label className="input-label-enhanced">Password</label>
                <input
                  type="password"
                  placeholder="Create a password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="input-enhanced"
                  required
                />
                {passwordError && <p className="error-text">{passwordError}</p>}
              </div>

              {/* Confirm Password */}
              <div className="input-group-enhanced">
                <input
                  type="password"
                  placeholder="Re-enter password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="input-enhanced"
                  required
                />
                {confirmPasswordError && <p className="error-text">{confirmPasswordError}</p>}
              </div>

              <button type="submit" className="signup-btn" style={{marginTop: '10px'}}>
                Sign Up
              </button>
              
              <button 
                type="button" 
                onClick={() => setStep(1)}
                className="back-step-btn-enhanced"
              >
                Back to Step 1
              </button>
            </form>
          )}
          
          {message && (
            <p className={message.includes('Successful') ? 'success-text' : 'error-text'}>
                {message}
            </p>
          )}
          
          {securityInitialized ? (
            <p className="security-indicator success">🔒 Secure connection established</p>
          ) : (
            <p className="security-indicator fail">Establishing secure connection...</p>
          )}
        </div>
      </div>
      
      <SessionErrorModal 
        isOpen={showSessionErrorModal} 
        onClose={() => setShowSessionErrorModal(false)} 
      />
    </div>
  );
};

export default Register;