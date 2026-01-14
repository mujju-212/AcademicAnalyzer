# Section Analyzer - Technical Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [UI Components](#ui-components)
4. [Core Functionality](#core-functionality)
5. [Database Schema](#database-schema)
6. [Calculation Logic](#calculation-logic)
7. [Methods Reference](#methods-reference)
8. [Ranking Table System](#ranking-table-system)
9. [PDF Export System](#pdf-export-system)
10. [Integration Points](#integration-points)

---

## 1. Overview

### Purpose
The **Section Analyzer** is a comprehensive section-level performance analysis tool that provides aggregate insights into entire sections of students. It serves as the section-centric counterpart to the Student Analyzer, focusing on collective performance metrics, ranking tables, and comparative analytics across all students in a section.

### Key Features
- **Section-wide Performance Analysis**: Aggregate metrics for entire sections
- **Hierarchical Ranking Table**: 2-row header structure with subjects and exam types
- **Weighted Calculation System**: Uses actual weightage percentages (no normalization)
- **Visual Analytics**: Charts and graphs for performance distribution
- **Top Performers Display**: Leaderboard of top students
- **Subject-wise Analysis**: Breakdown of performance by subject
- **Failed Students Tracking**: Identification of students needing support
- **PDF Export**: Professional section reports with ranking tables
- **Selective Filtering**: Subject and exam type selection for focused analysis

### Technology Stack
- **Language**: Java 8+
- **UI Framework**: Java Swing with FlatLaf modern theme
- **Database**: MySQL
- **Charting**: JFreeChart
- **PDF Generation**: iText 5.x
- **Design Pattern**: MVC with DAO pattern
- **Table Rendering**: Custom hierarchical JTable with merged headers

---

## 2. Architecture

### Class Structure

```
SectionAnalyzer.java (Main Class)
├── Extends: JPanel
├── Dependencies:
│   ├── AnalyzerDAO (Data Access Layer)
│   ├── SectionDAO (Section Data)
│   ├── DatabaseConnection (DB Connectivity)
│   ├── Student (Data Model)
│   └── StudentAnalyzer (Comparison View)
└── Responsibilities:
    ├── UI Rendering (Ranking Table, Charts, Metrics)
    ├── Section Selection & Filtering
    ├── Aggregate Calculations Orchestration
    ├── PDF Report Generation
    └── Data Visualization
```

### Key Data Structures

#### SectionAnalysisData (AnalyzerDAO)
```java
public static class SectionAnalysisData {
    public double averagePercentage;          // Section average (0-100)
    public double averageCGPA;                // Section CGPA (0-10)
    public int totalStudents;                 // Total students in section
    public int passedStudents;                // Students with >= 50%
    public int failedStudents;                // Students with < 50%
    public double passPercentage;             // (passed/total) * 100
    public List<TopStudent> topStudents;      // Top 5 performers
    public List<SubjectAnalysis> subjectAnalysisList;  // Per-subject metrics
    public Map<Integer, Integer> failedStudentsMap;    // studentId -> failedSubjectCount
}
```

#### StudentRankingData (AnalyzerDAO)
```java
public static class StudentRankingData {
    public int studentId;
    public String studentName;
    public String rollNumber;
    public double percentage;                  // Overall percentage
    public double cgpa;                        // CGPA (percentage/10)
    public String grade;                       // A+, A, B+, ..., F
    public int rank;                          // Position in section
    public Map<String, SubjectData> subjectData;  // Subject-wise breakdown
    public Map<String, Map<String, Integer>> examMarks;  // Subject -> ExamType -> Marks
}
```

#### SubjectData
```java
public static class SubjectData {
    public String subjectName;
    public double percentage;                  // Weighted percentage (0-100)
    public String grade;                       // Letter grade
    public Set<String> examTypes;             // Included exam types
    public int maxMarks;                      // Total max marks
}
```

---

## 3. UI Components

### 3.1 Header Section

**Components:**
```
┌──────────────────────────────────────────────────────────┐
│  ← Back (if embedded)                                    │
│                                                           │
│  🎓 Section Performance Analyzer                         │
│  ○ Student   ● Section                                   │
│                                                           │
│  Section: [Dropdown ▼]   [Apply Filter]   [📄 Export]  │
└──────────────────────────────────────────────────────────┘
```

**Key Elements:**
1. **Back Button**: Returns to previous view (if opened from Dashboard/Library)
2. **Title**: "Section Performance Analyzer" with icon
3. **Radio Buttons**: Toggle between Student and Section analysis modes
4. **Section Dropdown**: Select different sections for analysis
5. **Action Buttons**: Apply filters, Export to PDF

**Color Scheme:**
```java
BACKGROUND_COLOR = #F5F7FA (Light gray-blue)
CARD_COLOR = #FFFFFF (White)
PRIMARY_COLOR = #6366F1 (Indigo)
SUCCESS_COLOR = #22C55E (Green)
DANGER_COLOR = #EF4444 (Red)
WARNING_COLOR = #FB9238 (Orange)
```

---

### 3.2 Filter Panel

**Purpose**: Select specific subjects and exam types for focused analysis

**Layout:**
```
┌─────────────────────────────────────────┐
│  📋 Select Subjects & Components        │
│                                          │
│  ☑ Mathematics                          │
│    ☑ Internal 1                         │
│    ☑ Internal 2                         │
│    ☑ External                           │
│                                          │
│  ☑ Physics                              │
│    ☑ Mid Term                           │
│    ☑ End Term                           │
│                                          │
│  [Calculate Analysis]                   │
└─────────────────────────────────────────┘
```

**Features:**
- Tree structure with checkboxes
- Parent checkbox toggles all children
- Selective exam type filtering
- All subjects selected by default
- Real-time filter tracking

**Data Structure:**
```java
private Map<String, Set<String>> selectedFilters;
// {"Mathematics": ["Internal 1", "External"], "Physics": ["Mid Term"]}

private Map<String, Set<String>> availableComponents;
// All possible components loaded from database per section
```

---

### 3.3 Main Content Tabs

**Tab Structure:**
```
┌─────────────────────────────────────────────┐
│  Overview | Ranking | Top Students | Charts│
└─────────────────────────────────────────────┘
```

#### Tab 1: Overview
```
┌───────────────────────────────────────────┐
│  Section Metrics (4 cards)                │
│  ┌──────┬──────┬──────┬──────┐          │
│  │Avg % │ Pass │Total │ CGPA │          │
│  │94.6% │ 98%  │  50  │ 9.46 │          │
│  └──────┴──────┴──────┴──────┘          │
│                                           │
│  Performance Distribution Chart           │
│  [Bar Chart]                              │
│                                           │
│  Subject-wise Analysis Table              │
│  [Table with avg %, pass rate]           │
└───────────────────────────────────────────┘
```

#### Tab 2: Ranking (Hierarchical Table)
```
┌─────────────────────────────────────────────────────────┐
│                Mathematics (100)         Physics (100)  │
│  ┌─────────┬─────────┬──────┬─────────┬─────────┬─────┐
│  │Internal1│Internal2│External│ Total │Mid Term │Total│
│  │  (20)   │  (20)   │ (60)  │       │  (50)   │     │
│  ├─────────┼─────────┼───────┼────────┼─────────┼─────┤
│  │   18    │   19    │  58   │ 94.64  │   45    │92.0 │
│  │   17    │   18    │  55   │ 92.33  │   44    │90.0 │
│  └─────────┴─────────┴───────┴────────┴─────────┴─────┘
│                                                          │
│  Overall % | CGPA | Grade | Rank                        │
│    93.5%   │ 9.35 │  A+   │  1                         │
└─────────────────────────────────────────────────────────┘
```

**Key Features:**
- 2-row hierarchical headers
- Subject name spans multiple exam columns
- Exam types with actual max marks in brackets
- Subject totals (weighted calculation)
- Overall metrics columns (%, CGPA, Grade, Rank)
- Sortable by any column
- Hover highlighting
- Right-click context menu

#### Tab 3: Top Students
```
┌──────────────────────────────────────┐
│  🏆 Top 5 Students                   │
│  ┌────────────────────────────────┐ │
│  │ Rank | Name | Roll | % | Grade│ │
│  ├────────────────────────────────┤ │
│  │  1   │ John │ 101  │95%│  A+  │ │
│  │  2   │ Jane │ 102  │93%│  A+  │ │
│  │  3   │ Mike │ 103  │91%│  A+  │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘
```

#### Tab 4: Charts
```
┌──────────────────────────────────────┐
│  Grade Distribution                  │
│  [Bar Chart: A+, A, B+, ...]        │
│                                       │
│  Pass/Fail Distribution              │
│  [Pie Chart or Bar Chart]            │
└──────────────────────────────────────┘
```

---

### 3.4 Ranking Table Details

**Hierarchical Header Structure:**

**Row 1 (Subject Headers):**
- Subject name with total marks in parentheses
- Spans multiple columns (one per exam type + total)
- Example: "Mathematics (100)" spans 4 columns (3 exams + 1 total)

**Row 2 (Exam Type Headers):**
- Individual exam type names with max marks
- One column per exam type
- Total column for subject
- Example: "Internal 1 (20)", "Internal 2 (20)", "External (60)", "Total"

**Data Rows:**
- One row per student
- Marks for each exam type
- Weighted total per subject
- Overall percentage, CGPA, grade, rank

**Column Structure Example:**
```
Column Index | Header Row 1        | Header Row 2      | Data
─────────────┼────────────────────┼──────────────────┼──────
0            | "Rank"             | "Rank"            | 1, 2, 3...
1            | "Roll No"          | "Roll No"         | 101, 102...
2            | "Name"             | "Name"            | John, Jane...
3            | "Mathematics (100)" [spans 4 cols]                    
             |                    | "Internal 1 (20)" | 18, 17...
4            |                    | "Internal 2 (20)" | 19, 18...
5            |                    | "External (60)"   | 58, 55...
6            |                    | "Total"           | 94.64, 92.33...
7            | "Physics (100)" [spans 3 cols]
             |                    | "Mid Term (50)"   | 45, 44...
8            |                    | "End Term (50)"   | 47, 46...
9            |                    | "Total"           | 92.0, 90.0...
10           | "Overall %"        | "Overall %"       | 93.5, 91.2...
11           | "CGPA"             | "CGPA"            | 9.35, 9.12...
12           | "Grade"            | "Grade"           | A+, A+...
```

**Implementation:**
- Custom TableCellRenderer for merged headers
- TableColumnModel for column management
- Fixed header rows (non-scrollable)
- Data rows scrollable
- Column width auto-sizing based on content

---

## 4. Core Functionality

### 4.1 Section Selection & Loading

**Flow Diagram:**
```
Application Start
    ↓
Load Sections from DB (by userId)
    ↓
Populate Section Dropdown
    ↓
Select Default Section (first or specified)
    ↓
Load Available Components for Section
    ↓
Initialize Filters (all selected by default)
    ↓
Display Ranking Table
```

**Load Sections Method:**
```java
private void loadSectionsFromDatabase() {
    SectionDAO sectionDAO = new SectionDAO();
    List<SectionDAO.SectionInfo> sections = sectionDAO.getSectionsByUser(
        com.sms.login.LoginScreen.currentUserId
    );
    
    this.availableSections = sections;
    
    if (!sections.isEmpty()) {
        // Find matching section or use first
        this.currentSectionId = sections.get(0).id;
        this.currentSectionName = sections.get(0).sectionName;
        
        // Load components and initialize filters
        loadAvailableComponentsForSection(this.currentSectionId);
        initializeFilters();
    }
}
```

---

### 4.2 Component Loading

**Purpose:** Load all available exam types (components) for a section from database

**Query Path:**
1. Try `marking_components` table (new system with schemes)
2. Fallback to `exam_types` table (legacy system)

**Implementation:**
```java
private void loadAvailableComponentsForSection(int sectionId) {
    this.availableComponents = new LinkedHashMap<>();
    
    Connection conn = DatabaseConnection.getConnection();
    
    // Get all subjects for this section
    String subjectQuery = 
        "SELECT DISTINCT s.subject_name, s.id " +
        "FROM section_subjects ss " +
        "JOIN subjects s ON ss.subject_id = s.id " +
        "WHERE ss.section_id = ?";
    
    PreparedStatement ps1 = conn.prepareStatement(subjectQuery);
    ps1.setInt(1, sectionId);
    ResultSet rs1 = ps1.executeQuery();
    
    while (rs1.next()) {
        String subjectName = rs1.getString("subject_name");
        int subjectId = rs1.getInt("id");
        Set<String> components = new LinkedHashSet<>();
        
        // Try new system (marking_components)
        String componentQuery = 
            "SELECT mc.component_name " +
            "FROM marking_components mc " +
            "JOIN marking_schemes ms ON mc.scheme_id = ms.id " +
            "WHERE ms.section_id = ? AND ms.subject_id = ? AND ms.is_active = 1";
        
        PreparedStatement ps2 = conn.prepareStatement(componentQuery);
        ps2.setInt(1, sectionId);
        ps2.setInt(2, subjectId);
        ResultSet rs2 = ps2.executeQuery();
        
        while (rs2.next()) {
            components.add(rs2.getString("component_name"));
        }
        
        // If no components found, try legacy system
        if (components.isEmpty()) {
            String legacyQuery = 
                "SELECT DISTINCT et.exam_name " +
                "FROM exam_types et " +
                "WHERE et.section_id = ?";
            
            PreparedStatement ps3 = conn.prepareStatement(legacyQuery);
            ps3.setInt(1, sectionId);
            ResultSet rs3 = ps3.executeQuery();
            
            while (rs3.next()) {
                components.add(rs3.getString("exam_name"));
            }
        }
        
        availableComponents.put(subjectName, components);
    }
}
```

---

### 4.3 Filter Initialization

**Purpose:** Set all subjects and components as selected by default

**Implementation:**
```java
private void initializeFilters() {
    selectedFilters = new LinkedHashMap<>();
    
    // Copy all available components to selected filters
    for (Map.Entry<String, Set<String>> entry : availableComponents.entrySet()) {
        String subject = entry.getKey();
        Set<String> components = new LinkedHashSet<>(entry.getValue());
        selectedFilters.put(subject, components);
    }
}
```

**Filter Application:**
When user changes filter selection:
1. Update `selectedFilters` map
2. Call `refreshDataOnly()` to recalculate
3. Update ranking table with filtered data

---

### 4.4 Ranking Table Creation

**Main Method:** `createRankingTableFromDAO()`

**Process:**
```
1. Get student ranking data from DAO
   ├── Call: getAllStudentsRanking(sectionId, selectedSubjects, selectedFilters)
   ├── Returns: List<StudentRankingData> sorted by rank
   └── Includes: All student data, subject data, exam marks

2. Extract unique exam types per subject
   ├── Iterate through all students
   ├── Collect exam types for each subject
   └── Store in Map<String, Set<String>>

3. Build column structure
   ├── Fixed columns: Rank, Roll No, Name
   ├── Subject columns: For each subject
   │   ├── Exam type columns (one per exam)
   │   └── Total column (weighted)
   ├── Overall columns: %, CGPA, Grade
   └── Calculate total column count

4. Create table with custom renderer
   ├── Set up TableColumnModel
   ├── Configure column widths
   ├── Apply hierarchical header renderer
   └── Set data cell renderer

5. Populate data rows
   ├── One row per student
   ├── Rank, roll number, name
   ├── Marks for each exam type
   ├── Weighted totals per subject
   ├── Overall percentage, CGPA, grade
   └── Handle missing data (show "-")

6. Apply styling
   ├── Alternating row colors
   ├── Header styling
   ├── Centered alignment
   └── Font and padding
```

**Code Example:**
```java
private JScrollPane createRankingTableFromDAO() {
    // 1. Get ranking data
    AnalyzerDAO dao = new AnalyzerDAO();
    Set<String> selectedSubjects = selectedFilters.keySet();
    
    List<AnalyzerDAO.StudentRankingData> rankings = 
        dao.getAllStudentsRanking(currentSectionId, selectedSubjects, selectedFilters);
    
    if (rankings.isEmpty()) {
        return createEmptyTable();
    }
    
    // 2. Extract exam types per subject
    Map<String, Set<String>> subjectExamTypes = new LinkedHashMap<>();
    for (String subject : selectedSubjects) {
        Set<String> examTypes = new LinkedHashSet<>();
        for (AnalyzerDAO.StudentRankingData student : rankings) {
            if (student.examMarks.containsKey(subject)) {
                examTypes.addAll(student.examMarks.get(subject).keySet());
            }
        }
        subjectExamTypes.put(subject, examTypes);
    }
    
    // 3. Build column structure
    List<String> columnNames = new ArrayList<>();
    columnNames.add("Rank");
    columnNames.add("Roll No");
    columnNames.add("Name");
    
    int totalCols = 3; // Fixed columns
    for (Map.Entry<String, Set<String>> entry : subjectExamTypes.entrySet()) {
        totalCols += entry.getValue().size(); // Exam type columns
        totalCols++; // Total column
    }
    totalCols += 3; // Overall %, CGPA, Grade
    
    // 4. Create table data
    Object[][] data = new Object[rankings.size()][totalCols];
    
    for (int i = 0; i < rankings.size(); i++) {
        AnalyzerDAO.StudentRankingData student = rankings.get(i);
        int col = 0;
        
        // Fixed columns
        data[i][col++] = student.rank;
        data[i][col++] = student.rollNumber;
        data[i][col++] = student.studentName;
        
        // Subject columns
        for (Map.Entry<String, Set<String>> entry : subjectExamTypes.entrySet()) {
            String subject = entry.getKey();
            
            // Exam marks
            for (String examType : entry.getValue()) {
                Integer mark = student.examMarks.get(subject) != null ?
                    student.examMarks.get(subject).get(examType) : null;
                data[i][col++] = mark != null ? mark : "-";
            }
            
            // Subject total
            AnalyzerDAO.SubjectData subjectData = student.subjectData.get(subject);
            if (subjectData != null && subjectData.percentage >= 0) {
                data[i][col++] = String.format("%.2f", subjectData.percentage);
            } else {
                data[i][col++] = "-";
            }
        }
        
        // Overall columns
        data[i][col++] = String.format("%.2f", student.percentage);
        data[i][col++] = String.format("%.2f", student.cgpa);
        data[i][col++] = student.grade;
    }
    
    // 5. Create JTable with custom renderer
    JTable table = new JTable(data, columnNames.toArray(new String[0]));
    table.setDefaultRenderer(Object.class, new HierarchicalHeaderRenderer());
    
    // 6. Wrap in scroll pane
    JScrollPane scrollPane = new JScrollPane(table);
    return scrollPane;
}
```

---

## 5. Database Schema

*(Same tables as Student Analyzer, included here for completeness)*

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
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_section (section_id),
    INDEX idx_user (user_id)
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
    actual_max_marks INT NOT NULL,           -- Displayed in brackets
    scaled_to_marks INT NOT NULL,            -- Used for weighted calculation
    passing_marks INT DEFAULT 0,
    FOREIGN KEY (section_id) REFERENCES sections(id),
    UNIQUE KEY unique_component (section_id, subject_name, exam_type),
    INDEX idx_section_subject (section_id, subject_name)
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
    max_marks INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES sections(id),
    UNIQUE KEY unique_mark_entry (student_id, subject_name, exam_type),
    INDEX idx_student_section (student_id, section_id),
    INDEX idx_subject_exam (subject_name, exam_type)
);
```

---

## 6. Calculation Logic

### 6.1 Section Average Calculation

**Purpose:** Calculate average performance across all students in section

**Formula:**
```
Section Average % = Σ(student_percentage) / number_of_students
```

**Method:** `getSectionAnalysisData()`

**Implementation:**
```java
// In AnalyzerDAO.java
public SectionAnalysisData getSectionAnalysisData(int sectionId, Set<String> selectedSubjects) {
    SectionAnalysisData data = new SectionAnalysisData();
    
    // Get all students for this section
    List<Integer> studentIds = getStudentIdsForSection(sectionId);
    
    double totalPercentage = 0.0;
    int passedCount = 0;
    int failedCount = 0;
    
    for (int studentId : studentIds) {
        // Calculate each student's weighted percentage
        double studentPercentage = calculateStudentOverallPercentage(
            studentId, sectionId, selectedSubjects
        );
        
        if (studentPercentage >= 0) {
            totalPercentage += studentPercentage;
            
            if (studentPercentage >= 50.0) {
                passedCount++;
            } else {
                failedCount++;
            }
        }
    }
    
    data.totalStudents = studentIds.size();
    data.passedStudents = passedCount;
    data.failedStudents = failedCount;
    data.averagePercentage = totalPercentage / data.totalStudents;
    data.averageCGPA = data.averagePercentage / 10.0;
    data.passPercentage = (passedCount * 100.0) / data.totalStudents;
    
    return data;
}
```

---

### 6.2 Student Ranking Algorithm

**Purpose:** Rank all students in section based on overall percentage

**Method:** `getAllStudentsRanking()`

**Process:**
```
1. Get all students in section
   ├── Query students table WHERE section_id = ?
   └── Store student IDs

2. For each student:
   ├── Calculate weighted percentage for each subject
   │   └── Call: calculateWeightedSubjectTotalWithPass()
   ├── Calculate overall percentage
   │   └── Average of all subject percentages
   ├── Calculate CGPA
   │   └── percentage / 10.0
   ├── Determine grade
   │   └── Based on percentage thresholds
   └── Store in StudentRankingData object

3. Sort students by percentage (descending)
   ├── Use Collections.sort() with custom comparator
   └── Higher percentage = better rank

4. Assign ranks
   ├── Iterate through sorted list
   ├── Assign rank = index + 1
   └── Handle ties (same percentage = same rank)

5. Return ranked list
```

**Code Implementation:**
```java
public List<StudentRankingData> getAllStudentsRanking(
    int sectionId, 
    Set<String> selectedSubjects,
    Map<String, Set<String>> selectedFilters
) {
    List<StudentRankingData> rankings = new ArrayList<>();
    
    // 1. Get all students
    String query = "SELECT id, name, roll_number FROM students WHERE section_id = ?";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, sectionId);
    ResultSet rs = stmt.executeQuery();
    
    while (rs.next()) {
        int studentId = rs.getInt("id");
        String name = rs.getString("name");
        String rollNumber = rs.getString("roll_number");
        
        StudentRankingData rankingData = new StudentRankingData();
        rankingData.studentId = studentId;
        rankingData.studentName = name;
        rankingData.rollNumber = rollNumber;
        rankingData.subjectData = new LinkedHashMap<>();
        rankingData.examMarks = new LinkedHashMap<>();
        
        // 2. Calculate subject-wise performance
        double totalPercentage = 0.0;
        int validSubjects = 0;
        
        for (String subject : selectedSubjects) {
            Set<String> examTypes = selectedFilters.get(subject);
            
            // Get weighted total for subject
            SubjectPassResult result = calculateWeightedSubjectTotalWithPass(
                studentId, sectionId, subject, examTypes
            );
            
            // Check DUAL PASSING requirement
            if (result.percentage < 0) {
                // Failed - CGPA = 0
                SubjectData subData = new SubjectData();
                subData.subjectName = subject;
                subData.percentage = 0.0;
                subData.grade = "F";
                rankingData.subjectData.put(subject, subData);
                
                // Don't include in average
                continue;
            }
            
            totalPercentage += result.percentage;
            validSubjects++;
            
            // Store subject data
            SubjectData subData = new SubjectData();
            subData.subjectName = subject;
            subData.percentage = result.percentage;
            subData.grade = getLetterGrade(result.percentage);
            subData.examTypes = examTypes;
            rankingData.subjectData.put(subject, subData);
            
            // Get individual exam marks
            Map<String, Integer> examMarksForSubject = getExamMarks(
                studentId, subject, examTypes
            );
            rankingData.examMarks.put(subject, examMarksForSubject);
        }
        
        // 3. Calculate overall metrics
        if (validSubjects > 0) {
            rankingData.percentage = totalPercentage / validSubjects;
            rankingData.cgpa = rankingData.percentage / 10.0;
        } else {
            rankingData.percentage = 0.0;
            rankingData.cgpa = 0.0;
        }
        
        rankingData.grade = getLetterGrade(rankingData.percentage);
        rankings.add(rankingData);
    }
    
    // 4. Sort by percentage (descending)
    Collections.sort(rankings, new Comparator<StudentRankingData>() {
        @Override
        public int compare(StudentRankingData s1, StudentRankingData s2) {
            return Double.compare(s2.percentage, s1.percentage);
        }
    });
    
    // 5. Assign ranks
    for (int i = 0; i < rankings.size(); i++) {
        rankings.get(i).rank = i + 1;
    }
    
    return rankings;
}
```

---

### 6.3 Subject-wise Analysis

**Purpose:** Calculate aggregate metrics for each subject across all students

**Metrics Calculated:**
- Average percentage for subject
- Pass rate for subject
- Highest marks
- Lowest marks
- Number of students who passed/failed

**Implementation:**
```java
public List<SubjectAnalysis> getSubjectAnalysis(int sectionId, Set<String> selectedSubjects) {
    List<SubjectAnalysis> analysisList = new ArrayList<>();
    
    for (String subject : selectedSubjects) {
        SubjectAnalysis analysis = new SubjectAnalysis();
        analysis.subjectName = subject;
        
        List<Integer> studentIds = getStudentIdsForSection(sectionId);
        List<Double> percentages = new ArrayList<>();
        int passCount = 0;
        
        for (int studentId : studentIds) {
            SubjectPassResult result = calculateWeightedSubjectTotalWithPass(
                studentId, sectionId, subject, null
            );
            
            if (result.percentage >= 0) {
                percentages.add(result.percentage);
                if (result.percentage >= 50.0) {
                    passCount++;
                }
            }
        }
        
        // Calculate statistics
        if (!percentages.isEmpty()) {
            double sum = 0.0;
            double max = percentages.get(0);
            double min = percentages.get(0);
            
            for (double p : percentages) {
                sum += p;
                if (p > max) max = p;
                if (p < min) min = p;
            }
            
            analysis.averagePercentage = sum / percentages.size();
            analysis.highestPercentage = max;
            analysis.lowestPercentage = min;
            analysis.passRate = (passCount * 100.0) / percentages.size();
            analysis.totalStudents = percentages.size();
            analysis.passedStudents = passCount;
        }
        
        analysisList.add(analysis);
    }
    
    return analysisList;
}
```

---

## 7. Methods Reference

### 7.1 Constructor Methods

#### Constructor 1: Standalone Dialog Mode
```java
public SectionAnalyzer(
    JFrame parent,
    HashMap<String, ArrayList<Student>> sectionStudents
)
```

**Purpose:** Creates Section Analyzer as a standalone dialog

**Parameters:**
- `parent` - Parent JFrame for modal dialog
- `sectionStudents` - Pre-loaded student data by section name

**Usage:**
```java
HashMap<String, ArrayList<Student>> students = new HashMap<>();
SectionAnalyzer analyzer = new SectionAnalyzer(parentFrame, students);
// Opens as modal dialog
```

---

#### Constructor 2: Embedded Panel Mode
```java
public SectionAnalyzer(
    JFrame parent,
    HashMap<String, ArrayList<Student>> sectionStudents,
    Runnable onBackCallback
)
```

**Purpose:** Creates Section Analyzer as embeddable panel

**Parameters:**
- `parent` - Parent JFrame
- `sectionStudents` - Student data map
- `onBackCallback` - Callback executed when back button clicked

**Usage:**
```java
SectionAnalyzer analyzer = new SectionAnalyzer(frame, students, () -> {
    // Return to previous view
    showPreviousPanel();
});
mainPanel.add(analyzer);
```

---

### 7.2 Database Methods

#### loadSectionsFromDatabase()
```java
private void loadSectionsFromDatabase()
```

**Purpose:** Load all sections accessible to current user

**Process:**
1. Query sections by userId
2. Populate availableSections list
3. Set currentSectionId and currentSectionName
4. Load components and initialize filters

**Database Query:**
```sql
SELECT s.id, s.section_name 
FROM sections s 
WHERE s.user_id = ?
ORDER BY s.section_name
```

---

#### loadAvailableComponentsForSection()
```java
private void loadAvailableComponentsForSection(int sectionId)
```

**Purpose:** Load all exam types (components) for a section

**Parameters:**
- `sectionId` - Section ID to query

**Returns:** Populates `availableComponents` map

**Logic:**
1. Query section_subjects for subjects
2. For each subject:
   - Try marking_components (new system)
   - Fallback to exam_types (old system)
3. Store in LinkedHashMap to preserve order

**Database Queries:**
```sql
-- Get subjects
SELECT DISTINCT s.subject_name, s.id 
FROM section_subjects ss 
JOIN subjects s ON ss.subject_id = s.id 
WHERE ss.section_id = ?

-- Try new system
SELECT mc.component_name 
FROM marking_components mc 
JOIN marking_schemes ms ON mc.scheme_id = ms.id 
WHERE ms.section_id = ? AND ms.subject_id = ? AND ms.is_active = 1

-- Fallback to old system
SELECT DISTINCT et.exam_name 
FROM exam_types et 
WHERE et.section_id = ?
```

---

### 7.3 UI Creation Methods

#### initializeUI()
```java
private void initializeUI()
```

**Purpose:** Build main UI structure

**Components Created:**
1. Header panel with back button, title, radio buttons
2. Section selector dropdown
3. Export button
4. Main content area
5. Status bar

**Layout:** BorderLayout with NORTH header, CENTER content

---

#### createMainContent()
```java
private JPanel createMainContent()
```

**Purpose:** Create tabbed pane with all analysis views

**Returns:** JPanel with JTabbedPane

**Tabs Created:**
1. **Overview**: Section metrics, charts, subject analysis
2. **Ranking**: Hierarchical ranking table
3. **Top Students**: Leaderboard of top performers
4. **Charts**: Grade distribution and other visualizations

**Tab Configuration:**
- Tab placement: TOP
- Tab layout policy: SCROLL_TAB_LAYOUT
- Custom tab appearance with icons

---

#### createSectionResultAnalysis()
```java
private JPanel createSectionResultAnalysis()
```

**Purpose:** Create overview tab with metrics and charts

**Returns:** JPanel with scrollable content

**Components:**
1. Metrics Cards (4 cards):
   - Average Percentage
   - Pass Percentage
   - Total Students
   - Average CGPA
2. Performance Distribution Chart
3. Subject-wise Analysis Table

**Data Source:** `getSectionAnalysisData()` from AnalyzerDAO

---

#### createResultCard()
```java
private JPanel createResultCard(String title, String value, String subtitle, Color color)
```

**Purpose:** Create styled metric card

**Parameters:**
- `title` - Card title (e.g., "Average Percentage")
- `value` - Main value to display (e.g., "94.6%")
- `subtitle` - Additional info (e.g., "Above Average")
- `color` - Card accent color

**Returns:** JPanel with rounded corners and shadow

**Styling:**
- Background: CARD_COLOR (#FFFFFF)
- Border: Rounded with accent color
- Font: Bold for value, regular for title
- Layout: BorderLayout with vertical box

---

#### createTopStudentsTable()
```java
private JScrollPane createTopStudentsTable()
```

**Purpose:** Create table showing top 5 students

**Returns:** JScrollPane with JTable

**Columns:**
- Rank
- Name
- Roll Number
- Percentage
- CGPA
- Grade

**Data Source:** `getTopStudents()` from SectionAnalysisData

**Styling:**
- Header: Bold, centered
- Rows: Alternating colors
- Gold medal icon for rank 1
- Silver medal icon for rank 2
- Bronze medal icon for rank 3

---

#### createChart()
```java
private JPanel createChart()
```

**Purpose:** Create performance distribution chart

**Returns:** JPanel with ChartPanel

**Chart Type:** Bar Chart (JFreeChart)

**Data:**
- X-axis: Grade ranges (A+, A, B+, B, C, D, F)
- Y-axis: Number of students

**Calculation:**
```java
for (StudentRankingData student : rankings) {
    String grade = student.grade;
    gradeCount.put(grade, gradeCount.getOrDefault(grade, 0) + 1);
}
```

---

#### createSubjectTable()
```java
private JScrollPane createSubjectTable()
```

**Purpose:** Create subject-wise analysis table

**Returns:** JScrollPane with JTable

**Columns:**
- Subject Name
- Average %
- Pass Rate
- Highest %
- Lowest %
- Total Students

**Data Source:** `getSubjectAnalysis()` from AnalyzerDAO

**Styling:**
- Header: Bold background
- Cells: Centered text
- Pass rate: Green if > 80%, Red if < 50%

---

#### createFailedStudentTable()
```java
private JScrollPane createFailedStudentTable()
```

**Purpose:** Create table listing students who failed

**Returns:** JScrollPane with JTable

**Columns:**
- Name
- Roll Number
- Failed Subjects Count
- Overall %
- Status

**Data Source:** `failedStudentsMap` from SectionAnalysisData

**Criteria for Failure:**
- Overall percentage < 50%
- OR failed any subject (percentage < 50%)
- OR failed dual passing requirement

---

### 7.4 Ranking Table Methods

#### createRankingTableFromDAO()
```java
private JScrollPane createRankingTableFromDAO()
```

**Purpose:** Create hierarchical ranking table with 2-row headers

**Returns:** JScrollPane with custom JTable

**Process:**
1. Get ranking data from DAO
2. Extract exam types per subject
3. Build column structure
4. Create table data array
5. Apply custom renderer
6. Configure column widths

**Column Structure:**
```
Fixed Columns (3):
  - Rank
  - Roll No
  - Name

Subject Columns (dynamic per subject):
  - Exam Type 1
  - Exam Type 2
  - ...
  - Total

Overall Columns (3):
  - Overall %
  - CGPA
  - Grade
```

**Header Rendering:**
- Row 1: Subject name with total marks (spans multiple columns)
- Row 2: Individual exam types with max marks

---

#### HierarchicalHeaderRenderer (Inner Class)
```java
private class HierarchicalHeaderRenderer extends DefaultTableCellRenderer
```

**Purpose:** Custom renderer for 2-row hierarchical headers

**Override Methods:**
- `getTableCellRendererComponent()` - Render header cells
- `paintComponent()` - Draw merged headers with borders

**Rendering Logic:**
1. Determine if cell is in Row 1 or Row 2
2. For Row 1: Draw subject name spanning columns
3. For Row 2: Draw individual exam type names
4. Apply styling: borders, background, alignment

**Styling:**
- Row 1: Larger font, primary color background
- Row 2: Smaller font, lighter background
- Borders: GridLayout for clarity
- Text: Centered alignment

---

### 7.5 Data Refresh Methods

#### refreshSectionData()
```java
private void refreshSectionData()
```

**Purpose:** Reload all section data and refresh UI

**Process:**
1. Clear existing data
2. Reload section list from database
3. Reload components and filters
4. Rebuild UI components
5. Revalidate and repaint

**When Called:**
- Section changed via dropdown
- Filter applied
- Data modified externally

---

#### refreshDataOnly()
```java
private void refreshDataOnly()
```

**Purpose:** Refresh data without rebuilding entire UI

**Process:**
1. Recalculate ranking data
2. Update table model
3. Update charts
4. Update metric cards

**When Called:**
- Filter selection changed
- Data needs refresh without UI rebuild

**Optimization:** Faster than full refresh, only updates data layer

---

### 7.6 Filter Methods

#### initializeFilters()
```java
private void initializeFilters()
```

**Purpose:** Set all subjects/components as selected by default

**Process:**
1. Clear selectedFilters map
2. Copy all availableComponents to selectedFilters
3. All subjects included
4. All exam types included

**Initial State:**
```java
selectedFilters = {
    "Mathematics": ["Internal 1", "Internal 2", "External"],
    "Physics": ["Mid Term", "End Term"],
    // ... all subjects with all components
}
```

---

#### createFilterPanel()
```java
private JPanel createFilterPanel()
```

**Purpose:** Create filter UI with subject/component checkboxes

**Returns:** JPanel with tree of checkboxes

**Structure:**
```
☑ Subject 1
  ☑ Component 1
  ☑ Component 2
☑ Subject 2
  ☑ Component 1
[Calculate Analysis]
```

**Behavior:**
- Parent checkbox toggles all children
- Child checkbox updates parent indeterminate state
- Calculate button applies filter and refreshes data

---

#### updateTabAppearance()
```java
private void updateTabAppearance()
```

**Purpose:** Update tab styling when filters are active

**Behavior:**
- Show filter indicator on Ranking tab
- Change tab color when filters applied
- Badge showing number of filtered subjects

**Visual Feedback:**
- Default: No indicator
- Filtered: Orange badge with count
- Example: "Ranking (3/5)" when 3 of 5 subjects selected

---

### 7.7 PDF Export Methods

#### exportToPDF()
```java
private void exportToPDF()
```

**Purpose:** Export section ranking to PDF file

**Process:**
1. Show file chooser dialog
2. Get ranking data from DAO
3. Create PDF document (landscape orientation)
4. Add title and section info
5. Create ranking table with hierarchical headers
6. Add footer with timestamp
7. Save and close document

**PDF Layout:**
- Orientation: Landscape (for wide tables)
- Page size: A4
- Margins: 20pt all sides
- Font: Helvetica family

---

#### addRankingTableToPDF()
```java
private void addRankingTableToPDF(
    Document document,
    List<StudentRankingData> rankings,
    Map<String, Set<String>> subjectExamTypes
) throws DocumentException
```

**Purpose:** Add hierarchical ranking table to PDF

**Parameters:**
- `document` - iText Document object
- `rankings` - Student ranking data
- `subjectExamTypes` - Exam types per subject

**Table Structure:**
1. **Header Row 1**: Subject names with total marks
   - Uses `cell.setColspan()` for merging
2. **Header Row 2**: Individual exam types with max marks
3. **Data Rows**: Student data with marks and totals

**Styling:**
- Header: Gray background, bold text
- Data: Alternating row colors
- Borders: All cells
- Alignment: Center for marks, left for names

---

#### createPDFCell()
```java
private PdfPCell createPDFCell(
    String text,
    int colspan,
    int horizontalAlignment,
    boolean isHeader
)
```

**Purpose:** Create styled PDF table cell

**Parameters:**
- `text` - Cell content
- `colspan` - Number of columns to span
- `horizontalAlignment` - Element.ALIGN_LEFT/CENTER/RIGHT
- `isHeader` - Apply header styling

**Returns:** PdfPCell with styling applied

**Styling:**
- Header: Gray background, bold, 10pt
- Data: White background, normal, 9pt
- Border: 1pt all sides
- Padding: 5pt

---

### 7.8 Integration Methods

#### showStudentAnalyzer()
```java
private void showStudentAnalyzer(int studentId, String studentName)
```

**Purpose:** Open Student Analyzer for selected student

**Parameters:**
- `studentId` - Student ID to analyze
- `studentName` - Student name for title

**Trigger:** Right-click on student in ranking table

**Behavior:**
1. Close Section Analyzer dialog
2. Create StudentAnalyzer instance
3. Pass currentSectionId and studentId
4. Open as modal dialog

**Integration:** Bidirectional navigation between analyzers

---

#### openFromLibrary()
```java
public static void openFromLibrary(
    JFrame parent,
    int sectionId,
    String sectionName,
    JPanel mainPanel,
    Runnable onBackCallback
)
```

**Purpose:** Open Section Analyzer from Library view

**Parameters:**
- `parent` - Main application frame
- `sectionId` - Section to display
- `sectionName` - Section name for title
- `mainPanel` - Panel to add analyzer to
- `onBackCallback` - Return to library callback

**Process:**
1. Fetch students for section from database
2. Create sectionStudents map
3. Create SectionAnalyzer with callback
4. Add to mainPanel
5. Revalidate and repaint

**Usage:**
```java
// In Library.java
sectionCard.addMouseListener(new MouseAdapter() {
    public void mouseClicked(MouseEvent e) {
        SectionAnalyzer.openFromLibrary(
            frame, 
            section.id, 
            section.name,
            mainPanel,
            () -> showLibrary()
        );
    }
});
```

---

### 7.9 Utility Methods

#### createModernCard()
```java
private JPanel createModernCard(String title)
```

**Purpose:** Create styled container panel

**Parameters:**
- `title` - Card title

**Returns:** JPanel with rounded border and shadow

**Styling:**
- Background: CARD_COLOR
- Border: Rounded 10px, light gray
- Padding: 15px
- Title: Bold, 14pt

---

#### getLetterGrade()
```java
private String getLetterGrade(double percentage)
```

**Purpose:** Convert percentage to letter grade

**Parameters:**
- `percentage` - Percentage (0-100)

**Returns:** Letter grade (A+, A, B+, B, C, D, F)

**Grading Scale:**
```
A+  : 90-100
A   : 80-89
B+  : 70-79
B   : 60-69
C   : 50-59
D   : 40-49
F   : 0-39
```

---

## 8. Ranking Table System

### 8.1 Hierarchical Header Architecture

**Challenge:** Display multi-level headers with subject names spanning multiple exam type columns

**Solution:** Custom 2-row header system with colspan support

**Header Structure:**
```
┌────────────────────────────────────────────────────────────┐
│ Row 1 (Subject Headers):                                   │
│   ┌─────────────────────────────┐  ┌──────────────────┐   │
│   │  Mathematics (100)          │  │  Physics (100)   │   │
│   │  [spans 4 columns]          │  │  [spans 3 cols]  │   │
│   └─────────────────────────────┘  └──────────────────┘   │
├────────────────────────────────────────────────────────────┤
│ Row 2 (Exam Type Headers):                                 │
│   ┌──────┬──────┬──────┬──────┐  ┌──────┬──────┬──────┐  │
│   │Int 1 │Int 2 │Ext   │Total │  │Mid   │End   │Total │  │
│   │(20)  │(20)  │(60)  │      │  │(50)  │(50)  │      │  │
│   └──────┴──────┴──────┴──────┘  └──────┴──────┴──────┘  │
└────────────────────────────────────────────────────────────┘
```

---

### 8.2 Column Building Algorithm

**Step 1: Calculate Total Columns**
```java
int totalCols = 3; // Rank, Roll, Name

// For each subject
for (Map.Entry<String, Set<String>> entry : subjectExamTypes.entrySet()) {
    totalCols += entry.getValue().size(); // Exam type columns
    totalCols++; // Total column
}

totalCols += 3; // Overall %, CGPA, Grade
```

**Step 2: Create Column Arrays**
```java
String[] row1Headers = new String[totalCols];
String[] row2Headers = new String[totalCols];

int col = 0;

// Fixed columns
row1Headers[col] = "Rank";
row2Headers[col++] = "Rank";

row1Headers[col] = "Roll No";
row2Headers[col++] = "Roll No";

row1Headers[col] = "Name";
row2Headers[col++] = "Name";

// Subject columns (dynamic)
for (String subject : subjects) {
    // Row 1: Subject name (spans multiple columns)
    int examCount = subjectExamTypes.get(subject).size();
    row1Headers[col] = subject + " (100)";
    
    // Mark colspan for renderer
    for (int i = 0; i < examCount + 1; i++) {
        if (i > 0) row1Headers[col + i] = "SPAN"; // Marker for spanned cells
    }
    
    // Row 2: Exam types
    for (String examType : subjectExamTypes.get(subject)) {
        row2Headers[col++] = examType + " (20)";
    }
    row2Headers[col++] = "Total";
}

// Overall columns
row1Headers[col] = "Overall %";
row2Headers[col++] = "Overall %";
// ... etc
```

---

### 8.3 Custom Table Renderer

**HierarchicalHeaderRenderer Implementation:**

```java
private class HierarchicalHeaderRenderer extends DefaultTableCellRenderer {
    
    private String[] row1Headers;
    private String[] row2Headers;
    private Map<Integer, Integer> colspanMap; // column -> colspan
    
    public HierarchicalHeaderRenderer(
        String[] row1Headers,
        String[] row2Headers,
        Map<Integer, Integer> colspanMap
    ) {
        this.row1Headers = row1Headers;
        this.row2Headers = row2Headers;
        this.colspanMap = colspanMap;
    }
    
    @Override
    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column
    ) {
        Component c = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column
        );
        
        // Determine if this is header or data row
        if (row < 2) {
            // Header row
            setBackground(PRIMARY_COLOR);
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD));
            setHorizontalAlignment(CENTER);
            
            // Set text based on row
            if (row == 0) {
                setText(row1Headers[column]);
            } else {
                setText(row2Headers[column]);
            }
        } else {
            // Data row
            setBackground(row % 2 == 0 ? Color.WHITE : LIGHT_GRAY);
            setForeground(Color.BLACK);
            setFont(getFont().deriveFont(Font.PLAIN));
            setHorizontalAlignment(CENTER);
        }
        
        return c;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw borders for merged cells
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.GRAY);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }
}
```

---

### 8.4 Table Model Configuration

**Custom TableModel for Fixed Headers:**

```java
DefaultTableModel model = new DefaultTableModel(data, columnNames) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false; // Read-only table
    }
};

