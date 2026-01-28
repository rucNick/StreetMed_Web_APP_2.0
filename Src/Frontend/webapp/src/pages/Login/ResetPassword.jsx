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
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const getAxios = () => {
    const isDev = import.meta.env.VITE_ENVIRONMENT === 'development'
    return isDev ? publicAxios : secureAxios
  }

  const handleGoBack = () => {
    navigate('/')
  }

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
        setMessage('If your email is registered, you will receive a recovery code')
        setStep(2)
      }
    } catch (err) {
      handleApiError(err, 'Failed to send recovery code')
    } finally {
      setIsLoading(false)
    }
  }

  const handleVerifyAndReset = async () => {
    setMessage('')
    setError('')
    
    if (!email.trim() || !otp.trim() || !newPassword.trim()) {
      setError('All fields are required')
      return
    }
    
    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match')
      return
    }
    
    setIsLoading(true)
    try {
      const axios = getAxios()
      
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
      
      const resetResponse = await axios.post('/api/auth/password/reset', {
        resetToken: resetToken,
        newPassword: newPassword.trim()
      })
      
      if (resetResponse.data.status === 'success') {
        setMessage('Password reset successfully!')
        setTimeout(() => {
          navigate('/login')
        }, 2000)
      } else {
        setError(resetResponse.data.message || 'Failed to reset password')
      }
    } catch (err) {
      handleApiError(err, 'Failed to reset password')
    } finally {
      setIsLoading(false)
    }
  }

  const handleResendCode = async () => {
    setError('')
    setMessage('')
    setIsLoading(true)
    
    try {
      const axios = getAxios()
      await axios.post('/api/auth/password/request-reset', {
        email: email.trim()
      })
      setMessage('A new recovery code has been sent to your email')
    } catch (err) {
      handleApiError(err, 'Failed to resend code')
    } finally {
      setIsLoading(false)
    }
  }

  const handleApiError = (err, defaultMessage) => {
    console.error('API Error:', err)
    
    if (err.code === 'ERR_CERT_AUTHORITY_INVALID' || 
        err.message?.includes('certificate')) {
      setError('Certificate error. Please accept the certificate and try again.')
      const certUrl = import.meta.env.VITE_SECURE_BASE_URL || 'https://localhost:8443'
      window.open(`${certUrl}/api/test/tls/status`, '_blank')
      return
    }
    
    if (err.response?.status === 403 && err.response?.data?.httpsRequired) {
      if (import.meta.env.VITE_ENVIRONMENT === 'development') {
        setError('HTTPS required. Please accept the certificate first.')
        const certUrl = import.meta.env.VITE_SECURE_BASE_URL || 'https://localhost:8443'
        window.open(`${certUrl}/api/test/tls/status`, '_blank')
      } else {
        setError('Secure connection required.')
      }
      return
    }
    
    if (err.code === 'ERR_NETWORK' || err.message === 'Network Error') {
      setError('Cannot connect to server. Please try again.')
      return
    }
    
    setError(err.response?.data?.message || defaultMessage)
  }

  return (
    <div style={styles.pageContainer}>
      {/* Header */}
      <div style={styles.header}>
        <button style={styles.goBackButton} onClick={handleGoBack}>
          Go Back
        </button>
      </div>

      {/* Main Content */}
      <div style={styles.contentWrapper}>
        <h1 style={styles.title}>Reset Your Password</h1>

        {/* Progress Steps */}
        <div style={styles.progressContainer}>
          <div style={styles.progressStep}>
            <div style={{
              ...styles.stepCircle,
              ...(step >= 1 ? styles.stepCircleActive : {})
            }}>
              {step > 1 ? '✓' : '1'}
            </div>
            <span style={{
              ...styles.stepLabel,
              ...(step >= 1 ? styles.stepLabelActive : {})
            }}>Email</span>
          </div>
          
          <div style={{
            ...styles.progressLine,
            ...(step >= 2 ? styles.progressLineActive : {})
          }} />
          
          <div style={styles.progressStep}>
            <div style={{
              ...styles.stepCircle,
              ...(step >= 2 ? styles.stepCircleActive : {})
            }}>
              2
            </div>
            <span style={{
              ...styles.stepLabel,
              ...(step >= 2 ? styles.stepLabelActive : {})
            }}>Reset</span>
          </div>
        </div>

        {/* Form Card */}
        <div style={styles.card}>
          {/* Step 1: Email Entry */}
          {step === 1 && (
            <>
              <h3 style={styles.stepTitle}>Step 1: Request a Recovery Code</h3>
              <p style={styles.stepDescription}>
                Enter your registered email address and we'll send you a 6-digit code.
              </p>

              <div style={styles.inputGroup}>
                <label style={styles.label}>Email</label>
                <input
                  style={styles.input}
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter your registered email"
                  disabled={isLoading}
                  onKeyPress={(e) => e.key === 'Enter' && handleRequestReset()}
                />
              </div>

              {error && <div style={styles.errorBox}>{error}</div>}
              {message && <div style={styles.successBox}>{message}</div>}

              <button 
                style={{
                  ...styles.primaryButton,
                  opacity: isLoading ? 0.7 : 1
                }}
                onClick={handleRequestReset}
                disabled={isLoading}
              >
                {isLoading ? 'Sending...' : 'Send Recovery Code'}
              </button>

              <div style={styles.footerLinks}>
                <span style={styles.footerText}>Remember your password?</span>
                <button 
                  style={styles.linkButton}
                  onClick={() => navigate('/login')}
                >
                  Sign In
                </button>
              </div>
            </>
          )}

          {/* Step 2: OTP and New Password */}
          {step === 2 && (
            <>
              <h3 style={styles.stepTitle}>Step 2: Enter OTP & New Password</h3>
              
              <div style={styles.emailBadge}>
                <span>📧</span>
                <span style={styles.emailText}>{email}</span>
                <button 
                  style={styles.changeEmailButton}
                  onClick={() => {
                    setStep(1)
                    setError('')
                    setMessage('')
                    setOtp('')
                    setNewPassword('')
                    setConfirmPassword('')
                  }}
                >
                  Change
                </button>
              </div>

              <div style={styles.inputGroup}>
                <div style={styles.labelRow}>
                  <label style={styles.label}>Recovery Code</label>
                  <button 
                    style={styles.resendButton}
                    onClick={handleResendCode}
                    disabled={isLoading}
                  >
                    Resend Code
                  </button>
                </div>
                <input
                  style={{...styles.input, ...styles.otpInput}}
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  placeholder="000000"
                  disabled={isLoading}
                  maxLength={6}
                />
              </div>

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
                  <div style={styles.passwordStrength}>
                    <div style={{
                      ...styles.strengthBar,
                      width: newPassword.length >= 8 ? '100%' : `${(newPassword.length / 8) * 100}%`,
                      backgroundColor: newPassword.length >= 8 ? '#27ae60' : '#f39c12'
                    }} />
                  </div>
                )}
              </div>

              <div style={styles.inputGroup}>
                <label style={styles.label}>Confirm Password</label>
                <input
                  style={styles.input}
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter password"
                  disabled={isLoading}
                  onKeyPress={(e) => e.key === 'Enter' && handleVerifyAndReset()}
                />
                {confirmPassword && newPassword && (
                  <span style={{
                    ...styles.matchIndicator,
                    color: newPassword === confirmPassword ? '#27ae60' : '#e74c3c'
                  }}>
                    {newPassword === confirmPassword ? 'Passwords match' : 'Passwords do not match'}
                  </span>
                )}
              </div>

              {error && <div style={styles.errorBox}>{error}</div>}
              {message && <div style={styles.successBox}>{message}</div>}

              <button 
                style={{
                  ...styles.primaryButton,
                  opacity: (isLoading || otp.length < 6 || newPassword.length < 8 || newPassword !== confirmPassword) ? 0.6 : 1
                }}
                onClick={handleVerifyAndReset}
                disabled={isLoading || otp.length < 6 || newPassword.length < 8 || newPassword !== confirmPassword}
              >
                {isLoading ? 'Resetting...' : 'Reset Password'}
              </button>

              <button 
                style={styles.secondaryButton}
                onClick={() => {
                  setStep(1)
                  setError('')
                  setMessage('')
                }}
                disabled={isLoading}
              >
                Back to Step 1
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

const styles = {
  pageContainer: {
    minHeight: '100vh',
    backgroundColor: '#C8C9C7',
    padding: '20px',
    boxSizing: 'border-box'
  },
  header: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginBottom: '10px'
  },
  goBackButton: {
    padding: '12px 24px',
    backgroundColor: '#003e7e',
    color: '#fff',
    border: 'none',
    borderRadius: '25px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '600',
    transition: 'all 0.2s ease'
  },
  contentWrapper: {
    maxWidth: '420px',
    margin: '0 auto',
    paddingTop: '10px'
  },
  title: {
    fontSize: '26px',
    fontWeight: '700',
    color: '#1a1a2e',
    margin: '0 0 25px 0',
    textAlign: 'center'
  },
  progressContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: '25px'
  },
  progressStep: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '6px'
  },
  stepCircle: {
    width: '32px',
    height: '32px',
    borderRadius: '50%',
    backgroundColor: '#bbb',
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '13px',
    fontWeight: '600',
    transition: 'all 0.3s ease'
  },
  stepCircleActive: {
    backgroundColor: '#003e7e'
  },
  stepLabel: {
    fontSize: '11px',
    color: '#888',
    fontWeight: '500',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  stepLabelActive: {
    color: '#003e7e'
  },
  progressLine: {
    width: '60px',
    height: '3px',
    backgroundColor: '#bbb',
    margin: '0 12px',
    marginBottom: '20px',
    borderRadius: '2px',
    transition: 'all 0.3s ease'
  },
  progressLineActive: {
    backgroundColor: '#003e7e'
  },
  card: {
    backgroundColor: '#1a1a2e',
    borderRadius: '16px',
    padding: '30px 28px',
    boxShadow: '0 8px 30px rgba(0, 0, 0, 0.12)'
  },
  stepTitle: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#f78702',
    margin: '0 0 8px 0'
  },
  stepDescription: {
    fontSize: '13px',
    color: 'rgba(255, 255, 255, 0.6)',
    margin: '0 0 20px 0',
    lineHeight: '1.5'
  },
  inputGroup: {
    marginBottom: '18px'
  },
  labelRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px'
  },
  label: {
    display: 'block',
    fontSize: '13px',
    fontWeight: '600',
    color: '#fff',
    marginBottom: '8px'
  },
  input: {
    width: '100%',
    padding: '14px 16px',
    fontSize: '14px',
    border: '2px solid transparent',
    borderRadius: '10px',
    backgroundColor: '#fff',
    color: '#333',
    boxSizing: 'border-box',
    transition: 'all 0.2s ease',
    outline: 'none'
  },
  otpInput: {
    fontSize: '22px',
    letterSpacing: '10px',
    textAlign: 'center',
    fontFamily: 'monospace',
    fontWeight: '600'
  },
  emailBadge: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    padding: '10px 14px',
    borderRadius: '8px',
    marginBottom: '20px'
  },
  emailText: {
    flex: 1,
    color: '#fff',
    fontSize: '13px',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  changeEmailButton: {
    background: 'none',
    border: 'none',
    color: '#f78702',
    fontSize: '12px',
    fontWeight: '600',
    cursor: 'pointer',
    padding: '4px 8px'
  },
  resendButton: {
    background: 'none',
    border: 'none',
    color: '#f78702',
    fontSize: '12px',
    fontWeight: '600',
    cursor: 'pointer',
    padding: '0',
    marginBottom: '8px'
  },
  passwordStrength: {
    height: '4px',
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    borderRadius: '2px',
    marginTop: '8px',
    overflow: 'hidden'
  },
  strengthBar: {
    height: '100%',
    borderRadius: '2px',
    transition: 'all 0.3s ease'
  },
  matchIndicator: {
    fontSize: '12px',
    marginTop: '6px',
    display: 'block'
  },
  errorBox: {
    backgroundColor: 'rgba(231, 76, 60, 0.15)',
    border: '1px solid rgba(231, 76, 60, 0.3)',
    color: '#ff6b6b',
    padding: '12px 14px',
    borderRadius: '8px',
    fontSize: '13px',
    marginBottom: '16px'
  },
  successBox: {
    backgroundColor: 'rgba(39, 174, 96, 0.15)',
    border: '1px solid rgba(39, 174, 96, 0.3)',
    color: '#2ecc71',
    padding: '12px 14px',
    borderRadius: '8px',
    fontSize: '13px',
    marginBottom: '16px'
  },
  primaryButton: {
    width: '100%',
    padding: '14px',
    fontSize: '15px',
    fontWeight: '600',
    border: 'none',
    borderRadius: '10px',
    backgroundColor: '#f78702',
    color: '#fff',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
    marginTop: '5px'
  },
  secondaryButton: {
    width: '100%',
    padding: '12px',
    marginTop: '12px',
    fontSize: '14px',
    fontWeight: '600',
    border: '2px solid rgba(255, 255, 255, 0.2)',
    borderRadius: '10px',
    backgroundColor: 'transparent',
    color: 'rgba(255, 255, 255, 0.7)',
    cursor: 'pointer',
    transition: 'all 0.2s ease'
  },
  footerLinks: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    marginTop: '20px',
    paddingTop: '18px',
    borderTop: '1px solid rgba(255, 255, 255, 0.1)'
  },
  footerText: {
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: '13px'
  },
  linkButton: {
    background: 'none',
    border: 'none',
    color: '#f78702',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    padding: '0'
  }
}

export default Reset_Password