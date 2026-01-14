# Student Analyzer - Technical Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [UI Components](#ui-components)
4. [Core Functionality](#core-functionality)
5. [Database Schema](#database-schema)
6. [Calculation Logic](#calculation-logic)
7. [Methods Reference](#methods-reference)
8. [PDF Export System](#pdf-export-system)
9. [Integration Points](#integration-points)

---

## 1. Overview

### Purpose
The **Student Analyzer** is a comprehensive academic performance analysis tool that provides detailed insights into individual student performance across subjects, exam types, and overall academic standing. It serves as the student-centric counterpart to the Section Analyzer, focusing on granular student-level metrics.

### Key Features
- **Individual Student Analysis**: Detailed performance breakdown by subject and exam type
- **Weighted Calculation System**: Uses actual weightage percentages (no normalization)
- **Visual Performance Metrics**: SGPA, percentage, grade, rank display
- **Subject Performance Breakdown**: Hierarchical view of subjects and exam components
- **Selective Filtering**: Tree-based subject and exam type selection
- **PDF Report Export**: Professional report generation with detailed breakdowns
- **Comparison Mode**: Switch between student and section analysis views

### Technology Stack
- **Language**: Java 8+
- **UI Framework**: Java Swing with FlatLaf modern theme
- **Database**: MySQL
- **Charting**: JFreeChart
- **PDF Generation**: iText 5.x
- **Design Pattern**: MVC-inspired with DAO pattern

---

## 2. Architecture

### Class Structure

```
StudentAnalyzer.java (Main Class)
├── Extends: JPanel
├── Dependencies:
│   ├── AnalyzerDAO (Data Access Layer)
│   ├── SectionDAO (Section Data)
│   ├── DatabaseConnection (DB Connectivity)
│   ├── Student (Data Model)
│   └── SectionAnalyzer (Comparison View)
└── Responsibilities:
    ├── UI Rendering
    ├── User Input Handling
    ├── Analysis Orchestration
    ├── PDF Report Generation
    └── Data Presentation
```

### Student.java (Data Model)

```java
public class Student {
    private int id;                                          // Database primary key
    private String name;                                     // Student name
    private String rollNumber;                               // Unique roll number
    private Map<String, Map<String, Integer>> marks;        // Subject -> ExamType -> Marks
    private String section;                                  // Section name
}
```

**Key Characteristics:**
- **Nested Map Structure**: Organizes marks hierarchically (Subject → Exam Type → Marks)
- **Section Association**: Each student belongs to exactly one section
- **Database Binding**: `id` field links to `students` table primary key

---

## 3. UI Components

### 3.1 Header Section

**Components:**
```
┌──────────────────────────────────────────────────┐
│  🎓 Student Performance Analyzer                 │
│  ○ Student   ○ Section                          │
└──────────────────────────────────────────────────┘
```

**Radio Button Group:**
- **Student Radio**: Selected by default, shows student analysis view
- **Section Radio**: Switches to SectionAnalyzer comparison view
- **Purpose**: Quick navigation between analysis modes

**Color Scheme:**
```java
BACKGROUND_COLOR = #F5F7FA (Light gray-blue)
CARD_COLOR = #FFFFFF (White)
PRIMARY_COLOR = #6366F1 (Indigo)
PRIMARY_DARK = #4F46E5 (Dark indigo)
TEXT_PRIMARY = #111827 (Almost black)
TEXT_SECONDARY = #6B7280 (Gray)
SUCCESS_COLOR = #22C55E (Green)
DANGER_COLOR = #EF4444 (Red)
WARNING_COLOR = #FB9238 (Orange)
```

### 3.2 Input Card

**Purpose**: Student identification and section selection

**Layout:**
```
┌─────────────────────────────────────────────────┐
│  Section Dropdown                               │
│  [Select Section ▼]                            │
│                                                  │
│  Student Name                                   │
│  [Enter name or select...]                     │
│                                                  │
│  Roll Number                                    │
│  [Enter roll number...]                        │
│                                                  │
│  [🔍 Analyze Student]                          │
└─────────────────────────────────────────────────┘
```

**Auto-completion:**
- Student name field provides dropdown suggestions as user types
- Selecting from dropdown auto-fills roll number
- Bidirectional sync between name and roll number fields

### 3.3 Filter Card (Tree Selector)

**Purpose**: Select specific subjects and exam types for analysis

**Tree Structure:**
```
☑ Subject 1
  ☑ Internal 1
  ☑ Internal 2
  ☑ External
☑ Subject 2
  ☑ Mid Term
  ☑ End Term
```

**Features:**
- **Tri-state Checkboxes**: Parent selection toggles all children
- **Selective Analysis**: Only checked items included in calculations
- **Dynamic Loading**: Tree populated from database based on student's section
- **Visual Hierarchy**: Clear parent-child relationships

**Implementation:**
```java
private Map<String, Set<String>> selectedFilters;
// Structure: {"Subject1": ["Internal 1", "External"], "Subject2": ["Mid Term"]}
```

### 3.4 Analysis Panel (Results Display)

**Metrics Grid (4×2 Layout):**
```
┌─────────┬──────────┬─────────┬─────────┐
│ 🎯 SGPA │ 📈 %     │ 🏆 Rank │ 📚 Sub  │
│  9.46   │  94.6%   │    1    │    5    │
├─────────┼──────────┼─────────┼─────────┤
│ 📝 Exams│ 🎓 Grade │ ✅ Stat │ Export  │
│   15    │   A+     │  Pass   │  [📄]   │
└─────────┴──────────┴─────────┴─────────┘
```

**Metric Cards:**
1. **SGPA**: Scale 0-10, calculated as `percentage / 10`
2. **Percentage**: Weighted average across all selected subjects
3. **Rank**: Position in section based on overall percentage
4. **Subjects**: Count of selected subjects
5. **Exams**: Total exam types across all subjects
6. **Grade**: Letter grade (A+, A, B+, ..., F)
7. **Status**: Pass/Fail based on 50% threshold
8. **Export**: PDF report generation button

### 3.5 Subject Performance Panel

**Hierarchical Table Structure:**
```
┌──────────┬──────────────────────────────────┬─────────┬──────┐
│          │        Subject 1 (100)           │  Total  │Grade │
│          ├─────────┬─────────┬──────────────┤         │      │
│ Roll No  │Internal1│Internal2│   External   │  /100   │      │
│          │  (20)   │  (20)   │    (60)      │         │      │
├──────────┼─────────┼─────────┼──────────────┼─────────┼──────┤
│   101    │   18    │   19    │     58       │  94.64  │  A+  │
│   102    │   17    │   18    │     55       │  92.33  │  A+  │
└──────────┴─────────┴─────────┴──────────────┴─────────┴──────┘
```

**Key Features:**
- **2-Row Headers**: Subject name in row 1, exam types in row 2
- **Actual Max Marks**: Displayed in brackets (e.g., "Internal 1 (20)")
- **Weighted Total**: Calculated using actual weightage percentages
- **Grade Column**: Letter grade per subject
- **Auto-sizing**: Column widths adapt to content length

---

## 4. Core Functionality

### 4.1 Student Selection Process

**Flow Diagram:**
```
User Input → Validation → Database Lookup → Data Retrieval → Analysis
```

**Steps:**

1. **Section Selection:**
   ```java
   String selectedSection = (String) sectionDropdown.getSelectedItem();
   List<Student> students = sectionStudents.get(selectedSection);
   ```

2. **Student Identification:**
   - By Name: Searches `students` table with LIKE query
   - By Roll Number: Exact match on `roll_number` column

3. **Validation:**
   ```java
   if (studentName.trim().isEmpty() || rollNumber.trim().isEmpty()) {
       // Show error message
       return;
   }
   ```

4. **Data Loading:**
   ```java
   Student student = findStudentInSection(selectedSection, rollNumber);
   currentStudent = student;
   currentStudent.setId(fetchStudentIdFromDB(rollNumber));
   ```

### 4.2 Analysis Workflow

**Main Method: `analyzeStudent()`**

```java
private void analyzeStudent() {
    // 1. Validate inputs
    String studentName = studentNameField.getText().trim();
    String rollNumber = rollNumberField.getText().trim();
    String selectedSection = (String) sectionDropdown.getSelectedItem();
    
    // 2. Find student in memory
    Student student = findStudentInSection(selectedSection, rollNumber);
    
    // 3. Load database ID
    int studentId = fetchStudentIdFromDB(rollNumber);
    student.setId(studentId);
    
    // 4. Build filter tree (subjects and exam types)
    buildFilterTree(student);
    
    // 5. Show filter selection card
    showFilterCard();
}
```

**Filter Tree Construction:**
```java
private void buildFilterTree(Student student) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Select Subjects & Exams");
    
    for (String subject : student.getMarks().keySet()) {
        DefaultMutableTreeNode subjectNode = new DefaultMutableTreeNode(subject);
        
        for (String examType : student.getMarks().get(subject).keySet()) {
            DefaultMutableTreeNode examNode = new DefaultMutableTreeNode(examType);
            subjectNode.add(examNode);
        }
        
        root.add(subjectNode);
    }
    
    filterTree = new JTree(root);
    // Add checkbox renderer
}
```

### 4.3 Calculate Analysis

**Triggered by**: Clicking "Calculate Analysis" button after filter selection

**Process:**
```java
private void calculateAnalysis() {
    // 1. Extract selected filters from tree
    selectedFilters = extractSelectedFilters(filterTree);
    
    // 2. Calculate metrics using DAO
    AnalyzerDAO dao = new AnalyzerDAO();
    int studentId = currentStudent.getId();
    int sectionId = getStudentSectionId(studentId);
    
    // 3. Get detailed ranking data
    DetailedRankingData rankingData = dao.getDetailedStudentRanking(
        sectionId, 
        selectedFilters
    );
    
    // 4. Extract metrics
    double sgpa = rankingData.percentage / 10.0;
    double percentage = rankingData.percentage;
    String grade = getLetterGrade(percentage);
    int rank = calculateStudentRank(studentId);
    
    // 5. Update UI
    updateAnalysisPanel(sgpa, percentage, grade, subjectCount, examCount, rank);
    populateSubjectPerformanceTable(rankingData);
}
```

---

## 5. Database Schema

### 5.1 Core Tables

**students**
```sql
CREATE TABLE students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    roll_number VARCHAR(20) UNIQUE NOT NULL,
    section_id INT NOT NULL,
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (section_id) REFERENCES sections(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**sections**
```sql
CREATE TABLE sections (
    id INT PRIMARY KEY AUTO_INCREMENT,
    section_name VARCHAR(50) NOT NULL,
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_section_per_user (section_name, user_id)
);
```

**marking_components**
```sql
CREATE TABLE marking_components (
    id INT PRIMARY KEY AUTO_INCREMENT,
    section_id INT NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    exam_type VARCHAR(50) NOT NULL,
    actual_max_marks INT NOT NULL,           -- Original max marks
    scaled_to_marks INT NOT NULL,            -- Weightage out of 100
    passing_marks INT DEFAULT 0,
    FOREIGN KEY (section_id) REFERENCES sections(id),
    UNIQUE KEY unique_component (section_id, subject_name, exam_type)
);
```

**marks**
```sql
CREATE TABLE marks (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    section_id INT NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    exam_type VARCHAR(50) NOT NULL,
    marks INT NOT NULL,
    max_marks INT NOT NULL,                  -- Reference max marks
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES sections(id),
    UNIQUE KEY unique_mark_entry (student_id, subject_name, exam_type)
);
```

### 5.2 Relationship Diagram

```
users (1) ────────┐
                  │
                  ├──> sections (N)
                  │         │
                  │         ├──> marking_components (N)
                  │         │
                  │         └──> students (N)
                  │                   │
                  └──> students       └──> marks (N)
```

**Key Relationships:**
- One user has many sections
- One section has many students
- One section has many marking components (weightage configuration)
- One student has many mark entries
- Marks reference both student and marking components

---

## 6. Calculation Logic

### 6.1 Weighted Subject Total

**Method:** `calculateWeightedSubjectTotalWithPass()`

**Purpose:** Calculate student's weighted total for a subject considering actual weightage percentages

**Formula:**
```
Weighted Total = Σ (marks_obtained / actual_max_marks) × scaled_to_marks
                 for all selected exam types in subject
```

**Example Calculation:**

*Subject: Mathematics (Total 100)*
- Internal 1: 18/20 marks (scaled to 20)
- Internal 2: 19/20 marks (scaled to 20)
- External: 58/60 marks (scaled to 60)

```
Weighted Total = (18/20 × 20) + (19/20 × 20) + (58/60 × 60)
               = 18.00 + 19.00 + 58.00
               = 95.00
```

**Code Implementation:**
```java
public SubjectPassResult calculateWeightedSubjectTotalWithPass(
    int studentId, 
    int sectionId, 
    String subjectName, 
    Set<String> selectedExamTypes
) {
    double totalWeighted = 0.0;
    boolean allComponentsPassed = true;
    List<String> failedComponents = new ArrayList<>();
    
    // Query marking_components and marks tables
    String query = 
        "SELECT mc.exam_type, mc.actual_max_marks, mc.scaled_to_marks, " +
        "       mc.passing_marks, m.marks " +
        "FROM marking_components mc " +
        "LEFT JOIN marks m ON m.subject_name = mc.subject_name " +
        "   AND m.exam_type = mc.exam_type " +
        "   AND m.student_id = ? " +
        "WHERE mc.section_id = ? AND mc.subject_name = ?";
    
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, studentId);
    stmt.setInt(2, sectionId);
    stmt.setString(3, subjectName);
    ResultSet rs = stmt.executeQuery();
    
    while (rs.next()) {
        String examType = rs.getString("exam_type");
        
        // Apply filter if specified
        if (selectedExamTypes != null && !selectedExamTypes.contains(examType)) {
            continue;
        }
        
        int actualMaxMarks = rs.getInt("actual_max_marks");
        int scaledToMarks = rs.getInt("scaled_to_marks");
        int passingMarks = rs.getInt("passing_marks");
        int marksObtained = rs.getInt("marks");
        
        // Calculate weighted contribution
        double percentage = (double) marksObtained / actualMaxMarks;
        double weightedContribution = percentage * scaledToMarks;
        totalWeighted += weightedContribution;
        
        // Check passing status (DUAL PASSING REQUIREMENT)
        if (marksObtained < passingMarks) {
            allComponentsPassed = false;
            failedComponents.add(examType);
        }
    }
    
    // Return result
    return new SubjectPassResult(
        totalWeighted,                           // percentage (0-100 scale)
        allComponentsPassed,                     // passed flag
        totalWeighted >= 50.0,                   // totalPassed (50% threshold)
        allComponentsPassed,                     // allComponentsPassed
        failedComponents                         // failed exam types
    );
}
```

**SubjectPassResult Class:**
```java
public static class SubjectPassResult {
    public double percentage;                    // Weighted total (0-100)
    public boolean passed;                       // All components passed?
    public boolean totalPassed;                  // Total >= 50%?
    public boolean allComponentsPassed;          // Same as passed
    public List<String> failedComponents;        // List of failed exam types
}
```

### 6.2 Overall Percentage Calculation

**Method:** `getDetailedStudentRanking()`

**Purpose:** Calculate student's overall percentage across all selected subjects

**Formula:**
```
Overall Percentage = (Σ weighted_subject_totals) / (number_of_subjects × 100) × 100
                   = Σ weighted_subject_totals / number_of_subjects
```

**Dual Passing Requirement:**
1. Student must pass each individual component (marks >= passing_marks)
2. Student must achieve >= 50% in subject's weighted total

**If either fails:** Subject percentage = -1 (indicates failure)

**Code Implementation:**
```java
public DetailedRankingData getDetailedStudentRanking(
    int sectionId, 
    Map<String, Set<String>> selectedFilters
) {
    DetailedRankingData data = new DetailedRankingData();
    double totalPercentage = 0.0;
    int validSubjects = 0;
    
    for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
        String subject = entry.getKey();
        Set<String> examTypes = entry.getValue();
        
        // Get student ID from context
        int studentId = currentStudent.getId();
        
        // Calculate subject total with passing logic
        SubjectPassResult result = calculateWeightedSubjectTotalWithPass(
            studentId, sectionId, subject, examTypes
        );
        
        // Apply DUAL PASSING logic
        if (result.percentage < 0) {
            // Failed - set CGPA to 0
            data.subjects.add(new SubjectData(subject, 0.0, "F", examTypes));
            continue;
        }
        
        totalPercentage += result.percentage;
        validSubjects++;
        
        String grade = getLetterGrade(result.percentage);
        data.subjects.add(new SubjectData(subject, result.percentage, grade, examTypes));
    }
    
    // Calculate average
    if (validSubjects > 0) {
        data.percentage = totalPercentage / validSubjects;
        data.cgpa = data.percentage / 10.0;
    } else {
        data.percentage = 0.0;
        data.cgpa = 0.0;
    }
    
    return data;
}
```

### 6.3 SGPA Calculation

**Formula:**
```
SGPA = Overall Percentage / 10
```

**Scale:** 0.00 to 10.00

**Example:**
- If percentage = 94.6%, then SGPA = 94.6 / 10 = 9.46

**Code:**
```java
double sgpa = percentage / 10.0;
```

### 6.4 Letter Grade Assignment

**Grading Scale:**
| Percentage Range | Grade | Description    |
|-----------------|-------|----------------|
| 90 - 100        | A+    | Outstanding    |
| 85 - 89         | A     | Excellent      |
| 80 - 84         | B+    | Very Good      |
| 75 - 79         | B     | Good           |
| 70 - 74         | C+    | Above Average  |
| 65 - 69         | C     | Average        |
| 60 - 64         | D+    | Below Average  |
| 50 - 59         | D     | Pass           |
| < 50            | F     | Fail           |

**Implementation:**
```java
private String getLetterGrade(double percentage) {
    if (percentage >= 90) return "A+";
    else if (percentage >= 85) return "A";
    else if (percentage >= 80) return "B+";
    else if (percentage >= 75) return "B";
    else if (percentage >= 70) return "C+";
    else if (percentage >= 65) return "C";
    else if (percentage >= 60) return "D+";
    else if (percentage >= 50) return "D";
    else return "F";
}
```

### 6.5 Rank Calculation

**Purpose:** Determine student's position within section based on overall percentage

**Query:**
```sql
SELECT s.id, s.name, s.roll_number,
       -- Calculate weighted percentage for each student
FROM students s
WHERE s.section_id = ?
ORDER BY weighted_percentage DESC;
```

**Simplified Rank (Current Implementation):**
```java
private int calculateStudentRank(int studentId) {
    String query = 
        "SELECT COUNT(*) as rank " +
        "FROM students " +
        "WHERE section_id = (SELECT section_id FROM students WHERE id = ?) " +
        "AND id < ?";
    
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, studentId);
    stmt.setInt(2, studentId);
    ResultSet rs = stmt.executeQuery();
    
    if (rs.next()) {
        return rs.getInt("rank") + 1;
    }
    return 1;
}
```

**Note:** For performance optimization, current implementation uses simplified ranking. Full weighted ranking is available in `getAllStudentsRanking()` method in AnalyzerDAO.

---

---

## 7. Methods Reference

### 7.1 Constructor Methods

#### `public StudentAnalyzer(JFrame parent, HashMap<String, List<Student>> sectionStudents)`

**Purpose:** Primary constructor for StudentAnalyzer panel

**Parameters:**
- `parent` (JFrame): Parent frame for dialog positioning and callbacks
- `sectionStudents` (HashMap<String, List<Student>>): Pre-loaded section-student mappings

**Usage:**
```java
HashMap<String, List<Student>> sectionStudents = loadSectionsFromDatabase();
StudentAnalyzer analyzer = new StudentAnalyzer(parentFrame, sectionStudents);
```

**Behavior:**
- Initializes UI components
- Sets up FlatLaf modern theme
- Configures arc radius for rounded corners
- Loads current user ID from LoginScreen

---

#### `public StudentAnalyzer(JFrame parent, HashMap<String, List<Student>> sectionStudents, Runnable onCloseCallback)`

**Purpose:** Constructor with close callback support

**Parameters:**
- `parent` (JFrame): Parent frame reference
- `sectionStudents` (HashMap<String, List<Student>>): Section data
- `onCloseCallback` (Runnable): Callback executed when analyzer closes

**Usage:**
```java
StudentAnalyzer analyzer = new StudentAnalyzer(
    parentFrame, 
    sectionStudents,
    () -> {
        // Cleanup or navigation logic
        returnToDashboard();
    }
);
```

---

### 7.2 UI Initialization Methods

#### `private void initializeUI()`

**Purpose:** Initialize main user interface layout and components

**Key Operations:**
1. Creates header card with title and radio buttons
2. Builds main content area
3. Sets up background colors and padding
4. Registers radio button listeners for view switching

**Components Created:**
- Header panel with "Student" and "Section" radio buttons
- Main content panel with card layout
- Input card for student selection
- Filter card for subject/exam type selection
- Analysis results panel

---

#### `private void createMainContent()`

**Purpose:** Build the primary content area with input form

**Structure Created:**
```
Main Content Panel
├── Input Card
│   ├── Section Dropdown
│   ├── Student Name Field (with autocomplete)
│   ├── Roll Number Field
│   └── Analyze Button
└── Results Area (initially hidden)
```

**Autocomplete Feature:**
```java
studentNameField.addKeyListener(new KeyAdapter() {
    public void keyReleased(KeyEvent e) {
        String input = studentNameField.getText().trim();
        if (input.length() > 0) {
            List<String> suggestions = getMatchingStudents(input);
            showSuggestionsPopup(suggestions);
        }
    }
});
```

---

#### `private void createStudentAnalysisLayout()`

**Purpose:** Create the results display panel (shown after analysis)

**Layout:**
```
Analysis Layout
├── Analysis Panel (Metric Cards)
│   ├── SGPA Card
│   ├── Percentage Card
│   ├── Rank Card
│   ├── Subjects Card
│   ├── Exams Card
│   ├── Grade Card
│   └── Status Card
├── Export Button
└── Subject Performance Table
```

**Visibility Control:**
- Initially hidden
- Shown after "Calculate Analysis" is clicked
- Contains scrollable table with hierarchical headers

---

### 7.3 Analysis Methods

#### `private void analyzeStudent()`

**Purpose:** Main entry point for student analysis workflow

**Flow:**
```
1. Validate Inputs
   ├── Check student name not empty
   ├── Check roll number not empty
   └── Check section selected

2. Find Student
   ├── Search in sectionStudents map
   ├── Match by roll number
   └── Set as currentStudent

3. Load Database ID
   ├── Query students table
   ├── Retrieve primary key
   └── Set student.id

4. Build Filter Tree
   ├── Extract subjects from student.marks
   ├── Extract exam types per subject
   └── Create tree structure with checkboxes

5. Show Filter Card
   ├── Hide input card
   ├── Display filter tree
   └── Show "Calculate Analysis" button
```

**Error Handling:**
```java
if (student == null) {
    JOptionPane.showMessageDialog(this, 
        "❌ Student not found in selected section!",
        "Error", 
        JOptionPane.ERROR_MESSAGE);
    return;
}
```

---

#### `private void calculateAnalysis()`

**Purpose:** Perform weighted calculations and display results

**Process:**
```java
// 1. Extract selected filters
Map<String, Set<String>> selectedFilters = extractSelectedFilters(filterTree);

// 2. Get section ID
int studentId = currentStudent.getId();
int sectionId = getStudentSectionId(studentId);

// 3. Call DAO method
AnalyzerDAO dao = new AnalyzerDAO();
DetailedRankingData rankingData = dao.getDetailedStudentRanking(
    sectionId, 
    selectedFilters
);

// 4. Calculate metrics
double sgpa = rankingData.percentage / 10.0;
double percentage = rankingData.percentage;
String grade = getLetterGrade(percentage);
int rank = calculateStudentRank(studentId);

// 5. Update UI
updateAnalysisPanel(sgpa, percentage, grade, subjectCount, examCount, rank);
populateSubjectPerformanceTable(rankingData);
```

**Calculation Details:**
- Calls `getDetailedStudentRanking()` which uses `calculateWeightedSubjectTotalWithPass()`
- Applies DUAL PASSING requirement
- Handles negative percentages (failure cases)

---

#### `private Map<String, Set<String>> extractSelectedFilters(JTree tree)`

**Purpose:** Parse tree checkboxes to extract selected subjects and exam types

**Algorithm:**
```java
Map<String, Set<String>> filters = new HashMap<>();
TreeModel model = tree.getModel();
DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

for (int i = 0; i < root.getChildCount(); i++) {
    DefaultMutableTreeNode subjectNode = (DefaultMutableTreeNode) root.getChildAt(i);
    
    if (isNodeChecked(subjectNode)) {
        String subject = subjectNode.getUserObject().toString();
        Set<String> examTypes = new HashSet<>();
        
        for (int j = 0; j < subjectNode.getChildCount(); j++) {
            DefaultMutableTreeNode examNode = (DefaultMutableTreeNode) subjectNode.getChildAt(j);
            
            if (isNodeChecked(examNode)) {
                examTypes.add(examNode.getUserObject().toString());
            }
        }
        
        if (!examTypes.isEmpty()) {
            filters.put(subject, examTypes);
        }
    }
}

return filters;
```

**Return Structure:**
```
{
    "Mathematics": ["Internal 1", "Internal 2", "External"],
    "Physics": ["Mid Term", "End Term"],
    "Chemistry": ["Internal 1", "External"]
}
```

---

#### `private void updateAnalysisPanel(double sgpa, double percentage, String letterGrade, int subjectCount, int examCount, int studentRank)`

**Purpose:** Populate metric cards with calculated values

**Parameters:**
- `sgpa` (double): Student Grade Point Average (0-10)
- `percentage` (double): Overall percentage (0-100)
- `letterGrade` (String): A+, A, B+, ..., F
- `subjectCount` (int): Number of subjects analyzed
- `examCount` (int): Total exam types across all subjects
- `studentRank` (int): Position in section

**Card Creation:**
```java
metricsGrid.add(createLargeMetricCard("🎯", "SGPA", 
    String.format("%.2f", sgpa), SUCCESS_COLOR));
metricsGrid.add(createLargeMetricCard("📈", "Percentage", 
    String.format("%.1f%%", percentage), PRIMARY_COLOR));
metricsGrid.add(createLargeMetricCard("🏆", "Rank", 
    String.valueOf(studentRank), new Color(234, 179, 8)));
metricsGrid.add(createLargeMetricCard("🎓", "Grade", 
    letterGrade, getGradeColor(letterGrade)));
```

---

#### `private int calculateStudentRank(int studentId)`

**Purpose:** Determine student's rank within section

**Current Implementation:** Simplified ranking based on ID order

```java
String query = 
    "SELECT COUNT(*) as rank " +
    "FROM students " +
    "WHERE section_id = (SELECT section_id FROM students WHERE id = ?) " +
    "AND id < ?";

PreparedStatement stmt = conn.prepareStatement(query);
stmt.setInt(1, studentId);
stmt.setInt(2, studentId);
ResultSet rs = stmt.executeQuery();

if (rs.next()) {
    return rs.getInt("rank") + 1;
}
return 1;
```

**Note:** Full weighted ranking available in `AnalyzerDAO.getAllStudentsRanking()` but not used here for performance reasons.

---

#### `private String getLetterGrade(double percentage)`

**Purpose:** Convert percentage to letter grade

**Grade Mapping:**
```java
if (percentage >= 90) return "A+";
else if (percentage >= 85) return "A";
else if (percentage >= 80) return "B+";
else if (percentage >= 75) return "B";
else if (percentage >= 70) return "C+";
else if (percentage >= 65) return "C";
else if (percentage >= 60) return "D+";
else if (percentage >= 50) return "D";
else return "F";
```

---

#### `private Color getGradeColor(String grade)`

**Purpose:** Assign color coding to grades for visual hierarchy

**Color Scheme:**
```java
switch(grade) {
    case "A+": case "A": 
        return new Color(34, 197, 94);      // Green
    case "B+": case "B": 
        return new Color(34, 197, 94);      // Green
    case "C+": case "C": 
        return new Color(251, 146, 60);     // Orange
    case "D+": case "D": 
        return new Color(251, 146, 60);     // Orange
    default: 
        return new Color(239, 68, 68);      // Red
}
```

---

### 7.4 Database Helper Methods

#### `private int getStudentSectionId(int studentId)`

**Purpose:** Retrieve section ID for a student from database

**Query:**
```sql
SELECT section_id FROM students WHERE id = ?
```

**Returns:** Section ID (int) or -1 if not found

**Usage:**
```java
int sectionId = getStudentSectionId(currentStudent.getId());
if (sectionId == -1) {
    // Handle error
}
```

---

#### `private int fetchStudentIdFromDB(String rollNumber)`

**Purpose:** Get student's database primary key from roll number

**Query:**
```sql
SELECT id FROM students WHERE roll_number = ? AND user_id = ?
```

**Returns:** Student ID (int) or -1 if not found

---

#### `private Student findStudentInSection(String sectionName, String rollNumber)`

**Purpose:** Search in-memory sectionStudents map for student

**Algorithm:**
```java
List<Student> students = sectionStudents.get(sectionName);
if (students == null) return null;

for (Student student : students) {
    if (student.getRollNumber().equals(rollNumber)) {
        return student;
    }
}
return null;
```

**Efficiency:** O(n) where n = students in section

---

### 7.5 UI Helper Methods

#### `private JPanel createLargeMetricCard(String icon, String title, String value, Color color)`

**Purpose:** Create visual metric card with icon, title, and value

**Parameters:**
- `icon` (String): Emoji icon (e.g., "🎯", "📈")
- `title` (String): Card label (e.g., "SGPA", "Percentage")
- `value` (String): Displayed value (e.g., "9.46", "94.6%")
- `color` (Color): Border and value text color

**Layout:**
```
┌─ color border (3px left)
│  🎯  [icon]
│  SGPA [title - gray]
│  9.46 [value - colored, bold]
└─
```

**Dimensions:** 110×80 pixels

---

#### `private JButton createModernButton(String text)`

**Purpose:** Create styled button with gradient and rounded corners

**Features:**
- Gradient background (PRIMARY_COLOR → PRIMARY_DARK)
- 15px border radius
- Hand cursor on hover
- White text, bold font
- Pressed state animation

**Style:**
```java
button.setFont(new Font("SansSerif", Font.BOLD, 16));
button.setForeground(Color.WHITE);
button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
button.setCursor(new Cursor(Cursor.HAND_CURSOR));
```

---

#### `private JTextField createModernTextField()`

**Purpose:** Create rounded text field with custom styling

**Features:**
- 10px border radius
- Light gray background (#F9FAFB)
- Custom border color (#E5E7EB)
- 15px horizontal padding
- 40px height

---

#### `private JRadioButton createModernRadioButton(String text, boolean selected)`

**Purpose:** Create styled radio button with custom icons

**Custom Icons:**
- Unselected: Purple circle outline
- Selected: Purple filled circle with inner dot

**Icon Rendering:**
```java
radio.setIcon(new Icon() {
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(147, 51, 234, 120));
        g2.drawOval(x + 1, y + 1, 18, 18);
        g2.dispose();
    }
    public int getIconWidth() { return 22; }
    public int getIconHeight() { return 22; }
});
```

---

### 7.6 Navigation Methods

#### `private void showSectionAnalyzer(HashMap<String, ArrayList<Student>> sectionMap)`

**Purpose:** Switch view from Student Analyzer to Section Analyzer

**Process:**
1. Create SectionAnalyzer instance
2. Pass section data and callback
3. Remove current panel from parent
4. Add SectionAnalyzer panel
5. Revalidate and repaint parent

**Code:**
```java
SectionAnalyzer sectionAnalyzer = new SectionAnalyzer(
    parentFrame, 
    sectionMap, 
    () -> {
        // Return to Student Analyzer
        parentFrame.getContentPane().removeAll();
        parentFrame.getContentPane().add(this);
        parentFrame.revalidate();
        parentFrame.repaint();
    }
);

parentFrame.getContentPane().removeAll();
parentFrame.getContentPane().add(sectionAnalyzer);
parentFrame.revalidate();
parentFrame.repaint();
```

---

## 8. PDF Export System

### 8.1 Export Method Overview

#### `private void exportToPDF(double sgpa, double percentage, String grade, int totalObtained, int totalMax, int subjectCount, int examCount)`

**Purpose:** Generate comprehensive academic performance report in PDF format

**Parameters:**
- `sgpa` (double): Student Grade Point Average
- `percentage` (double): Overall percentage
- `grade` (String): Letter grade
- `totalObtained` (int): Total marks obtained
- `totalMax` (int): Total maximum marks
- `subjectCount` (int): Number of subjects
- `examCount` (int): Total exam types

---

### 8.2 PDF Structure

**Document Layout:**
```
┌─────────────────────────────────────┐
│  📊 Academic Performance Report     │  [Title - 24pt, centered, blue]
├─────────────────────────────────────┤
│  👤 Student Information             │  [Section Header - 16pt]
│  ┌───────────────────────────────┐  │
│  │ Name:        [Student Name]   │  │
│  │ Roll Number: [Roll Number]    │  │
│  │ Section:     [Section Name]   │  │
│  │ Rank:        [Rank]           │  │
│  │ Report Date: [Date & Time]    │  │
│  └───────────────────────────────┘  │
├─────────────────────────────────────┤
│  📈 Performance Metrics             │
│  ┌──────┬──────┬──────┬──────┐    │
│  │Total │ SGPA │  %   │Grade │    │
│  │850/  │ 9.46 │94.6% │ A+   │    │
│  │1000  │/10.00│      │      │    │
│  └──────┴──────┴──────┴──────┘    │
├─────────────────────────────────────┤
│  📋 Detailed Marks Breakdown        │
│  [Hierarchical Table]               │
├─────────────────────────────────────┤
│  📊 Performance Summary             │
│  [Paragraph description]            │
├─────────────────────────────────────┤
│  📌 Grading System                  │
│  [Color-coded grade legend]         │
└─────────────────────────────────────┘
```

---

### 8.3 PDF Components

#### Student Information Table

**Implementation:**
```java
PdfPTable infoTable = new PdfPTable(2);
infoTable.setWidthPercentage(100);
infoTable.setWidths(new float[]{1.2f, 2f});

addModernPdfTableRow(infoTable, "Name:", currentStudent.getName(), 
    boldFont, normalFont, headerBg);
addModernPdfTableRow(infoTable, "Roll Number:", currentStudent.getRollNumber(), 
    boldFont, normalFont, headerBg);
addModernPdfTableRow(infoTable, "Section:", currentStudent.getSection(), 
    boldFont, normalFont, headerBg);
addModernPdfTableRow(infoTable, "Rank:", String.valueOf(rank), 
    boldFont, normalFont, headerBg);
```

**Styling:**
- 2 columns: Label (bold) | Value (normal)
- Column widths: 1.2:2 ratio
- Background: Light blue for labels, white for values
- No borders, 10px padding

---

#### Performance Metrics Cards

**Implementation:**
```java
PdfPTable metricsTable = new PdfPTable(4);
metricsTable.setWidthPercentage(100);

addMetricCard(metricsTable, "Total", 
    String.format("%.0f / %d", actualTotal, subjectCount * 100), 
    primaryColor);
addMetricCard(metricsTable, "SGPA", 
    String.format("%.2f / 10.00", sgpa), 
    successColor);
addMetricCard(metricsTable, "Percentage", 
    String.format("%.2f%%", percentage), 
    purpleColor);
addMetricCard(metricsTable, "Grade", 
    grade, 
    orangeColor);
```

**Card Design:**
- 4 columns (equal width)
- Each card: Title (small gray) + Value (large colored)
- Light gray background
- Colored border (2px)

---

#### Detailed Marks Breakdown Table

**Structure:**
```
┌─────────┬──────────┬──────────┬──────────┬─────────┬───────┐
│ Subject │ Internal1│ Internal2│ External │ Weighted│ Grade │
│         │   (20)   │   (20)   │   (60)   │  Total  │       │
├─────────┼──────────┼──────────┼──────────┼─────────┼───────┤
│  Math   │    18    │    19    │    58    │  94.64  │  A+   │
│ Physics │    17    │    18    │    55    │  92.33  │  A+   │
├─────────┼──────────┼──────────┼──────────┼─────────┼───────┤
│  TOTAL  │    35    │    37    │   113    │ 187.00  │  A+   │
└─────────┴──────────┴──────────┴──────────┴─────────┴───────┘
```

**Implementation:**
```java
// Create dynamic column count
int columnCount = 2 + allExamTypes.size() + 1;
PdfPTable marksTable = new PdfPTable(columnCount);

// Header row
addModernPdfTableCell(marksTable, "Subject", tableHeaderFont, primaryColor, true);
for (String examType : allExamTypes) {
    addModernPdfTableCell(marksTable, examType, tableHeaderFont, primaryColor, true);
}
addModernPdfTableCell(marksTable, "Weighted Total", tableHeaderFont, primaryColor, true);
addModernPdfTableCell(marksTable, "Grade", tableHeaderFont, primaryColor, true);

// Data rows
for (Map.Entry<String, Map<String, Integer>> entry : marks.entrySet()) {
    String subject = entry.getKey();
    
    // Calculate weighted total using DAO
    SubjectPassResult result = analyzerDAO.calculateWeightedSubjectTotalWithPass(
        studentId, sectionId, subject, selectedExams
    );
    
    addModernPdfTableCell(marksTable, subject, tableCellBoldFont, rowBg, false);
    
    // Add exam marks
    for (String examType : allExamTypes) {
        Integer mark = entry.getValue().get(examType);
        String displayValue = (mark != null) ? String.valueOf(mark) : "-";
        addModernPdfTableCell(marksTable, displayValue, tableCellFont, rowBg, false);
    }
    
    // Weighted total
    addModernPdfTableCell(marksTable, 
        String.format("%.2f / 100", result.percentage), 
        tableCellBoldFont, rowBg, false);
    
    // Grade
    String subjectGrade = getLetterGrade(result.percentage);
    addModernPdfTableCell(marksTable, subjectGrade, tableCellBoldFont, rowBg, false);
}
```

**Features:**
- Alternating row colors (white/light gray)
- Centered alignment
- Bold fonts for subject name and totals
- TOTAL row with gray background

---

#### Performance Summary Text

**Implementation:**
```java
String performanceText = String.format(
    "The student has achieved an overall weighted total of %.0f out of %d marks " +
    "across %d subject(s), resulting in a percentage of %.2f%% and a SGPA of %.2f. " +
    "The student's performance is graded as '%s'. This report uses weighted calculation " +
    "where each exam type contributes its actual weightage percentage to the final score " +
    "(no normalization). The rank is %d in the section.",
    grandWeightedTotal, subjectCount * 100, subjectCount, percentage, sgpa, grade, rank
);

Paragraph summaryText = new Paragraph();
summaryText.setFont(normalFont);
summaryText.setAlignment(Element.ALIGN_JUSTIFIED);
summaryText.add(performanceText);
```

**Purpose:** Provide narrative explanation of performance metrics and calculation methodology

---

#### Grading System Legend

**Implementation:**
```java
PdfPTable legendTable = new PdfPTable(8);
legendTable.setWidthPercentage(100);

String[] grades = {"A+", "A", "B+", "B", "C+", "C", "D", "F"};
String[] ranges = {"90-100", "85-89", "80-84", "75-79", "70-74", "65-69", "50-64", "< 50"};
BaseColor[] gradeColors = {
    new BaseColor(34, 197, 94),      // Green - A+
    new BaseColor(59, 130, 246),     // Blue - A
    new BaseColor(139, 92, 246),     // Purple - B+
    new BaseColor(249, 115, 22),     // Orange - B
    new BaseColor(236, 72, 153),     // Pink - C+
    new BaseColor(251, 191, 36),     // Yellow - C
    new BaseColor(148, 163, 184),    // Gray - D
    new BaseColor(239, 68, 68)       // Red - F
};

for (int i = 0; i < grades.length; i++) {
    PdfPCell cell = new PdfPCell();
    cell.setBackgroundColor(gradeColors[i]);
    cell.setPadding(8);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    
    Phrase phrase = new Phrase();
    phrase.add(new Chunk(grades[i] + "\n", gradeFont));
    phrase.add(new Chunk(ranges[i] + "%", rangeFont));
    
    cell.setPhrase(phrase);
    legendTable.addCell(cell);
}
```

**Layout:**
```
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ A+ │ A  │ B+ │ B  │ C+ │ C  │ D  │ F  │
│90-│85-│80-│75-│70-│65-│50-│ < │
│100│89 │84 │79 │74 │69 │64 │50 │
└────┴────┴────┴────┴────┴────┴────┴────┘
```

---

### 8.4 PDF Helper Methods

#### `private void addModernPdfTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, BaseColor bgColor)`

**Purpose:** Add styled 2-column row to information table

**Features:**
- No borders
- 10px padding
- Custom background colors
- Different fonts for label and value

---

#### `private void addMetricCard(PdfPTable table, String title, String value, BaseColor color)`

**Purpose:** Create metric card cell for metrics table

**Layout:**
```
┌───────────────┐
│ Title (small) │
│ VALUE (large) │
└───────────────┘
```

**Styling:**
- Title: Gray, 9pt
- Value: Colored, bold, 14pt
- Background: Light gray (#F9FAFB)
- Border: 2px colored
- Centered alignment

---

#### `private void addModernPdfTableCell(PdfPTable table, String text, Font font, BaseColor bgColor, boolean isHeader)`

**Purpose:** Add single cell to marks breakdown table

**Parameters:**
- `isHeader` (boolean): If true, no border; if false, light gray border

**Behavior:**
```java
PdfPCell cell = new PdfPCell(new Phrase(text, font));
cell.setPadding(8);
cell.setHorizontalAlignment(Element.ALIGN_CENTER);
cell.setBackgroundColor(bgColor);

if (isHeader) {
    cell.setBorder(Rectangle.NO_BORDER);
} else {
    cell.setBorderColor(new BaseColor(229, 231, 235));
    cell.setBorderWidth(0.5f);
}

table.addCell(cell);
```

---

### 8.5 File Export Flow

**User Interaction:**
```
1. User clicks "📄 Export PDF Report" button
   ↓
2. JFileChooser dialog opens
   - Default name: [StudentName]_Report.pdf
   - File filter: PDF files only
   ↓
3. User selects location and clicks Save
   ↓
4. PDF generation process:
   - Create Document (A4 size)
   - Add title and headers
   - Populate information table
   - Add metric cards
   - Generate marks breakdown
   - Add summary text
   - Append grading legend
   ↓
5. Document closed and saved
   ↓
6. Success message shown with file path
```

**Error Handling:**
```java
try {
    // PDF generation code
    document.close();
    
    JOptionPane.showMessageDialog(this, 
        "✅ Report exported successfully!\n\n" + filePath,
        "Export Success", 
        JOptionPane.INFORMATION_MESSAGE);
        
} catch (Exception e) {
    e.printStackTrace();
    JOptionPane.showMessageDialog(this, 
        "❌ Failed to export report: " + e.getMessage(),
        "Export Error", 
        JOptionPane.ERROR_MESSAGE);
}
```

---

## 9. Integration Points

### 9.1 Dashboard Integration

**Entry Point:** DashboardScreen.java

**Navigation Flow:**
```
Dashboard
├── Student Radio Button Click
│   └── Opens StudentAnalyzer panel
└── Section Radio Button Click
    └── Opens SectionAnalyzer panel (comparison view)