JTable table = new JTable(model);

// Set renderer
table.setDefaultRenderer(Object.class, new HierarchicalHeaderRenderer(...));

// Configure header
JTableHeader header = table.getTableHeader();
header.setPreferredSize(new Dimension(header.getWidth(), 60)); // 2 rows * 30px
header.setReorderingAllowed(false); // Prevent column reordering

// Configure columns
TableColumnModel columnModel = table.getColumnModel();
for (int i = 0; i < table.getColumnCount(); i++) {
    TableColumn column = columnModel.getColumn(i);
    
    // Auto-size based on content
    int maxWidth = 0;
    for (int row = 0; row < table.getRowCount(); row++) {
        Object value = table.getValueAt(row, i);
        String text = value != null ? value.toString() : "";
        maxWidth = Math.max(maxWidth, text.length() * 8);
    }
    
    column.setPreferredWidth(Math.max(maxWidth, 60));
    column.setMinWidth(40);
}
```

---

### 8.5 Context Menu Integration

**Right-Click Menu on Student Rows:**

```java
table.addMouseListener(new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
                
                // Get student data
                int studentId = getStudentIdFromRow(row);
                String studentName = (String) table.getValueAt(row, 2); // Name column
                
                // Show context menu
                JPopupMenu menu = new JPopupMenu();
                
                JMenuItem viewDetails = new JMenuItem("View Student Details");
                viewDetails.addActionListener(e2 -> {
                    showStudentAnalyzer(studentId, studentName);
                });
                menu.add(viewDetails);
                
                menu.show(table, e.getX(), e.getY());
            }
        }
    }
});
```

---

## 9. PDF Export System

### 9.1 Export Configuration

**PDF Settings:**
```java
Document document = new Document(PageSize.A4.rotate()); // Landscape
document.setMargins(20, 20, 20, 20); // Left, Right, Top, Bottom

PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
document.open();
```

**Font Configuration:**
```java
Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
Font dataFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
```

---

### 9.2 PDF Structure

**Page Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  Section Performance Report                                 │
│  Section: A ISE                                             │
│  Date: January 14, 2026                                     │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  [Hierarchical Ranking Table]                               │
│                                                              │
│  Subject headers spanning columns                           │
│  Exam type headers with max marks                           │
│  Student data rows                                          │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│  Generated by Academic Analyzer | Page 1 of 1              │
└─────────────────────────────────────────────────────────────┘
```

---

### 9.3 Table Creation

**Create PDF Table with Hierarchical Headers:**

```java
private void addRankingTableToPDF(
    Document document,
    List<StudentRankingData> rankings,
    Map<String, Set<String>> subjectExamTypes
) throws DocumentException {
    
    // Calculate total columns
    int totalCols = 3; // Rank, Roll, Name
    for (Set<String> examTypes : subjectExamTypes.values()) {
        totalCols += examTypes.size() + 1; // Exams + Total
    }
    totalCols += 3; // Overall %, CGPA, Grade
    
    // Create table
    PdfPTable table = new PdfPTable(totalCols);
    table.setWidthPercentage(100);
    
    // Add Row 1: Subject Headers
    addFixedHeaders(table); // Rank, Roll, Name
    
    for (Map.Entry<String, Set<String>> entry : subjectExamTypes.entrySet()) {
        String subject = entry.getKey();
        int colspan = entry.getValue().size() + 1; // Exams + Total
        
        PdfPCell subjectCell = createPDFCell(
            subject + " (100)", 
            colspan, 
            Element.ALIGN_CENTER, 
            true
        );
        table.addCell(subjectCell);
    }
    
    addOverallHeaders(table); // %, CGPA, Grade
    
    // Add Row 2: Exam Type Headers
    addFixedHeaders(table); // Rank, Roll, Name
    
    for (Map.Entry<String, Set<String>> entry : subjectExamTypes.entrySet()) {
        for (String examType : entry.getValue()) {
            // Get max marks for exam type
            int maxMarks = getExamTypeMaxMarks(examType);
            PdfPCell examCell = createPDFCell(
                examType + " (" + maxMarks + ")",
                1,
                Element.ALIGN_CENTER,
                true
            );
            table.addCell(examCell);
        }
        
        // Total column
        PdfPCell totalCell = createPDFCell("Total", 1, Element.ALIGN_CENTER, true);
        table.addCell(totalCell);
    }
    
    addOverallHeaders(table); // %, CGPA, Grade
    
    // Add Data Rows
    for (StudentRankingData student : rankings) {
        // Fixed columns
        table.addCell(createPDFCell(
            String.valueOf(student.rank), 1, Element.ALIGN_CENTER, false
        ));
        table.addCell(createPDFCell(
            student.rollNumber, 1, Element.ALIGN_CENTER, false
        ));
        table.addCell(createPDFCell(
            student.studentName, 1, Element.ALIGN_LEFT, false
        ));
        
        // Subject columns
        for (String subject : subjectExamTypes.keySet()) {
            // Exam marks
            for (String examType : subjectExamTypes.get(subject)) {
                Integer mark = student.examMarks.get(subject) != null ?
                    student.examMarks.get(subject).get(examType) : null;
                
                String markText = mark != null ? String.valueOf(mark) : "-";
                table.addCell(createPDFCell(
                    markText, 1, Element.ALIGN_CENTER, false
                ));
            }
            
            // Subject total
            SubjectData subjectData = student.subjectData.get(subject);
            String totalText = subjectData != null && subjectData.percentage >= 0 ?
                String.format("%.2f", subjectData.percentage) : "-";
            
            table.addCell(createPDFCell(
                totalText, 1, Element.ALIGN_CENTER, false
            ));
        }
        
        // Overall columns
        table.addCell(createPDFCell(
            String.format("%.2f", student.percentage), 1, Element.ALIGN_CENTER, false
        ));
        table.addCell(createPDFCell(
            String.format("%.2f", student.cgpa), 1, Element.ALIGN_CENTER, false
        ));
        table.addCell(createPDFCell(
            student.grade, 1, Element.ALIGN_CENTER, false
        ));
    }
    
    document.add(table);
}
```

---

### 9.4 Cell Styling

**Create Styled PDF Cell:**

```java
private PdfPCell createPDFCell(
    String text,
    int colspan,
    int horizontalAlignment,
    boolean isHeader
) {
    Font font = isHeader ? headerFont : dataFont;
    PdfPCell cell = new PdfPCell(new Phrase(text, font));
    
    cell.setColspan(colspan);
    cell.setHorizontalAlignment(horizontalAlignment);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    cell.setPadding(5);
    
    if (isHeader) {
        cell.setBackgroundColor(new BaseColor(200, 200, 200)); // Light gray
    } else {
        cell.setBackgroundColor(BaseColor.WHITE);
    }
    
    cell.setBorder(PdfPCell.BOX);
    cell.setBorderWidth(1);
    cell.setBorderColor(BaseColor.GRAY);
    
    return cell;
}
```

---

### 9.5 Export Workflow

**Complete Export Process:**

```java
private void exportToPDF() {
    // 1. Show file chooser
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Section Report");
    fileChooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
    fileChooser.setSelectedFile(new File(currentSectionName + "_Report.pdf"));
    
    int result = fileChooser.showSaveDialog(this);
    if (result != JFileChooser.APPROVE_OPTION) {
        return;
    }
    
    File file = fileChooser.getSelectedFile();
    if (!file.getName().endsWith(".pdf")) {
        file = new File(file.getAbsolutePath() + ".pdf");
    }
    
    try {
        // 2. Create document
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();
        
        // 3. Add title
        Paragraph title = new Paragraph("Section Performance Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);
        
        // 4. Add section info
        Paragraph info = new Paragraph(
            "Section: " + currentSectionName + "\n" +
            "Date: " + new SimpleDateFormat("MMMM dd, yyyy").format(new Date()),
            dataFont
        );
        info.setAlignment(Element.ALIGN_CENTER);
        info.setSpacingAfter(20);
        document.add(info);
        
        // 5. Get ranking data
        AnalyzerDAO dao = new AnalyzerDAO();
        Set<String> selectedSubjects = selectedFilters.keySet();
        List<StudentRankingData> rankings = dao.getAllStudentsRanking(
            currentSectionId,
            selectedSubjects,
            selectedFilters
        );
        
        // Extract exam types
        Map<String, Set<String>> subjectExamTypes = new LinkedHashMap<>();
        for (String subject : selectedSubjects) {
            Set<String> examTypes = new LinkedHashSet<>();
            for (StudentRankingData student : rankings) {
                if (student.examMarks.containsKey(subject)) {
                    examTypes.addAll(student.examMarks.get(subject).keySet());
                }
            }
            subjectExamTypes.put(subject, examTypes);
        }
        
        // 6. Add ranking table
        addRankingTableToPDF(document, rankings, subjectExamTypes);
        
        // 7. Add footer
        Paragraph footer = new Paragraph(
            "Generated by Academic Analyzer",
            new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
        
        // 8. Close document
        document.close();
        
        // 9. Show success message
        JOptionPane.showMessageDialog(
            this,
            "Report exported successfully to:\n" + file.getAbsolutePath(),
            "Export Successful",
            JOptionPane.INFORMATION_MESSAGE
        );
        
        // 10. Open file
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
        }
        
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Error exporting report: " + ex.getMessage(),
            "Export Error",
            JOptionPane.ERROR_MESSAGE
        );
        ex.printStackTrace();
    }
}
```

---

## 10. Integration Points

### 10.1 Dashboard Integration

**Opening from Dashboard:**

```java
// In DashboardScreen.java
JRadioButton sectionRadio = new JRadioButton("Section");
sectionRadio.addActionListener(e -> {
    // Show section selection dialog
    showSectionSelectionDialog();
});

private void showSectionSelectionDialog() {
    SectionDAO sectionDAO = new SectionDAO();
    List<SectionInfo> sections = sectionDAO.getSectionsByUser(currentUserId);
    
    if (sections.isEmpty()) {
        JOptionPane.showMessageDialog(
            this,
            "No sections found. Please create a section first.",
            "No Sections",
            JOptionPane.INFORMATION_MESSAGE
        );
        return;
    }
    
    // Create dialog with section dropdown
    JDialog dialog = new JDialog(this, "Select Section", true);
    JComboBox<String> sectionCombo = new JComboBox<>();
    
    for (SectionInfo section : sections) {
        sectionCombo.addItem(section.sectionName);
    }
    
    JButton okButton = new JButton("OK");
    okButton.addActionListener(e -> {
        int selectedIndex = sectionCombo.getSelectedIndex();
        if (selectedIndex >= 0) {
            SectionInfo selected = sections.get(selectedIndex);
            
            // Fetch students
            HashMap<String, ArrayList<Student>> sectionStudents = 
                fetchStudentsForSection(selected.id);
            
            // Open Section Analyzer
            SectionAnalyzer analyzer = new SectionAnalyzer(
                this,
                sectionStudents,
                () -> {
                    // Return to dashboard
                    showDashboard();
                }
            );
            
            // Replace main panel
            mainPanel.removeAll();
            mainPanel.add(analyzer);
            mainPanel.revalidate();
            mainPanel.repaint();
            
            dialog.dispose();
        }
    });
    
    dialog.add(sectionCombo, BorderLayout.CENTER);
    dialog.add(okButton, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
}
```

