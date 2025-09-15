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

  const loadOrders = useCallback(async (status) => {
    try {
      setIsLoading(true);
      setOrdersError('');
      
      // Use secureAxios for admin operations (HTTPS required)
      const response = await secureAxios.get('/api/orders/all', {
        params: {
          authenticated: true,
          userId: userData.userId,
          userRole: "VOLUNTEER"
        }
      });
      
      const fetched = response.data.orders || [];
      setOrders(fetched.filter(o => o.status === status));
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

  const cancelOrder = async (orderId) => {
    if (!window.confirm(`Are you sure you want to cancel order ${orderId}?`)) {
      return;
    }
    
    try {
      const response = await secureAxios.post(`/api/orders/${orderId}/cancel`, {
        authenticated: true,
        userId: userData.userId,
        userRole: "VOLUNTEER"
      });
      alert(response.data.message);
      loadOrders(orderFilter);
    } catch (error) {
      console.error("Error cancelling order:", error);
      alert(error.response?.data?.message || error.message);
    }
  };

  const completeOrder = async (orderId) => {
    if (!window.confirm(`Mark order ${orderId} as completed?`)) {
      return;
    }
    
    try {
      const response = await secureAxios.put(`/api/orders/${orderId}/status`, {
        authenticated: true,
        userId: userData.userId,
        userRole: "VOLUNTEER",
        status: "COMPLETED"
      });
      alert(response.data.message || "Order completed successfully");
      loadOrders(orderFilter);
    } catch (error) {
      console.error("Error completing order:", error);
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

  const handleFilterChange = (status) => {
    setOrderFilter(status);
    loadOrders(status);
  };

  useEffect(() => {
    loadOrders(orderFilter);
  }, [loadOrders, orderFilter]);

  return (
    <div className="page-container">
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">Order Management</span>
          </div>
          <div className="header-right">
            <button
              className="manage-btn"
              onClick={() => navigate(-1)}
            >
              ← Go Back
            </button>
          </div>
        </div>
      </header>
  
      <main className="main-content">
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
            {["PENDING", "PROCESSING", "COMPLETED", "CANCELLED"].map(status => (
              <button
                key={status}
                className={`manage-btn filter-btn ${orderFilter === status ? "active" : ""}`}
                onClick={() => handleFilterChange(status)}
                disabled={isLoading}
              >
                {status.charAt(0) + status.slice(1).toLowerCase()}
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
                    <th>Item</th>
                    <th>Qty</th>
                    <th>Time</th>
                    <th>User ID</th>
                    <th>Address</th>
                    <th>Phone</th>
                    <th>Note</th>
                    <th>Type</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map(order =>
                    order.orderItems && order.orderItems.length > 0 ? (
                      order.orderItems.map((item, idx) => (
                        <tr key={`${order.orderId}-${idx}`}>
                          <td>{order.orderId}</td>
                          <td>
                            <span className={`status ${order.status.toLowerCase()}`}>
                              {order.status}
                            </span>
                          </td>
                          <td>{item.itemName}</td>
                          <td>{item.quantity}</td>
                          <td>{formatDateTime(order.requestTime)}</td>
                          <td>{order.userId || 'Guest'}</td>
                          <td>{order.deliveryAddress || 'N/A'}</td>
                          <td>{order.phoneNumber || 'N/A'}</td>
                          <td>{order.notes || 'N/A'}</td>
                          <td>{order.orderType || 'STANDARD'}</td>
                          <td>
                            {order.status === "PENDING" && (
                              <>
                                <button
                                  className="manage-btn complete-btn"
                                  onClick={() => completeOrder(order.orderId)}
                                  style={{ marginRight: '5px' }}
                                >
                                  Complete
                                </button>
                                <button
                                  className="manage-btn cancel-btn"
                                  onClick={() => cancelOrder(order.orderId)}
                                >
                                  Cancel
                                </button>
                              </>
                            )}
                            {order.status === "PROCESSING" && (
                              <>
                                <button
                                  className="manage-btn complete-btn"
                                  onClick={() => completeOrder(order.orderId)}
                                  style={{ marginRight: '5px' }}
                                >
                                  Complete
                                </button>
                                <button
                                  className="manage-btn cancel-btn"
                                  onClick={() => cancelOrder(order.orderId)}
                                >
                                  Cancel
                                </button>
                              </>
                            )}
                            {(order.status === "COMPLETED" || order.status === "CANCELLED") && (
                              <span style={{ color: '#999' }}>No actions</span>
                            )}
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr key={order.orderId}>
                        <td>{order.orderId}</td>
                        <td>
                          <span className={`status ${order.status.toLowerCase()}`}>
                            {order.status}
                          </span>
                        </td>
                        <td colSpan="3">No items</td>
                        <td>{formatDateTime(order.requestTime)}</td>
                        <td>{order.userId || 'Guest'}</td>
                        <td>{order.deliveryAddress || 'N/A'}</td>
                        <td>{order.phoneNumber || 'N/A'}</td>
                        <td>{order.notes || 'N/A'}</td>
                        <td>{order.orderType || 'STANDARD'}</td>
                        <td>
                          {(order.status === "PENDING" || order.status === "PROCESSING") && (
                            <button
                              className="manage-btn cancel-btn"
                              onClick={() => cancelOrder(order.orderId)}
                            >
                              Cancel
                            </button>
                          )}
                        </td>
                      </tr>
                    )
                  )}
                </tbody>
              </table>
            )}
          </div>
          
          {/* Order Summary */}
          {!isLoading && orders.length > 0 && (
            <div style={{ 
              marginTop: '20px', 
              padding: '10px', 
              backgroundColor: '#f5f5f5', 
              borderRadius: '4px' 
            }}>
              <strong>Summary:</strong> Showing {orders.length} {orderFilter.toLowerCase()} order(s)
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default AdminOrders;