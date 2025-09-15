import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { secureAxios } from '../../config/axiosConfig';
import '../../css/Admin/Admin_Feedback.css';

const AdminFeedback = ({ userData }) => {
  const navigate = useNavigate();

  const [feedbacks, setFeedbacks] = useState([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [selectedFeedback, setSelectedFeedback] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const loadFeedback = useCallback(async () => {
    try {
      setIsLoading(true);
      setError('');
      
      // Use secureAxios for admin operations (HTTPS required)
      const resp = await secureAxios.get('/api/feedback/all', {
        headers: {
          "Admin-Username": userData.username,
          "Authentication-Status": "true"
        }
      });
      
      setFeedbacks(resp.data.data || []);
    } catch (e) {
      console.error("Error loading feedback:", e);
      if (e.response?.data?.httpsRequired) {
        setError("Secure HTTPS connection required for admin operations.");
      } else {
        setError(e.response?.data?.message || e.message);
      }
    } finally {
      setIsLoading(false);
    }
  }, [userData.username]);

  const deleteFeedback = async (feedbackId) => {
    if (!window.confirm('Are you sure you want to delete this feedback?')) {
      return;
    }
    
    try {
      const resp = await secureAxios.delete(`/api/feedback/${feedbackId}`, {
        headers: {
          "Admin-Username": userData.username,
          "Authentication-Status": "true"
        }
      });
      
      alert(resp.data.message || 'Feedback deleted successfully');
      loadFeedback();
    } catch (e) {
      console.error("Error deleting feedback:", e);
      alert(e.response?.data?.message || 'Failed to delete feedback');
    }
  };

  const markAsRead = async (feedbackId) => {
    try {
      await secureAxios.put(`/api/feedback/${feedbackId}/read`, {
        adminUsername: userData.username,
        authenticated: "true"
      });
      
      loadFeedback();
    } catch (e) {
      console.error("Error marking feedback as read:", e);
    }
  };

  const viewFeedbackDetails = (feedback) => {
    setSelectedFeedback(feedback);
    setShowModal(true);
    
    // Mark as read when viewing
    if (!feedback.isRead) {
      markAsRead(feedback.id);
    }
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedFeedback(null);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleString();
    } catch {
      return dateString;
    }
  };

  const exportFeedback = () => {
    try {
      const csvContent = [
        ['ID', 'Name', 'Phone', 'Content', 'Created At'],
        ...feedbacks.map(f => [
          f.id,
          f.name,
          f.phoneNumber,
          f.content.replace(/,/g, ';'), // Replace commas to avoid CSV issues
          formatDate(f.createdAt)
        ])
      ].map(row => row.join(',')).join('\n');
      
      const blob = new Blob([csvContent], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `feedback_export_${new Date().toISOString().split('T')[0]}.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      console.error("Error exporting feedback:", e);
      alert('Failed to export feedback');
    }
  };

  useEffect(() => {
    loadFeedback();
  }, [loadFeedback]);

  // Calculate statistics
  const totalFeedback = feedbacks.length;
  const unreadFeedback = feedbacks.filter(f => !f.isRead).length;
  const todaysFeedback = feedbacks.filter(f => {
    const feedbackDate = new Date(f.createdAt).toDateString();
    const today = new Date().toDateString();
    return feedbackDate === today;
  }).length;

  return (
    <div className="page-container">
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">Feedback Management System</span>
          </div>
          <div className="header-right">
            <button className="manage-btn" onClick={() => navigate(-1)}>
              ← Go Back
            </button>
            <button 
              className="manage-btn" 
              onClick={loadFeedback}
              disabled={isLoading}
              style={{ marginLeft: '10px' }}
            >
              {isLoading ? 'Refreshing...' : 'Refresh'}
            </button>
            {feedbacks.length > 0 && (
              <button 
                className="manage-btn" 
                onClick={exportFeedback}
                style={{ marginLeft: '10px' }}
              >
                Export CSV
              </button>
            )}
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="cargo-container">
          <h1 className="cargo-title">Customer Feedback</h1>
          
          {/* Statistics Cards */}
          <div style={{ 
            display: 'flex', 
            gap: '20px', 
            marginBottom: '20px',
            flexWrap: 'wrap'
          }}>
            <div style={{ 
              flex: 1,
              minWidth: '150px',
              padding: '15px', 
              backgroundColor: '#e3f2fd', 
              borderRadius: '8px',
              textAlign: 'center'
            }}>
              <h3 style={{ margin: '0 0 10px 0', color: '#1976d2' }}>Total</h3>
              <p style={{ margin: 0, fontSize: '24px', fontWeight: 'bold' }}>
                {totalFeedback}
              </p>
            </div>
            <div style={{ 
              flex: 1,
              minWidth: '150px',
              padding: '15px', 
              backgroundColor: '#fff3e0', 
              borderRadius: '8px',
              textAlign: 'center'
            }}>
              <h3 style={{ margin: '0 0 10px 0', color: '#f57c00' }}>Unread</h3>
              <p style={{ margin: 0, fontSize: '24px', fontWeight: 'bold' }}>
                {unreadFeedback}
              </p>
            </div>
            <div style={{ 
              flex: 1,
              minWidth: '150px',
              padding: '15px', 
              backgroundColor: '#e8f5e9', 
              borderRadius: '8px',
              textAlign: 'center'
            }}>
              <h3 style={{ margin: '0 0 10px 0', color: '#388e3c' }}>Today</h3>
              <p style={{ margin: 0, fontSize: '24px', fontWeight: 'bold' }}>
                {todaysFeedback}
              </p>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div style={{ 
              padding: '10px', 
              margin: '10px 0', 
              backgroundColor: '#ffebee', 
              color: '#c62828', 
              borderRadius: '4px' 
            }}>
              Error: {error}
            </div>
          )}

          {/* Feedback Table */}
          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">All Feedback</div>
            {isLoading ? (
              <div style={{ padding: '20px', textAlign: 'center' }}>
                Loading feedback...
              </div>
            ) : feedbacks.length === 0 ? (
              <div style={{ padding: '20px', textAlign: 'center' }}>
                No feedback received yet.
              </div>
            ) : (
              <table className="cargo-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Status</th>
                    <th>Name</th>
                    <th>Phone</th>
                    <th>Content</th>
                    <th>Created At</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {feedbacks.map(f => (
                    <tr 
                      key={f.id}
                      style={{ 
                        backgroundColor: f.isRead ? 'transparent' : '#fffde7',
                        cursor: 'pointer'
                      }}
                      onClick={() => viewFeedbackDetails(f)}
                    >
                      <td>{f.id}</td>
                      <td>
                        {f.isRead ? (
                          <span style={{ color: '#4caf50' }}>✓ Read</span>
                        ) : (
                          <span style={{ color: '#ff9800', fontWeight: 'bold' }}>
                            • New
                          </span>
                        )}
                      </td>
                      <td>{f.name || 'Anonymous'}</td>
                      <td>{f.phoneNumber || 'N/A'}</td>
                      <td style={{ 
                        maxWidth: '300px', 
                        overflow: 'hidden', 
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }}>
                        {f.content}
                      </td>
                      <td>{formatDate(f.createdAt)}</td>
                      <td onClick={e => e.stopPropagation()}>
                        <button
                          className="manage-btn"
                          onClick={() => viewFeedbackDetails(f)}
                          style={{ marginRight: '5px' }}
                        >
                          View
                        </button>
                        <button
                          className="manage-btn"
                          onClick={() => deleteFeedback(f.id)}
                          style={{ backgroundColor: '#f44336', color: 'white' }}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </main>

      {/* Feedback Detail Modal */}
      {showModal && selectedFeedback && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            borderRadius: '8px',
            padding: '20px',
            maxWidth: '600px',
            width: '90%',
            maxHeight: '80vh',
            overflow: 'auto'
          }}>
            <h2>Feedback Details</h2>
            <div style={{ marginBottom: '15px' }}>
              <strong>ID:</strong> {selectedFeedback.id}
            </div>
            <div style={{ marginBottom: '15px' }}>
              <strong>Name:</strong> {selectedFeedback.name || 'Anonymous'}
            </div>
            <div style={{ marginBottom: '15px' }}>
              <strong>Phone Number:</strong> {selectedFeedback.phoneNumber || 'N/A'}
            </div>
            <div style={{ marginBottom: '15px' }}>
              <strong>Created At:</strong> {formatDate(selectedFeedback.createdAt)}
            </div>
            <div style={{ marginBottom: '15px' }}>
              <strong>Status:</strong> {selectedFeedback.isRead ? 'Read' : 'Unread'}
            </div>
            <div style={{ marginBottom: '15px' }}>
              <strong>Content:</strong>
              <div style={{ 
                marginTop: '10px',
                padding: '10px',
                backgroundColor: '#f5f5f5',
                borderRadius: '4px',
                whiteSpace: 'pre-wrap'
              }}>
                {selectedFeedback.content}
              </div>
            </div>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button 
                className="manage-btn"
                onClick={() => deleteFeedback(selectedFeedback.id)}
                style={{ backgroundColor: '#f44336', color: 'white' }}
              >
                Delete
              </button>
              <button 
                className="manage-btn"
                onClick={closeModal}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminFeedback;