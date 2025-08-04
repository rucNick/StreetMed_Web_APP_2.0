import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

import '../../css/Admin/Admin.css';

const Admin = ({ onLogout, userData }) => {
  const navigate = useNavigate();
  const baseURL = process.env.REACT_APP_BASE_URL;

  const [outOfStockCount, setOutOfStockCount] = useState(0);
  const [lowStockCount, setLowStockCount] = useState(0);
  const [pendingOrdersCount, setPendingOrdersCount] = useState(0);
  const [pendingAppsCount, setPendingAppsCount] = useState(0);

  const fetchAlerts = useCallback(async () => {
    try {
      const itemsResp = await axios.get(`${baseURL}/api/cargo/items`, {
        headers: {
          "Admin-Username": userData.username,
          "Authentication-Status": "true",
        },
      });
  
      const listRaw = itemsResp.data;
      const items = Array.isArray(listRaw)
        ? listRaw
        : Array.isArray(listRaw.items)
        ? listRaw.items
        : [];
  
      const getQty = (it) =>
        it.totalQuantity ?? it.quantity ?? it.stock ?? 0;
  
      setOutOfStockCount(items.filter((i) => getQty(i) === 0).length);
  
      setLowStockCount(
        items.filter((i) => {
          const q = getQty(i);
          return q > 0 && q < 5;
        }).length
      );
  
      const ordersResp = await axios.get(`${baseURL}/api/orders/all`, {
        params: {
          authenticated: true,
          userId: userData.userId,
          userRole: "VOLUNTEER",
        },
      });
      const orders = ordersResp.data.orders || [];
      setPendingOrdersCount(orders.filter((o) => o.status === "PENDING").length);
  
      const appsResp = await axios.get(`${baseURL}/api/volunteer/pending`, {
        headers: {
          "Admin-Username": userData.username,
          "Authentication-Status": "true",
        },
      });
      setPendingAppsCount((appsResp.data.data || []).length);
    } catch (e) {
      console.error("Failed to fetch alerts", e);
    }
  }, [baseURL, userData]);
  

  useEffect(() => {
    fetchAlerts();
  }, [fetchAlerts]);

  const handleLogout = () => {
    onLogout();
    navigate('/');
  };

  return (
    <div className="page-container">
      <header className="store-header">
        <img src="/Untitled.png" alt="Logo" className="store-logo" />
        <button className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </header>

      <main className="admin-dashboard">
        <section className="admin-left">
          <h1 className="admin-greeting">
            Hello, {userData.firstName || userData.username}
          </h1>

          <div
            className="admin-card light-blue"
            onClick={() => navigate('/cargo_admin')}
          >
            <span className="card-icon">🚚</span>
            <span className="card-text">Manage Cargo</span>
          </div>

          <div
            className="admin-card light-yellow"
            onClick={() => navigate('/admin/orders')}
          >
            <span className="card-icon">📦</span>
            <span className="card-text">All Orders</span>
          </div>

          <div
            className="admin-card light-blue"
            onClick={() => navigate('/admin/applications')}
          >
            <span className="card-icon">👥</span>
            <span className="card-text">Manage Volunteers</span>
          </div>

          <div
            className="admin-card light-yellow"
            onClick={() => navigate('/admin/users')}
          >
            <span className="card-icon">🧊</span>
            <span className="card-text">Manage Users</span>
          </div>

          <div
            className="admin-card light-blue"
            onClick={() => navigate('/round_admin')}
          >
            <span className="card-icon">📆</span>
            <span className="card-text">Manage Round</span>
          </div>

          <div
            className="admin-card light-yellow"
            onClick={() => navigate('/admin/feedback')}
          >
            <span className="card-icon">💬</span>
            <span className="card-text">View Feedback</span>
          </div>
        </section>

        <div className="divider-vertical"></div>

        <section className="admin-right">
          <div className="alerts-card">
            <h2 className="alerts-title">Alerts</h2>
            <hr />
            <div className="alert-entry">
              <span>Items out of stock</span>
              <span className="card-badge red-badge">{outOfStockCount}</span>
            </div>
            <div className="alert-entry">
              <span>Items running low</span>
              <span className="card-badge yellow-badge">{lowStockCount}</span>
            </div>
            <div className="alert-entry">
              <span>New orders</span>
              <span className="card-badge blue-badge">{pendingOrdersCount}</span>
            </div>
            <div className="alert-entry">
              <span>New volunteer applications</span>
              <span className="card-badge blue-badge">{pendingAppsCount}</span>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default Admin;
