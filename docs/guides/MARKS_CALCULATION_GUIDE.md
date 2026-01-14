# Marks Calculation & Display Guide

## Overview
This document explains how marks are calculated, displayed, and validated in the Mark Entry Dialog, including the scaled grading formula, color coding system, and incomplete entry handling.

---

## 📐 Scaled Calculation Formula

### Core Formula:
```
Subject Total = Σ [(marks_obtained / max_marks) × weightage]
```

Where:
- **marks_obtained** = Student's score on the exam
- **max_marks** = Maximum possible score for that exam
- **weightage** = Percentage contribution to subject total
- **Σ** = Sum across all exam components

### Result:
- **Subject total is ALWAYS out of 100%**
- Automatically scales different exam types
- Handles mixed max marks (40, 100, etc.)

---

## 🧮 Calculation Examples

### Example 1: Cloud Computing (4 Components)

**Exam Configuration:**
| Component | Max Marks | Weightage | Passing Marks |
|-----------|-----------|-----------|---------------|
| Internal 1 | 40 | 10% | 18 |
| Internal 2 | 40 | 10% | 18 |
| Internal 3 | 40 | 10% | 18 |
| Final Exam | 100 | 70% | 40 |

**Student A (Topper) - Aarav Sharma:**
| Component | Obtained | Calculation | Contribution |
|-----------|----------|-------------|--------------|
| Internal 1 | 38 | (38/40) × 10 | 9.50% |
| Internal 2 | 39 | (39/40) × 10 | 9.75% |
| Internal 3 | 40 | (40/40) × 10 | 10.00% |
| Final Exam | 95 | (95/100) × 70 | 66.50% |
| **Total** | - | - | **95.75%** |

**Status:** Complete ✓

---

**Student B (Average):**
| Component | Obtained | Calculation | Contribution |
|-----------|----------|-------------|--------------|
| Internal 1 | 30 | (30/40) × 10 | 7.50% |
| Internal 2 | 32 | (32/40) × 10 | 8.00% |
| Internal 3 | 35 | (35/40) × 10 | 8.75% |
| Final Exam | 65 | (65/100) × 70 | 45.50% |
| **Total** | - | - | **69.75%** |

**Status:** Complete ✓

---

**Student C (Fail):**
| Component | Obtained | Calculation | Contribution |
|-----------|----------|-------------|--------------|
| Internal 1 | 15 ❌ | (15/40) × 10 | 3.75% |
| Internal 2 | 20 | (20/40) × 10 | 5.00% |
| Internal 3 | 25 | (25/40) × 10 | 6.25% |
| Final Exam | 35 ❌ | (35/100) × 70 | 24.50% |
| **Total** | - | - | **39.50%** ❌ |

**Status:** Complete (but FAILED - red components)

---

### Example 2: Gen AI (3 Components)

**Exam Configuration:**
| Component | Max Marks | Weightage | Passing Marks |
|-----------|-----------|-----------|---------------|
| Internal 1 | 40 | 25% | 18 |
| Internal 2 | 40 | 25% | 18 |
| Final Exam | 100 | 50% | 40 |

**Student D (Distinction):**
| Component | Obtained | Calculation | Contribution |
|-----------|----------|-------------|--------------|
| Internal 1 | 35 | (35/40) × 25 | 21.88% |
| Internal 2 | 36 | (36/40) × 25 | 22.50% |
| Final Exam | 82 | (82/100) × 50 | 41.00% |
| **Total** | - | - | **85.38%** |

**Status:** Complete ✓

---

## 🎨 Color Coding System

### Component-Level Color Coding:

The Mark Entry table uses color coding to provide visual feedback on student performance for each exam component.

#### Color Rules:
| Color | Condition | RGB Value | Meaning |
|-------|-----------|-----------|---------|
| 🔴 Red | `mark < passing_marks` | (254, 226, 226) | **FAIL** - Below pass threshold |
| 🟢 Green | `mark >= 80% of max_marks` | (220, 252, 231) | **Excellent** - 80%+ score |
| ⚪ White | `passing_marks ≤ mark < 80%` | (255, 255, 255) | **Pass** - Normal range |

