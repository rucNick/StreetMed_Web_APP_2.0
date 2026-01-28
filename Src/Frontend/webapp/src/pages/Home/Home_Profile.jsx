// Home_Profile.js
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { encrypt, decrypt, getSessionId, isInitialized } from "../../security/ecdhClient";
import { secureAxios } from "../../config/axiosConfig";

const Home_Profile = ({
  username,
  email,
  phone,
  userId,
  firstName,
  lastName,
  onLogout,
  onProfileUpdate, 
}) => {
  const navigate = useNavigate();

  const [profileOption, setProfileOption] = useState("username");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newUsername, setNewUsername] = useState("");
  const [newEmail, setNewEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [newPhone, setNewPhone] = useState("");
  const [newFirstName, setNewFirstName] = useState(firstName || "");
  const [newLastName, setNewLastName] = useState(lastName || "");
  const [profileError, setProfileError] = useState("");
  const [profileMessage, setProfileMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const baseURL = import.meta.env.VITE_SECURE_BASE_URL || import.meta.env.VITE_BASE_URL;

  const getAuthToken = () => {
    const storedUser = sessionStorage.getItem("auth_user") || localStorage.getItem("auth_user");
    if (storedUser) {
      const userData = JSON.parse(storedUser);
      return userData.authToken;
    }
    return null;
  };

  const handleApiError = (error, defaultMessage) => {
    console.error("API Error:", error);
    
    if (error.code === 'ERR_CERT_AUTHORITY_INVALID' || 
        error.message?.includes('certificate')) {
      setProfileError("Certificate error. Please accept the certificate and try again.");
      const certUrl = import.meta.env.VITE_SECURE_BASE_URL || 'https://localhost:8443';
      window.open(`${certUrl}/api/test/tls/status`, '_blank');
      return;
    }
    
    if (error.response?.status === 403 && error.response?.data?.httpsRequired) {
      setProfileError("Secure connection required for this operation.");
      return;
    }
    
    if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
      setProfileError("Cannot connect to server. Please try again.");
      return;
    }
    
    setProfileError(error.response?.data?.message || error.message || defaultMessage);
  };

  const handleSubmitProfile = async () => {
    setProfileError("");
    setProfileMessage("");
    setIsLoading(true);

    try {
      const authToken = getAuthToken();

      // --- Username update ---
      if (profileOption === "username") {
        const usernameRegex = /^[A-Za-z]+$/;
        if (!newUsername.trim() || !usernameRegex.test(newUsername.trim())) {
          setProfileError("Username must contain only letters.");
          setIsLoading(false);
          return;
        }
        if (newUsername.trim() === username) {
          setProfileError("New username must be different from the current one.");
          setIsLoading(false);
          return;
        }

        const userData = {
          userId: userId.toString(),
          newUsername: newUsername.trim(),
          authenticated: "true",
        };

        if (isInitialized() && getSessionId()) {
          const encryptedData = await encrypt(JSON.stringify(userData));
          const response = await fetch(`${baseURL}/api/auth/update/username`, {
            method: "PUT",
            headers: {
              "Content-Type": "text/plain",
              "X-Session-ID": getSessionId(),
              "X-Auth-Token": authToken || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || "Username update failed");
          }
        } else {
          const response = await secureAxios.put('/api/auth/update/username', userData, {
            headers: { "X-Auth-Token": authToken || "" }
          });
          
          if (response.data.status !== "success") {
            throw new Error(response.data.message || "Failed to update username");
          }
        }

        setProfileMessage("Username updated successfully!");
        if (onProfileUpdate) {
          onProfileUpdate({ username: newUsername.trim(), email, phone, userId, firstName, lastName });
        }
        setTimeout(() => navigate("/"), 1500);
      }

      // --- Email update ---
      else if (profileOption === "email") {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!newEmail.trim() || !emailRegex.test(newEmail.trim())) {
          setProfileError("Please enter a valid email address.");
          setIsLoading(false);
          return;
        }
        if (newEmail.trim() === email) {
          setProfileError("New email must be different from the current one.");
          setIsLoading(false);
          return;
        }
        if (!currentPassword.trim()) {
          setProfileError("Current password is required.");
          setIsLoading(false);
          return;
        }

        const userData = {
          userId: userId.toString(),
          currentPassword: currentPassword.trim(),
          newEmail: newEmail.trim(),
          authenticated: "true",
        };

        if (isInitialized() && getSessionId()) {
          const encryptedData = await encrypt(JSON.stringify(userData));
          const response = await fetch(`${baseURL}/api/auth/update/email`, {
            method: "PUT",
            headers: {
              "Content-Type": "text/plain",
              "X-Session-ID": getSessionId(),
              "X-Auth-Token": authToken || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || "Email update failed");
          }
        } else {
          const response = await secureAxios.put('/api/auth/update/email', userData, {
            headers: { "X-Auth-Token": authToken || "" }
          });
          
          if (response.data.status !== "success") {
            throw new Error(response.data.message || "Failed to update email");
          }
        }

        setProfileMessage("Email updated successfully!");
        if (onProfileUpdate) {
          onProfileUpdate({ username, email: newEmail.trim(), phone, userId, firstName, lastName });
        }
        setTimeout(() => navigate("/"), 1500);
      }

      // --- Password update ---
      else if (profileOption === "password") {
        if (newPassword.length < 8) {
          setProfileError("Password must be at least 8 characters long.");
          setIsLoading(false);
          return;
        }
        if (newPassword !== confirmPassword) {
          setProfileError("Passwords do not match.");
          setIsLoading(false);
          return;
        }
        if (!currentPassword.trim()) {
          setProfileError("Current password is required.");
          setIsLoading(false);
          return;
        }

        const userData = {
          userId: userId.toString(),
          currentPassword: currentPassword.trim(),
          newPassword: newPassword.trim(),
          authenticated: "true",
        };

        if (isInitialized() && getSessionId()) {
          const encryptedData = await encrypt(JSON.stringify(userData));
          const response = await fetch(`${baseURL}/api/auth/update/password`, {
            method: "PUT",
            headers: {
              "Content-Type": "text/plain",
              "X-Session-ID": getSessionId(),
              "X-Auth-Token": authToken || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || "Password update failed");
          }
        } else {
          const response = await secureAxios.put('/api/auth/update/password', userData, {
            headers: { "X-Auth-Token": authToken || "" }
          });
          
          if (response.data.status !== "success") {
            throw new Error(response.data.message || "Failed to update password");
          }
        }

        setProfileMessage("Password updated! Redirecting to login...");
        setTimeout(() => {
          onLogout();
          navigate("/login");
        }, 1500);
      }

      // --- Phone update ---
      else if (profileOption === "phone") {
        const phoneRegex = /^\d{10}$/;
        if (!newPhone.trim() || !phoneRegex.test(newPhone.trim())) {
          setProfileError("Phone number must be a 10-digit number.");
          setIsLoading(false);
          return;
        }
        if (newPhone.trim() === phone) {
          setProfileError("New phone must be different from the current one.");
          setIsLoading(false);
          return;
        }
        if (!currentPassword.trim()) {
          setProfileError("Current password is required.");
          setIsLoading(false);
          return;
        }

        const userData = {
          userId: userId.toString(),
          currentPassword: currentPassword.trim(),
          newPhone: newPhone.trim(),
          authenticated: "true",
        };

        if (isInitialized() && getSessionId()) {
          const encryptedData = await encrypt(JSON.stringify(userData));
          const response = await fetch(`${baseURL}/api/auth/update/phone`, {
            method: "PUT",
            headers: {
              "Content-Type": "text/plain",
              "X-Session-ID": getSessionId(),
              "X-Auth-Token": authToken || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || "Phone update failed");
          }
        } else {
          const response = await secureAxios.put('/api/auth/update/phone', userData, {
            headers: { "X-Auth-Token": authToken || "" }
          });
          
          if (response.data.status !== "success") {
            throw new Error(response.data.message || "Failed to update phone");
          }
        }

        setProfileMessage("Phone number updated successfully!");
        if (onProfileUpdate) {
          onProfileUpdate({ username, email, phone: newPhone.trim(), userId, firstName, lastName });
        }
        setTimeout(() => navigate("/"), 1500);
      }

      // --- Name update ---
      else if (profileOption === "name") {
        if (!newFirstName.trim() && !newLastName.trim()) {
          setProfileError("Please enter at least first or last name.");
          setIsLoading(false);
          return;
        }

        const userData = {
          userId: userId.toString(),
          firstName: newFirstName.trim(),
          lastName: newLastName.trim(),
          authenticated: "true",
        };

        if (isInitialized() && getSessionId()) {
          const encryptedData = await encrypt(JSON.stringify(userData));
          const response = await fetch(`${baseURL}/api/auth/update/name`, {
            method: "PUT",
            headers: {
              "Content-Type": "text/plain",
              "X-Session-ID": getSessionId(),
              "X-Auth-Token": authToken || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || "Name update failed");
          }
        } else {
          const response = await secureAxios.put('/api/auth/update/name', userData, {
            headers: { "X-Auth-Token": authToken || "" }
          });
          
          if (response.data.status !== "success") {
            throw new Error(response.data.message || "Failed to update name");
          }
        }

        setProfileMessage("Name updated successfully!");
        if (onProfileUpdate) {
          onProfileUpdate({ 
            username, email, phone, userId, 
            firstName: newFirstName.trim(), 
            lastName: newLastName.trim() 
          });
        }
        setTimeout(() => navigate("/"), 1500);
      }

    } catch (error) {
      handleApiError(error, "Failed to update profile");
    } finally {
      setIsLoading(false);
    }
  };

const profileOptions = [
  { value: 'name', label: 'Name', icon: '👤' },
  { value: 'username', label: 'Username', icon: '🏷️' },
  { value: 'email', label: 'Email', icon: '✉️' },
  { value: 'phone', label: 'Phone', icon: '📱' },
  { value: 'password', label: 'Password', icon: '🔐' },
];

  const needsPassword = ['email', 'phone', 'password'].includes(profileOption);

  return (
    <div style={styles.pageContainer}>
      {/* Header */}
      <div style={styles.header}>
        <button style={styles.backButton} onClick={() => navigate(-1)}>
          Back
        </button>
      </div>

      <div style={styles.contentWrapper}>
        <h1 style={styles.title}>Update Profile</h1>

        {/* Current User Info Card */}
        <div style={styles.userInfoCard}>
          <div style={styles.avatar}>
            {(firstName?.[0] || username?.[0] || '?').toUpperCase()}
          </div>
          <div style={styles.userDetails}>
            <span style={styles.userName}>
              {username}{(firstName || lastName) ? ` (${[firstName, lastName].filter(Boolean).join(' ')})` : ''}
            </span>
            <span style={styles.userEmail}>{email}</span>
          </div>
        </div>

        {/* Main Card */}
        <div style={styles.card}>
          {/* Option Selector */}
          <div style={styles.optionGrid}>
            {profileOptions.map((option) => (
              <button
                key={option.value}
                style={{
                  ...styles.optionButton,
                  ...(profileOption === option.value ? styles.optionButtonActive : {})
                }}
                onClick={() => {
                  setProfileOption(option.value);
                  setProfileError("");
                  setProfileMessage("");
                  setCurrentPassword("");
                }}
                disabled={isLoading}
              >
                <span style={styles.optionIcon}>{option.icon}</span>
                <span style={styles.optionLabel}>{option.label}</span>
              </button>
            ))}
          </div>

          <div style={styles.divider} />

          {/* Form Fields */}
          <div style={styles.formSection}>
            {/* Name Fields */}
            {profileOption === "name" && (
              <>
                <div style={styles.inputGroup}>
                  <label style={styles.label}>First Name</label>
                  <input
                    style={styles.input}
                    type="text"
                    value={newFirstName}
                    onChange={(e) => setNewFirstName(e.target.value)}
                    placeholder="Enter first name"
                    disabled={isLoading}
                  />
                </div>
                <div style={styles.inputGroup}>
                  <label style={styles.label}>Last Name</label>
                  <input
                    style={styles.input}
                    type="text"
                    value={newLastName}
                    onChange={(e) => setNewLastName(e.target.value)}
                    placeholder="Enter last name"
                    disabled={isLoading}
                  />
                </div>
              </>
            )}

            {/* Username Field */}
            {profileOption === "username" && (
              <div style={styles.inputGroup}>
                <label style={styles.label}>New Username</label>
                <input
                  style={styles.input}
                  type="text"
                  value={newUsername}
                  onChange={(e) => setNewUsername(e.target.value)}
                  placeholder="Letters only"
                  disabled={isLoading}
                />
                <span style={styles.hint}>Current: {username}</span>
              </div>
            )}

            {/* Email Field */}
            {profileOption === "email" && (
              <div style={styles.inputGroup}>
                <label style={styles.label}>New Email</label>
                <input
                  style={styles.input}
                  type="email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  placeholder="you@example.com"
                  disabled={isLoading}
                />
                <span style={styles.hint}>Current: {email}</span>
              </div>
            )}

            {/* Phone Field */}
            {profileOption === "phone" && (
              <div style={styles.inputGroup}>
                <label style={styles.label}>New Phone Number</label>
                <input
                  style={styles.input}
                  type="tel"
                  value={newPhone}
                  onChange={(e) => setNewPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
                  placeholder="10-digit number"
                  disabled={isLoading}
                />
                <span style={styles.hint}>Current: {phone || 'Not set'}</span>
              </div>
            )}

            {/* Password Fields */}
            {profileOption === "password" && (
              <>
                <div style={styles.inputGroup}>
                  <label style={styles.label}>New Password</label>
                  <input
                    style={styles.input}
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Min. 8 characters"
                    disabled={isLoading}
                  />
                  {newPassword && (
                    <div style={styles.strengthBar}>
                      <div style={{
                        ...styles.strengthFill,
                        width: newPassword.length >= 8 ? '100%' : `${(newPassword.length / 8) * 100}%`,
                        backgroundColor: newPassword.length >= 8 ? '#27ae60' : '#f39c12'
                      }} />
                    </div>
                  )}
                </div>
                <div style={styles.inputGroup}>
                  <label style={styles.label}>Confirm New Password</label>
                  <input
                    style={styles.input}
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Re-enter password"
                    disabled={isLoading}
                  />
                  {confirmPassword && newPassword && (
                    <span style={{
                      ...styles.matchText,
                      color: newPassword === confirmPassword ? '#27ae60' : '#e74c3c'
                    }}>
                      {newPassword === confirmPassword ? 'Passwords match' : 'Passwords do not match'}
                    </span>
                  )}
                </div>
              </>
            )}

            {/* Current Password (for sensitive changes) */}
            {needsPassword && (
              <div style={styles.inputGroup}>
                <label style={styles.label}>Current Password</label>
                <input
                  style={styles.input}
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="Enter current password"
                  disabled={isLoading}
                />
                <span style={styles.hint}>Required for security verification</span>
              </div>
            )}

            {/* Error/Success Messages */}
            {profileError && <div style={styles.errorBox}>{profileError}</div>}
            {profileMessage && <div style={styles.successBox}>{profileMessage}</div>}

            {/* Action Buttons */}
            <button 
              style={{
                ...styles.primaryButton,
                opacity: isLoading ? 0.6 : 1
              }}
              onClick={handleSubmitProfile}
              disabled={isLoading}
            >
              {isLoading ? 'Updating...' : 'Save Changes'}
            </button>

            <button 
              style={styles.cancelButton}
              onClick={() => navigate(-1)}
              disabled={isLoading}
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

const styles = {
  pageContainer: {
    minHeight: '100vh',
    backgroundColor: '#1a1a2e',
    padding: '20px',
    boxSizing: 'border-box'
  },
  header: {
    marginBottom: '20px'
  },
  backButton: {
    padding: '10px 20px',
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    color: '#fff',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    borderRadius: '8px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '500',
    transition: 'all 0.2s ease'
  },
  contentWrapper: {
    maxWidth: '500px',
    margin: '0 auto'
  },
  title: {
    fontSize: '28px',
    fontWeight: '700',
    color: '#fff',
    margin: '0 0 25px 0',
    textAlign: 'center'
  },
  userInfoCard: {
    display: 'flex',
    alignItems: 'center',
    gap: '15px',
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    padding: '16px 20px',
    borderRadius: '12px',
    marginBottom: '20px'
  },
  avatar: {
    width: '50px',
    height: '50px',
    borderRadius: '50%',
    backgroundColor: '#f78702',
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '20px',
    fontWeight: '700'
  },
  userDetails: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px'
  },
  userName: {
    color: '#fff',
    fontSize: '16px',
    fontWeight: '600'
  },
  userEmail: {
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: '13px'
  },
  card: {
    backgroundColor: '#d4c5a9',
    borderRadius: '16px',
    padding: '25px',
    boxShadow: '0 8px 30px rgba(0, 0, 0, 0.3)'
  },
  optionGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '10px',
    marginBottom: '20px'
  },
  optionButton: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '6px',
    padding: '14px 10px',
    backgroundColor: '#fff',
    border: '2px solid transparent',
    borderRadius: '10px',
    cursor: 'pointer',
    transition: 'all 0.2s ease'
  },
  optionButtonActive: {
    border: '2px solid #003e7e',
    backgroundColor: 'rgba(0, 62, 126, 0.08)'
  },
  optionIcon: {
    fontSize: '20px'
  },
  optionLabel: {
    fontSize: '12px',
    fontWeight: '600',
    color: '#333'
  },
  divider: {
    height: '1px',
    backgroundColor: 'rgba(0, 0, 0, 0.1)',
    margin: '0 0 20px 0'
  },
  formSection: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  inputGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  label: {
    fontSize: '13px',
    fontWeight: '600',
    color: '#333'
  },
  input: {
    padding: '14px 16px',
    fontSize: '14px',
    border: '2px solid #e0e0e0',
    borderRadius: '10px',
    backgroundColor: '#fff',
    color: '#333',
    outline: 'none',
    transition: 'border-color 0.2s ease'
  },
  hint: {
    fontSize: '11px',
    color: '#888'
  },
  strengthBar: {
    height: '4px',
    backgroundColor: '#e0e0e0',
    borderRadius: '2px',
    overflow: 'hidden',
    marginTop: '4px'
  },
  strengthFill: {
    height: '100%',
    borderRadius: '2px',
    transition: 'all 0.3s ease'
  },
  matchText: {
    fontSize: '12px',
    marginTop: '2px'
  },
  errorBox: {
    backgroundColor: 'rgba(231, 76, 60, 0.1)',
    border: '1px solid rgba(231, 76, 60, 0.3)',
    color: '#c0392b',
    padding: '12px 14px',
    borderRadius: '8px',
    fontSize: '13px'
  },
  successBox: {
    backgroundColor: 'rgba(39, 174, 96, 0.1)',
    border: '1px solid rgba(39, 174, 96, 0.3)',
    color: '#27ae60',
    padding: '12px 14px',
    borderRadius: '8px',
    fontSize: '13px'
  },
  primaryButton: {
    padding: '14px',
    fontSize: '15px',
    fontWeight: '600',
    border: 'none',
    borderRadius: '10px',
    backgroundColor: '#f78702',
    color: '#fff',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
    marginTop: '8px'
  },
  cancelButton: {
    padding: '12px',
    fontSize: '14px',
    fontWeight: '600',
    border: 'none',
    borderRadius: '10px',
    backgroundColor: '#003e7e',
    color: '#fff',
    cursor: 'pointer',
    transition: 'all 0.2s ease'
  }
};

export default Home_Profile;