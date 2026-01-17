# AcademicAnalyzer - Complete Codebase Architecture Documentation

**Project:** Academic Performance Management System  
**Language:** Java (Swing GUI)  
**Database:** MySQL  
**Build Tool:** Maven  
**Last Updated:** January 17, 2026

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Project Structure](#project-structure)
3. [Package Architecture](#package-architecture)
4. [Core Components](#core-components)
5. [Database Layer](#database-layer)
6. [UI Layer](#ui-layer)
7. [Business Logic Layer](#business-logic-layer)
8. [Utility Components](#utility-components)
9. [Dependencies & Imports](#dependencies--imports)
10. [Data Flow & Routing](#data-flow--routing)

---

## 1. Project Overview

AcademicAnalyzer is a comprehensive Java Swing desktop application for managing academic performance, student records, marking schemes, and result generation. The system provides:

- **Student Management**: Add, edit, view student records
- **Section Management**: Create sections with custom marking schemes
- **Marks Entry**: Flexible component-based marking system
- **Result Launcher**: Generate and launch student results with JSON export
- **Analytics**: Section-wide and student-wise performance analysis
- **View & Export**: Multi-mode data viewing with PDF/Excel export

**Key Features:**
- Component-based weighted marking system with group selection logic
- Multi-row header support for complex result display
- JSON-based result launching with email notifications
- Dual-mode authentication (login/create account with OTP)
- Section and student analytics with grade distribution

---

## 2. Project Structure

```
AcademicAnalyzer/
├── src/
│   ├── Main.java                           # Application entry point
│   ├── TestEmail.java                      # (Deleted - was test file)
│   └── com/sms/
│       ├── analyzer/                       # Performance analysis components
│       ├── calculation/                    # Calculation engine & models
│       ├── dao/                           # Data Access Objects
│       ├── dashboard/                     # Main dashboard UI
│       ├── database/                      # Database connection singleton
│       ├── login/                         # Authentication screens
│       ├── marking/                       # Marking scheme & entry system
│       ├── resultlauncher/                # Result generation & launch
│       ├── theme/                         # Theme/styling manager
│       ├── util/                          # Utility classes
│       └── viewtool/                      # View selection & export tool
├── lib/                                   # External JAR dependencies
├── docs/                                  # Project documentation
├── result-portal/                         # Flask web app for result viewing
├── bin/                                   # Compiled .class files
└── pom.xml                               # Maven configuration
```

---

## 3. Package Architecture

### 3.1 Package Dependency Hierarchy

```
Main.java
    ↓
login/ (LoginScreen)
    ↓
dashboard/ (DashboardScreen)
    ↓
    ├── analyzer/ (SectionAnalyzer, StudentAnalyzer)
    ├── viewtool/ (ViewSelectionTool)
    ├── resultlauncher/ (ResultLauncher)
    └── marking/ (MarkEntry dialogs)
        ↓
        ├── dao/ (All database operations)
        ├── calculation/ (StudentCalculator, SectionCalculator)
        ├── database/ (DatabaseConnection)
        ├── theme/ (ThemeManager)
        └── util/ (ConfigLoader)
```

**Layered Architecture:**
1. **Presentation Layer**: `login/`, `dashboard/`, `analyzer/`, `viewtool/`, `resultlauncher/`
2. **Business Logic Layer**: `calculation/`, `marking/`
3. **Data Access Layer**: `dao/`, `database/`
4. **Utility Layer**: `util/`, `theme/`

---

## 4. Core Components

### 4.1 Main.java

**Location:** `src/Main.java`  
**Purpose:** Application entry point  
**Imports:**
```java
import com.sms.login.LoginScreen;
import com.sms.dashboard.DashboardScreen;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Initialize Look & Feel (FlatLaf)
- Test database connection
- Launch LoginScreen
- Set application-wide UI properties

**Usage:** Entry point executed by JVM

---

## 5. Database Layer

### 5.1 com.sms.database Package

#### DatabaseConnection.java

**Location:** `src/com/sms/database/DatabaseConnection.java`  
**Purpose:** Singleton database connection manager  
**Pattern:** Singleton  
**Imports:**
```java
import com.sms.util.ConfigLoader;
import java.sql.Connection;
import java.sql.DriverManager;
```

**Responsibilities:**
- Load database credentials from config.properties
- Provide singleton Connection instance
- Handle MySQL driver loading
- Connection pooling management

**Used By (24+ classes):**
- All DAO classes
- All service classes
- All dialogs requiring database access
- Main.java (connection test)

**Key Methods:**
```java
public static Connection getConnection()  // Returns singleton connection
```

**Configuration:**
```properties
db.url=jdbc:mysql://localhost:3306/academic_analyzer
db.username=root
db.password=your_password
```

---

### 5.2 com.sms.dao Package

Data Access Object pattern for database operations.

#### AnalyzerDAO.java

**Location:** `src/com/sms/dao/AnalyzerDAO.java`  
**Purpose:** Student/Section analysis data retrieval  
**Lines:** ~2500  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
import com.sms.analyzer.Student;
```

**Responsibilities:**
- Fetch section analysis data with filters (year, semester, exam types)
- Retrieve student lists per section
- Get component marks for students
- Calculate section statistics
- Generate subject-wise analysis

**Used By:**
- SectionAnalyzer (analysis display)
- StudentAnalyzer (individual student analysis)
- DashboardScreen (student data)
- ViewSelectionTool (section data export)
- ResultLauncher components

**Key Methods:**
```java
public SectionAnalysisData getSectionAnalysisData(int sectionId, Map<String, Set<String>> filters)
public HashMap<String, List<Student>> getSectionStudents()
public Map<Integer, StudentComponentMark> getStudentComponentMarks(int studentId, List<Integer> componentIds)
```

**Nested Classes:**
- `SectionAnalysisData` - Container for analysis results
- `SubjectAnalysis` - Subject-level statistics
- `ComponentAnalysis` - Component-level breakdown
- `StudentComponentMark` - Student mark data

---

#### SectionDAO.java

**Location:** `src/com/sms/dao/SectionDAO.java`  
**Purpose:** Section CRUD operations and marking scheme management  
**Lines:** ~800  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
import com.sms.dao.SectionDAO;
import com.sms.marking.models.*;
import com.sms.marking.dao.MarkingSchemeDAO;
```

**Responsibilities:**
- Create/update/delete sections
- Manage section subjects
- Link marking schemes to sections
- Retrieve section information with filters

**Used By:**
- CreateSectionDialog (section creation)
- DashboardScreen (section listing)
- SectionService (business logic)
- SectionAnalyzer (section info)
- StudentAnalyzer (section validation)

**Key Methods:**
```java
public int createSection(String sectionName, String academicYear, String semester, int capacity)
public List<SectionInfo> getAllSections()
public void createSectionSubjects(int sectionId, List<String> subjects)
public int createSectionWithScheme(String sectionName, ..., MarkingScheme scheme)
```

**Nested Classes:**
- `SectionInfo` - Section metadata container
- `SubjectInfo` - Subject details per section

---

#### StudentDAO.java

**Location:** `src/com/sms/dao/StudentDAO.java`  
**Purpose:** Student CRUD operations  
**Lines:** ~400  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Add students to sections
- Validate roll number uniqueness
- Retrieve student lists by section
- Check duplicate email addresses

**Used By:**
- StudentEntryDialog (add students)
- YearSemesterPanel (student management)
- ViewSelectionTool (student data)
- MarkEntryDialog (student selection)

**Key Methods:**
```java
public boolean addStudent(int sectionId, String rollNumber, String studentName, String email)
public List<StudentInfo> getStudentsBySection(int sectionId)
public boolean isRollNumberExists(int sectionId, String rollNumber)
```

**Nested Classes:**
- `StudentInfo` - Student details container

---

#### SectionEditDAO.java

**Location:** `src/com/sms/dao/SectionEditDAO.java`  
**Purpose:** Section editing and deletion operations  
**Lines:** ~200  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Update section details
- Delete sections (cascade)
- Modify section capacity
- Update academic year/semester

**Used By:**
- SectionCardPanel (edit section)
- YearSemesterPanel (section management)

**Key Methods:**
```java
public boolean updateSection(int sectionId, String newName, String newYear, String newSemester, int newCapacity)
public boolean deleteSection(int sectionId)
```

---

## 6. UI Layer

### 6.1 com.sms.login Package

Authentication and account management screens.

#### LoginScreen.java

**Location:** `src/com/sms/login/LoginScreen.java`  
**Purpose:** Main authentication screen  
**Lines:** ~500  
**Imports:**
```java
import com.sms.dashboard.DashboardScreen;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- User authentication
- Navigate to ForgotPasswordScreen
- Navigate to CreateAccountScreen
- Launch DashboardScreen on success

**Used By:**
- Main.java (application start)
- DashboardScreen (logout action)
- ResultLauncher components (logout navigation)

**Key Features:**
- Modern gradient UI
- Remember me functionality
- Password visibility toggle
- Validation with error messages

---

#### CreateAccountScreen.java

**Location:** `src/com/sms/login/CreateAccountScreen.java`  
**Purpose:** New user registration  
**Lines:** ~850  
**Imports:**
```java
import com.sms.util.ConfigLoader;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- User registration form
- Email validation
- Password strength validation
- Send verification email via EmailJS API

**Used By:**
- LoginScreen (create account link)

**Key Features:**
- Email/password validation
- Institution field
- Terms acceptance checkbox
- EmailJS integration for verification

**Configuration Used:**
```java
EMAILJS_SERVICE_ID
EMAILJS_TEMPLATE_ID
EMAILJS_PUBLIC_KEY
```

---

#### ForgotPasswordScreen.java

**Location:** `src/com/sms/login/ForgotPasswordScreen.java`  
**Purpose:** Password recovery workflow  
**Lines:** ~500  
**Imports:**
```java
import com.sms.util.ConfigLoader;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Email validation
- Generate 6-digit OTP
- Send OTP via EmailJS
- Navigate to OTPVerificationScreen

**Used By:**
- LoginScreen (forgot password link)

**Key Features:**
- Email validation against database
- OTP generation and email sending
- Modern card-based UI

---

#### OTPVerificationScreen.java

**Location:** `src/com/sms/login/OTPVerificationScreen.java`  
**Purpose:** OTP validation for password reset  
**Lines:** ~300  
**Imports:** Standard Java Swing

**Responsibilities:**
- Display OTP input fields
- Validate entered OTP
- Navigate to ResetPasswordScreen on success
- Resend OTP functionality

**Used By:**
- ForgotPasswordScreen (OTP flow)

**Key Features:**
- 6 individual input fields for OTP
- Auto-focus navigation
- Countdown timer for resend
- Verification animation

---

#### ResetPasswordScreen.java

**Location:** `src/com/sms/login/ResetPasswordScreen.java`  
**Purpose:** Password reset after OTP verification  
**Lines:** ~300  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- New password input
- Password confirmation validation
- Update password in database
- Navigate back to LoginScreen

**Used By:**
- OTPVerificationScreen (after successful OTP)

**Key Features:**
- Password visibility toggle
- Password strength indicator
- Confirmation matching validation
- Database update with SHA-256 hashing

---

### 6.2 com.sms.dashboard Package

Main application dashboard and related components.

#### DashboardScreen.java

**Location:** `src/com/sms/dashboard/DashboardScreen.java`  
**Purpose:** Main application hub  
**Lines:** ~600  
**Imports:**
```java
import com.sms.dashboard.data.DashboardDataManager;
import com.sms.dashboard.components.*;
import com.sms.dashboard.dialogs.*;
import com.sms.dashboard.services.*;
import com.sms.dashboard.util.*;
import com.sms.analyzer.*;
import com.sms.viewtool.ViewSelectionTool;
import com.sms.resultlauncher.ResultLauncher;
import com.sms.dao.*;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Central navigation hub
- Panel switching (CardLayout)
- Launch dialogs and tools
- Manage sidebar navigation
- Data refresh coordination

**Used By:**
- LoginScreen (successful login)
- Main.java (direct launch for testing)

**Key Panels Managed:**
1. Year/Semester Panel (default view)
2. Section Analyzer
3. Student Analyzer
4. View Data Tool
5. Result Launcher

**Key Methods:**
```java
public void showYearSemesterPanel()
public void showSectionAnalyzer()
public void showStudentAnalyzer()
public void showViewDataPanel()
public void showResultLauncherPanel()
```

**CardLayout Views:**
```java
private static final String YEAR_SEMESTER_VIEW = "yearSemester";
private static final String SECTION_ANALYZER_VIEW = "sectionAnalyzer";
private static final String STUDENT_ANALYZER_VIEW = "studentAnalyzer";
private static final String VIEW_DATA_VIEW = "viewData";
private static final String RESULT_LAUNCHER_VIEW = "resultLauncher";
```

---

#### 6.2.1 Dashboard Components

##### SidebarPanel.java

**Location:** `src/com/sms/dashboard/components/SidebarPanel.java`  
**Purpose:** Navigation sidebar with menu buttons  
**Lines:** ~150  
**Imports:**
```java
import com.sms.dashboard.DashboardActions;
```

**Responsibilities:**
- Render vertical navigation menu
- Handle navigation button clicks
- Highlight active section
- Delegate actions to DashboardActions interface

**Used By:**
- DashboardScreen (main navigation)

**Key Features:**
- Icon-based menu items
- Active state highlighting
- Hover effects
- Logout button at bottom

---

##### YearSemesterPanel.java

**Location:** `src/com/sms/dashboard/components/YearSemesterPanel.java`  
**Purpose:** Section management dashboard (default view)  
**Lines:** ~400  
**Imports:**
```java
import com.sms.analyzer.SectionAnalyzer;
import com.sms.analyzer.Student;
import com.sms.dao.SectionDAO.SectionInfo;
import com.sms.dao.SectionEditDAO;
import com.sms.dao.StudentDAO;
```

**Responsibilities:**
- Display sections grouped by year/semester
- Create new sections
- Add students to sections
- Launch section analyzer
- Edit/delete sections

**Used By:**
- DashboardScreen (default panel)

**Key Features:**
- Filterable section cards
- Year/semester grouping
- Student count display
- Quick actions: Add Student, View Section, Edit, Delete

**Key Methods:**
```java
private void loadSections()
private void createSectionCard(SectionInfo section)
private void handleAddStudent(SectionInfo section)
private void handleViewSection(SectionInfo section)
private void handleDeleteSection(SectionInfo section)
```

---

##### SectionCardPanel.java

**Location:** `src/com/sms/dashboard/components/SectionCardPanel.java`  
**Purpose:** Individual section card display  
**Lines:** ~200  
**Imports:**
```java
import com.sms.dao.SectionEditDAO;
import com.sms.dashboard.dialogs.CreateSectionDialog;
```

**Responsibilities:**
- Display section information card
- Handle edit button
- Handle delete button
- Show section capacity and enrollment

**Used By:**
- YearSemesterPanel (section listing)

**Key Features:**
- Card-based design
- Student count vs capacity
- Edit/delete actions
- Hover animations

---

##### GradeDistributionPanel.java

**Location:** `src/com/sms/dashboard/components/GradeDistributionPanel.java`  
**Purpose:** Visualize grade distribution for a section  
**Lines:** ~300  
**Imports:**
```java
import javax.swing.*;
import java.awt.*;
```

**Responsibilities:**
- Display grade distribution bar chart
- Show percentage breakdown (A+, A, B+, B, C, D, F)
- Animate bar rendering
- Color-code grades

**Used By:**
- DashboardScreen (analytics view)

**Key Features:**
- Custom bar chart rendering
- Gradient colors for grades
- Percentage labels
- Responsive layout

---

#### 6.2.2 Dashboard Dialogs

##### CreateSectionDialog.java

**Location:** `src/com/sms/dashboard/dialogs/CreateSectionDialog.java`  
**Purpose:** Section creation with marking scheme builder  
**Lines:** ~2600  
**Imports:**
```java
import com.sms.dao.SectionDAO;
import com.sms.database.DatabaseConnection;
import com.sms.theme.ThemeManager;
import com.sms.marking.models.*;
```

**Responsibilities:**
- Section metadata input (name, year, semester, capacity)
- Subject selection
- Marking scheme configuration
- Component group management (Best Of, All Required, Weighted)
- Exam type assignment
- Weight distribution validation
- Save section with complete marking scheme

**Used By:**
- DashboardScreen (Create Section action)
- SectionCardPanel (Edit section)

**Key Features:**
- Multi-step wizard interface
- Template selector (10 predefined schemes)
- Component group builder with selection logic
- Real-time weight calculation
- Validation with error reporting
- Scheme preview panel

**Key Methods:**
```java
private void buildMarkingScheme()
private void addComponentGroup(String groupType)
private void validateWeights()
private void saveSection()
```

**Component Group Types:**
1. **Best Of (n)**: Select best n components from group
2. **All Required**: Include all components
3. **Weighted**: Custom weight distribution

---

##### CreateSectionPanel.java

**Location:** `src/com/sms/dashboard/dialogs/CreateSectionPanel.java`  
**Purpose:** Alternative panel-based section creator  
**Lines:** ~1200  
**Imports:**
```java
import com.sms.dao.SectionDAO;
import com.sms.database.DatabaseConnection;
import com.sms.dashboard.constants.DashboardConstants;
import com.sms.dashboard.util.UIComponentFactory;
import com.sms.theme.ThemeManager;
import com.sms.marking.models.*;
```

**Responsibilities:**
- Similar to CreateSectionDialog but embedded panel
- Section creation workflow
- Marking scheme configuration
- Template-based quick setup

**Used By:**
- DashboardScreen (alternative section creation UI)

**Key Features:**
- Embedded panel design
- Template library
- Component-based marking
- Validation and preview

---

##### StudentEntryDialog.java

**Location:** `src/com/sms/dashboard/dialogs/StudentEntryDialog.java`  
**Purpose:** Add students to sections  
**Lines:** ~1100  
**Imports:**
```java
import com.sms.dao.SectionDAO;
import com.sms.dao.StudentDAO;
import com.sms.dashboard.data.DashboardDataManager;
```

**Responsibilities:**
- Display section information
- Student input form (roll number, name, email)
- Batch student entry
- Duplicate validation
- Student list display with edit/delete

**Used By:**
- YearSemesterPanel (Add Student action)
- DashboardScreen (direct student addition)

**Key Features:**
- Real-time duplicate checking
- Email validation
- Student list management
- Bulk entry support
- Import from CSV (planned)

**Key Methods:**
```java
private void addStudent()
private void loadExistingStudents()
private void deleteStudent(int studentId)
private boolean validateInput()
```

---

##### MarkEntryDialog.java

**Location:** `src/com/sms/dashboard/dialogs/MarkEntryDialog.java`  
**Purpose:** Enter marks for components  
**Lines:** ~800  
**Imports:**
```java
import com.sms.dao.SectionDAO;
import com.sms.dao.StudentDAO;
import com.sms.database.DatabaseConnection;
import com.sms.theme.ThemeManager;
```

**Responsibilities:**
- Select section and subject
- Display component list
- Enter marks for each student
- Validate marks against max marks
- Save marks to database
- Show calculation results

**Used By:**
- DashboardScreen (Mark Entry action)

**Key Features:**
- Subject-wise component filtering
- Student-wise mark entry
- Max marks validation
- Passing marks highlighting
- Bulk save operation

**Key Methods:**
```java
private void loadComponents()
private void loadStudents()
private void saveMarks()
private void calculateTotals()
```

---

#### 6.2.3 Dashboard Services

##### SectionService.java

**Location:** `src/com/sms/dashboard/services/SectionService.java`  
**Purpose:** Business logic for section operations  
**Lines:** ~200  
**Imports:**
```java
import com.sms.dao.SectionDAO;
import com.sms.dao.SectionDAO.SectionInfo;
import com.sms.dao.SectionDAO.SubjectInfo;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Section creation validation
- Subject management
- Section-subject association
- Section metadata retrieval

**Used By:**
- CreateSectionDialog
- YearSemesterPanel

**Key Methods:**
```java
public boolean createSection(SectionInfo info)
public List<SubjectInfo> getSectionSubjects(int sectionId)
public boolean updateSection(SectionInfo info)
```

---

##### StudentService.java

**Location:** `src/com/sms/dashboard/services/StudentService.java`  
**Purpose:** Student management business logic  
**Lines:** ~150  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Student enrollment validation
- Duplicate checking
- Student information updates
- Email uniqueness validation

**Used By:**
- StudentEntryDialog
- YearSemesterPanel

---

##### AnalyticsService.java

**Location:** `src/com/sms/dashboard/services/AnalyticsService.java`  
**Purpose:** Analytics and reporting calculations  
**Lines:** ~300  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Calculate section statistics
- Generate grade distribution
- Performance trend analysis
- Top/bottom performers identification

**Used By:**
- DashboardScreen (analytics display)
- GradeDistributionPanel

**Key Methods:**
```java
public Map<String, Integer> getGradeDistribution(int sectionId)
public double getSectionAverage(int sectionId)
public List<StudentPerformance> getTopPerformers(int sectionId, int limit)
```

---

#### 6.2.4 Dashboard Utilities

##### UIComponentFactory.java

**Location:** `src/com/sms/dashboard/util/UIComponentFactory.java`  
**Purpose:** Factory for common UI components  
**Lines:** ~400  
**Imports:**
```java
import javax.swing.*;
import java.awt.*;
```

**Responsibilities:**
- Create styled buttons
- Generate form fields
- Build card panels
- Produce consistent UI elements

**Used By:**
- All dashboard dialogs
- CreateSectionPanel
- DashboardScreen

**Key Methods:**
```java
public static JButton createPrimaryButton(String text)
public static JButton createSecondaryButton(String text)
public static JPanel createCard()
public static JLabel createLabel(String text, Font font, Color color)
```

---

##### DashboardErrorHandler.java

**Location:** `src/com/sms/dashboard/util/DashboardErrorHandler.java`  
**Purpose:** Centralized error handling and user notification  
**Lines:** ~200  
**Imports:**
```java
import javax.swing.*;
```

**Responsibilities:**
- Display error dialogs
- Log errors to file
- Show validation messages
- Handle exceptions gracefully

**Used By:**
- DashboardScreen
- All dashboard dialogs

**Key Methods:**
```java
public static void showError(Component parent, String message)
public static void showWarning(Component parent, String message)
public static void showSuccess(Component parent, String message)
public static void logError(Exception e)
```

---

##### BackgroundTaskUtil.java

**Location:** `src/com/sms/dashboard/util/BackgroundTaskUtil.java`  
**Purpose:** Execute long-running tasks in background  
**Lines:** ~150  
**Imports:**
```java
import javax.swing.SwingWorker;
```

**Responsibilities:**
- Run database operations in background
- Show progress indicators
- Handle task completion callbacks
- Prevent UI freezing

**Used By:**
- DashboardScreen
- All data-loading operations

**Key Methods:**
```java
public static <T> void execute(Callable<T> task, Consumer<T> onComplete)
public static void executeWithProgress(Callable<Void> task, JProgressBar progressBar)
```

---

##### DashboardDataManager.java

**Location:** `src/com/sms/dashboard/data/DashboardDataManager.java`  
**Purpose:** Cached data manager for dashboard  
**Lines:** ~250  
**Imports:**
```java
import com.sms.analyzer.Student;
import com.sms.dao.SectionDAO;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Cache section-student mappings
- Refresh data on demand
- Provide quick data access
- Reduce database queries

**Used By:**
- DashboardScreen (data provider)
- StudentEntryDialog

**Key Methods:**
```java
public HashMap<String, List<Student>> getSectionStudents()
public void refreshData()
public List<Student> getStudentsBySection(String sectionName)
```

---

##### DashboardConstants.java

**Location:** `src/com/sms/dashboard/constants/DashboardConstants.java`  
**Purpose:** Application-wide constants  
**Lines:** ~100  
**Imports:** None

**Responsibilities:**
- Define color schemes
- Font configurations
- Size constants
- Grade thresholds

**Used By:**
- All dashboard components
- CreateSectionPanel

**Key Constants:**
```java
public static final Color PRIMARY_COLOR = new Color(66, 133, 244);
public static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 24);
public static final int CARD_WIDTH = 300;
public static final double[] GRADE_THRESHOLDS = {90, 80, 70, 60, 50, 40};
```

---

## 7. Analyzer Package

### 7.1 com.sms.analyzer Package

Performance analysis and visualization components.

#### Student.java

**Location:** `src/com/sms/analyzer/Student.java`  
**Purpose:** Student data model  
**Lines:** ~150  
**Imports:** None

**Responsibilities:**
- Store student information
- Getters/setters for all fields
- Comparable implementation for sorting

**Used By (10+ classes):**
- AnalyzerDAO
- DashboardScreen
- YearSemesterPanel
- ViewSelectionTool
- ResultLauncher components
- PDFExporter, ReportPrinter (deleted)

**Key Fields:**
```java
private int id;
private String rollNumber;
private String name;
private String section;
private String email;
```

---

#### SectionAnalyzer.java

**Location:** `src/com/sms/analyzer/SectionAnalyzer.java`  
**Purpose:** Section-wide performance analysis display  
**Lines:** ~2000  
**Imports:**
```java
import com.sms.dao.AnalyzerDAO;
import com.sms.dao.SectionDAO;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Display section statistics (average, pass %, distribution)
- Show subject-wise analysis
- Filter by year, semester, exam types
- Export analysis to PDF/Excel
- Visualize grade distribution
- Show component-wise breakdown

**Used By:**
- DashboardScreen (Section Analyzer view)
- YearSemesterPanel (View Section action)

**Key Features:**
- Multi-filter support (year, semester, exam types)
- Dynamic table generation
- Color-coded performance indicators
- Export to PDF (A4 landscape)
- Subject cards with statistics

**Key Methods:**
```java
private void loadAnalysisData()
private JPanel createSectionResultAnalysis(SectionAnalysisData data)
private JPanel createSubjectTable(List<SubjectAnalysis> subjects)
private void exportToPDF()
```

**Filter Options:**
- Academic Year (2023-24, 2024-25, etc.)
- Semester (Odd, Even)
- Exam Types (Mid-term, End-term, Quiz, Assignment)

---

#### StudentAnalyzer.java

**Location:** `src/com/sms/analyzer/StudentAnalyzer.java`  
**Purpose:** Individual student performance analysis  
**Lines:** ~800  
**Imports:**
```java
import com.sms.analyzer.SectionAnalyzer;
import com.sms.dao.AnalyzerDAO;
import com.sms.dao.SectionDAO;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Display student report card
- Subject-wise marks breakdown
- Component-wise performance
- Grade calculation
- CGPA/percentage display
- Export student report

**Used By:**
- DashboardScreen (Student Analyzer view)

**Key Features:**
- Student selection dropdown
- Subject cards with grades
- Component marks table
- Overall performance summary
- Print/export functionality

**Key Methods:**
```java
private void loadStudentData()
private void displayStudentReport()
private void calculateGrades()
private void exportReport()
```

---

## 8. Calculation Package

### 8.1 com.sms.calculation Package

Core calculation engine for marks, grades, and statistics.

#### StudentCalculator.java

**Location:** `src/com/sms/calculation/StudentCalculator.java`  
**Purpose:** Calculate individual student results  
**Lines:** ~350  
**Imports:**
```java
import com.sms.calculation.models.*;
```

**Responsibilities:**
- Calculate total marks from components
- Apply component group selection logic (Best Of, All Required)
- Calculate weighted averages
- Determine pass/fail status
- Compute percentage and grade
- Apply group selection rules

**Used By:**
- ResultPreviewDialog
- ResultLauncherDAO
- Any result calculation workflow

**Key Methods:**
```java
public CalculationResult calculateStudentResult(int studentId, int subjectId, List<Component> components)
private double applyWeights(List<Component> components, ComponentWeightManager weights)
private List<Component> applyGroupSelection(ComponentGroup group)
```

**Calculation Flow:**
1. Load student component marks
2. Apply group selection logic
3. Calculate weighted totals
4. Determine pass/fail
5. Calculate percentage
6. Assign grade

---

#### SectionCalculator.java

**Location:** `src/com/sms/calculation/SectionCalculator.java`  
**Purpose:** Calculate section-wide statistics  
**Lines:** ~250  
**Imports:**
```java
import com.sms.calculation.models.*;
```

**Responsibilities:**
- Calculate section average
- Determine pass percentage
- Generate grade distribution
- Identify top/bottom performers
- Calculate standard deviation
- Subject-wise analysis
- Component-wise analysis

**Used By:**
- SectionAnalyzer
- AnalyticsService

**Key Methods:**
```java
public SectionResult calculateSectionStatistics(List<CalculationResult> studentResults)
public Map<String, SubjectAnalysis> calculateSubjectWiseAnalysis(Map<String, List<CalculationResult>> subjectResults)
public Map<String, ComponentAnalysis> calculateComponentWiseAnalysis(List<Component> components)
```

---

#### CalculationUtils.java

**Location:** `src/com/sms/calculation/CalculationUtils.java`  
**Purpose:** Utility methods for calculations  
**Lines:** ~100  
**Imports:**
```java
import com.sms.calculation.models.Component;
```

**Responsibilities:**
- Round decimals to specified places
- Calculate percentages
- Determine grades from percentages
- Validation helpers

**Used By:**
- StudentCalculator
- SectionCalculator

**Key Methods:**
```java
public static double roundToDecimalPlaces(double value, int places)
public static double calculatePercentage(double obtained, double total)
public static String getGrade(double percentage)
```

**Grade Scale:**
```
90-100: A+
80-89:  A
70-79:  B+
60-69:  B
50-59:  C
40-49:  D
<40:    F
```

---

#### ComponentWeightManager.java

**Location:** `src/com/sms/calculation/ComponentWeightManager.java`  
**Purpose:** Manage component weight distribution  
**Lines:** ~150  
**Imports:**
```java
import com.sms.calculation.models.Component;
import com.sms.calculation.models.ComponentGroup;
```

**Responsibilities:**
- Store weight mappings
- Validate weight totals (must sum to 100%)
- Apply weights during calculation
- Handle group-level weights

**Used By:**
- StudentCalculator (8 references)

**Key Methods:**
```java
public void setWeight(int componentId, double weight)
public double getWeight(int componentId)
public boolean validateWeights()
public double getTotalWeight()
```

---

#### GroupSelectionLogic.java

**Location:** `src/com/sms/calculation/GroupSelectionLogic.java`  
**Purpose:** Apply component group selection rules  
**Lines:** ~200  
**Imports:**
```java
import com.sms.calculation.models.Component;
import com.sms.calculation.models.ComponentGroup;
```

**Responsibilities:**
- Implement "Best Of n" logic (select top n components)
- Implement "All Required" logic (include all)
- Sort components by marks
- Select applicable components for calculation

**Used By:**
- StudentCalculator (2 references)

**Key Methods:**
```java
public static List<Component> applyGroupSelection(ComponentGroup group)
public static double calculateGroupScore(ComponentGroup group)
```

**Selection Rules:**
- **Best Of 3**: Takes top 3 components by marks
- **All Required**: Includes all components
- **Weighted**: Custom distribution

---

### 8.2 Calculation Models

#### Component.java

**Location:** `src/com/sms/calculation/models/Component.java`  
**Purpose:** Component data model  
**Lines:** ~200  
**Imports:** None

**Responsibilities:**
- Store component metadata (id, name, type, max marks)
- Store student marks (obtained, passing marks)
- Calculate percentage
- Determine pass/fail status

**Used By (15+ classes):**
- ResultLauncher (all files)
- StudentCalculator
- FlexibleMarkEntryDialog
- And more...

**Key Fields:**
```java
private int id;
private String name;
private String examType;
private double maxMarks;
private double obtainedMarks;
private double passingMarks;
private double weight;
```

---

#### CalculationResult.java

**Location:** `src/com/sms/calculation/models/CalculationResult.java`  
**Purpose:** Result calculation output model  
**Lines:** ~250  
**Imports:** None

**Responsibilities:**
- Store calculation results
- Total obtained/possible marks
- Percentage and grade
- Pass/fail status
- Component breakdown
- Validation messages

**Used By:**
- StudentCalculator
- ResultPreviewDialog
- ResultLauncherDAO

**Key Fields:**
```java
private double totalObtained;
private double totalPossible;
private double percentage;
private String grade;
private boolean passed;
private List<Component> includedComponents;
private List<String> validationMessages;
```

---

#### SubjectAnalysis.java

**Location:** `src/com/sms/calculation/models/SubjectAnalysis.java`  
**Purpose:** Subject-level analysis model  
**Lines:** ~150  
**Imports:** None

**Responsibilities:**
- Store subject statistics
- Average, highest, lowest marks
- Pass percentage
- Component breakdown

**Used By:**
- SectionCalculator (20+ references)
- AnalyzerDAO

**Key Fields:**
```java
private String subjectName;
private double averagePercentage;
private double highestPercentage;
private double lowestPercentage;
private int totalStudents;
private int passedStudents;
```

---

#### SectionResult.java

**Location:** `src/com/sms/calculation/models/SectionResult.java`  
**Purpose:** Section-level statistics model  
**Lines:** ~200  
**Imports:** None

**Responsibilities:**
- Store section-wide statistics
- Grade distribution
- Top/bottom performers
- Standard deviation

**Used By:**
- SectionCalculator (20+ references)
- SectionAnalyzer

**Key Fields:**
```java
private int totalStudents;
private int passedStudents;
private double passPercentage;
private double averagePercentage;
private Map<String, Integer> gradeDistribution;
private List<String> topPerformers;
```

---

#### ComponentGroup.java

**Location:** `src/com/sms/calculation/models/ComponentGroup.java`  
**Purpose:** Component grouping model for selection logic  
**Lines:** ~150  
**Imports:** None

**Responsibilities:**
- Store group metadata (name, type, selection rule)
- Store components in group
- Calculate group marks with selection applied

**Used By:**
- ComponentWeightManager
- GroupSelectionLogic

**Key Fields:**
```java
private String groupName;
private String selectionType; // "BEST_OF", "ALL_REQUIRED", "WEIGHTED"
private int selectionCount; // For BEST_OF
private List<Component> components;
```

---

#### ComponentAnalysis.java

**Location:** `src/com/sms/calculation/models/ComponentAnalysis.java`  
**Purpose:** Component-level analysis model  
**Lines:** ~120  
**Imports:** None

**Responsibilities:**
- Store component statistics
- Average marks
- Pass rate
- Distribution

**Used By:**
- SectionCalculator (9 references)

**Key Fields:**
```java
private String componentName;
private double averageMarks;
private double maxMarks;
private int passedCount;
private int failedCount;
```

---

#### ValidationResult.java

**Location:** `src/com/sms/calculation/models/ValidationResult.java`  
**Purpose:** Validation result container  
**Lines:** ~80  
**Imports:** None

**Responsibilities:**
- Store validation status
- Collect error messages
- Provide validation summary

**Used By:**
- CalculationResult
- Validation operations

**Note:** Duplicate exists in `marking.models` package - both are used.

**Key Fields:**
```java
private boolean isValid;
private String message;
private List<String> errors;
```

---

## 9. Marking Package

### 9.1 com.sms.marking Package

Marking scheme configuration and mark entry system.

#### 9.1.1 Marking DAOs

##### MarkingSchemeDAO.java

**Location:** `src/com/sms/marking/dao/MarkingSchemeDAO.java`  
**Purpose:** Marking scheme CRUD operations  
**Lines:** ~600  
**Imports:**
```java
import com.sms.marking.models.*;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Save marking schemes to database
- Load schemes for sections
- Store component groups
- Store marking components
- Link schemes to sections/subjects

**Used By (7 instantiations):**
- SectionDAO (2)
- MarkCalculator (1)
- FlexibleMarkEntryDialog (1)
- StudentComponentMarkDAO (1)

**Key Methods:**
```java
public int createMarkingScheme(MarkingScheme scheme)
public MarkingScheme getMarkingSchemeForSection(int sectionId, int subjectId)
public void saveComponentGroups(int schemeId, List<ComponentGroup> groups)
public void saveComponents(int groupId, List<MarkingComponent> components)
```

**Database Tables:**
- marking_schemes
- component_groups
- marking_components
- section_marking_schemes

---

##### StudentComponentMarkDAO.java

**Location:** `src/com/sms/marking/dao/StudentComponentMarkDAO.java`  
**Purpose:** Student marks CRUD operations  
**Lines:** ~300  
**Imports:**
```java
import com.sms.marking.models.*;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- Save student marks for components
- Retrieve marks for calculations
- Update marks
- Bulk mark operations

**Used By:**
- MarkCalculator
- FlexibleMarkEntryDialog

**Key Methods:**
```java
public void saveStudentMark(StudentComponentMark mark)
public List<StudentComponentMark> getStudentMarks(int studentId, int subjectId)
public void bulkSaveMarks(List<StudentComponentMark> marks)
```

---

#### 9.1.2 Marking Dialogs

##### FlexibleMarkEntryDialog.java

**Location:** `src/com/sms/marking/dialogs/FlexibleMarkEntryDialog.java`  
**Purpose:** Component-based mark entry interface  
**Lines:** ~700  
**Imports:**
```java
import com.sms.marking.models.*;
import com.sms.marking.utils.MarkCalculator;
import com.sms.marking.dao.*;
import com.sms.database.DatabaseConnection;
import com.sms.theme.ThemeManager;
```

**Responsibilities:**
- Display marking scheme components
- Student-wise mark entry
- Validate marks (max marks, passing marks)
- Calculate totals and percentages
- Save marks to database
- Show real-time results

**Used By:**
- Mark entry workflows (needs integration)

**Key Features:**
- Component list display
- Student table with mark entry
- Real-time validation
- Calculation preview
- Bulk save

**Key Methods:**
```java
private void loadMarkingScheme()
private void loadStudents()
private void saveAllMarks()
private void calculateResults()
```

---

##### ComponentGroupDialog.java

**Location:** `src/com/sms/marking/dialogs/ComponentGroupDialog.java`  
**Purpose:** Create/edit component groups  
**Lines:** ~250  
**Imports:**
```java
import com.sms.marking.models.*;
import com.sms.theme.ThemeManager;
```

**Responsibilities:**
- Component group creation
- Set selection type (Best Of, All Required, Weighted)
- Add components to group
- Configure group parameters

**Used By:**
- CreateSectionDialog (group builder)

**Key Features:**
- Group type selector
- Component list builder
- Selection count input (for Best Of)
- Validation

---

#### 9.1.3 Marking Panels

##### SchemePreviewPanel.java

**Location:** `src/com/sms/marking/panels/SchemePreviewPanel.java`  
**Purpose:** Preview marking scheme before saving  
**Lines:** ~120  
**Imports:**
```java
import com.sms.marking.models.*;
import com.sms.theme.ThemeManager;
```

**Responsibilities:**
- Display scheme structure
- Show component groups
- List components with weights
- Validate scheme

**Used By:**
- CreateSectionDialog

**Key Features:**
- Tree-like hierarchy display
- Weight summaries
- Validation status indicator

---

##### ComponentGroupPanel.java

**Location:** `src/com/sms/marking/panels/ComponentGroupPanel.java`  
**Purpose:** Display individual component group  
**Lines:** ~150  
**Imports:**
```java
import com.sms.marking.models.*;
import com.sms.theme.ThemeManager;
```

**Responsibilities:**
- Render group information
- Show component list
- Display selection rules
- Edit/delete actions

**Used By:**
- SchemePreviewPanel
- CreateSectionDialog

---

#### 9.1.4 Marking Models

##### MarkingScheme.java

**Location:** `src/com/sms/marking/models/MarkingScheme.java`  
**Purpose:** Marking scheme model  
**Lines:** ~300  
**Imports:** None

**Responsibilities:**
- Store scheme metadata
- Store component groups
- Validate scheme
- Calculate total weights

**Used By:**
- CreateSectionDialog
- CreateSectionPanel
- SectionDAO
- All marking scheme operations

**Key Fields:**
```java
private int id;
private String schemeName;
private String description;
private int sectionId;
private int subjectId;
private List<ComponentGroup> componentGroups;
```

**Key Methods:**
```java
public ValidationResult validate()
public double getTotalWeight()
public List<MarkingComponent> getAllComponents()
```

---

##### ComponentGroup.java

**Location:** `src/com/sms/marking/models/ComponentGroup.java`  
**Purpose:** Component group model  
**Lines:** ~200  
**Imports:** None

**Responsibilities:**
- Store group configuration
- Store components
- Calculate group marks
- Apply selection logic

**Used By:**
- CreateSectionDialog
- CreateSectionPanel
- ComponentWeightManager
- GroupSelectionLogic

**Key Fields:**
```java
private int id;
private String groupName;
private String selectionType;
private int bestOfCount;
private List<MarkingComponent> components;
```

---

##### MarkingComponent.java

**Location:** `src/com/sms/marking/models/MarkingComponent.java`  
**Purpose:** Individual marking component model  
**Lines:** ~200  
**Imports:** None

**Responsibilities:**
- Store component details
- Weight, max marks, passing marks
- Exam type association

**Used By:**
- CreateSectionDialog
- CreateSectionPanel
- SectionDAO
- All component operations

**Key Fields:**
```java
private int id;
private String componentName;
private String examType;
private double weight;
private double maxMarks;
private double passingMarks;
```

---

##### StudentComponentMark.java

**Location:** `src/com/sms/marking/models/StudentComponentMark.java`  
**Purpose:** Student mark record  
**Lines:** ~150  
**Imports:** None

**Responsibilities:**
- Store student-component mark
- Timestamp tracking
- Grading metadata

**Used By:**
- MarkCalculator
- FlexibleMarkEntryDialog

**Key Fields:**
```java
private int id;
private int studentId;
private int componentId;
private double marksObtained;
private Date enteredDate;
```

---

##### MarkingTemplate.java

**Location:** `src/com/sms/marking/models/MarkingTemplate.java`  
**Purpose:** Template model for quick scheme creation  
**Lines:** ~100  
**Imports:** None

**Responsibilities:**
- Store template metadata
- Provide predefined schemes

**Used By:**
- MarkingTemplateDAO (unused DAO)

**Status:** DAO not integrated, model exists but unused

---

##### ValidationResult.java (marking)

**Location:** `src/com/sms/marking/models/ValidationResult.java`  
**Purpose:** Validation results for marking schemes  
**Lines:** ~80  
**Imports:** None

**Responsibilities:**
- Store validation status
- Collect scheme errors
- Weight validation

**Used By:**
- CreateSectionDialog
- CreateSectionPanel
- MarkingScheme.validate()

**Note:** Duplicate of `calculation.models.ValidationResult` - both actively used

---

#### 9.1.5 Marking Utils

##### MarkCalculator.java

**Location:** `src/com/sms/marking/utils/MarkCalculator.java`  
**Purpose:** Calculate marks for flexible marking schemes  
**Lines:** ~200  
**Imports:**
```java
import com.sms.marking.models.*;
import com.sms.marking.dao.*;
```

**Responsibilities:**
- Load student marks
- Calculate totals
- Apply scheme rules
- Generate calculation results

**Used By:**
- FlexibleMarkEntryDialog (4 instantiations)

**Key Methods:**
```java
public CalculationResult calculateForStudent(int studentId, int subjectId)
```

---

## 10. Result Launcher Package

### 10.1 com.sms.resultlauncher Package

Result generation, launching, and student notification system with JSON export.

#### ResultLauncher.java

**Location:** `src/com/sms/resultlauncher/ResultLauncher.java`  
**Purpose:** Main result launcher interface  
**Lines:** ~500  
**Imports:**
```java
import com.sms.resultlauncher.*;
```

**Responsibilities:**
- Manage result launch workflow (section → students → components)
- Coordinate between selection panels
- Launch configuration dialog
- Display launched results
- Navigate between launch steps

**Used By:**
- DashboardScreen (Result Launcher panel)

**Key Features:**
- Multi-step wizard interface
- Section selection → Student selection → Component selection → Preview → Launch
- Result history display
- Email notification trigger
- JSON export for web portal

**Key Methods:**
```java
private void showSectionSelection()
private void showStudentSelection()
private void showComponentSelection()
private void previewResults()
private void launchResults()
```

**Launch Workflow:**
1. Select section and subject
2. Select students (all or specific)
3. Select components to include
4. Configure launch (title, publish date)
5. Preview results
6. Launch (save JSON + send emails)

---

#### SectionSelectionPanel.java

**Location:** `src/com/sms/resultlauncher/SectionSelectionPanel.java`  
**Purpose:** First step - section and subject selection  
**Lines:** ~300  
**Imports:**
```java
import com.sms.resultlauncher.ResultLauncher;
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.dao.SectionDAO;
import com.sms.login.LoginScreen;
```

**Responsibilities:**
- Display available sections
- Subject selection for selected section
- Navigate to student selection
- Validate selection

**Used By:**
- ResultLauncher (step 1)

**Key Features:**
- Section cards with info
- Subject dropdown
- Modern card UI
- Validation before proceeding

---

#### StudentSelectionPanel.java

**Location:** `src/com/sms/resultlauncher/StudentSelectionPanel.java`  
**Purpose:** Second step - student selection  
**Lines:** ~300  
**Imports:**
```java
import com.sms.resultlauncher.ResultLauncher;
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.analyzer.Student;
import com.sms.dao.AnalyzerDAO;
import com.sms.login.LoginScreen;
```

**Responsibilities:**
- Display student list with checkboxes
- Select all / Select none functionality
- Show student count
- Navigate to component selection

**Used By:**
- ResultLauncher (step 2)

**Key Features:**
- Searchable student list
- Checkbox selection
- Student cards with roll numbers
- Selection count display

**Key Methods:**
```java
private void loadStudents()
private void selectAllStudents()
private void deselectAllStudents()
private List<Integer> getSelectedStudentIds()
```

---

#### ComponentSelectionPanel.java

**Location:** `src/com/sms/resultlauncher/ComponentSelectionPanel.java`  
**Purpose:** Third step - component selection  
**Lines:** ~400  
**Imports:**
```java
import com.sms.resultlauncher.ResultLauncher;
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.calculation.models.Component;
import com.sms.dao.AnalyzerDAO;
```

**Responsibilities:**
- Display marking scheme components
- Component selection with checkboxes
- Show component details (max marks, weight)
- Navigate to preview

**Used By:**
- ResultLauncher (step 3)

**Key Features:**
- Component list with metadata
- Group-wise display
- Select by exam type
- Weight display

**Key Methods:**
```java
private void loadComponents()
private List<Component> getSelectedComponents()
private void selectComponentsByExamType(String examType)
```

---

#### ResultPreviewDialog.java

**Location:** `src/com/sms/resultlauncher/ResultPreviewDialog.java`  
**Purpose:** Preview results before launching  
**Lines:** ~700  
**Imports:**
```java
import com.sms.analyzer.Student;
import com.sms.calculation.models.Component;
import com.sms.calculation.models.CalculationResult;
import com.sms.calculation.StudentCalculator;
import com.sms.dao.AnalyzerDAO;
import com.sms.login.LoginScreen;
```

**Responsibilities:**
- Display result preview table
- Calculate results for all students
- Show component-wise marks
- Total and percentage display
- Grade calculation
- Allow launch confirmation

**Used By:**
- ResultLauncher (preview step)

**Key Features:**
- Student-wise result table
- Component columns
- Total/percentage/grade
- Pass/fail indicator
- Launch button

**Key Methods:**
```java
private void loadStudentComponentMarks(int studentId)
private void calculateResults()
private void displayResultsTable()
private void launchResults()
```

**Table Columns:**
- Roll Number
- Student Name
- Component 1 marks
- Component 2 marks
- ...
- Total Obtained
- Total Possible
- Percentage
- Grade
- Status (Pass/Fail)

---

#### LaunchConfigurationDialog.java

**Location:** `src/com/sms/resultlauncher/LaunchConfigurationDialog.java`  
**Purpose:** Configure launch metadata  
**Lines:** ~300  
**Imports:**
```java
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.resultlauncher.ResultConfiguration;
```

**Responsibilities:**
- Input launch title
- Set publish date
- Optional message
- Email notification toggle
- Visibility settings

**Used By:**
- ResultLauncher (configuration step)

**Key Features:**
- Title input
- Date picker
- Message text area
- Email checkbox
- Public/private toggle

**Configuration Fields:**
```java
String launchTitle
Date publishDate
String message
boolean sendEmails
boolean isPublic
```

---

#### LaunchedResultsPanel.java

**Location:** `src/com/sms/resultlauncher/LaunchedResultsPanel.java`  
**Purpose:** Display history of launched results  
**Lines:** ~400  
**Imports:**
```java
import com.sms.resultlauncher.ResultLauncher;
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.resultlauncher.LaunchedResult;
```

**Responsibilities:**
- Display launched results list
- Show launch metadata (date, title, student count)
- View/edit/delete launched results
- Resend emails
- Download JSON

**Used By:**
- ResultLauncher (results history view)

**Key Features:**
- Result cards with metadata
- Action buttons (View, Edit, Delete, Resend)
- Sort by date
- Filter by section/subject

**Key Methods:**
```java
private void loadLaunchedResults()
private void viewResult(LaunchedResult result)
private void deleteResult(int resultId)
private void resendEmails(int resultId)
```

---

#### ResultDetailsDialog.java

**Location:** `src/com/sms/resultlauncher/ResultDetailsDialog.java`  
**Purpose:** Display launched result details  
**Lines:** ~300  
**Imports:**
```java
import com.sms.resultlauncher.LaunchedResult;
```

**Responsibilities:**
- Show complete result information
- Display JSON preview
- Show email status
- View student list
- Download options

**Used By:**
- LaunchedResultsPanel (View action)

**Key Features:**
- Tabbed interface (Info, Students, JSON)
- Email delivery status
- JSON syntax highlighting
- Export buttons

---

#### ResultLauncherDAO.java

**Location:** `src/com/sms/resultlauncher/ResultLauncherDAO.java`  
**Purpose:** Result launch database operations  
**Lines:** ~1000  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
import com.sms.dao.AnalyzerDAO;
import com.sms.calculation.models.Component;
import com.sms.calculation.models.CalculationResult;
import com.sms.calculation.StudentCalculator;
import com.sms.login.LoginScreen;
```

**Responsibilities:**
- Save launched results to database
- Generate JSON for web portal
- Store result metadata
- Track email status
- Retrieve launched results
- Update result status

**Used By:**
- ResultLauncher
- LaunchedResultsPanel
- ResultPreviewDialog

**Key Methods:**
```java
public int saveLaunchedResult(ResultConfiguration config, List<Student> students, List<Component> components)
public String generateResultJSON(int launchId)
public List<LaunchedResult> getLaunchedResults()
public boolean deleteLaunchedResult(int launchId)
public void updateEmailStatus(int launchId, boolean sent)
```

**Database Tables:**
- launched_results (metadata)
- launched_result_students (student mapping)
- launched_result_components (component mapping)
- student_result_data (calculated results)

**JSON Structure:**
```json
{
  "launch_id": 1,
  "title": "Mid-term Results",
  "publish_date": "2026-01-17",
  "section": "CSE-A",
  "subject": "Data Structures",
  "students": [
    {
      "roll_number": "2021001",
      "name": "John Doe",
      "components": [
        {"name": "Quiz 1", "obtained": 8, "max": 10},
        {"name": "Assignment 1", "obtained": 18, "max": 20}
      ],
      "total_obtained": 26,
      "total_possible": 30,
      "percentage": 86.67,
      "grade": "A",
      "status": "Pass"
    }
  ]
}
```

---

#### EmailService.java

**Location:** `src/com/sms/resultlauncher/EmailService.java`  
**Purpose:** Send result notification emails via MailerSend API  
**Lines:** ~400  
**Imports:**
```java
import com.sms.database.DatabaseConnection;
import com.sms.util.ConfigLoader;
```

**Responsibilities:**
- Send emails via MailerSend API
- Compose result notification emails
- Generate result portal links
- Track email delivery status
- Handle email failures

**Used By:**
- ResultLauncherDAO (email notification)

**Key Features:**
- MailerSend HTTP API integration
- HTML email templates
- Result portal link generation
- Batch email sending
- Delivery tracking

**Configuration:**
```properties
mailersend.api.key=your_api_key
mailersend.from.email=noreply@yourdomain.com
mailersend.from.name=Academic Analyzer
```

**Email Template:**
```
Subject: Your [Subject Name] Results are Published!

Dear [Student Name],

Your results for [Subject Name] - [Launch Title] have been published.

View your results: http://localhost:5000/results?launch_id=[ID]&roll=[ROLL]

Best regards,
Academic Analyzer Team
```

**Key Methods:**
```java
public static boolean sendResultNotifications(List<Student> students, String launchTitle, int launchId)
private static String buildEmailHtml(Student student, String title, String link)
```

---

#### ResultLauncherUtils.java

**Location:** `src/com/sms/resultlauncher/ResultLauncherUtils.java`  
**Purpose:** UI utilities for result launcher  
**Lines:** ~300  
**Imports:**
```java
import javax.swing.*;
import java.awt.*;
```

**Responsibilities:**
- Create styled components
- Color scheme constants
- Button factory methods
- Card creation helpers
- Icon utilities

**Used By:**
- All ResultLauncher components (HEAVILY USED)

**Key Constants:**
```java
public static final Color PRIMARY_COLOR = new Color(66, 133, 244);
public static final Color SUCCESS_COLOR = new Color(52, 168, 83);
public static final Color DANGER_COLOR = new Color(234, 67, 53);
public static final Color INFO_COLOR = new Color(23, 162, 184);
public static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
```

**Key Methods:**
```java
public static JPanel createModernCard()
public static JButton createModernButton(String text)
public static JButton createActionButton(String text, Color color)
public static JLabel createTitleLabel(String text)
```

---

#### LaunchedResult.java

**Location:** `src/com/sms/resultlauncher/LaunchedResult.java`  
**Purpose:** Launched result data model  
**Lines:** ~200  
**Imports:** None

**Responsibilities:**
- Store launch metadata
- Result configuration
- Email status
- JSON path

**Used By:**
- LaunchedResultsPanel
- ResultDetailsDialog
- ResultLauncherDAO

**Key Fields:**
```java
private int id;
private String launchTitle;
private Date publishDate;
private int sectionId;
private int subjectId;
private int studentCount;
private int componentCount;
private boolean emailsSent;
private String jsonFilePath;
```

---

#### ResultConfiguration.java

**Location:** `src/com/sms/resultlauncher/ResultConfiguration.java`  
**Purpose:** Launch configuration model  
**Lines:** ~150  
**Imports:** None

**Responsibilities:**
- Store launch settings
- Validation

**Used By:**
- LaunchConfigurationDialog
- ResultLauncherDAO

**Key Fields:**
```java
private String launchTitle;
private Date publishDate;
private String message;
private boolean sendEmails;
private boolean isPublic;
```

---

## 11. View Tool Package

### 11.1 com.sms.viewtool Package

Advanced data viewing and export tool with dual display modes.

#### ViewSelectionTool.java

**Location:** `src/com/sms/viewtool/ViewSelectionTool.java`  
**Purpose:** Comprehensive view and export tool with multiple display modes  
**Lines:** ~2600  
**Imports:**
```java
import com.sms.analyzer.Student;
import com.sms.dao.AnalyzerDAO;
import com.sms.dao.SectionDAO;
import com.sms.dao.StudentDAO;
import com.sms.database.DatabaseConnection;
```

**Responsibilities:**
- **Section Analysis Mode**: Display section data with field selection
- **Launched Results Mode**: Display launched results from JSON
- Multi-row header support for complex tables
- Export to PDF (A4 landscape, multi-row headers)
- Export to Excel (professional formatting)
- Field selection and filtering
- Dynamic table generation

**Used By:**
- DashboardScreen (View Data panel)

**Key Features:**

**Display Modes:**
1. **Section Analysis Mode**:
   - Select section and fields (Name, Roll, Email, Subjects, Marks)
   - Group by section
   - Component-wise breakdown
   - Exam type filtering

2. **Launched Results Mode**:
   - Load launched result JSON
   - Display student results
   - Component columns
   - Total/percentage/grade

**Export Capabilities:**
- **PDF Export**:
  - A4 landscape orientation
  - Multi-row headers (category → subcategory → field)
  - Professional styling
  - Page numbering
  - Auto-fit columns

- **Excel Export**:
  - Multi-row headers with merged cells
  - Color-coded headers
  - Borders and styling
  - Auto-width columns
  - Freeze panes

**Multi-Row Header System:**
```
Row 1: [Student Info] [Subject 1      ] [Subject 2      ]
Row 2: [Name] [Roll]  [Quiz1] [Mid]   [Quiz1] [Mid]
```

**Key Methods:**
```java
private void buildSectionAnalysisTable()
private void buildLaunchedResultsTable()
private void exportToPDF()
private void exportToExcel()
private List<ColumnGroup> buildColumnGroups()
```

**Data Models:**
```java
class ExtendedStudentData {
    Student student;
    Map<String, Map<String, Double>> subjectMarks; // subject → component → marks
    Map<String, Double> subjectTotals;
}

class ColumnGroup {
    String category;
    List<String> subColumns;
}

class SectionInfo {
    String sectionName;
    List<ExtendedStudentData> students;
}
```

**Technical Highlights:**
- Custom table model with multi-level headers
- Apache POI for Excel (XSSFWorkbook)
- iText for PDF generation
- JTable custom renderer for merged headers
- Dynamic column generation based on data

**Export File Names:**
```
Section_Analysis_[SectionName]_[Date].pdf
Section_Analysis_[SectionName]_[Date].xlsx
Launched_Results_[LaunchTitle]_[Date].pdf
```

---

## 12. Theme Package

### 12.1 com.sms.theme Package

Application-wide theme and styling management.

#### ThemeManager.java

**Location:** `src/com/sms/theme/ThemeManager.java`  
**Purpose:** Centralized theme configuration and color management  
**Lines:** ~200  
**Pattern:** Singleton  
**Imports:**
```java
import java.util.prefs.Preferences;
```

**Responsibilities:**
- Provide consistent color scheme
- Manage light/dark mode (future)
- Store user preferences
- Font configurations
- UI element styling

**Used By (40+ references):**
- CreateSectionDialog
- CreateSectionPanel
- MarkEntryDialog
- SchemePreviewPanel
- ComponentGroupPanel
- FlexibleMarkEntryDialog
- ComponentGroupDialog

**Key Constants:**
```java
// Primary Colors
private final Color PRIMARY_COLOR = new Color(66, 133, 244);
private final Color PRIMARY_DARK = new Color(48, 79, 254);
private final Color PRIMARY_LIGHT = new Color(138, 180, 248);

// Status Colors
private final Color SUCCESS_COLOR = new Color(52, 168, 83);
private final Color WARNING_COLOR = new Color(251, 188, 5);
private final Color DANGER_COLOR = new Color(234, 67, 53);
private final Color INFO_COLOR = new Color(23, 162, 184);

// UI Colors
private final Color BACKGROUND_COLOR = new Color(248, 249, 250);
private final Color CARD_COLOR = Color.WHITE;
private final Color BORDER_COLOR = new Color(222, 226, 230);
private final Color TEXT_PRIMARY = new Color(33, 37, 41);
private final Color TEXT_SECONDARY = new Color(108, 117, 125);

// Fonts
private final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 20);
private final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 16);
private final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
```

**Key Methods:**
```java
public static ThemeManager getInstance()
public Color getPrimaryColor()
public Color getSuccessColor()
public Color getBackgroundColor()
public Color getCardColor()
public Color getBorderColor()
public Color getTextPrimaryColor()
public Font getTitleFont()
```

**Preferences Storage:**
```java
private Preferences prefs;
// Stores: theme mode, font size, custom colors (future)
```

**Usage Example:**
```java
ThemeManager theme = ThemeManager.getInstance();
panel.setBackground(theme.getCardColor());
label.setForeground(theme.getTextPrimaryColor());
button.setBackground(theme.getPrimaryColor());
```

---

## 13. Utility Package

### 13.1 com.sms.util Package

Application utilities and configuration management.

#### ConfigLoader.java

**Location:** `src/com/sms/util/ConfigLoader.java`  
**Purpose:** Load application configuration from config.properties  
**Lines:** ~200  
**Imports:**
```java
import java.util.Properties;
import java.io.FileInputStream;
```

**Responsibilities:**
- Load config.properties file
- Provide database credentials
- Provide email service credentials (MailerSend, EmailJS)
- Environment-specific configuration
- Fallback to defaults if config missing

**Used By (17 references):**
- DatabaseConnection (database credentials)
- EmailService (MailerSend API)
- ForgotPasswordScreen (EmailJS)
- CreateAccountScreen (EmailJS)

**Configuration File:** `config.properties`
```properties
# Database Configuration
db.url=jdbc:mysql://localhost:3306/academic_analyzer
db.username=root
db.password=your_password

# MailerSend API (Result Notifications)
mailersend.api.key=your_mailersend_api_key
mailersend.from.email=noreply@yourdomain.com
mailersend.from.name=Academic Analyzer

# EmailJS API (OTP/Account Verification)
emailjs.service.id=your_service_id
emailjs.template.id=your_template_id
emailjs.public.key=your_public_key
```

**Key Methods:**
```java
// Database
public static String getDatabaseUrl()
public static String getDatabaseUsername()
public static String getDatabasePassword()

// MailerSend
public static String getMailerSendApiKey()
public static String getMailerSendFromEmail()
public static String getMailerSendFromName()

// EmailJS
public static String getEmailJsServiceId()
public static String getEmailJsTemplateId()
public static String getEmailJsPublicKey()
```

**Error Handling:**
```java
// Returns default values if config.properties missing
// Logs warning to console
// Graceful degradation
```

---

## 14. Dependencies & External Libraries

### 14.1 Maven Dependencies (pom.xml)

**Build Tool:** Maven  
**Java Version:** 11+

#### Core Dependencies:

**1. MySQL Connector (8.0.33)**
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```
**Purpose:** Database connectivity  
**Used By:** DatabaseConnection, all DAO classes

---

**2. Apache POI (5.2.3)**
```xml
<!-- Excel file handling -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.3</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```
**Purpose:** Excel export (XLSX)  
**Used By:** ViewSelectionTool (Excel export)  
**Features:** XSSFWorkbook, cell styling, merged cells

---

**3. iText PDF (5.5.13.3)**
```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>
    <version>5.5.13.3</version>
</dependency>
```
**Purpose:** PDF generation  
**Used By:** ViewSelectionTool, SectionAnalyzer (PDF export)  
**Features:** A4 landscape, tables, multi-page support

---

**4. FlatLaf (3.2.5)**
```xml
<dependency>
    <groupId>com.formdev</groupId>
    <artifactId>flatlaf</artifactId>
    <version>3.2.5</version>
</dependency>
```
**Purpose:** Modern Look & Feel for Swing  
**Used By:** Main.java (application-wide)  
**Features:** Flat design, cross-platform consistency

---

**5. JSON-java (20231013)**
```xml
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20231013</version>
</dependency>
```
**Purpose:** JSON parsing and generation  
**Used By:** ResultLauncherDAO (JSON export), ViewSelectionTool (JSON loading)

---

**6. Apache Commons CSV (1.10.0)**
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.10.0</version>
</dependency>
```
**Purpose:** CSV import/export (future feature)  
**Status:** Included but not yet used

---

### 14.2 lib/ Directory (Manual JARs)

The following JARs are manually added in `lib/` folder:

```
lib/
├── commons-csv-1.10.0.jar
├── flatlaf-3.2.5.jar
├── itextpdf-5.5.13.3.jar
├── json-20231013.jar
├── mysql-connector-j-8.0.33.jar
├── poi-5.2.3.jar
├── poi-ooxml-5.2.3.jar
├── poi-ooxml-lite-5.2.3.jar
├── xmlbeans-5.1.1.jar
├── commons-compress-1.21.jar
└── commons-collections4-4.4.jar
```

**Classpath Configuration:**
```bash
javac -cp "lib/*;bin" ...
java -cp "bin;lib/*" Main
```

---

## 15. Data Flow & Routing

### 15.1 Application Flow Diagram

```
Main.java
    ↓
[Database Connection Test]
    ↓
LoginScreen
    ├─→ ForgotPasswordScreen → OTPVerificationScreen → ResetPasswordScreen
    ├─→ CreateAccountScreen
    └─→ [Successful Login]
            ↓
    DashboardScreen (CardLayout Hub)
            ↓
    ┌───────┴───────────────────────────┐
    │                                   │
YearSemesterPanel              SidebarPanel (Navigation)
    │                                   │
    ├─→ CreateSectionDialog             ├─→ Section Analyzer
    ├─→ StudentEntryDialog              ├─→ Student Analyzer
    └─→ MarkEntryDialog                 ├─→ View Data Tool
                                        ├─→ Result Launcher
                                        └─→ Logout
```

---

### 15.2 Section Creation Flow

```
CreateSectionDialog
    ↓
[Input: Name, Year, Semester, Capacity]
    ↓
[Select Subjects]
    ↓
[Build Marking Scheme]
    ├─→ Select Template (optional)
    └─→ Custom Configuration
            ↓
    [Add Component Groups]
        ├─→ Best Of (n)
        ├─→ All Required
        └─→ Weighted
            ↓
    [Add Components to Groups]
        ├─→ Name, Exam Type
        ├─→ Max Marks, Passing Marks
        └─→ Weight
            ↓
    [Validate Weights = 100%]
            ↓
    [Preview Scheme]
            ↓
    [Save to Database]
        ├─→ SectionDAO.createSectionWithScheme()
        ├─→ MarkingSchemeDAO.createMarkingScheme()
        └─→ Save component groups and components
            ↓
    [Refresh Dashboard]
```

---

### 15.3 Mark Entry Flow

```
MarkEntryDialog / FlexibleMarkEntryDialog
    ↓
[Select Section]
    ↓
[Select Subject]
    ↓
[Load Marking Scheme]
    ├─→ MarkingSchemeDAO.getMarkingSchemeForSection()
    └─→ Display component list
            ↓
[Load Students]
    └─→ StudentDAO.getStudentsBySection()
            ↓
[Mark Entry Table]
    ├─→ Student rows
    └─→ Component columns
            ↓
[Input Marks]
    ├─→ Validate max marks
    ├─→ Validate passing marks
    └─→ Real-time calculation
            ↓
[Save Marks]
    └─→ StudentComponentMarkDAO.bulkSaveMarks()
            ↓
[Calculate Results]
    └─→ StudentCalculator.calculateStudentResult()
```

---

### 15.4 Result Launch Flow

```
ResultLauncher
    ↓
Step 1: SectionSelectionPanel
    └─→ Select section and subject
            ↓
Step 2: StudentSelectionPanel
    └─→ Select students (all or specific)
            ↓
Step 3: ComponentSelectionPanel
    └─→ Select components to include
            ↓
Step 4: LaunchConfigurationDialog
    ├─→ Launch title
    ├─→ Publish date
    ├─→ Optional message
    └─→ Email notification toggle
            ↓
Step 5: ResultPreviewDialog
    ├─→ Load student marks (AnalyzerDAO)
    ├─→ Calculate results (StudentCalculator)
    ├─→ Display preview table
    └─→ Confirm launch
            ↓
Step 6: Launch Execution
    ├─→ ResultLauncherDAO.saveLaunchedResult()
    ├─→ Generate JSON file
    │   └─→ Save to result-portal/static/
    ├─→ Save to database
    └─→ Send email notifications
        └─→ EmailService.sendResultNotifications()
            ├─→ MailerSend API call
            └─→ Result portal links
                    ↓
Step 7: Confirmation
    └─→ Display success message
    └─→ Show LaunchedResultsPanel
```

---

### 15.5 Calculation Flow

```
StudentCalculator.calculateStudentResult()
    ↓
[Load Student Component Marks]
    └─→ StudentComponentMarkDAO.getStudentMarks()
            ↓
[Load Marking Scheme]
    └─→ MarkingSchemeDAO.getMarkingSchemeForSection()
            ↓
[Apply Component Groups]
    ├─→ For each ComponentGroup:
    │   ├─→ If "Best Of n":
    │   │   └─→ GroupSelectionLogic.applyGroupSelection()
    │   │       └─→ Sort by marks, select top n
    │   ├─→ If "All Required":
    │   │   └─→ Include all components
    │   └─→ If "Weighted":
    │       └─→ Apply custom weights
    │           └─→ ComponentWeightManager.applyWeights()
            ↓
[Calculate Totals]
    ├─→ Sum obtained marks
    ├─→ Sum possible marks
    └─→ Calculate percentage
            ↓
[Determine Pass/Fail]
    ├─→ Check passing marks for each component
    └─→ Overall pass threshold (40%)
            ↓
[Assign Grade]
    └─→ CalculationUtils.getGrade(percentage)
            ↓
[Return CalculationResult]
    ├─→ totalObtained
    ├─→ totalPossible
    ├─→ percentage
    ├─→ grade
    ├─→ passed
    └─→ includedComponents
```

---

### 15.6 Import Relationships

**Critical Import Chains:**

**1. Database Access Chain:**
```
Any Component
    ↓
import com.sms.database.DatabaseConnection
    ↓
import com.sms.util.ConfigLoader
    ↓
config.properties
```

**2. Theme Chain:**
```
Any Dialog/Panel
    ↓
import com.sms.theme.ThemeManager
    ↓
ThemeManager.getInstance()
```

**3. Calculation Chain:**
```
Result Display Component
    ↓
import com.sms.calculation.StudentCalculator
    ↓
import com.sms.calculation.models.CalculationResult
    ↓
import com.sms.calculation.models.Component
```

**4. DAO Chain:**
```
UI Component
    ↓
import com.sms.dao.{AnalyzerDAO|SectionDAO|StudentDAO}
    ↓
import com.sms.database.DatabaseConnection
```

**5. Analyzer Chain:**
```
DashboardScreen
    ↓
import com.sms.analyzer.{SectionAnalyzer|StudentAnalyzer}
    ↓
import com.sms.dao.AnalyzerDAO
    ↓
import com.sms.analyzer.Student
```

---

### 15.7 Package Dependencies Matrix

```
Package          → Depends On
────────────────────────────────────────────
login            → database, util
dashboard        → dao, analyzer, viewtool, resultlauncher, marking, theme, util, database
analyzer         → dao, database
calculation      → (models only, no external deps)
dao              → database, marking.models
marking          → database, theme, calculation.models
resultlauncher   → database, dao, calculation, analyzer, login
viewtool         → database, dao, analyzer
theme            → (no external deps)
util             → (no external deps)
database         → util
```

---

## 16. Best Practices & Conventions

### 16.1 Code Organization

**Package Naming:**
- `com.sms.{feature}` - Feature-based packaging
- `{feature}.dao` - Data access objects
- `{feature}.models` - Data models
- `{feature}.dialogs` - Dialog windows
- `{feature}.components` - Reusable UI components
- `{feature}.services` - Business logic
- `{feature}.utils` - Utilities

**Class Naming:**
- `*DAO` - Data Access Objects
- `*Dialog` - JDialog subclasses
- `*Panel` - JPanel subclasses
- `*Screen` - JFrame subclasses
- `*Service` - Business logic services
- `*Utils` - Utility classes
- `*Manager` - Singleton managers

---

### 16.2 Database Patterns

**Connection Management:**
```java
// Always use singleton
Connection conn = DatabaseConnection.getConnection();

// Use try-with-resources for statements
try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // ...
}
```

**Transaction Handling:**
```java
conn.setAutoCommit(false);
try {
    // Multiple operations
    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw e;
}
```

---

### 16.3 UI Patterns

**Theme Usage:**
```java
ThemeManager theme = ThemeManager.getInstance();
panel.setBackground(theme.getCardColor());
label.setForeground(theme.getTextPrimaryColor());
```

**Component Factory:**
```java
JButton btn = UIComponentFactory.createPrimaryButton("Save");
JPanel card = UIComponentFactory.createCard();
```

**Background Tasks:**
```java
BackgroundTaskUtil.execute(
    () -> dao.loadData(),
    data -> updateUI(data)
);
```

---

### 16.4 Error Handling

**User-Facing Errors:**
```java
DashboardErrorHandler.showError(this, "Error message");
DashboardErrorHandler.showWarning(this, "Warning message");
DashboardErrorHandler.showSuccess(this, "Success message");
```

**Logging:**
```java
try {
    // operation
} catch (Exception e) {
    DashboardErrorHandler.logError(e);
    // Show user-friendly message
}
```

---

## 17. Future Enhancements

**Planned Features:**
1. **CSV Import**: Bulk student/marks import
2. **Report Templates**: Customizable report formats
3. **Dark Mode**: Theme switcher
4. **Attendance Tracking**: Integrated attendance management
5. **Parent Portal**: Parent access to student results
6. **Analytics Dashboard**: Visual analytics with charts
7. **Backup/Restore**: Database backup utilities
8. **Multi-Language**: Internationalization support

---

## 18. Conclusion

This document provides a comprehensive overview of the AcademicAnalyzer codebase. The application follows a layered architecture with clear separation of concerns:

- **Presentation Layer**: Swing UI components
- **Business Logic Layer**: Calculation engine, marking system
- **Data Access Layer**: DAO pattern for database operations
- **Utility Layer**: Theme management, configuration, helpers

**Key Strengths:**
- Modular package structure
- Consistent naming conventions
- Singleton patterns for shared resources
- Factory patterns for UI components
- Comprehensive marking scheme system
- Flexible result launching with JSON export

**Total Statistics:**
- **Packages**: 13
- **Classes**: 80+
- **Lines of Code**: ~25,000+
- **Database Tables**: 15+
- **External Dependencies**: 6 major libraries

---

**Document Version:** 1.0  
**Last Updated:** January 17, 2026  

---