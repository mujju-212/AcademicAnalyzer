# View Selection Tool - Technical Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Features](#core-features)
4. [Section Analysis Mode](#section-analysis-mode)
5. [Launched Results Mode](#launched-results-mode)
6. [Multi-Row Header System](#multi-row-header-system)
7. [Export System](#export-system)
8. [Data Models](#data-models)

---

## 1. Overview

### Purpose
The View Selection Tool is a comprehensive data visualization and analysis component that displays student academic performance data in a flexible, customizable table format. It serves as the primary interface for viewing, analyzing, and exporting student results.

### Key Features
- **Dual Display Modes**: Section-based analysis or launched results viewing
- **Flexible Column Selection**: Customize visible fields via checkboxes
- **Subject & Exam Type Display**: Multi-row headers showing subject breakdown
- **Advanced Filtering**: Search, filter, and rank students
- **Export Capabilities**: PDF (A4 landscape) and Excel (professional format)
- **Real-time Calculation**: Automatic CGPA, grade, and ranking computation
- **Launched Results Integration**: View previously published results with exam type details

### Use Cases
1. **Academic Review**: Teachers analyze class performance by section
2. **Result Verification**: View launched results with original configuration
3. **Report Generation**: Export formatted reports for administration
4. **Student Counseling**: Identify struggling students for intervention
5. **Performance Tracking**: Monitor trends across different exam types

---

## 2. Architecture

### Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    ViewSelectionTool                         │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐     ┌──────────────────────────┐     │
│  │  Section Mode    │     │  Launched Results Mode   │     │
│  │  - All Sections  │     │  - Dropdown Selection    │     │
│  │  - Single Section│     │  - JSON Parsing          │     │
│  │  - Live Data     │     │  - Historical Data       │     │
│  └──────────────────┘     └──────────────────────────┘     │
│           │                            │                     │
│           └────────────┬───────────────┘                     │
│                        │                                     │
│                        ▼                                     │
│           ┌────────────────────────────┐                    │
│           │   Data Processing Layer    │                    │
│           │  - Column Generation       │                    │
│           │  - Row Population          │                    │
│           │  - Calculations            │                    │
│           │  - Ranking                 │                    │
│           └────────────────────────────┘                    │
│                        │                                     │
│           ┌────────────┼────────────┐                       │
│           │            │            │                       │
│           ▼            ▼            ▼                       │
│    ┌──────────┐ ┌──────────┐ ┌──────────┐                │
│    │  Table   │ │   PDF    │ │  Excel   │                │
│    │ Display  │ │  Export  │ │  Export  │                │
│    └──────────┘ └──────────┘ └──────────┘                │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Class Hierarchy

```java
public class ViewSelectionTool extends JPanel {
    
    // UI Components
    private JComboBox<String> sectionDropdown;
    private JComboBox<String> launchedResultsDropdown;
    private JTable resultTable;
    private JScrollPane tableScrollPane;
    
    // Checkboxes for column selection
    private JCheckBox nameCheckBox;
    private JCheckBox rollNumberCheckBox;
    private JCheckBox emailCheckBox;
    private JCheckBox phoneCheckBox;
    private JCheckBox sectionCheckBox;
    private JCheckBox yearCheckBox;
    private JCheckBox semesterCheckBox;
    private JCheckBox totalMarksCheckBox;
    private JCheckBox percentageCheckBox;
    private JCheckBox sgpaCheckBox;
    private JCheckBox gradeCheckBox;
    private JCheckBox statusCheckBox;
    private JCheckBox rankCheckBox;
    private JCheckBox failedSubjectsCheckBox;
    
    // Subject selection
    private JPanel subjectsPanel;
    private Map<String, JCheckBox> subjectCheckboxes;
    private Map<String, Map<String, JCheckBox>> examTypeCheckboxes;
    
    // Data structures
    private Map<String, List<Student>> sectionStudents;
    private List<String> selectedSubjects;
    private Map<String, List<String>> subjectExamTypesMap;
    private Map<String, Map<String, Integer>> maxMarksMap;
    
    // Inner Classes
    private static class ExtendedStudentData { }
    private static class SectionInfo { }
    private static class ColumnGroup { }
}
```

### Data Flow

```
1. User Interaction
   ├─ Select Section/Launched Result
   ├─ Choose Columns (checkboxes)
   ├─ Select Subjects & Exam Types
   └─ Click "Display"

2. Data Fetching
   ├─ Section Mode: Query entered_exam_marks + students tables
   └─ Launched Mode: Query launched_student_results + parse JSON

3. Data Processing
   ├─ Build Column Structure
   │  ├─ Student Info Columns
   │  ├─ Subject-Exam Type Columns
   │  └─ Overall Stats Columns
   │
   ├─ Calculate Results
   │  ├─ Weighted Subject Totals
   │  ├─ Overall Percentage & CGPA
   │  ├─ Grade Assignment
   │  └─ Pass/Fail Status
   │
   └─ Ranking (if enabled)

4. Display
   ├─ Generate Table Model
   ├─ Apply Multi-Row Headers
   ├─ Render with Formatting
   └─ Enable Sorting

5. Export (Optional)
   ├─ PDF: iText with adaptive layout
   └─ Excel: Apache POI with styling
```

---

## 3. Core Features

### 3.1 Section Dropdown

**Purpose**: Select which section's data to display

**Implementation**:
```java
private void loadSections() {
    sectionDropdown.removeAllItems();
    sectionDropdown.addItem("All Sections");
    
    // Fetch sections from database
    SectionDAO sectionDAO = new SectionDAO();
    List<Section> sections = sectionDAO.getSectionsByUser(currentUserId);
    
    for (Section section : sections) {
        String displayText = String.format("%s (%d students)",
            section.getName(), section.getStudentCount());
        sectionDropdown.addItem(displayText);
    }
}
```

**Selection Types**:
1. **All Sections**: Aggregate data from all sections (combined view)
2. **Single Section**: Data from specific section only

### 3.2 Launched Results Dropdown

**Purpose**: View previously published results with original configuration

**Implementation**:
```java
private void loadLaunchedResultsDropdown() {
    launchedResultsDropdown.removeAllItems();
    launchedResultsDropdown.addItem("-- Select Launched Result --");
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        String query = "SELECT id, launch_name, created_at, " +
                      "(SELECT COUNT(*) FROM launched_student_results " +
                      " WHERE launch_id = lr.id) as student_count " +
                      "FROM launched_results lr " +
                      "WHERE section_id = ? " +
                      "ORDER BY created_at DESC";
        
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, selectedSectionId);
        ResultSet rs = ps.executeQuery();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm a");
        
        while (rs.next()) {
            int id = rs.getInt("id");
            Timestamp timestamp = rs.getTimestamp("created_at");
            int studentCount = rs.getInt("student_count");
            
            String displayText = String.format("#%d - %s (%d students)",
                id, dateFormat.format(timestamp), studentCount);
            
            launchedResultsDropdown.addItem(displayText);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

**Display Format**: `#9 - 16-Jan-2026 11:50 pm (50 students)`

### 3.3 Column Selection Checkboxes

**Student Info Columns**:
- Name (always visible in launched mode)
- Roll Number (always visible in launched mode)
- Email
- Phone
- Section
- Year
- Semester

**Performance Columns**:
- Total Marks
- Percentage
- SGPA
- Grade
- Status (Pass/Fail)
- Rank
- Failed Subjects Count

**Subject Columns**:
- Dynamically generated based on selected subjects
- Each subject has multiple exam type columns
- Each subject has a "Total" column

### 3.4 Subject & Exam Type Selection

**Purpose**: Choose which subjects and exam components to display

**UI Structure**:
```
┌─────────────────────────────────────────┐
│ Subjects                                │
├─────────────────────────────────────────┤
│ ☑ Computer Networks                     │
│   ☑ Lab Internal                        │
│   ☑ Theory Final                        │
│   ☑ Lab Final                           │
│   ☑ Theory Internal                     │
│                                          │
│ ☑ Data Structures                       │
│   ☑ Internal 1                          │
│   ☑ Internal 2                          │
│   ☑ Final Exam                          │
│                                          │
│ ☑ Operating Systems                     │
│   ☑ Internal 1                          │
│   ☑ Internal 2                          │
│   ☑ Internal 3                          │
│   ☑ Final Exam                          │
└─────────────────────────────────────────┘
```

**Implementation**:
```java
private void loadSubjectsPanel() {
    subjectsPanel.removeAll();
    subjectsPanel.setLayout(new BoxLayout(subjectsPanel, BoxLayout.Y_AXIS));
    
    subjectCheckboxes.clear();
    examTypeCheckboxes.clear();
    
    // Get subjects with exam types
    AnalyzerDAO dao = new AnalyzerDAO();
    Map<String, List<String>> subjectsWithExamTypes = 
        dao.getSubjectsWithExamTypes(selectedSectionId);
    
    for (Map.Entry<String, List<String>> entry : subjectsWithExamTypes.entrySet()) {
        String subject = entry.getKey();
        List<String> examTypes = entry.getValue();
        
        // Subject checkbox
        JCheckBox subjectCheckbox = new JCheckBox(subject);
        subjectCheckbox.setFont(new Font("SansSerif", Font.BOLD, 12));
        subjectCheckboxes.put(subject, subjectCheckbox);
        subjectsPanel.add(subjectCheckbox);
        
        // Exam type checkboxes (indented)
        JPanel examTypesPanel = new JPanel();
        examTypesPanel.setLayout(new BoxLayout(examTypesPanel, BoxLayout.Y_AXIS));
        examTypesPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 5, 0));
        
        Map<String, JCheckBox> examTypeMap = new HashMap<>();
        
        for (String examType : examTypes) {
            JCheckBox examTypeCheckbox = new JCheckBox(examType);
            examTypeCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 11));
            examTypeMap.put(examType, examTypeCheckbox);
            examTypesPanel.add(examTypeCheckbox);
        }
        
        examTypeCheckboxes.put(subject, examTypeMap);
        subjectsPanel.add(examTypesPanel);
        
        // Subject checkbox controls exam type checkboxes
        subjectCheckbox.addActionListener(e -> {
            boolean selected = subjectCheckbox.isSelected();
            for (JCheckBox cb : examTypeMap.values()) {
                cb.setSelected(selected);
            }
        });
    }
    
    subjectsPanel.revalidate();
    subjectsPanel.repaint();
}
```

### 3.5 Display Button Logic

```java
private void displaySelectedData() {
    // Validate selections
    if (!validateSelections()) {
        return;
    }
    
    // Determine mode
    boolean showLaunchedResults = launchedResultsDropdown.getSelectedIndex() > 0;
    
    if (showLaunchedResults) {
        displayLaunchedResults();
    } else {
        displaySectionData();
    }
}

private boolean validateSelections() {
    // Check if at least one column is selected
    if (!anyCheckboxSelected()) {
        showStyledMessage("Please select at least one field to display!", 
                         "No Fields Selected", 
                         JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    // Check if at least one subject is selected
    if (selectedSubjects.isEmpty()) {
        showStyledMessage("Please select at least one subject!", 
                         "No Subjects Selected", 
                         JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    return true;
}
```

---

## 4. Section Analysis Mode

### 4.1 Overview

Section Analysis Mode displays real-time data from the database, showing current marks entered for students in selected section(s).

### 4.2 Data Fetching

```java
private List<ExtendedStudentData> getStudentsFromSection(
        String sectionName, 
        List<String> selectedSubjects) {
    
    List<ExtendedStudentData> studentDataList = new ArrayList<>();
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        
        // Fetch students
        String studentQuery = "SELECT s.id, s.student_name, s.roll_number, " +
                             "s.email, s.phone, sec.section_name, " +
                             "sec.academic_year, sec.semester " +
                             "FROM students s " +
                             "JOIN sections sec ON s.section_id = sec.id " +
                             "WHERE sec.section_name = ? " +
                             "ORDER BY s.roll_number";
        
        PreparedStatement ps = conn.prepareStatement(studentQuery);
        ps.setString(1, sectionName);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            ExtendedStudentData data = new ExtendedStudentData();
            data.studentId = rs.getInt("id");
            data.name = rs.getString("student_name");
            data.rollNumber = rs.getString("roll_number");
            data.email = rs.getString("email");
            data.phone = rs.getString("phone");
            data.section = rs.getString("section_name");
            data.year = rs.getInt("academic_year");
            data.semester = rs.getInt("semester");
            
            // Fetch marks for this student
            fetchStudentMarks(data, selectedSubjects);
            
            // Calculate results
            calculateStudentResults(data, selectedSubjects);
            
            studentDataList.add(data);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return studentDataList;
}
```

### 4.3 Marks Fetching

```java
private void fetchStudentMarks(ExtendedStudentData data, 
                               List<String> selectedSubjects) {
    try {
        Connection conn = DatabaseConnection.getConnection();
        
        // Fetch marks for selected subjects and exam types
        String marksQuery = "SELECT sm.subject_name, et.exam_type_name, " +
                           "sm.marks_obtained, mc.max_marks " +
                           "FROM entered_exam_marks sm " +
                           "JOIN exam_types et ON sm.exam_type_id = et.id " +
                           "JOIN marking_components mc ON et.id = mc.exam_type_id " +
                           "WHERE sm.student_id = ? " +
                           "AND sm.subject_name IN (" + 
                           buildPlaceholders(selectedSubjects.size()) + ")";
        
        PreparedStatement ps = conn.prepareStatement(marksQuery);
        ps.setInt(1, data.studentId);
        
        // Set subject parameters
        for (int i = 0; i < selectedSubjects.size(); i++) {
            ps.setString(i + 2, selectedSubjects.get(i));
        }
        
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            String subject = rs.getString("subject_name");
            String examType = rs.getString("exam_type_name");
            int marksObtained = rs.getInt("marks_obtained");
            int maxMarks = rs.getInt("max_marks");
            
            // Store in data structure
            data.subjectMarks
                .computeIfAbsent(subject, k -> new HashMap<>())
                .put(examType, marksObtained);
            
            // Store max marks
            maxMarksMap
                .computeIfAbsent(subject, k -> new HashMap<>())
                .put(examType, maxMarks);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

### 4.4 Result Calculation

```java
private void calculateStudentResults(ExtendedStudentData data, 
                                    List<String> selectedSubjects) {
    AnalyzerDAO dao = new AnalyzerDAO();
    
    double totalWeightedPercentage = 0;
    int subjectCount = 0;
    int failedCount = 0;
    
    for (String subject : selectedSubjects) {
        Map<String, Integer> examMarks = data.subjectMarks.get(subject);
        
        if (examMarks != null && !examMarks.isEmpty()) {
            // Calculate weighted total using AnalyzerDAO
            AnalyzerDAO.SubjectPassResult result = 
                dao.calculateWeightedSubjectTotalWithPass(
                    data.studentId, 
                    getSectionId(data.section), 
                    subject, 
                    examMarks.keySet());
            
            double weightedPercentage = Math.abs(result.percentage);
            boolean passed = result.passed;
            
            data.subjectWeightedTotals.put(subject, weightedPercentage);
            data.subjectPassStatus.put(subject, passed);
            
            totalWeightedPercentage += weightedPercentage;
            subjectCount++;
            
            if (!passed) {
                failedCount++;
                data.subjectFailedComponents.put(subject, result.failedComponents);
            }
        }
    }
    
    // Calculate overall metrics
    if (subjectCount > 0) {
        data.percentage = totalWeightedPercentage / subjectCount;
        data.sgpa = data.percentage / 10.0;
        data.grade = calculateGrade(data.percentage);
        data.status = (failedCount == 0) ? "Pass" : "Fail";
        data.totalMarks = totalWeightedPercentage;
        data.failedSubjectsCount = failedCount;
    }
}

private String calculateGrade(double percentage) {
    if (percentage >= 90) return "A+";
    if (percentage >= 80) return "A";
    if (percentage >= 70) return "B+";
    if (percentage >= 60) return "B";
    if (percentage >= 50) return "C";
    if (percentage >= 40) return "D";
    return "F";
}
```

### 4.5 Ranking System

```java
private void calculateRanks(List<ExtendedStudentData> students) {
    // Sort by total marks descending
    students.sort((s1, s2) -> Double.compare(s2.totalMarks, s1.totalMarks));
    
    // Assign ranks
    int currentRank = 1;
    double previousMarks = -1;
    int sameRankCount = 0;
    
    for (ExtendedStudentData student : students) {
        if (student.totalMarks == previousMarks) {
            // Same marks = same rank
            student.rank = currentRank - sameRankCount;
            sameRankCount++;
        } else {
            currentRank += sameRankCount;
            student.rank = currentRank;
            currentRank++;
            sameRankCount = 1;
            previousMarks = student.totalMarks;
        }
    }
}
```

---

## 5. Launched Results Mode

### 5.1 Overview

Launched Results Mode displays historical data from previously published results. The data is stored as JSON in the `launched_student_results` table and parsed dynamically to recreate the original table structure.

### 5.2 Data Retrieval

```java
private void displayLaunchedResults() {
    String selectedItem = (String) launchedResultsDropdown.getSelectedItem();
    
    if (selectedItem == null || selectedItem.startsWith("--")) {
        showStyledMessage("Please select a launched result!", 
                         "No Result Selected", 
                         JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // Extract launch ID from dropdown text: "#9 - 16-Jan-2026..."
    int launchId = extractLaunchId(selectedItem);
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        
        // Fetch all student results for this launch
        String query = "SELECT student_id, result_json " +
                      "FROM launched_student_results " +
                      "WHERE launch_id = ? " +
                      "ORDER BY student_id";
        
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, launchId);
        ResultSet rs = ps.executeQuery();
        
        List<ExtendedStudentData> studentDataList = new ArrayList<>();
        
        while (rs.next()) {
            String resultJson = rs.getString("result_json");
            
            // Parse JSON to extract student data
            ExtendedStudentData data = parseResultData(resultJson);
            
            if (data != null) {
                studentDataList.add(data);
            }
        }
        
        // Build and display table
        buildTableFromData(studentDataList, true);
        
    } catch (SQLException e) {
        e.printStackTrace();
        showStyledMessage("Error loading launched results: " + e.getMessage(),
                         "Database Error",
                         JOptionPane.ERROR_MESSAGE);
    }
}
```

### 5.3 JSON Parsing

The JSON structure stored in the database:

```json
{
  "student_info": {
    "student_id": 123,
    "name": "John Doe",
    "roll_number": "2024CS001",
    "email": "john@example.com",
    "phone": "9876543210"
  },
  "config": {
    "section": "CS-A",
    "year": 2024,
    "semester": 5,
    "launch_date": "2026-01-16T23:50:00"
  },
  "subjects": [
    {
      "subject_name": "Computer Networks",
      "exam_types": [
        {
          "exam_name": "Lab Internal",
          "obtained": 18,
          "max": 20,
          "weightage": 5.0
        },
        {
          "exam_name": "Theory Final",
          "obtained": 85,
          "max": 100,
          "weightage": 70.0
        }
      ],
      "weighted_total": 62.4,
      "passed": true
    }
  ],
  "overall": {
    "percentage": 78.5,
    "sgpa": 7.85,
    "grade": "B+",
    "status": "Pass",
    "failed_subjects": 0
  },
  "ranking": {
    "rank": 12,
    "total_students": 50
  },
  "class_stats": {
    "average_percentage": 72.3,
    "highest_percentage": 92.1,
    "pass_percentage": 88.0
  }
}
```

**Parsing Implementation**:

```java
private ExtendedStudentData parseResultData(String jsonString) {
    ExtendedStudentData data = new ExtendedStudentData();
    
    try {
        // Extract student info
        data.studentId = extractJsonInt(jsonString, "student_id");
        data.name = extractJsonString(jsonString, "name");
        data.rollNumber = extractJsonString(jsonString, "roll_number");
        data.email = extractJsonString(jsonString, "email");
        data.phone = extractJsonString(jsonString, "phone");
        
        // Extract config
        data.section = extractJsonString(jsonString, "section");
        data.year = extractJsonInt(jsonString, "year");
        data.semester = extractJsonInt(jsonString, "semester");
        
        // Extract overall stats
        data.percentage = extractJsonDouble(jsonString, "percentage");
        data.sgpa = extractJsonDouble(jsonString, "sgpa");
        data.grade = extractJsonString(jsonString, "grade");
        data.status = extractJsonString(jsonString, "status");
        data.failedSubjectsCount = extractJsonInt(jsonString, "failed_subjects");
        data.rank = extractJsonInt(jsonString, "rank");
        
        // Extract subjects array
        parseSubjectsFromJson(jsonString, data);
        
        return data;
        
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

private void parseSubjectsFromJson(String jsonString, ExtendedStudentData data) {
    // Extract subjects array using regex
    Pattern subjectPattern = Pattern.compile(
        "\"subject_name\":\"([^\"]+)\".*?" +
        "\"exam_types\":\\[(.*?)\\].*?" +
        "\"weighted_total\":(\\d+(?:\\.\\d+)?).*?" +
        "\"passed\":(true|false)"
    );
    
    Matcher subjectMatcher = subjectPattern.matcher(jsonString);
    
    while (subjectMatcher.find()) {
        String subjectName = subjectMatcher.group(1);
        String examTypesJson = subjectMatcher.group(2);
        double weightedTotal = Double.parseDouble(subjectMatcher.group(3));
        boolean passed = Boolean.parseBoolean(subjectMatcher.group(4));
        
        // Store subject result
        data.subjectWeightedTotals.put(subjectName, weightedTotal);
        data.subjectPassStatus.put(subjectName, passed);
        
        // Parse exam types within subject
        parseExamTypesFromJson(examTypesJson, subjectName, data);
    }
}

private void parseExamTypesFromJson(String examTypesJson, 
                                    String subjectName, 
                                    ExtendedStudentData data) {
    // Extract individual exam types
    Pattern examPattern = Pattern.compile(
        "\"exam_name\":\"([^\"]+)\".*?" +
        "\"obtained\":(\\d+).*?" +
        "\"max\":(\\d+)"
    );
    
    Matcher examMatcher = examPattern.matcher(examTypesJson);
    
    // Initialize maps
    List<String> examTypesList = new ArrayList<>();
    Map<String, Integer> marksMap = new HashMap<>();
    Map<String, Integer> maxMarksMap = new HashMap<>();
    
    while (examMatcher.find()) {
        String examName = examMatcher.group(1);
        int obtained = Integer.parseInt(examMatcher.group(2));
        int max = Integer.parseInt(examMatcher.group(3));
        
        examTypesList.add(examName);
        marksMap.put(examName, obtained);
        maxMarksMap.put(examName, max);
    }
    
    // Store in data structures
    launchedResultsExamTypesMap.put(subjectName, examTypesList);
    data.subjectMarks.put(subjectName, marksMap);
    data.subjectMaxMarks.put(subjectName, maxMarksMap);
}
```

### 5.4 Column Generation for Launched Results

```java
private void buildColumnsForLaunchedResults() {
    List<String> columnNames = new ArrayList<>();
    
    // Student info columns (always include Name and Roll Number)
    if (nameCheckBox.isSelected()) columnNames.add("Name");
    if (rollNumberCheckBox.isSelected()) columnNames.add("Roll Number");
    if (emailCheckBox.isSelected()) columnNames.add("Email");
    if (phoneCheckBox.isSelected()) columnNames.add("Phone");
    
    // Section info columns (no duplicates)
    if (sectionCheckBox.isSelected()) columnNames.add("Section");
    if (yearCheckBox.isSelected()) columnNames.add("Year");
    if (semesterCheckBox.isSelected()) columnNames.add("Semester");
    
    // Subject columns with exam types
    for (String subject : selectedSubjects) {
        List<String> examTypes = launchedResultsExamTypesMap.get(subject);
        
        if (examTypes != null) {
            for (String examType : examTypes) {
                String columnName = subject + " - " + examType;
                columnNames.add(columnName);
            }
            
            // Add subject total column
            columnNames.add(subject + " - Total");
        }
    }
    
    // Overall stats columns
    if (totalMarksCheckBox.isSelected()) columnNames.add("Total Marks");
    if (percentageCheckBox.isSelected()) columnNames.add("Percentage");
    if (sgpaCheckBox.isSelected()) columnNames.add("SGPA");
    if (gradeCheckBox.isSelected()) columnNames.add("Grade");
    if (statusCheckBox.isSelected()) columnNames.add("Status");
    if (rankCheckBox.isSelected()) columnNames.add("Rank");
    if (failedSubjectsCheckBox.isSelected()) columnNames.add("Failed Subjects");
    
    return columnNames;
}
```

### 5.5 Row Population for Launched Results

```java
private Object[] buildRowForStudent(ExtendedStudentData data, 
                                    boolean isLaunchedResult) {
    List<Object> rowData = new ArrayList<>();
    
    // Student info
    if (nameCheckBox.isSelected()) rowData.add(data.name);
    if (rollNumberCheckBox.isSelected()) rowData.add(data.rollNumber);
    if (emailCheckBox.isSelected()) rowData.add(data.email != null ? data.email : "N/A");
    if (phoneCheckBox.isSelected()) rowData.add(data.phone != null ? data.phone : "N/A");
    
    // Section info
    if (sectionCheckBox.isSelected()) rowData.add(data.section);
    if (yearCheckBox.isSelected()) rowData.add(data.year);
    if (semesterCheckBox.isSelected()) rowData.add(data.semester);
    
    // Subject marks
    for (String subject : selectedSubjects) {
        // Choose correct exam types map based on mode
        Map<String, List<String>> examTypesMap = isLaunchedResult 
            ? launchedResultsExamTypesMap 
            : subjectExamTypesMap;
        
        List<String> examTypes = examTypesMap.get(subject);
        
        if (examTypes != null) {
            Map<String, Integer> subjectMarks = data.subjectMarks.get(subject);
            
            // Individual exam marks
            for (String examType : examTypes) {
                Integer marks = subjectMarks != null 
                    ? subjectMarks.get(examType) 
                    : null;
                rowData.add(marks != null ? marks : "N/A");
            }
            
            // Subject total
            Double weightedTotal = data.subjectWeightedTotals.get(subject);
            rowData.add(weightedTotal != null 
                ? String.format("%.1f", weightedTotal) 
                : "N/A");
        }
    }
    
    // Overall stats
    if (totalMarksCheckBox.isSelected()) {
        rowData.add(String.format("%.1f", data.totalMarks));
    }
    if (percentageCheckBox.isSelected()) {
        rowData.add(String.format("%.2f%%", data.percentage));
    }
    if (sgpaCheckBox.isSelected()) {
        rowData.add(String.format("%.2f", data.sgpa));
    }
    if (gradeCheckBox.isSelected()) {
        rowData.add(data.grade);
    }
    if (statusCheckBox.isSelected()) {
        rowData.add(data.status);
    }
    if (rankCheckBox.isSelected()) {
        rowData.add(data.rank);
    }
    if (failedSubjectsCheckBox.isSelected()) {
        rowData.add(data.failedSubjectsCount);
    }
    
    return rowData.toArray();
}
```

---

## 6. Multi-Row Header System

### 6.1 Overview

The multi-row header system creates a hierarchical table structure with:
- **Row 1**: Subject names (merged cells spanning exam type columns)
- **Row 2**: Exam type names with max marks display

### 6.2 Header Structure

```
┌─────────────┬──────────┬─────────────────────────────────────┬──────────────┐
│ Name        │ Roll No  │     Computer Networks               │  Total Marks │
├─────────────┼──────────┼──────┬──────┬──────┬──────┬────────┼──────────────┤
│             │          │Lab I │Theory│Lab F │Theory│ Total  │              │
│             │          │(20)  │Final │(30)  │Int   │        │              │
│             │          │      │(100) │      │(50)  │        │              │
└─────────────┴──────────┴──────┴──────┴──────┴──────┴────────┴──────────────┘
```

### 6.3 Implementation

```java
private void setupMultiRowHeader(JTable table, 
                                 List<String> subjects,
                                 Map<String, List<String>> examTypesMap,
                                 Map<String, Map<String, Integer>> maxMarksMap) {
    
    JTableHeader header = table.getTableHeader();
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            // Determine if this is a subject column
            String columnName = table.getColumnName(column);
            boolean isSubjectColumn = false;
            String currentSubject = null;
            
            for (String subject : subjects) {
                if (columnName.startsWith(subject)) {
                    isSubjectColumn = true;
                    currentSubject = subject;
                    break;
                }
            }
            
            if (isSubjectColumn && currentSubject != null) {
                // Multi-line header with max marks
                String examTypePart = columnName.substring(
                    currentSubject.length() + 3); // Remove "Subject - "
                
                if (examTypePart.equals("Total")) {
                    label.setText("<html><center>" + currentSubject + 
                                 "<br><b>Total</b></center></html>");
                } else {
                    // Get max marks for this exam type
                    Integer maxMarks = null;
                    if (maxMarksMap.containsKey(currentSubject)) {
                        maxMarks = maxMarksMap.get(currentSubject).get(examTypePart);
                    }
                    
                    String maxMarksStr = maxMarks != null 
                        ? "<br><small>(" + maxMarks + ")</small>" 
                        : "";
                    
                    label.setText("<html><center>" + currentSubject + 
                                 "<br><b>" + examTypePart + "</b>" + 
                                 maxMarksStr + "</center></html>");
                }
            } else {
                // Simple single-line header
                label.setText("<html><center><b>" + columnName + 
                             "</b></center></html>");
            }
            
            // Styling
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setBackground(new Color(59, 130, 246)); // Blue
            label.setForeground(Color.WHITE);
            label.setBorder(BorderFactory.createMatteBorder(
                1, 1, 1, 1, Color.GRAY));
            
            // Increase header height for multi-line text
            header.setPreferredSize(new Dimension(
                header.getWidth(), 50));
            
            return label;
        }
    };
    
    // Apply renderer to all columns
    for (int i = 0; i < table.getColumnCount(); i++) {
        table.getColumnModel().getColumn(i).setHeaderRenderer(renderer);
    }
}
```

### 6.4 Dynamic Header Height

```java
private void adjustHeaderHeight(JTable table) {
    JTableHeader header = table.getTableHeader();
    
    // Calculate required height based on max lines in any header
    int maxLines = 1;
    
    for (int i = 0; i < table.getColumnCount(); i++) {
        String headerText = table.getColumnName(i);
        
        // Count HTML line breaks
        int lines = 1 + (headerText.length() - 
                        headerText.replace("<br>", "").length()) / 4;
        
        maxLines = Math.max(maxLines, lines);
    }
    
    // Set height: 20px base + 18px per additional line
    int headerHeight = 20 + (maxLines - 1) * 18;
    header.setPreferredSize(new Dimension(header.getWidth(), headerHeight));
}
```

---

## 7. Export System

### 7.1 Overview

The export system provides two formats:
1. **PDF Export**: A4 landscape (842×595pt) with adaptive sizing
2. **Excel Export**: Professional formatting with merged cells

### 7.2 PDF Export

#### 7.2.1 Page Setup

```java
private void exportToPDF() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save PDF Report");
    fileChooser.setSelectedFile(new File("Student_Results.pdf"));
    
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        
        try {
            // A4 Landscape: 842 x 595 points
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            
            document.open();
            
            // Add header
            addPDFHeader(document);
            
            // Add table
            addPDFTable(document);
            
            document.close();
            
            showStyledMessage("PDF exported successfully!",
                             "Export Complete",
                             JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            e.printStackTrace();
            showStyledMessage("Error exporting PDF: " + e.getMessage(),
                             "Export Error",
                             JOptionPane.ERROR_MESSAGE);
        }
    }
}
```

#### 7.2.2 Professional Header

```java
private void addPDFHeader(Document document) throws DocumentException {
    // Institution name
    Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    Paragraph title = new Paragraph("ACADEMIC MANAGEMENT SYSTEM", titleFont);
    title.setAlignment(Element.ALIGN_CENTER);
    title.setSpacingAfter(5);
    document.add(title);
    
    // Report title
    Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    Paragraph subtitle = new Paragraph("Student Results Report", subtitleFont);
    subtitle.setAlignment(Element.ALIGN_CENTER);
    subtitle.setSpacingAfter(3);
    document.add(subtitle);
    
    // Metadata
    Font metaFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    
    String sectionInfo = "Section: " + getSelectedSectionName();
    String dateInfo = "Generated: " + 
        new SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new Date());
    String studentCount = "Total Students: " + resultTable.getRowCount();
    
    Paragraph meta = new Paragraph(
        sectionInfo + "  |  " + dateInfo + "  |  " + studentCount, 
        metaFont);
    meta.setAlignment(Element.ALIGN_CENTER);
    meta.setSpacingAfter(10);
    document.add(meta);
    
    // Separator line
    LineSeparator line = new LineSeparator();
    line.setLineWidth(1f);
    document.add(new Chunk(line));
    document.add(Chunk.NEWLINE);
}
```

#### 7.2.3 Adaptive Table Sizing

```java
private void addPDFTable(Document document) throws DocumentException {
    int columnCount = resultTable.getColumnCount();
    
    // Create PDF table
    PdfPTable pdfTable = new PdfPTable(columnCount);
    pdfTable.setWidthPercentage(100);
    
    // Calculate adaptive font size
    Font headerFont = calculateAdaptiveFont(columnCount, true);
    Font dataFont = calculateAdaptiveFont(columnCount, false);
    
    // Set column widths dynamically
    float[] columnWidths = calculateColumnWidths(columnCount);
    pdfTable.setWidths(columnWidths);
    
    // Add headers
    for (int i = 0; i < columnCount; i++) {
        String headerText = resultTable.getColumnName(i);
        
        PdfPCell cell = new PdfPCell(new Phrase(headerText, headerFont));
        cell.setBackgroundColor(new BaseColor(59, 130, 246)); // Blue
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        
        pdfTable.addCell(cell);
    }
    
    // Add data rows with alternating colors
    for (int row = 0; row < resultTable.getRowCount(); row++) {
        BaseColor rowColor = (row % 2 == 0) 
            ? BaseColor.WHITE 
            : new BaseColor(249, 250, 251); // Light gray
        
        for (int col = 0; col < columnCount; col++) {
            Object value = resultTable.getValueAt(row, col);
            String text = value != null ? value.toString() : "";
            
            PdfPCell cell = new PdfPCell(new Phrase(text, dataFont));
            cell.setBackgroundColor(rowColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(3);
            
            pdfTable.addCell(cell);
        }
    }
    
    document.add(pdfTable);
}

private Font calculateAdaptiveFont(int columnCount, boolean isHeader) {
    int fontSize;
    
    if (columnCount >= 35) {
        fontSize = isHeader ? 5 : 4;
    } else if (columnCount >= 25) {
        fontSize = isHeader ? 7 : 6;
    } else if (columnCount >= 15) {
        fontSize = isHeader ? 8 : 7;
    } else {
        fontSize = isHeader ? 10 : 9;
    }
    
    int style = isHeader ? Font.BOLD : Font.NORMAL;
    return new Font(Font.FontFamily.HELVETICA, fontSize, style);
}

private float[] calculateColumnWidths(int columnCount) {
    float[] widths = new float[columnCount];
    
    for (int i = 0; i < columnCount; i++) {
        String columnName = resultTable.getColumnName(i);
        
        // Name and Email columns get more space
        if (columnName.equalsIgnoreCase("Name")) {
            widths[i] = 3.5f;
        } else if (columnName.equalsIgnoreCase("Email")) {
            widths[i] = 3.0f;
        } else if (columnName.equalsIgnoreCase("Roll Number")) {
            widths[i] = 2.0f;
        } else if (columnName.contains("Total")) {
            widths[i] = 1.5f;
        } else {
            widths[i] = 0.6f; // Compact for exam marks
        }
    }
    
    return widths;
}
```

### 7.3 Excel Export

#### 7.3.1 Workbook Setup

```java
private void exportToExcel() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save Excel Report");
    fileChooser.setSelectedFile(new File("Student_Results.xlsx"));
    
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Student Results");
            
            // Create styles
            Map<String, CellStyle> styles = createExcelStyles(workbook);
            
            // Add header rows
            int currentRow = addExcelHeader(sheet, styles);
            
            // Add data table
            addExcelTable(sheet, styles, currentRow);
            
            // Auto-size columns
            autoSizeColumns(sheet);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            
            showStyledMessage("Excel exported successfully!",
                             "Export Complete",
                             JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            e.printStackTrace();
            showStyledMessage("Error exporting Excel: " + e.getMessage(),
                             "Export Error",
                             JOptionPane.ERROR_MESSAGE);
        }
    }
}
```

#### 7.3.2 Professional Styling

```java
private Map<String, CellStyle> createExcelStyles(XSSFWorkbook workbook) {
    Map<String, CellStyle> styles = new HashMap<>();
    
    // Title style
    CellStyle titleStyle = workbook.createCellStyle();
    Font titleFont = workbook.createFont();
    titleFont.setBold(true);
    titleFont.setFontHeightInPoints((short) 16);
    titleStyle.setFont(titleFont);
    titleStyle.setAlignment(HorizontalAlignment.CENTER);
    styles.put("title", titleStyle);
    
    // Subtitle style
    CellStyle subtitleStyle = workbook.createCellStyle();
    Font subtitleFont = workbook.createFont();
    subtitleFont.setBold(true);
    subtitleFont.setFontHeightInPoints((short) 12);
    subtitleStyle.setFont(subtitleFont);
    subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
    styles.put("subtitle", subtitleStyle);
    
    // Header style (blue background, white text)
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerFont.setColor(IndexedColors.WHITE.getIndex());
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);
    headerStyle.setBorderTop(BorderStyle.THIN);
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBorderLeft(BorderStyle.THIN);
    headerStyle.setBorderRight(BorderStyle.THIN);
    styles.put("header", headerStyle);
    
    // Data style (alternating rows)
    CellStyle dataStyle = workbook.createCellStyle();
    dataStyle.setAlignment(HorizontalAlignment.CENTER);
    dataStyle.setBorderTop(BorderStyle.THIN);
    dataStyle.setBorderBottom(BorderStyle.THIN);
    dataStyle.setBorderLeft(BorderStyle.THIN);
    dataStyle.setBorderRight(BorderStyle.THIN);
    styles.put("data", dataStyle);
    
    // Alternating row style
    CellStyle altRowStyle = workbook.createCellStyle();
    altRowStyle.cloneStyleFrom(dataStyle);
    altRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    altRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    styles.put("altRow", altRowStyle);
    
    return styles;
}
```

#### 7.3.3 Header Rows with Merged Cells

```java
private int addExcelHeader(XSSFSheet sheet, Map<String, CellStyle> styles) {
    int rowNum = 0;
    
    // Row 1: Institution name (merged across all columns)
    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("ACADEMIC MANAGEMENT SYSTEM");
    titleCell.setCellStyle(styles.get("title"));
    
    int columnCount = resultTable.getColumnCount();
    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));
    
    // Row 2: Report title
    Row subtitleRow = sheet.createRow(rowNum++);
    Cell subtitleCell = subtitleRow.createCell(0);
    subtitleCell.setCellValue("Student Results Report");
    subtitleCell.setCellStyle(styles.get("subtitle"));
    sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnCount - 1));
    
    // Row 3: Metadata
    Row metaRow = sheet.createRow(rowNum++);
    Cell metaCell = metaRow.createCell(0);
    String metadata = String.format("Section: %s  |  Generated: %s  |  Students: %d",
        getSelectedSectionName(),
        new SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new Date()),
        resultTable.getRowCount());
    metaCell.setCellValue(metadata);
    sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, columnCount - 1));
    
    // Empty row
    rowNum++;
    
    return rowNum;
}
```

#### 7.3.4 Data Table

```java
private void addExcelTable(XSSFSheet sheet, 
                           Map<String, CellStyle> styles, 
                           int startRow) {
    int rowNum = startRow;
    
    // Header row
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < resultTable.getColumnCount(); i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(resultTable.getColumnName(i));
        cell.setCellStyle(styles.get("header"));
    }
    
    // Data rows
    for (int i = 0; i < resultTable.getRowCount(); i++) {
        Row dataRow = sheet.createRow(rowNum++);
        
        CellStyle rowStyle = (i % 2 == 0) 
            ? styles.get("data") 
            : styles.get("altRow");
        
        for (int j = 0; j < resultTable.getColumnCount(); j++) {
            Cell cell = dataRow.createCell(j);
            Object value = resultTable.getValueAt(i, j);
            
            if (value != null) {
                cell.setCellValue(value.toString());
            }
            
            cell.setCellStyle(rowStyle);
        }
    }
}

