import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { publicAxios } from '../../config/axiosConfig';
import '../../css/Volunteer/Volunteer_Order.css';

const VolunteerOrders = ({ userData }) => {
  const navigate = useNavigate();

  // States for orders
  const [pendingOrders, setPendingOrders] = useState([]);
  const [myAssignments, setMyAssignments] = useState([]);
  const [ordersError, setOrdersError] = useState('');
  const [activeTab, setActiveTab] = useState("PENDING");
  const [isLoading, setIsLoading] = useState(false);

  // Load pending orders from priority queue
  const loadPendingOrders = useCallback(async () => {
    try {
      setIsLoading(true);
      setOrdersError('');
      
      const response = await publicAxios.get('/api/orders/pending', {
        params: {
          page: 0,
          size: 20
        },
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
      setIsLoading(false);
    }
  }, [userData.userId]);

  // Load volunteer's assigned orders
  const loadMyAssignments = useCallback(async () => {
    try {
      setIsLoading(true);
      setOrdersError('');
      
      const response = await publicAxios.get('/api/orders/my-assignments', {
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
      setIsLoading(false);
    }
  }, [userData.userId]);

  // Accept an order (volunteer takes responsibility)
  const acceptOrder = async (orderId, roundId = null) => {
    if (!window.confirm(`Accept order ${orderId}? You will be responsible for fulfilling this order.`)) {
      return;
    }
    
    try {
      const response = await publicAxios.post(`/api/orders/${orderId}/accept`, {
        authenticated: true,
        volunteerId: userData.userId,
        userId: userData.userId,
        userRole: 'VOLUNTEER',
        orderId: orderId,
        roundId: roundId
      });
      
      if (response.data.status === "success") {
        alert("Order accepted successfully!");
        await loadPendingOrders();
        await loadMyAssignments();
        setActiveTab("ASSIGNED");
      } else {
        alert(response.data.message || "Failed to accept order");
      }
    } catch (error) {
      console.error("Error accepting order:", error);
      
      // Handle specific error messages
      if (error.response?.data?.message?.includes("ORDER_ALREADY_ACCEPTED_BY_YOU")) {
        alert("You have already accepted this order. Check your assignments tab.");
        await loadMyAssignments();
        setActiveTab("ASSIGNED");
      } else if (error.response?.status === 409 || error.response?.data?.message?.includes("ORDER_ALREADY_ACCEPTED")) {
        alert("This order has already been accepted by another volunteer.");
        await loadPendingOrders();
      } else {
        alert(error.response?.data?.message || "Failed to accept order");
      }
    }
  };

  // Cancel assignment (return order to pending queue)
  const cancelAssignment = async (orderId) => {
    if (!window.confirm(`Cancel your assignment for order ${orderId}? This will return the order to the pending queue.`)) {
      return;
    }
    
    try {
      const response = await publicAxios.delete(`/api/orders/${orderId}/assignment`, {
        headers: {
          'User-Id': userData.userId,
          'User-Role': 'VOLUNTEER',
          'Authentication-Status': 'true'
        }
      });
      
      if (response.data.status === "success") {
        alert("Assignment cancelled. Order returned to pending queue.");
        await loadMyAssignments();
        await loadPendingOrders();
      } else {
        alert(response.data.message || "Failed to cancel assignment");
      }
    } catch (error) {
      console.error("Error cancelling assignment:", error);
      alert(error.response?.data?.message || "Failed to cancel assignment");
    }
  };

  // Start working on an assigned order
  const startOrder = async (assignmentId) => {
    try {
      const response = await publicAxios.put(`/api/orders/assignment/${assignmentId}/start`, {}, {
        headers: {
          'User-Id': userData.userId,
          'User-Role': 'VOLUNTEER',
          'Authentication-Status': 'true'
        }
      });
      
      if (response.data.status === "success") {
        alert("Order processing started!");
        await loadMyAssignments();
      } else {
        alert(response.data.message || "Failed to start order");
      }
    } catch (error) {
      console.error("Error starting order:", error);
      alert(error.response?.data?.message || "Failed to start order");
    }
  };

  // Complete an assigned order
  const completeOrder = async (assignmentId) => {
    if (!window.confirm(`Mark this order as completed?`)) {
      return;
    }
    
    try {
      const response = await publicAxios.put(`/api/orders/assignment/${assignmentId}/complete`, {}, {
        headers: {
          'User-Id': userData.userId,
          'User-Role': 'VOLUNTEER',
          'Authentication-Status': 'true'
        }
      });
      
      if (response.data.status === "success") {
        alert("Order completed successfully!");
        await loadMyAssignments();
      } else {
        alert(response.data.message || "Failed to complete order");
      }
    } catch (error) {
      console.error("Error completing order:", error);
      alert(error.response?.data?.message || "Failed to complete order");
    }
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleString();
    } catch {
      return dateString;
    }
  };

  const getOrderAge = (requestTime) => {
    if (!requestTime) return 'Unknown';
    const now = new Date();
    const orderTime = new Date(requestTime);
    const diffMs = now - orderTime;
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 60) return `${diffMins} mins ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours} hours ago`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays} days ago`;
  };

  const getPriorityColor = (index) => {
    if (index === 0) return '#ff6b00';
    if (index < 3) return '#f39c12';
    if (index < 5) return '#27ae60';
    return '#95a5a6';
  };

  useEffect(() => {
    if (activeTab === "PENDING") {
      loadPendingOrders();
    } else if (activeTab === "ASSIGNED") {
      loadMyAssignments();
    }
  }, [activeTab, loadPendingOrders, loadMyAssignments]);

  const activeCount = myAssignments.filter(a => a.status !== 'COMPLETED' && a.status !== 'CANCELLED').length;
  const completedCount = myAssignments.filter(a => a.status === 'COMPLETED').length;
  return (
    <div className="page-container">
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">Order Management Center</span>
          </div>
          <div className="header-right">
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginRight: '20px' }}>
              <span style={{ 
                padding: '5px 10px', 
                backgroundColor: '#27ae60', 
                color: 'white', 
                borderRadius: '15px',
                fontSize: '12px',
                fontWeight: 'bold'
              }}>
                {activeCount} Active
              </span>
              <span style={{ 
                padding: '5px 10px', 
                backgroundColor: '#3498db', 
                color: 'white', 
                borderRadius: '15px',
                fontSize: '12px',
                fontWeight: 'bold'
              }}>
                {completedCount} Completed
              </span>
              <span style={{ 
                padding: '5px 10px', 
                backgroundColor: '#ff6b00', 
                color: 'white', 
                borderRadius: '15px',
                fontSize: '12px',
                fontWeight: 'bold'
              }}>
                {pendingOrders.length} Available
              </span>
            </div>
            <button
              className="manage-btn"
              onClick={() => navigate('/')}
              style={{ backgroundColor: '#009E2C' }}
            >
              ← Back to Dashboard
            </button>
          </div>
        </div>
      </header>
  
      <main className="main-content">
        <div className="cargo-card orders-card">
          <div className="cargo-header orders-header">
            <h2 className="cargo-title orders-title">
              {activeTab === "PENDING" ? "📦 Available Orders Queue" : "✅ My Assignments"}
            </h2>
            <button
              className="manage-btn"
              onClick={() => activeTab === "PENDING" ? loadPendingOrders() : loadMyAssignments()}
              disabled={isLoading}
              style={{ backgroundColor: '#009E2C' }}
            >
              {isLoading ? 'Refreshing...' : '🔄 Refresh'}
            </button>
          </div>

          {/* Tab Buttons */}
          <div className="drawer-container orders-filterGroup">
            <button
              className={`manage-btn filter-btn ${activeTab === "PENDING" ? "active" : ""}`}
              onClick={() => setActiveTab("PENDING")}
              disabled={isLoading}
              style={{ 
                backgroundColor: activeTab === "PENDING" ? '#ff6b00' : '#f0f0f0',
                color: activeTab === "PENDING" ? 'white' : 'black'
              }}
            >
              🔥 Available Orders ({pendingOrders.length})
            </button>
            <button
              className={`manage-btn filter-btn ${activeTab === "ASSIGNED" ? "active" : ""}`}
              onClick={() => setActiveTab("ASSIGNED")}
              disabled={isLoading}
              style={{ 
                backgroundColor: activeTab === "ASSIGNED" ? '#009E2C' : '#f0f0f0',
                color: activeTab === "ASSIGNED" ? 'white' : 'black'
              }}
            >
              📋 My Assignments ({activeCount})
            </button>
          </div>

          {/* Error Message */}
          {ordersError && (
            <div style={{ 
              padding: '10px', 
              margin: '10px 0', 
              backgroundColor: '#ffebee', 
              color: '#c62828', 
              borderRadius: '4px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <span>⚠️ {ordersError}</span>
              <button 
                onClick={() => setOrdersError('')}
                style={{ 
                  background: 'none', 
                  border: 'none', 
                  color: '#c62828',
                  fontSize: '20px',
                  cursor: 'pointer'
                }}
              >
                ×
              </button>
            </div>
          )}

          {/* Content Area */}
          <div className="table-scroll">
            {isLoading ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <div className="loading-spinner"></div>
                <p>Loading orders...</p>
              </div>
            ) : activeTab === "PENDING" ? (
              // Pending Orders View
              pendingOrders.length === 0 ? (
                <div style={{ 
                  padding: '60px', 
                  textAlign: 'center',
                  backgroundColor: '#f9f9f9',
                  borderRadius: '8px'
                }}>
                  <h3 style={{ color: '#27ae60', fontSize: '24px' }}>🎉 All Clear!</h3>
                  <p style={{ color: '#666', marginTop: '10px' }}>
                    No pending orders at the moment. Great job keeping up!
                  </p>
                </div>
              ) : (
                <div style={{ padding: '20px' }}>
                  <div style={{ 
                    marginBottom: '20px',
                    padding: '10px',
                    backgroundColor: '#f8f9fa',
                    borderRadius: '8px',
                    display: 'flex',
                    gap: '20px',
                    alignItems: 'center'
                  }}>
                    <span style={{ fontWeight: 'bold' }}>Priority Legend:</span>
                    <span style={{ color: '#ff6b00' }}>⚡ Urgent (Oldest)</span>
                    <span style={{ color: '#f39c12' }}>🔥 High</span>
                    <span style={{ color: '#27ae60' }}>📦 Normal</span>
                  </div>
                  
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                    {pendingOrders.map((order, idx) => (
                      <div 
                        key={order.orderId} 
                        style={{
                          border: idx === 0 ? '2px solid #ff6b00' : '1px solid #ddd',
                          borderLeft: `5px solid ${getPriorityColor(idx)}`,
                          borderRadius: '8px',
                          padding: '20px',
                          backgroundColor: idx === 0 ? '#fffaf0' : 'white',
                          position: 'relative',
                          transition: 'all 0.3s',
                          cursor: 'pointer'
                        }}
                        onMouseOver={(e) => e.currentTarget.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)'}
                        onMouseOut={(e) => e.currentTarget.style.boxShadow = 'none'}
                      >
                        {idx === 0 && (
                          <div style={{
                            position: 'absolute',
                            top: '-10px',
                            right: '20px',
                            backgroundColor: '#ff6b00',
                            color: 'white',
                            padding: '5px 15px',
                            borderRadius: '15px',
                            fontSize: '12px',
                            fontWeight: 'bold'
                          }}>
                            ⚡ URGENT - ACCEPT NOW
                          </div>
                        )}
                        
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '15px' }}>
                          <h3 style={{ margin: 0, color: idx === 0 ? '#ff6b00' : '#333' }}>
                            Order #{order.orderId}
                          </h3>
                          <span style={{ 
                            color: getPriorityColor(idx),
                            fontWeight: idx === 0 ? 'bold' : 'normal',
                            fontSize: '14px'
                          }}>
                            {getOrderAge(order.requestTime)}
                          </span>
                        </div>
                        
                        <div style={{ marginBottom: '15px' }}>
                          <p style={{ marginBottom: '8px' }}><strong>📦 Items:</strong></p>
                          <ul style={{ margin: '0 0 0 20px', padding: 0 }}>
                            {order.orderItems?.map((item, i) => (
                              <li key={i} style={{ marginBottom: '4px' }}>
                                {item.itemName} × {item.quantity}
                              </li>
                            ))}
                          </ul>
                        </div>
                        
                        <div style={{ marginBottom: '15px' }}>
                          <p style={{ marginBottom: '5px' }}>
                            <strong>📍 Address:</strong> {order.deliveryAddress}
                          </p>
                          <p style={{ marginBottom: '5px' }}>
                            <strong>📞 Phone:</strong> {order.phoneNumber}
                          </p>
                          {order.notes && (
                            <p style={{ marginBottom: '5px' }}>
                              <strong>📝 Notes:</strong> {order.notes}
                            </p>
                          )}
                          {order.roundId && (
                            <p style={{ marginBottom: '5px' }}>
                              <strong>🔄 Round:</strong> #{order.roundId}
                            </p>
                          )}
                        </div>
                        
                        <button
                          onClick={() => acceptOrder(order.orderId, order.roundId)}
                          style={{ 
                            width: '100%',
                            padding: '12px',
                            backgroundColor: idx === 0 ? '#ff6b00' : '#009E2C',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            fontSize: '16px',
                            fontWeight: 'bold',
                            cursor: 'pointer',
                            transition: 'all 0.3s'
                          }}
                          onMouseOver={(e) => e.target.style.opacity = '0.9'}
                          onMouseOut={(e) => e.target.style.opacity = '1'}
                        >
                          {idx === 0 ? '⚡ Accept Urgent Order' : '✓ Accept Order'}
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )
            ) : (
              // Assignments View
              myAssignments.length === 0 ? (
                <div style={{ 
                  padding: '60px', 
                  textAlign: 'center',
                  backgroundColor: '#f9f9f9',
                  borderRadius: '8px'
                }}>
                  <h3 style={{ color: '#666', fontSize: '24px' }}>No Active Assignments</h3>
                  <p style={{ color: '#999', marginTop: '10px' }}>
                    Accept orders from the Available Orders tab to get started!
                  </p>
                  <button 
                    onClick={() => setActiveTab("PENDING")}
                    style={{
                      marginTop: '20px',
                      padding: '10px 30px',
                      backgroundColor: '#ff6b00',
                      color: 'white',
                      border: 'none',
                      borderRadius: '5px',
                      fontSize: '16px',
                      fontWeight: 'bold',
                      cursor: 'pointer'
                    }}
                  >
                    View Available Orders
                  </button>
                </div>
              ) : (
                <div style={{ padding: '20px' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                    {myAssignments.map(assignment => (
                      <div 
                        key={assignment.assignmentId} 
                        style={{
                          border: '1px solid #ddd',
                          borderLeft: `5px solid ${
                            assignment.status === 'COMPLETED' ? '#27ae60' :
                            assignment.status === 'IN_PROGRESS' ? '#3498db' : '#f39c12'
                          }`,
                          borderRadius: '8px',
                          padding: '20px',
                          backgroundColor: 'white'
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '15px' }}>
                          <h3 style={{ margin: 0 }}>Assignment #{assignment.assignmentId}</h3>
                          <span style={{
                            padding: '5px 12px',
                            borderRadius: '15px',
                            fontSize: '12px',
                            fontWeight: 'bold',
                            backgroundColor: 
                              assignment.status === 'COMPLETED' ? '#d4edda' :
                              assignment.status === 'IN_PROGRESS' ? '#cce5ff' : '#fff3cd',
                            color: 
                              assignment.status === 'COMPLETED' ? '#155724' :
                              assignment.status === 'IN_PROGRESS' ? '#004085' : '#856404'
                          }}>
                            {assignment.status.replace('_', ' ')}
                          </span>
                        </div>
                        
                        <div style={{ marginBottom: '15px' }}>
                          <p style={{ marginBottom: '8px' }}>
                            <strong>Order ID:</strong> #{assignment.orderId}
                          </p>
                          <p style={{ marginBottom: '8px' }}><strong>Items:</strong></p>
                          <ul style={{ margin: '0 0 0 20px', padding: 0 }}>
                            {assignment.items?.map((item, i) => (
                              <li key={i} style={{ marginBottom: '4px' }}>
                                {item.itemName} × {item.quantity}
                              </li>
                            ))}
                          </ul>
                          <p style={{ marginTop: '8px', marginBottom: '5px' }}>
                            <strong>📍 Address:</strong> {assignment.deliveryAddress}
                          </p>
                          <p style={{ marginBottom: '5px' }}>
                            <strong>📞 Phone:</strong> {assignment.phoneNumber}
                          </p>
                          {assignment.notes && (
                            <p style={{ marginBottom: '5px' }}>
                              <strong>📝 Notes:</strong> {assignment.notes}
                            </p>
                          )}
                          <p style={{ marginBottom: '5px' }}>
                            <strong>⏰ Accepted:</strong> {formatDateTime(assignment.acceptedAt)}
                          </p>
                        </div>
                        
                        <div style={{ display: 'flex', gap: '10px' }}>
                          {assignment.status === 'ACCEPTED' && (
                            <>
                              <button
                                onClick={() => startOrder(assignment.assignmentId)}
                                style={{
                                  flex: 1,
                                  padding: '10px',
                                  backgroundColor: '#3498db',
                                  color: 'white',
                                  border: 'none',
                                  borderRadius: '5px',
                                  fontWeight: 'bold',
                                  cursor: 'pointer'
                                }}
                              >
                                🚀 Start Delivery
                              </button>
                              <button
                                onClick={() => cancelAssignment(assignment.orderId)}
                                style={{
                                  flex: 1,
                                  padding: '10px',
                                  backgroundColor: '#e74c3c',
                                  color: 'white',
                                  border: 'none',
                                  borderRadius: '5px',
                                  fontWeight: 'bold',
                                  cursor: 'pointer'
                                }}
                              >
                                ❌ Cancel
                              </button>
                            </>
                          )}
                          {assignment.status === 'IN_PROGRESS' && (
                            <>
                              <button
                                onClick={() => completeOrder(assignment.assignmentId)}
                                style={{
                                  flex: 1,
                                  padding: '10px',
                                  backgroundColor: '#27ae60',
                                  color: 'white',
                                  border: 'none',
                                  borderRadius: '5px',
                                  fontWeight: 'bold',
                                  cursor: 'pointer'
                                }}
                              >
                                ✅ Mark Complete
                              </button>
                              <button
                                onClick={() => cancelAssignment(assignment.orderId)}
                                style={{
                                  flex: 1,
                                  padding: '10px',
                                  backgroundColor: '#e74c3c',
                                  color: 'white',
                                  border: 'none',
                                  borderRadius: '5px',
                                  fontWeight: 'bold',
                                  cursor: 'pointer'
                                }}
                              >
                                ❌ Cancel
                              </button>
                            </>
                          )}
                          {assignment.status === 'COMPLETED' && (
                            <div style={{ 
                              width: '100%', 
                              textAlign: 'center', 
                              color: '#27ae60', 
                              fontWeight: 'bold',
                              padding: '10px'
                            }}>
                              ✅ Delivered Successfully
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

export default VolunteerOrders;