import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import '../../index.css'; 

function Before_Login() {
  const navigate = useNavigate();
  const [showSuppliesModal, setShowSuppliesModal] = useState(false);

  const handleGuestClick = (e) => {
    e.preventDefault();
    setShowSuppliesModal(false);
    navigate("/guest");
  };

  const handleLoginClick = (e) => {
    e.preventDefault();
    setShowSuppliesModal(false);
    navigate("/login");
  };

  const handleSignUpClick = (e) => {
    e.preventDefault();
    setShowSuppliesModal(false);
    navigate("/register");
  };

  const handleRequestSuppliesClick = (e) => {
    e.preventDefault();
    setShowSuppliesModal(true);
  };

  const handleCloseModal = () => {
    setShowSuppliesModal(false);
  };

  // Close modal when clicking outside
  const handleOverlayClick = (e) => {
    if (e.target.classList.contains('supplies-modal-overlay')) {
      setShowSuppliesModal(false);
    }
  };

  // Your original mobile menu toggle logic
  useEffect(() => {
    const menuToggle = document.querySelector(".mobile-menu-toggle");
    const navList = document.querySelector(".nav-list");

    if (menuToggle && navList) {
      const handleToggle = () => {
        navList.classList.toggle("nav-active");
      };

      menuToggle.addEventListener("click", handleToggle);

      return () => {
        menuToggle.removeEventListener("click", handleToggle);
      };
    }
  }, []);

  return (
    <div className="full-screen-wrapper">
      <main className="login-page-content">
        <div className="login-card">
          <img 
              src="/Untitled.png"
              alt="Street Med Go Logo" 
              className="login-card-logo"
            />
          <h2 className="login-page-title">Street Med Go</h2>
          <p className="login-page-subtitle"></p>

          <div className="button-row">
            <button className="login-option-btn primary-option" onClick={handleLoginClick}>
              Log In
            </button>
            <button className="login-option-btn secondary-option" onClick={handleSignUpClick}>
              Sign Up
            </button>
          </div>

          <div className="divider_bottom"> </div>
          <button className="login-option-btn guest-option" onClick={handleRequestSuppliesClick}>
            Request Supplies
          </button>
        </div>
      </main>

      {/* Request Supplies Modal */}
      {showSuppliesModal && (
        <div className="supplies-modal-overlay" onClick={handleOverlayClick}>
          <div className="supplies-modal">
            <button className="supplies-modal-close" onClick={handleCloseModal}>
              &times;
            </button>
            <h3 className="supplies-modal-title">Do you have an account?</h3>
            <p className="supplies-modal-subtitle">
              Log in or sign up to track your requests and get faster service.
            </p>
            
            <div className="supplies-modal-buttons">
              <button className="login-option-btn primary-option" onClick={handleLoginClick}>
                Yes, Log In
              </button>
              <button className="login-option-btn secondary-option" onClick={handleSignUpClick}>
                No, Sign Up
              </button>
            </div>
            
            <div className="supplies-modal-divider">
              <span>or</span>
            </div>
            
            <button className="login-option-btn guest-option" onClick={handleGuestClick}>
              Continue as Guest
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Before_Login;