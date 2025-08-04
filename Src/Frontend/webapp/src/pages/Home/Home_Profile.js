// Home_Profile.js
import React, { useState } from "react";
import axios from "axios";
import { encrypt, decrypt, getSessionId, isInitialized } from "../../security/ecdhClient";
import { useNavigate } from "react-router-dom";
import "../../css/Home/Home_Profile.css";

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

  const baseURL = process.env.REACT_APP_BASE_URL;

  // Handle submission of profile update
  const handleSubmitProfile = async () => {

    setProfileError("");
    setProfileMessage("");

    // --- For username update ---
    if (profileOption === "username") {
      const usernameRegex = /^[A-Za-z]+$/;
      if (!newUsername.trim() || !usernameRegex.test(newUsername.trim())) {
        setProfileError("Username must contain only letters.");
        return;
      }
      if (newUsername.trim() === username) {
        setProfileError("New username must be different from the current one.");
        return;
      }

      try {
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
            },
            body: encryptedData,
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
        }

        else {
          const response = await axios.put(`${baseURL}/api/auth/update/username`, userData);
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
      } catch (error) {
        console.error("Error updating username", error);

        setProfileError(error.message || "Failed to update username.");
      }
    }

    // --- For email update ---
    else if (profileOption === "email") {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!newEmail.trim() || !emailRegex.test(newEmail.trim())) {
        setProfileError("Please enter a valid email address.");
        return;
      }
      if (newEmail.trim() === email) {
        setProfileError("New email must be different from the current one.");
        return;
      }
      if (!currentPassword.trim()) {
        setProfileError("Current password is required.");
        return;
      }

      try {
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
            },
            body: encryptedData,
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
          const response = await axios.put(`${baseURL}/api/auth/update/email`, userData);
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
      } catch (error) {
        console.error("Error updating email", error);
        setProfileError(error.message || "Failed to update email.");
      }
    }

    // --- For password update ---
    else if (profileOption === "password") {
      const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;
      // 1) check new password format
      if (!newPassword.trim() || !passwordRegex.test(newPassword.trim())) {
        setProfileError("Password must be alphanumeric and at least 8 characters long.");
        return;
      }
      // 2) confirm new password
      if (newPassword.trim() !== confirmPassword.trim()) {
        setProfileError("The two passwords do not match.");
        return;
      }
      // 3) check input
      if (!currentPassword.trim()) {
        setProfileError("Current password is required.");
        return;
      }

      try {
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
            },
            body: encryptedData,
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
            navigate("/");
          }, 1500);
        } else {
          const response = await axios.put(`${baseURL}/api/auth/update/password`, userData);
          if (response.data.status === "success") {
            setProfileMessage("Password updated successfully. Redirecting to login...");
            setTimeout(() => {
              onLogout();
              navigate("/");
            }, 1500);
          } else {
            setProfileError(response.data.message || "Failed to update password.");
          }
        }
      } catch (error) {
        console.error("Error updating password", error);
        setProfileError(error.message || "Failed to update password.");
      }
    }

    // --- For phone update ---
    else if (profileOption === "phone") {
      const phoneRegex = /^\d{10}$/;
      if (!newPhone.trim() || !phoneRegex.test(newPhone.trim())) {
        setProfileError("Phone number must be a 10-digit US number.");
        return;
      }
      if (newPhone.trim() === phone) {
        setProfileError("New phone number must be different from the current one.");
        return;
      }
      if (!currentPassword.trim()) {
        setProfileError("Current password is required.");
        return;
      }

      try {
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
            },
            body: encryptedData,
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
          const response = await axios.put(`${baseURL}/api/auth/update/phone`, userData);
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
      } catch (error) {
        console.error("Error updating phone number", error);
        setProfileError(error.message || "Failed to update phone number.");
      }
    }
  };

  return (
    <div className="profile-container">
      <h2>Update Profile</h2>
      <br></br>
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
            />
            Username
          </label>
          <label>
            <input
              type="radio"
              value="email"
              checked={profileOption === "email"}
              onChange={(e) => setProfileOption(e.target.value)}
            />
            Email
          </label>
          <label>
            <input
              type="radio"
              value="password"
              checked={profileOption === "password"}
              onChange={(e) => setProfileOption(e.target.value)}
            />
            Password
          </label>
          <label>
            <input
              type="radio"
              value="phone"
              checked={profileOption === "phone"}
              onChange={(e) => setProfileOption(e.target.value)}
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
          />
        </div>
      )}

      {profileError && <p className="profile-errorText">{profileError}</p>}
      {profileMessage && <p className="profile-successText">{profileMessage}</p>}

      <button className="profile-button" onClick={handleSubmitProfile}>
        Submit Profile Update
      </button>
      <button className="profile-cancelButton" onClick={() => navigate(-1)}>
        Cancel
      </button>
    </div>
  );
};

export default Home_Profile;
