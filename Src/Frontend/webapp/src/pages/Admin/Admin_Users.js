import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import '../../css/Admin/Admin_Users.css';

const AdminUsers = ({ userData }) => {
  const baseURL = process.env.REACT_APP_BASE_URL;
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [usersError, setUsersError] = useState('');
  const [newUser, setNewUser] = useState({
    username: "",
    email: "",
    phone: "",
    role: "CLIENT",
    firstName: "",
    lastName: ""
  });
  const [updateUserData, setUpdateUserData] = useState(null);
  const [updateSubroleUser, setUpdateSubroleUser] = useState(null);
  const [subroleSelection, setSubroleSelection] = useState("REGULAR");
  const [subroleNotes, setSubroleNotes] = useState("");

  const loadUsers = useCallback(async () => {
    try {
      const response = await axios.get(
        `${baseURL}/api/admin/users`,
        {
          headers: {
            "Admin-Username": userData.username,
            "Authentication-Status": "true"
          },
          withCredentials: true
        }
      );
      const data = response.data.data;
      setUsers([
        ...(data.clients || []),
        ...(data.volunteers || []),
        ...(data.admins || [])
      ]);
    } catch (error) {
      setUsersError(error.response?.data?.message || error.message);
    }
  }, [userData.username, baseURL]);

  const deleteUser = async (usernameToDelete) => {
    try {
      const response = await axios.delete(
        `${baseURL}/api/admin/user/delete`,
        { data: {
            authenticated: "true",
            adminUsername: userData.username,
            username: usernameToDelete
          }
        }
      );
      alert(response.data.message);
      loadUsers();
    } catch (error) {
      alert(error.response?.data?.message || error.message);
    }
  };

  const addUser = async () => {
    try {
      const response = await axios.post(
        `${baseURL}/api/admin/user/create`,
        {
          adminUsername: userData.username,
          authenticated: "true",
          ...newUser
        }
      );
      alert(
        response.data.message +
        "\nGenerated Password: " +
        response.data.generatedPassword
      );
      setNewUser({
        username: "", email: "", phone: "",
        role: "CLIENT", firstName: "", lastName: ""
      });
      loadUsers();
    } catch (error) {
      alert(error.response?.data?.message || error.message);
    }
  };

  const updateUser = async () => {
    try {
      const response = await axios.put(
        `${baseURL}/api/admin/user/update/${updateUserData.userId}`,
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
      alert(error.response?.data?.message || error.message);
    }
  };

  const resetUserPassword = async (user) => {
    if (!user.userId) {
      alert("Cannot reset password: User ID is missing");
      return;
    }
    const newPassword = window.prompt(
      `Enter new password for user ${user.username}`
    );
    if (!newPassword) return;
    try {
      const response = await axios.put(
        `${baseURL}/api/admin/user/reset-password/${user.userId}`,
        {
          adminUsername: userData.username,
          authenticated: "true",
          newPassword
        }
      );
      alert(response.data.message);
    } catch (error) {
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
      const resp = await axios.put(
        `${baseURL}/api/admin/volunteer/subrole`,
        {
          adminUsername: userData.username,
          authenticated: "true",
          userId: updateSubroleUser.userId.toString(),
          volunteerSubRole: subroleSelection,
          notes: subroleNotes
        }
      );
      alert(resp.data.message);
      loadUsers();
    } catch (error) {
      alert(error.response?.data?.message || error.message);
    }
    handleCancelSubrole();
  };

  useEffect(() => { loadUsers(); }, [loadUsers]);

  return (
    <div className="page-container">
      <header className="site-header">
        <div className="header-content">
          <div className="logo-container">
            <img src="/Untitled.png" alt="Logo" className="logo" />
            <span className="site-title">Cargo Management System</span>
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
          {/* ——— All Users ——— */}
          {usersError && <p className="cargo-error">Error: {usersError}</p>}

          <div className="cargo-card blue-block table-scroll">
            <div className="table-title">All Users</div>
            <table className="cargo-table">
              <thead>
                <tr>
                  <th>User ID</th><th>Username</th><th>Email</th>
                  <th>Role</th><th>Phone</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user, idx) => (
                  <tr key={idx}>
                    <td>{user.userId||idx}</td>
                    <td>{user.username}</td>
                    <td>{user.email}</td>
                    <td>{user.role}</td>
                    <td>{user.phone}</td>
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
                      {user.role==="VOLUNTEER" && (
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
          </div>

          {/* ——— Add New User ——— */}
          <div className="content-block beige-block">
            <div className="block-content">
              <h3 className="block-title">Add New User</h3>
              <input
                className="cargo-input"
                placeholder="Username"
                value={newUser.username}
                onChange={e=>setNewUser({...newUser,username:e.target.value})}
              />
              <input
                className="cargo-input"
                placeholder="Email"
                value={newUser.email}
                onChange={e=>setNewUser({...newUser,email:e.target.value})}
              />
              <input
                className="cargo-input"
                placeholder="Phone"
                value={newUser.phone}
                onChange={e=>setNewUser({...newUser,phone:e.target.value})}
              />
              <input
                className="cargo-input"
                placeholder="First Name"
                value={newUser.firstName}
                onChange={e=>setNewUser({...newUser,firstName:e.target.value})}
              />
              <input
                className="cargo-input"
                placeholder="Last Name"
                value={newUser.lastName}
                onChange={e=>setNewUser({...newUser,lastName:e.target.value})}
              />
              <select
                className="cargo-input"
                value={newUser.role}
                onChange={e=>setNewUser({...newUser,role:e.target.value})}
              >
                <option>CLIENT</option><option>VOLUNTEER</option>
              </select>
              <button className="cargo-button" onClick={addUser}>
                Add User
              </button>
            </div>
          </div>

          {/* ——— Update User——— */}
          {updateUserData && (
            <div className="content-block blue-block">
              <div className="block-content">
                <h3 className="block-title">
                  Update User: {updateUserData.username}
                </h3>
                {["username","email","phone","firstName","lastName"].map(field=>(
                  <input
                    key={field}
                    className="cargo-input"
                    placeholder={field}
                    value={updateUserData[field]||""}
                    onChange={e=>setUpdateUserData({
                      ...updateUserData,[field]:e.target.value
                    })}
                  />
                ))}
                <select
                  className="cargo-input"
                  value={updateUserData.role}
                  onChange={e=>setUpdateUserData({
                    ...updateUserData,role:e.target.value
                  })}
                >
                  <option>CLIENT</option><option>VOLUNTEER</option>
                </select>
                <button className="cargo-button" onClick={updateUser}>
                  Submit Update
                </button>
                <button
                  className="cargo-button"
                  onClick={()=>setUpdateUserData(null)}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {/* ——— Update Subrole ——— */}
          {updateSubroleUser && (
            <div className="content-block blue-block">
              <div className="block-content">
                <h3 className="block-title">
                  Update Subrole: {updateSubroleUser.username}
                </h3>
                <select
                  className="cargo-input"
                  value={subroleSelection}
                  onChange={e=>setSubroleSelection(e.target.value)}
                >
                  <option>REGULAR</option>
                  <option>TEAM_LEAD</option>
                  <option>CLINICIAN</option>
                </select>
                <input
                  className="cargo-input"
                  placeholder="Notes (optional)"
                  value={subroleNotes}
                  onChange={e=>setSubroleNotes(e.target.value)}
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
