import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import '../../css/Admin/Cargo_Admin.css';

const Cargo_Admin = ({ userData }) => {
  const baseURL = process.env.REACT_APP_BASE_URL;
  const navigate = useNavigate();

  // === Inventory Data ===
  const [allItems, setAllItems] = useState([]);
  const [allItemsError, setAllItemsError] = useState('');

  const fetchAllItems = useCallback(async () => {
    try {
      const res = await axios.get(`${baseURL}/api/cargo/items`, {
        headers: {
          'Admin-Username': userData.username,
          'Authentication-Status': 'true'
        }
      });
      setAllItems(res.data);
      setAllItemsError('');
    } catch (err) {
      setAllItemsError(err.response?.data?.message || err.message);
    }
  }, [baseURL, userData.username]);

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
    tmp[idx] = { ...tmp[idx], [field]: field==='quantity'? +val : val };
    setNewSizeEntries(tmp);
  };

  const handleRemoveSizeEntry = idx =>
    setNewSizeEntries(newSizeEntries.filter((_,i) => i!==idx));

  const renderStatus = qty => {
    if (qty===0) return <span className="status out">Out</span>;
    if (qty<5)  return <span className="status low">Low</span>;
    return <span className="status fine">Fine</span>;
  };

  const handleAddNewItem = async () => {
    try {
      const sizeQuantities = {};
      newSizeEntries.forEach(e => e.size && (sizeQuantities[e.size]=e.quantity));
      let finalQuantity = newItemData.quantity;
      if (Object.keys(sizeQuantities).length>0) {
        finalQuantity = Object.values(sizeQuantities).reduce((a,b)=>a+b,0);
      }
      const dataToSend = { ...newItemData, quantity: finalQuantity, sizeQuantities };
      const fd = new FormData();
      fd.append('data', new Blob([JSON.stringify(dataToSend)], { type:'application/json' }));
      newItemImage && fd.append('image', newItemImage);
      const resp = await axios.post(`${baseURL}/api/cargo/items`, fd, {
        headers: {
          'Content-Type':'multipart/form-data',
          'Admin-Username': userData.username,
          'Authentication-Status':'true'
        }
      });
      alert(resp.data.message||'Item added');
      setNewItemData({ name:'',description:'',category:'',quantity:0 });
      setNewSizeEntries([]);
      setNewItemImage(null);
      fetchAllItems();
    } catch (err) {
      alert(err.response?.data?.message||err.message);
    }
  };

  const handleUpdateItem = async () => {
    if (!updateItemId) {
      alert('Enter Item ID to update');
      return;
    }
    try {
      const resp1 = await axios.put(
        `${baseURL}/api/cargo/items/${updateItemId}`,
        updateItemData,
        { headers:{
            'Admin-Username': userData.username,
            'Authentication-Status':'true'
          }
        }
      );
      if (updateItemImage) {
        const fd = new FormData();
        fd.append('image', updateItemImage);
        await axios.put(
          `${baseURL}/api/cargo/items/${updateItemId}?image`,
          fd,
          { headers:{
              'Content-Type':'multipart/form-data',
              'Admin-Username': userData.username,
              'Authentication-Status':'true'
            }
          }
        );
      }
      alert(resp1.data.message||'Item updated');
      fetchAllItems();
    } catch (err) {
      alert(err.response?.data?.message||err.message);
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
            <button className="manage-btn" onClick={()=>navigate(-1)}>
              Go Back
            </button>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="cargo-container">
          <div className="cargo-header">
            <h2 className="cargo-title">Cargo Status</h2>
            <button className="manage-btn" onClick={fetchAllItems}>
              Refresh
            </button>
          </div>

          <div className="cargo-card">
            {allItemsError && <p className="cargo-error">{allItemsError}</p>}

            <div className="table-title">Inventory Overview</div>
            <div className="table-scroll">
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
                  </tr>
                </thead>
                <tbody>
                  {allItems.map(item => {
                    const sizes = item.sizeQuantities||{};
                    const hasSizes = Object.keys(sizes).length>0;
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
                        </tr>
                        {hasSizes && Object.entries(sizes).map(([sz,qty])=>(
                          <tr key={`${item.id}-${sz}`}>
                            <td></td><td></td><td></td><td></td><td></td>
                            <td>{sz}</td>
                            <td>{qty}</td>
                            <td>{renderStatus(qty)}</td>
                          </tr>
                        ))}
                      </React.Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* drawer buttons */}
          <div className="drawer-container">
            <div className="drawer-section">
              <button
                className="drawer-toggle"
                onClick={()=>setShowAdd(!showAdd)}
              >
                {showAdd? 'Hide Add Form' : 'Add New Item'}
              </button>
            </div>
            <div className="drawer-section">
              <button
                className="drawer-toggle"
                onClick={()=>setShowUpdate(!showUpdate)}
              >
                {showUpdate? 'Hide Update Form' : 'Update Item'}
              </button>
            </div>
          </div>

          {/* Add Form Drawer */}
          <div className={`drawer-panel ${showAdd? 'open':''}`}>
            <div className="content-block blue-block">
              <div className="block-content">
                <h3 className="block-title">Add New Item</h3>
                <input
                  className="cargo-input"
                  placeholder="Name"
                  value={newItemData.name}
                  onChange={e=>setNewItemData({ ...newItemData, name:e.target.value })}
                />
                <input
                  className="cargo-input"
                  placeholder="Description"
                  value={newItemData.description}
                  onChange={e=>setNewItemData({ ...newItemData, description:e.target.value })}
                />
                <input
                  className="cargo-input"
                  placeholder="Category"
                  value={newItemData.category}
                  onChange={e=>setNewItemData({ ...newItemData, category:e.target.value })}
                />
                <input
                  className="cargo-input"
                  type="number"
                  placeholder="Quantity"
                  value={newItemData.quantity}
                  onChange={e=>setNewItemData({ ...newItemData, quantity:+e.target.value })}
                />

                <div className="cargo-sizes">
                  {newSizeEntries.map((ent,i)=>(
                    <div key={i} className="cargo-size-entry">
                      <input
                        className="cargo-input"
                        placeholder="Size"
                        value={ent.size}
                        onChange={e=>handleSizeEntryChange(i,'size',e.target.value)}
                      />
                      <input
                        className="cargo-input"
                        type="number"
                        placeholder="Qty"
                        value={ent.quantity}
                        onChange={e=>handleSizeEntryChange(i,'quantity',e.target.value)}
                      />
                      <button className="cargo-small-btn" onClick={()=>handleRemoveSizeEntry(i)}>×</button>
                    </div>
                  ))}
                  <button className="cargo-button" onClick={handleAddSizeEntry}>
                    + Add Size Option
                  </button>
                </div>

                <div style={{ margin:'12px 0' }}>
                  <input type="file" onChange={e=>setNewItemImage(e.target.files[0])} />
                </div>

                <button className="cargo-button" onClick={handleAddNewItem}>
                  Add Item
                </button>
              </div>
            </div>
          </div>

          {/* Update Form Drawer */}
          <div className={`drawer-panel ${showUpdate? 'open':''}`}>
            <div className="content-block beige-block">
              <div className="block-content">
                <h3 className="block-title">Update Item</h3>
                <input
                  className="cargo-input"
                  placeholder="Item ID"
                  value={updateItemId}
                  onChange={e=>setUpdateItemId(e.target.value)}
                />
                <input
                  className="cargo-input"
                  placeholder="Name"
                  value={updateItemData.name}
                  onChange={e=>setUpdateItemData({ ...updateItemData, name:e.target.value })}
                />
                <input
                  className="cargo-input"
                  placeholder="Description"
                  value={updateItemData.description}
                  onChange={e=>setUpdateItemData({ ...updateItemData, description:e.target.value })}
                />
                <input
                  className="cargo-input"
                  placeholder="Category"
                  value={updateItemData.category}
                  onChange={e=>setUpdateItemData({ ...updateItemData, category:e.target.value })}
                />
                <input
                  className="cargo-input"
                  type="number"
                  placeholder="Quantity"
                  value={updateItemData.quantity}
                  onChange={e=>setUpdateItemData({ ...updateItemData, quantity:+e.target.value })}
                />

                <div style={{ margin:'12px 0' }}>
                  <input type="file" onChange={e=>setUpdateItemImage(e.target.files[0])} />
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