private void autoSizeColumns(XSSFSheet sheet) {
    for (int i = 0; i < resultTable.getColumnCount(); i++) {
        sheet.autoSizeColumn(i);
        
        // Set maximum width to prevent excessive column sizes
        int currentWidth = sheet.getColumnWidth(i);
        int maxWidth = 25 * 256; // 25 characters
        
        if (currentWidth > maxWidth) {
            sheet.setColumnWidth(i, maxWidth);
        }
    }
}
```

---

## 8. Data Models

### 8.1 ExtendedStudentData Class

```java
private static class ExtendedStudentData {
    // Student info
    int studentId;
    String name;
    String rollNumber;
    String email;
    String phone;
    String section;
    int year;
    int semester;
    
    // Subject marks: subject -> (examType -> marks)
    Map<String, Map<String, Integer>> subjectMarks = new HashMap<>();
    
    // Max marks: subject -> (examType -> maxMarks)
    Map<String, Map<String, Integer>> subjectMaxMarks = new HashMap<>();
    
    // Subject results
    Map<String, Double> subjectWeightedTotals = new HashMap<>();
    Map<String, Boolean> subjectPassStatus = new HashMap<>();
    Map<String, List<String>> subjectFailedComponents = new HashMap<>();
    
    // Overall stats
    double totalMarks;
    double percentage;
    double sgpa;
    String grade;
    String status;
    int rank;
    int failedSubjectsCount;
    
