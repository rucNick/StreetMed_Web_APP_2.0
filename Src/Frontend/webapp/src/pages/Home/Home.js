//=========================================== JS part ==============================================
import React, { useState } from "react";
import axios from "axios";
import "../../css/Home/Home.css";

//import { encrypt, decrypt, getSessionId, isInitialized } from "./security/ecdhClient";

import { useNavigate } from "react-router-dom"; // for pages jump

//const Home = ({ username, email, password, phone, userId, onLogout }) => {
const Home = ({ username, email, phone, userId, onLogout }) => {

  const baseURL = process.env.REACT_APP_BASE_URL;

  console.log("Home component initialized", { username, email, phone, userId });

  // ============== Cart States ==============
  const [showCart, setShowCart] = useState(false);
  const [cart, setCart] = useState([]);
  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [notes, setNotes] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [cartError, setCartError] = useState("");
  const [cartMessage, setCartMessage] = useState("");
  console.log("Cart States initialized");

  // ============== "Make a New Order" - Cargo Items ==============
  const [showNewOrder, setShowNewOrder] = useState(false);
  const [cargoItems, setCargoItems] = useState([]);
  const [selectedItem, setSelectedItem] = useState(null);
  const [showItemDetailModal, setShowItemDetailModal] = useState(false);
  const [selectedSize, setSelectedSize] = useState("");
  const [selectedQuantity, setSelectedQuantity] = useState(1);
  console.log("New Order and Cargo Items States initialized");

  // ============== Customize Item Related State ==============
  const [showCustomItemModal, setShowCustomItemModal] = useState(false);
  const [customItemName, setCustomItemName] = useState("");
  const [customItemQuantity, setCustomItemQuantity] = useState(1);

  // Initialize navigate hook for page navigation (Profile, Feedback, Order History)
  const navigate = useNavigate();

  // ================= Other functions (Cart, New Order, etc.) =================

  const fetchCargoItems = async () => {
    console.log("fetchCargoItems: Fetching cargo items");
    try {
      const response = await axios.get(`${baseURL}/api/cargo/items`);
      console.log("fetchCargoItems: Received response", response);
      setCargoItems(response.data);
    } catch (error) {
      console.error("Failed to fetch cargo items:", error);
    }
  };

  const handleOpenNewOrder = () => {
    console.log("handleOpenNewOrder: Opening new order");
    setShowNewOrder(true);
    fetchCargoItems();
  };

  const handleSelectItem = (item) => {
    console.log("handleSelectItem: Selected item", item);
    setSelectedItem(item);
    setShowItemDetailModal(true);
    const sizes = item.sizeQuantities ? Object.keys(item.sizeQuantities) : [];
    setSelectedSize(sizes.length > 0 ? sizes[0] : "");
    setSelectedQuantity(1);
    console.log("handleSelectItem: Set selectedSize and selectedQuantity");
  };

  const closeItemDetailModal = () => {
    console.log("closeItemDetailModal: Closing item detail modal");
    setShowItemDetailModal(false);
    setSelectedItem(null);
    setSelectedSize("");
    setSelectedQuantity(1);
  };

  const handleAddSelectedItemToCart = () => {
    console.log("handleAddSelectedItemToCart: called");
    if (!selectedItem) return;
    if (selectedQuantity <= 0) {
      alert("Please enter a valid quantity.");
      return;
    }
    const itemName = selectedSize
      ? `${selectedItem.name} (${selectedSize})`
      : selectedItem.name;
    const newCart = [...cart];
    const existingIndex = newCart.findIndex((c) => c.name === itemName);
    if (existingIndex >= 0) {
      newCart[existingIndex].quantity += selectedQuantity;
      console.log("handleAddSelectedItemToCart: Updated quantity for existing cart item");
    } else {
      newCart.push({ name: itemName, 
                      quantity: selectedQuantity,
                      imageId: selectedItem.imageId, 
                      description: selectedItem.description,
                      category: selectedItem.category});
      console.log("handleAddSelectedItemToCart: Added new item to cart");
    }
    setCart(newCart);
    closeItemDetailModal();
  };

  // === Opens the custom item popup window ===
  const handleOpenCustomItemModal = () => {
    setShowCustomItemModal(true);
  };

  // === Add custom items to cart ===
  const handleAddCustomItemToCart = () => {
    if (!customItemName.trim()) {
      alert("Please enter an item name.");
      return;
    }
    const quantity = parseInt(customItemQuantity, 10);
    if (isNaN(quantity) || quantity <= 0) {
      alert("Please enter a valid quantity (positive integer).");
      return;
    }
    const newCart = [...cart];
    const existingIndex = newCart.findIndex((c) => c.name === customItemName.trim());
    if (existingIndex >= 0) {
      newCart[existingIndex].quantity += quantity;
    } else {
      newCart.push({ name: customItemName.trim(), quantity });
    }
    setCart(newCart);
    setShowCustomItemModal(false);
    setCustomItemName("");
    setCustomItemQuantity(1);
  };

  const toggleCart = () => {
    console.log("toggleCart: toggling cart visibility");
    setShowCart(!showCart);
    setCartError("");
    setCartMessage("");
  };

  const handleCartQuantityChange = (index, newQuantity) => {
    console.log("handleCartQuantityChange: index", index, "newQuantity", newQuantity);
    const updated = [...cart];
    updated[index].quantity = parseInt(newQuantity, 10) || 0;
    setCart(updated);
  };

  const handleRemoveCartItem = (index) => {
    console.log("handleRemoveCartItem: Removing cart item at index", index);
    const updated = [...cart];
    updated.splice(index, 1);
    setCart(updated);
  };

  const handlePlaceOrder = () => {
    console.log("handlePlaceOrder: called");
    if (navigator.geolocation) {
      console.log("handlePlaceOrder: Geolocation supported, fetching location");
      navigator.geolocation.getCurrentPosition(
        (position) => {
          console.log("handlePlaceOrder: Received geolocation", position.coords);
          placeOrderWithLocation(position.coords.latitude, position.coords.longitude);
        },
        () => {
          console.log("handlePlaceOrder: Geolocation error, proceeding without location");
          placeOrderWithLocation(null, null);
        }
      );
    } else {
      console.log("handlePlaceOrder: Geolocation not supported, proceeding without location");
      placeOrderWithLocation(null, null);
    }
  };

  const placeOrderWithLocation = async (latitude, longitude) => {
    console.log("placeOrderWithLocation: called with", { latitude, longitude });
    if (cart.length === 0) {
      console.log("placeOrderWithLocation: Cart is empty");
      setCartError("Your cart is empty.");
      return;
    }
    if (!deliveryAddress.trim() || !notes.trim() || !phoneNumber.trim()) {
      console.log("placeOrderWithLocation: Missing delivery address, notes or phone number");
      setCartError("Please fill in delivery address, phone number, and notes.");
      return;
    }
    setCartError("");
    setCartMessage("");
    try {
      const payload = {
        authenticated: true,
        userId,
        deliveryAddress,
        notes,
        phoneNumber,
        items: cart.map((item) => ({ itemName: item.name, quantity: item.quantity })),
      };
      if (latitude !== null && longitude !== null) {
        payload.latitude = latitude;
        payload.longitude = longitude;
      }
      console.log("placeOrderWithLocation: Sending order payload", payload);
      const response = await axios.post(`${baseURL}/api/orders/create`, payload);
      console.log("placeOrderWithLocation: Received response", response);
      if (response.data.status !== "success") {
        setCartError(response.data.message || "Order creation failed");
        return;
      }
      setCartMessage("Order placed successfully!");
      setCart([]);
      setDeliveryAddress("");
      setNotes("");
      setPhoneNumber("");
    } catch (error) {
      console.error("placeOrderWithLocation: Error occurred", error);
      setCartError(error.response?.data?.message || "Order creation failed.");
    }
  };

  // Modified: Remove orders history logic from Home.
  // Instead, when "View Orders History" is clicked, navigate to the separate Order History page.
  const handleOrderHistoryNavigation = () => {
    navigate("/orderhistory");
  };





  // ================================== Rendering Section =================
  console.log("Home: Rendering component");
  return (
    <div className="page-container">
      {/* ---------------- NAVBAR ---------------- */}
      <header className="site-header">
        <div className="header-content">
          <div className="header-left">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="welcome-text">Hello, {username} !</span>
            <button
              className="profileButton"
              onClick={() => navigate("/profile")}
            >
              Profile
            </button>
          </div>

          <div className="header-right">
            <button className="cartButton" onClick={toggleCart}>
              Cart
            </button>
            <button
              className="feedbackButton"
              onClick={() => navigate("/feedback")}
            >
              Feedback
            </button>
            <button className="logoutButton" onClick={onLogout}>
              Log&nbsp;Out
            </button>
          </div>
        </div>
      </header>

      <main className="user-dashboard">
        <h2 className="dashboard-greeting">Hello, {username}</h2>

        <div className="dashboard-cards">
          <button
            className="dashboard-card light-blue"
            onClick={handleOrderHistoryNavigation}
          >
            <span className="card-icon">&#9660;</span>
            View Orders
          </button>

          <button
            className="dashboard-card light-yellow"
            onClick={handleOpenNewOrder}
          >
            <span className="card-icon">&#128722;</span>
            Make a New Order
          </button>
        </div>

        {showNewOrder && (
          <div style={{ marginTop: 30, width: "100%" }}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
              }}
            >
              <div className="items-header">
                <h3 className="items-title">Available Items</h3>
                <span
                  className="items-miss-link"
                  onClick={handleOpenCustomItemModal}
                >
                  Didn't find items you want? Click here.
                </span>
              </div>
            </div>

            <div className="itemGrid" style={{ marginTop: 12 }}>
              {cargoItems.length === 0 ? (
                <p>No items found in cargo.</p>
              ) : (
                cargoItems.map((item) => (
                  <div
                    key={item.id}
                    className="itemCard"
                    onClick={() => handleSelectItem(item)}
                  >
                    {item.imageId ? (
                      <img
                        src={`${baseURL}/api/cargo/images/${item.imageId}`}
                        alt={item.name}
                        className="itemImage"
                      />
                    ) : (
                      <div className="itemImagePlaceholder">No Image</div>
                    )}
                    <h4>{item.name}</h4>
                    <p style={{ fontSize: 14, color: "#999" }}>
                      {item.category}
                    </p>
                    <p style={{ fontSize: 14 }}>
                      Total Stock: {item.quantity}
                    </p>
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </main>

      {showItemDetailModal && selectedItem && (
        <div className="modalOverlay">
          <div className="itemDetailContent">
            {selectedItem.imageId ? (
              <img
                src={`${baseURL}/api/cargo/images/${selectedItem.imageId}`}
                alt={selectedItem.name}
                className="itemDetailImage"
              />
            ) : (
              <div className="itemDetailImage" style={{ display:'flex',alignItems:'center',justifyContent:'center',color:'#666' }}>
                No Image
              </div>
            )}

            <h3 className="detail-title">{selectedItem.name}</h3>
            <p className="detail-desc">{selectedItem.description}</p>
            <p className="detail-meta">
              Category: {selectedItem.category || "N/A"}
            </p>

            {selectedItem.sizeQuantities &&
              Object.keys(selectedItem.sizeQuantities).length > 0 && (
                <div className="detail-row">
                  <label>Size:</label>
                  <select
                    value={selectedSize}
                    onChange={(e) => setSelectedSize(e.target.value)}
                  >
                    {Object.entries(selectedItem.sizeQuantities).map(([s, q]) => (
                      <option key={s} value={s}>
                        {s} (stock: {q})
                      </option>
                    ))}
                  </select>
                </div>
              )}

            <div className="detail-row">
              <label>Quantity:</label>
              <input
                type="number"
                min="1"
                value={selectedQuantity}
                onChange={(e) => setSelectedQuantity(Number(e.target.value))}
                style={{ width: 80 }}
              />
            </div>

            <button className="add-btn" onClick={handleAddSelectedItemToCart}>
              Add to Cart
            </button>
            <button className="cancel-btn" onClick={closeItemDetailModal}>
              Cancel
            </button>
          </div>
        </div>
      )}

      {showCustomItemModal && (
        <div className="modalOverlay">
          <div className="modalContent">
            <h3>Add a custom item</h3>

            <div className="formGroup">
              <label>Item Name:</label>
              <input
                type="text"
                value={customItemName}
                onChange={(e) => setCustomItemName(e.target.value)}
                className="input"
              />
            </div>

            <div className="formGroup">
              <label>Quantity:</label>
              <input
                type="number"
                min="1"
                value={customItemQuantity}
                onChange={(e) => setCustomItemQuantity(e.target.value)}
                className="input"
              />
            </div>

            <button className="button" onClick={handleAddCustomItemToCart}>
              Add to Cart
            </button>
            <button className="cancelButton" onClick={() => setShowCustomItemModal(false)}>
              Cancel
            </button>
          </div>
        </div>
      )}

      {showCart && (
        <div className="modalOverlay">
          <div className="cartContent">
            <button className="cartClose" onClick={toggleCart}>
              ×
            </button>
            <div className="cartLeft">
              <h3>Cart</h3>
              {cart.length === 0 ? (
                <p>No items in cart.</p>
              ) : (
                cart.map((c, i) => (
                  <div key={i} className="cartItem">
                    {c.imageId ? (
                      <img
                        src={`${baseURL}/api/cargo/images/${c.imageId}`}
                        alt={c.name}
                        className="cartItemImage"
                      />
                    ) : (
                      <div className="cartItemImagePlaceholder" />
                    )}
                    <div className="cartItemInfo">
                      <h4>{c.name}</h4>
                      {c.description && <p>{c.description}</p>}
                      {c.category && (
                        <p style={{ fontSize: 12, color: "#999" }}>
                          {c.category}
                        </p>
                      )}
                    </div>
                    <div className="amountSection">
                      <span>Amount</span>
                      <input
                        type="number"
                        min="0"
                        value={c.quantity}
                        onChange={(e) =>
                          handleCartQuantityChange(i, e.target.value)
                        }
                      />
                    </div>
                    <button
                      className="removeButton"
                      onClick={() => handleRemoveCartItem(i)}
                    >
                      Remove
                    </button>
                  </div>
                ))
              )}
            </div>

            <div className="cartRight">
              <h3>Overview</h3>
              <ul className="overviewList">
                {cart.map((c, idx) => (
                  <li key={idx}>
                    {c.name} x{c.quantity}
                  </li>
                ))}
              </ul>

              <div className="formGroup">
                <label>Delivery Address:</label>
                <input
                  type="text"
                  value={deliveryAddress}
                  onChange={(e) => setDeliveryAddress(e.target.value)}
                  className="input"
                />
              </div>

              <div className="formGroup">
                <label>Phone Number:</label>
                <input
                  type="text"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="input"
                />
              </div>

              <div className="formGroup">
                <label>Notes:</label>
                <input
                  type="text"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="input"
                />
              </div>

              {cartError && <p className="errorText">{cartError}</p>}
              {cartMessage && <p className="successText">{cartMessage}</p>}

              <button className="placeOrderButton" onClick={handlePlaceOrder}>
                Place Order
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Home;