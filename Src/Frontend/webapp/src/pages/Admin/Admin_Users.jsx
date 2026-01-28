import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { secureAxios } from '../../config/axiosConfig';
import '../../index.css'; 

const AdminUsers = ({ userData }) => {
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [usersError, setUsersError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showAddUserForm, setShowAddUserForm] = useState(false);
  const [activeActionMenu, setActiveActionMenu] = useState(null);
  const actionMenuRef = useRef(null);
  
  const [newUser, setNewUser] = useState({
    username: "",
    email: "",
    phone: "",
    role: "CLIENT",
    firstName: "",
    lastName: "",
    password: ""
  });
  const [updateUserData, setUpdateUserData] = useState(null);
  const [updateSubroleUser, setUpdateSubroleUser] = useState(null);
  const [subroleSelection, setSubroleSelection] = useState("REGULAR");
  const [subroleNotes, setSubroleNotes] = useState("");

  // Close action menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (actionMenuRef.current && !actionMenuRef.current.contains(event.target)) {
        setActiveActionMenu(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Helper function to format display name
  const formatDisplayName = (firstName, lastName) => {
    const first = firstName && firstName !== 'N/A' ? firstName : '';
    const last = lastName && lastName !== 'N/A' ? lastName : '';
    const fullName = `${first} ${last}`.trim();
    return fullName || 'N/A';
  };

  const loadUsers = useCallback(async () => {
    try {
      setIsLoading(true);
      setUsersError('');
      
      const response = await secureAxios.get('/api/admin/users', {
        headers: {
          "Admin-Username": userData.username,
          "Authentication-Status": "true"
        },
        withCredentials: true
      });
      
      const data = response.data.data;
      setUsers([
        ...(data.clients || []),
        ...(data.volunteers || []),
        ...(data.admins || [])
      ]);
    } catch (error) {
      console.error("Error loading users:", error);
      if (error.response?.data?.httpsRequired) {
        setUsersError("Secure HTTPS connection required. Please ensure you're using HTTPS.");
      } else {
        setUsersError(error.response?.data?.message || error.message);
      }
    } finally {
      setIsLoading(false);
    }
  }, [userData.username]);

  const deleteUser = async (usernameToDelete) => {
    if (!window.confirm(`Are you sure you want to delete user: ${usernameToDelete}?`)) {
      return;
    }
    
    try {
      const response = await secureAxios.delete('/api/admin/user/delete', { 
        data: {
          authenticated: "true",
          adminUsername: userData.username,
          username: usernameToDelete
        }
      });
      alert(response.data.message);
      loadUsers();
    } catch (error) {
      console.error("Error deleting user:", error);
      alert(error.response?.data?.message || error.message);
    }
    setActiveActionMenu(null);
  };

  const addUser = async () => {
    try {
      if (!newUser.username || !newUser.role) {
        alert("Username and role are required");
        return;
      }

      const validRoles = ["CLIENT", "VOLUNTEER", "ADMIN"];
      if (!validRoles.includes(newUser.role)) {
        alert("Invalid role. Must be CLIENT, VOLUNTEER, or ADMIN");
        return;
      }

      if (newUser.role === "VOLUNTEER" && !newUser.email) {
        alert("Email is required for VOLUNTEER role");
        return;
      }

      const requestPayload = {
        adminUsername: userData.username,
        authenticated: "true",
        userData: {
          username: newUser.username,
          role: newUser.role,
          email: newUser.email || "",
          phone: newUser.phone || "",
          firstName: newUser.firstName || "",
          lastName: newUser.lastName || "",
          password: newUser.password || ""
        }
      };

      const response = await secureAxios.post('/api/admin/user/create', requestPayload);
      
      let successMessage = response.data.message || "User created successfully!";
      
      if (response.data.generatedPassword) {
        successMessage = `User created successfully!\n\n` +
                        `Username: ${newUser.username}\n` +
                        `Role: ${newUser.role}\n` +
                        `Generated Password: ${response.data.generatedPassword}\n\n` +
                        `SAVE THIS PASSWORD - It cannot be retrieved later!`;
      }
      
      alert(successMessage);
      
      setNewUser({
        username: "", 
        email: "", 
        phone: "",
        role: "CLIENT", 
        firstName: "", 
        lastName: "",
        password: ""
      });
      
      setShowAddUserForm(false);
      loadUsers();
      
    } catch (error) {
      console.error("Error creating user:", error);
      
      if (error.response?.data?.message) {
        const errorMsg = error.response.data.message;
        
        if (errorMsg.includes("already exists")) {
          alert("Username already exists. Please choose a different username.");
        } else if (errorMsg.includes("required")) {
          alert(errorMsg);
        } else {
          alert(`Error: ${errorMsg}`);
        }
      } else if (error.request) {
        alert("No response from server. Please check if the backend is running.");
      } else {
        alert(`Error: ${error.message}`);
      }
    }
  };

  const updateUser = async () => {
    if (!updateUserData) return;
    
    try {
      const response = await secureAxios.put(
        `/api/admin/user/update/${updateUserData.userId}`,
        {
          adminUsername: userData.username,
          authenticated: "true",
          updateData: { 
            username: updateUserData.username || "",
            email: updateUserData.email || "",
            phone: updateUserData.phone || "",
            firstName: updateUserData.firstName || "",
            lastName: updateUserData.lastName || "",
            role: updateUserData.role || ""
          }
        }
      );
      alert(response.data.message || "User updated successfully");
      setUpdateUserData(null);
      loadUsers();
    } catch (error) {
      console.error("Error updating user:", error);
      alert(error.response?.data?.message || error.message);
    }
  };

  const resetUserPassword = async (user) => {
    if (!user.userId) {
      alert("Cannot reset password: User ID is missing");
      return;
    }
    const newPassword = window.prompt(
      `Enter new password for user ${user.username}:`
    );
    if (!newPassword) return;
    
    try {
      const response = await secureAxios.put(
        `/api/admin/user/reset-password/${user.userId}`,
        {
          adminUsername: userData.username,
          authenticated: "true",
          newPassword
        }
      );
      alert(response.data.message);
    } catch (error) {
      console.error("Error resetting password:", error);
      alert(error.response?.data?.message || error.message);
    }
    setActiveActionMenu(null);
  };

  const handleOpenSubroleForm = (user) => {
    setUpdateSubroleUser(user);
    setSubroleSelection(user.volunteerSubRole || "REGULAR");
    setSubroleNotes("");
    setActiveActionMenu(null);
  };
  
  const handleCancelSubrole = () => {
    setUpdateSubroleUser(null);
    setSubroleSelection("REGULAR");
    setSubroleNotes("");
  };
  
  const handleSubmitSubrole = async () => {
    if (!updateSubroleUser?.userId) {
      alert("User ID is missing");
      return;
    }
    try {
      const resp = await secureAxios.put('/api/admin/volunteer/subrole', {
        adminUsername: userData.username,
        authenticated: "true",
        userId: updateSubroleUser.userId.toString(),
        volunteerSubRole: subroleSelection,
        notes: subroleNotes
      });
      alert(resp.data.message);
      loadUsers();
    } catch (error) {
      console.error("Error updating subrole:", error);
      alert(error.response?.data?.message || error.message);
    }
    handleCancelSubrole();
  };

  const handleUpdateClick = (user) => {
    setUpdateUserData(user);
    setActiveActionMenu(null);
  };

  const toggleActionMenu = (userId) => {
    setActiveActionMenu(activeActionMenu === userId ? null : userId);
  };

  useEffect(() => { 
    loadUsers(); 
  }, [loadUsers]);

  return (
    <div className="page-container">
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">User Management System</span>
          </div>
          <div className="header-right">
            <button className="manage-btn" onClick={() => navigate(-1)}>
              ← Go Back
            </button>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="cargo-container">
          <h1 className="cargo-title">Add or Update Users</h1>
          
          {/* Error Message */}
          {usersError && (
            <div className="error-banner">
              Error: {usersError}
            </div>
          )}

          {/* Add New User Button */}
          <div style={{ marginBottom: '20px' }}>
            <button 
              className="cargo-button"
              onClick={() => setShowAddUserForm(!showAddUserForm)}
              style={{ 
                display: 'flex', 
                alignItems: 'center', 
                gap: '8px',
                fontSize: '16px',
                padding: '12px 24px'
              }}
            >
              {showAddUserForm ? '− Cancel' : '+ Add New User'}
            </button>
          </div>

          {/* Add New User Form (Collapsible) */}
          {showAddUserForm && (
            <div className="content-block beige-block" style={{ marginBottom: '20px' }}>
              <div className="block-content" style={{ flexDirection: 'column', gap: '12px' }}>
                <h3 className="block-title">Add New User</h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', width: '100%' }}>
                  <input
                    className="cargo-input"
                    placeholder="Username *"
                    value={newUser.username}
                    onChange={e => setNewUser({...newUser, username: e.target.value})}
                  />
                  <input
                    className="cargo-input"
                    placeholder="Email"
                    type="email"
                    value={newUser.email}
                    onChange={e => setNewUser({...newUser, email: e.target.value})}
                  />
                  <input
                    className="cargo-input"
                    placeholder="Phone"
                    value={newUser.phone}
                    onChange={e => setNewUser({...newUser, phone: e.target.value})}
                  />
                  <input
                    className="cargo-input"
                    placeholder="First Name"
                    value={newUser.firstName}
                    onChange={e => setNewUser({...newUser, firstName: e.target.value})}
                  />
                  <input
                    className="cargo-input"
                    placeholder="Last Name"
                    value={newUser.lastName}
                    onChange={e => setNewUser({...newUser, lastName: e.target.value})}
                  />
                  <input
                    className="cargo-input"
                    placeholder="Password (leave empty for auto-generate)"
                    type="password"
                    value={newUser.password}
                    onChange={e => setNewUser({...newUser, password: e.target.value})}
                  />
                  <select
                    className="cargo-input"
                    value={newUser.role}
                    onChange={e => setNewUser({...newUser, role: e.target.value})}
                  >
                    <option value="CLIENT">CLIENT</option>
                    <option value="VOLUNTEER">VOLUNTEER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>
                <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                  <button className="cargo-button primary" onClick={addUser}>
                    Create User
                  </button>
                  <button className="cargo-button secondary" onClick={() => setShowAddUserForm(false)}>
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* All Users Table */}
          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">All Users</div>
            {isLoading ? (
              <div className="loading-container">
                <div className="loading-spinner"></div>
                <p>Loading users...</p>
              </div>
            ) : (
              <table className="cargo-table">
                <thead>
                  <tr>
                    <th>User ID</th>
                    <th>Username</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Phone</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user, idx) => (
                    <tr key={user.userId || idx}>
                      <td>{user.userId || idx}</td>
                      <td>{user.username}</td>
                      <td>{formatDisplayName(user.firstName, user.lastName)}</td>
                      <td>{user.email || 'N/A'}</td>
                      <td>{user.role}</td>
                      <td>{user.phone || 'N/A'}</td>
                      <td>
                        <div className="action-dropdown" ref={activeActionMenu === user.userId ? actionMenuRef : null}>
                          <button
                            className="manage-btn action-dropdown-trigger"
                            onClick={() => toggleActionMenu(user.userId)}
                          >
                            Actions ▾
                          </button>
                          {activeActionMenu === user.userId && (
                            <div className="action-dropdown-menu">
                              <button 
                                className="action-dropdown-item"
                                onClick={() => handleUpdateClick(user)}
                              >
                                ✏️ Update
                              </button>
                              <button 
                                className="action-dropdown-item"
                                onClick={() => resetUserPassword(user)}
                              >
                                🔑 Reset Password
                              </button>
                              {user.role === "VOLUNTEER" && (
                                <button 
                                  className="action-dropdown-item"
                                  onClick={() => handleOpenSubroleForm(user)}
                                >
                                  👤 Update Subrole
                                </button>
                              )}
                              <button 
                                className="action-dropdown-item danger"
                                onClick={() => deleteUser(user.username)}
                              >
                                🗑️ Delete
                              </button>
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* Update User Form */}
          {updateUserData && (
            <div className="content-block blue-block">
              <div className="block-content" style={{ flexDirection: 'column', gap: '12px' }}>
                <h3 className="block-title">
                  Update User: {updateUserData.username}
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', width: '100%' }}>
                  {["username", "email", "phone", "firstName", "lastName"].map(field => (
                    <input
                      key={field}
                      className="cargo-input"
                      placeholder={field.charAt(0).toUpperCase() + field.slice(1)}
                      value={updateUserData[field] || ""}
                      onChange={e => setUpdateUserData({
                        ...updateUserData, 
                        [field]: e.target.value
                      })}
                    />
                  ))}
                  <select
                    className="cargo-input"
                    value={updateUserData.role}
                    onChange={e => setUpdateUserData({
                      ...updateUserData, 
                      role: e.target.value
                    })}
                  >
                    <option value="CLIENT">CLIENT</option>
                    <option value="VOLUNTEER">VOLUNTEER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>
                <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                  <button className="cargo-button primary" onClick={updateUser}>
                    Submit Update
                  </button>
                  <button
                    className="cargo-button secondary"
                    onClick={() => setUpdateUserData(null)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Update Subrole Form */}
          {updateSubroleUser && (
            <div className="content-block blue-block">
              <div className="block-content" style={{ flexDirection: 'column', gap: '12px' }}>
                <h3 className="block-title">
                  Update Subrole: {updateSubroleUser.username}
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', width: '100%' }}>
                  <select
                    className="cargo-input"
                    value={subroleSelection}
                    onChange={e => setSubroleSelection(e.target.value)}
                  >
                    <option value="REGULAR">REGULAR</option>
                    <option value="TEAM_LEAD">TEAM_LEAD</option>
                    <option value="CLINICIAN">CLINICIAN</option>
                  </select>
                  <input
                    className="cargo-input"
                    placeholder="Notes (optional)"
                    value={subroleNotes}
                    onChange={e => setSubroleNotes(e.target.value)}
                  />
                </div>
                <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                  <button
                    className="cargo-button primary"
                    onClick={handleSubmitSubrole}
                  >
                    Submit Subrole Update
                  </button>
                  <button
                    className="cargo-button secondary"
                    onClick={handleCancelSubrole}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Inline styles for the action dropdown */}
      <style>{`
        .action-dropdown {
          position: relative;
          display: inline-block;
        }
        
        .action-dropdown-trigger {
          min-width: 100px;
        }
        
        .action-dropdown-menu {
          position: absolute;
          top: 100%;
          right: 0;
          background: #1a2332;
          border: 1px solid #3a5070;
          border-radius: 8px;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
          z-index: 100;
          min-width: 160px;
          overflow: hidden;
          margin-top: 4px;
        }
        
        .action-dropdown-item {
          display: block;
          width: 100%;
          padding: 10px 16px;
          background: none;
          border: none;
          color: #ffffff;
          font-size: 14px;
          text-align: left;
          cursor: pointer;
          transition: background-color 0.2s;
        }
        
        .action-dropdown-item:hover {
          background: #2a3a52;
        }
        
        .action-dropdown-item.danger {
          color: #ff6b6b;
        }
        
        .action-dropdown-item.danger:hover {
          background: rgba(255, 107, 107, 0.15);
        }

        .error-banner {
          padding: 12px 16px;
          margin-bottom: 20px;
          background-color: rgba(255, 77, 79, 0.2);
          color: #ff6b6b;
          border-radius: 8px;
          border-left: 4px solid #f44336;
        }

        .loading-container {
          padding: 40px;
          text-align: center;
          color: #ffffff;
        }

        .loading-spinner {
          width: 40px;
          height: 40px;
          border: 4px solid #3a5070;
          border-top: 4px solid #f6b800;
          border-radius: 50%;
          animation: spin 1s linear infinite;
          margin: 0 auto 16px;
        }

        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
};

export default AdminUsers;