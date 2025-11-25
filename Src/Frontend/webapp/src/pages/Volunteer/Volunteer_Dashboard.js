// Volunteer_Dashboard.js - FIXED VERSION
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
  
  // Orders states - Simplified
  const [myAssignments, setMyAssignments] = useState([]);
  const [ordersError, setOrdersError] = useState("");
  const [isLoadingOrders, setIsLoadingOrders] = useState(false);
  
  // Calendar and modals
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [roundsForSelectedDate, setRoundsForSelectedDate] = useState([]);
  const [showRoundsModal, setShowRoundsModal] = useState(false);
  const [fullViewModalOpen, setFullViewModalOpen] = useState(false);
  const [selectedRoundDetails, setSelectedRoundDetails] = useState(null);
  const [, setRoundOrders] = useState([]);
  const [orderModalOpen, setOrderModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showPastRounds, setShowPastRounds] = useState(false);
  const [showCompletedOrders, setShowCompletedOrders] = useState(false);

  // Load volunteer's assignments - FIXED without extra API call
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

  // Load my rounds with proper filtering - FIXED
  const loadMyRounds = useCallback(async () => {
    if (!userData || !userData.userId) return;
    try {
      const r = await publicAxios.get('/api/rounds/my-rounds', {
        params: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      const d = r.data;
      if (d.status === "success") {
        // Filter out cancelled rounds
        const upcoming = (d.upcomingRounds || []).filter(round => 
          round.status !== 'CANCELLED' && round.status !== 'CANCELED'
        );
        const past = (d.pastRounds || []).filter(round => 
          round.status !== 'CANCELLED' && round.status !== 'CANCELED'
        );
        
        setMyUpcomingRounds(upcoming);
        setMyPastRounds(past);
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
        // Filter out cancelled rounds
        const nonCancelledRounds = (d.rounds || []).filter(round => 
          round.status !== 'CANCELLED' && round.status !== 'CANCELED'
        );
        setAllUpcomingRounds(nonCancelledRounds);
      } else {
        setAllRoundsError(d.message || "Failed to load upcoming rounds");
      }
    } catch (e) {
      setAllRoundsError(e.response?.data?.message || e.message);
    }
  }, [userData]);

  // Other functions remain the same...
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

  useEffect(() => {
  if (!userData || !userData.userId) return;
  
  // Initial load
  loadMyRounds();
  loadAllUpcomingRounds();
  loadMyAssignments();
  
  // Set up auto-refresh for assignments ONLY
  const interval = setInterval(() => {
    loadMyAssignments();
  }, 30000); // Refresh every 30 seconds
  
  return () => clearInterval(interval);
}, [userData, loadMyRounds, loadAllUpcomingRounds, loadMyAssignments]); 

  // Separate useEffect for function updates
  useEffect(() => {
    // This just ensures functions are updated when needed
    // but doesn't cause re-renders
  }, [loadMyRounds, loadAllUpcomingRounds, loadMyAssignments]);

  const activeAssignments = myAssignments.filter(a => a.status !== 'COMPLETED' && a.status !== 'CANCELLED');
  const completedAssignments = myAssignments.filter(a => a.status === 'COMPLETED');

  // REST OF COMPONENT REMAINS THE SAME...
  return (
    <>
      {/* Keep all the JSX the same but use completedAssignments instead of allMyCompletedOrders */}
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
            📦 Order Management
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
          
          {/* My Current Assignments */}
          <div style={{ marginBottom: '30px', padding: '15px', borderRadius: '8px' }}>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <strong>📋 My Current Assignments</strong>
              <span style={{ fontSize: '14px', color: '#666' }}>
                ({activeAssignments.length} active)
              </span>
              <button 
                onClick={loadMyAssignments}
                style={{
                  marginLeft: 'auto',
                  padding: '5px 10px',
                  fontSize: '12px',
                  backgroundColor: '#27ae60',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                🔄 Refresh
              </button>
            </h2>
            
            {ordersError && <p className="error-text">{ordersError}</p>}
            
            <div className="orders-cards">
              {isLoadingOrders ? (
                <p>Loading assignments...</p>
              ) : activeAssignments.length === 0 ? (
                <div style={{ 
                  padding: '20px', 
                  textAlign: 'center',
                  backgroundColor: '#f9f9f9',
                  borderRadius: '8px'
                }}>
                  <p>No active assignments</p>
                  {myUpcomingRounds.length === 0 ? (
                    <p style={{ marginTop: '10px', color: '#666', fontSize: '14px' }}>
                      Sign up for rounds to access orders
                    </p>
                  ) : (
                    <button 
                      onClick={() => navigate("/volunteer/orders")}
                      style={{
                        marginTop: '10px',
                        padding: '8px 16px',
                        backgroundColor: '#ff6b00',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer'
                      }}
                    >
                      Go to Order Queue →
                    </button>
                  )}
                </div>
              ) : (
                activeAssignments.map((assignment) => (
                  <div key={assignment.assignmentId} className="order-card" style={{ 
                    borderLeft: assignment.status === 'IN_PROGRESS' ? '4px solid #3498db' : '4px solid #f39c12'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                      <h3 style={{ margin: 0 }}>Order #{assignment.orderId}</h3>
                      <span style={{
                        padding: '4px 12px',
                        borderRadius: '12px',
                        fontSize: '12px',
                        fontWeight: 'bold',
                        backgroundColor: assignment.status === 'ACCEPTED' ? '#fff3cd' : '#cce5ff',
                        color: assignment.status === 'ACCEPTED' ? '#856404' : '#004085'
                      }}>
                        {assignment.status === 'IN_PROGRESS' ? 'IN PROGRESS' : assignment.status}
                      </span>
                    </div>
                    
                    {assignment.roundId && (
                      <p style={{ marginBottom: '8px', color: '#666', fontSize: '14px' }}>
                        <strong>Round #{assignment.roundId}</strong>
                      </p>
                    )}
                    
                    <p><strong>Items:</strong> {assignment.items?.map(i => {
                      let itemText = `${i.itemName} (${i.quantity})`;
                      if (i.size) itemText = `${i.itemName} [${i.size}] (${i.quantity})`;
                      if (i.isCustom) itemText += ' 🛒[CUSTOM]';
                      return itemText;
                    }).join(', ')}</p>
                    <p><strong>Address:</strong> {assignment.deliveryAddress}</p>
                    <p><strong>Phone:</strong> {assignment.phoneNumber}</p>
                    {assignment.notes && <p><strong>Notes:</strong> {assignment.notes}</p>}
                    
                    <div style={{ marginTop: '10px', paddingTop: '10px', borderTop: '1px solid #eee' }}>
                      <button
                        className="open-view-btn"
                        onClick={() => openOrderFullView(assignment)}
                        style={{ width: '100%' }}
                      >
                        View Full Details
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* My Upcoming Rounds */}
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
                <p style={{ marginTop: '8px', fontSize: '14px', color: '#666' }}>
                  <strong>Orders:</strong> {r.currentOrderCount || 0} / {r.orderCapacity || 20}
                </p>
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

          {/* Completed Orders - Use completedAssignments from my-assignments */}
          <h2 onClick={() => setShowCompletedOrders(!showCompletedOrders)} style={{ cursor: "pointer" }}>
            <strong>Completed Orders</strong>
            {showCompletedOrders ? " ▲ Hide all" : " ▼ View all"}
            <span style={{ fontSize: '14px', color: '#666', marginLeft: '10px' }}>
              ({completedAssignments.length} total)
            </span>
          </h2>
          {showCompletedOrders && (
            <div className="orders-cards">
              {completedAssignments.length === 0 ? (
                <p>No completed orders yet.</p>
              ) : (
                completedAssignments.map((a) => (
                  <div key={a.assignmentId} className="order-card">
                    <span style={{
                      padding: '4px 12px',
                      borderRadius: '12px',
                      fontSize: '12px',
                      fontWeight: 'bold',
                      backgroundColor: '#d4edda',
                      color: '#155724'
                    }}>
                      ✓ Completed
                    </span>
                    <h3>Order #{a.orderId}</h3>
                    {a.roundId && <p><strong>Round:</strong> #{a.roundId}</p>}
                    <p>{a.deliveryAddress}</p>
                    <p>Items: {a.items?.map((i) => {
                      let itemText = `${i.itemName} (${i.quantity})`;
                      if (i.size) itemText = `${i.itemName} [${i.size}] (${i.quantity})`;
                      return itemText;
                    }).join(", ")}</p>
                    <button className="open-view-btn" onClick={() => openOrderFullView(a)}>
                      View Details
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* Rest of component remains same */}
        <div className="vertical-line"></div>

        <div className="volunteer-right-panel">
          <br /><br /><br />
          <h2><strong>Select a date to see rounds</strong></h2>
          <br /><br />
          {allRoundsError && <p className="error-text">{allRoundsError}</p>}
          <Calendar onClickDay={handleDateClick} tileClassName={highlightDates} />
          
          {/* Modals remain the same */}
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
                    <p>Order Capacity: {r.currentOrderCount || 0}/{r.orderCapacity || 20}</p>
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

        {/* Full View Modals remain the same */}
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
              <p><strong>Order Capacity:</strong> {selectedRoundDetails.currentOrderCount || 0}/{selectedRoundDetails.orderCapacity || 20}</p>
              <p><strong>Already Signed Up?</strong> {selectedRoundDetails.userSignedUp ? "Yes" : "No"}</p>
              
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

        {orderModalOpen && selectedOrder && (
          <div className="fullview-modal" onClick={closeOrderFullView}>
            <div className="fullview-modal-content" onClick={(e) => e.stopPropagation()}>
              <h2>Order Full Details</h2>
              <p>Order ID: {selectedOrder.orderId}</p>
              <p>Status: {selectedOrder.status}</p>
              {selectedOrder.roundId && <p>Round ID: {selectedOrder.roundId}</p>}
              <p>Delivery Address: {selectedOrder.deliveryAddress}</p>
              <p>Phone Number: {selectedOrder.phoneNumber}</p>
              <p>Note: {selectedOrder.notes}</p>
              <p>Order Time: {selectedOrder.requestTime ? new Date(selectedOrder.requestTime).toLocaleString() : "N/A"}</p>
              {selectedOrder.items && selectedOrder.items.length > 0 && (
                <div>
                  <h4>Items:</h4>
                  {selectedOrder.items.map((itm, idx) => (
                    <p key={idx}>
                      {itm.itemName} - Quantity: {itm.quantity}
                      {itm.size && ` (Size: ${itm.size})`}
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