### Code Implementation:
```java
// In MarkEntryDialog.java - Custom Cell Renderer (Lines 415-455)

public Component getTableCellRendererComponent(...) {
    // Get component metadata
    ExamTypeInfo examInfo = examTypes.get(column - 2);
    int maxMark = examInfo.maxMarks;
    int passingMark = examInfo.passingMarks;
    
    // Parse mark value
    double mark = Double.parseDouble(value.toString());
    
    // Apply color based on performance
    if (mark < passingMark) {
        // RED - Failed this component
        c.setBackground(new Color(254, 226, 226));
    } else if (mark >= (maxMark * 0.8)) {
        // GREEN - Excellent (80%+)
        c.setBackground(new Color(220, 252, 231));
    } else {
        // WHITE - Pass (between passing and 80%)
        c.setBackground(Color.WHITE);
    }
    
    return c;
}
```

### Visual Examples:

**Cloud Computing - Internal 1 (Max: 40, Pass: 18):**
- 15 marks → 🔴 RED (< 18 passing)
- 25 marks → ⚪ WHITE (18-31)
- 35 marks → 🟢 GREEN (≥ 32, which is 80% of 40)

**Final Exam (Max: 100, Pass: 40):**
- 35 marks → 🔴 RED (< 40 passing)
- 60 marks → ⚪ WHITE (40-79)
- 85 marks → 🟢 GREEN (≥ 80, which is 80% of 100)

---

## ✅ Incomplete Entry Validation

### Purpose:
Prevent displaying misleading partial totals when not all exam components have marks entered.

### Logic:
```java
// In calculateRowTotal() method (Lines 1390-1435)

private void calculateRowTotal(int row) {
    double total = 0;
    int filledCount = 0;
    
    // Count filled components
    for (int i = 0; i < examTypes.size(); i++) {
        Object value = tableModel.getValueAt(row, i + 2);
        if (value != null && !value.toString().trim().isEmpty()) {
            double marksObtained = Double.parseDouble(value.toString());
            ExamTypeInfo examInfo = examTypes.get(i);
            double contribution = (marksObtained / examInfo.maxMarks) * examInfo.weightage;
            total += contribution;
            filledCount++;
        }
    }
    
    // Only show total if ALL components are filled
    if (filledCount == examTypes.size()) {
        tableModel.setValueAt(String.format("%.2f", total), row, examTypes.size() + 2);
        tableModel.setValueAt("Complete", row, examTypes.size() + 3);
    } else {
        tableModel.setValueAt("", row, examTypes.size() + 2);
        tableModel.setValueAt("Incomplete (" + filledCount + "/" + examTypes.size() + ")", 
                             row, examTypes.size() + 3);
    }
}
```

### Display States:

#### State 1: Complete Entry ✓
```
| Internal 1 | Internal 2 | Internal 3 | Final | Total | Status |
|------------|------------|------------|-------|-------|--------|
| 38         | 39         | 40         | 95    | 95.75 | Complete |
```

#### State 2: Incomplete Entry ⚠️
```
| Internal 1 | Internal 2 | Internal 3 | Final | Total | Status |
|------------|------------|------------|-------|-------|------------------|
| 38         | 39         |            | 95    |       | Incomplete (3/4) |
```

#### State 3: No Entry
```
| Internal 1 | Internal 2 | Internal 3 | Final | Total | Status |
|------------|------------|------------|-------|-------|------------------|
|            |            |            |       |       | Incomplete (0/4) |
```

### Benefits:
- ✓ Prevents confusion from partial totals (e.g., showing 75.25% when only 3/4 exams entered)
- ✓ Clear indication of data entry progress
- ✓ Forces complete data entry before showing final marks
- ✓ Status column shows exactly how many components are filled

---

## 📊 Mark Entry Table Structure

### Column Layout:
```
[ Roll No ] [ Student Name ] [ Internal 1 (40) ] [ Internal 2 (40) ] [ Internal 3 (40) ] [ Final Exam (100) ] [ Total ] [ Status ]
     100px        200px              150px+              150px+              150px+              150px+           100px     120px
```

### Column Width Calculation:
```java
// Auto-size based on header text length (Lines 1226-1234)
String headerText = examInfo.name + " (" + examInfo.maxMarks + ")";
int calculatedWidth = Math.max(150, headerText.length() * 10);
marksTable.getColumnModel().getColumn(i).setPreferredWidth(calculatedWidth);
marksTable.getColumnModel().getColumn(i).setMinWidth(150);
```

