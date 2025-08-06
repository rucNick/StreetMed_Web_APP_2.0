## Prerequisites

Before setting up the project, make sure you have the following dependencies installed:

### Required Software

#### 1. Java Development Kit (JDK) 22+
- **Download**: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/install/)
- **Verify installation**: `java -version`

#### 2. Apache Maven 3.6+
- **Download**: [Apache Maven](https://maven.apache.org/download.cgi)
- **Installation Guide**: [Maven Installation](https://maven.apache.org/install.html)
- **Verify installation**: `mvn -version`

#### 3. Node.js 16+ and npm
- **Download**: [Node.js Official Website](https://nodejs.org/)
- **Verify installation**: `node -version` and `npm -version`

#### 4. MySQL 8.0+
- **macOS**: `brew install mysql` or [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
- **Windows**: [MySQL Installer](https://dev.mysql.com/downloads/installer/)
- **Ubuntu/Debian**: `sudo apt-get install mysql-server`
- **CentOS/RHEL**: `sudo yum install mysql-server`
- **Docker**: `docker run --name mysql -e MYSQL_ROOT_PASSWORD=password -p 3306:3306 -d mysql:8.0`

#### 5. MongoDB 6.0+
- **macOS**: `brew tap mongodb/brew && brew install mongodb-community`
- **Windows**: [MongoDB Community Edition](https://www.mongodb.com/try/download/community)
- **Ubuntu/Debian**: [MongoDB Installation Guide](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/)
- **Docker**: `docker run --name mongodb -p 27017:27017 -d mongo:6.0`

### Optional Tools

#### Git
- **Download**: [Git Official Website](https://git-scm.com/downloads)
- **Verify installation**: `git --version`

#### IDE Recommendations
- **Backend**: [IntelliJ IDEA](https://www.jetbrains.com/idea/) or [Eclipse](https://www.eclipse.org/downloads/)
- **Frontend**: [VS Code](https://code.visualstudio.com/) with React extensions

## Quick Setup

### Database Setup

#### MySQL Setup
1. Start MySQL service
2. Connect to MySQL: `mysql -u root -p`
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
2. Connect to MongoDB: `mongosh`
3. Create database:
   ```javascript
   use streetmed
   db.createCollection("test")
   ```

## Backend Setup

### 1. Navigate to Backend Directory
```bash
cd Src/Backend
```
### 2. Run the Backend

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

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
npm run start
```

### 5. Access the Application

- Frontend: http://localhost:3000
- The application will automatically connect to your local backend


**Happy Coding! 🚀**

For questions or support, please check the troubleshooting guide above or refer to the technical documentation.
