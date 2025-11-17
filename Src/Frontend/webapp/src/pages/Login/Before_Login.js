import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import "../../css/Login/Before_Login.css";

function Before_Login() {
  const navigate = useNavigate();

  const handleVolunteerApplicationClick = (e) => {
    e.preventDefault();
    navigate("/volunteerAppli");
  };

  const handleGuestClick = (e) => {
    e.preventDefault();
    navigate("/guest");
  };

  const handleLoginClick = (e) => {
    e.preventDefault();
    navigate("/login");
  };

  const handleSignUpClick = (e) => {
    e.preventDefault();
    navigate("/register");
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
      
      {/* <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img
              src="/Untitled.png"
              alt="Pitt Street Medicine Logo"
              className="logo"
            />
            <h1 className="site-title">Street Med Go</h1>
          </div>
          <div className="header-right">
            <nav className="main-nav">
              <ul className="nav-list">
                <li className="nav-item">
                  <button
                    type="button"
                    className="nav-link"
                    onClick={handleVolunteerApplicationClick}
                  >
                    Volunteer Application
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className="nav-link"
                    onClick={handleGuestClick}
                  >
                    Guest
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className="nav-link"
                    onClick={handleLoginClick}
                  >
                    Login
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    type="button"
                    className="nav-link"
                    onClick={handleSignUpClick}
                  >
                    Sign up
                  </button>
                </li>
              </ul>
              <button className="mobile-menu-toggle" aria-label="Toggle menu">
                <span className="menu-bar"></span>
                <span className="menu-bar"></span>
                <span className="menu-bar"></span>
              </button>
            </nav>
          </div>
        </div>
      </header> */}

      <main className="login-page-content">
        <div className="login-card">
          <img 
              src="/Untitled.png" // The path to your logo image
              alt="Street Med Go Logo" 
              className="login-card-logo" // A new CSS class for styling
            />
          <h2 className="login-page-title">Street Med Go</h2>
          <p className="login-page-subtitle"></p>

          <div className="button-row"> {/* <--- NEW: This is the container for the two buttons */}
            <button className="login-option-btn primary-option" onClick={handleLoginClick}>
              Log In
            </button>
            <button className="login-option-btn secondary-option" onClick={handleSignUpClick}>
              Sign Up
            </button>
          </div>

          {/* <div className="divider">or</div> */}
          <button className="login-option-btn guest-option" onClick={handleGuestClick}>
            Continue as Guest
          </button>
          <div className="divider_bottom"> </div>
            <button className="login-option-btn request-supplies-btn">
              Request Supplies
            </button>
        </div>
      </main>
    </div>
  );
}

export default Before_Login;