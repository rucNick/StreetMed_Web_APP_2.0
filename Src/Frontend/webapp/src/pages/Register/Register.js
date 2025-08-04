import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  performKeyExchange,
  encrypt,
  decrypt,
  getSessionId,
  isInitialized
} from '../../security/ecdhClient';
import '../../css/Login/Register.css';
import SessionErrorModal from '../../components/SessionErrorModal';

const Register = () => {
  const navigate = useNavigate();
  const [securityInitialized, setSecurityInitialized] = useState(false);
  const baseURL = process.env.REACT_APP_BASE_URL;
  const [showSessionErrorModal, setShowSessionErrorModal] = useState(false);

  useEffect(() => {
    const initSecurity = async () => {
      try {
        console.log("Initializing security for registration...");
        const result = await performKeyExchange();
        if (result.success) {
          console.log("Security initialized successfully for registration");
          setSecurityInitialized(true);
        } else {
          console.error("Failed to initialize security:", result.error);
        }
      } catch (error) {
        console.error("Error during security initialization:", error);
      }
    };
    initSecurity();
  }, []);

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
  
    // Validate username
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
  
    // Validate email format if provided (but don't require it)
    if (email.trim() && !validateEmail()) {
      return;
    }
  
    // Validate phone format if provided (but don't require it)
    if (phone.trim() && !validatePhone()) {
      return;
    }
  
    // Move to step 2 even if no email or phone is provided
    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage(''); // Clear previous messages
  
    const isUsernameValid = validateUsername();
    const isPasswordValid = validatePassword();
    const isConfirmPasswordValid = validateConfirmPassword();
    
    // Only validate email and phone if they're provided
    let isEmailValid = true;
    let isPhoneValid = true;
    
    if (email.trim()) {
      isEmailValid = validateEmail();
    }
    
    if (phone.trim()) {
      isPhoneValid = validatePhone();
    }
  
    if (!isUsernameValid || !isPasswordValid || !isConfirmPasswordValid ||
        !isEmailValid || !isPhoneValid) {
      return;
    }
  
    const userData = {
      username,
      password,
      role: "CLIENT"
    };
    
    // Only include email and phone if they're provided
    if (email.trim()) {
      userData.email = email;
    }
    
    if (phone.trim()) {
      userData.phone = phone;
    }
    
    // Include name fields if provided
    if (firstName.trim()) {
      userData.firstName = firstName;
    }
    
    if (lastName.trim()) {
      userData.lastName = lastName;
    }
    try {
      if (securityInitialized && isInitialized()) {
        console.log("Using secure encrypted registration");
        const encryptedData = await encrypt(JSON.stringify(userData));
        const response = await fetch(`${baseURL}/api/auth/register`, {
          method: 'POST',
          headers: {
            'Content-Type': 'text/plain',
            'X-Session-ID': getSessionId()
          },
          body: encryptedData
        });

        try {
          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (data.status === 'success') {
            setMessage('Successful registration!');
            setTimeout(() => {
              navigate('/');
            }, 2000);
          } else {
            // Specific error handling based on error message
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
          // Handle session expiration or decryption errors
          if (decryptError.message.includes('atob') || 
              decryptError.message.includes('session') ||
              decryptError.message.includes('decode')) {
            setMessage("Your session has expired. Please refresh the page.");
            setShowSessionErrorModal(true);
          } else {
            throw decryptError;
          }
        }
      }
    } catch (error) {
      console.error("Registration error:", error);
      
      // User-friendly error handling
      if (error.message.includes('session') || 
          error.message.includes('atob') || 
          error.message.includes('decode')) {
        setMessage("Session error. Please refresh the page.");
        setShowSessionErrorModal(true);
      } else if (error.message.includes('Username already exists')) {
        setMessage("This username is already taken. Please try a different username.");
      } else if (error.message.includes('Email already exists')) {
        setMessage("This email is already registered. Please use a different email or try logging in.");
      } else if (error.message.includes('400')) {
        // Generic 400 error probably means validation error
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
      <header className="site-header">
        <div className="logo-container">
          <img src="/Untitled.png" alt="Site Logo" className="logo" />
          <h2 className="site-title">Street Med Go</h2>
        </div>
        <button className="go-back-btn" onClick={handleGoBack}>
          Go Back
        </button>
      </header>

      <button className="go-back-btn" onClick={handleGoBack}>
        Go back
      </button>

      {/* step 1 */}
      {step === 1 && (
      <div className="signup-container">
        <div className="signup-card">
          <h2 className="signup-title">Sign Up</h2>
          <form onSubmit={handleContinue}>
            {/* Username field */}
            <input
              type="text"
              placeholder="Username (Required)"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
            {usernameError && <p className="error-text">{usernameError}</p>}
            
            <input
              type="email"
              placeholder="Email (Optional)"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />

            <div className="divider"><span>Or</span></div>

            <input
              type="tel"
              placeholder="Phone Number (Optional)"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />

            {emailError && <p className="error-text">{emailError}</p>}
            {phoneError && <p className="error-text">{phoneError}</p>}
            
            <p className="form-note">Email or Phone Number is recommended but not required</p>

            <button type="submit" className="signup-btn">
              Continue
            </button>
          </form>
        </div>
      </div>
    )}

      {/* step 2 */}
      {step === 2 && (
        <div className="signup-container">
          <div className="signup-card">
            <h2 className="signup-title">Welcome, {username}</h2>
            <form onSubmit={handleSubmit}>
              {/* First and last name fields moved to step 2 */}
              <div className="input-row">
                <input
                  type="text"
                  placeholder="First Name (Optional)"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                />
                <input
                  type="text"
                  placeholder="Last Name (Optional)"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                />
              </div>
              
              <input
                type="password"
                placeholder="Create a password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              {passwordError && <p className="error-text">{passwordError}</p>}

              <input
                type="password"
                placeholder="Re-enter password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
              {confirmPasswordError && (
                <p className="error-text">{confirmPasswordError}</p>
              )}

              <button type="submit" className="signup-btn">
                Sign up
              </button>

              {message && (
                <p className={message.includes('Successful') ? 'success-text' : 'error-text'}>
                  {message}
                </p>
              )}

              {securityInitialized ? (
                <p className="security-indicator success">Secure connection established</p>
              ) : (
                <p className="security-indicator fail">Establishing secure connection...</p>
              )}
            </form>
          </div>
        </div>
      )}

      {/* Session Error Modal */}
      <SessionErrorModal 
        isOpen={showSessionErrorModal}
        onClose={() => setShowSessionErrorModal(false)}
      />
    </div>
  );
};

export default Register;