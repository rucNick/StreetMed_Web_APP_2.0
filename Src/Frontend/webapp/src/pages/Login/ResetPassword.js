// File: Reset_Password.js
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { secureAxios, publicAxios } from '../../config/axiosConfig'
import '../../index.css'; 

const Reset_Password = () => {
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  // Use publicAxios (HTTP) in development for non-sensitive requests
  const getAxios = () => {
    const isDev = process.env.REACT_APP_ENVIRONMENT === 'development'
    return isDev ? publicAxios : secureAxios
  }

  const handleGoBack = () => {
    navigate('/')
  }

  // Step 1: Request password reset - sends OTP to email
  const handleRequestReset = async () => {
    setMessage('')
    setError('')
    if (!email.trim()) {
      setError('Email is required')
      return
    }
    
    setIsLoading(true)
    try {
      const axios = getAxios()
      const response = await axios.post('/api/auth/password/request-reset', {
        email: email.trim()
      })
      
      if (response.data.status === 'success') {
        setMessage(response.data.message || 'Recovery code sent to your email')
        setStep(2)
      } else {
        // Show generic message for security (don't reveal if email exists)
        setMessage('If your email is registered, you will receive a recovery code')
        setStep(2)
      }
    } catch (err) {
      handleApiError(err, 'Failed to send recovery code')
    } finally {
      setIsLoading(false)
    }
  }

  // Step 2: Verify OTP AND reset password (calls two backend endpoints in sequence)
  const handleVerifyAndReset = async () => {
    setMessage('')
    setError('')
    
    if (!email.trim() || !otp.trim() || !newPassword.trim()) {
      setError('Email, OTP, and new password are required.')
      return
    }
    
    // Validate password strength
    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long')
      return
    }
    
    setIsLoading(true)
    try {
      const axios = getAxios()
      
      // First: Verify OTP and get reset token
      const verifyResponse = await axios.post('/api/auth/password/verify-otp', {
        email: email.trim(),
        otp: otp.trim()
      })
      
      if (verifyResponse.data.status !== 'success') {
        setError(verifyResponse.data.message || 'Invalid or expired OTP')
        setIsLoading(false)
        return
      }
      
      const resetToken = verifyResponse.data.resetToken
      
      if (!resetToken) {
        setError('Failed to get reset token. Please try again.')
        setIsLoading(false)
        return
      }
      
      // Second: Reset password using the token
      const resetResponse = await axios.post('/api/auth/password/reset', {
        resetToken: resetToken,
        newPassword: newPassword.trim()
      })
      
      if (resetResponse.data.status === 'success') {
        setMessage('Password reset successfully! Redirecting to login...')
        setTimeout(() => {
          navigate('/login')
        }, 2000)
      } else {
        setError(resetResponse.data.message || 'Failed to reset password.')
      }
    } catch (err) {
      handleApiError(err, 'Failed to reset password')
    } finally {
      setIsLoading(false)
    }
  }

  // Centralized error handling
  const handleApiError = (err, defaultMessage) => {
    console.error('API Error:', err)
    
    // Handle certificate errors
    if (err.code === 'ERR_CERT_AUTHORITY_INVALID' || 
        err.message?.includes('certificate')) {
      setError('Certificate error. Please accept the self-signed certificate and try again.')
      const certUrl = process.env.REACT_APP_SECURE_BASE_URL || 'https://localhost:8443'
      window.open(`${certUrl}/api/test/tls/status`, '_blank')
      return
    }
    
    // Handle HTTPS required errors - don't redirect browser, just show message
    if (err.response?.status === 403 && err.response?.data?.httpsRequired) {
      if (process.env.REACT_APP_ENVIRONMENT === 'development') {
        setError('HTTPS required. Please accept the certificate first.')
        const certUrl = process.env.REACT_APP_SECURE_BASE_URL || 'https://localhost:8443'
        window.open(`${certUrl}/api/test/tls/status`, '_blank')
      } else {
        setError('Secure connection required for password recovery.')
      }
      return
    }
    
    // Handle network errors
    if (err.code === 'ERR_NETWORK' || err.message === 'Network Error') {
      setError('Cannot connect to server. Please ensure the backend is running.')
      return
    }
    
    // Default error message
    setError(err.response?.data?.message || defaultMessage)
  }

  return (
    <div style={styles.container}>
      <div style={styles.topBar}>
        <button style={styles.goBackButton} onClick={handleGoBack}>
          Go Back
        </button>
      </div>
      <h2 style={styles.title}>Reset Your Password</h2>
      
      {/* Step 1: Request Reset */}
      {step === 1 && (
        <div style={styles.formContainer}>
          <h3 style={styles.stepTitle}>Step 1: Request a Recovery Code</h3>
          <label style={styles.label}>Email:</label>
          <input
            style={styles.input}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your registered email"
            disabled={isLoading}
            onKeyPress={(e) => e.key === 'Enter' && handleRequestReset()}
          />
          {error && <p style={styles.errorText}>{error}</p>}
          {message && <p style={styles.successText}>{message}</p>}
          <button 
            style={{...styles.actionButton, opacity: isLoading ? 0.6 : 1}}
            onClick={handleRequestReset}
            disabled={isLoading}
          >
            {isLoading ? 'Sending...' : 'Send Recovery Code'}
          </button>
        </div>
      )}
      
      {/* Step 2: Enter OTP & New Password */}
      {step === 2 && (
        <div style={styles.formContainer}>
          <h3 style={styles.stepTitle}>Step 2: Enter OTP & New Password</h3>
          <label style={styles.label}>Email:</label>
          <input
            style={styles.input}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Re-enter your email"
            disabled={isLoading}
          />
          <label style={styles.label}>OTP (Recovery Code):</label>
          <input
            style={styles.input}
            type="text"
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
            placeholder="Enter the code you received"
            disabled={isLoading}
          />
          <label style={styles.label}>New Password:</label>
          <input
            style={styles.input}
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="Enter your new password (min 8 characters)"
            disabled={isLoading}
            onKeyPress={(e) => e.key === 'Enter' && handleVerifyAndReset()}
          />
          {error && <p style={styles.errorText}>{error}</p>}
          {message && <p style={styles.successText}>{message}</p>}
          <button 
            style={{...styles.actionButton, opacity: isLoading ? 0.6 : 1}}
            onClick={handleVerifyAndReset}
            disabled={isLoading}
          >
            {isLoading ? 'Resetting...' : 'Reset Password'}
          </button>
          <button 
            style={styles.backButton}
            onClick={() => {
              setStep(1)
              setError('')
              setMessage('')
            }}
            disabled={isLoading}
          >
            Back to Step 1
          </button>
        </div>
      )}
    </div>
  )
}

