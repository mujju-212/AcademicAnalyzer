# 📚 Academic Analyzer

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A comprehensive **Student Management System** with powerful academic analysis capabilities. Built with Java Swing and modern UI/UX design, Academic Analyzer helps educators manage student records, track performance, create flexible marking schemes, and generate insightful analytics.

## ✨ Features

### 🔐 Authentication & Security
- **Secure Login System** with encrypted password storage
- **OTP-based Email Verification** for account creation
- **Password Recovery** with security questions
- **Session Management** for secure user access

### 👥 Student Management
- **Student Registration** with comprehensive details
- **Section/Class Organization** for easy grouping
- **Bulk Import/Export** via Excel files
- **Search & Filter** capabilities
- **Student Profile Management**

### 📊 Flexible Marking System
- **Customizable Marking Schemes** with component groups
- **Multiple Assessment Types** (exams, assignments, quizzes, projects)
- **Weighted Grading** with configurable percentages
- **Automatic Grade Calculation** with letter grades
- **Mark Entry Interface** with validation
- **Bonus Marks Support**

### 📈 Advanced Analytics
- **Grade Distribution Charts** (pie charts, bar graphs)
- **Student Performance Tracking** over time
- **Section-wise Comparisons**
- **Statistical Analysis:**
  - Mean, Median, Mode
  - Standard Deviation
  - Percentile Rankings
  - Pass/Fail Rates
- **Top Performers Identification**
- **Performance Trends**

### 📄 Reporting & Export
- **Excel Export** for student records and grades
- **PDF Report Generation** with formatted layouts
- **Custom Report Templates**
- **Printable Grade Sheets**
- **Analytics Dashboards**

### 🎨 Modern UI/UX
- **Neumorphic Design** with smooth shadows and gradients
- **FlatLaf Look and Feel** for modern appearance
- **Dark/Light Theme Support**
- **Responsive Layout** adapting to window size
- **Interactive Charts** with JFreeChart
- **Custom Components** with rounded corners and animations

## 🖼️ Screenshots

### Login Screen
Modern authentication interface with neumorphic design and gradient backgrounds.

### Dashboard
Comprehensive overview with section cards, grade distribution charts, and quick actions.

### Marking Scheme Builder
Flexible interface to create complex grading structures with multiple component groups.

### Analytics Dashboard
Interactive charts showing grade distributions, performance trends, and statistical insights.

## 🚀 Quick Start

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/AcademicAnalyzer.git
cd AcademicAnalyzer

# Setup MySQL database
mysql -u root -p
CREATE DATABASE sms_database;
EXIT;