---

### 10.2 Library Integration

**Opening from Library (Section Card Click):**

```java
// In Library.java
private JPanel createSectionCard(SectionInfo section) {
    JPanel card = new JPanel();
    card.setLayout(new BorderLayout());
    
    // ... card styling ...
    
    // Add click listener
    card.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            System.out.println("@@@ CARD CLICKED: " + section.sectionName + " @@@");
            
            // Open Section Analyzer
            SectionAnalyzer.openFromLibrary(
                parentFrame,
                section.id,
                section.sectionName,
                mainPanel,
                () -> {
                    // Return to library
                    showLibrary();
                }
            );
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            card.setBackground(HOVER_COLOR);
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            card.setBackground(CARD_COLOR);
            card.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    });
    
    return card;
}
```

**Static Helper Method in SectionAnalyzer:**

```java
public static void openFromLibrary(
    JFrame parent,
    int sectionId,
    String sectionName,
    JPanel mainPanel,
    Runnable onBackCallback
) {
    // Fetch students for section
    HashMap<String, ArrayList<Student>> sectionStudents = new HashMap<>();
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        String query = 
            "SELECT s.id, s.name, s.roll_number " +
            "FROM students s " +
            "WHERE s.section_id = ? " +
            "ORDER BY s.roll_number";
        
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, sectionId);
        ResultSet rs = stmt.executeQuery();
        
        ArrayList<Student> students = new ArrayList<>();
        while (rs.next()) {
            Student student = new Student(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("roll_number"),
                sectionId
            );
            students.add(student);
        }
        
        sectionStudents.put(sectionName, students);
        
        // Create analyzer with embedded mode
        SectionAnalyzer analyzer = new SectionAnalyzer(
            parent,
            sectionStudents,
            onBackCallback
        );
        
        // Set current section
        analyzer.currentSectionId = sectionId;
        analyzer.currentSectionName = sectionName;
        
        // Replace main panel
        mainPanel.removeAll();
        mainPanel.add(analyzer, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(
            parent,
            "Error loading section data: " + e.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
        e.printStackTrace();
    }
}
```

