import React, { useState } from 'react';
import { secureAxios } from '../../config/axiosConfig';
import { useNavigate } from 'react-router-dom';
import '../../index.css'; 

function Round_Admin() {

  const navigate = useNavigate();
  const userData = JSON.parse(sessionStorage.getItem("auth_user")) || {};

  const [activeTab, setActiveTab] = useState("viewRounds");
  const [rounds, setRounds] = useState([]);
  const [roundFilter, setRoundFilter] = useState("all");
  const [newRound, setNewRound] = useState({
    title: "",
    description: "",
    startTime: "",
    endTime: "",
    location: "",
    maxParticipants: "",
    orderCapacity: "20",
    teamLeadId: "",
    clinicianId: ""
  });
  const [message, setMessage] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [selectedRound, setSelectedRound] = useState(null);
  // modalTab: "details" | "edit" | "lottery" | "signups" | "orders"
  const [modalTab, setModalTab] = useState("details");
  const [modalLotteryResult, setModalLotteryResult] = useState("");
  const [modalRoundDetails, setModalRoundDetails] = useState(null);
  const [roundOrders, setRoundOrders] = useState([]);

  // Edit form data in modal
  const [editRoundData, setEditRoundData] = useState(null);

  const formatDatetimeLocal = (dt) => {
    if (!dt) return "";
    return new Date(dt).toISOString().slice(0,16);
  };

  // 1. view rounds
  const fetchRounds = async () => {
    try {
      const url = roundFilter === "all" ? `/api/admin/rounds/all` : `/api/admin/rounds/upcoming`;
      const response = await secureAxios.get(url, {
        params: {
          authenticated: true,
          adminUsername: userData.username
        }
      });
      if (response.data.status === "success") {
        setRounds(response.data.rounds);
        setMessage("");
      } else {
        setMessage(response.data.message || "Error fetching rounds");
      }
    } catch (error) {
      console.error(error);
      if (error.response?.data?.httpsRequired) {
        setMessage("Secure HTTPS connection required for admin operations.");
      } else {
        setMessage(error.response?.data?.message || error.message);
      }
    }
  };

  // 2. create rounds
  const createRound = async () => {
    try {
      const payload = {
        authenticated: true,
        adminUsername: userData.username,
        title: newRound.title,
        description: newRound.description,
        startTime: newRound.startTime,
        endTime: newRound.endTime,
        location: newRound.location,
        maxParticipants: parseInt(newRound.maxParticipants, 10),
        orderCapacity: parseInt(newRound.orderCapacity, 10) || 20
      };
      if (newRound.teamLeadId.trim() !== "") {
        const tid = parseInt(newRound.teamLeadId, 10);
        if (!isNaN(tid)) payload.teamLeadId = tid;
      }
      if (newRound.clinicianId.trim() !== "") {
        const cid = parseInt(newRound.clinicianId, 10);
        if (!isNaN(cid)) payload.clinicianId = cid;
      }

      const response = await secureAxios.post('/api/admin/rounds/create', payload);
      if (response.data.status === "success") {
        setMessage("Round created with ID: " + response.data.roundId);
        // Reset form
        setNewRound({
          title: "",
          description: "",
          startTime: "",
          endTime: "",
          location: "",
          maxParticipants: "",
          orderCapacity: "20",
          teamLeadId: "",
          clinicianId: ""
        });
        fetchRounds();
      } else {
        setMessage(response.data.message || "Error creating round");
      }
    } catch (error) {
      console.error(error);
      if (error.response?.data?.httpsRequired) {
        setMessage("Secure HTTPS connection required for admin operations.");
      } else {
        setMessage(error.response?.data?.message || error.message);
      }
    }
  };

  // 3. cancel rounds
  const cancelRoundById = async (roundId, e) => {
    e.stopPropagation();
    try {
      const response = await secureAxios.put(`/api/admin/rounds/${roundId}/cancel`, {
        authenticated: true,
        adminUsername: userData.username
      });
      if (response.data.status === "success") {
        setMessage("Round cancelled successfully with ID: " + response.data.roundId);
        fetchRounds();
      } else {
        setMessage(response.data.message || "Error cancelling round");
      }
    } catch (error) {
      console.error(error);
      if (error.response?.data?.httpsRequired) {
        setMessage("Secure HTTPS connection required for admin operations.");
      } else {
        setMessage(error.response?.data?.message || error.message);
      }
    }
  };

  // Fetch orders for a round
  const fetchRoundOrders = async () => {
    try {
      const response = await secureAxios.get(`/api/admin/rounds/${selectedRound.roundId}/order-status`, {
        params: {
          authenticated: true
        }
      });
      if (response.data.status === "success") {
        setRoundOrders(response.data.orders || []);
      }
    } catch (error) {
      console.error(error);
      setMessage(error.response?.data?.message || error.message);
    }
  };

  // Auto-assign unassigned orders
  const autoAssignOrders = async () => {
    try {
      const response = await secureAxios.post('/api/admin/rounds/auto-assign-orders', {
        authenticated: true,
        adminUsername: userData.username
      });
      if (response.data.status === "success") {
        setMessage(response.data.message);
        fetchRounds();
      } else {
        setMessage(response.data.message || "Error auto-assigning orders");
      }
    } catch (error) {
      console.error(error);
      setMessage(error.response?.data?.message || error.message);
    }
  };

  // Modal functions
  const openModal = (round) => {
    setSelectedRound(round);
    setModalOpen(true);
    setModalTab("details");
    setModalLotteryResult("");
    setModalRoundDetails(null);
    setRoundOrders([]);
    setEditRoundData(null);
  };

  const closeModal = () => {
    setModalOpen(false);
    setSelectedRound(null);
    setModalTab("details");
    setEditRoundData(null);
  };

  // Switch to edit mode with pre-filled data
  const startEditMode = () => {
    setEditRoundData({
      roundId: selectedRound.roundId,
      title: selectedRound.title || "",
      description: selectedRound.description || "",
      startTime: formatDatetimeLocal(selectedRound.startTime),
      endTime: formatDatetimeLocal(selectedRound.endTime),
      location: selectedRound.location || "",
      maxParticipants: selectedRound.maxParticipants || "",
      orderCapacity: selectedRound.orderCapacity || 20,
      status: selectedRound.status || ""
    });
    setModalTab("edit");
  };

  // Update round from modal
  const updateRoundFromModal = async () => {
    try {
      const payload = {
        authenticated: true,
        adminUsername: userData.username,
        title: editRoundData.title,
        description: editRoundData.description,
        startTime: editRoundData.startTime,
        endTime: editRoundData.endTime,
        location: editRoundData.location,
        maxParticipants: parseInt(editRoundData.maxParticipants, 10),
        orderCapacity: parseInt(editRoundData.orderCapacity, 10) || 20,
        status: editRoundData.status
      };
      const response = await secureAxios.put(`/api/admin/rounds/${editRoundData.roundId}`, payload);
      if (response.data.status === "success") {
        setMessage("Round updated successfully!");
        // Update the selected round with new data
        setSelectedRound({
          ...selectedRound,
          ...editRoundData,
          startTime: editRoundData.startTime,
          endTime: editRoundData.endTime
        });
        setModalTab("details");
        setEditRoundData(null);
        fetchRounds(); // Refresh the list
      } else {
        setMessage(response.data.message || "Error updating round");
      }
    } catch (error) {
      console.error(error);
      if (error.response?.data?.httpsRequired) {
        setMessage("Secure HTTPS connection required for admin operations.");
      } else {
        setMessage(error.response?.data?.message || error.message);
      }
    }
  };

  // Run lottery
  const runLotteryForModal = async () => {
    try {
      const response = await secureAxios.post(`/api/admin/rounds/${selectedRound.roundId}/lottery`, {
        authenticated: true,
        adminUsername: userData.username
      });
      if (response.data.status === "success") {
        setModalLotteryResult("Lottery run successfully. Selected volunteers: " + response.data.selectedVolunteers);
      } else {
        setModalLotteryResult(response.data.message || "Error running lottery");
      }
    } catch (error) {
      console.error(error);
      if (error.response?.data?.httpsRequired) {
        setModalLotteryResult("Secure HTTPS connection required for admin operations.");
      } else {
        setModalLotteryResult(error.response?.data?.message || error.message);
      }
    }
  };

  // Fetch signups
  const fetchModalSignups = async () => {
    try {
      const response = await secureAxios.get(`/api/admin/rounds/${selectedRound.roundId}`, {
        params: {
          authenticated: true,
          adminUsername: userData.username
        }
      });
      if (response.data.status === "success") {
        setModalRoundDetails(response.data);
      } else {
        setMessage(response.data.message || "Error fetching round details");
      }
    } catch (error) {
      console.error(error);
      if (error.response?.data?.httpsRequired) {
        setMessage("Secure HTTPS connection required for admin operations.");
      } else {
        setMessage(error.response?.data?.message || error.message);
      }
    }
  };

  // Confirm/Reject signups
  const confirmSignup = async (signupId) => {
    try {
      const response = await secureAxios.put(`/api/admin/rounds/signup/${signupId}/confirm`, {
        authenticated: true,
        adminUsername: userData.username,
        adminId: userData.userId
      });
      if (response.data.status === "success") {
        setMessage("Signup confirmed: " + signupId);
        fetchModalSignups();
      } else {
        setMessage(response.data.message || "Error confirming signup");
      }
    } catch (error) {
      console.error(error);
      if (error.response?.data?.httpsRequired) {
        setMessage("Secure HTTPS connection required for admin operations.");
      } else {
        setMessage(error.response?.data?.message || error.message);
      }
    }
  };

  const rejectSignup = async (signupId) => {
    try {
      const response = await secureAxios.delete(`/api/admin/rounds/signup/${signupId}`, {
        data: {
          authenticated: true,
          adminUsername: userData.username,
          adminId: userData.userId
        }
      });
      if (response.data.status === "success") {
        setMessage("Signup rejected: " + signupId);
        fetchModalSignups();
      } else {
        setMessage(response.data.message || "Error rejecting signup");
      }
    } catch (error) {
      console.error(error);
      if (error.response?.data?.httpsRequired) {
        setMessage("Secure HTTPS connection required for admin operations.");
      } else {
        setMessage(error.response?.data?.message || error.message);
      }
    }
  };

  return (
    <div className="rounds-container">
      {/* ─────────────────────  NAVBAR  ───────────────────── */}
      <header className="smg-navbar">
        <div className="navbar-left">
          <img src="/Untitled.png" alt="SMG logo" className="navbar-logo" />
        </div>
  
        <nav className="navbar-center">
          <button
            className={`nav-btn ${activeTab === "viewRounds" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("viewRounds");
              fetchRounds();
            }}
          >
            View Rounds
          </button>
          <button
            className={`nav-btn ${activeTab === "createRound" ? "active" : ""}`}
            onClick={() => setActiveTab("createRound")}
          >
            Create Round
          </button>
          <button className="nav-btn" onClick={autoAssignOrders}>
            Auto-Assign
          </button>
        </nav>
  
        <div className="navbar-right">
          <button className="nav-btn back-btn" onClick={() => navigate("/")}>
            ← Dashboard
          </button>
        </div>
      </header>
  
      <h1 className="rounds-title">Rounds Administration</h1>
  
      {message && <p className="status-msg">{message}</p>}
  
      {/* ─────────────────────  MAIN CONTENT  ───────────────────── */}
      <div className="rounds-section">
        {activeTab === "viewRounds" && (
          <>
            <div className="section-header">
              <h2>View Rounds</h2>
              <div className="btn-group">
                <button
                  className={`chip ${roundFilter === "all" ? "selected" : ""}`}
                  onClick={() => {
                    setRoundFilter("all");
                    fetchRounds();
                  }}
                >
                  All
                </button>
                <button
                  className={`chip ${roundFilter === "upcoming" ? "selected" : ""}`}
                  onClick={() => {
                    setRoundFilter("upcoming");
                    fetchRounds();
                  }}
                >
                  Upcoming
                </button>
              </div>
            </div>
  
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th className="table-header-cell">ID</th>
                    <th className="table-header-cell">Title</th>
                    <th className="table-header-cell">Start Time</th>
                    <th className="table-header-cell">Location</th>
                    <th className="table-header-cell">Orders</th>
                    <th className="table-header-cell">Status</th>
                    <th className="table-header-cell">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {rounds.map((round, idx) => (
                    <tr key={idx} onClick={() => openModal(round)}>
                      <td className="table-cell">{round.roundId}</td>
                      <td className="table-cell">{round.title}</td>
                      <td className="table-cell">
                        {new Date(round.startTime).toLocaleString()}
                      </td>
                      <td className="table-cell">{round.location}</td>
                      <td className="table-cell">
                        {round.currentOrderCount || 0}/{round.orderCapacity || 20}
                      </td>
                      <td className="table-cell">
                        <span className={`status-badge status-${round.status?.toLowerCase()}`}>
                          {round.status}
                        </span>
                      </td>
                      <td className="table-cell">
                        <button
                          className="action-button cancel-btn"
                          onClick={(e) => cancelRoundById(round.roundId, e)}
                        >
                          Cancel
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
  
        {/* ========== CREATE ROUND TAB ========== */}
        {activeTab === "createRound" && (
          <>
            <h2 className="form-title">Create New Round</h2>
            <div className="form-wrapper">
              <div className="form-card">
                <div className="form-group">
                  <label>Title</label>
                  <input
                    className="input"
                    type="text"
                    placeholder="Enter round title"
                    value={newRound.title}
                    onChange={(e) => setNewRound({ ...newRound, title: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Description</label>
                  <input
                    className="input"
                    type="text"
                    placeholder="Enter description"
                    value={newRound.description}
                    onChange={(e) => setNewRound({ ...newRound, description: e.target.value })}
                  />
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Start Time</label>
                    <input
                      className="input"
                      type="datetime-local"
                      value={newRound.startTime}
                      onChange={(e) => setNewRound({ ...newRound, startTime: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label>End Time</label>
                    <input
                      className="input"
                      type="datetime-local"
                      value={newRound.endTime}
                      onChange={(e) => setNewRound({ ...newRound, endTime: e.target.value })}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>Location</label>
                  <input
                    className="input"
                    type="text"
                    placeholder="Enter location"
                    value={newRound.location}
                    onChange={(e) => setNewRound({ ...newRound, location: e.target.value })}
                  />
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Max Participants</label>
                    <input
                      className="input"
                      type="number"
                      placeholder="e.g., 10"
                      value={newRound.maxParticipants}
                      onChange={(e) => setNewRound({ ...newRound, maxParticipants: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label>Order Capacity</label>
                    <input
                      className="input"
                      type="number"
                      placeholder="Default: 20"
                      value={newRound.orderCapacity}
                      onChange={(e) => setNewRound({ ...newRound, orderCapacity: e.target.value })}
                    />
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Team Lead ID (optional)</label>
                    <input
                      className="input"
                      type="text"
                      placeholder="User ID"
                      value={newRound.teamLeadId}
                      onChange={(e) => setNewRound({ ...newRound, teamLeadId: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label>Clinician ID (optional)</label>
                    <input
                      className="input"
                      type="text"
                      placeholder="User ID"
                      value={newRound.clinicianId}
                      onChange={(e) => setNewRound({ ...newRound, clinicianId: e.target.value })}
                    />
                  </div>
                </div>
                <button className="action-button submit-btn" onClick={createRound}>
                  Create Round
                </button>
              </div>
            </div>
          </>
        )}
      </div>
  
      {/* ─────────────────────  MODAL  ───────────────────── */}
      {modalOpen && selectedRound && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-container" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{selectedRound.title}</h2>
              <button className="modal-close-btn" onClick={closeModal}>×</button>
            </div>
  
            <div className="modal-tabs">
              <button
                className={`modal-tab ${modalTab === "details" ? "active" : ""}`}
                onClick={() => setModalTab("details")}
              >
                Details
              </button>
              <button
                className={`modal-tab ${modalTab === "edit" ? "active" : ""}`}
                onClick={startEditMode}
              >
                Edit
              </button>
              <button
                className={`modal-tab ${modalTab === "signups" ? "active" : ""}`}
                onClick={() => {
                  setModalTab("signups");
                  fetchModalSignups();
                }}
              >
                Sign-ups
              </button>
              <button
                className={`modal-tab ${modalTab === "orders" ? "active" : ""}`}
                onClick={() => {
                  setModalTab("orders");
                  fetchRoundOrders();
                }}
              >
                Orders
              </button>
              <button
                className={`modal-tab ${modalTab === "lottery" ? "active" : ""}`}
                onClick={() => setModalTab("lottery")}
              >
                Lottery
              </button>
            </div>
  
            <div className="modal-body">
              {/* DETAILS TAB */}
              {modalTab === "details" && (
                <div className="details-grid">
                  <div className="detail-item">
                    <span className="detail-label">Round ID</span>
                    <span className="detail-value">{selectedRound.roundId}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Status</span>
                    <span className={`detail-value status-badge status-${selectedRound.status?.toLowerCase()}`}>
                      {selectedRound.status}
                    </span>
                  </div>
                  <div className="detail-item full-width">
                    <span className="detail-label">Description</span>
                    <span className="detail-value">{selectedRound.description || "No description"}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Start Time</span>
                    <span className="detail-value">{new Date(selectedRound.startTime).toLocaleString()}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">End Time</span>
                    <span className="detail-value">{new Date(selectedRound.endTime).toLocaleString()}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Location</span>
                    <span className="detail-value">{selectedRound.location}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Max Participants</span>
                    <span className="detail-value">{selectedRound.maxParticipants}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Order Capacity</span>
                    <span className="detail-value">{selectedRound.currentOrderCount || 0} / {selectedRound.orderCapacity || 20}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Available Slots</span>
                    <span className="detail-value">{(selectedRound.orderCapacity || 20) - (selectedRound.currentOrderCount || 0)}</span>
                  </div>
                  <div className="detail-actions">
                    <button className="action-button edit-btn" onClick={startEditMode}>
                      ✏️ Edit Round
                    </button>
                  </div>
                </div>
              )}

              {/* EDIT TAB */}
              {modalTab === "edit" && editRoundData && (
                <div className="edit-form">
                  <div className="form-group">
                    <label>Title</label>
                    <input
                      className="input"
                      type="text"
                      value={editRoundData.title}
                      onChange={(e) => setEditRoundData({ ...editRoundData, title: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label>Description</label>
                    <input
                      className="input"
                      type="text"
                      value={editRoundData.description}
                      onChange={(e) => setEditRoundData({ ...editRoundData, description: e.target.value })}
                    />
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Start Time</label>
                      <input
                        className="input"
                        type="datetime-local"
                        value={editRoundData.startTime}
                        onChange={(e) => setEditRoundData({ ...editRoundData, startTime: e.target.value })}
                      />
                    </div>
                    <div className="form-group">
                      <label>End Time</label>
                      <input
                        className="input"
                        type="datetime-local"
                        value={editRoundData.endTime}
                        onChange={(e) => setEditRoundData({ ...editRoundData, endTime: e.target.value })}
                      />
                    </div>
                  </div>
                  <div className="form-group">
                    <label>Location</label>
                    <input
                      className="input"
                      type="text"
                      value={editRoundData.location}
                      onChange={(e) => setEditRoundData({ ...editRoundData, location: e.target.value })}
                    />
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Max Participants</label>
                      <input
                        className="input"
                        type="number"
                        value={editRoundData.maxParticipants}
                        onChange={(e) => setEditRoundData({ ...editRoundData, maxParticipants: e.target.value })}
                      />
                    </div>
                    <div className="form-group">
                      <label>Order Capacity</label>
                      <input
                        className="input"
                        type="number"
                        value={editRoundData.orderCapacity}
                        onChange={(e) => setEditRoundData({ ...editRoundData, orderCapacity: e.target.value })}
                      />
                    </div>
                  </div>
                  <div className="form-group">
                    <label>Status</label>
                    <select
                      className="input"
                      value={editRoundData.status}
                      onChange={(e) => setEditRoundData({ ...editRoundData, status: e.target.value })}
                    >
                      <option value="SCHEDULED">SCHEDULED</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="COMPLETED">COMPLETED</option>
                      <option value="CANCELLED">CANCELLED</option>
                    </select>
                  </div>
                  <div className="form-actions">
                    <button className="action-button cancel-btn" onClick={() => setModalTab("details")}>
                      Cancel
                    </button>
                    <button className="action-button submit-btn" onClick={updateRoundFromModal}>
                      Save Changes
                    </button>
                  </div>
                </div>
              )}
  
              {/* LOTTERY TAB */}
              {modalTab === "lottery" && (
                <div className="lottery-section">
                  <p>Run the lottery to randomly select volunteers for this round.</p>
                  <button className="action-button submit-btn" onClick={runLotteryForModal}>
                    🎲 Run Lottery
                  </button>
                  {modalLotteryResult && <p className="status-msg">{modalLotteryResult}</p>}
                </div>
              )}
  
              {/* SIGN-UPS TAB */}
              {modalTab === "signups" && (
                <div className="signups-section">
                  {modalRoundDetails?.signups?.length > 0 ? (
                    <div className="table-wrapper">
                      <table className="table">
                        <thead>
                          <tr>
                            <th className="table-header-cell">ID</th>
                            <th className="table-header-cell">Volunteer</th>
                            <th className="table-header-cell">Status</th>
                            <th className="table-header-cell">Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {modalRoundDetails.signups.map((signup, idx) => (
                            <tr key={idx}>
                              <td className="table-cell">{signup.signupId}</td>
                              <td className="table-cell">
                                {signup.firstName
                                  ? `${signup.firstName} ${signup.lastName || ""}`
                                  : signup.username || "N/A"}
                              </td>
                              <td className="table-cell">{signup.status}</td>
                              <td className="table-cell">
                                <button
                                  className="action-button small"
                                  onClick={() => confirmSignup(signup.signupId)}
                                >
                                  ✓
                                </button>
                                <button
                                  className="action-button small cancel-btn"
                                  onClick={() => rejectSignup(signup.signupId)}
                                >
                                  ✕
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <p className="empty-state">No sign-ups yet.</p>
                  )}
                </div>
              )}

              {/* ORDERS TAB */}
              {modalTab === "orders" && (
                <div className="orders-section">
                  <p className="order-summary">
                    <strong>Orders:</strong> {roundOrders.length || 0} / {selectedRound.orderCapacity || 20}
                  </p>
                  {roundOrders?.length > 0 ? (
                    <div className="table-wrapper">
                      <table className="table">
                        <thead>
                          <tr>
                            <th className="table-header-cell">Order ID</th>
                            <th className="table-header-cell">User</th>
                            <th className="table-header-cell">Status</th>
                            <th className="table-header-cell">Address</th>
                          </tr>
                        </thead>
                        <tbody>
                          {roundOrders.map((order, idx) => (
                            <tr key={idx}>
                              <td className="table-cell">{order.orderId}</td>
                              <td className="table-cell">{order.userId === -1 ? "Guest" : order.userId}</td>
                              <td className="table-cell">{order.status}</td>
                              <td className="table-cell">{order.deliveryAddress}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <p className="empty-state">No orders assigned yet.</p>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Round_Admin;