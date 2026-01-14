# Weighted Marks Calculation System

## Overview
The Academic Analyzer now uses a **weighted contribution system** for calculating subject totals, replacing the previous simple addition approach.

## Calculation Formula

### Subject Total Calculation
```
Subject Total (out of 100) = Σ (marks obtained in each component)
```

**Important:** Each component's marks are entered **directly out of its weightage**.

### Example Configuration
**Subject:** Computer Science (Total: 100 marks)
- **Mid Term 1:** Weightage = 15 (student enters marks out of 15)
- **Mid Term 2:** Weightage = 15 (student enters marks out of 15)  
- **Assignment:** Weightage = 20 (student enters marks out of 20)
- **Final Exam:** Weightage = 50 (student enters marks out of 50)

**Total Weightage:** 15 + 15 + 20 + 50 = **100**

### Sample Calculation
**Student Performance:**
- Mid Term 1: **12/15** (80%)
- Mid Term 2: **13/15** (86.67%)
- Assignment: **17/20** (85%)
- Final Exam: **38/50** (76%)

**Subject Total = 12 + 13 + 17 + 38 = 80/100 (80%)**

## Passing Criteria

A student must satisfy **BOTH** conditions to pass:

### 1. Overall Subject Passing
```
Subject Total ≥ Subject Passing Marks
```
Example: If subject passing marks = 40, then 80 ≥ 40 ✓ **PASS**

### 2. Component-Level Passing
```
Each Component Marks ≥ Component Passing Marks
```
Example (assuming 40% passing for each):
- Mid 1: 12 ≥ 6 (40% of 15) ✓
- Mid 2: 13 ≥ 6 ✓
- Assignment: 17 ≥ 8 (40% of 20) ✓
- Final: 38 ≥ 20 (40% of 50) ✓

**Result:** ✓ **PASS** (both criteria met)

### Failure Scenario
If a student scores:
- Mid 1: 12/15 ✓
- Mid 2: 13/15 ✓
- Assignment: 17/20 ✓
- Final: **15/50** ✗ (below 20)

**Subject Total = 57/100** (above 40) ✓  
**BUT Final Exam:** 15 < 20 ✗

**Result:** ✗ **FAIL** (failed in Final Exam component)

## Implementation Locations

### 1. Mark Entry (`MarkEntryDialog.java`)
- Students enter marks directly out of the weightage
- Example: For "Mid Term 1 (15%)", student enters 0-15
- The system displays: "Enter marks out of X" where X = weightage

### 2. Section Creation (`CreateSectionPanel.java`)
- When adding components, weightage represents max marks
- System validates that total weightages = 100
- Each component has:
  - **Name:** e.g., "Internal 1"
  - **Max Marks (Weightage):** e.g., 25
  - **Weightage %:** 25 (same value, represents 25% of 100)
  - **Passing Marks:** e.g., 10

### 3. Student Analyzer (`StudentAnalyzer.java`)
- Displays component-wise breakdown
- Shows weighted contribution of each component
- Calculates subject total as sum of all component marks
- Checks both passing criteria

### 4. Section Analyzer (`SectionAnalyzer.java`)
- Aggregates data using weighted totals
- Shows class average based on weighted system
- Identifies students failing in specific components
- Generates reports with weighted calculations

### 5. Database Layer (`AnalyzerDAO.java`)
- `calculateWeightedSubjectTotal()` method
- Fetches component weightages from `exam_types` table
- Joins with `marks` table to get obtained marks
- Returns calculated weighted total

## Database Schema

### `exam_types` Table
```sql
CREATE TABLE exam_types (
    id INT PRIMARY KEY AUTO_INCREMENT,
    section_id INT NOT NULL,
    exam_name VARCHAR(100),
    weightage INT NOT NULL,          -- Max marks (e.g., 15, 20, 50)
    passing_marks INT DEFAULT 0,     -- Min marks to pass this component
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### `marks` Table
```sql
CREATE TABLE marks (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    subject_id INT NOT NULL,
    exam_type_id INT NOT NULL,       -- FK to exam_types
    marks DECIMAL(5,2),               -- Marks obtained (out of weightage)
    FOREIGN KEY (exam_type_id) REFERENCES exam_types(id)
);
```

### `subject_exam_types` Linking Table
```sql
CREATE TABLE subject_exam_types (
    id INT PRIMARY KEY AUTO_INCREMENT,
    section_id INT NOT NULL,
    subject_id INT NOT NULL,
    exam_type_id INT NOT NULL,
    FOREIGN KEY (exam_type_id) REFERENCES exam_types(id)
);
```

## Key Benefits

1. **Flexible Weighting:** Different components can have different importance
2. **Industry Standard:** Aligns with university/college grading systems
3. **Component Accountability:** Students must pass each component, not just overall
4. **Scalable:** Easy to add/modify components without changing subject total
5. **Transparent:** Students see exact contribution of each component

## Migration Notes

### For Existing Data
- Old system used simple addition (e.g., 30+30+40 = 100)
- New system uses weighted contribution (marks entered out of weightage)
- No migration needed if marks were already entered correctly
- Verify weightage configuration matches your grading system

### Configuration Best Practices
1. **Total weightages should equal 100** (enforced by system)
2. **Set realistic passing marks** per component (typically 40% of weightage)
3. **Balance component weights** based on importance and difficulty
4. **Document grading scheme** for students and faculty

## User Interface Changes

### Mark Entry Dialog
**Old:** "Enter marks for Internal 1 (Max: 40)"  
**New:** "Enter marks out of 15 (Pass: 6)"

### Section Creation
- Added "Weightage (%)" field (required)
- Added "Passing Marks" field (required)
- Validation: Weightage total must = 100
- Validation: Passing marks ≤ Weightage

### Analysis Reports
- Shows component-wise breakdown
- Displays weighted contribution
- Highlights component failures
- Calculates accurate percentages

---

**Version:** 2.0  
**Last Updated:** January 10, 2026  
**Status:** ✓ Implemented and Active