### Features:
- **Auto-sizing** - Columns expand based on header text
- **Minimum width** - 150px ensures readability
- **Formula** - 10 pixels per character in header
- **Example:** "Internal 1 (40)" = 15 chars × 10px = 150px (uses minimum)
- **Example:** "Final Examination (100)" = 23 chars × 10px = 230px

---

## 🔄 Auto-Save Functionality

### Trigger:
- Marks are auto-saved when user moves to another cell (focus lost)
- No manual "Save" button required
- Instant database update

### Process:
```java
// In autoSaveMark() method
private void autoSaveMark(int row, int column) {
    String rollNumber = tableModel.getValueAt(row, 0).toString();
    Object markValue = tableModel.getValueAt(row, column);
    int examTypeId = examTypes.get(column - 2).id;
    
    // Get student_id from roll number
    int studentId = getStudentIdByRoll(rollNumber);
    
    // Parse marks
    double marks = Double.parseDouble(markValue.toString());
    
    // Save to database
    saveMarkToDatabase(studentId, currentSubjectId, examTypeId, marks);
    
    // Show success indicator (green checkmark animation)
    showSuccessIndicator(row, column);
}
```

### Validation:
- ✓ Marks must be numeric
- ✓ Marks must be ≤ max_marks for that component
- ✓ "ABS" is valid for absent students
- ✗ Empty is valid (treated as not entered)

---

## 📤 Export Functionality

### Supported Formats:
1. **Excel 2007+ (.xlsx)** - Modern format with styles
2. **Excel 97-2003 (.xls)** - Legacy compatibility
3. **PDF (.pdf)** - Print-ready report

### Export Process:
```java
// In showExportDialog() method
private void showExportDialog() {
    // Select format
    String[] options = {"Excel (.xlsx)", "Excel (.xls)", "PDF", "Cancel"};
    int choice = showOptionDialog(...);
    
    // Choose file location
    JFileChooser fileChooser = new JFileChooser();
    String suggestedName = "Marks_" + sectionName + "_" + subjectName + "_" + date + ".xlsx";
    
    // Export in background
    SwingWorker worker = new SwingWorker() {
        protected Boolean doInBackground() {
            exportToExcel(file, isXlsx);
            return true;
        }
    };
}
```

### Excel Export Features:
- ✅ Title row with section and subject name
- ✅ Styled header row (bold, grey background)
- ✅ Border around all cells
- ✅ Auto-sized columns for roll no and name
- ✅ Numeric values for marks (not text)
- ✅ Maintains all data including totals and status

### Excel Export Structure:
```
Row 1: [Title] Mark Entry - A ISE - CLOUD COMPUTING
Row 2: [Empty]
Row 3: [Headers] Roll No | Student Name | Internal 1 (40) | ... | Total | Status
Row 4+: [Data] 132 | Aarav Sharma | 38 | 39 | 40 | 95 | 95.75 | Complete
```

### PDF Export Features:
- ✅ Title: "Mark Entry Report"
- ✅ Info section: Section, Subject, Date/Time
- ✅ Complete table with all data
- ✅ Page breaks for large sections
- ✅ Professional formatting

### Verification Checklist:
After exporting, verify:
- [ ] All student names present
- [ ] All mark values correct
- [ ] Totals match application display
- [ ] Status column shows completion state
- [ ] Headers include max marks (e.g., "Internal 1 (40)")
- [ ] File opens correctly in Excel/PDF viewer

---

## 📥 Import Functionality

### Supported Formats:
- Excel 2007+ (.xlsx)
- Excel 97-2003 (.xls)

