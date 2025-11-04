import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { secureAxios } from '../../config/axiosConfig';
import '../../css/Admin/Admin_Orders.css';

const AdminOrders = ({ userData }) => {
  const navigate = useNavigate();

  const [orders, setOrders] = useState([]);
  const [ordersError, setOrdersError] = useState('');
  const [orderFilter, setOrderFilter] = useState("PENDING");
  const [isLoading, setIsLoading] = useState(false);
  const [roundCapacities, setRoundCapacities] = useState({});

  // Load all orders for admin view
  const loadOrders = useCallback(async (status) => {
    try {
      setIsLoading(true);
      setOrdersError('');
      
      // Use query parameters instead of headers to avoid CORS issues
      const response = await secureAxios.get('/api/orders/all', {
        params: {
          authenticated: true,
          userId: userData.userId,
          userRole: "ADMIN"  // Changed to ADMIN
        }
      });
      
      if (response.data.status === "success") {
        const fetched = response.data.orders || [];
        setOrders(status === "ALL" ? fetched : fetched.filter(o => o.status === status));
        
        // Load round capacities for monitoring
        const uniqueRounds = [...new Set(fetched.map(o => o.roundId).filter(Boolean))];
        for (const roundId of uniqueRounds) {
          loadRoundCapacity(roundId);
        }
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
  }, [userData.userId]);

  // Load round capacity for monitoring
  const loadRoundCapacity = async (roundId) => {
    try {
      const response = await secureAxios.get(`/api/orders/rounds/${roundId}/capacity`, {
        params: {
          authenticated: true,
          userRole: "ADMIN"
        }
      });
      
      if (response.data.status === "success") {
        setRoundCapacities(prev => ({
          ...prev,
          [roundId]: response.data.capacity
        }));
      }
    } catch (error) {
      console.error(`Error loading capacity for round ${roundId}:`, error);
    }
  };

  // Cancel an order (Admin action)
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

  // Update order status (Admin action)
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

  // Delete order (Admin action)
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
    
    if (diffMins < 60) return `${diffMins} mins`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d`;
  };

  const handleFilterChange = (status) => {
    setOrderFilter(status);
    loadOrders(status);
  };

  useEffect(() => {
    loadOrders(orderFilter);
  }, [loadOrders, orderFilter]);

  // Calculate statistics
  const stats = {
    total: orders.length,
    pending: orders.filter(o => o.status === 'PENDING').length,
    processing: orders.filter(o => o.status === 'PROCESSING').length,
    completed: orders.filter(o => o.status === 'COMPLETED').length,
    cancelled: orders.filter(o => o.status === 'CANCELLED').length
  };

  return (
    <div className="page-container">
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">Order Management - Admin</span>
          </div>
          <div className="header-right">
            <span style={{ marginRight: '20px', color: '#666', fontSize: '14px' }}>
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
        {/* Statistics Bar */}
        <div style={{ 
          display: 'flex', 
          gap: '15px', 
          marginBottom: '20px',
          padding: '15px',
          backgroundColor: '#f5f5f5',
          borderRadius: '8px'
        }}>
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#333' }}>{stats.total}</div>
            <div style={{ fontSize: '12px', color: '#666' }}>Total Orders</div>
          </div>
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#ff6b00' }}>{stats.pending}</div>
            <div style={{ fontSize: '12px', color: '#666' }}>Pending</div>
          </div>
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#3498db' }}>{stats.processing}</div>
            <div style={{ fontSize: '12px', color: '#666' }}>Processing</div>
          </div>
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#27ae60' }}>{stats.completed}</div>
            <div style={{ fontSize: '12px', color: '#666' }}>Completed</div>
          </div>
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#e74c3c' }}>{stats.cancelled}</div>
            <div style={{ fontSize: '12px', color: '#666' }}>Cancelled</div>
          </div>
        </div>

        <div className="cargo-card orders-card">
          <div className="cargo-header orders-header">
            <h2 className="cargo-title orders-title">All Orders</h2>
            <button
              className="manage-btn"
              onClick={() => loadOrders(orderFilter)}
              disabled={isLoading}
            >
              {isLoading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
  
          {/* Filter Buttons */}
          <div className="drawer-container orders-filterGroup">
            {["ALL", "PENDING", "PROCESSING", "COMPLETED", "CANCELLED"].map(status => (
              <button
                key={status}
                className={`manage-btn filter-btn ${orderFilter === status ? "active" : ""}`}
                onClick={() => handleFilterChange(status)}
                disabled={isLoading}
              >
                {status.charAt(0) + status.slice(1).toLowerCase()}
                {status !== "ALL" && ` (${orders.filter(o => o.status === status).length})`}
              </button>
            ))}
          </div>
  
          {/* Error Message */}
          {ordersError && (
            <div style={{ 
              padding: '10px', 
              margin: '10px 0', 
              backgroundColor: '#ffebee', 
              color: '#c62828', 
              borderRadius: '4px' 
            }}>
              Error: {ordersError}
            </div>
          )}
  
          {/* Orders Table */}
          <div className="table-scroll">
            {isLoading ? (
              <div style={{ padding: '20px', textAlign: 'center' }}>
                Loading orders...
              </div>
            ) : orders.length === 0 ? (
              <div style={{ padding: '20px', textAlign: 'center' }}>
                No {orderFilter.toLowerCase()} orders found.
              </div>
            ) : (
              <table className="cargo-table orders-table">
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
                      <td>{order.orderType || 'STANDARD'}</td>
                      <td>{order.userId === -1 ? 'Guest' : `User #${order.userId}`}</td>
                      <td>
                        {order.orderItems?.map(item => 
                          `${item.itemName} (${item.quantity})`
                        ).join(', ') || 'N/A'}
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
                          <span>
                            Round #{order.roundId}
                            {roundCapacities[order.roundId] && (
                              <span style={{ fontSize: '11px', color: '#666' }}>
                                <br />({roundCapacities[order.roundId].current}/{roundCapacities[order.roundId].max})
                              </span>
                            )}
                          </span>
                        ) : 'Any'}
                      </td>
                      <td>
                        {order.assignedVolunteerId ? 
                          `Vol #${order.assignedVolunteerId}` : 
                          '-'}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '5px', flexWrap: 'wrap' }}>
                          {order.status === "PENDING" && (
                            <>
                              <button
                                className="manage-btn"
                                onClick={() => updateOrderStatus(order.orderId, 'PROCESSING')}
                                style={{ backgroundColor: '#3498db', fontSize: '12px', padding: '4px 8px' }}
                                title="Mark as Processing"
                              >
                                Process
                              </button>
                              <button
                                className="manage-btn cancel-btn"
                                onClick={() => cancelOrder(order.orderId)}
                                style={{ fontSize: '12px', padding: '4px 8px' }}
                                title="Cancel Order"
                              >
                                Cancel
                              </button>
                            </>
                          )}
                          {order.status === "PROCESSING" && (
                            <>
                              <button
                                className="manage-btn complete-btn"
                                onClick={() => updateOrderStatus(order.orderId, 'COMPLETED')}
                                style={{ fontSize: '12px', padding: '4px 8px' }}
                                title="Mark as Completed"
                              >
                                Complete
                              </button>
                              <button
                                className="manage-btn cancel-btn"
                                onClick={() => cancelOrder(order.orderId)}
                                style={{ fontSize: '12px', padding: '4px 8px' }}
                                title="Cancel Order"
                              >
                                Cancel
                              </button>
                            </>
                          )}
                          {(order.status === "COMPLETED" || order.status === "CANCELLED") && (
                            <button
                              className="manage-btn"
                              onClick={() => deleteOrder(order.orderId)}
                              style={{ backgroundColor: '#e74c3c', fontSize: '12px', padding: '4px 8px' }}
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
          
          {/* Summary Footer */}
          {!isLoading && (
            <div style={{ 
              marginTop: '20px', 
              padding: '15px', 
              backgroundColor: '#f9f9f9', 
              borderRadius: '4px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <div>
                <strong>Total Orders:</strong> {orders.length}
                {orderFilter !== "ALL" && ` (filtered by ${orderFilter.toLowerCase()})`}
              </div>
              <div style={{ fontSize: '12px', color: '#666' }}>
                Last updated: {new Date().toLocaleTimeString()}
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default AdminOrders;