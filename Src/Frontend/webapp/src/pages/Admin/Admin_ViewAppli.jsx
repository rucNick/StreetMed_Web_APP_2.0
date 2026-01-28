import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { secureAxios } from '../../config/axiosConfig';
import '../../index.css'; 

const AdminViewAppli = ({ userData }) => {
  const navigate = useNavigate();

  const [applications, setApplications] = useState({ 
    pending: [], 
    approved: [], 
    rejected: [] 
  });
  const [applicationsError, setApplicationsError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [processingId, setProcessingId] = useState(null);

  const loadApplications = useCallback(async () => {
    try {
      setIsLoading(true);
      setApplicationsError('');
      
      // Use secureAxios for admin operations (HTTPS required)
      const response = await secureAxios.get('/api/volunteer/applications', {
        headers: {
          "Admin-Username": userData.username,
          "Authentication-Status": "true"
        }
      });
      
      setApplications(response.data.data || { 
        pending: [], 
        approved: [], 
        rejected: [] 
      });
    } catch (error) {
      console.error("Error loading applications:", error);
      if (error.response?.data?.httpsRequired) {
        setApplicationsError("Secure HTTPS connection required for admin operations.");
      } else {
        setApplicationsError(error.response?.data?.message || error.message);
      }
    } finally {
      setIsLoading(false);
    }
  }, [userData.username]);

  const approveApplication = async (applicationId) => {
    if (!window.confirm('Approve this volunteer application?')) {
      return;
    }
    
    try {
      setProcessingId(applicationId);
      
      const resp = await secureAxios.post('/api/volunteer/approve', {
        adminUsername: userData.username,
        authenticated: "true",
        applicationId: applicationId.toString()
      });
      
      alert(resp.data.message || 'Application approved successfully');
      loadApplications();
    } catch (e) {
      console.error("Error approving application:", e);
      alert(e.response?.data?.message || e.message);
    } finally {
      setProcessingId(null);
    }
  };

  const rejectApplication = async (applicationId) => {
    if (!window.confirm('Reject this volunteer application?')) {
      return;
    }
    
    try {
      setProcessingId(applicationId);
      
      const resp = await secureAxios.post('/api/volunteer/reject', {
        adminUsername: userData.username,
        authenticated: "true",
        applicationId: applicationId.toString()
      });
      
      alert(resp.data.message || 'Application rejected');
      loadApplications();
    } catch (e) {
      console.error("Error rejecting application:", e);
      alert(e.response?.data?.message || e.message);
    } finally {
      setProcessingId(null);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleString();
    } catch {
      return dateString;
    }
  };

  useEffect(() => {
    loadApplications();
  }, [loadApplications]);

  return (
    <div className="page-container">
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">Volunteer Application Management</span>
          </div>
          <div className="header-right">
            <button className="manage-btn" onClick={() => navigate(-1)}>
              ← Go Back
            </button>
            <button 
              className="manage-btn" 
              onClick={loadApplications}
              disabled={isLoading}
              style={{ marginLeft: '10px' }}
            >
              {isLoading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="cargo-container">
          <div className="cargo-header">
            <h1 className="cargo-title">Volunteer Applications</h1>
          </div>

          {/* Error Message */}
          {applicationsError && (
            <div style={{ 
              padding: '10px', 
              margin: '10px 0', 
              backgroundColor: '#ffebee', 
              color: '#c62828', 
              borderRadius: '4px' 
            }}>
              Error: {applicationsError}
            </div>
          )}

          {/* Loading State */}
          {isLoading && (
            <div style={{ 
              padding: '20px', 
              textAlign: 'center',
              backgroundColor: '#f5f5f5',
              borderRadius: '4px',
              margin: '10px 0'
            }}>
              Loading applications...
            </div>
          )}

          {/* Pending Applications */}
          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">
              Pending Applications ({applications.pending.length})
            </div>
            {applications.pending.length === 0 ? (
              <div style={{ padding: '20px', textAlign: 'center' }}>
                No pending applications
              </div>
            ) : (
              <table className="cargo-table">
                <thead>
                  <tr>
                    <th>Application ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Notes</th>
                    <th>Submitted</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {applications.pending.map((app) => (
                    <tr key={app.applicationId}>
                      <td>{app.applicationId}</td>
                      <td>{`${app.firstName || ''} ${app.lastName || ''}`.trim() || 'N/A'}</td>
                      <td>{app.email || 'N/A'}</td>
                      <td>{app.phone || 'N/A'}</td>
                      <td>{app.notes || 'N/A'}</td>
                      <td>{formatDate(app.submissionDate)}</td>
                      <td>
                        <button
                          className="manage-btn"
                          onClick={() => approveApplication(app.applicationId)}
                          disabled={processingId === app.applicationId}
                          style={{ marginRight: '5px' }}
                        >
                          {processingId === app.applicationId ? 'Processing...' : 'Approve'}
                        </button>
                        <button
                          className="manage-btn"
                          onClick={() => rejectApplication(app.applicationId)}
                          disabled={processingId === app.applicationId}
                        >
                          {processingId === app.applicationId ? 'Processing...' : 'Reject'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          
          <br />
          <br />

          {/* Approved Applications */}
          <div className="cargo-card beige-block table-scroll">
            <div className="table-title">
              Approved Applications ({applications.approved.length})
            </div>
            {applications.approved.length === 0 ? (
              <div style={{ padding: '20px', textAlign: 'center' }}>
                No approved applications
              </div>
            ) : (
              <table className="cargo-table">
                <thead>
                  <tr>
                    <th>Application ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Notes</th>
                    <th>Submitted</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {applications.approved.map((app) => (
                    <tr key={app.applicationId}>
                      <td>{app.applicationId}</td>
                      <td>{`${app.firstName || ''} ${app.lastName || ''}`.trim() || 'N/A'}</td>
                      <td>{app.email || 'N/A'}</td>
                      <td>{app.phone || 'N/A'}</td>
                      <td>{app.notes || 'N/A'}</td>
                      <td>{formatDate(app.submissionDate)}</td>
                      <td>
                        <span style={{ color: 'green', fontWeight: 'bold' }}>
                          Approved
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          
          <br />
          <br />
          
          {/* Rejected Applications */}
          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">
              Rejected Applications ({applications.rejected.length})
            </div>
            {applications.rejected.length === 0 ? (
              <div style={{ padding: '20px', textAlign: 'center' }}>
                No rejected applications
              </div>
            ) : (
              <table className="cargo-table">
                <thead>
                  <tr>
                    <th>Application ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Notes</th>
                    <th>Submitted</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {applications.rejected.map((app) => (
                    <tr key={app.applicationId}>
                      <td>{app.applicationId}</td>
                      <td>{`${app.firstName || ''} ${app.lastName || ''}`.trim() || 'N/A'}</td>
                      <td>{app.email || 'N/A'}</td>
                      <td>{app.phone || 'N/A'}</td>
                      <td>{app.notes || 'N/A'}</td>
                      <td>{formatDate(app.submissionDate)}</td>
                      <td>
                        <span style={{ color: 'red', fontWeight: 'bold' }}>
                          Rejected
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* Summary Statistics */}
          {!isLoading && (
            <div style={{ 
              marginTop: '20px', 
              padding: '15px', 
              backgroundColor: '#7f92b8ff', 
              borderRadius: '4px' 
            }}>
              <h3>Application Summary</h3>
              <p>Total Pending: {applications.pending.length}</p>
              <p>Total Approved: {applications.approved.length}</p>
              <p>Total Rejected: {applications.rejected.length}</p>
              <p>Total Applications: {
                applications.pending.length + 
                applications.approved.length + 
                applications.rejected.length
              }</p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default AdminViewAppli;