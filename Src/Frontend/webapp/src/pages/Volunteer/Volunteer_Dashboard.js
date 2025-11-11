import React, { useState, useEffect, useCallback } from "react";
import Calendar from "react-calendar";
import "react-calendar/dist/Calendar.css";
import { publicAxios, secureAxios } from "../../config/axiosConfig";
import { useNavigate } from "react-router-dom";
import "../../css/Volunteer/Volunteer_Dashboard.css";

const Volunteer_Dashboard = ({ userData, onLogout }) => {
  const navigate = useNavigate();

  // Rounds states
  const [myUpcomingRounds, setMyUpcomingRounds] = useState([]);
  const [myPastRounds, setMyPastRounds] = useState([]);
  const [myRoundsError, setMyRoundsError] = useState("");
  const [allUpcomingRounds, setAllUpcomingRounds] = useState([]);
  const [allRoundsError, setAllRoundsError] = useState("");
  
  // Orders states - Updated for new workflow
  const [pendingOrders, setPendingOrders] = useState([]);
  const [myAssignments, setMyAssignments] = useState([]);
  const [ordersError, setOrdersError] = useState("");
  const [isLoadingOrders, setIsLoadingOrders] = useState(false);
  const [showOrdersTab, setShowOrdersTab] = useState("pending"); // "pending" or "assigned"
  
  // Calendar and modals
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [roundsForSelectedDate, setRoundsForSelectedDate] = useState([]);
  const [showRoundsModal, setShowRoundsModal] = useState(false);
  const [fullViewModalOpen, setFullViewModalOpen] = useState(false);
  const [selectedRoundDetails, setSelectedRoundDetails] = useState(null);
  const [roundOrders, setRoundOrders] = useState([]);
  const [orderModalOpen, setOrderModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showPastRounds, setShowPastRounds] = useState(false);
  const [showCompletedOrders, setShowCompletedOrders] = useState(false);

  // Load pending orders from queue
  const loadPendingOrders = useCallback(async () => {
    if (!userData || !userData.userId) return;
    setIsLoadingOrders(true);
    try {
      const response = await secureAxios.get('/api/orders/pending', {
        params: { page: 0, size: 20 },
        headers: {
          'Authentication-Status': 'true',
          'User-Id': userData.userId,
          'User-Role': 'VOLUNTEER'
        }
      });
      
      if (response.data.status === "success") {
        setPendingOrders(response.data.orders || []);
      } else {
        setOrdersError(response.data.message || "Failed to load pending orders");
      }
    } catch (error) {
      console.error("Error loading pending orders:", error);
      setOrdersError(error.response?.data?.message || error.message);
    } finally {
      setIsLoadingOrders(false);
    }
  }, [userData]);

  // Load volunteer's assignments
  const loadMyAssignments = useCallback(async () => {
    if (!userData || !userData.userId) return;
    setIsLoadingOrders(true);
    try {
      const response = await secureAxios.get('/api/orders/my-assignments', {
        headers: {
          'User-Id': userData.userId,
          'User-Role': 'VOLUNTEER',
          'Authentication-Status': 'true'
        }
      });
      
      if (response.data.status === "success") {
        setMyAssignments(response.data.assignments || []);
      } else {
        setOrdersError(response.data.message || "Failed to load assignments");
      }
    } catch (error) {
      console.error("Error loading assignments:", error);
      setOrdersError(error.response?.data?.message || error.message);
    } finally {
      setIsLoadingOrders(false);
    }
  }, [userData]);

  // Accept an order from the pending queue
  const acceptOrder = async (orderId, roundId = null) => {
    if (!window.confirm(`Accept order ${orderId}? You will be responsible for delivering this order.`)) {
      return;
    }
    
    try {
      const response = await secureAxios.post(`/api/orders/${orderId}/accept`, {
        authenticated: true,
        userId: userData.userId,
        userRole: 'VOLUNTEER',
        roundId: roundId
      });
      
      if (response.data.status === "success") {
        alert("Order accepted successfully! Check your assignments.");
        loadPendingOrders();
        loadMyAssignments();
        setShowOrdersTab("assigned"); // Switch to assigned tab
      } else {
        alert(response.data.message || "Failed to accept order");
      }
    } catch (error) {
      console.error("Error accepting order:", error);
      if (error.response?.status === 409) {
        alert("This order has already been accepted by another volunteer.");
        loadPendingOrders(); // Refresh to remove the taken order
      } else {
        alert(error.response?.data?.message || "Failed to accept order");
      }
    }
  };

  // Cancel assignment (return to queue)
  const cancelAssignment = async (orderId) => {
    if (!window.confirm(`Cancel your assignment for order ${orderId}? This will return it to the pending queue.`)) {
      return;
    }
    
    try {
      const response = await secureAxios.delete(`/api/orders/${orderId}/assignment`, {
        headers: {
          'User-Id': userData.userId,
          'User-Role': 'VOLUNTEER',
          'Authentication-Status': 'true'
        }
      });
      
      if (response.data.status === "success") {
        alert("Assignment cancelled successfully.");
        loadMyAssignments();
        loadPendingOrders(); // Refresh pending orders
      } else {
        alert(response.data.message || "Failed to cancel assignment");
      }
    } catch (error) {
      console.error("Error cancelling assignment:", error);
      alert(error.response?.data?.message || "Failed to cancel assignment");
    }
  };

  // Start working on an order
  const startOrder = async (assignmentId) => {
    try {
      const response = await secureAxios.put(`/api/orders/assignment/${assignmentId}/start`, {}, {
        headers: {
          'User-Id': userData.userId,
          'User-Role': 'VOLUNTEER',
          'Authentication-Status': 'true'
        }
      });
      
      if (response.data.status === "success") {
        alert("Started working on order!");
        loadMyAssignments();
      }
    } catch (error) {
      alert(error.response?.data?.message || "Failed to start order");
    }
  };

  // Complete an order
  const completeOrder = async (assignmentId) => {
    if (!window.confirm("Mark this order as completed?")) {
      return;
    }
    
    try {
      const response = await secureAxios.put(`/api/orders/assignment/${assignmentId}/complete`, {}, {
        headers: {
          'User-Id': userData.userId,
          'User-Role': 'VOLUNTEER',
          'Authentication-Status': 'true'
        }
      });
      
      if (response.data.status === "success") {
        alert("Order completed successfully!");
        loadMyAssignments();
      }
    } catch (error) {
      alert(error.response?.data?.message || "Failed to complete order");
    }
  };

  // Existing rounds functions remain the same...
  const loadMyRounds = useCallback(async () => {
    if (!userData || !userData.userId) return;
    try {
      const r = await publicAxios.get('/api/rounds/my-rounds', {
        params: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      const d = r.data;
      if (d.status === "success") {
        setMyUpcomingRounds(d.upcomingRounds || []);
        setMyPastRounds(d.pastRounds || []);
      } else {
        setMyRoundsError(d.message || "Failed to load my rounds");
      }
    } catch (e) {
      if (e.code === 'ERR_CERT_AUTHORITY_INVALID') {
        setMyRoundsError('Certificate error. Please accept the certificate and try again.');
      } else {
        setMyRoundsError(e.response?.data?.message || e.message);
      }
    }
  }, [userData]);

  const loadAllUpcomingRounds = useCallback(async () => {
    if (!userData || !userData.userId) return;
    try {
      const r = await publicAxios.get('/api/rounds/all', {
        params: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      const d = r.data;
      if (d.status === "success") {
        setAllUpcomingRounds(d.rounds || []);
      } else {
        setAllRoundsError(d.message || "Failed to load upcoming rounds");
      }
    } catch (e) {
      setAllRoundsError(e.response?.data?.message || e.message);
    }
  }, [userData]);

  const signupForRound = async (roundId, requestedRole = "VOLUNTEER") => {
    try {
      const r = await publicAxios.post(`/api/rounds/${roundId}/signup`, {
        authenticated: true,
        userId: userData.userId,
        userRole: "VOLUNTEER",
        requestedRole
      });
      alert(r.data.message);
      loadMyRounds();
      loadAllUpcomingRounds();
      handleDateClick(selectedDate);
    } catch (e) {
      alert(e.response?.data?.message || e.message);
    }
  };

  const handleDateClick = (date) => {
    setSelectedDate(date);
    const ds = date.toISOString().split("T")[0];
    const f = allUpcomingRounds.filter((r) => {
      const rs = r.startTime.split("T")[0];
      return rs === ds;
    });
    setRoundsForSelectedDate(f);
    setShowRoundsModal(true);
  };

  const highlightDates = ({ date, view }) => {
    if (view !== "month") return null;
    const ds = date.toISOString().split("T")[0];
    const f = allUpcomingRounds.some((r) => {
      const rs = r.startTime.split("T")[0];
      return rs === ds;
    });
    return f ? "highlight-day" : null;
  };

  const openFullViewModal = async (roundId) => {
    try {
      const r = await publicAxios.get(`/api/rounds/${roundId}`, {
        params: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      if (r.data.status === "success") {
        setSelectedRoundDetails(r.data.round);
        setFullViewModalOpen(true);
      }
      
      const ordersRes = await publicAxios.get(`/api/rounds/${roundId}/orders`, {
        params: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      if (ordersRes.data.status === "success") {
        setRoundOrders(ordersRes.data.orders || []);
      }
    } catch (e) {
      alert(e.response?.data?.message || e.message);
    }
  };

  const closeFullViewModal = () => {
    setFullViewModalOpen(false);
    setSelectedRoundDetails(null);
    setRoundOrders([]);
  };

  const handleCancelSignupFullView = async () => {
    if (!selectedRoundDetails) return;
    const s =
      (selectedRoundDetails.signupDetails && selectedRoundDetails.signupDetails.signupId) ||
      selectedRoundDetails.signupId;
    if (!s) {
      alert("No signup found for this round");
      return;
    }
    try {
      const r = await publicAxios.delete(`/api/rounds/signup/${s}`, {
        data: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      alert(r.data.message);
      closeFullViewModal();
      loadMyRounds();
      loadAllUpcomingRounds();
    } catch (e) {
      alert(e.response?.data?.message || e.message);
    }
  };

  const openOrderFullView = (o) => {
    setSelectedOrder(o);
    setOrderModalOpen(true);
  };

  const closeOrderFullView = () => {
    setOrderModalOpen(false);
    setSelectedOrder(null);
  };

  const getOrderAge = (requestTime) => {
    if (!requestTime) return 'Unknown';
    const now = new Date();
    const orderTime = new Date(requestTime);
    const diffMs = now - orderTime;
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 60) return `${diffMins} mins`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d`;
  };

  useEffect(() => {
    if (!userData || !userData.userId) return;
    loadMyRounds();
    loadAllUpcomingRounds();
    loadPendingOrders();
    loadMyAssignments();
  }, [loadMyRounds, loadAllUpcomingRounds, loadPendingOrders, loadMyAssignments, userData]);

  const activeAssignments = myAssignments.filter(a => a.status !== 'COMPLETED');
  const completedAssignments = myAssignments.filter(a => a.status === 'COMPLETED');

  return (
    <>
      <header className="nav-bar">
        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
          <img className="nav-logo" src="/Untitled.png" alt="Logo" />
          <span className="nav-welcome"><h2>Welcome Back, {userData.username}!</h2></span>
          <button className="nav-btn" style={{ marginLeft: "10px" }} onClick={() => navigate("/profile")}>
            Profile
          </button>
        </div>
        <div className="nav-right-group">
          <button className="nav-btn" onClick={() => navigate("/volunteer/orders")}>
            Order Queue ({pendingOrders.length})
          </button>
          <button className="nav-btn" onClick={() => navigate("/cargo_volunteer")}>
            Cargo
          </button>
          <button className="nav-btn" onClick={onLogout}>
            Logout
          </button>
        </div>
      </header>

      <div className="volunteer-dashboard-container">
        <div className="volunteer-left-panel">
          <br />
          <br />
          
          {/* Orders Section - New Priority Queue System */}
          <div style={{ marginBottom: '30px', border: '2px solid #ff6b00', padding: '15px', borderRadius: '8px' }}>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <strong>📦 Order Management</strong>
              <span style={{ fontSize: '14px', color: '#666' }}>
                ({activeAssignments.length} active)
              </span>
            </h2>
            
            {/* Tab buttons */}
            <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
              <button 
                onClick={() => setShowOrdersTab("pending")}
                style={{
                  padding: '8px 16px',
                  backgroundColor: showOrdersTab === "pending" ? '#ff6b00' : '#f0f0f0',
                  color: showOrdersTab === "pending" ? 'white' : 'black',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Pending Queue ({pendingOrders.length})
              </button>
              <button 
                onClick={() => setShowOrdersTab("assigned")}
                style={{
                  padding: '8px 16px',
                  backgroundColor: showOrdersTab === "assigned" ? '#ff6b00' : '#f0f0f0',
                  color: showOrdersTab === "assigned" ? 'white' : 'black',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                My Assignments ({activeAssignments.length})
              </button>
            </div>

            {ordersError && <p className="error-text">{ordersError}</p>}
            
            {showOrdersTab === "pending" ? (
              <div className="orders-cards">
                {isLoadingOrders ? (
                  <p>Loading orders...</p>
                ) : pendingOrders.length === 0 ? (
                  <p>No pending orders in queue.</p>
                ) : (
                  pendingOrders.slice(0, 5).map((order, idx) => (
                    <div 
                      key={order.orderId} 
                      className="order-card"
                      style={{
                        border: idx === 0 ? '2px solid #ff6b00' : '1px solid #ddd',
                        backgroundColor: idx === 0 ? '#fff8e1' : 'white'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <h3>
                          {idx === 0 && <span style={{color: '#ff6b00'}}>⚡ PRIORITY - </span>}
                          Order #{order.orderId}
                        </h3>
                        <span style={{ 
                          fontSize: '12px', 
                          color: idx === 0 ? '#ff6b00' : '#666',
                          fontWeight: idx === 0 ? 'bold' : 'normal'
                        }}>
                          {getOrderAge(order.requestTime)} old
                        </span>
                      </div>
                      <p><strong>Items:</strong> {order.orderItems?.map(i => `${i.itemName} (${i.quantity})`).join(', ')}</p>
                      <p><strong>Address:</strong> {order.deliveryAddress}</p>
                      <p><strong>Phone:</strong> {order.phoneNumber}</p>
                      {order.notes && <p><strong>Notes:</strong> {order.notes}</p>}
                      <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                        <button 
                          className="open-view-btn"
                          style={{ 
                            backgroundColor: idx === 0 ? '#ff6b00' : '#009E2C'
                          }}
                          onClick={() => acceptOrder(order.orderId, order.roundId)}
                        >
                          {idx === 0 ? '⚡ Accept Priority Order' : 'Accept Order'}
                        </button>
                        <button 
                          className="open-view-btn"
                          onClick={() => openOrderFullView(order)}
                        >
                          View Details
                        </button>
                      </div>
                    </div>
                  ))
                )}
                {pendingOrders.length > 5 && (
                  <p style={{ textAlign: 'center', marginTop: '10px', color: '#666' }}>
                    + {pendingOrders.length - 5} more orders in queue
                  </p>
                )}
              </div>
            ) : (
              <div className="orders-cards">
                {activeAssignments.length === 0 ? (
                  <p>No active assignments. Check the pending queue!</p>
                ) : (
                  activeAssignments.map((assignment) => (
                    <div key={assignment.assignmentId} className="order-card" style={{ 
                      borderLeft: assignment.status === 'IN_PROGRESS' ? '4px solid #3498db' : '4px solid #27ae60'
                    }}>
                      <h3>Assignment #{assignment.assignmentId}</h3>
                      <p><strong>Order ID:</strong> {assignment.orderId}</p>
                      <p><strong>Status:</strong> 
                        <span style={{
                          padding: '2px 8px',
                          borderRadius: '4px',
                          marginLeft: '5px',
                          backgroundColor: assignment.status === 'ACCEPTED' ? '#e8f5e9' : '#e3f2fd',
                          color: assignment.status === 'ACCEPTED' ? '#2e7d32' : '#1565c0'
                        }}>
                          {assignment.status}
                        </span>
                      </p>
                      <p><strong>Items:</strong> {assignment.items?.map(i => `${i.itemName} (${i.quantity})`).join(', ')}</p>
                      <p><strong>Address:</strong> {assignment.deliveryAddress}</p>
                      <p><strong>Phone:</strong> {assignment.phoneNumber}</p>
                      <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                        {assignment.status === 'ACCEPTED' && (
                          <button
                            className="open-view-btn"
                            style={{ backgroundColor: '#3498db' }}
                            onClick={() => startOrder(assignment.assignmentId)}
                          >
                            Start Delivery
                          </button>
                        )}
                        {assignment.status === 'IN_PROGRESS' && (
                          <button
                            className="open-view-btn"
                            style={{ backgroundColor: '#27ae60' }}
                            onClick={() => completeOrder(assignment.assignmentId)}
                          >
                            Complete
                          </button>
                        )}
                        <button
                          className="open-view-btn"
                          style={{ backgroundColor: '#e74c3c' }}
                          onClick={() => cancelAssignment(assignment.orderId)}
                        >
                          Cancel Assignment
                        </button>
                        <button
                          className="open-view-btn"
                          onClick={() => openOrderFullView(assignment)}
                        >
                          Full Details
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>

          {/* Rounds Section - Existing code */}
          <h2><strong>My Upcoming Rounds</strong></h2>
          {myRoundsError && <p className="error-text">{myRoundsError}</p>}
          <div className="rounds-cards">
            {myUpcomingRounds.length === 0 && <p>No upcoming rounds yet.</p>}
            {myUpcomingRounds.map((r) => (
              <div
                key={r.roundId}
                className="round-card"
                style={{
                  backgroundColor:
                    r.signupStatus === "CONFIRMED"
                      ? "lightgreen"
                      : r.signupStatus === "WAITLISTED"
                      ? "lightyellow"
                      : undefined
                }}
              >
                <h3><strong>{r.title}</strong></h3>
                <p>{r.description}</p>
                <p><strong>Location: </strong> 📍{r.location}</p>
                <p><strong>Start: </strong>{new Date(r.startTime).toLocaleString()}</p>
                <p><strong>End: </strong>{new Date(r.endTime).toLocaleString()}</p>
                <br />
                <button className="open-view-btn" onClick={() => openFullViewModal(r.roundId)}>
                  View Details
                </button>
              </div>
            ))}
          </div>

          <h2 onClick={() => setShowPastRounds(!showPastRounds)} style={{ cursor: "pointer" }}>
            <strong>My Past Rounds</strong>
            {showPastRounds ? " ▲ Hide all" : " ▼ View all"}
          </h2>
          {showPastRounds && (
            <div className="rounds-cards">
              {myPastRounds.length === 0 ? (
                <p>No past rounds available.</p>
              ) : (
                myPastRounds.map((r) => (
                  <div key={r.roundId} className="round-card">
                    <h3><strong>{r.title}</strong></h3>
                    <p>{r.description}</p>
                    <p><strong>Location: </strong>{r.location}</p>
                    <p><strong>Start: </strong>{new Date(r.startTime).toLocaleString()}</p>
                    <p><strong>End: </strong>{new Date(r.endTime).toLocaleString()}</p>
                  </div>
                ))
              )}
            </div>
          )}

          <h2 onClick={() => setShowCompletedOrders(!showCompletedOrders)} style={{ cursor: "pointer" }}>
            <strong>Completed Orders</strong>
            {showCompletedOrders ? " ▲ Hide all" : " ▼ View all"}
          </h2>
          {showCompletedOrders && (
            <div className="orders-cards">
              {completedAssignments.length === 0 ? (
                <p>No completed orders.</p>
              ) : (
                completedAssignments.map((a) => (
                  <div key={a.assignmentId} className="order-card">
                    <span className="completed-badge">Completed</span>
                    <h3>Order #{a.orderId}</h3>
                    <p>{a.deliveryAddress}</p>
                    <p>Items: {a.items?.map((i) => `${i.itemName} (${i.quantity})`).join(", ")}</p>
                    <button className="open-view-btn" onClick={() => openOrderFullView(a)}>
                      View Details
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        <div className="vertical-line"></div>

        <div className="volunteer-right-panel">
          <br /><br /><br />
          <h2><strong>Select a date to see rounds</strong></h2>
          <br /><br />
          {allRoundsError && <p className="error-text">{allRoundsError}</p>}
          <Calendar onClickDay={handleDateClick} tileClassName={highlightDates} />
          
          {showRoundsModal && (
            <div className="rounds-modal">
              <div className="rounds-modal-content">
                <h3>Rounds on {selectedDate.toDateString()}</h3>
                {roundsForSelectedDate.length === 0 && <p>No rounds scheduled for this date.</p>}
                {roundsForSelectedDate.map((r) => (
                  <div key={r.roundId} className="round-detail">
                    <h4>{r.title}</h4>
                    <p>{r.description}</p>
                    <p>Location: {r.location}</p>
                    <p>Start: {new Date(r.startTime).toLocaleString()}</p>
                    <p>End: {new Date(r.endTime).toLocaleString()}</p>
                    <p>Available Slots: {r.availableSlots}</p>
                    <p>Already Signed Up? {r.userSignedUp ? "Yes" : "No"}</p>
                    {r.userSignedUp ? (
                      <p style={{ color: "green" }}>You are already signed up.</p>
                    ) : r.openForSignup ? (
                      <button onClick={() => signupForRound(r.roundId, "VOLUNTEER")}>Sign Up</button>
                    ) : (
                      <p style={{ color: "red" }}>No slots available.</p>
                    )}
                  </div>
                ))}
                <button className="close-modal-btn" onClick={() => setShowRoundsModal(false)}>
                  Close
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Full View Modal for Rounds */}
        {fullViewModalOpen && selectedRoundDetails && (
          <div className="fullview-modal" onClick={closeFullViewModal}>
            <div className="fullview-modal-content" onClick={(e) => e.stopPropagation()}>
              <h2>{selectedRoundDetails.title}</h2>
              <p>____________________________________________________________</p>
              <br />
              <p><strong>Description:</strong> {selectedRoundDetails.description}</p>
              <p><strong>Location:</strong> {selectedRoundDetails.location}</p>
              <p><strong>Start:</strong> {new Date(selectedRoundDetails.startTime).toLocaleString()}</p>
              <p><strong>End:</strong> {new Date(selectedRoundDetails.endTime).toLocaleString()}</p>
              <p><strong>Available Slots:</strong> {selectedRoundDetails.availableSlots}</p>
              <p><strong>Already Signed Up?</strong> {selectedRoundDetails.userSignedUp ? "Yes" : "No"}</p>
              <br />
              <h3>Round Orders</h3>
              <div style={{
                marginTop: "10px",
                border: "1px solid #ccc",
                borderRadius: "8px",
                maxHeight: "200px",
                overflowY: "auto",
                padding: "10px"
              }}>
                {roundOrders.length === 0 ? (
                  <p>No orders for this round yet.</p>
                ) : (
                  roundOrders.map((ord) => (
                    <div key={ord.orderId} style={{ marginBottom: "12px", paddingBottom: "8px", borderBottom: "1px dashed #aaa" }}>
                      <p><strong>Order ID:</strong> {ord.orderId}</p>
                      <p><strong>Status:</strong> {ord.status}</p>
                      <p><strong>Type:</strong> {ord.orderType}</p>
                      <p><strong>Delivery:</strong> {ord.deliveryAddress}</p>
                      <p><strong>Items:</strong> {ord.orderItems?.map((i) => i.itemName).join(", ")}</p>
                      {ord.status === "PENDING" && (
                        <button 
                          style={{
                            marginTop: '5px',
                            padding: '5px 10px',
                            backgroundColor: '#009E2C',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            acceptOrder(ord.orderId, selectedRoundDetails.roundId);
                          }}
                        >
                          Accept This Order
                        </button>
                      )}
                    </div>
                  ))
                )}
              </div>

              {selectedRoundDetails.userSignedUp &&
                (selectedRoundDetails.signupDetails || selectedRoundDetails.signupId) && (
                  <button className="cancel-signup-btn" onClick={handleCancelSignupFullView}>
                    Cancel Signup
                  </button>
                )}
              <button className="close-modal-btn" onClick={closeFullViewModal}>
                Close
              </button>
            </div>
          </div>
        )}

        {/* Order Detail Modal */}
        {orderModalOpen && selectedOrder && (
          <div className="fullview-modal" onClick={closeOrderFullView}>
            <div className="fullview-modal-content" onClick={(e) => e.stopPropagation()}>
              <h2>Order Full Details</h2>
              <p>Order ID: {selectedOrder.orderId}</p>
              <p>Status: {selectedOrder.status}</p>
              <p>Order Type: {selectedOrder.orderType}</p>
              <p>User ID: {selectedOrder.userId}</p>
              <p>Delivery Address: {selectedOrder.deliveryAddress}</p>
              <p>Phone Number: {selectedOrder.phoneNumber}</p>
              <p>Note: {selectedOrder.notes}</p>
              <p>Order Time: {selectedOrder.requestTime ? new Date(selectedOrder.requestTime).toLocaleString() : ""}</p>
              {selectedOrder.items && selectedOrder.items.length > 0 && (
                <div>
                  <h4>Items:</h4>
                  {selectedOrder.items.map((itm, idx) => (
                    <p key={idx}>
                      {itm.itemName} - Quantity: {itm.quantity}
                    </p>
                  ))}
                </div>
              )}
              {selectedOrder.orderItems && selectedOrder.orderItems.length > 0 && (
                <div>
                  <h4>Items:</h4>
                  {selectedOrder.orderItems.map((itm, idx) => (
                    <p key={idx}>
                      {itm.itemName} - Quantity: {itm.quantity}
                    </p>
                  ))}
                </div>
              )}
              <button className="close-modal-btn" onClick={closeOrderFullView}>
                Close
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default Volunteer_Dashboard;