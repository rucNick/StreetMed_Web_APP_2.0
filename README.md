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

## Repository Structure

```
StreetMed/
├── Src/
│   ├── Backend/          # Spring Boot backend application
│   └── Frontend/webapp/  # React frontend application
├── docs/                 # Supporting documents for development
└── Required_dependencies.md
```

## Prerequisites

Before setting up the project, please install all required dependencies listed in:

📋 **[Required Dependencies Guide](Requried_dependencies.md)**

## Quick Start

### Backend
```bash
cd Src/Backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend
```bash
cd Src/Frontend/webapp
npm install
npm start
```

### Verify Setup
- **Frontend**: http://localhost:3000
- **API Docs**: http://localhost:8080/swagger-ui.html

## Documentation

- **Setup Guide**: [Required_dependencies.md](Requried_dependencies.md)
- **Technical Guide**: [Backend Technical Guide](Docs/BackendTechGuide.md)
