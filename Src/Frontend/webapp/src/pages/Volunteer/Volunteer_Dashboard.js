import React, { useState, useEffect, useCallback } from "react";
import Calendar from "react-calendar";
import "react-calendar/dist/Calendar.css";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "../../css/Volunteer/Volunteer_Dashboard.css";

const Volunteer_Dashboard = ({ userData, onLogout }) => {
  const baseURL = process.env.REACT_APP_BASE_URL;
  const navigate = useNavigate();

  const [myUpcomingRounds, setMyUpcomingRounds] = useState([]);
  const [myPastRounds, setMyPastRounds] = useState([]);
  const [myRoundsError, setMyRoundsError] = useState("");
  const [allUpcomingRounds, setAllUpcomingRounds] = useState([]);
  const [allRoundsError, setAllRoundsError] = useState("");
  const [assignedOrders, setAssignedOrders] = useState([]);
  const [ordersError, setOrdersError] = useState("");

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

  const loadMyRounds = useCallback(async () => {
    if (!userData || !userData.userId) return;
    try {
      const r = await axios.get(`${baseURL}/api/rounds/my-rounds`, {
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
      setMyRoundsError(e.response?.data?.message || e.message);
    }
  }, [userData, baseURL]);

  const loadAllUpcomingRounds = useCallback(async () => {
    if (!userData || !userData.userId) return;
    try {
      const r = await axios.get(`${baseURL}/api/rounds/all`, {
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
  }, [userData, baseURL]);

  const loadAssignedOrders = useCallback(async () => {
    if (!userData || !userData.userId) return;
    try {
      const r = await axios.get(`${baseURL}/api/orders/volunteer/assigned`, {
        params: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      const d = r.data;
      setAssignedOrders(d.orders || []);
    } catch (e) {
      setOrdersError(e.response?.data?.message || e.message);
    }
  }, [userData, baseURL]);

  const signupForRound = async (roundId, requestedRole = "VOLUNTEER") => {
    try {
      const r = await axios.post(`${baseURL}/api/rounds/${roundId}/signup`, {
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
      const r = await axios.get(`${baseURL}/api/rounds/${roundId}`, {
        params: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      if (r.data.status === "success") {
        setSelectedRoundDetails(r.data.round);
        setFullViewModalOpen(true);
      } else {
        alert(r.data.message);
        return;
      }
      const ordersRes = await axios.get(`${baseURL}/api/rounds/${roundId}/orders`, {
        params: { authenticated: true, userId: userData.userId, userRole: "VOLUNTEER" }
      });
      if (ordersRes.data.status === "success") {
        setRoundOrders(ordersRes.data.orders || []);
      } else {
        alert(ordersRes.data.message);
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
      const r = await axios.delete(`${baseURL}/api/rounds/signup/${s}`, {
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

  const handleCompleteOrder = async (orderId) => {
    try {
      const r = await axios.put(`${baseURL}/api/orders/${orderId}/status`, {
        authenticated: true,
        userId: userData.userId,
        userRole: "VOLUNTEER",
        status: "COMPLETED"
      });
      alert(r.data.message);
      loadAssignedOrders();
    } catch (e) {
      alert(e.response?.data?.message || e.message);
    }
  };

  useEffect(() => {
    if (!userData || !userData.userId) return;
    loadMyRounds();
    loadAllUpcomingRounds();
    loadAssignedOrders();
  }, [loadMyRounds, loadAllUpcomingRounds, loadAssignedOrders, userData]);

  const pendingOrders = assignedOrders.filter((o) => o.status === "PENDING");
  const completedOrders = assignedOrders.filter((o) => o.status === "COMPLETED");

  return (
    <>
      <header className="nav-bar">
        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
          <img className="nav-logo" src="/Untitled.png" alt="Logo" />
          <span className="nav-welcome"><h2>Welcome Back, {userData.username} !</h2></span>
          <button
          className="nav-btn"
          style={{ marginLeft: "10px" }}
          onClick={() => navigate("/profile")}
        >
          Profile
        </button>
        </div>
        <div className="nav-right-group">
          <button className="nav-btn" onClick={() => navigate("/cargo_volunteer")}>
            Cargo Volunteer
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
          <h2>
            <strong>My Upcoming Rounds</strong>
          </h2>
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
                <h3>
                  <strong>{r.title}</strong>
                </h3>
                <p>{r.description}</p>
                <p>
                  <strong>Location: </strong> 📍{r.location}
                </p>
                <p>
                  <strong>Start: </strong>
                  {new Date(r.startTime).toLocaleString()}
                </p>
                <p>
                  <strong>End: </strong>
                  {new Date(r.endTime).toLocaleString()}
                </p>
                <br />
                <button className="open-view-btn" onClick={() => openFullViewModal(r.roundId)}>
                  View Details
                </button>
              </div>
            ))}
          </div>

          <h2>
            <strong>Assigned Orders</strong>
          </h2>
          {ordersError && <p className="error-text">{ordersError}</p>}
          <div className="orders-cards">
            {pendingOrders.map((o) => (
              <div key={o.orderId} className="order-card">
                <h3>{o.orderType}</h3>
                <p>{o.deliveryAddress}</p>
                <p>Items: {o.orderItems?.map((i) => i.itemName).join(", ")}</p>
                <div style={{ display: "flex", gap: "8px", marginTop: "10px" }}>
                  <button className="open-view-btn" onClick={() => openOrderFullView(o)}>
                    Open full view
                  </button>
                  <button
                    className="open-view-btn"
                    style={{ backgroundColor: "#009E2C" }}
                    onClick={() => handleCompleteOrder(o.orderId)}
                  >
                    Complete
                  </button>
                </div>
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
                    <h3>
                      <strong>{r.title}</strong>
                    </h3>
                    <p>{r.description}</p>
                    <p>
                      <strong>Location: </strong>
                      {r.location}
                    </p>
                    <p>
                      <strong>Start: </strong>
                      {new Date(r.startTime).toLocaleString()}
                    </p>
                    <p>
                      <strong>End: </strong>
                      {new Date(r.endTime).toLocaleString()}
                    </p>
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
              {completedOrders.length === 0 ? (
                <p>No completed orders.</p>
              ) : (
                completedOrders.map((o) => (
                  <div key={o.orderId} className="order-card">
                    <span className="completed-badge">Completed</span>
                    <h3>{o.orderType}</h3>
                    <p>{o.deliveryAddress}</p>
                    <p>Items: {o.orderItems?.map((i) => i.itemName).join(", ")}</p>
                    <button className="open-view-btn" onClick={() => openOrderFullView(o)}>
                      Open full view
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        <div className="vertical-line"></div>

        <div className="volunteer-right-panel">
          <br />
          <br />
          <br />
          <h2>
            <strong>Select a date to see rounds</strong>
          </h2>
          <br />
          <br />
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
                      <p style={{ color: "red" }}>
                        No slots available (waitlist not shown in this demo).
                      </p>
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

        {fullViewModalOpen && selectedRoundDetails && (
          <div className="fullview-modal" onClick={closeFullViewModal}>
            <div className="fullview-modal-content" onClick={(e) => e.stopPropagation()}>
              <h2>{selectedRoundDetails.title}</h2>
              <p>____________________________________________________________</p>
              <br />
              <p>
                <strong>Description:</strong> {selectedRoundDetails.description}
              </p>
              <p>
                <strong>Location:</strong> {selectedRoundDetails.location}
              </p>
              <p>
                <strong>Start:</strong> {new Date(selectedRoundDetails.startTime).toLocaleString()}
              </p>
              <p>
                <strong>End:</strong> {new Date(selectedRoundDetails.endTime).toLocaleString()}
              </p>
              <p>
                <strong>Available Slots:</strong> {selectedRoundDetails.availableSlots}
              </p>
              <p>
                <strong>Already Signed Up?</strong> {selectedRoundDetails.userSignedUp ? "Yes" : "No"}
              </p>
              <br />
              <h3>Assigned Orders for this Round</h3>
              <div
                style={{
                  marginTop: "10px",
                  border: "1px solid #ccc",
                  borderRadius: "8px",
                  maxHeight: "200px",
                  overflowY: "auto",
                  padding: "10px"
                }}
              >
                {roundOrders.length === 0 ? (
                  <p>No orders for this round yet.</p>
                ) : (
                  roundOrders.map((ord) => (
                    <div key={ord.orderId} style={{ marginBottom: "12px", paddingBottom: "8px", borderBottom: "1px dashed #aaa" }}>
                      <p>
                        <strong>Order ID:</strong> {ord.orderId}
                      </p>
                      <p>
                        <strong>Status:</strong> {ord.status}
                      </p>
                      <p>
                        <strong>Type:</strong> {ord.orderType}
                      </p>
                      <p>
                        <strong>Delivery:</strong> {ord.deliveryAddress}
                      </p>
                      <p>
                        <strong>Items:</strong> {ord.orderItems?.map((i) => i.itemName).join(", ")}
                      </p>
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
              <p>
                Order Time:{" "}
                {selectedOrder.requestTime
                  ? new Date(selectedOrder.requestTime).toLocaleString()
                  : ""}
              </p>
              {selectedOrder.orderItems && selectedOrder.orderItems.length > 0 && (
                <div>
                  {selectedOrder.orderItems.map((itm, idx) => (
                    <p key={idx}>
                      Item Name: {itm.itemName}, Quantity: {itm.quantity}
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
