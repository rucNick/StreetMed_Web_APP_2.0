// Home_OrderHistory.js
import React, { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "../../css/Home/Home_OrderHistory.css";

const Home_OrderHistory = ({ userId }) => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState("");

  const baseURL = process.env.REACT_APP_BASE_URL;

  useEffect(() => {
    const fetchOrders = async () => {
      if (!userId || typeof userId !== "number") {
        setOrdersError("Order history is not available for guest users.");
        return;
      }
      try {
        setOrdersLoading(true);
        setOrdersError("");
        const response = await axios.get(
          `${baseURL}/api/orders/user/${userId}`,
          { params: { authenticated: true, userRole: "CLIENT", userId } }
        );
        if (response.data.status === "success") {
          const filtered = response.data.orders.filter((o) => o.status !== "CANCELLED");
          setOrders(filtered);
        } else {
          setOrdersError(response.data.message || "Failed to load orders.");
        }
      } catch (error) {
        setOrdersError(error.response?.data?.message || "Failed to load orders.");
      } finally {
        setOrdersLoading(false);
      }
    };

    fetchOrders();
  }, [userId, baseURL]);

  const handleCancelOrder = async (orderId) => {
    try {
      const payload = { authenticated: true, userId, userRole: "CLIENT" };
      const response = await axios.post(
        `${baseURL}/api/orders/${orderId}/cancel`,
        payload
      );
      if (response.data.status === "success") {
        // Refresh orders list by filtering out the cancelled order
        setOrders(orders.filter((order) => order.orderId !== orderId));
      } else {
        alert(response.data.message || "Failed to cancel order.");
      }
    } catch (error) {
      alert(error.response?.data?.message || "Failed to cancel order.");
    }
  };

  return (
    <div className="orderHistory-container">
      <div className="orderHistory-header">
        <h2>Order History</h2>
        <button className="backButton" onClick={() => navigate("/")}>
          Back to Home
        </button>
      </div>
      {ordersLoading && <p>Loading orders...</p>}
      {ordersError && <p className="errorText">{ordersError}</p>}
      <div className="ordersList">
        {orders.length === 0 ? (
          <p>No orders found.</p>
        ) : (
          orders.map((order, idx) => (
            <div key={idx} className="orderItem">
              <p>
                <strong>Order ID:</strong> {order.orderId}
              </p>
              <p>
                <strong>Address:</strong> {order.deliveryAddress}
              </p>
              <p>
                <strong>Notes:</strong> {order.notes}
              </p>
              <p>
                <strong>Request Time:</strong> {order.requestTime}
              </p>
              {order.orderItems && order.orderItems.length > 0 ? (
                <>
                  <p>
                    <strong>Total Items:</strong> {order.orderItems.length}
                  </p>
                  <ul className="orderItemsList">
                    {order.orderItems.map((item, iidx) => (
                      <li key={iidx}>
                        {item.itemName} x {item.quantity}
                      </li>
                    ))}
                  </ul>
                </>
              ) : (
                <p>
                  <em>No items found for this order.</em>
                </p>
              )}
              <button className="cancelOrderButton" onClick={() => handleCancelOrder(order.orderId)}>
                Cancel Order
              </button>
              <hr />
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default Home_OrderHistory;