---

### 10.3 Student Analyzer Integration

**Bidirectional Navigation:**

**From Section → Student:**
```java
// Right-click on student row in ranking table
table.addMouseListener(new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0) {
                int studentId = getStudentIdFromRow(row);
                String studentName = (String) table.getValueAt(row, 2);
                
                // Open Student Analyzer
                showStudentAnalyzer(studentId, studentName);
            }
        }
    }
});

private void showStudentAnalyzer(int studentId, String studentName) {
    StudentAnalyzer analyzer = new StudentAnalyzer(
        parentFrame,
        currentSectionId,
        studentId,
        studentName
    );
    
    // Opens as modal dialog
    analyzer.setVisible(true);
}
```

**From Student → Section:**
```java
// In StudentAnalyzer.java
JButton viewSectionButton = new JButton("View Section Analysis");
viewSectionButton.addActionListener(e -> {
    // Close Student Analyzer
    dispose();
    
    // Open Section Analyzer
    HashMap<String, ArrayList<Student>> sectionStudents = 
        fetchStudentsForSection(currentSectionId);
    
    SectionAnalyzer sectionAnalyzer = new SectionAnalyzer(
        parentFrame,
        sectionStudents
    );
    
    sectionAnalyzer.setVisible(true);
});
```

---

## 11. Error Handling

