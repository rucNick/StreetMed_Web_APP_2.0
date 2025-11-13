import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { secureAxios } from '../../config/axiosConfig';
import '../../css/Admin/Admin_Orders.css';

const AdminOrders = ({ userData }) => {
  const navigate = useNavigate();

  // ============= STATE MANAGEMENT =============
  const [orders, setOrders] = useState([]);
  const [ordersError, setOrdersError] = useState('');
  const [orderFilter, setOrderFilter] = useState("PENDING");
  const [isLoading, setIsLoading] = useState(false);
  const [roundCapacities, setRoundCapacities] = useState({});
  const [availableRounds, setAvailableRounds] = useState([]);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [selectedOrderForAssign, setSelectedOrderForAssign] = useState(null);
  const [selectedRoundId, setSelectedRoundId] = useState('');
  const [unassignedOrders, setUnassignedOrders] = useState([]);

  // ============= API FUNCTIONS =============
  
  // Load available rounds for assignment dropdown
  const loadAvailableRounds = async () => {
    try {
      const response = await secureAxios.get('/api/admin/rounds/upcoming', {
        params: {
          authenticated: true,
          adminUsername: userData.username
        }
      });
      
      if (response.data.status === "success") {
        setAvailableRounds(response.data.rounds || []);
      }
    } catch (error) {
      console.error("Error loading rounds:", error);
    }
  };

  // Load unassigned orders count - memoized to avoid dependency issues
  const loadUnassignedOrders = useCallback(async () => {
    try {
      const response = await secureAxios.get('/api/admin/rounds/orders/unassigned', {
        params: {
          authenticated: true,
          adminUsername: userData.username
        }
      });
      
      if (response.data.status === "success") {
        setUnassignedOrders(response.data.orders || []);
      }
    } catch (error) {
      console.error("Error loading unassigned orders:", error);
    }
  }, [userData.username]);

  // Load round capacity information - FIXED to use query params
  const loadRoundCapacity = async (roundId) => {
    try {
      const response = await secureAxios.get(`/api/orders/rounds/${roundId}/capacity`, {
        params: {
          authenticated: "true",
          userRole: "ADMIN"
        }
      });
      
      if (response.data.status === "success") {
        setRoundCapacities(prev => ({
          ...prev,
          [roundId]: response.data.summary
        }));
      }
    } catch (error) {
      console.error(`Error loading capacity for round ${roundId}:`, error);
    }
  };

  // Main function to load all orders - FIXED dependency
  const loadOrders = useCallback(async (status) => {
    try {
      setIsLoading(true);
      setOrdersError('');
      
      const response = await secureAxios.get('/api/orders/all', {
        params: {
          authenticated: true,
          userId: userData.userId,
          userRole: "ADMIN"
        }
      });
      
      if (response.data.status === "success") {
        const fetched = response.data.orders || [];
        setOrders(status === "ALL" ? fetched : fetched.filter(o => o.status === status));
        
        // Load round capacities for all unique rounds
        const uniqueRounds = [...new Set(fetched.map(o => o.roundId).filter(Boolean))];
        for (const roundId of uniqueRounds) {
          loadRoundCapacity(roundId);
        }
        
        // Load unassigned orders count
        loadUnassignedOrders();
      } else {
        setOrdersError(response.data.message || "Failed to load orders");
      }
    } catch (error) {
      console.error("Error loading orders:", error);
      if (error.response?.data?.httpsRequired) {
        setOrdersError("Secure HTTPS connection required for admin operations.");
      } else {
        setOrdersError(error.response?.data?.message || error.message);
      }
    } finally {
      setIsLoading(false);
    }
  }, [userData.userId, loadUnassignedOrders]); // Added loadUnassignedOrders to dependencies

  // ============= ORDER ACTIONS =============

  // Open assignment modal
  const openAssignModal = (order) => {
    setSelectedOrderForAssign(order);
    setSelectedRoundId(order.roundId || '');
    setAssignModalOpen(true);
    loadAvailableRounds();
  };

  // Assign order to a round
  const assignOrderToRound = async () => {
    if (!selectedOrderForAssign) return;
    
    try {
      const response = await secureAxios.put(
        `/api/admin/rounds/orders/${selectedOrderForAssign.orderId}/assign-round`,
        {
          authenticated: true,
          adminUsername: userData.username,
          roundId: selectedRoundId ? parseInt(selectedRoundId) : null
        }
      );
      
      if (response.data.status === "success") {
        alert(response.data.message);
        setAssignModalOpen(false);
        loadOrders(orderFilter);
      } else {
        alert(response.data.message || "Failed to assign order");
      }
    } catch (error) {
      console.error("Error assigning order:", error);
      alert(error.response?.data?.message || error.message);
    }
  };

  // Cancel an order
  const cancelOrder = async (orderId) => {
    if (!window.confirm(`Are you sure you want to cancel order ${orderId}?`)) {
      return;
    }
    
    try {
      const response = await secureAxios.post(`/api/orders/${orderId}/cancel`, {
        authenticated: true,
        userId: userData.userId,
        userRole: "ADMIN"
      });
      
      if (response.data.status === "success") {
        alert("Order cancelled successfully");
        loadOrders(orderFilter);
      } else {
        alert(response.data.message || "Failed to cancel order");
      }
    } catch (error) {
      console.error("Error cancelling order:", error);
      alert(error.response?.data?.message || error.message);
    }
  };

  // Update order status
  const updateOrderStatus = async (orderId, newStatus) => {
    if (!window.confirm(`Update order ${orderId} to ${newStatus}?`)) {
      return;
    }
    
    try {
      const response = await secureAxios.put(`/api/orders/${orderId}/status`, {
        authenticated: true,
        userId: userData.userId,
        userRole: "ADMIN",
        status: newStatus
      });
      
      if (response.data.status === "success") {
        alert("Order status updated successfully");
        loadOrders(orderFilter);
      } else {
        alert(response.data.message || "Failed to update order");
      }
    } catch (error) {
      console.error("Error updating order:", error);
      alert(error.response?.data?.message || error.message);
    }
  };

  // Delete an order permanently
  const deleteOrder = async (orderId) => {
    if (!window.confirm(`Are you sure you want to DELETE order ${orderId}? This action cannot be undone.`)) {
      return;
    }
    
    try {
      const response = await secureAxios.delete(`/api/orders/${orderId}`, {
        params: {
          authenticated: true,
          userId: userData.userId,
          userRole: "ADMIN"
        }
      });
      
      if (response.data.status === "success") {
        alert("Order deleted successfully");
        loadOrders(orderFilter);
      } else {
        alert(response.data.message || "Failed to delete order");
      }
    } catch (error) {
      console.error("Error deleting order:", error);
      alert(error.response?.data?.message || error.message);
    }
  };

  // ============= UTILITY FUNCTIONS =============

  // Calculate order age
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

  // Handle filter change
  const handleFilterChange = (status) => {
    setOrderFilter(status);
    loadOrders(status);
  };

  // Format order items with sizes
  const formatOrderItems = (orderItems) => {
    if (!orderItems || orderItems.length === 0) return 'N/A';
    
    return orderItems.map(item => {
      let itemText = `${item.itemName} (${item.quantity})`;
      if (item.size) {
        itemText = `${item.itemName} [${item.size}] (${item.quantity})`;
      }
      return itemText;
    }).join(', ');
  };

  // Get order type display
  const getOrderTypeDisplay = (order) => {
    // Check if it's a guest order based on userId
    if (order.userId === -1) {
      return 'GUEST';
    }
    // Otherwise use the orderType field
    return order.orderType || 'CLIENT';
  };

  // ============= EFFECTS =============
  
  useEffect(() => {
    loadOrders(orderFilter);
  }, [loadOrders, orderFilter]);

  // ============= COMPUTED VALUES =============
  
  // Calculate statistics
  const stats = {
    total: orders.length,
    pending: orders.filter(o => o.status === 'PENDING').length,
    processing: orders.filter(o => o.status === 'PROCESSING').length,
    completed: orders.filter(o => o.status === 'COMPLETED').length,
    cancelled: orders.filter(o => o.status === 'CANCELLED').length,
    unassigned: unassignedOrders.length
  };

  // ============= RENDER =============
  
  return (
    <div className="page-container">
      {/* HEADER SECTION */}
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">Order Management - Admin</span>
          </div>
          <div className="header-right">
            <span className="user-info">
              Logged in as: {userData.username} (Admin)
            </span>
            <button
              className="manage-btn"
              onClick={() => navigate('/admin')}
            >
              ← Back to Dashboard
            </button>
          </div>
        </div>
      </header>
  
      <main className="main-content">
        {/* STATISTICS BAR */}
        <div className="stats-bar">
          <div className="stat-item">
            <div className="stat-value">{stats.total}</div>
            <div className="stat-label">Total Orders</div>
          </div>
          <div className="stat-item">
            <div className="stat-value stat-pending">{stats.pending}</div>
            <div className="stat-label">Pending</div>
          </div>
          <div className="stat-item">
            <div className="stat-value stat-processing">{stats.processing}</div>
            <div className="stat-label">Processing</div>
          </div>
          <div className="stat-item">
            <div className="stat-value stat-completed">{stats.completed}</div>
            <div className="stat-label">Completed</div>
          </div>
          <div className="stat-item">
            <div className="stat-value stat-cancelled">{stats.cancelled}</div>
            <div className="stat-label">Cancelled</div>
          </div>
          <div className="stat-item">
            <div className="stat-value stat-unassigned">{stats.unassigned}</div>
            <div className="stat-label">Unassigned</div>
          </div>
        </div>

        {/* ORDERS CARD */}
        <div className="orders-card">
          <div className="orders-header">
            <h2 className="orders-title">All Orders</h2>
            <button
              className="manage-btn"
              onClick={() => loadOrders(orderFilter)}
              disabled={isLoading}
            >
              {isLoading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
  
          {/* FILTER BUTTONS */}
          <div className="orders-filterGroup">
            {["ALL", "PENDING", "PROCESSING", "COMPLETED", "CANCELLED"].map(status => (
              <button
                key={status}
                className={`filter-btn ${orderFilter === status ? "active" : ""}`}
                onClick={() => handleFilterChange(status)}
                disabled={isLoading}
              >
                {status.charAt(0) + status.slice(1).toLowerCase()}
                {status !== "ALL" && ` (${orders.filter(o => o.status === status).length})`}
              </button>
            ))}
          </div>
  
          {/* ERROR MESSAGE */}
          {ordersError && (
            <div className="error-message">
              Error: {ordersError}
            </div>
          )}
  
          {/* ORDERS TABLE */}
          <div className="table-scroll">
            {isLoading ? (
              <div className="loading-container">
                Loading orders...
              </div>
            ) : orders.length === 0 ? (
              <div className="empty-state">
                No {orderFilter.toLowerCase()} orders found.
              </div>
            ) : (
              <table className="orders-table">
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Age</th>
                    <th>Type</th>
                    <th>User</th>
                    <th>Items</th>
                    <th>Address</th>
                    <th>Phone</th>
                    <th>Notes</th>
                    <th>Round</th>
                    <th>Assigned To</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((order) => (
                    <tr key={order.orderId}>
                      <td>{order.orderId}</td>
                      <td>
                        <span className={`status ${order.status.toLowerCase()}`}>
                          {order.status}
                        </span>
                      </td>
                      <td>{getOrderAge(order.requestTime)}</td>
                      <td>{getOrderTypeDisplay(order)}</td>
                      <td>{order.userId === -1 ? 'Guest' : `User #${order.userId}`}</td>
                      <td title={formatOrderItems(order.orderItems)}>
                        {formatOrderItems(order.orderItems)}
                      </td>
                      <td>{order.deliveryAddress || 'N/A'}</td>
                      <td>{order.phoneNumber || 'N/A'}</td>
                      <td title={order.notes || 'N/A'}>
                        {order.notes ? 
                          (order.notes.length > 50 ? 
                            order.notes.substring(0, 50) + '...' : 
                            order.notes) : 
                          'N/A'}
                      </td>
                      <td>
                        {order.roundId ? (
                          <span className="round-info">
                            Round #{order.roundId}
                            {roundCapacities[order.roundId] && (
                              <span className="round-capacity">
                                <br />({roundCapacities[order.roundId].totalOrders}/{roundCapacities[order.roundId].maxCapacity})
                              </span>
                            )}
                          </span>
                        ) : (
                          <span className="unassigned-label">Unassigned</span>
                        )}
                      </td>
                      <td>
                        {order.assignedVolunteerId ? 
                          `Vol #${order.assignedVolunteerId}` : 
                          '-'}
                      </td>
                      <td>
                        <div className="action-buttons">
                          {/* ASSIGN BUTTON - Always visible */}
                          <button
                            className="manage-btn assign-btn"
                            onClick={() => openAssignModal(order)}
                            title="Assign to Round"
                          >
                            Assign
                          </button>
                          
                          {/* PENDING STATUS ACTIONS */}
                          {order.status === "PENDING" && (
                            <>
                              <button
                                className="manage-btn process-btn"
                                onClick={() => updateOrderStatus(order.orderId, 'PROCESSING')}
                                title="Mark as Processing"
                              >
                                Process
                              </button>
                              <button
                                className="manage-btn cancel-btn"
                                onClick={() => cancelOrder(order.orderId)}
                                title="Cancel Order"
                              >
                                Cancel
                              </button>
                            </>
                          )}
                          
                          {/* PROCESSING STATUS ACTIONS */}
                          {order.status === "PROCESSING" && (
                            <>
                              <button
                                className="manage-btn complete-btn"
                                onClick={() => updateOrderStatus(order.orderId, 'COMPLETED')}
                                title="Mark as Completed"
                              >
                                Complete
                              </button>
                              <button
                                className="manage-btn cancel-btn"
                                onClick={() => cancelOrder(order.orderId)}
                                title="Cancel Order"
                              >
                                Cancel
                              </button>
                            </>
                          )}
                          
                          {/* COMPLETED/CANCELLED STATUS ACTIONS */}
                          {(order.status === "COMPLETED" || order.status === "CANCELLED") && (
                            <button
                              className="manage-btn delete-btn"
                              onClick={() => deleteOrder(order.orderId)}
                              title="Delete Order"
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          
          {/* SUMMARY FOOTER */}
          {!isLoading && (
            <div className="summary-footer">
              <div>
                <strong>Total Orders:</strong> {orders.length}
                {orderFilter !== "ALL" && ` (filtered by ${orderFilter.toLowerCase()})`}
              </div>
              <div className="timestamp">
                Last updated: {new Date().toLocaleTimeString()}
              </div>
            </div>
          )}
        </div>
      </main>

      {/* ASSIGN ORDER MODAL */}
      {assignModalOpen && selectedOrderForAssign && (
        <div className="modal-overlay" onClick={() => setAssignModalOpen(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <h2>Assign Order #{selectedOrderForAssign.orderId} to Round</h2>
            
            <div className="modal-field">
              <label>
                <strong>Current Round:</strong> 
                {selectedOrderForAssign.roundId ? 
                  ` Round #${selectedOrderForAssign.roundId}` : 
                  ' Unassigned'}
              </label>
            </div>
            
            <div className="modal-field">
              <label>
                <strong>Select New Round:</strong>
                <select 
                  value={selectedRoundId} 
                  onChange={e => setSelectedRoundId(e.target.value)}
                >
                  <option value="">-- Unassign from Round --</option>
                  {availableRounds.map(round => (
                    <option key={round.roundId} value={round.roundId}>
                      Round #{round.roundId} - {round.title} 
                      ({round.currentOrderCount || 0}/{round.orderCapacity || 20} orders)
                      - {new Date(round.startTime).toLocaleDateString()}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            
            <div className="modal-buttons">
              <button 
                className="modal-cancel"
                onClick={() => setAssignModalOpen(false)}
              >
                Cancel
              </button>
              <button 
                className="modal-confirm"
                onClick={assignOrderToRound}
              >
                Assign Order
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminOrders;