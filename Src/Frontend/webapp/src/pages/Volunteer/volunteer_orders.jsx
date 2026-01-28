// Volunteer_Order.js
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { publicAxios } from '../../config/axiosConfig';
import '../../index.css'; 

const VolunteerOrders = ({ userData }) => {
  const navigate = useNavigate();

  // States for orders
  const [pendingOrders, setPendingOrders] = useState([]);
  const [myAssignments, setMyAssignments] = useState([]);
  const [ordersError, setOrdersError] = useState('');
  const [activeTab, setActiveTab] = useState("PENDING");
  const [isLoading, setIsLoading] = useState(false);
  const [roundInfo, setRoundInfo] = useState({ roundIds: [], message: '' });

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
        if (response.data.roundIds) {
          setRoundInfo({
            roundIds: response.data.roundIds,
            message: response.data.roundIds.length > 0 
              ? `Showing orders from your rounds: ${response.data.roundIds.join(', ')}`
              : 'You need to sign up for rounds to see orders'
          });
        }
      } else {
        setOrdersError(response.data.message || "Failed to load pending orders");
      }
    } catch (error) {
      console.error("Error loading pending orders:", error);
      if (error.response?.status === 403 || error.response?.data?.message?.includes('No rounds')) {
        setOrdersError("You need to be signed up for a round to view orders");
        setRoundInfo({ roundIds: [], message: 'Sign up for rounds to access orders' });
      } else {
        setOrdersError(error.response?.data?.message || error.message);
      }
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

  const activeAssignments = myAssignments.filter(
    a => a.status === 'ACCEPTED' || a.status === 'IN_PROGRESS'
  );

  const activeCount = activeAssignments.length;
  const completedCount = myAssignments.filter(a => a.status === 'COMPLETED').length;

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
  
  // Auto-refresh every 10 seconds when on ASSIGNED tab
  const interval = setInterval(() => {
    if (activeTab === "ASSIGNED") {
      loadMyAssignments();
    } else if (activeTab === "PENDING") {
      loadPendingOrders();
    }
  }, 10000);
  
  return () => clearInterval(interval);
}, [activeTab, loadPendingOrders, loadMyAssignments]);

  // ============================================
  // DARK THEME STYLES
  // ============================================
  const styles = {
    // Page container
    pageContainer: {
      backgroundColor: '#0f1c38',
      minHeight: '100vh'
    },
    // Header
    header: {
      backgroundColor: '#0f1c38',
      borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
      padding: '0 30px',
      height: '80px'
    },
    headerContent: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      height: '100%',
      width: '100%'
    },
    logoContainer: {
      display: 'flex',
      alignItems: 'center',
      gap: '15px'
    },
    siteTitle: {
      fontSize: '22px',
      fontWeight: '700',
      color: '#ffffff',
      fontFamily: "'Courier New', Courier, monospace"
    },
    headerRight: {
      display: 'flex',
      gap: '15px',
      alignItems: 'center'
    },
    badgeContainer: {
      display: 'flex',
      gap: '10px',
      alignItems: 'center',
      marginRight: '20px'
    },
    badge: {
      padding: '5px 10px',
      color: 'white',
      borderRadius: '15px',
      fontSize: '12px',
      fontWeight: 'bold'
    },
    // Main content
    mainContent: {
      padding: '30px',
      backgroundColor: '#0f1c38'
    },
    // Card
    ordersCard: {
      backgroundColor: '#1a2332',
      borderRadius: '16px',
      padding: '30px',
      border: '1px solid rgba(255, 255, 255, 0.08)'
    },
    cardHeader: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginBottom: '20px'
    },
    cardTitle: {
      fontSize: '28px',
      fontWeight: '700',
      color: '#ffffff',
      margin: 0
    },
    // Round info banner
    roundBannerSuccess: {
      padding: '10px 15px',
      backgroundColor: 'rgba(39, 174, 96, 0.2)',
      color: '#4ade80',
      borderRadius: '4px',
      margin: '10px 0',
      fontSize: '14px',
      fontWeight: '500',
      border: '1px solid rgba(39, 174, 96, 0.3)'
    },
    roundBannerWarning: {
      padding: '10px 15px',
      backgroundColor: 'rgba(255, 107, 0, 0.2)',
      color: '#ffb366',
      borderRadius: '4px',
      margin: '10px 0',
      fontSize: '14px',
      fontWeight: '500',
      border: '1px solid rgba(255, 107, 0, 0.3)'
    },
    // Tab buttons container
    tabContainer: {
      display: 'flex',
      gap: '15px',
      margin: '20px 0',
      justifyContent: 'center'
    },
    tabButtonActive: {
      padding: '12px 24px',
      borderRadius: '8px',
      fontSize: '14px',
      fontWeight: '600',
      cursor: 'pointer',
      border: 'none',
      color: 'white'
    },
    tabButtonInactive: {
      padding: '12px 24px',
      borderRadius: '8px',
      fontSize: '14px',
      fontWeight: '600',
      cursor: 'pointer',
      backgroundColor: '#2a3f5f',
      color: '#ffffff',
      border: '1px solid #3a5070'
    },
    // Error message
    errorMessage: {
      padding: '10px',
      margin: '10px 0',
      backgroundColor: 'rgba(231, 76, 60, 0.2)',
      color: '#ff6b6b',
      borderRadius: '4px',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      borderLeft: '4px solid #e74c3c'
    },
    // Loading
    loadingContainer: {
      padding: '40px',
      textAlign: 'center',
      color: '#cccccc'
    },
    // Empty state
    emptyState: {
      padding: '60px',
      textAlign: 'center',
      backgroundColor: '#212c46',
      borderRadius: '8px',
      border: '1px dashed rgba(255, 255, 255, 0.2)'
    },
    emptyStateTitle: {
      color: '#ffffff',
      fontSize: '24px',
      margin: 0
    },
    emptyStateText: {
      color: '#aaaaaa',
      marginTop: '10px'
    },
    emptyStateButton: {
      marginTop: '20px',
      padding: '10px 30px',
      backgroundColor: '#3498db',
      color: 'white',
      border: 'none',
      borderRadius: '5px',
      fontSize: '16px',
      cursor: 'pointer'
    },
    // Priority legend
    priorityLegend: {
      marginBottom: '20px',
      padding: '10px',
      backgroundColor: '#212c46',
      borderRadius: '8px',
      display: 'flex',
      gap: '20px',
      alignItems: 'center',
      border: '1px solid rgba(255, 255, 255, 0.1)'
    },
    priorityLegendText: {
      fontWeight: 'bold',
      color: '#ffffff'
    },
    // Order cards
    orderCard: {
      borderRadius: '8px',
      padding: '20px',
      backgroundColor: '#1a2332',
      position: 'relative',
      transition: 'all 0.3s',
      cursor: 'pointer',
      border: '1px solid #3a5070'
    },
    orderCardUrgent: {
      borderRadius: '8px',
      padding: '20px',
      backgroundColor: '#2a2a1a',
      position: 'relative',
      transition: 'all 0.3s',
      cursor: 'pointer',
      border: '2px solid #ff6b00'
    },
    urgentBadge: {
      position: 'absolute',
      top: '-10px',
      right: '20px',
      backgroundColor: '#ff6b00',
      color: 'white',
      padding: '5px 15px',
      borderRadius: '15px',
      fontSize: '12px',
      fontWeight: 'bold'
    },
    orderHeader: {
      display: 'flex',
      justifyContent: 'space-between',
      marginBottom: '15px'
    },
    orderTitle: {
      margin: 0,
      color: '#ffffff'
    },
    orderTitleUrgent: {
      margin: 0,
      color: '#ff6b00'
    },
    // Round info in order
    roundInfo: {
      marginBottom: '10px',
      padding: '8px',
      backgroundColor: 'rgba(52, 152, 219, 0.2)',
      borderRadius: '4px',
      fontSize: '14px',
      color: '#5dade2'
    },
    roundInfoStrong: {
      color: '#5dade2'
    },
    // Order details
    orderDetails: {
      marginBottom: '15px'
    },
    orderText: {
      marginBottom: '8px',
      color: '#cccccc'
    },
    orderStrong: {
      color: '#ffffff'
    },
    orderList: {
      margin: '0 0 0 20px',
      padding: 0
    },
    orderListItem: {
      marginBottom: '4px',
      color: '#cccccc'
    },
    customBadge: {
      marginLeft: '8px',
      backgroundColor: 'rgba(255, 97, 0, 0.2)',
      color: '#ff6100',
      padding: '2px 6px',
      borderRadius: '10px',
      fontSize: '11px',
      fontWeight: 'bold'
    },
    // Assignment card
    assignmentCard: {
      border: '1px solid #3a5070',
      borderRadius: '8px',
      padding: '20px',
      backgroundColor: '#1a2332'
    },
    assignmentRoundInfo: {
      marginBottom: '10px',
      padding: '6px 10px',
      backgroundColor: 'rgba(39, 174, 96, 0.2)',
      borderRadius: '4px',
      fontSize: '14px',
      display: 'inline-block',
      color: '#4ade80'
    },
    // Buttons container
    buttonContainer: {
      display: 'flex',
      gap: '10px'
    },
    // Completed message
    completedMessage: {
      width: '100%',
      textAlign: 'center',
      color: '#27ae60',
      fontWeight: 'bold',
      padding: '10px'
    }
  };

  return (
    <div className="page-container" style={styles.pageContainer}>
      <header className="site-header" style={styles.header}>
        <div className="header-content" style={styles.headerContent}>
          <div className="logo-container" style={styles.logoContainer}>
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title" style={styles.siteTitle}>Order Management Center</span>
          </div>
          <div className="header-right" style={styles.headerRight}>
            <div style={styles.badgeContainer}>
              <span style={{ ...styles.badge, backgroundColor: '#27ae60' }}>
                {activeCount} Active
              </span>
              <span style={{ ...styles.badge, backgroundColor: '#3498db' }}>
                {completedCount} Completed
              </span>
              <span style={{ ...styles.badge, backgroundColor: '#ff6b00' }}>
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
  
      <main className="main-content" style={styles.mainContent}>
        <div className="cargo-card orders-card" style={styles.ordersCard}>
          <div className="cargo-header orders-header" style={styles.cardHeader}>
            <h2 className="cargo-title orders-title" style={styles.cardTitle}>
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

          {/* Round Info Banner */}
          {roundInfo.message && activeTab === "PENDING" && (
            <div style={roundInfo.roundIds.length > 0 ? styles.roundBannerSuccess : styles.roundBannerWarning}>
              {roundInfo.message}
            </div>
          )}

          {/* Tab Buttons */}
          <div className="drawer-container orders-filterGroup" style={styles.tabContainer}>
            <button
              className={`manage-btn filter-btn ${activeTab === "PENDING" ? "active" : ""}`}
              onClick={() => setActiveTab("PENDING")}
              disabled={isLoading}
              style={activeTab === "PENDING" 
                ? { ...styles.tabButtonActive, backgroundColor: '#ff6b00' }
                : styles.tabButtonInactive
              }
            >
              🔥 Available Orders ({pendingOrders.length})
            </button>
            <button
              className={`manage-btn filter-btn ${activeTab === "ASSIGNED" ? "active" : ""}`}
              onClick={() => setActiveTab("ASSIGNED")}
              disabled={isLoading}
              style={activeTab === "ASSIGNED"
                ? { ...styles.tabButtonActive, backgroundColor: '#009E2C' }
                : styles.tabButtonInactive
              }
            >
              📋 My Assignments ({activeAssignments.length})
            </button>
          </div>

          {/* Error Message */}
          {ordersError && (
            <div style={styles.errorMessage}>
              <span>⚠️ {ordersError}</span>
              <button 
                onClick={() => setOrdersError('')}
                style={{ 
                  background: 'none', 
                  border: 'none', 
                  color: '#ff6b6b',
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
              <div style={styles.loadingContainer}>
                <div className="loading-spinner"></div>
                <p>Loading orders...</p>
              </div>
            ) : activeTab === "PENDING" ? (
              // Pending Orders View
              pendingOrders.length === 0 ? (
                <div style={styles.emptyState}>
                  <h3 style={styles.emptyStateTitle}>
                    {roundInfo.roundIds && roundInfo.roundIds.length > 0 
                      ? '🎉 All Clear!' 
                      : '📅 No Orders Available'}
                  </h3>
                  <p style={styles.emptyStateText}>
                    {roundInfo.roundIds && roundInfo.roundIds.length > 0 
                      ? "All orders in your rounds have been assigned. Check back later!"
                      : "You need to sign up for a round first to see available orders."}
                  </p>
                  {(!roundInfo.roundIds || roundInfo.roundIds.length === 0) && (
                    <button 
                      onClick={() => navigate('/')}
                      style={styles.emptyStateButton}
                    >
                      Go to Dashboard → Sign up for Rounds
                    </button>
                  )}
                </div>
              ) : (
                <div style={{ padding: '20px' }}>
                  <div style={styles.priorityLegend}>
                    <span style={styles.priorityLegendText}>Priority Legend:</span>
                    <span style={{ color: '#ff6b00' }}>⚡ Urgent (Oldest)</span>
                    <span style={{ color: '#f39c12' }}>🔥 High</span>
                    <span style={{ color: '#27ae60' }}>📦 Normal</span>
                  </div>
                  
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                    {pendingOrders.map((order, idx) => (
                      <div 
                        key={order.orderId} 
                        style={{
                          ...(idx === 0 ? styles.orderCardUrgent : styles.orderCard),
                          borderLeft: `5px solid ${getPriorityColor(idx)}`
                        }}
                        onMouseOver={(e) => e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.3)'}
                        onMouseOut={(e) => e.currentTarget.style.boxShadow = 'none'}
                      >
                        {idx === 0 && (
                          <div style={styles.urgentBadge}>
                            ⚡ URGENT - ACCEPT NOW
                          </div>
                        )}
                        
                        <div style={styles.orderHeader}>
                          <h3 style={idx === 0 ? styles.orderTitleUrgent : styles.orderTitle}>
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
                        
                        {order.roundId && (
                          <div style={styles.roundInfo}>
                            <strong style={styles.roundInfoStrong}>🔄 Round #{order.roundId}</strong>
                            {order.roundTitle && ` - ${order.roundTitle}`}
                          </div>
                        )}
                        
                        <div style={styles.orderDetails}>
                          <p style={styles.orderText}><strong style={styles.orderStrong}>📦 Items:</strong></p>
                          <ul style={styles.orderList}>
                            {order.items?.map((item, i) => (
                              <li key={i} style={{ 
                                ...styles.orderListItem,
                                color: item.isCustom ? '#ff6100' : '#cccccc' 
                              }}>
                                {item.itemName} × {item.quantity}
                                {item.size && ` (Size: ${item.size})`}
                                {item.isCustom && (
                                  <span style={styles.customBadge}>
                                    CUSTOM REQUEST
                                  </span>
                                )}
                              </li>
                            ))}
                          </ul>
                        </div>
                        <div style={styles.orderDetails}>
                          <p style={styles.orderText}>
                            <strong style={styles.orderStrong}>📍 Address:</strong> {order.deliveryAddress}
                          </p>
                          <p style={styles.orderText}>
                            <strong style={styles.orderStrong}>📞 Phone:</strong> {order.phoneNumber}
                          </p>
                          {order.notes && (
                            <p style={styles.orderText}>
                              <strong style={styles.orderStrong}>📝 Notes:</strong> {order.notes}
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
              activeAssignments.length === 0 ? (
                <div style={styles.emptyState}>
                  <h3 style={styles.emptyStateTitle}>No Active Assignments</h3>
                  <p style={styles.emptyStateText}>
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
                    {activeAssignments.map(assignment => (
                      <div 
                        key={assignment.assignmentId} 
                        style={{
                          ...styles.assignmentCard,
                          borderLeft: `5px solid ${
                            assignment.status === 'COMPLETED' ? '#27ae60' :
                            assignment.status === 'IN_PROGRESS' ? '#3498db' : '#f39c12'
                          }`
                        }}
                      >
                        <div style={styles.orderHeader}>
                          <h3 style={styles.orderTitle}>Assignment #{assignment.assignmentId}</h3>
                          <span style={{
                            padding: '5px 12px',
                            borderRadius: '15px',
                            fontSize: '12px',
                            fontWeight: 'bold',
                            backgroundColor: 
                              assignment.status === 'COMPLETED' ? 'rgba(39, 174, 96, 0.2)' :
                              assignment.status === 'IN_PROGRESS' ? 'rgba(52, 152, 219, 0.2)' : 'rgba(243, 156, 18, 0.2)',
                            color: 
                              assignment.status === 'COMPLETED' ? '#4ade80' :
                              assignment.status === 'IN_PROGRESS' ? '#5dade2' : '#f6b800'
                          }}>
                            {assignment.status.replace('_', ' ')}
                          </span>
                        </div>
                        
                        {assignment.roundId && (
                          <div style={styles.assignmentRoundInfo}>
                            <strong>Round #{assignment.roundId}</strong>
                          </div>
                        )}
                        
                        <div style={styles.orderDetails}>
                          <p style={styles.orderText}>
                            <strong style={styles.orderStrong}>Order ID:</strong> #{assignment.orderId}
                          </p>
                          <p style={styles.orderText}><strong style={styles.orderStrong}>Items:</strong></p>
                          <ul style={styles.orderList}>
                            {assignment.items?.map((item, i) => (
                              <li key={i} style={{ 
                                ...styles.orderListItem,
                                color: item.isCustom ? '#ff6100' : '#cccccc'
                              }}>
                                {item.itemName} × {item.quantity}
                                {item.size && ` (Size: ${item.size})`}
                                {item.isCustom && (
                                  <span style={styles.customBadge}>
                                    CUSTOM REQUEST
                                  </span>
                                )}
                              </li>
                            ))}
                          </ul>
                          <p style={{ ...styles.orderText, marginTop: '8px' }}>
                            <strong style={styles.orderStrong}>📍 Address:</strong> {assignment.deliveryAddress}
                          </p>
                          <p style={styles.orderText}>
                            <strong style={styles.orderStrong}>📞 Phone:</strong> {assignment.phoneNumber}
                          </p>
                          {assignment.notes && (
                            <p style={styles.orderText}>
                              <strong style={styles.orderStrong}>📝 Notes:</strong> {assignment.notes}
                            </p>
                          )}
                          <p style={styles.orderText}>
                            <strong style={styles.orderStrong}>⏰ Accepted:</strong> {formatDateTime(assignment.acceptedAt)}
                          </p>
                        </div>
                        
                        <div style={styles.buttonContainer}>
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
                            <div style={styles.completedMessage}>
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