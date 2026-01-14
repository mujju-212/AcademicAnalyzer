# Academic Analyzer - System Requirements

## Prerequisites

### 1. Java Development Kit (JDK)
- **Version Required:** JDK 21 or higher
- **Download:** [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
- **Verify Installation:**
  ```bash
  java -version
  ```

### 2. Apache Maven
- **Version Required:** Maven 3.6.0 or higher
- **Download:** [Apache Maven](https://maven.apache.org/download.cgi)
- **Verify Installation:**
  ```bash
  mvn -version
  ```

### 3. MySQL Database
- **Version Required:** MySQL 8.0 or higher
- **Download:** [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
- **Configuration:**
  - Default Port: 3306
  - Root password required
  - Create database: `sms_database`

### 4. Git (Optional - for cloning)
- **Download:** [Git SCM](https://git-scm.com/downloads)

## Java Dependencies (Managed by Maven)

All the following dependencies will be automatically downloaded by Maven when you build the project:

### Core Framework
- **FlatLaf** (3.6) - Modern look and feel for Swing applications
- **MySQL Connector/J** (8.0.33) - JDBC driver for MySQL

### Data Visualization
- **JFreeChart** (1.5.4) - Charts and graphs generation

### Document Processing
- **Apache POI** (5.2.3) - Excel file creation and manipulation
- **Apache POI OOXML** (5.2.3) - Excel XLSX format support
- **iText PDF** (5.5.13.3) - PDF document generation

### Utilities
- **Apache Commons IO** (2.19.0) - I/O utilities
- **Apache Commons Math3** (3.6.1) - Mathematical and statistical functions
- **JavaMail API** (1.6.2) - Email functionality for OTP verification

### Supporting Libraries
- **XMLBeans** (5.1.1) - XML processing
- **Apache Commons Compress** (1.21) - File compression
- **Apache Commons Collections4** (4.4) - Enhanced collections
- **Apache Commons Codec** (1.15) - Encoding/decoding utilities
- **SparseBitSet** (1.2) - Memory-efficient bit sets
- **Log4j API** (2.18.0) - Logging framework
- **Protobuf Java** (3.21.9) - Protocol buffers
- **Activation** (1.1) - JavaBeans Activation Framework

## System Requirements

### Minimum Hardware
- **RAM:** 4 GB (8 GB recommended)
- **Storage:** 500 MB free disk space
- **Processor:** Dual-core 2.0 GHz or higher
- **Display:** 1280x720 resolution minimum

### Operating System
- **Windows:** Windows 10 or higher
- **macOS:** macOS 10.14 or higher
- **Linux:** Any modern distribution with GUI support

## Network Requirements
- **Internet Connection:** Required for:
  - Initial Maven dependency download
  - Email OTP verification (via EmailJS)
  - MySQL database connectivity (if hosted remotely)

## Database Setup

### MySQL Configuration
1. Install MySQL Server 8.0+
2. Create database:
   ```sql
   CREATE DATABASE sms_database;
   ```
3. Update connection settings in `DatabaseConnection.java` if needed:
   - Host: `localhost`
   - Port: `3306`
   - Username: `root`
   - Password: (your MySQL root password)

## EmailJS Configuration (for OTP)
- The application uses EmailJS for sending OTP verification emails
- Default configuration is included
- For production use, register at [EmailJS](https://www.emailjs.com/) and update credentials

## IDE Recommendations (Optional)
- **IntelliJ IDEA** (Community or Ultimate)
- **Eclipse IDE for Java Developers**
- **NetBeans IDE**
- **VS Code** with Java Extension Pack

## Build Tool
The project uses **Apache Maven** for:
- Dependency management
- Project compilation
- Application execution
- Build automation

All dependencies will be automatically downloaded to `lib/` directory on first build.
