//=========================================== JS part ==============================================

import React, { useState, useEffect, useRef } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "../../css/Guest/Guest.css";

const Guest = ({ onLogout }) => {
  const baseURL = process.env.REACT_APP_BASE_URL;

  const navigate = useNavigate();

  // ========== cart status ==========
  const [showCart, setShowCart] = useState(false);
  const [cart, setCart] = useState([]); // [{ name, displayName, quantity }, ...]
  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [guestFirstName, setGuestFirstName] = useState("");
  const [guestLastName, setGuestLastName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [guestNotes, setGuestNotes] = useState("");

  const [cartError, setCartError] = useState("");
  const [cartMessage, setCartMessage] = useState("");

  // ========== current order box ==========
  const [showCurrentOrderModal, setShowCurrentOrderModal] = useState(false);

  const [currentOrder, setCurrentOrder] = useState(null);

  // ========== Logout ==========
  const handleLogout = () => {
    onLogout();
    navigate("/"); // back to login
  };


  // ========== cargo items ==========
  const [cargoItems, setCargoItems] = useState([]);
  const [selectedItem, setSelectedItem] = useState(null);
  const [showItemDetailModal, setShowItemDetailModal] = useState(false);
  const [selectedSize, setSelectedSize] = useState("");
  const [selectedQuantity, setSelectedQuantity] = useState(1);
  const itemsSectionRef = useRef(null);


  // States for custom item functionality
  const [showCustomItemModal, setShowCustomItemModal] = useState(false);
  const [customItemName, setCustomItemName] = useState("");
  const [customItemQuantity, setCustomItemQuantity] = useState(1);


  useEffect(() => {
    const fetchCargoItems = async () => {
      try {
        const response = await axios.get(`${baseURL}/api/cargo/items`);
        setCargoItems(response.data || []);
      } catch (error) {
        console.error("Failed to fetch cargo items:", error);
      }
    };

    fetchCargoItems();
  }, [baseURL]);


  // When user clicks "Make a New Order", fetch cargo items and show them
  const handleOpenNewOrder = () => {
    if (itemsSectionRef.current) {
      itemsSectionRef.current.scrollIntoView({ behavior: "smooth" });
    }
  };

  // Clicking an item -> show detail modal
  const handleSelectItem = (item) => {
    setSelectedItem(item);
    setShowItemDetailModal(true);
    const sizes = item.sizeQuantities ? Object.keys(item.sizeQuantities) : [];
    setSelectedSize(sizes.length > 0 ? sizes[0] : "");
    setSelectedQuantity(1);
  };

  // Close detail modal
  const closeItemDetailModal = () => {
    setSelectedItem(null);
    setShowItemDetailModal(false);
    setSelectedSize("");
    setSelectedQuantity(1);
  };

  // Add selected item to cart
  const handleAddSelectedItemToCart = () => {
    if (!selectedItem) return;
    if (selectedQuantity <= 0) {
      alert("Please enter a valid quantity.");
      return;
    }

    // Create display name with size for UI, but keep original name for backend
    const displayName = selectedSize
      ? `${selectedItem.name} (${selectedSize})`
      : selectedItem.name;

    const newCart = [...cart];
    const existingIndex = newCart.findIndex((c) => c.displayName === displayName);

    if (existingIndex >= 0) {
      // Item already exists, just update quantity
      newCart[existingIndex].quantity += selectedQuantity;
      // Make sure the name field is correct (in case of old cart items)
      newCart[existingIndex].name = selectedItem.name;
    } else {
      // Add new item to cart
      newCart.push({
        name: selectedItem.name,  // Original name for backend (e.g., "Coat")
        displayName: displayName,  // Display name with size for UI (e.g., "Coat (XL)")
        quantity: selectedQuantity,
        imageId: selectedItem.imageId,
        description: selectedItem.description,
        category: selectedItem.category,
        size: selectedSize  // Store size separately
      });
    }
    setCart(newCart);
    closeItemDetailModal();
  };

  // Open the custom item modal
  const handleOpenCustomItemModal = () => {
    setShowCustomItemModal(true);
  };

  // Add custom item to cart
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

    const itemName = customItemName.trim();
    const newCart = [...cart];
    const existingIndex = newCart.findIndex(
      (c) => c.name === itemName || c.displayName === itemName
    );

    if (existingIndex >= 0) {
      newCart[existingIndex].quantity += quantity;
    } else {
      // For custom items, name and displayName are the same
      // These won't be checked against inventory
      newCart.push({
        name: itemName,
        displayName: itemName,
        quantity: quantity,
        isCustom: true  // Flag to identify custom items
      });
    }
    setCart(newCart);
    setShowCustomItemModal(false);
    setCustomItemName("");
    setCustomItemQuantity(1);
  };

  // open/close cart
  const toggleCart = () => {
    setShowCart(!showCart);
    setCartError("");
    setCartMessage("");
  };

  // change quantities in cart
  const handleCartQuantityChange = (index, newQuantity) => {
    const updated = [...cart];
    updated[index].quantity = parseInt(newQuantity, 10) || 0;
    setCart(updated);
  };

  // remove item in cart
  const handleRemoveCartItem = (index) => {
    const updated = [...cart];
    updated.splice(index, 1);
    setCart(updated);
  };

  // ========== place order：POST /api/orders/create with geolocation ==========
  // 1) get geolocation
  const handlePlaceOrder = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          placeOrderWithLocation(
            position.coords.latitude,
            position.coords.longitude
          );
        },
        () => {
          placeOrderWithLocation(null, null);
        }
      );
    } else {
      placeOrderWithLocation(null, null);
    }
  };

  const placeOrderWithLocation = async (latitude, longitude) => {
    if (cart.length === 0) {
      setCartError("Your cart is empty.");
      return;
    }
    if (!deliveryAddress.trim()) {
      setCartError("Please fill in delivery address.");
      return;
    }
    if (
      !guestFirstName.trim() ||
      !guestLastName.trim() ||
      !phone.trim()
    ) {
      setCartError(
        "Please fill in first name, last name, and phone number."
      );
      return;
    }
    setCartError("");
    setCartMessage("");

     try {
    let combinedUserNotes = `FirstName: ${guestFirstName}; LastName: ${guestLastName}`;

    if (guestNotes.trim()) {
      combinedUserNotes += `; ${guestNotes}`;
    }

    if (email.trim()) {
      combinedUserNotes += `; Email: ${email}`;
    }

    // Include isCustom flag in payload
    const payload = {
      deliveryAddress,
      phoneNumber: phone,
      notes: combinedUserNotes,
      items: cart.map((c) => ({
        itemName: c.name,
        quantity: c.quantity,
        size: c.size || null,
        isCustom: c.isCustom || false
      })),
      authenticated: false,
      userId: -1
    };

    if (latitude !== null && longitude !== null) {
      payload.latitude = latitude;
      payload.longitude = longitude;
    }

    console.log("Sending order payload:", payload);

    const response = await axios.post(
      `${baseURL}/api/orders/create`,
      payload
    );

      if (response.data.status === "success") {
        setCartMessage("Order placed successfully!");
        // generate currentOrder
        const newOrder = {
          orderId: response.data.orderId,
          orderStatus: response.data.status || "PENDING",
          firstName: guestFirstName,
          lastName: guestLastName,
          address: deliveryAddress,
          notes: combinedUserNotes,
          items: cart, // items in cart for display
        };
        setCurrentOrder(newOrder);
        setShowCurrentOrderModal(true);
        // clean cart & form
        setCart([]);
        setDeliveryAddress("");
        setGuestFirstName("");
        setGuestLastName("");
        setEmail("");
        setPhone("");
        setGuestNotes("");
      } else {
        setCartError(response.data.message || "Order creation failed.");
      }
    } catch (error) {
      console.error("Order creation error:", error.response?.data);
      setCartError(error.response?.data?.message || "Order creation failed.");
    }
  };

  //=========================================== HTML part ==============================================

  return (
    <div className="page-container">
      {/* ---------- NAVBAR ---------- */}
      <header className="site-header">
        <div className="header-content">
          <div className="header-left">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="welcome-text">Welcome, Guest!</span>
          </div>

          <div className="header-right">
            <button className="logoutButton" onClick={handleLogout}>
              Log&nbsp;Out
            </button>
          </div>
        </div>
      </header>

      {/* ---------- MAIN ---------- */}
      <main className="user-dashboard">
        <h2 className="dashboard-greeting">Hello, Guest</h2>

        <div className="dashboard-cards">
          <button
            className="dashboard-card light-yellow"
            onClick={handleOpenNewOrder}
          >
            <span className="card-icon">&#128722;</span>
            Make a New Order
          </button>

          <button className="dashboard-card light-blue" onClick={toggleCart}>
            <span className="card-icon">&#128179;</span>
            View Cart&nbsp;({cart.length})
          </button>
        </div>

        <div ref={itemsSectionRef} style={{ marginTop: 30, width: "100%" }}>
          <div className="items-header">
            <h3 className="items-title">Available Items</h3>
            <span
              className="items-miss-link"
              onClick={handleOpenCustomItemModal}
            >
              Didn't find items you want? Click here.
            </span>
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
                  <p style={{ fontSize: 14, color: "#999" }}>{item.category}</p>
                  <p style={{ fontSize: 14 }}>Total Stock: {item.quantity}</p>
                </div>
              ))
            )}
          </div>
        </div>


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
              <div
                className="itemDetailImage"
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "#666",
                }}
              >
                No Image
              </div>
            )}

            <h3 className="detail-title">{selectedItem.name}</h3>
            <p className="detail-desc">{selectedItem.description}</p>
            <p className="detail-meta">
              Category:&nbsp;{selectedItem.category || "N/A"}
            </p>

            {selectedItem.sizeQuantities &&
              Object.keys(selectedItem.sizeQuantities).length > 0 && (
                <div className="detail-row">
                  <label>Size:</label>
                  <select
                    value={selectedSize}
                    onChange={(e) => setSelectedSize(e.target.value)}
                  >
                    {Object.entries(selectedItem.sizeQuantities).map(
                      ([s, q]) => (
                        <option key={s} value={s}>
                          {s} (stock:&nbsp;{q})
                        </option>
                      )
                    )}
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
              Add to Cart
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
            <p style={{ fontSize: 14, color: "#666", marginBottom: 15 }}>
              Note: Custom items may not be in our current inventory
            </p>

            <div className="formGroup">
              <label>Item Name:</label>
              <input
                type="text"
                value={customItemName}
                onChange={(e) => setCustomItemName(e.target.value)}
                className="input"
                placeholder="Enter item name"
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
              Add to Cart
            </button>
            <button
              className="cancelButton"
              onClick={() => setShowCustomItemModal(false)}
            >
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
                        alt={c.displayName || c.name}
                        className="cartItemImage"
                      />
                    ) : (
                      <div className="cartItemImagePlaceholder" />
                    )}
                    <div className="cartItemInfo">
                      <h4>{c.displayName || c.name}</h4>
                      {c.isCustom && (
                        <span style={{ fontSize: 12, color: "#ff9800" }}>
                          (Custom Item)
                        </span>
                      )}
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
                    {c.displayName || c.name}&nbsp;x{c.quantity}
                    {c.isCustom && " (Custom)"}
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
                <label>First Name:</label>
                <input
                  type="text"
                  value={guestFirstName}
                  onChange={(e) => setGuestFirstName(e.target.value)}
                  className="input"
                />
              </div>

              <div className="formGroup">
                <label>Last Name:</label>
                <input
                  type="text"
                  value={guestLastName}
                  onChange={(e) => setGuestLastName(e.target.value)}
                  className="input"
                />
              </div>

              <div className="formGroup">
                <label>Email (optional):</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="input"
                  placeholder="Optional"
                />
              </div>

              <div className="formGroup">
                <label>Phone:</label>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="input"
                />
              </div>

              <div className="formGroup">
                <label>Notes (optional):</label>
                <input
                  type="text"
                  value={guestNotes}
                  onChange={(e) => setGuestNotes(e.target.value)}
                  className="input"
                  placeholder="Optional"
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

      {showCurrentOrderModal && currentOrder && (
        <div className="modalOverlay">
          <div className="modalContent">
            <p
              style={{
                color: "red",
                fontSize: 18,
                fontStyle: "italic",
                marginBottom: 10,
              }}
            >
              NOTE: Please save this information!
            </p>

            <h3>Current Order</h3>
            <p>
              <strong>Order ID:</strong>&nbsp;{currentOrder.orderId}
            </p>
            <p>
              <strong>Status:</strong>&nbsp;{currentOrder.orderStatus}
            </p>
            <p>
              <strong>Guest Name:</strong>&nbsp;{currentOrder.firstName}&nbsp;
              {currentOrder.lastName}
            </p>
            <p>
              <strong>Address:</strong>&nbsp;{currentOrder.address}
            </p>
            <p>
              <strong>Notes:</strong>&nbsp;{currentOrder.notes}
            </p>
            <div style={{ margin: "10px 0" }}>
              <strong>Items:</strong>
              {currentOrder.items.map((it, idx) => (
                <div key={idx}>
                  {it.displayName || it.name}&nbsp;x&nbsp;{it.quantity}
                  {it.isCustom && " (Custom)"}
                </div>
              ))}
            </div>

            <button
              className="button"
              onClick={() => setShowCurrentOrderModal(false)}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Guest;