const styles = {
  container: {
    position: 'relative',
    minHeight: '100vh',
    padding: '20px',
    textAlign: 'center',
    backgroundColor: '#C8C9C7'
  },
  topBar: {
    position: 'absolute',
    top: '20px',
    right: '20px'
  },
  goBackButton: {
    padding: '10px 20px',
    backgroundColor: '#003e7e',
    color: '#fff',
    border: 'none',
    borderRadius: '25px',
    cursor: 'pointer'
  },
  title: {
    fontSize: '24px',
    marginTop: '60px',
    color: '#333'
  },
  formContainer: {
    display: 'inline-block',
    textAlign: 'left',
    backgroundColor: '#D1DAE2',
    padding: '30px',
    borderRadius: '25px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
    marginTop: '40px',
    width: '100%',
    maxWidth: '420px'
  },
  stepTitle: {
    fontSize: '18px',
    marginBottom: '20px',
    color: '#333'
  },
  label: {
    display: 'block',
    fontSize: '14px',
    fontWeight: '600',
    marginBottom: '6px',
    color: '#333'
  },
  input: {
    width: '100%',
    padding: '12px',
    marginBottom: '16px',
    fontSize: '14px',
    border: '1px solid #ccc',
    borderRadius: '6px',
    boxSizing: 'border-box'
  },
  actionButton: {
    width: '100%',
    padding: '14px',
    fontSize: '16px',
    fontWeight: '600',
    border: 'none',
    borderRadius: '25px',
    backgroundColor: '#f78702',
    color: '#fff',
    cursor: 'pointer',
    transition: 'opacity 0.3s'
  },
  backButton: {
    width: '100%',
    padding: '14px',
    marginTop: '10px',
    fontSize: '14px',
    fontWeight: '600',
    border: '2px solid #003e7e',
    borderRadius: '25px',
    backgroundColor: 'transparent',
    color: '#003e7e',
    cursor: 'pointer'
  },
  errorText: {
    color: '#c0392b',
    marginBottom: '10px',
    fontSize: '14px'
  },
  successText: {
    color: '#27ae60',
    marginBottom: '10px',
    fontSize: '14px',
    fontWeight: '600'
  }
}

export default Reset_Password