// App.js
import React, { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "../Login/Login";
import Home from "../Home/Home";
import HomeFeedback from "../Home/Home_Feedback";
import HomeProfile from "../Home/Home_Profile";
import HomeOrderHistory from "../Home/Home_OrderHistory";
import Register from "../Register/Register";
import Guest from "../Guest/Guest";
import VolunteerAppli from "../Volunteer/volunteer_appli";
import Admin from "../Admin/Admin";
import CargoAdmin from "../Admin/Cargo_Admin";
import CargoVolunteer from "../Volunteer/Cargo_Volunteer";
import ResetPassword from "../Login/ResetPassword";
import RoundAdmin from "../Round/Round_Admin";
import VolunteerDashboard from "../Volunteer/Volunteer_Dashboard";
import BeforeLogin from "../Login/Before_Login";
import AdminUsers from "../Admin/Admin_Users";   
import AdminOrders from "../Admin/Admin_Orders";
import AdminViewAppli from "../Admin/Admin_ViewAppli";
import AdminFeedback from "../Admin/Admin_Feedback";
import CertificateHelper from "../../components/CertificateHelper";
import { checkTLSConnection } from "../../config/axiosConfig";

function App({ securityInitialized = false }) {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userData, setUserData] = useState({ username: "", userId: null, role: "" });
  const [tlsStatus, setTlsStatus] = useState(null);

  // Check TLS connection on app load
  useEffect(() => {
    const checkTLS = async () => {
      if (process.env.REACT_APP_USE_TLS === 'true') {
        const result = await checkTLSConnection();
        setTlsStatus(result);
        
        if (!result.success && process.env.REACT_APP_ENVIRONMENT === 'development') {
          console.warn('TLS connection check failed:', result.error);
        }
      }
    };
    
    checkTLS();
  }, []);

  // Rehydrate state when App mounts
  useEffect(() => {
    const storedUser = sessionStorage.getItem("auth_user");
    if (storedUser) {
      const user = JSON.parse(storedUser);
      setUserData(user);
      setIsLoggedIn(true);
    }
  }, []);

  // Save authentication state on successful login
  const handleLoginSuccess = (data) => {
    setIsLoggedIn(true);
    setUserData(data);
    sessionStorage.setItem("auth_user", JSON.stringify(data));
  };

  // On logout, clear the session data
  const handleLogout = () => {
    setIsLoggedIn(false);
    setUserData({ username: "", userId: null, role: "" });
    sessionStorage.removeItem("auth_user");
    localStorage.removeItem("ecdh_session_id");
  };

  // Handle profile updates
  const handleProfileUpdate = (updatedData) => {
    setUserData(updatedData);
    sessionStorage.setItem("auth_user", JSON.stringify(updatedData));
  };

  // Show security initialization error if needed
  if (!securityInitialized && process.env.REACT_APP_USE_AUTH === 'true') {
    return (
      <div
        style={{
          margin: "20px",
          padding: "20px",
          backgroundColor: "#ffebee",
          color: "#c62828",
          borderRadius: "4px",
          textAlign: "center",
        }}
      >
        <h2>Security Error</h2>
        <p>Secure connection could not be established.</p>
        {process.env.REACT_APP_ENVIRONMENT === 'development' && (
          <>
            <p>Make sure the backend is running on https://localhost:8443</p>
            <p>You may need to accept the self-signed certificate.</p>
            <button 
              onClick={() => window.open('https://localhost:8443/api/test/tls/status', '_blank')}
              style={{
                padding: '10px 20px',
                margin: '10px',
                backgroundColor: '#ff6b00',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Accept Certificate
            </button>
          </>
        )}
        <button 
          onClick={() => window.location.reload()}
          style={{
            padding: '10px 20px',
            margin: '10px',
            backgroundColor: '#2196F3',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Reload Page
        </button>
      </div>
    );
  }

  return (
    <Router>
      {/* Certificate Helper Component - shows only in development when needed */}
      <CertificateHelper />
      
      {/* TLS Status Indicator (optional - for debugging) */}
      {process.env.REACT_APP_DEBUG_MODE === 'true' && tlsStatus && (
        <div style={{
          position: 'fixed',
          bottom: '10px',
          right: '10px',
          padding: '5px 10px',
          backgroundColor: tlsStatus.success ? '#4caf50' : '#f44336',
          color: 'white',
          borderRadius: '4px',
          fontSize: '12px',
          zIndex: 9999
        }}>
          TLS: {tlsStatus.success ? '✓ Secure' : '✗ Not Connected'}
        </div>
      )}
      
      <Routes>
        <Route
          path="/"
          element={
            isLoggedIn ? (
              userData.username === "Guest" ? (
                <Guest onLogout={handleLogout} />
              ) : userData.role === "VOLUNTEER" ? (
                <VolunteerDashboard onLogout={handleLogout} userData={userData} />
              ) : userData.role === "ADMIN" ? (
                <Admin onLogout={handleLogout} userData={userData} />
              ) : (
                <Home
                  username={userData.username}
                  email={userData.email}
                  phone={userData.phone}
                  userId={userData.userId}
                  onLogout={handleLogout}
                />
              )
            ) : (
              <BeforeLogin />
            )
          }
        />
        <Route path="/login" element={<Login onLoginSuccess={handleLoginSuccess} />} /> 
        <Route path="/feedback" element={<HomeFeedback username={userData.username} />} />
        <Route 
          path="/profile" 
          element={
            <HomeProfile 
              username={userData.username} 
              email={userData.email} 
              phone={userData.phone} 
              userId={userData.userId} 
              onLogout={handleLogout}
              onProfileUpdate={handleProfileUpdate}
            />
          } 
        />
        <Route path="/orderhistory" element={<HomeOrderHistory userId={userData.userId} />} />
        <Route path="/register" element={<Register />} />
        <Route path="/guest" element={<Guest onLogout={handleLogout} />} />
        <Route path="/volunteerAppli" element={<VolunteerAppli />} />
        <Route path="/cargo_admin" element={<CargoAdmin userData={userData} />} />
        <Route path="/cargo_volunteer" element={<CargoVolunteer />} />
        <Route path="/reset_password" element={<ResetPassword />} />
        <Route path="/round_admin" element={<RoundAdmin />} />
        <Route path="/admin/users" element={<AdminUsers userData={userData} />} />
        <Route path="/admin/orders" element={<AdminOrders userData={userData} />} />
        <Route path="/admin/applications" element={<AdminViewAppli userData={userData} />} />
        <Route path="/admin/feedback" element={<AdminFeedback userData={userData} />} />
      </Routes>
    </Router>
  );
}

export default App;