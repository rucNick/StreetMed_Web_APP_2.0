import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import '../../css/Admin/Admin_Orders.css';

const AdminOrders = ({ userData }) => {
  const baseURL = process.env.REACT_APP_BASE_URL;
  const navigate = useNavigate();

  const [orders, setOrders] = useState([]);
  const [ordersError, setOrdersError] = useState('');
  const [orderFilter, setOrderFilter] = useState("PENDING");

  const loadOrders = useCallback(async (status) => {
    try {
      const response = await axios.get(
        `${baseURL}/api/orders/all`,
        {
          params: {
            authenticated: true,
            userId: userData.userId,
            userRole: "VOLUNTEER"
          }
        }
      );
      const fetched = response.data.orders || [];
      setOrders(fetched.filter(o => o.status === status));
    } catch (error) {
      setOrdersError(error.response?.data?.message || error.message);
    }
  }, [baseURL, userData.userId]);

  const cancelOrder = async (orderId) => {
    try {
      const response = await axios.post(
        `${baseURL}/api/orders/${orderId}/cancel`,
        {
          authenticated: true,
          userId: userData.userId,
          userRole: "VOLUNTEER"
        }
      );
      alert(response.data.message);
      loadOrders(orderFilter);
    } catch (error) {
      alert(error.response?.data?.message || error.message);
    }
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
            >
              Refresh
            </button>
          </div>
  
          <div className="drawer-container orders-filterGroup">
            {["PENDING","COMPLETED","CANCELLED"].map(status => (
              <button
                key={status}
                className={`manage-btn filter-btn ${orderFilter===status ? "active" : ""}`}
                onClick={() => setOrderFilter(status)}
              >
                {status.charAt(0)+status.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
  
          {ordersError && (
            <p className="cargo-error orders-error">{ordersError}</p>
          )}
  
          <div className="table-scroll">
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
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {orders.map(order =>
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
                      <td>{new Date(order.requestTime).toLocaleString()}</td>
                      <td>{order.userId}</td>
                      <td>{order.deliveryAddress}</td>
                      <td>{order.phoneNumber}</td>
                      <td>{order.notes}</td>
                      <td>{order.orderType}</td>
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
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
};

export default AdminOrders;
