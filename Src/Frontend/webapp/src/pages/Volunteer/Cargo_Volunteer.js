import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { publicAxios } from '../../config/axiosConfig';
import '../../index.css'; 

const Cargo_Volunteer = () => {
  const navigate = useNavigate();
  const [allItems, setAllItems] = useState([]);
  const [error, setError] = useState('');

  const fetchAllItems = useCallback(async () => {
    try {
      // Use publicAxios for cargo items (public endpoint)
      const response = await publicAxios.get('/api/cargo/items');
      setAllItems(response.data);
      setError('');
    } catch (err) {
      // Handle certificate errors
      if (err.code === 'ERR_CERT_AUTHORITY_INVALID') {
        setError('Certificate error. Please accept the certificate and try again.');
        window.dispatchEvent(new CustomEvent('certificate-error', { 
          detail: { url: process.env.REACT_APP_BASE_URL }
        }));
      } else {
        setError(err.response?.data?.message || err.message);
      }
    }
  }, []);

  useEffect(() => {
    fetchAllItems();
  }, [fetchAllItems]);

  const handleBack = () => {
    navigate('/');
  };

  const getStatus = (count) => {
    if (typeof count !== 'number' || isNaN(count)) return '';
    if (count === 0) return '<span class="status out">Out</span>';
    if (count < 5) return '<span class="status low">Low</span>';
    return '<span class="status fine">Fine</span>';
  };

  return (
    <div className="page-body">
      <header className="store-header">
        <div className="cargo-header">
          <img src="/Untitled.png" alt="Logo" className="store-logo" />
          <h2 className="cargo-title">Cargo Volunteer Page</h2>
        </div>
        <button onClick={handleBack} className="manage-btn">Back to Dashboard</button>
      </header>

      <main className="cargo-container">
        <div className="cargo-card">
          <h3 className="table-title">Inventory Management (Read-Only)</h3>
          {error && <p style={{ color: 'red' }}>{error}</p>}

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
              </tr>
            </thead>
            <tbody>
              {allItems.map((item) => {
                const hasSizes = item.sizeQuantities && Object.keys(item.sizeQuantities).length > 0;

                return (
                  <React.Fragment key={item.id}>
                    <tr>
                      <td>{item.id}</td>
                      <td>{item.name}</td>
                      <td>{item.description}</td>
                      <td>{item.category}</td>
                      <td>
                        <div className="cell-container">
                          <span>
                            {item.quantity}
                            {/* {item.quantity < 5 && (
                              <span style={{ color: 'red', marginLeft: '5px' }}>
                                (Low-Stock!)
                              </span>
                            )} */}
                          </span>
                          <span
                            dangerouslySetInnerHTML={{ __html: getStatus(item.quantity) }}
                          />
                        </div>
                      </td>
                      <td></td>
                      <td></td>
                    </tr>

                    {hasSizes &&
                      Object.entries(item.sizeQuantities).map(([size, qty]) => (
                        <tr key={`${item.id}-${size}`}>
                          <td></td>
                          <td></td>
                          <td></td>
                          <td></td>
                          <td></td>
                          <td>{size}</td>
                          <td>
                            <div className="cell-container">
                              <span>{qty}</span>
                              <span
                                dangerouslySetInnerHTML={{ __html: getStatus(qty) }}
                              />
                            </div>
                          </td>
                        </tr>
                      ))}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
};

export default Cargo_Volunteer;