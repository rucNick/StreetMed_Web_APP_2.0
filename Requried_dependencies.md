# Required Dependencies

## Prerequisites

Before setting up the project for **local development**, make sure you have the following dependencies installed.

> **Note**: For production, the application is deployed on Google Cloud Run and these dependencies are handled automatically. This guide is for local development only.

---

## Required Software

### 1. Java Development Kit (JDK) 21+

| Platform | Installation |
|----------|-------------|
| **macOS** | `brew install openjdk@21` |
| **Windows** | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/) |
| **Linux** | `sudo apt install openjdk-21-jdk` |

**Verify installation:**
```bash
java -version
```

### 2. Apache Maven 3.6+

| Platform | Installation |
|----------|-------------|
| **macOS** | `brew install maven` |
| **Windows** | [Apache Maven Download](https://maven.apache.org/download.cgi) |
| **Linux** | `sudo apt install maven` |

**Verify installation:**
```bash
mvn -version
```

### 3. Node.js 20+ and npm

| Platform | Installation |
|----------|-------------|
| **macOS** | `brew install node@20` |
| **Windows** | [Node.js Official Website](https://nodejs.org/) |
| **Linux** | [NodeSource](https://github.com/nodesource/distributions) |

**Verify installation:**
```bash
node --version
npm --version
```

> **Important**: Node.js 20+ is required for Vite 7 compatibility.

### 4. MySQL 8.0+ (Local Development Only)

| Platform | Installation |
|----------|-------------|
| **macOS** | `brew install mysql` |
| **Windows** | [MySQL Installer](https://dev.mysql.com/downloads/installer/) |
| **Linux** | `sudo apt install mysql-server` |
| **Docker** | `docker run --name mysql -e MYSQL_ROOT_PASSWORD=password -p 3306:3306 -d mysql:8.0` |

### 5. MongoDB 6.0+ (Local Development Only)

| Platform | Installation |
|----------|-------------|
| **macOS** | `brew tap mongodb/brew && brew install mongodb-community` |
| **Windows** | [MongoDB Community Edition](https://www.mongodb.com/try/download/community) |
| **Linux** | [MongoDB Installation Guide](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/) |
| **Docker** | `docker run --name mongodb -p 27017:27017 -d mongo:6.0` |

---

## Optional Tools

### Git
```bash
# Verify installation
git --version
```
- **Download**: [Git Official Website](https://git-scm.com/downloads)

### Google Cloud CLI (for deployment management)
```bash
# Install
curl https://sdk.cloud.google.com | bash

# Initialize
gcloud init

# Verify
gcloud --version
```

### IDE Recommendations
- **Backend**: [IntelliJ IDEA](https://www.jetbrains.com/idea/) or [VS Code](https://code.visualstudio.com/)
- **Frontend**: [VS Code](https://code.visualstudio.com/) with React/Vite extensions

---

## Quick Setup

### Using Docker (Recommended)

Docker handles MySQL and MongoDB automatically. Just install:
- [Docker Desktop](https://www.docker.com/products/docker-desktop)

Then run:
```bash
cd Src/Backend
docker-compose up -d
```

### Manual Database Setup

#### MySQL Setup

1. Start MySQL service
2. Connect to MySQL:
   ```bash
   mysql -u root -p
   ```
3. Create database and user:
   ```sql
   CREATE DATABASE streetmed;
   CREATE USER 'streetmed_user'@'localhost' IDENTIFIED BY 'streetmed_password';
   GRANT ALL PRIVILEGES ON streetmed.* TO 'streetmed_user'@'localhost';
   FLUSH PRIVILEGES;
   EXIT;
   ```

#### MongoDB Setup

1. Start MongoDB service
2. Connect to MongoDB:
   ```bash
   mongosh
   ```
3. Create database:
   ```javascript
   use streetmed
   db.createCollection("test")
   ```

---

## Backend Setup

### 1. Navigate to Backend Directory
```bash
cd Src/Backend
```

### 2. Run the Backend (Local Profile)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The backend will start on:
- HTTP: http://localhost:8080
- HTTPS: https://localhost:8443

---

## Frontend Setup

### 1. Navigate to Frontend Directory
```bash
cd Src/Frontend/webapp
```

### 2. Install Dependencies
```bash
npm install
```

### 3. Start Development Server
```bash
npm run dev
```

The frontend will start on http://localhost:3000

---

## Access Points

### Local Development

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Backend API (HTTPS) | https://localhost:8443 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

### Production (Google Cloud Run)

| Service | URL |
|---------|-----|
| Frontend | https://streetmed-frontend-900663028964.us-central1.run.app |
| Backend API | https://streetmed-backend-900663028964.us-central1.run.app |
| Swagger UI | https://streetmed-backend-900663028964.us-central1.run.app/swagger-ui.html |

---

## Troubleshooting

### Java Version Issues
```bash
# Check Java version
java -version

# If wrong version, set JAVA_HOME
export JAVA_HOME=/path/to/jdk-21
```

### Node.js Version Issues
```bash
# Use nvm to manage Node versions
nvm install 20
nvm use 20
```

### Port Already in Use
```bash
# Find process using port
lsof -i :8080
# or on Windows
netstat -ano | findstr :8080

# Kill process
kill -9 <PID>
```

### npm Install Fails
```bash
# Clear npm cache
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

---

**Happy Coding! 🚀**

For questions or support, please refer to the main [README.md](README.md) or contact the development team.