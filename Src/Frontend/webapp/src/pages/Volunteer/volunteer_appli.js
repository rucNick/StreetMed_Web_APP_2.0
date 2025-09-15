import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { publicAxios } from '../../config/axiosConfig';

const VolunteerAppli = () => {
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [notes, setNotes] = useState('');

  const [firstNameError, setFirstNameError] = useState('');
  const [lastNameError, setLastNameError] = useState('');
  const [emailError, setEmailError] = useState('');
  const [phoneError, setPhoneError] = useState('');
  const [message, setMessage] = useState('');

  const validateFirstName = () => {
    if (!firstName.trim()) {
      setFirstNameError('First name is required');
      return false;
    }
    setFirstNameError('');
    return true;
  };

  const validateLastName = () => {
    if (!lastName.trim()) {
      setLastNameError('Last name is required');
      return false;
    }
    setLastNameError('');
    return true;
  };

  const validateEmail = () => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setEmailError('Invalid email format');
      return false;
    }
    setEmailError('');
    return true;
  };

  const validatePhone = () => {
    const phoneRegex = /^[\d\s\-+()]+$/;
    if (!phoneRegex.test(phone) || phone.trim() === '') {
      setPhoneError('Invalid phone number');
      return false;
    }
    setPhoneError('');
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const isFirstNameValid = validateFirstName();
    const isLastNameValid = validateLastName();
    const isEmailValid = validateEmail();
    const isPhoneValid = validatePhone();

    if (!isFirstNameValid || !isLastNameValid || !isEmailValid || !isPhoneValid) {
      return;
    }
    const submissionTime = Math.floor(Date.now() / 1000);

    const payload = {
      firstName,
      lastName,
      email,
      phone,
      notes,
      submissionTime
    };

    try {
      // Use publicAxios for volunteer application (public endpoint)
      const response = await publicAxios.post('/api/volunteer/apply', payload);
      if (response.data.status === 'success') {
        setMessage('Application submitted successfully!');
        setFirstName('');
        setLastName('');
        setEmail('');
        setPhone('');
        setNotes('');
      } else {
        setMessage(response.data.message || 'Submission failed');
      }
    } catch (error) {
      // Handle certificate errors if needed
      if (error.code === 'ERR_CERT_AUTHORITY_INVALID') {
        setMessage('Certificate error. Please accept the certificate and try again.');
        window.dispatchEvent(new CustomEvent('certificate-error', { 
          detail: { url: process.env.REACT_APP_BASE_URL }
        }));
      } else {
        setMessage(error.response?.data?.message || 'Submission failed');
      }
    }
  };

  const handleGoBack = () => {
    navigate(-1);
  };

  return (
    <div style={styles.pageContainer}>
      <header style={styles.navbar}>
        <div style={styles.navLeft}>
          <img
            src="/Untitled.png"
            alt="Logo"
            style={styles.logo}
          />
          <span style={styles.navTitle}>Street Med Go - JOIN US !!!</span>
        </div>
        <button style={styles.backButton} onClick={handleGoBack}>
          Go Back
        </button>
      </header>

      <div style={styles.volunteerContainer}>
        <div style={styles.volunteerCard}>
          <h2 style={styles.volunteerTitle}>Volunteer Application</h2>
          <form style={styles.volunteerForm} onSubmit={handleSubmit}>
            <div style={styles.inputPair}>
              <div style={styles.inputGroup}>
                <label style={styles.label}>First Name</label>
                <input
                  type="text"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  onBlur={validateFirstName}
                  style={styles.input}
                  required
                />
                {firstNameError && <p style={styles.errorText}>{firstNameError}</p>}
              </div>
              <div style={styles.inputGroup}>
                <label style={styles.label}>Last Name</label>
                <input
                  type="text"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  onBlur={validateLastName}
                  style={styles.input}
                  required
                />
                {lastNameError && <p style={styles.errorText}>{lastNameError}</p>}
              </div>
            </div>

            <div style={styles.inputPair}>
              <div style={styles.inputGroup}>
                <label style={styles.label}>Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  onBlur={validateEmail}
                  style={styles.input}
                  required
                />
                {emailError && <p style={styles.errorText}>{emailError}</p>}
              </div>
              <div style={styles.inputGroup}>
                <label style={styles.label}>Phone</label>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  onBlur={validatePhone}
                  style={styles.input}
                  required
                />
                {phoneError && <p style={styles.errorText}>{phoneError}</p>}
              </div>
            </div>

            <div style={styles.inputGroup}>
              <label style={styles.label}>Note</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                style={styles.textarea}
                placeholder="Enter additional information"
              />
            </div>

            <button type="submit" style={styles.volunteerSubmit}>
              Continue
            </button>
            {message && <p style={styles.message}>{message}</p>}
          </form>
        </div>
      </div>
    </div>
  );
};

const styles = {
  pageContainer: {
    width: '100%',
    minHeight: '100vh',
    backgroundColor: '#c8c9c7',
    display: 'flex',
    flexDirection: 'column'
  },
  navbar: {
    width: '100%',
    height: '83px',
    backgroundColor: '#d9d9d9',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 20px'
  },
  navLeft: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
  },
  logo: {
    width: '65px',
    height: '65px',
    borderRadius: '50%'
  },
  navTitle: {
    fontSize: '40px',
    fontWeight: 'bold',
    color: '#333'
  },
  backButton: {
    backgroundColor: '#003e7e',
    border: 'none',
    color: '#fff',
    cursor: 'pointer',
    fontWeight: 'bold',
    width: '100px',
    height: '40px',
    borderRadius: '8px',
    fontSize: '16px'
  },
  volunteerContainer: {
    flex: 1,
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    padding: '20px'
  },
  volunteerCard: {
    backgroundColor: '#d1dae2',
    borderRadius: '20px',
    padding: '50px 40px',
    width: '100%',
    maxWidth: '700px',
    boxShadow: '2px 4px 10px rgba(0, 0, 0, 0.1)',
    minHeight: '500px',
    display: 'flex',
    flexDirection: 'column'
  },
  volunteerTitle: {
    fontSize: '28px',
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: '40px',
    color: '#333'
  },
  volunteerForm: {
    display: 'flex',
    flexDirection: 'column',
    gap: '25px'
  },
  inputPair: {
    display: 'flex',
    gap: '20px',
    flexWrap: 'wrap'
  },
  inputGroup: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column'
  },
  label: {
    fontSize: '14px',
    fontWeight: '600',
    marginBottom: '6px',
    color: '#333'
  },
  input: {
    width: '100%',
    padding: '14px',
    fontSize: '14px',
    border: '1px solid #ccc',
    backgroundColor: '#fff',
    borderRadius: '6px'
  },
  textarea: {
    width: '100%',
    padding: '14px',
    fontSize: '14px',
    border: '1px solid #ccc',
    backgroundColor: '#fff',
    borderRadius: '6px',
    minHeight: '80px'
  },
  volunteerSubmit: {
    backgroundColor: '#003594',
    color: '#fff',
    border: 'none',
    padding: '14px',
    width: '200px',
    margin: '0 auto',
    borderRadius: '999px',
    fontSize: '16px',
    fontWeight: 'bold',
    cursor: 'pointer'
  },
  errorText: {
    color: 'red',
    fontSize: '12px',
    marginTop: '4px'
  },
  message: {
    textAlign: 'center',
    fontWeight: 'bold',
    marginTop: '10px'
  }
};

export default VolunteerAppli;