### Import Process:
```java
private void importMarksFromExcel(File file) {
    // Read Excel file
    Workbook workbook = WorkbookFactory.create(file);
    Sheet sheet = workbook.getSheetAt(0);
    
    // Match header row to exam types
    Row headerRow = sheet.getRow(0);
    Map<Integer, Integer> columnToExamIndex = new HashMap<>();
    
    for (int col = 2; col < headerRow.getLastCellNum(); col++) {
        String headerText = headerRow.getCell(col).getStringCellValue();
        // Find matching exam type by name
        for (int i = 0; i < examTypes.size(); i++) {
            if (examTypes.get(i).name.equals(headerText)) {
                columnToExamIndex.put(col, i);
                break;
            }
        }
    }
    
    // Import data rows
    for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
        Row row = sheet.getRow(rowNum);
        String rollNumber = getCellValueAsString(row.getCell(0));
        
        // Import marks for each exam type
        for (Map.Entry<Integer, Integer> entry : columnToExamIndex.entrySet()) {
            int excelCol = entry.getKey();
            int examIndex = entry.getValue();
            String markValue = getCellValueAsString(row.getCell(excelCol));
            
            // Save to database
            saveImportedMark(rollNumber, examTypes.get(examIndex).id, markValue);
        }
    }
}
```

### Import Requirements:
- ✅ First row must contain headers matching exam type names
- ✅ Roll numbers must exist in the selected section
- ✅ Marks must be valid numbers or "ABS"
- ✅ Excel file structure must match export format

### Error Handling:
- Invalid roll numbers → Skipped with warning
- Invalid marks → Skipped with error
- Missing headers → Import aborted
- File format error → User notified

---

## ⚠️ Common Issues & Solutions

### Issue 1: Total showing raw sum (e.g., 212.00)
**Problem:** Not using scaled formula
**Solution:** Verify ExamTypeInfo includes weightage and uses scaled calculation
```java
// WRONG:
total += marksObtained;

// CORRECT:
double contribution = (marksObtained / examInfo.maxMarks) * examInfo.weightage;
total += contribution;
```

### Issue 2: All components red regardless of marks
**Problem:** Hardcoded passing marks (e.g., always checking < 40)
**Solution:** Use component-specific passing marks
```java
// WRONG:
if (mark < 40) // Hardcoded

// CORRECT:
if (mark < examInfo.passingMarks) // Component-specific
```

### Issue 3: Showing partial total (e.g., 29.25 for 3/4 exams)
**Problem:** Not checking if all components are filled
**Solution:** Implemented - shows "Incomplete (3/4)" instead

### Issue 4: Export missing max_marks in headers
**Problem:** Headers show "Internal 1" instead of "Internal 1 (40)"
**Solution:** Verify export uses `examInfo.name + " (" + examInfo.maxMarks + ")"`

---

## 🎯 Testing Scenarios

### Test Case 1: Complete Entry
1. Enter all exam marks for a student
2. Verify total calculates correctly using scaled formula
3. Verify status shows "Complete"
4. Verify color coding correct for each component

### Test Case 2: Incomplete Entry
1. Enter only 2 out of 4 exam marks
2. Verify total column is empty
3. Verify status shows "Incomplete (2/4)"
4. Enter remaining marks
5. Verify total appears and status changes to "Complete"

### Test Case 3: Export & Re-Import
1. Enter marks for all students
2. Export to Excel
3. Clear all marks in application
4. Import from saved Excel file
5. Verify all marks restored correctly
6. Verify totals recalculate correctly

### Test Case 4: Color Coding
1. Enter failing mark (< passing)
2. Verify cell is red
3. Edit to passing mark (≥ passing, < 80%)
4. Verify cell is white
5. Edit to excellent mark (≥ 80% of max)
6. Verify cell is green

---

## 📌 Key Takeaways

1. **Scaled Formula** - Always uses (marks/max_marks) × weightage
2. **Subject total out of 100%** - Regardless of component max marks
3. **Incomplete validation** - Shows "Incomplete (x/y)" until all filled
4. **Color coding per component** - Uses actual passing marks
5. **Auto-save** - No manual save button required
6. **Export includes metadata** - Headers show max marks
7. **Import validates** - Checks roll numbers and mark values

---

## 📞 Support

For calculation issues:
1. Verify database has max_marks column
2. Check ExamTypeInfo loads all 5 fields (id, name, maxMarks, weightage, passingMarks)
3. Review calculateRowTotal() method for scaled formula
4. Test with known values (e.g., perfect scores should give 100%)

For export issues:
1. Check Apache POI library is in lib folder
2. Verify file permissions for export location
3. Test with small dataset first
4. Review export logs for errors

**Documentation Version:** 2.0 - Scaled Calculation System
**Last Updated:** January 11, 2026
