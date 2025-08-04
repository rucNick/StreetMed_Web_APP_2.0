import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import '../../css/Admin/Admin_Feedback.css';

const AdminFeedback = ({ userData }) => {
  const baseURL = process.env.REACT_APP_BASE_URL;
  const navigate = useNavigate();

  const [feedbacks, setFeedbacks] = useState([]);
  const [error, setError] = useState('');

  const loadFeedback = useCallback(async () => {
    try {
      const resp = await axios.get(
        `${baseURL}/api/feedback/all`,
        {
          headers: {
            "Admin-Username": userData.username,
            "Authentication-Status": "true"
          }
        }
      );
      setFeedbacks(resp.data.data);
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    }
  }, [baseURL, userData.username]);

  useEffect(() => {
    loadFeedback();
  }, [loadFeedback]);

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
          <h1 className="cargo-title">Feedback</h1>
          {error && <p className="cargo-error">Error: {error}</p>}

          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">All Feedback</div>
            <table className="cargo-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Phone</th>
                  <th>Content</th>
                  <th>Created At</th>
                </tr>
              </thead>
              <tbody>
                {feedbacks.map(f => (
                  <tr key={f.id}>
                    <td>{f.id}</td>
                    <td>{f.name}</td>
                    <td>{f.phoneNumber}</td>
                    <td>{f.content}</td>
                    <td>{new Date(f.createdAt).toLocaleString()}</td>
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

export default AdminFeedback;
