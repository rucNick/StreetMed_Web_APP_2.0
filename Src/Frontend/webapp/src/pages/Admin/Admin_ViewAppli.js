import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import '../../css/Admin/Admin_ViewAppli.css';

const AdminViewAppli = ({ userData }) => {
  const baseURL = process.env.REACT_APP_BASE_URL;
  const navigate = useNavigate();

  const [applications, setApplications] = useState({ pending: [], approved: [], rejected: [] });
  const [applicationsError, setApplicationsError] = useState('');

  const loadApplications = useCallback(async () => {
    try {
      const response = await axios.get(
        `${baseURL}/api/volunteer/applications`,
        {
          headers: {
            "Admin-Username": userData.username,
            "Authentication-Status": "true"
          }
        }
      );
      setApplications(response.data.data);
    } catch (error) {
      setApplicationsError(error.response?.data?.message || error.message);
    }
  }, [baseURL, userData.username]);

  const approveApplication = async (applicationId) => {
    try {
      const resp = await axios.post(
        `${baseURL}/api/volunteer/approve`,
        {
          adminUsername: userData.username,
          authenticated: "true",
          applicationId: applicationId.toString()
        }
      );
      alert(resp.data.message);
      loadApplications();
    } catch (e) {
      alert(e.response?.data?.message || e.message);
    }
  };

  const rejectApplication = async (applicationId) => {
    try {
      const resp = await axios.post(
        `${baseURL}/api/volunteer/reject`,
        {
          adminUsername: userData.username,
          authenticated: "true",
          applicationId: applicationId.toString()
        }
      );
      alert(resp.data.message);
      loadApplications();
    } catch (e) {
      alert(e.response?.data?.message || e.message);
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
            <span className="site-title">Cargo Management System</span>
          </div>
          <div className="header-right">
            <button className="manage-btn" onClick={() => navigate(-1)}>
              ← Go Back
            </button>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="cargo-container">
          <div className="cargo-header">
            <h1 className="cargo-title">Volunteer Applications</h1>
          </div>

          {applicationsError && (
            <p className="cargo-error">Error: {applicationsError}</p>
          )}

          {/* —— Pending —— */}
          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">Pending</div>
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
                {applications.pending.map((app, idx) => (
                  <tr key={idx}>
                    <td>{app.applicationId}</td>
                    <td>{app.firstName} {app.lastName}</td>
                    <td>{app.email}</td>
                    <td>{app.phone}</td>
                    <td>{app.notes}</td>
                    <td>{new Date(app.submissionDate).toLocaleString()}</td>
                    <td>
                      <button
                        className="manage-btn"
                        onClick={() => approveApplication(app.applicationId)}
                      >
                        Approve
                      </button>
                      <button
                        className="manage-btn"
                        onClick={() => rejectApplication(app.applicationId)}
                      >
                        Reject
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <br></br>
          <br></br>


          {/* —— Approved —— */}
          <div className="cargo-card beige-block table-scroll">
            <div className="table-title">Approved</div>
            <table className="cargo-table">
              <thead>
                <tr>
                  <th>Application ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Notes</th>
                  <th>Submitted</th>
                </tr>
              </thead>
              <tbody>
                {applications.approved.map((app, idx) => (
                  <tr key={idx}>
                    <td>{app.applicationId}</td>
                    <td>{app.firstName} {app.lastName}</td>
                    <td>{app.email}</td>
                    <td>{app.phone}</td>
                    <td>{app.notes}</td>
                    <td>{new Date(app.submissionDate).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <br></br>
          <br></br>
          {/* —— Rejected —— */}
          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">Rejected</div>
            <table className="cargo-table">
              <thead>
                <tr>
                  <th>Application ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Notes</th>
                  <th>Submitted</th>
                </tr>
              </thead>
              <tbody>
                {applications.rejected.map((app, idx) => (
                  <tr key={idx}>
                    <td>{app.applicationId}</td>
                    <td>{app.firstName} {app.lastName}</td>
                    <td>{app.email}</td>
                    <td>{app.phone}</td>
                    <td>{app.notes}</td>
                    <td>{new Date(app.submissionDate).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

        </div>
      </main>
    </div>
  );
};

export default AdminViewAppli;
