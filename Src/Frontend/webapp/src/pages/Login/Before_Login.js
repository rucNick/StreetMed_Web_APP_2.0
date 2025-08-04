import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "../../css/Login/Before_Login.css";

function Before_Login() {
  const navigate = useNavigate();

  // const handleForgetPasswordClick = (e) => {
  //   e.preventDefault();
  //   navigate("/reset_password");
  // };

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

  // useEffect hook to handle the mobile menu toggle
  useEffect(() => {
    const menuToggle = document.querySelector(".mobile-menu-toggle");
    const navList = document.querySelector(".nav-list");
    
    if (menuToggle && navList) {
      const handleToggle = () => {
        navList.classList.toggle("nav-active");
      };
      
      menuToggle.addEventListener("click", handleToggle);
      
      // Clean up the event listener when component unmounts
      return () => {
        menuToggle.removeEventListener("click", handleToggle);
      };
    }
  }, []);

  return (
    <div className="page-container">
      {/* nav bar */}
      <header className="site-header">
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
                {/* <li className="nav-item">
                  <button
                    type="button"
                    className="nav-link"
                    onClick={handleForgetPasswordClick}
                  >
                    Forget Password
                  </button>
                </li> */}
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
              {/* mobile navbar */}
              <button className="mobile-menu-toggle" aria-label="Toggle menu">
                <span className="menu-bar"></span>
                <span className="menu-bar"></span>
                <span className="menu-bar"></span>
              </button>
            </nav>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="content-wrapper">
          <article className="feature-article">
            <img
              src="/loginPic1.png" 
              alt="Street Medicine at Pitt"
              className="feature-image"
            />
            <div className="article-content">
              <h2 className="article-title">
                Street Medicine at Pitt serves Pittsburghers without homes — and
                educates future medical professionals
              </h2>
              <p className="article-text">
                It's a little after 6 p.m. on a Wednesday in January, and student
                members of Street Medicine at Pitt form a huddle on a downtown
                Pittsburgh sidewalk. They're about to start their evening rounds,
                providing health care, cold weather supplies and a listening ear to
                men who are experiencing homelessness and are waiting to enter the
                Winter Shelter at the Smithfield United Church of Christ. The
                evening's weather is 40 degrees with clear skies, but the
                temperature is expected to drop into the single digits the next day,
                increasing outdoor sleepers' risks for frostbite and hypothermia.
              </p>
            </div>
          </article>

          <section className="content-blocks">
            <article className="content-block beige-block">
              <img
                src="/loginPic2.png"
                alt="Pic 1"
                className="block-image"
              />
              <div className="block-content">
                <h3 className="block-title">Volunteering</h3>
                <p className="block-text">
                Volunteer and engage with other organizations supporting the homeless population in Pittsburgh.
                </p>
              </div>
            </article>

            <article className="content-block blue-block">
              <div className="block-content">
                <h3 className="block-title">Volunteering</h3>
                <p className="block-text">
                  Volunteer and engage with other organizations supporting the homeless population in Pittsburgh.
                </p>
              </div>
              <img
                src="/loginPic3.png"
                alt="Pic 2"
                className="block-image"
              />
            </article>
          </section>
        </div>
      </main>

      {/* mobile adapt*/}
      <script
        dangerouslySetInnerHTML={{
          __html: `
            document.querySelector(".mobile-menu-toggle").addEventListener("click", function () {
            document.querySelector(".nav-list").classList.toggle("nav-active");
          });
          `,
        }}
      />
    </div>
  );
}

export default Before_Login;
