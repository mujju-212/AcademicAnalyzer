# Academic Analyzer - Setup Guide

Complete guide to clone, configure, and run the Academic Analyzer project.

## Table of Contents
1. [Quick Start](#quick-start)
2. [Detailed Setup](#detailed-setup)
3. [Database Configuration](#database-configuration)
4. [Running the Application](#running-the-application)
5. [Troubleshooting](#troubleshooting)

---

## Quick Start

For experienced developers who want to get started quickly:

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/AcademicAnalyzer.git
cd AcademicAnalyzer

# Setup MySQL database
mysql -u root -p
CREATE DATABASE sms_database;
EXIT;

# Build and run with Maven
mvn clean install
mvn exec:java -Dexec.mainClass="com.sms.login.LoginScreen"
```

---

## Detailed Setup

### Step 1: Install Prerequisites

Before you begin, ensure you have the following installed:

#### 1.1 Install Java JDK 21+
**Windows:**
1. Download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
2. Run the installer
3. Set `JAVA_HOME` environment variable:
   - Open System Properties → Environment Variables
   - Add `JAVA_HOME` = `C:\Program Files\Java\jdk-21`
   - Add to `Path`: `%JAVA_HOME%\bin`
4. Verify: `java -version`

**macOS:**
```bash
brew install openjdk@21
```

**Linux:**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

#### 1.2 Install Apache Maven
**Windows:**
1. Download from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to `C:\Program Files\Apache\maven`
3. Add to `Path`: `C:\Program Files\Apache\maven\bin`
4. Verify: `mvn -version`

**macOS:**
```bash
brew install maven
```

**Linux:**
```bash
sudo apt install maven
```

#### 1.3 Install MySQL 8.0+
**Windows:**
1. Download [MySQL Installer](https://dev.mysql.com/downloads/installer/)
2. Run installer and choose "Developer Default"
3. Set root password during installation
4. Start MySQL Server

**macOS:**
```bash
brew install mysql
brew services start mysql
```

**Linux:**
```bash
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

#### 1.4 Install Git
Download from [Git SCM](https://git-scm.com/downloads) and follow installation instructions.

---

### Step 2: Clone the Repository

```bash
# Using HTTPS
git clone https://github.com/YOUR_USERNAME/AcademicAnalyzer.git

# OR using SSH
git clone git@github.com:YOUR_USERNAME/AcademicAnalyzer.git

# Navigate to project directory
cd AcademicAnalyzer
```

---

### Step 3: Database Configuration

#### 3.1 Create Database
Open MySQL command line or MySQL Workbench:

```sql
-- Create database
CREATE DATABASE sms_database;

-- Use the database
USE sms_database;

-- Verify creation
SHOW DATABASES;
```

#### 3.2 Configure Database Connection
The database connection is configured in:
```
src/com/sms/database/DatabaseConnection.java
```

**Default Settings:**
- **Host:** `localhost`
- **Port:** `3306`
- **Database:** `sms_database`
- **Username:** `root`
- **Password:** (your MySQL root password)

**To change password:**
Edit line in `DatabaseConnection.java`:
```java
private static final String PASSWORD = "your_password_here";
```

#### 3.3 Initialize Database Schema
The application will automatically create required tables on first run. Tables include:
- `users` - User accounts and authentication
- `sections` - Class sections
- `students` - Student information
- `marking_schemes` - Assessment structures
- `student_marks` - Grade records
- And more...

---

### Step 4: Build the Project

#### 4.1 Download Dependencies
Maven will automatically download all required dependencies:

```bash
# Clean previous builds and download dependencies
mvn clean install

# Or just download dependencies
mvn dependency:copy-dependencies -DoutputDirectory=lib
```

This will download approximately 50MB of libraries to the `lib/` directory.

#### 4.2 Compile the Project
```bash
mvn compile
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXX s
```

---

### Step 5: Running the Application

#### Method 1: Using Maven (Recommended)
```bash
mvn exec:java -Dexec.mainClass="com.sms.login.LoginScreen"
```

**For PowerShell (Windows):**
```powershell
mvn exec:java '-Dexec.mainClass=com.sms.login.LoginScreen'
```

#### Method 2: Using Compiled Classes
```bash
# Build JAR
mvn package

# Run JAR
java -jar target/AcademicAnalyzer-1.0-SNAPSHOT.jar
```

#### Method 3: Using IDE
1. Import project as Maven project
2. Wait for dependencies to download
3. Right-click `com.sms.login.LoginScreen.java`
4. Select "Run"

---

## Running the Application

### First Launch

1. **Login Screen** appears
2. Click **"Create Account"** to register a new user
3. Fill in required information:
   - Name
   - Email (for OTP verification)
   - Username
   - Password
   - Security question/answer
4. Enter OTP sent to your email
5. Login with your credentials

### Main Features

After login, you can:
- **Create Sections:** Add new class sections
- **Add Students:** Enroll students in sections
- **Create Marking Schemes:** Define grading structures
- **Enter Marks:** Record student grades
- **View Analytics:** 
  - Grade distribution charts
  - Student performance analysis
  - Section comparisons
  - Statistical insights
- **Export Reports:**
  - Excel spreadsheets
  - PDF documents

---

## Troubleshooting

### Common Issues

#### 1. Maven not recognized
**Error:** `mvn: command not found`

**Solution:**
- Verify Maven is installed: Check installation directory
- Add Maven to PATH environment variable
- Restart terminal/command prompt

#### 2. Java version mismatch
**Error:** `Unsupported class version`

**Solution:**
```bash
# Check Java version
java -version

# Should show version 21 or higher
# If not, install JDK 21 and update JAVA_HOME
```

#### 3. MySQL Connection Failed
**Error:** `Communications link failure` or `Access denied`

**Solution:**
- Verify MySQL is running: `mysql -u root -p`
- Check database exists: `SHOW DATABASES;`
- Verify credentials in `DatabaseConnection.java`
- Check firewall isn't blocking port 3306

#### 4. Dependencies Download Failed
**Error:** `Failed to download artifact`

**Solution:**
```bash
# Clear Maven cache
mvn dependency:purge-local-repository

# Try again
mvn clean install
```

#### 5. Port Already in Use
**Error:** `Address already in use`

**Solution:**
- MySQL default port 3306 may be in use
- Change port in MySQL config or `DatabaseConnection.java`

#### 6. Email OTP Not Received
**Issue:** OTP email not arriving

**Solution:**
- Check spam/junk folder
- Verify email address is correct
- Check internet connection
- EmailJS service may need configuration

#### 7. Build Errors
**Error:** Compilation failures

**Solution:**
```bash
# Clean and rebuild
mvn clean compile

# Skip tests if needed
mvn clean install -DskipTests
```

### Getting Help

If you encounter issues:

1. Check the [Issues](https://github.com/YOUR_USERNAME/AcademicAnalyzer/issues) page
2. Review error logs in terminal
3. Verify all prerequisites are installed correctly
4. Create a new issue with:
   - Error message
   - Steps to reproduce
   - System information (OS, Java version, Maven version)

---

## Development Setup

### IDE Configuration

#### IntelliJ IDEA
1. File → Open → Select project directory
2. Wait for Maven import
3. File → Project Structure → SDK → Select JDK 21
4. Run → Edit Configurations → Add Application
   - Main class: `com.sms.login.LoginScreen`
   - Module: `AcademicAnalyzer`

#### Eclipse
1. File → Import → Maven → Existing Maven Projects
2. Select project directory
3. Right-click project → Properties → Java Build Path → Libraries → Add JDK 21
4. Right-click `LoginScreen.java` → Run As → Java Application

#### VS Code
1. Install Java Extension Pack
2. Open project folder
3. VS Code will auto-detect Maven project
4. Press F5 to run

---

## Project Structure

```
AcademicAnalyzer/
├── src/
│   ├── Main.java
│   └── com/sms/
│       ├── analyzer/      # Analysis logic
│       ├── dao/           # Data Access Objects
│       ├── dashboard/     # Main UI
│       ├── database/      # DB connection
│       ├── login/         # Authentication
│       ├── marking/       # Grading system
│       ├── theme/         # UI theming
│       └── util/          # Utilities
├── lib/                   # Dependencies (auto-generated)
├── bin/                   # Compiled classes
├── pom.xml               # Maven configuration
├── README.md             # Project overview
├── SETUP.md              # This file
└── REQUIREMENTS.md       # System requirements
```

---

## Next Steps

After successful setup:
1. Explore the user interface
2. Create test sections and students
3. Try different marking schemes
4. Generate reports and analytics
5. Customize themes and settings

---

## Additional Resources

- [Java Documentation](https://docs.oracle.com/en/java/)
- [Maven Guide](https://maven.apache.org/guides/)
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [Project Wiki](https://github.com/YOUR_USERNAME/AcademicAnalyzer/wiki)

---

**Happy Coding! 🚀**

For questions or contributions, please visit the [GitHub repository](https://github.com/YOUR_USERNAME/AcademicAnalyzer).
