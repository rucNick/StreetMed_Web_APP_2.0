// src/components/SessionErrorModal.js
import React from 'react';

const SessionErrorModal = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <h3>Session Expired</h3>
        <p>Your secure session has expired or is invalid. This can happen when the page has been open for too long.</p>
        <div className="modal-buttons">
          <button className="modal-btn" onClick={() => window.location.reload()}>
            Refresh Page
          </button>
          <button className="modal-cancel-btn" onClick={onClose}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
};

export default SessionErrorModal;