import React, { useState } from 'react';
import { secureAxios } from '../../config/axiosConfig';
import { useNavigate } from 'react-router-dom';
import '../../css/Round/Round_Admin.css';

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
    teamLeadId: "",
    clinicianId: ""
  });
  const [message, setMessage] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [selectedRound, setSelectedRound] = useState(null);
  // modalTab: "details" | "lottery" | "signups"
  const [modalTab, setModalTab] = useState("details");
  const [modalLotteryResult, setModalLotteryResult] = useState("");
  const [modalRoundDetails, setModalRoundDetails] = useState(null);

  const [updateRoundStep, setUpdateRoundStep] = useState("inputId");
  const [roundIdToUpdate, setRoundIdToUpdate] = useState("");
  const [updateRoundData, setUpdateRoundData] = useState(null);

  const formatDatetimeLocal = (dt) => {
    if (!dt) return "";
    return new Date(dt).toISOString().slice(0,16);
  };

  // 1. view rounds - Using secureAxios for admin operations
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

  // 2. create rounds - Using secureAxios
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
        maxParticipants: parseInt(newRound.maxParticipants, 10)
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

  // 3. cancel rounds - Using secureAxios
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

  // 4. modal(lottory, detail, manage signup)

  const openModal = (round) => {
    setSelectedRound(round);
    setModalOpen(true);
    setModalTab("details");
    setModalLotteryResult("");
    setModalRoundDetails(null);
  };

  const closeModal = () => {
    setModalOpen(false);
    setSelectedRound(null);
    setModalTab("details");
  };

  // run lottery（in modal）- Using secureAxios
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

  // detail(in modal）- Using secureAxios
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

  // approve/reject signups（in modal）- Using secureAxios
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

  const fetchRoundForUpdate = async () => {
    try {
      const response = await secureAxios.get(`/api/admin/rounds/${roundIdToUpdate}`, {
        params: {
          authenticated: true,
          adminUsername: userData.username
        }
      });
      if (response.data.status === "success") {
        const round = response.data.round;
        round.startTime = formatDatetimeLocal(round.startTime);
        round.endTime = formatDatetimeLocal(round.endTime);
        setUpdateRoundData(round);
        setUpdateRoundStep("editForm");
        setMessage("");
      } else {
        setMessage(response.data.message || "Error fetching round");
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

  const updateRound = async () => {
    try {
      const payload = {
        authenticated: true,
        adminUsername: userData.username,
        title: updateRoundData.title,
        description: updateRoundData.description,
        startTime: updateRoundData.startTime,
        endTime: updateRoundData.endTime,
        location: updateRoundData.location,
        maxParticipants: parseInt(updateRoundData.maxParticipants, 10),
        status: updateRoundData.status
      };
      const response = await secureAxios.put(`/api/admin/rounds/${updateRoundData.roundId}`, payload);
      if (response.data.status === "success") {
        setMessage("Round updated successfully with ID: " + response.data.roundId);
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
          <button
            className={`nav-btn ${activeTab === "updateRound" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("updateRound");
              setUpdateRoundStep("inputId");
              setRoundIdToUpdate("");
              setUpdateRoundData(null);
            }}
          >
            Update Round
          </button>
        </nav>
  
        <div className="navbar-right">
          <button className="nav-btn back-btn" onClick={() => navigate("/")}>
            Back to Dashboard
          </button>
        </div>
      </header>
  
      <h1 className="rounds-title">Rounds Administration</h1>
  
      {message && <p className="status-msg">{message}</p>}
  
      {/* ─────────────────────  MAIN CARD  ───────────────────── */}
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
                  className={`chip ${
                    roundFilter === "upcoming" ? "selected" : ""
                  }`}
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
                    <th className="table-header-cell">Round ID</th>
                    <th className="table-header-cell">Title</th>
                    <th className="table-header-cell">Description</th>
                    <th className="table-header-cell">Start Time</th>
                    <th className="table-header-cell">End Time</th>
                    <th className="table-header-cell">Location</th>
                    <th className="table-header-cell">Max</th>
                    <th className="table-header-cell">Status</th>
                    <th className="table-header-cell">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {rounds.map((round, idx) => (
                    <tr key={idx} onClick={() => openModal(round)}>
                      <td className="table-cell">{round.roundId}</td>
                      <td className="table-cell">{round.title}</td>
                      <td className="table-cell">{round.description}</td>
                      <td className="table-cell">
                        {new Date(round.startTime).toLocaleString()}
                      </td>
                      <td className="table-cell">
                        {new Date(round.endTime).toLocaleString()}
                      </td>
                      <td className="table-cell">{round.location}</td>
                      <td className="table-cell">{round.maxParticipants}</td>
                      <td className="table-cell">{round.status}</td>
                      <td className="table-cell">
                        <button
                          className="action-button"
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
            <h2 className="form-title">Create Round</h2>
            <div className="form-wrapper">
              <div className="form-card">
                <input
                  className="input"
                  type="text"
                  placeholder="Title"
                  value={newRound.title}
                  onChange={(e) =>
                    setNewRound({ ...newRound, title: e.target.value })
                  }
                />
                <input
                  className="input"
                  type="text"
                  placeholder="Description"
                  value={newRound.description}
                  onChange={(e) =>
                    setNewRound({ ...newRound, description: e.target.value })
                  }
                />
                <input
                  className="input"
                  type="datetime-local"
                  placeholder="Start Time"
                  value={newRound.startTime}
                  onChange={(e) =>
                    setNewRound({ ...newRound, startTime: e.target.value })
                  }
                />
                <input
                  className="input"
                  type="datetime-local"
                  placeholder="End Time"
                  value={newRound.endTime}
                  onChange={(e) =>
                    setNewRound({ ...newRound, endTime: e.target.value })
                  }
                />
                <input
                  className="input"
                  type="text"
                  placeholder="Location"
                  value={newRound.location}
                  onChange={(e) =>
                    setNewRound({ ...newRound, location: e.target.value })
                  }
                />
                <input
                  className="input"
                  type="text"
                  placeholder="Max Participants"
                  value={newRound.maxParticipants}
                  onChange={(e) =>
                    setNewRound({
                      ...newRound,
                      maxParticipants: e.target.value,
                    })
                  }
                />
                <input
                  className="input"
                  type="text"
                  placeholder="Team Lead ID (optional)"
                  value={newRound.teamLeadId}
                  onChange={(e) =>
                    setNewRound({ ...newRound, teamLeadId: e.target.value })
                  }
                />
                <input
                  className="input"
                  type="text"
                  placeholder="Clinician ID (optional)"
                  value={newRound.clinicianId}
                  onChange={(e) =>
                    setNewRound({ ...newRound, clinicianId: e.target.value })
                  }
                />
                <button className="action-button" onClick={createRound}>
                  Create Round
                </button>
              </div>
            </div>
          </>
        )}
  
        {/* ========== UPDATE ROUND TAB ========== */}
        {activeTab === "updateRound" && (
          <>
            <h2 className="form-title">Update Round</h2>
  
            {/* step 1 │ enter ID */}
            {updateRoundStep === "inputId" && (
              <div className="form-wrapper">
                <div className="form-card">
                  <input
                    className="input"
                    type="text"
                    placeholder="Enter Round ID"
                    value={roundIdToUpdate}
                    onChange={(e) => setRoundIdToUpdate(e.target.value)}
                  />
                  <button className="action-button" onClick={fetchRoundForUpdate}>
                    Next
                  </button>
                </div>
              </div>
            )}
  
            {/* step 2 │ edit form */}
            {updateRoundStep === "editForm" && updateRoundData && (
              <div className="form-wrapper">
                <div className="form-card">
                  <input
                    className="input"
                    type="text"
                    placeholder="Title"
                    value={updateRoundData.title}
                    onChange={(e) =>
                      setUpdateRoundData({
                        ...updateRoundData,
                        title: e.target.value,
                      })
                    }
                  />
                  <input
                    className="input"
                    type="text"
                    placeholder="Description"
                    value={updateRoundData.description}
                    onChange={(e) =>
                      setUpdateRoundData({
                        ...updateRoundData,
                        description: e.target.value,
                      })
                    }
                  />
                  <input
                    className="input"
                    type="datetime-local"
                    placeholder="Start Time"
                    value={updateRoundData.startTime}
                    onChange={(e) =>
                      setUpdateRoundData({
                        ...updateRoundData,
                        startTime: e.target.value,
                      })
                    }
                  />
                  <input
                    className="input"
                    type="datetime-local"
                    placeholder="End Time"
                    value={updateRoundData.endTime}
                    onChange={(e) =>
                      setUpdateRoundData({
                        ...updateRoundData,
                        endTime: e.target.value,
                      })
                    }
                  />
                  <input
                    className="input"
                    type="text"
                    placeholder="Location"
                    value={updateRoundData.location}
                    onChange={(e) =>
                      setUpdateRoundData({
                        ...updateRoundData,
                        location: e.target.value,
                      })
                    }
                  />
                  <input
                    className="input"
                    type="text"
                    placeholder="Max Participants"
                    value={updateRoundData.maxParticipants}
                    onChange={(e) =>
                      setUpdateRoundData({
                        ...updateRoundData,
                        maxParticipants: e.target.value,
                      })
                    }
                  />
                  <input
                    className="input"
                    type="text"
                    placeholder="Status"
                    value={updateRoundData.status || ""}
                    onChange={(e) =>
                      setUpdateRoundData({
                        ...updateRoundData,
                        status: e.target.value,
                      })
                    }
                  />
                  <button className="action-button" onClick={updateRound}>
                    Update Round
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
  
      {/* ─────────────────────  MODAL (original content unchanged)  ───────────────────── */}
      {modalOpen && selectedRound && (
        <div className="item-modal-overlay" onClick={closeModal}>
          <div className="item-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="modal-title">Round Detail – {selectedRound.title}</h2>
  
            <div className="modal-nav">
              <button
                className={`modal-chip ${modalTab === "details" ? "selected" : ""}`}
                onClick={() => setModalTab("details")}
              >
                Details
              </button>
              <button
                className={`modal-chip ${modalTab === "lottery" ? "selected" : ""}`}
                onClick={() => setModalTab("lottery")}
              >
                Run Lottery
              </button>
              <button
                className={`modal-chip ${modalTab === "signups" ? "selected" : ""}`}
                onClick={() => {
                  setModalTab("signups");
                  fetchModalSignups();
                }}
              >
                Manage Sign‑ups
              </button>
            </div>
  
            <div className="modal-body">
              {modalTab === "details" && (
                <div>
                  <p><strong>ID:</strong> {selectedRound.roundId}</p>
                  <p><strong>Title:</strong> {selectedRound.title}</p>
                  <p><strong>Description:</strong> {selectedRound.description}</p>
                  <p><strong>Start Time:</strong> {new Date(selectedRound.startTime).toLocaleString()}</p>
                  <p><strong>End Time:</strong> {new Date(selectedRound.endTime).toLocaleString()}</p>
                  <p><strong>Location:</strong> {selectedRound.location}</p>
                  <p><strong>Max Participants:</strong> {selectedRound.maxParticipants}</p>
                  <p><strong>Status:</strong> {selectedRound.status}</p>
                </div>
              )}
  
              {/* LOTTERY */}
              {modalTab === "lottery" && (
                <div>
                  <button className="action-button" onClick={runLotteryForModal}>
                    Run Lottery
                  </button>
                  {modalLotteryResult && <p className="status-msg">{modalLotteryResult}</p>}
                </div>
              )}
  
              {/* SIGN‑UPS */}
              {modalTab === "signups" && (
                <div>
                  {modalRoundDetails &&
                  modalRoundDetails.signups &&
                  modalRoundDetails.signups.length > 0 ? (
                    <table className="table">
                      <thead>
                        <tr>
                          <th className="table-header-cell">Signup ID</th>
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
                                ? signup.firstName +
                                  (signup.lastName ? " " + signup.lastName : "")
                                : signup.username || "N/A"}
                            </td>
                            <td className="table-cell">{signup.status}</td>
                            <td className="table-cell">
                              <button
                                className="action-button"
                                onClick={() => confirmSignup(signup.signupId)}
                              >
                                Confirm
                              </button>
                              <button
                                className="action-button"
                                onClick={() => rejectSignup(signup.signupId)}
                              >
                                Reject
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  ) : (
                    <p>No sign‑ups available.</p>
                  )}
                </div>
              )}
            </div>
  
            <div className="modal-buttons">
              <button className="modal-close" onClick={closeModal}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Round_Admin;