# Build and run
mvn clean install
mvn exec:java -Dexec.mainClass="com.sms.login.LoginScreen"
```

## 📋 Prerequisites

- **Java JDK 21+** - [Download](https://www.oracle.com/java/technologies/downloads/)
- **Apache Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- **MySQL 8.0+** - [Download](https://dev.mysql.com/downloads/mysql/)
- **Git** - [Download](https://git-scm.com/downloads)

## 🛠️ Installation

For detailed setup instructions, see [SETUP.md](SETUP.md)

### Basic Setup

1. **Install Prerequisites**
   ```bash
   java -version    # Verify Java 21+
   mvn -version     # Verify Maven
   mysql --version  # Verify MySQL
   ```

2. **Clone Repository**
   ```bash
   git clone https://github.com/mujju-212/AcademicAnalyzer.git
   cd AcademicAnalyzer
   ```

3. **Configure Database**
   ```sql
   CREATE DATABASE sms_database;
   ```
   
   Update credentials in `src/com/sms/database/DatabaseConnection.java` if needed.

4. **Build Project**
   ```bash
   mvn clean install
   ```

5. **Run Application**
   ```bash
   mvn exec:java -Dexec.mainClass="com.sms.login.LoginScreen"
   ```

## 📦 Dependencies

All dependencies are automatically managed by Maven. See [REQUIREMENTS.md](REQUIREMENTS.md) for details.

**Key Libraries:**
- FlatLaf 3.6 - Modern Swing Look and Feel
- JFreeChart 1.5.4 - Charts and Graphs
- Apache POI 5.2.3 - Excel Processing
- MySQL Connector/J 8.0.33 - Database Connectivity
- iText PDF 5.5.13.3 - PDF Generation
- JavaMail API 1.6.2 - Email Functionality

## 🏗️ Project Structure

```
AcademicAnalyzer/
├── src/
│   ├── Main.java                    # Alternative entry point
│   └── com/sms/
│       ├── analyzer/                # Analysis logic and algorithms
│       │   ├── SectionAnalyzer.java
│       │   ├── StudentAnalyzer.java
│       │   └── Student.java
│       ├── dao/                     # Data Access Objects
│       │   ├── AnalyzerDAO.java
│       │   ├── SectionDAO.java
│       │   └── StudentDAO.java
│       ├── dashboard/               # Main dashboard UI
│       │   ├── DashboardScreen.java
│       │   ├── DashboardActions.java
│       │   ├── components/          # Reusable UI components
│       │   ├── data/                # Data management
│       │   ├── dialogs/             # Dialog windows
│       │   └── util/                # UI utilities
│       ├── database/                # Database connection
│       │   └── DatabaseConnection.java
│       ├── login/                   # Authentication screens
│       │   ├── LoginScreen.java
│       │   ├── CreateAccountScreen.java
│       │   ├── ForgotPasswordScreen.java
│       │   ├── OTPVerificationScreen.java
│       │   └── ResetPasswordScreen.java
│       ├── marking/                 # Marking system
│       │   ├── dao/                 # Marking DAOs
│       │   ├── dialogs/             # Mark entry dialogs
│       │   ├── models/              # Data models
│       │   ├── panels/              # UI panels
│       │   └── utils/               # Utilities
│       ├── theme/                   # UI theming
│       │   ├── ThemeManager.java
│       │   └── NeumorphicUtils.java
│       └── util/                    # General utilities
│           └── AnalyzerConstants.java
├── lib/                             # Dependencies (auto-downloaded)
├── bin/                             # Compiled classes
├── pom.xml                          # Maven configuration
├── README.md                        # This file
├── SETUP.md                         # Detailed setup guide
└── REQUIREMENTS.md                  # System requirements
```

## 💻 Usage

### First Time Setup

1. **Launch Application**
   ```bash
   mvn exec:java -Dexec.mainClass="com.sms.login.LoginScreen"
   ```

2. **Create Account**
   - Click "Create Account" on login screen
   - Fill in your details
   - Verify email with OTP
   - Set security question/answer

3. **Login**
   - Enter username and password
   - Access the dashboard

### Core Workflows

#### Creating a Section
1. Navigate to Dashboard
2. Click "Create New Section"
3. Enter section details (name, semester, year)
4. Save section

#### Adding Students
1. Select a section
2. Click "Add Student"
3. Enter student information
4. Assign to section
5. Save student

#### Creating Marking Scheme
1. Go to Marking section
2. Click "Create New Scheme"
3. Add component groups (e.g., Exams 60%, Assignments 40%)
4. Add individual components within groups
5. Set weightages and maximum marks
6. Save scheme

#### Entering Marks
1. Select section and marking scheme
2. Click "Enter Marks"
3. Select component
4. Enter marks for each student
5. System auto-calculates totals and grades

#### Viewing Analytics
1. Select section
2. Click "View Analytics"
3. Explore:
   - Grade distribution charts
   - Student rankings
   - Statistical summaries
   - Performance trends

#### Exporting Reports
1. Select data to export
2. Choose format (Excel/PDF)
3. Configure report options
4. Generate and save

## 🎯 Key Capabilities

- **Multi-Section Management:** Handle multiple classes simultaneously
- **Flexible Grading:** Adapt to any grading system or rubric
- **Real-time Calculations:** Instant grade updates and statistics
- **Data Validation:** Prevent invalid mark entries
- **Audit Trail:** Track all changes and modifications
- **Scalable:** Handles hundreds of students efficiently

## 🔧 Configuration

### Database Settings
Edit `src/com/sms/database/DatabaseConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/sms_database";
private static final String USER = "root";
private static final String PASSWORD = "your_password";
```

### Email Settings (OTP)
The application uses EmailJS for OTP delivery. To configure your own:
1. Create account at [EmailJS](https://www.emailjs.com/)
2. Update credentials in relevant classes
3. Configure email templates

### UI Themes
Modify theme settings in `src/com/sms/theme/ThemeManager.java` to customize colors and appearance.

## 🐛 Troubleshooting

### Common Issues

**Application won't start:**
- Verify Java 21+ is installed
- Check MySQL is running
- Ensure database exists

**Database connection errors:**
- Verify MySQL credentials
- Check database exists
- Ensure port 3306 is open

**Dependencies not downloading:**
- Check internet connection
- Clear Maven cache: `mvn dependency:purge-local-repository`
- Try: `mvn clean install -U`

For more troubleshooting, see [SETUP.md](SETUP.md#troubleshooting)

## 📚 Documentation

- **[SETUP.md](SETUP.md)** - Detailed installation and configuration guide
- **[REQUIREMENTS.md](REQUIREMENTS.md)** - Complete system requirements and dependencies
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Guidelines for contributors *(coming soon)*
- **[API Documentation](docs/API.md)** - Code documentation *(coming soon)*

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit your changes** (`git commit -m 'Add some AmazingFeature'`)
4. **Push to the branch** (`git push origin feature/AmazingFeature`)
5. **Open a Pull Request**

Please ensure:
- Code follows existing style conventions
- All tests pass
- Documentation is updated
- Commits are descriptive

## 🛣️ Roadmap

- [ ] **Cloud Integration** - Store data in cloud databases
- [ ] **Mobile App** - Android/iOS companion app
- [ ] **REST API** - Backend API for integrations
- [ ] **Advanced Analytics** - Machine learning predictions
- [ ] **Multi-language Support** - Internationalization
- [ ] **Web Dashboard** - Browser-based interface
- [ ] **Automated Backups** - Scheduled database backups
- [ ] **Parent Portal** - View student progress
- [ ] **Attendance Tracking** - Integrated attendance system

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Authors

- **Mujju** - *Initial work* - [mujju-212](https://github.com/mujju-212)

## 🙏 Acknowledgments

- **FlatLaf** for beautiful modern UI components
- **JFreeChart** for powerful charting capabilities
- **Apache POI** for Excel file handling
- **iText** for PDF generation
- **EmailJS** for email delivery service
- All contributors and users of this project

## 📞 Contact

Have questions or suggestions?

- **Email:** mujju718263@gmail.com
- **GitHub Issues:** [Create an issue](https://github.com/mujju-212/AcademicAnalyzer/issues)
- **Discussions:** [Join discussions](https://github.com/mujju-212/AcademicAnalyzer/discussions)

---

<p align="center">
  <strong>Built with ❤️ for educators and students everywhere</strong>
</p>

<p align="center">
  <sub>Made with Java • Powered by Maven • Styled with FlatLaf</sub>
</p>
