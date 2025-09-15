import React, { useState, useEffect, useCallback } from 'react';
import { secureAxios } from '../../config/axiosConfig';
import { useNavigate } from 'react-router-dom';
import '../../css/Admin/Cargo_Admin.css';

const Cargo_Admin = ({ userData }) => {
  const navigate = useNavigate();

  // === Inventory Data ===
  const [allItems, setAllItems] = useState([]);
  const [allItemsError, setAllItemsError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const fetchAllItems = useCallback(async () => {
    try {
      setIsLoading(true);
      setAllItemsError('');
      
      // Use secureAxios for admin operations (HTTPS required)
      const res = await secureAxios.get('/api/cargo/items', {
        headers: {
          'Admin-Username': userData.username,
          'Authentication-Status': 'true'
        }
      });
      
      setAllItems(res.data || []);
    } catch (err) {
      console.error("Error fetching items:", err);
      if (err.response?.data?.httpsRequired) {
        setAllItemsError("Secure HTTPS connection required for admin operations.");
      } else {
        setAllItemsError(err.response?.data?.message || err.message);
      }
    } finally {
      setIsLoading(false);
    }
  }, [userData.username]);

  useEffect(() => {
    fetchAllItems();
  }, [fetchAllItems]);

  // === Add New Item State ===
  const [newItemData, setNewItemData] = useState({
    name: '', description: '', category: '', quantity: 0
  });
  const [newSizeEntries, setNewSizeEntries] = useState([]);
  const [newItemImage, setNewItemImage] = useState(null);

  // === Update Item State ===
  const [updateItemId, setUpdateItemId] = useState('');
  const [updateItemData, setUpdateItemData] = useState({
    name: '', description: '', category: '', quantity: 0
  });
  const [updateItemImage, setUpdateItemImage] = useState(null);

  // === Drawer toggles ===
  const [showAdd, setShowAdd] = useState(false);
  const [showUpdate, setShowUpdate] = useState(false);

  // === Helpers ===
  const handleAddSizeEntry = () =>
    setNewSizeEntries([...newSizeEntries, { size: '', quantity: 0 }]);

  const handleSizeEntryChange = (idx, field, val) => {
    const tmp = [...newSizeEntries];
    tmp[idx] = { ...tmp[idx], [field]: field === 'quantity' ? +val : val };
    setNewSizeEntries(tmp);
  };

  const handleRemoveSizeEntry = idx =>
    setNewSizeEntries(newSizeEntries.filter((_, i) => i !== idx));

  const renderStatus = qty => {
    if (qty === 0) return <span className="status out">Out</span>;
    if (qty < 5) return <span className="status low">Low</span>;
    return <span className="status fine">Fine</span>;
  };

  const handleAddNewItem = async () => {
    try {
      const sizeQuantities = {};
      newSizeEntries.forEach(e => {
        if (e.size) {
          sizeQuantities[e.size] = e.quantity;
        }
      });
      
      let finalQuantity = newItemData.quantity;
      if (Object.keys(sizeQuantities).length > 0) {
        finalQuantity = Object.values(sizeQuantities).reduce((a, b) => a + b, 0);
      }
      
      const dataToSend = { 
        ...newItemData, 
        quantity: finalQuantity, 
        sizeQuantities 
      };
      
      const fd = new FormData();
      fd.append('data', new Blob([JSON.stringify(dataToSend)], { type: 'application/json' }));
      if (newItemImage) {
        fd.append('image', newItemImage);
      }
      
      // Use secureAxios for admin operations
      const resp = await secureAxios.post('/api/cargo/items', fd, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'Admin-Username': userData.username,
          'Authentication-Status': 'true'
        }
      });
      
      alert(resp.data.message || 'Item added successfully');
      setNewItemData({ name: '', description: '', category: '', quantity: 0 });
      setNewSizeEntries([]);
      setNewItemImage(null);
      setShowAdd(false);
      fetchAllItems();
    } catch (err) {
      console.error("Error adding item:", err);
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleUpdateItem = async () => {
    if (!updateItemId) {
      alert('Enter Item ID to update');
      return;
    }
    
    try {
      // Update item data
      const resp1 = await secureAxios.put(
        `/api/cargo/items/${updateItemId}`,
        updateItemData,
        { 
          headers: {
            'Admin-Username': userData.username,
            'Authentication-Status': 'true'
          }
        }
      );
      
      // Update image if provided
      if (updateItemImage) {
        const fd = new FormData();
        fd.append('image', updateItemImage);
        await secureAxios.put(
          `/api/cargo/items/${updateItemId}?image`,
          fd,
          { 
            headers: {
              'Content-Type': 'multipart/form-data',
              'Admin-Username': userData.username,
              'Authentication-Status': 'true'
            }
          }
        );
      }
      
      alert(resp1.data.message || 'Item updated successfully');
      setUpdateItemId('');
      setUpdateItemData({ name: '', description: '', category: '', quantity: 0 });
      setUpdateItemImage(null);
      setShowUpdate(false);
      fetchAllItems();
    } catch (err) {
      console.error("Error updating item:", err);
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleDeleteItem = async (itemId) => {
    if (!window.confirm(`Are you sure you want to delete item with ID ${itemId}?`)) {
      return;
    }
    
    try {
      const resp = await secureAxios.delete(`/api/cargo/items/${itemId}`, {
        headers: {
          'Admin-Username': userData.username,
          'Authentication-Status': 'true'
        }
      });
      
      alert(resp.data.message || 'Item deleted successfully');
      fetchAllItems();
    } catch (err) {
      console.error("Error deleting item:", err);
      alert(err.response?.data?.message || err.message);
    }
  };

  return (
    <div className="page-container">
      {/* Header */}
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">Cargo Management System</span>
          </div>
          <div className="header-right">
            <button className="manage-btn" onClick={() => navigate(-1)}>
              Go Back
            </button>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="cargo-container">
          <div className="cargo-header">
            <h2 className="cargo-title">Cargo Status</h2>
            <button 
              className="manage-btn" 
              onClick={fetchAllItems}
              disabled={isLoading}
            >
              {isLoading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>

          {/* Error Message */}
          {allItemsError && (
            <div style={{ 
              padding: '10px', 
              margin: '10px 0', 
              backgroundColor: '#ffebee', 
              color: '#c62828', 
              borderRadius: '4px' 
            }}>
              Error: {allItemsError}
            </div>
          )}

          <div className="cargo-card">
            <div className="table-title">Inventory Overview</div>
            <div className="table-scroll">
              {isLoading ? (
                <div style={{ padding: '20px', textAlign: 'center' }}>
                  Loading inventory...
                </div>
              ) : (
                <table className="cargo-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Name</th>
                      <th>Description</th>
                      <th>Category</th>
                      <th>Total-Quantity</th>
                      <th>Size</th>
                      <th>Size-Quantity</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {allItems.map(item => {
                      const sizes = item.sizeQuantities || {};
                      const hasSizes = Object.keys(sizes).length > 0;
                      return (
                        <React.Fragment key={item.id}>
                          <tr>
                            <td>{item.id}</td>
                            <td>{item.name}</td>
                            <td>{item.description}</td>
                            <td>{item.category}</td>
                            <td>{item.quantity}</td>
                            <td></td>
                            <td></td>
                            <td>{renderStatus(item.quantity)}</td>
                            <td>
                              <button
                                className="manage-btn"
                                onClick={() => handleDeleteItem(item.id)}
                                style={{ 
                                  backgroundColor: '#f44336', 
                                  color: 'white',
                                  fontSize: '12px',
                                  padding: '4px 8px'
                                }}
                              >
                                Delete
                              </button>
                            </td>
                          </tr>
                          {hasSizes && Object.entries(sizes).map(([sz, qty]) => (
                            <tr key={`${item.id}-${sz}`}>
                              <td></td>
                              <td></td>
                              <td></td>
                              <td></td>
                              <td></td>
                              <td>{sz}</td>
                              <td>{qty}</td>
                              <td>{renderStatus(qty)}</td>
                              <td></td>
                            </tr>
                          ))}
                        </React.Fragment>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </div>

          {/* Drawer buttons */}
          <div className="drawer-container">
            <div className="drawer-section">
              <button
                className="drawer-toggle"
                onClick={() => setShowAdd(!showAdd)}
              >
                {showAdd ? 'Hide Add Form' : 'Add New Item'}
              </button>
            </div>
            <div className="drawer-section">
              <button
                className="drawer-toggle"
                onClick={() => setShowUpdate(!showUpdate)}
              >
                {showUpdate ? 'Hide Update Form' : 'Update Item'}
              </button>
            </div>
          </div>

          {/* Add Form Drawer */}
          <div className={`drawer-panel ${showAdd ? 'open' : ''}`}>
            <div className="content-block blue-block">
              <div className="block-content">
                <h3 className="block-title">Add New Item</h3>
                <input
                  className="cargo-input"
                  placeholder="Name"
                  value={newItemData.name}
                  onChange={e => setNewItemData({ ...newItemData, name: e.target.value })}
                />
                <input
                  className="cargo-input"
                  placeholder="Description"
                  value={newItemData.description}
                  onChange={e => setNewItemData({ ...newItemData, description: e.target.value })}
                />
                <input
                  className="cargo-input"
                  placeholder="Category"
                  value={newItemData.category}
                  onChange={e => setNewItemData({ ...newItemData, category: e.target.value })}
                />
                <input
                  className="cargo-input"
                  type="number"
                  placeholder="Quantity"
                  value={newItemData.quantity}
                  onChange={e => setNewItemData({ ...newItemData, quantity: +e.target.value })}
                />

                <div className="cargo-sizes">
                  {newSizeEntries.map((ent, i) => (
                    <div key={i} className="cargo-size-entry">
                      <input
                        className="cargo-input"
                        placeholder="Size"
                        value={ent.size}
                        onChange={e => handleSizeEntryChange(i, 'size', e.target.value)}
                      />
                      <input
                        className="cargo-input"
                        type="number"
                        placeholder="Qty"
                        value={ent.quantity}
                        onChange={e => handleSizeEntryChange(i, 'quantity', e.target.value)}
                      />
                      <button 
                        className="cargo-small-btn" 
                        onClick={() => handleRemoveSizeEntry(i)}
                      >
                        ×
                      </button>
                    </div>
                  ))}
                  <button className="cargo-button" onClick={handleAddSizeEntry}>
                    + Add Size Option
                  </button>
                </div>

                <div style={{ margin: '12px 0' }}>
                  <input 
                    type="file" 
                    accept="image/*"
                    onChange={e => setNewItemImage(e.target.files[0])} 
                  />
                </div>

                <button className="cargo-button" onClick={handleAddNewItem}>
                  Add Item
                </button>
              </div>
            </div>
          </div>

          {/* Update Form Drawer */}
          <div className={`drawer-panel ${showUpdate ? 'open' : ''}`}>
            <div className="content-block beige-block">
              <div className="block-content">
                <h3 className="block-title">Update Item</h3>
                <input
                  className="cargo-input"
                  placeholder="Item ID"
                  value={updateItemId}
                  onChange={e => setUpdateItemId(e.target.value)}
                />
                <input
                  className="cargo-input"
                  placeholder="Name"
                  value={updateItemData.name}
                  onChange={e => setUpdateItemData({ ...updateItemData, name: e.target.value })}
                />
                <input
                  className="cargo-input"
                  placeholder="Description"
                  value={updateItemData.description}
                  onChange={e => setUpdateItemData({ ...updateItemData, description: e.target.value })}
                />
                <input
                  className="cargo-input"
                  placeholder="Category"
                  value={updateItemData.category}
                  onChange={e => setUpdateItemData({ ...updateItemData, category: e.target.value })}
                />
                <input
                  className="cargo-input"
                  type="number"
                  placeholder="Quantity"
                  value={updateItemData.quantity}
                  onChange={e => setUpdateItemData({ ...updateItemData, quantity: +e.target.value })}
                />

                <div style={{ margin: '12px 0' }}>
                  <input 
                    type="file" 
                    accept="image/*"
                    onChange={e => setUpdateItemImage(e.target.files[0])} 
                  />
                </div>

                <button className="cargo-button" onClick={handleUpdateItem}>
                  Update Item
                </button>
              </div>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
};

export default Cargo_Admin;