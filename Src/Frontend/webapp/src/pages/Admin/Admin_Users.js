import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { secureAxios } from '../../config/axiosConfig';
import '../../css/Admin/Admin_Users.css';

const AdminUsers = ({ userData }) => {
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [usersError, setUsersError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [newUser, setNewUser] = useState({
    username: "",
    email: "",
    phone: "",
    role: "CLIENT",
    firstName: "",
    lastName: "",
    password: "" // Added password field for new user creation
  });
  const [updateUserData, setUpdateUserData] = useState(null);
  const [updateSubroleUser, setUpdateSubroleUser] = useState(null);
  const [subroleSelection, setSubroleSelection] = useState("REGULAR");
  const [subroleNotes, setSubroleNotes] = useState("");

  const loadUsers = useCallback(async () => {
    try {
      setIsLoading(true);
      setUsersError('');
      
      // Use secureAxios for admin operations (HTTPS required)
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
  };

  const addUser = async () => {
    try {
      // Validate required fields
      if (!newUser.username || !newUser.role) {
        alert("Username and role are required");
        return;
      }
      
      const response = await secureAxios.post('/api/admin/user/create', {
        adminUsername: userData.username,
        authenticated: "true",
        ...newUser
      });
      
      alert(
        response.data.message +
        (response.data.generatedPassword ? 
          "\nGenerated Password: " + response.data.generatedPassword : "")
      );
      
      setNewUser({
        username: "", 
        email: "", 
        phone: "",
        role: "CLIENT", 
        firstName: "", 
        lastName: "",
        password: ""
      });
      loadUsers();
    } catch (error) {
      console.error("Error creating user:", error);
      alert(error.response?.data?.message || error.message);
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
          ...updateUserData
        }
      );
      alert(response.data.message);
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
  };

  const handleOpenSubroleForm = (user) => {
    setUpdateSubroleUser(user);
    setSubroleSelection(user.volunteerSubRole || "REGULAR");
    setSubroleNotes("");
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
          <h1 className="cargo-title">User Management</h1>
          
          {/* Error Message */}
          {usersError && (
            <div style={{ 
              padding: '10px', 
              margin: '10px 0', 
              backgroundColor: '#ffebee', 
              color: '#c62828', 
              borderRadius: '4px' 
            }}>
              Error: {usersError}
            </div>
          )}

          {/* All Users Table */}
          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">All Users</div>
            {isLoading ? (
              <div style={{ padding: '20px', textAlign: 'center' }}>
                Loading users...
              </div>
            ) : (
              <table className="cargo-table">
                <thead>
                  <tr>
                    <th>User ID</th>
                    <th>Username</th>
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
                      <td>{user.email || 'N/A'}</td>
                      <td>{user.role}</td>
                      <td>{user.phone || 'N/A'}</td>
                      <td>
                        <button
                          className="manage-btn"
                          onClick={() => deleteUser(user.username)}
                        >Delete</button>
                        <button
                          className="manage-btn"
                          onClick={() => setUpdateUserData(user)}
                        >Update</button>
                        <button
                          className="manage-btn"
                          onClick={() => resetUserPassword(user)}
                        >Reset Password</button>
                        {user.role === "VOLUNTEER" && (
                          <button
                            className="manage-btn"
                            onClick={() => handleOpenSubroleForm(user)}
                          >Update Subrole</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* Add New User Form */}
          <div className="content-block beige-block">
            <div className="block-content">
              <h3 className="block-title">Add New User</h3>
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
              <button className="cargo-button" onClick={addUser}>
                Add User
              </button>
            </div>
          </div>

          {/* Update User Form */}
          {updateUserData && (
            <div className="content-block blue-block">
              <div className="block-content">
                <h3 className="block-title">
                  Update User: {updateUserData.username}
                </h3>
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
                <button className="cargo-button" onClick={updateUser}>
                  Submit Update
                </button>
                <button
                  className="cargo-button"
                  onClick={() => setUpdateUserData(null)}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {/* Update Subrole Form */}
          {updateSubroleUser && (
            <div className="content-block blue-block">
              <div className="block-content">
                <h3 className="block-title">
                  Update Subrole: {updateSubroleUser.username}
                </h3>
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
                <button
                  className="cargo-button"
                  onClick={handleSubmitSubrole}
                >
                  Submit Subrole Update
                </button>
                <button
                  className="cargo-button"
                  onClick={handleCancelSubrole}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default AdminUsers;