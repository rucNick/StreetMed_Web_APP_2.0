# StreetMed Project Repository

## Overview

This repository contains the StreetMedGo application, including both the backend and frontend code, along with UI/UX design patterns.

## Web App Overview
**WEB URL: https://app.streetmedatpitt.org/**

StreetMedGo is a comprehensive platform designed to support the operations of StreetMed@Pitt, a non-profit organization providing medical outreach to underserved communities. The web application serves three primary user roles:

- **Clients**: Can request medical supplies and services, track their orders, and receive updates on deliveries.
- **Volunteers**: Can sign up for outreach rounds, view assigned orders, manage their availability, and coordinate with team members.
- **Administrators**: Can oversee inventory, manage volunteer applications, schedule rounds, assign team leads and clinicians, and generate reports.

The platform integrates several key systems:

- Round scheduling and volunteer coordination
- Order creation and fulfillment tracking
- Inventory management with automatic stock reservation
- Volunteer application and approval workflow
- Real-time communication between stakeholders

StreetMedGo streamlines the process of connecting those in need with essential medical supplies while providing volunteers with the tools they need to effectively serve the community.

Clients: Can request medical supplies and services, track their orders, and receive updates on deliveries.
Volunteers: Can sign up for outreach rounds, view assigned orders, manage their availability, and coordinate with team members.
Administrators: Can oversee inventory, manage volunteer applications, schedule rounds, assign team leads and clinicians, and generate reports.

The platform integrates several key systems:

Round scheduling and volunteer coordination
Order creation and fulfillment tracking
Inventory management with automatic stock reservation
Volunteer application and approval workflow
Real-time communication between stakeholders

StreetMedGo streamlines the process of connecting those in need with essential medical supplies while providing volunteers with the tools they need to effectively serve the community.

## Repository Structure

- `src/Backend/` - Spring Boot backend application
- `src/Frontend/` - React frontend application
- `docs` - Supporting Documents for development

## Backend

The backend is built using Spring Boot and handles server-side functionality including user authentication, order management, volunteer coordination, and inventory management.

### Quick Start (Backend)

1. Navigate to the backend directory: `cd src/Backend`
2. Configure environment variables or update application.properties
3. Run using Maven:
   ```
   ./mvn spring-boot:run
   ```

## Frontend

The frontend is built with React and provides the user interface for clients, volunteers, and administrators.

### Quick Start (Frontend)

1. Navigate to the frontend directory: `cd src/Frontend`
2. Install dependencies: `npm install`
3. Start the development server: `npm start`
4. The application will be available at http://localhost:3000


## CI/CD

This project currently will not have the CI/CD for deployment

## Technical Documentation

For comprehensive technical documentation including API endpoints, data models, and business logic, please refer to:

[Backend Technical Guide](docs/BackendTechGuide.md)