```

**Code Integration:**
```java
// In DashboardScreen.java
studentRadio.addActionListener(e -> {
    // Load section-student mappings
    HashMap<String, List<Student>> sectionStudents = loadSectionsFromDatabase();
    
    // Create StudentAnalyzer
    StudentAnalyzer analyzer = new StudentAnalyzer(
        parentFrame, 
        sectionStudents,
        () -> {
            // Callback to return to dashboard
            returnToDashboard();
        }
    );
    
    // Switch view
    mainPanel.removeAll();
    mainPanel.add(analyzer, BorderLayout.CENTER);
    mainPanel.revalidate();
    mainPanel.repaint();
});
```

---

### 9.2 Database Integration

**Primary DAO:** AnalyzerDAO.java

**Key Methods Used:**

1. **getDetailedStudentRanking(int sectionId, Map<String, Set<String>> selectedFilters)**
   - Returns: DetailedRankingData with percentage, CGPA, subject breakdowns
   - Used by: calculateAnalysis()

2. **calculateWeightedSubjectTotalWithPass(int studentId, int sectionId, String subjectName, Set<String> selectedExamTypes)**
   - Returns: SubjectPassResult with percentage and pass/fail status
   - Used by: PDF export for subject totals

3. **getAllStudentsRanking(int sectionId, Set<String> selectedSubjects, Map<String, Set<String>> selectedExamTypes)**
   - Returns: List of StudentRankingData sorted by performance
   - Used by: Section comparison view

**Database Connections:**
```java
Connection conn = DatabaseConnection.getConnection();
```

**Connection Pooling:** Single connection reused throughout session

---

### 9.3 Section Analyzer Integration

**Bidirectional Navigation:**

```
StudentAnalyzer ↔ SectionAnalyzer
```

**From Student to Section:**
```java
sectionRadio.addActionListener(e -> {
    // Convert data format
    HashMap<String, ArrayList<Student>> sectionMap = convertToSectionMap();
    
    // Show section analyzer
    showSectionAnalyzer(sectionMap);
});
```

**From Section to Student:**
```java
studentRadio.addActionListener(e -> {
    // Call onCloseCallback to return
    if (onCloseCallback != null) {
        onCloseCallback.run();
    }
});
```

**Data Sharing:**
- Both use same sectionStudents data structure
- Both call same DAO methods for calculations
- Ensures consistency between views

---

### 9.4 Theme Integration

**FlatLaf Setup:**
```java
FlatLightLaf.setup();

