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
import '../../css/Login/Register.css'; // Note: This should be a new file
import SessionErrorModal from '../../components/SessionErrorModal';

const Register = () => {
  const navigate = useNavigate();
  const [securityInitialized, setSecurityInitialized] = useState(false);
  const baseURL = process.env.REACT_APP_SECURE_BASE_URL || process.env.REACT_APP_BASE_URL;
  const [showSessionErrorModal, setShowSessionErrorModal] = useState(false);

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

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');

  const [usernameError, setUsernameError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [confirmPasswordError, setConfirmPasswordError] = useState('');
  const [emailError, setEmailError] = useState('');
  const [phoneError, setPhoneError] = useState('');
  const [message, setMessage] = useState('');

  const validateUsername = () => {
    const usernameRegex = /^[A-Za-z]+$/;
    if (!usernameRegex.test(username)) {
      setUsernameError('Usernames can only contain English letters');
      return false;
    }
    setUsernameError('');
    return true;
  };

  const validatePassword = () => {
    const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;
    if (!passwordRegex.test(password)) {
      setPasswordError('The password must be at least 8 digits long and contain both letters and numbers');
      return false;
    }
    setPasswordError('');
    return true;
  };

  const validateConfirmPassword = () => {
    if (confirmPassword !== password) {
      setConfirmPasswordError('Two different passwords');
      return false;
    }
    setConfirmPasswordError('');
    return true;
  };

  const validateEmail = () => {
    if (!email.trim()) {
      // Email is optional
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
      setPhoneError('The format of the phone number is incorrect');
      return false;
    }
    setPhoneError('');
    return true;
  };

  const [step, setStep] = useState(1);

  const handleContinue = (e) => {
    e.preventDefault();
    if (!username.trim()) {
      setUsernameError('Username is required');
      return;
    }
    const usernameRegex = /^[A-Za-z]+$/;
    if (!usernameRegex.test(username)) {
      setUsernameError('Username can only contain English letters');
      return;
    }
    setUsernameError('');
    if (email.trim() && !validateEmail()) {
      return;
    }
    if (phone.trim() && !validatePhone()) {
      return;
    }
    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    const isUsernameValid = validateUsername();
    const isPasswordValid = validatePassword();
    const isConfirmPasswordValid = validateConfirmPassword();
    let isEmailValid = true;
    let isPhoneValid = true;
    if (email.trim()) {
      isEmailValid = validateEmail();
    }
    if (phone.trim()) {
      isPhoneValid = validatePhone();
    }
    if (!isUsernameValid || !isPasswordValid || !isConfirmPasswordValid || !isEmailValid || !isPhoneValid) {
      return;
    }
    const userData = {
      username,
      password,
      role: "CLIENT"
    };
    if (email.trim()) {
      userData.email = email;
    }
    if (phone.trim()) {
      userData.phone = phone;
    }
    if (firstName.trim()) {
      userData.firstName = firstName;
    }
    if (lastName.trim()) {
      userData.lastName = lastName;
    }

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
              setMessage("Secure connection required. Redirecting to HTTPS...");
              if (window.location.protocol !== 'https:') {
                setTimeout(() => {
                  window.location.href = window.location.href.replace('http:', 'https:');
                }, 1500);
              }
              return;
            }
          } catch {
          }
        }
        try {
          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);
          if (data.status === 'success') {
            setMessage('Successful registration!');
            setTimeout(() => {
              navigate('/login');
            }, 2000);
          } else {
            if (data.message.includes('Username already exists')) {
              setMessage('This username is already taken. Please try a different username.');
            } else if (data.message.includes('Email already exists')) {
              setMessage('This email is already registered. Please use a different email or try logging in.');
            } else {
              setMessage(data.message || 'Registration failed');
            }
          }
        } catch (decryptError) {
          console.error("Decryption error:", decryptError);
          if (decryptError.message.includes('atob') ||
              decryptError.message.includes('session') ||
              decryptError.message.includes('decode')) {
            setMessage("Your session has expired. Please refresh the page.");
            setShowSessionErrorModal(true);
          } else {
            throw decryptError;
          }
        }
      } else {
        console.log("Using secure HTTPS registration without encryption");
        try {
          const response = await secureAxios.post('/api/auth/register', userData);
          if (response.data.status === 'success') {
            setMessage('Successful registration!');
            setTimeout(() => {
              navigate('/login');
            }, 2000);
          } else {
            if (response.data.message.includes('Username already exists')) {
              setMessage('This username is already taken. Please try a different username.');
            } else if (response.data.message.includes('Email already exists')) {
              setMessage('This email is already registered. Please use a different email or try logging in.');
            } else {
              setMessage(response.data.message || 'Registration failed');
            }
          }
        } catch (axiosError) {
          if (axiosError.code === 'ERR_CERT_AUTHORITY_INVALID') {
            setMessage("Certificate error. Please accept the certificate and try again.");
            window.dispatchEvent(new CustomEvent('certificate-error', {
              detail: { url: baseURL }
            }));
          } else if (axiosError.response?.status === 403 && axiosError.response?.data?.httpsRequired) {
            setMessage("Secure connection required. Redirecting to HTTPS...");
            if (window.location.protocol !== 'https:') {
              setTimeout(() => {
                window.location.href = window.location.href.replace('http:', 'https:');
              }, 1500);
            }
          } else {
            setMessage(axiosError.response?.data?.message || "Registration failed");
          }
        }
      }
    } catch (error) {
      console.error("Registration error:", error);
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
      } else if (error.message.includes('Username already exists')) {
        setMessage("This username is already taken. Please try a different username.");
      } else if (error.message.includes('Email already exists')) {
        setMessage("This email is already registered. Please use a different email or try logging in.");
      } else if (error.message.includes('400')) {
        setMessage("Registration failed. Please check your information and try again.");
      } else {
        setMessage("Registration failed: " + error.message);
      }
    }
  };

  const handleGoBack = () => {
    navigate('/');
  };

  return (
    <div className="signup-page-container">
      {/* --- This div is for the blurred background layer --- */}
      <div className="background-image-layer"></div>

      <header className="site-header">
        <div className="logo-container">
        </div>
        <button className="go-back-btn" onClick={handleGoBack}>
          Go Back
        </button>
      </header>

      {/* Main content wrapper with a single card for both steps */}
      <div className="signup-container">
        <div className="signup-card">

          {/* New logo and title inside the card */}
          <img src="/Untitled.png" alt="Logo" className="signup-card-logo" />
          <h2 className="signup-card-subtitle">Create Your Account</h2>

          {step === 1 && (
            <form onSubmit={handleContinue}>
              <div className="input-group">
                <input
                  type="text"
                  id="username"
                  placeholder="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
                {usernameError && <p className="error-text">{usernameError}</p>}
              </div>

              <div className="input-group">
                <input
                  type="email"
                  id="email"
                  placeholder="Email Address"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
                {emailError && <p className="error-text">{emailError}</p>}
              </div>

              <div className="input-group">
                <input
                  type="tel"
                  id="phone"
                  placeholder="Phone Number"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                />
                {phoneError && <p className="error-text">{phoneError}</p>}
              </div>

              <button type="submit" className="signup-btn">
                Sign Up
              </button>
            </form>
          )}

          {step === 2 && (
            <form onSubmit={handleSubmit}>
              <div className="input-group">
                <label htmlFor="firstName">Full Name</label>
                <div className="input-row">
                  <input
                    type="text"
                    id="firstName"
                    placeholder="Full Name"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                  />
                  <input
                    type="text"
                    id="lastName"
                    placeholder="Last Name (Optional)"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                  />
                </div>
              </div>

              <div className="input-group">
                <label htmlFor="password">Password</label>
                <input
                  type="password"
                  id="password"
                  placeholder="Create a password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                {passwordError && <p className="error-text">{passwordError}</p>}
              </div>

              <div className="input-group">
                <label htmlFor="confirmPassword">Confirm Password</label>
                <input
                  type="password"
                  id="confirmPassword"
                  placeholder="Re-enter password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
                {confirmPasswordError && (<p className="error-text">{confirmPasswordError}</p>)}
              </div>

              <button type="submit" className="signup-btn">
                Sign up
              </button>
            </form>
          )}
          
          {message && (<p className={message.includes('Successful') ? 'success-text' : 'error-text'}>{message}</p>)}
          
          {securityInitialized ? (
            <p className="security-indicator success">🔒 Secure connection established</p>
          ) : (
            <p className="security-indicator fail">Establishing secure connection...</p>
          )}
        </div>
      </div>
      
      <SessionErrorModal isOpen={showSessionErrorModal} onClose={() => setShowSessionErrorModal(false)} />
    </div>
  );
};

export default Register;