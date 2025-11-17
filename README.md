# StreetMed Project Repository

## Overview

This repository contains the StreetMedGo application, including both the backend and frontend code, along with UI/UX design patterns.

## Web App Overview (Currently Undeployed)
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
│   │   ├── Dockerfile    # Docker configuration for backend
│   │   └── docker-compose.yml  # Multi-container orchestration
│   └── Frontend/webapp/  # React frontend application
├── docs/                 # Supporting documents for development
└── Required_dependencies.md
```

## Prerequisites

### For Docker Setup (Recommended)
- Docker Desktop (see installation guide below)
- Docker Compose (included with Docker Desktop)

### For Local Development Setup
Please install all required dependencies listed in:
📋 **[Required Dependencies Guide](Requried_dependencies.md)**

---

## Quick Start

You can run the application using either **Docker** (recommended) or **local development setup**.

### Option 1: Using Docker (Recommended) 🐳

Docker provides an isolated, consistent environment and handles all dependencies automatically.

#### Step 1: Start the Backend Services

```bash
# Navigate to backend directory
cd Src/Backend

# Build and start all services (MySQL, MongoDB, Backend)
docker-compose up --build

# Or run in detached mode (runs in background)
docker-compose up -d --build
```

This single command will:
- Start MySQL database on port 3307
- Start MongoDB on port 27017
- Build and start the Spring Boot backend on ports 8080 (HTTP) and 8443 (HTTPS)

#### Step 2: Start the Frontend

```bash
# Open a new terminal and navigate to frontend directory
cd Src/Frontend/webapp

# Install dependencies (first time only)
npm install

# Start the development server
npm start
```

#### Managing Docker Services

**View running containers:**
```bash
docker-compose ps
```

**View logs:**
```bash
# View logs for all services
docker-compose logs

# View logs for specific service
docker-compose logs backend
docker-compose logs mysql
docker-compose logs mongodb

# Follow logs in real-time
docker-compose logs -f backend
```

**Stop services:**
```bash
# Stop all services (preserves data)
docker-compose down

# Stop and remove volumes (clears all database data)
docker-compose down -v
```

**Restart a specific service:**
```bash
# Restart just the backend
docker-compose restart backend
```

**Rebuild after code changes:**
```bash
# Rebuild and restart
docker-compose up --build

# Or rebuild without cache
docker-compose build --no-cache
docker-compose up
```

### Option 2: Local Development Setup

If you prefer running services locally without Docker:

#### Backend
```bash
cd Src/Backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

#### Frontend
```bash
cd Src/Frontend/webapp
npm install
npm start
```

**Note:** For local setup, ensure MySQL and MongoDB are installed and running. See [Required Dependencies Guide](Requried_dependencies.md) for details.

---

## Verify Setup

Once everything is running, access the application at:

- **Frontend**: http://localhost:3000
- **Backend API (HTTP)**: http://localhost:8080
- **Backend API (HTTPS)**: https://localhost:8443
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Secure API Documentation**: https://localhost:8443/swagger-ui.html

---

## Docker Installation Guide

### Windows

1. **Download Docker Desktop for Windows**
   - Visit: https://www.docker.com/products/docker-desktop
   - Click "Download for Windows"
   - Requires Windows 10 64-bit: Pro, Enterprise, or Education (Build 16299 or later)

2. **Install Docker Desktop**
   - Run the installer (Docker Desktop Installer.exe)
   - Follow the installation wizard
   - Enable WSL 2 (Windows Subsystem for Linux) if prompted
   - Restart your computer

3. **Verify Installation**
   ```bash
   docker --version
   docker-compose --version
   ```

### macOS

1. **Download Docker Desktop for Mac**
   - Visit: https://www.docker.com/products/docker-desktop
   - Choose the version for your chip:
     - **Apple Silicon (M1/M2/M3)**: Download for Apple Silicon
     - **Intel Chip**: Download for Intel

2. **Install Docker Desktop**
   - Open the downloaded .dmg file
   - Drag Docker to Applications folder
   - Launch Docker from Applications
   - Grant necessary permissions when prompted

3. **Verify Installation**
   ```bash
   docker --version
   docker-compose --version
   ```

### Linux (Ubuntu/Debian)

1. **Update package index**
   ```bash
   sudo apt-get update
   ```

2. **Install prerequisites**
   ```bash
   sudo apt-get install \
       apt-transport-https \
       ca-certificates \
       curl \
       gnupg \
       lsb-release
   ```

3. **Add Docker's official GPG key**
   ```bash
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
   ```

4. **Set up the stable repository**
   ```bash
   echo \
     "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
     $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   ```

5. **Install Docker Engine**
   ```bash
   sudo apt-get update
   sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin
   ```

6. **Start Docker service**
   ```bash
   sudo systemctl start docker
   sudo systemctl enable docker
   ```

7. **Add your user to docker group (optional, to run without sudo)**
   ```bash
   sudo usermod -aG docker $USER
   newgrp docker
   ```

8. **Verify Installation**
   ```bash
   docker --version
   docker compose version
   ```



### Cleaning Up Docker Resources

```bash
# Remove all stopped containers, unused networks, dangling images
docker system prune

# Remove everything (including volumes)
docker system prune -a --volumes

# Check disk usage
docker system df
```

---
### Frontend Issues

**Cannot connect to backend:**
- Verify backend is running: `docker-compose ps`
- Check backend logs: `docker-compose logs backend`
- Ensure ports are not blocked by firewall

**npm install fails:**
```bash
# Clear npm cache
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### HTTPS Certificate Issues

When accessing https://localhost:8443, you may see a certificate warning. This is normal for development. You can:
- Click "Advanced" and proceed (Chrome/Edge)
- Click "Accept the Risk and Continue" (Firefox)
- Or configure your browser to trust the self-signed certificate

---


## Development Workflow

### Making Backend Changes

1. Make your code changes
2. Rebuild and restart:
   ```bash
   docker-compose up -d --build backend
   ```

### Making Database Schema Changes

1. Update entity classes in the backend
2. Restart backend with rebuild:
   ```bash
   docker-compose down
   docker-compose up --build
   ```

### Resetting Database Data

```bash
# Stop services and remove volumes
docker-compose down -v

# Start fresh
docker-compose up -d
```
---

**Happy Coding! 🚀**