UIManager.put("Button.arc", 15);
UIManager.put("Component.arc", 15);
UIManager.put("TextField.arc", 15);
UIManager.put("Panel.arc", 15);
```

**Color Consistency:**
- All colors defined as static constants
- Shared across StudentAnalyzer and SectionAnalyzer
- Ensures uniform visual experience

---

## 10. Error Handling & Validation

### 10.1 Input Validation

**Empty Field Checks:**
```java
if (studentName.trim().isEmpty() || rollNumber.trim().isEmpty()) {
    JOptionPane.showMessageDialog(this,
        "⚠️ Please enter both student name and roll number",
        "Validation Error",
        JOptionPane.WARNING_MESSAGE);
    return;
}
```

**Section Selection:**
```java
if (selectedSection == null || selectedSection.isEmpty()) {
    JOptionPane.showMessageDialog(this,
        "⚠️ Please select a section first",
        "Validation Error",
        JOptionPane.WARNING_MESSAGE);
    return;
}
```

**Student Not Found:**
```java
Student student = findStudentInSection(selectedSection, rollNumber);
if (student == null) {
    JOptionPane.showMessageDialog(this,
        "❌ Student not found in selected section!",
        "Error",
        JOptionPane.ERROR_MESSAGE);
    return;
}
```

---

### 10.2 Database Error Handling

**Connection Failures:**
```java
try {
    Connection conn = DatabaseConnection.getConnection();
    // Database operations
} catch (SQLException e) {
    e.printStackTrace();
    JOptionPane.showMessageDialog(this,
        "❌ Database connection error: " + e.getMessage(),
        "Database Error",
        JOptionPane.ERROR_MESSAGE);
}
```

**Query Failures:**
```java
try {
    PreparedStatement stmt = conn.prepareStatement(query);
    ResultSet rs = stmt.executeQuery();
    // Process results
} catch (SQLException e) {
    System.err.println("Query failed: " + e.getMessage());
    return -1; // Or appropriate default value
}
```

---

### 10.3 PDF Export Error Handling

**File Access Errors:**
```java
catch (FileNotFoundException e) {
    JOptionPane.showMessageDialog(this,
        "❌ Cannot write to selected location. Check permissions.",
        "File Error",
        JOptionPane.ERROR_MESSAGE);
}
```

**Document Creation Errors:**
```java
catch (DocumentException e) {
    JOptionPane.showMessageDialog(this,
        "❌ PDF generation failed: " + e.getMessage(),
        "Export Error",
        JOptionPane.ERROR_MESSAGE);
}
```

---

## 11. Performance Considerations

### 11.1 Optimization Strategies

**Lazy Loading:**
- Sections loaded only when dropdown opened
- Student marks fetched only after selection
- Analysis performed only when "Calculate" clicked

**Caching:**
```java
private Map<String, List<Student>> cachedSectionStudents;
private DetailedRankingData cachedRankingData;
```

**Efficient Queries:**
- Use prepared statements (prevents SQL injection)
- Index on frequently queried columns (student_id, section_id, roll_number)
- Minimize database round-trips

---

### 11.2 Memory Management

**Data Structure Sizes:**
- sectionStudents map: ~100KB per section (typical)
- Student marks map: ~5KB per student (typical)
- PDF generation: ~2MB temporary memory

**Cleanup:**
```java
@Override
public void removeNotify() {
    super.removeNotify();
    // Clear cached data
    cachedSectionStudents = null;
    cachedRankingData = null;
}
```

---

### 11.3 UI Responsiveness

**Background Processing:**
```java
SwingWorker<DetailedRankingData, Void> worker = new SwingWorker<>() {
    @Override
    protected DetailedRankingData doInBackground() {
        return dao.getDetailedStudentRanking(sectionId, selectedFilters);
    }
    
    @Override
    protected void done() {
        try {
            DetailedRankingData data = get();
            updateAnalysisPanel(data);
        } catch (Exception e) {
            handleError(e);
        }
    }
};
worker.execute();
```

**Progress Indicators:**
- Show loading cursor during database operations
- Disable buttons during processing
- Display "Calculating..." message

---

## 12. Future Enhancements

### 12.1 Potential Features

1. **Graphical Analytics**
   - Subject-wise performance radar charts
   - Trend analysis over multiple exams
   - Comparison with section average

2. **Advanced Filtering**
   - Date range selection for exams
   - Subject category grouping
   - Custom weightage adjustments

3. **Export Options**
   - Excel spreadsheet export
   - CSV data export
   - HTML report generation

4. **Real-time Ranking**
   - Live rank calculation using weighted algorithm
   - Section percentile display
   - Historical rank tracking

5. **Recommendation System**
   - Identify weak subjects
   - Suggest improvement strategies
   - Predict future performance

---

## 13. Conclusion

The **Student Analyzer** provides a comprehensive, user-friendly interface for analyzing individual student academic performance with precise weighted calculations, visual metrics, and professional PDF reporting. Its integration with the Section Analyzer creates a complete academic performance management system.

**Key Strengths:**
- Accurate weighted calculation system
- Modern, intuitive UI design
- Flexible subject/exam filtering
- Professional PDF export
- Seamless integration with existing system

**Maintenance Notes:**
- Calculation logic centralized in AnalyzerDAO
- UI components follow consistent design patterns
- Database queries use prepared statements
- Error handling at all critical points

---

**Document Version:** 1.0  
**Last Updated:** January 14, 2026  
**Author:** Technical Documentation Team  
**Related Documents:**
- [Section Analyzer Technical Documentation](SECTION_ANALYZER_TECHNICAL_DOC.md)
- [Marks Calculation Guide](../guides/MARKS_CALCULATION_GUIDE.md)
- [Database Schema Documentation](../database/schema_current_2026-01-11.sql)
