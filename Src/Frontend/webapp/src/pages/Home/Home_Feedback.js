// Home_Feedback.js
import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "../../css/Home/Home_Feedback.css"; // Use dedicated CSS for the feedback page

const Home_Feedback = ({ username }) => {
  const navigate = useNavigate();
  const [feedbackContent, setFeedbackContent] = useState("");
  const [feedbackPhoneNumber, setFeedbackPhoneNumber] = useState("");
  const [feedbackError, setFeedbackError] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState("");

  const baseURL = process.env.REACT_APP_BASE_URL;

  // Handle submission of feedback
  const handleSubmitFeedback = async () => {
    if (!feedbackContent.trim()) {
      setFeedbackError("Feedback content cannot be empty.");
      return;
    }
    if (!feedbackPhoneNumber.trim()) {
      setFeedbackError("Phone number cannot be empty.");
      return;
    }
    setFeedbackError("");
    try {
      const payload = {
        name: username,
        phoneNumber: feedbackPhoneNumber,
        content: feedbackContent,
      };
      const response = await axios.post(`${baseURL}/api/feedback/submit`, payload);
      if (response.data.status === "success") {
        setFeedbackMessage("Feedback submitted successfully!");
        setTimeout(() => {
          // After successful submission, navigate back to the previous page or home
          navigate(-1);
        }, 1500);
      } else {
        setFeedbackError(response.data.message || "Failed to submit feedback.");
      }
    } catch (error) {
      setFeedbackError(error.response?.data?.message || "Failed to submit feedback.");
    }
  };

  // Cancel and navigate back
  const handleCancel = () => {
    navigate(-1);
  };

  return (
    <div className="feedback-container">
      <h2>We need your Feedback</h2>
      <div className="feedback-formGroup">
        <label>Phone Number:</label>
        <input
          type="text"
          value={feedbackPhoneNumber}
          onChange={(e) => setFeedbackPhoneNumber(e.target.value)}
          className="feedback-input"
        />
      </div>
      <div className="feedback-formGroup">
        <label>Feedback:</label>
        <textarea
          value={feedbackContent}
          onChange={(e) => setFeedbackContent(e.target.value)}
          className="feedback-input feedback-textarea"
        />
      </div>
      {feedbackError && <p className="feedback-errorText">{feedbackError}</p>}
      {feedbackMessage && <p className="feedback-successText">{feedbackMessage}</p>}
      <button className="feedback-button" onClick={handleSubmitFeedback}>
        Submit Feedback
      </button>
      <button className="feedback-cancelButton" onClick={handleCancel}>
        Cancel
      </button>
    </div>
  );
};

export default Home_Feedback;
