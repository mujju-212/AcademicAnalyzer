# Database Schema Analysis
**Date:** January 15, 2026  
**Purpose:** Schema dump and analysis for result data storage

---

## Database Configuration

### Connection Details
- **Source:** ConfigLoader.java (loads from .env or environment variables)
- **Database Type:** MySQL 8.x
- **Driver:** `com.mysql.cj.jdbc.Driver`
- **Configuration Keys:**
  - `DB_URL` - Database connection URL
  - `DB_USERNAME` - Database username
  - `DB_PASSWORD` - Database password

---

## Schema Overview (20 Tables)

### Core User & Authentication Tables
1. **users** - User accounts (teachers, admins)
2. **registration_otps** - OTPs for user registration
3. **password_reset_otps** - OTPs for password reset

### Academic Structure Tables
4. **sections** - Academic sections/classes
5. **subjects** - Subject definitions
6. **section_subjects** - Many-to-many: sections ↔ subjects
7. **students** - Student records
8. **subject_exam_types** - Mapping: subjects to exam types within sections

### Marking & Assessment Tables
9. **exam_types** - Exam definitions (tests, midterm, final, etc.)
10. **marking_schemes** - Flexible marking schemes per section-subject
11. **component_groups** - Groups for marking components (internal/external)
12. **marking_components** - Individual assessment components
13. **student_component_marks** - Marks for flexible system
14. **student_marks** - Marks for traditional system
15. **marks** - Alternative marks table (appears redundant)

### **Result Launcher Tables** ⭐
16. **launched_results** - Launch metadata and configuration
17. **student_web_results** - Calculated result data per student

---

## Result Storage Tables (Detailed Analysis)

### 1. `launched_results` Table
**Purpose:** Stores launch configuration and metadata when teacher launches results

| Column | Type | Description | Redundancy Check |
|--------|------|-------------|------------------|
| `id` | INT (PK) | Unique launch ID | ✅ Required |
| `launch_name` | VARCHAR(255) | Name given to this launch | ✅ Required |
| `section_id` | INT | Section for which results launched | ✅ Required |
| `component_ids` | TEXT | Comma-separated component IDs | ✅ Required (for filtering) |
| `student_ids` | TEXT | Comma-separated student IDs | ✅ Required (for filtering) |
| `launched_by` | INT (FK→users) | Teacher who launched | ✅ Required (audit) |
| `launch_date` | TIMESTAMP | When launched | ✅ Required (audit) |
| `status` | ENUM('active','inactive') | Current status | ✅ Required (lifecycle) |
| `email_sent` | TINYINT(1) | Email notification status | ✅ Required (tracking) |
| `show_component_marks` | TINYINT(1) | Visibility: component details | ✅ Required (config) |
| `show_subject_details` | TINYINT(1) | Visibility: subject breakdown | ✅ Required (config) |
| `show_rank` | TINYINT(1) | Visibility: student rank | ✅ Required (config) |
| `show_class_stats` | TINYINT(1) | Visibility: class statistics | ✅ Required (config) |
| `allow_pdf_download` | TINYINT(1) | Allow PDF generation | ✅ Required (config) |

**Indexes:**
- PRIMARY KEY on `id`
- `idx_launched_results_status` on `(status, launched_by)` - ✅ Good for filtering active launches

**Foreign Keys:**
- `section_id` → sections(id)
- `launched_by` → users(id)

**✅ NO REDUNDANCY:** All columns serve distinct purposes

---

### 2. `student_web_results` Table
**Purpose:** Stores calculated result data for each student in a launch

| Column | Type | Description | Redundancy Check |
|--------|------|-------------|------------------|
| `id` | INT (PK) | Unique result ID | ✅ Required |
| `launch_id` | INT (FK→launched_results) | Associated launch | ✅ Required |
| `student_id` | INT (FK→students) | Student for this result | ✅ Required |
| `result_data` | TEXT | JSON/serialized calculated data | ✅ Required (stores all calc fields) |
| `created_at` | TIMESTAMP | When result was calculated | ✅ Required (audit) |

**Indexes:**
- PRIMARY KEY on `id`
- `idx_student_web_results_launch_student` on `(launch_id, student_id)` - ✅ Good for lookup

**Foreign Keys:**
- `launch_id` → launched_results(id)
- `student_id` → students(id)

**✅ NO REDUNDANCY:** Minimal design, all columns necessary

**`result_data` Contents (JSON Structure):**
```json
{
  "studentId": 123,
  "studentName": "John Doe",
  "rollNumber": "CS101",
  "section": "CS-A",
  "rank": 5,
  "subjects": [
    {
      "subjectName": "Mathematics",
      "maxMarks": 100,
      "examTypes": [
        {"name": "Quiz 1", "obtained": 18, "max": 20},
        {"name": "Midterm", "obtained": 40, "max": 50},
        {"name": "Final", "obtained": 85, "max": 100}
      ],
      "totalObtained": 143,
      "totalMax": 170,
      "percentage": 84.12,
      "passed": true
    }
  ],
  "overallTotal": 756,
  "overallMax": 900,
  "percentage": 84.0,
  "grade": "A",
  "cgpa": 8.4,
  "status": "PASS",
  "classAverage": 72.5,
  "classHighest": 92.3,
  "classLowest": 45.6
}
```

---

## Calculated Fields Stored in `result_data`

### Per Student (Overall):
- ✅ **Rank** - Position in class
- ✅ **Total Obtained** - Sum of all marks
- ✅ **Total Max** - Sum of all possible marks
- ✅ **Percentage** - Overall percentage
- ✅ **Grade** - Letter grade (A, B, C, etc.)
- ✅ **CGPA** - Cumulative GPA (percentage/10)
- ✅ **Status** - PASS/FAIL (requires ALL subjects to pass)