    // Constructor
    public ExtendedStudentData() {
        this.email = "N/A";
        this.phone = "N/A";
        this.totalMarks = 0;
        this.percentage = 0;
        this.sgpa = 0;
        this.grade = "F";
        this.status = "Fail";
        this.rank = 0;
        this.failedSubjectsCount = 0;
    }
}
```

### 8.2 SectionInfo Class

```java
private static class SectionInfo {
    int sectionId;
    String sectionName;
    int year;
    int semester;
    int studentCount;
    
    public SectionInfo(int id, String name, int year, int semester, int count) {
        this.sectionId = id;
        this.sectionName = name;
        this.year = year;
        this.semester = semester;
        this.studentCount = count;
    }
}
```

### 8.3 ColumnGroup Class

```java
private static class ColumnGroup {
    String groupName;
    List<String> columns;
    
    public ColumnGroup(String name) {
        this.groupName = name;
        this.columns = new ArrayList<>();
    }
    
    public void addColumn(String columnName) {
        this.columns.add(columnName);
    }
    
    public int getColumnCount() {
        return columns.size();
    }
}
```

---

## 9. Best Practices and Recommendations

### 9.1 Performance Optimization

1. **Lazy Loading**: Load data only when "Display" button is clicked
2. **Batch Database Queries**: Fetch all marks for a section in one query
3. **Index Usage**: Ensure proper indexes on `student_id`, `section_id`, `subject_name`
4. **Connection Pooling**: Reuse database connections instead of creating new ones
5. **Pagination**: For large datasets (>500 students), implement pagination

### 9.2 Data Validation

```java
private boolean validateTableData() {
    if (resultTable.getRowCount() == 0) {
        showStyledMessage("No data to display!",
                         "Empty Table",
                         JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    if (resultTable.getColumnCount() < 2) {
        showStyledMessage("Insufficient columns to display!",
                         "Invalid Configuration",
                         JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    return true;
}
```

### 9.3 Error Handling

```java
private void safeExecute(Runnable operation, String errorContext) {
    try {
        operation.run();
    } catch (Exception e) {
        e.printStackTrace();
        
        String errorMessage = String.format(
            "Error in %s: %s",
            errorContext,
            e.getMessage()
        );
        
        showStyledMessage(errorMessage,
                         "Operation Failed",
                         JOptionPane.ERROR_MESSAGE);
        
        // Log to file
        logError(errorContext, e);
    }
}
```

### 9.4 Memory Management

```java
private void clearCachedData() {
    // Clear maps
    sectionStudents.clear();
    subjectExamTypesMap.clear();
    maxMarksMap.clear();
    launchedResultsExamTypesMap.clear();
    launchedResultsMaxMarksMap.clear();
    
    // Clear UI components
    if (resultTable != null) {
        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        model.setRowCount(0);
    }
    
    // Force garbage collection (optional)
    System.gc();
}
```

### 9.5 UI Responsiveness

```java
private void displayDataInBackground() {
    // Show loading indicator
    showLoadingDialog("Loading data...");
    
    // Execute in background thread
    SwingWorker<List<ExtendedStudentData>, Void> worker = 
        new SwingWorker<List<ExtendedStudentData>, Void>() {
            
            @Override
            protected List<ExtendedStudentData> doInBackground() {
                return fetchStudentData();
            }
            
            @Override
            protected void done() {
                try {
                    List<ExtendedStudentData> data = get();
                    buildTableFromData(data, false);
                    hideLoadingDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                    hideLoadingDialog();
                    showError("Failed to load data: " + e.getMessage());
                }
            }
        };
    
    worker.execute();
}
```

---

## 10. Troubleshooting Guide

### 10.1 Common Issues

**Issue 1: Duplicate Year/Semester Columns**
- **Cause**: Adding columns twice in `displaySelectedData()`
- **Solution**: Ensure columns are added only once, check loop boundaries

**Issue 2: Marks Showing "N/A"**
- **Cause**: Using wrong exam types map for launched results
- **Solution**: Use `launchedResultsExamTypesMap` when `showLaunchedResults=true`

**Issue 3: Max Marks Not Displaying**
- **Cause**: `subjectMaxMarks` field not populated during JSON parsing
- **Solution**: Extract max marks from JSON exam types array

**Issue 4: PDF Export Columns Not Fitting**
- **Cause**: Fixed font size for all column counts
- **Solution**: Implement adaptive font sizing based on column count

**Issue 5: Excel File Corrupted**
- **Cause**: Workbook not closed properly
- **Solution**: Use try-with-resources for `XSSFWorkbook`

### 10.2 Debugging Tips

1. **Enable SQL Logging**: Print all database queries for inspection
2. **JSON Validation**: Use online JSON validators for launched results data
3. **Table Model Inspection**: Print column names and row data before rendering
4. **Export Preview**: Test with small datasets before full export

---

## 11. Future Enhancements

### 11.1 Planned Features

1. **Advanced Filtering**
   - Filter by percentage range
   - Filter by pass/fail status
   - Filter by grade
   - Multi-column sorting

2. **Statistical Analysis**
   - Class average calculation
   - Standard deviation display
   - Top performers highlighting
   - Trend graphs

3. **Custom Report Templates**
   - User-defined PDF layouts
   - Logo and watermark support
   - Custom color schemes
   - Report scheduling

4. **Export Enhancements**
   - CSV export option
   - Direct email delivery
   - Cloud storage integration
   - Batch export for multiple sections

5. **Real-time Updates**
   - Auto-refresh when marks are updated
   - WebSocket integration
   - Change notifications

6. **Accessibility**
   - Screen reader support
   - Keyboard navigation
   - High contrast mode
   - Font size customization

7. **Mobile View**
   - Responsive table design
   - Touch-friendly controls
   - Mobile export formats

---

## 12. Conclusion

The View Selection Tool is a comprehensive, flexible data visualization component that serves as the primary interface for viewing and analyzing student academic performance. Its dual-mode operation (Section Analysis and Launched Results), combined with powerful export capabilities and customizable display options, makes it an essential tool for academic administrators and teachers.

### Key Strengths:
- **Flexibility**: Customizable columns and subjects
- **Dual Modes**: Real-time and historical data viewing
- **Professional Exports**: A4 landscape PDF and styled Excel
- **Performance**: Optimized database queries and rendering
- **User Experience**: Intuitive checkboxes and dropdowns

### Maintenance Recommendations:
1. Regularly review database query performance
2. Test with large datasets (1000+ students)
3. Update export libraries to latest versions
4. Gather user feedback for UI improvements
5. Monitor memory usage during exports

For detailed information about related components, refer to:
- [Result Launcher Technical Documentation](RESULT_LAUNCHER_TECHNICAL_DOC.md)
- [Database Schema Analysis](../DATABASE_SCHEMA_ANALYSIS.md)
- [Marks Calculation Guide](../guides/MARKS_CALCULATION_GUIDE.md)

---

**Document Version**: 1.0  
**Last Updated**: January 17, 2026  
**Author**: Academic Analyzer Development Team