# StreetMed Project Repository

## Overview

This repository contains the StreetMedGo application, including both the backend and frontend code, along with UI/UX design patterns.

## 🌐 Live Application

| Environment | URL |
|-------------|-----|
| **Production Frontend** | https://streetmed-frontend-900663028964.us-central1.run.app |
| **Production Backend** | https://streetmed-backend-900663028964.us-central1.run.app |
| **Custom Domain** | https://app.streetmedatpitt.org (pending DNS configuration) |

## Web App Overview

StreetMedGo is a comprehensive platform designed to support the operations of StreetMed@Pitt, a non-profit organization providing medical outreach to underserved communities. The web application serves three primary user roles:

- **Clients**: Can request medical supplies and services, track their orders, and receive updates on deliveries.
- **Volunteers**: Can sign up for outreach rounds, view assigned orders, manage their availability, and coordinate with team members.
- **Administrators**: Can oversee inventory, manage volunteer applications, schedule rounds, assign team leads and clinicians, and generate reports.

The platform integrates several key systems:

- Round scheduling and volunteer coordination
- Order creation and fulfillment tracking
- Inventory management with automatic stock reservation
- Volunteer application and approval workflow
- ECDH encryption for secure communications

## Repository Structure

```
StreetMed/
├── .github/
│   └── workflows/
│       ├── backenddeploy.yml   # Backend CI/CD to Google Cloud Run
│       └── frontenddeploy.yml  # Frontend CI/CD to Google Cloud Run
├── Src/
│   ├── Backend/                # Spring Boot backend application
│   │   ├── Dockerfile          # Docker configuration for backend
│   │   └── docker-compose.yml  # Multi-container orchestration (local dev)
│   └── Frontend/webapp/        # React frontend application
├── docs/                       # Supporting documents for development
└── Required_dependencies.md
```

---

## 🚀 Deployment

### Production Environment (Google Cloud Run)

The application is deployed on **Google Cloud Run** with automatic CI/CD via GitHub Actions.

| Component | Service | Region |
|-----------|---------|--------|
| Frontend | streetmed-frontend | us-central1 |
| Backend | streetmed-backend | us-central1 |
| Database | Cloud SQL (MySQL 8.0) | us-central1 |

### Deployment Workflow

Deployments are **automatic** when changes are pushed to the `main` branch:

### Monitoring Deployments

```bash
# Check deployment status
gcloud run services list --region=us-central1

# View backend logs
gcloud run services logs read streetmed-backend --region=us-central1 --limit=50

# View frontend logs
gcloud run services logs read streetmed-frontend --region=us-central1 --limit=50
```

---

## 🌿 Git Branching Strategy

> ⚠️ **Important**: The `main` branch is for **production deployment only**.

### Branch Rules

| Branch | Purpose | Deploys to |
|--------|---------|------------|
| `main` | Production-ready code only | Google Cloud Run (automatic) |
| `feature/*` | New features | Local development only |
| `bugfix/*` | Bug fixes | Local development only |
| `hotfix/*` | Urgent production fixes | Merge to main after testing |

### Development Workflow

1. **Create a feature branch** from `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/your-feature-name
   ```

2. **Develop and test locally** using Docker or local setup

3. **Push your feature branch** and create a Pull Request:
   ```bash
   git push origin feature/your-feature-name
   ```

4. **Code review** - Get approval from team members

5. **Merge to main** - This triggers automatic deployment

### Branch Naming Conventions

- `feature/add-user-notifications` - New features
- `bugfix/fix-login-error` - Bug fixes
- `hotfix/critical-security-patch` - Urgent fixes
- `refactor/cleanup-order-service` - Code refactoring

---

## Prerequisites

### For Docker Setup (Recommended for Local Development)
- Docker Desktop (see installation guide below)
- Docker Compose (included with Docker Desktop)

### For Local Development Setup
Please install all required dependencies listed in:
📋 **[Required Dependencies Guide](Requried_dependencies.md)**

---

## Quick Start (Local Development)

You can run the application locally using either **Docker** (recommended) or **local development setup**.

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
npm run dev
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

**Rebuild after code changes:**
```bash
docker-compose up --build
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
npm run dev
```

**Note:** For local setup, ensure MySQL and MongoDB are installed and running. See [Required Dependencies Guide](Required_dependencies.md) for details.

---

## Verify Local Setup

Once everything is running locally, access the application at:

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API (HTTP) | http://localhost:8080 |
| Backend API (HTTPS) | https://localhost:8443 |
| API Documentation | http://localhost:8080/swagger-ui.html |

---

## Environment Configuration

### Frontend Environment Variables

Create `.env.local` for local development:
```env
VITE_BASE_URL=http://localhost:8080
VITE_SECURE_BASE_URL=https://localhost:8443
VITE_ENVIRONMENT=development
```

Production environment is configured via `.env.production`:
```env
VITE_BASE_URL=https://streetmed-backend-900663028964.us-central1.run.app
VITE_ENVIRONMENT=production
```

### Backend Environment Variables

Local development uses `application-local.properties`.

Production environment variables are configured in Google Cloud Run:
- `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `CLIENT_AUTH_KEY`
- `MAIL_USERNAME`, `MAIL_PASSWORD`

---

## Docker Installation Guide

### Windows

1. **Download Docker Desktop for Windows**
   - Visit: https://www.docker.com/products/docker-desktop
   - Requires Windows 10 64-bit: Pro, Enterprise, or Education

2. **Install and restart your computer**

3. **Verify Installation**
   ```bash
   docker --version
   docker-compose --version
   ```

### macOS

1. **Download Docker Desktop for Mac**
   - Visit: https://www.docker.com/products/docker-desktop
   - Choose version for your chip (Apple Silicon or Intel)

2. **Install and launch Docker from Applications**

3. **Verify Installation**
   ```bash
   docker --version
   docker-compose --version
   ```

### Linux (Ubuntu/Debian)

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker

# Verify
docker --version
```

---

## Troubleshooting

### Frontend Cannot Connect to Backend
- Verify backend is running: `docker-compose ps`
- Check backend logs: `docker-compose logs backend`
- Ensure CORS is configured correctly

### HTTPS Certificate Issues (Local Development)
When accessing https://localhost:8443, you may see a certificate warning. This is normal for development:
- Click "Advanced" and proceed (Chrome/Edge)
- Click "Accept the Risk and Continue" (Firefox)

### Production Issues
```bash
# Check service status
gcloud run services describe streetmed-backend --region=us-central1

# View recent logs
gcloud run services logs read streetmed-backend --region=us-central1 --limit=100
```

---

## API Documentation

- **Local**: http://localhost:8080/swagger-ui.html
- **Production**: https://streetmed-backend-900663028964.us-central1.run.app/swagger-ui.html

For detailed API documentation, see [API Documentation](docs/API_readme.md).

---

## Contributing

1. Create a feature branch from `main`
2. Make your changes and test locally
3. Submit a Pull Request
4. Get code review approval
5. Merge to `main` (triggers automatic deployment)

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Frontend | React 18, Vite 7, React Router 7 |
| Backend | Spring Boot 3.4, Java 21 |
| Database | MySQL 8.0 (Cloud SQL), MongoDB |
| Deployment | Google Cloud Run |
| CI/CD | GitHub Actions |
| Security | ECDH Key Exchange, AES-GCM Encryption, BCrypt |

---

**Happy Coding! 🚀**
.
