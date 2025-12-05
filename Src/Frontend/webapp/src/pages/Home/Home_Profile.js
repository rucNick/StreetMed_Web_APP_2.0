// Home_Profile.js
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { encrypt, decrypt, getSessionId, isInitialized } from "../../security/ecdhClient";
import { secureAxios } from "../../config/axiosConfig";
import '../../index.css'; 

const Home_Profile = ({
  username,
  email,
  password,
  phone,
  userId,
  onLogout,
  onProfileUpdate, 
}) => {
  const navigate = useNavigate();

  // State for profile update
  const [profileOption, setProfileOption] = useState("username");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newUsername, setNewUsername] = useState("");
  const [newEmail, setNewEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [newPhone, setNewPhone] = useState("");
  const [profileError, setProfileError] = useState("");
  const [profileMessage, setProfileMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const baseURL = process.env.REACT_APP_SECURE_BASE_URL || process.env.REACT_APP_BASE_URL;

  // Get auth token from storage
  const getAuthToken = () => {
    const storedUser = sessionStorage.getItem("auth_user") || localStorage.getItem("auth_user");
    if (storedUser) {
      const userData = JSON.parse(storedUser);
      return userData.authToken;
    }
    return null;
  };

  // Handle HTTPS requirement errors
  const handleHttpsError = (error) => {
    if (error.response?.status === 403 && error.response?.data?.httpsRequired) {
      setProfileError("Secure connection required for profile updates.");
      if (window.location.protocol !== 'https:') {
        setTimeout(() => {
          window.location.href = window.location.href.replace('http:', 'https:');
        }, 1500);
      }
      return true;
    }
    return false;
  };

  // Handle certificate errors
  const handleCertificateError = (error) => {
    if (error.code === 'ERR_CERT_AUTHORITY_INVALID' || 
        error.message?.includes('certificate')) {
      setProfileError("Certificate error. Please accept the certificate and try again.");
      window.dispatchEvent(new CustomEvent('certificate-error', { 
        detail: { url: baseURL }
      }));
      return true;
    }
    return false;
  };

  // Handle submission of profile update
  const handleSubmitProfile = async () => {
    setProfileError("");
    setProfileMessage("");
    setIsLoading(true);

    try {
      // --- For username update ---
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
          // Use encrypted request over HTTPS
          const encryptedData = await encrypt(JSON.stringify(userData));
          const response = await fetch(`${baseURL}/api/auth/update/username`, {
            method: "PUT",
            headers: {
              "Content-Type": "text/plain",
              "X-Session-ID": getSessionId(),
              "X-Auth-Token": getAuthToken() || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || `Username update failed: ${response.status}`);
          }

          setProfileMessage("Profile updated successfully.");
          if (onProfileUpdate) {
            onProfileUpdate({
              username: newUsername.trim(),
              email,
              phone,
              userId,
            });
          }
          setTimeout(() => {
            navigate("/");
          }, 1500);
        } else {
          // Use secureAxios for non-encrypted HTTPS request
          const response = await secureAxios.put('/api/auth/update/username', userData, {
            headers: {
              "X-Auth-Token": getAuthToken() || ""
            }
          });
          
          if (response.data.status === "success") {
            setProfileMessage("Profile updated successfully.");
            if (onProfileUpdate) {
              onProfileUpdate({
                username: newUsername.trim(),
                email,
                phone,
                userId,
              });
            }
            setTimeout(() => {
              navigate("/");
            }, 1500);
          } else {
            setProfileError(response.data.message || "Failed to update username.");
          }
        }
      }

      // --- For email update ---
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
              "X-Auth-Token": getAuthToken() || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || `Email update failed: ${response.status}`);
          }

          setProfileMessage("Profile updated successfully.");
          if (onProfileUpdate) {
            onProfileUpdate({
              username,
              email: newEmail.trim(),
              phone,
              userId,
            });
          }
          setTimeout(() => {
            navigate("/");
          }, 1500);
        } else {
          const response = await secureAxios.put('/api/auth/update/email', userData, {
            headers: {
              "X-Auth-Token": getAuthToken() || ""
            }
          });
          
          if (response.data.status === "success") {
            setProfileMessage("Profile updated successfully.");
            if (onProfileUpdate) {
              onProfileUpdate({
                username,
                email: newEmail.trim(),
                phone,
                userId,
              });
            }
            setTimeout(() => {
              navigate("/");
            }, 1500);
          } else {
            setProfileError(response.data.message || "Failed to update email.");
          }
        }
      }

      // --- For password update (ALWAYS requires HTTPS) ---
      else if (profileOption === "password") {
        // Check if we're on HTTPS
        if (window.location.protocol !== 'https:') {
          setProfileError("Password updates require a secure HTTPS connection. Redirecting...");
          setTimeout(() => {
            window.location.href = window.location.href.replace('http:', 'https:');
          }, 1500);
          setIsLoading(false);
          return;
        }

        const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;
        if (!newPassword.trim() || !passwordRegex.test(newPassword.trim())) {
          setProfileError("Password must be alphanumeric and at least 8 characters long.");
          setIsLoading(false);
          return;
        }
        if (newPassword.trim() !== confirmPassword.trim()) {
          setProfileError("The two passwords do not match.");
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
              "X-Auth-Token": getAuthToken() || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || `Password update failed: ${response.status}`);
          }

          setProfileMessage("Password updated successfully. Redirecting to login...");
          setTimeout(() => {
            onLogout();
            navigate("/login");
          }, 1500);
        } else {
          const response = await secureAxios.put('/api/auth/update/password', userData, {
            headers: {
              "X-Auth-Token": getAuthToken() || ""
            }
          });
          
          if (response.data.status === "success") {
            setProfileMessage("Password updated successfully. Redirecting to login...");
            setTimeout(() => {
              onLogout();
              navigate("/login");
            }, 1500);
          } else {
            setProfileError(response.data.message || "Failed to update password.");
          }
        }
      }

      // --- For phone update ---
      else if (profileOption === "phone") {
        const phoneRegex = /^\d{10}$/;
        if (!newPhone.trim() || !phoneRegex.test(newPhone.trim())) {
          setProfileError("Phone number must be a 10-digit US number.");
          setIsLoading(false);
          return;
        }
        if (newPhone.trim() === phone) {
          setProfileError("New phone number must be different from the current one.");
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
              "X-Auth-Token": getAuthToken() || ""
            },
            body: encryptedData,
            credentials: 'include',
            mode: 'cors'
          });

          const encryptedResponse = await response.text();
          const decryptedResponse = await decrypt(encryptedResponse);
          const data = JSON.parse(decryptedResponse);

          if (!response.ok || data.status !== "success") {
            throw new Error(data.message || `Phone update failed: ${response.status}`);
          }

          setProfileMessage("Profile updated successfully.");
          if (onProfileUpdate) {
            onProfileUpdate({
              username,
              email,
              phone: newPhone.trim(),
              userId,
            });
          }
          setTimeout(() => {
            navigate("/");
          }, 1500);
        } else {
          const response = await secureAxios.put('/api/auth/update/phone', userData, {
            headers: {
              "X-Auth-Token": getAuthToken() || ""
            }
          });
          
          if (response.data.status === "success") {
            setProfileMessage("Profile updated successfully.");
            if (onProfileUpdate) {
              onProfileUpdate({
                username,
                email,
                phone: newPhone.trim(),
                userId,
              });
            }
            setTimeout(() => {
              navigate("/");
            }, 1500);
          } else {
            setProfileError(response.data.message || "Failed to update phone number.");
          }
        }
      }
    } catch (error) {
      console.error("Error updating profile", error);
      
      // Check for certificate errors first
      if (handleCertificateError(error)) {
        // Error already handled
      } else if (handleHttpsError(error)) {
        // Error already handled
      } else {
        setProfileError(error.message || "Failed to update profile.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="profile-container">
      <h2>Update Profile</h2>
      <br></br>
      
      {/* Security indicator */}
      <div style={{ marginBottom: '15px', fontSize: '14px' }}>
        {window.location.protocol === 'https:' ? (
          <span style={{color: '#27ae60'}}>🔒 Secure Connection</span>
        ) : (
          <span style={{color: '#e67e22'}}>⚠️ Consider using HTTPS for secure profile updates</span>
        )}
      </div>
      
      <p>Current Username: <strong>{username}</strong></p>
      <div className="profile-formGroup">
        <label>Select field to update:</label>
        <div className="profile-radioGroup">
          <label>
            <input
              type="radio"
              value="username"
              checked={profileOption === "username"}
              onChange={(e) => setProfileOption(e.target.value)}
              disabled={isLoading}
            />
            Username
          </label>
          <label>
            <input
              type="radio"
              value="email"
              checked={profileOption === "email"}
              onChange={(e) => setProfileOption(e.target.value)}
              disabled={isLoading}
            />
            Email
          </label>
          <label>
            <input
              type="radio"
              value="password"
              checked={profileOption === "password"}
              onChange={(e) => setProfileOption(e.target.value)}
              disabled={isLoading}
            />
            Password {window.location.protocol !== 'https:' && <span style={{color: '#c0392b'}}>(HTTPS Required)</span>}
          </label>
          <label>
            <input
              type="radio"
              value="phone"
              checked={profileOption === "phone"}
              onChange={(e) => setProfileOption(e.target.value)}
              disabled={isLoading}
            />
            Phone Number
          </label>
        </div>
      </div>

      {(profileOption === "email" ||
        profileOption === "password" ||
        profileOption === "phone") && (
        <div className="profile-formGroup">
          <label>Current Password:</label>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            className="profile-input"
            disabled={isLoading}
          />
        </div>
      )}

      {profileOption === "username" && (
        <div className="profile-formGroup">
          <label>New Username:</label>
          <input
            type="text"
            value={newUsername}
            onChange={(e) => setNewUsername(e.target.value)}
            className="profile-input"
            placeholder="Only letters"
            disabled={isLoading}
          />
        </div>
      )}

      {profileOption === "email" && (
        <div className="profile-formGroup">
          <label>New Email:</label>
          <input
            type="email"
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            className="profile-input"
            placeholder="example@domain.com"
            disabled={isLoading}
          />
        </div>
      )}

      {profileOption === "password" && (
        <>
          <div className="profile-formGroup">
            <label>New Password:</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="profile-input"
              placeholder="Alphanumeric, min 8 chars"
              disabled={isLoading}
            />
          </div>
          <div className="profile-formGroup">
            <label>Confirm New Password:</label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="profile-input"
              placeholder="Confirm new password"
              disabled={isLoading}
            />
          </div>
        </>
      )}

      {profileOption === "phone" && (
        <div className="profile-formGroup">
          <label>New Phone Number:</label>
          <input
            type="text"
            value={newPhone}
            onChange={(e) => setNewPhone(e.target.value)}
            className="profile-input"
            placeholder="10-digit number"
            disabled={isLoading}
          />
        </div>
      )}

      {profileError && <p className="profile-errorText">{profileError}</p>}
      {profileMessage && <p className="profile-successText">{profileMessage}</p>}

      <button 
        className="profile-button" 
        onClick={handleSubmitProfile}
        disabled={isLoading}
        style={{ opacity: isLoading ? 0.6 : 1 }}
      >
        {isLoading ? 'Updating...' : 'Submit Profile Update'}
      </button>
      <button 
        className="profile-cancelButton" 
        onClick={() => navigate(-1)}
        disabled={isLoading}
      >
        Cancel
      </button>
    </div>
  );
};

export default Home_Profile;