### 11.1 Database Errors

**Connection Handling:**
```java
try {
    Connection conn = DatabaseConnection.getConnection();
    if (conn == null || conn.isClosed()) {
        throw new SQLException("Database connection not available");
    }
    
    // Execute queries
    
} catch (SQLException e) {
    JOptionPane.showMessageDialog(
        this,
        "Database error: " + e.getMessage() +
        "\n\nPlease check your database connection.",
        "Database Error",
        JOptionPane.ERROR_MESSAGE
    );
    e.printStackTrace();
    
    // Log error
    Logger.getLogger(SectionAnalyzer.class.getName())
        .log(Level.SEVERE, "Database error", e);
}
```

---

### 11.2 Empty Data Handling

**No Students in Section:**
```java
private JScrollPane createEmptyTable() {
    JPanel emptyPanel = new JPanel();
    emptyPanel.setLayout(new BorderLayout());
    emptyPanel.setBackground(BACKGROUND_COLOR);
    
    JLabel emptyLabel = new JLabel("No students found in this section");
    emptyLabel.setFont(new Font("Arial", Font.PLAIN, 16));
    emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
    
    emptyPanel.add(emptyLabel, BorderLayout.CENTER);
    
    return new JScrollPane(emptyPanel);
}
```

**No Marks Data:**
```java
if (rankings.isEmpty()) {
    JOptionPane.showMessageDialog(
        this,
        "No marks data found for this section.\n" +
        "Please ensure marks have been entered for students.",
        "No Data",
        JOptionPane.INFORMATION_MESSAGE
    );
    return createEmptyTable();
}
```

---

### 11.3 Validation

**Filter Validation:**
```java
private boolean validateFilters() {
    if (selectedFilters.isEmpty()) {
        JOptionPane.showMessageDialog(
            this,
            "Please select at least one subject to analyze.",
            "No Subjects Selected",
            JOptionPane.WARNING_MESSAGE
        );
        return false;
    }
    
    // Check if any components selected
    boolean hasComponents = false;
    for (Set<String> components : selectedFilters.values()) {
        if (!components.isEmpty()) {
            hasComponents = true;
            break;
        }
    }
    
    if (!hasComponents) {
        JOptionPane.showMessageDialog(
            this,
            "Please select at least one exam type for analysis.",
            "No Components Selected",
            JOptionPane.WARNING_MESSAGE
        );
        return false;
    }
    
    return true;
}
```

---

## 12. Performance Considerations

### 12.1 Lazy Loading

**Load Data Only When Needed:**
```java
// Don't load all data at construction
public SectionAnalyzer(...) {
    // Only load section list
    loadSectionsFromDatabase();
    
    // UI initialization
    initializeUI();
    
    // Data loaded when tab is clicked
}

// Load ranking data on demand
tabbedPane.addChangeListener(e -> {
    int selectedIndex = tabbedPane.getSelectedIndex();
    if (selectedIndex == 1) { // Ranking tab
        if (!rankingDataLoaded) {
            loadRankingData();
            rankingDataLoaded = true;
        }
    }
});
```

---

### 12.2 Caching

**Cache DAO Results:**
```java
private Map<String, List<StudentRankingData>> rankingCache = new HashMap<>();

private List<StudentRankingData> getRankingData() {
    String cacheKey = currentSectionId + "_" + selectedFilters.hashCode();
    
    if (rankingCache.containsKey(cacheKey)) {
        return rankingCache.get(cacheKey);
    }
    
    AnalyzerDAO dao = new AnalyzerDAO();
    List<StudentRankingData> rankings = dao.getAllStudentsRanking(
        currentSectionId,
        selectedFilters.keySet(),
        selectedFilters
    );
    
    rankingCache.put(cacheKey, rankings);
    return rankings;
}

// Clear cache when data changes
private void clearCache() {
    rankingCache.clear();
}
```

---

### 12.3 Database Query Optimization

**Use PreparedStatements:**
```java
// Good: PreparedStatement (prevents SQL injection, cached)
String query = "SELECT * FROM students WHERE section_id = ?";
PreparedStatement stmt = conn.prepareStatement(query);
stmt.setInt(1, sectionId);
ResultSet rs = stmt.executeQuery();

// Bad: String concatenation
String query = "SELECT * FROM students WHERE section_id = " + sectionId;
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(query);
```

**Batch Queries:**
```java
// Get all data in one query instead of multiple queries
String query = 
    "SELECT s.id, s.name, s.roll_number, " +
    "       m.subject_name, m.exam_type, m.marks, m.max_marks " +
    "FROM students s " +
    "LEFT JOIN marks m ON s.id = m.student_id " +
    "WHERE s.section_id = ? " +
    "ORDER BY s.roll_number, m.subject_name, m.exam_type";

// Process all data in one pass
```

---

### 12.4 UI Performance

**Swing Thread Management:**
```java
// Heavy operations in background thread
SwingWorker<List<StudentRankingData>, Void> worker = 
    new SwingWorker<List<StudentRankingData>, Void>() {
    
    @Override
    protected List<StudentRankingData> doInBackground() throws Exception {
        // Database query (background thread)
        AnalyzerDAO dao = new AnalyzerDAO();
        return dao.getAllStudentsRanking(
            currentSectionId,
            selectedFilters.keySet(),
            selectedFilters
        );
    }
    
    @Override
    protected void done() {
        try {
            // UI update (EDT)
            List<StudentRankingData> rankings = get();
            updateRankingTable(rankings);
        } catch (Exception e) {
            showError(e);
        }
    }
};

worker.execute();
```

---

## 13. Conclusion

The **Section Analyzer** is a comprehensive tool for section-level performance analysis in the Academic Analyzer application. Key features include:

✅ **Hierarchical Ranking Table**: 2-row headers with subject and exam type structure  
✅ **Weighted Calculation System**: Accurate percentage calculations using actual weightages  
✅ **Visual Analytics**: Charts and graphs for performance distribution  
✅ **PDF Export**: Professional reports with full ranking tables  
✅ **Selective Filtering**: Focus analysis on specific subjects/exam types  
✅ **Bidirectional Integration**: Navigate between Section and Student analysis  
✅ **Library Integration**: Quick access from section management view  

**Total Lines of Code**: 2623  
**Key Dependencies**: AnalyzerDAO, SectionDAO, Student, DatabaseConnection  
**UI Framework**: Java Swing with FlatLaf  
**Export Library**: iText 5.x  

---

**Document Version**: 1.0  
**Last Updated**: January 14, 2026  
**Author**: Academic Analyzer Development Team