### Per Subject:
- ✅ **Subject Name**
- ✅ **Max Marks** - Total possible for subject
- ✅ **Total Obtained** - Weighted total
- ✅ **Percentage** - Subject percentage
- ✅ **Pass Status** - Subject-level pass/fail

### Per Exam Type (within subject):
- ✅ **Exam Name** - e.g., "Quiz 1", "Midterm"
- ✅ **Obtained Marks** - Student's score
- ✅ **Max Marks** - Maximum possible

### Class Statistics (if enabled):
- ✅ **Class Average**
- ✅ **Class Highest**
- ✅ **Class Lowest**

---

## Redundancy Analysis

### ✅ NO REDUNDANCY FOUND

**Rationale:**
1. **`launched_results`** - Stores launch configuration (WHAT to show, WHO launched, WHEN)
2. **`student_web_results`** - Stores calculated data (WHAT VALUES to show)
3. **Separation is correct:** Configuration vs Data
4. **TEXT field for `result_data`** - Appropriate because:
   - Dynamic structure (flexible marking vs traditional)
   - Complex nested data (subjects → exam types → marks)
   - Avoids creating 20+ normalized tables
   - Pre-calculated for fast student access

### Alternative (Normalized) Approach Would Require:
```
- student_web_results (meta)
- student_result_subjects (one per subject)
- student_result_exam_types (one per exam per subject)
- student_result_stats (class statistics)
- student_result_rankings (rank data)
```
**Verdict:** Current JSON approach is MORE EFFICIENT for read-heavy workload

---

## Potential Marks Tables Redundancy

### Issue: Multiple Marks Tables
1. **`student_marks`** - Traditional system (old)
2. **`student_component_marks`** - Flexible system (new)
3. **`marks`** - Appears to be duplicate/old table

### Analysis:

**`student_marks`** (Traditional System):
```sql
student_id, subject_id, exam_type_id, marks_obtained
UNIQUE(student_id, subject_id, exam_type_old, academic_year)
```

**`student_component_marks`** (Flexible System):
```sql
student_id, component_id, marks_obtained, scaled_marks, is_counted
UNIQUE(student_id, component_id)
```

**`marks`** (Duplicate?):
```sql
student_id, exam_type_id, subject_id, marks
UNIQUE(student_id, exam_type_id, subject_id)
```

### ⚠️ REDUNDANCY DETECTED: `marks` vs `student_marks`

**Findings:**
- `marks` and `student_marks` store nearly identical data
- Both have UNIQUE constraint on (student, exam_type, subject)
- `marks.marks` is DECIMAL(5,2)
- `student_marks.marks_obtained` is INT
- `student_marks` has additional fields: `exam_type_old`, `academic_year`, `created_by`

**Recommendation:**
```sql
-- Option 1: Drop marks table if unused
DROP TABLE marks;

-- Option 2: Migrate data from marks to student_marks
INSERT INTO student_marks (student_id, subject_id, exam_type_id, marks_obtained)
SELECT student_id, subject_id, exam_type_id, ROUND(marks)
FROM marks
WHERE NOT EXISTS (
  SELECT 1 FROM student_marks sm 
  WHERE sm.student_id = marks.student_id 
    AND sm.subject_id = marks.subject_id 
    AND sm.exam_type_id = marks.exam_type_id
);

DROP TABLE marks;
```

---

## System Flow (Result Launcher)

```
1. Teacher clicks "Launch Results" in ResultLauncherDialog
   ↓
2. LaunchConfigurationDialog opens
   - Teacher configures visibility options
   - Teacher names the launch
   ↓
3. Preview shown in ResultPreviewDialog
   - Calculations performed in memory
   - No database writes yet
   ↓
4. Teacher confirms launch
   ↓
5. INSERT into `launched_results` table
   - Store metadata, config, component IDs, student IDs
   ↓
6. For each selected student:
   - Calculate all fields (rank, %, grade, CGPA)
   - Serialize to JSON
   - INSERT into `student_web_results`
   ↓
7. Students access web portal:
   - SELECT from student_web_results WHERE student_id = ?
   - Deserialize JSON
   - Apply visibility filters from launched_results
   - Render result page
```

---

## Recommendations

### 1. ✅ Result Storage Design is Correct
- Two tables are sufficient
- No redundancy in result launcher tables
- JSON approach is optimal for this use case

### 2. ⚠️ Remove `marks` Table
- Redundant with `student_marks`
- Consolidate to single table

### 3. ✅ Indexes are Optimal
- `idx_student_web_results_launch_student` - Good for lookup
- `idx_launched_results_status` - Good for filtering

### 4. Consider Adding:
```sql
-- Add index for faster student portal access
CREATE INDEX idx_student_web_results_student 
ON student_web_results(student_id, launch_id);

-- Add index for audit queries
CREATE INDEX idx_launched_results_date 
ON launched_results(launch_date DESC);
```

### 5. ✅ Foreign Keys Present
- All relationships properly enforced
- ON DELETE CASCADE where appropriate

---

## Conclusion

### Result Storage: ✅ PROPERLY DESIGNED
- `launched_results` - Stores launch configuration
- `student_web_results` - Stores calculated data
- NO redundancy between these two tables
- Both tables serve distinct purposes

### Marks Storage: ⚠️ REDUNDANCY FOUND
- `marks` table appears redundant with `student_marks`
- Recommend consolidation

### Overall Schema: ✅ WELL-STRUCTURED
- Proper normalization
- Good indexing strategy
- Foreign keys enforced
- Supports both traditional and flexible marking systems

---

**Schema File:** `docs/database/schema_current_2026-01-11.sql`  
**Update Script:** `docs/database/update_result_launcher_schema.sql`
