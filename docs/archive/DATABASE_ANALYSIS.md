# Database Analysis - academic_analyzer
**Date:** January 10, 2026
**Purpose:** Identify duplicate/inconsistent tables and data issues

---

## 🔴 CRITICAL ISSUES FOUND

### Issue 1: Multiple Exam Type Tables (CONFUSING & REDUNDANT)
**3 different tables storing exam types:**

1. **`exam_types`** (47 rows) - ✅ ACTIVELY USED
   - Stores: section_id, exam_name, weightage
   - Purpose: Section-wide exam components (e.g., "Mid Term 1", "Quiz", "Final Exam")
   - Used by: CreateSectionPanel for displaying exam patterns
   
2. **`section_exam_types`** (0 rows) - ❌ EMPTY/UNUSED
   - Stores: section_id, exam_name, exam_type ENUM('written','practical','assignment')
   - Purpose: Unknown - appears to be duplicate of exam_types
   - **RECOMMENDATION: DELETE THIS TABLE**
   
3. **`subject_exam_types`** (5 rows) - ⚠️ MINIMAL USE
   - Stores: section_id, subject_id, exam_type_id
   - Purpose: Links exam types to specific subjects
   - Problem: Not being used by current code (query uses exam_types directly)
   - **RECOMMENDATION: Either use this properly or delete it**

### Issue 2: Multiple Marks Storage Tables (INCONSISTENT DESIGN)
**3 different tables storing student marks:**

1. **`student_marks`** (290 rows) - ✅ ACTIVELY USED
   - Stores: student_id, subject_id, marks_obtained, **exam_type VARCHAR(50)**
   - Problem: exam_type is TEXT, not a foreign key to exam_types table
   - Issue: Leads to name mismatches (e.g., "Midterm" vs "Mid Term 1")
   - Current behavior: Free-text entry allows any exam type name
   
2. **`student_marks_detailed`** (0 rows) - ❌ EMPTY/UNUSED
   - Stores: student_id, subject_id, **exam_type_id INT**, marks_obtained
   - Purpose: Appears to be a better-designed version with FK to exam_types
   - **RECOMMENDATION: Either migrate to this table or delete it**
   
3. **`student_component_marks`** (45 rows) - ✅ ACTIVELY USED (different system)
   - Stores: student_id, component_id, marks_obtained, scaled_marks
   - Purpose: For the marking schemes system (component_groups/marking_components)
   - This is a SEPARATE system from exam_types
   - **Keep this - it's for the weightage-based marking system**

### Issue 3: Data Inconsistency in exam_types vs student_marks
**Example from Section 24:**

**exam_types table has:**
- Mid Term 1, Mid Term 2, Assignment, Quiz, Final Exam

**student_marks table has:**
- Midterm, Final, Lab, Project, Quiz

**Result:** Only "Quiz" matched between tables, causing all subjects to show same component

**Why this happened:**
- exam_types stores template names when section is created
- student_marks allows free-text entry when marks are entered
- No validation ensures consistency

---

## 📊 Table Usage Summary

| Table | Rows | Status | Purpose | Recommendation |
|-------|------|--------|---------|----------------|
| `exam_types` | 47 | ✅ Active | Section exam components | **Keep - primary table** |
| `section_exam_types` | 0 | ❌ Unused | Unknown/duplicate | **DELETE** |
| `subject_exam_types` | 5 | ⚠️ Minimal | Subject-specific exam links | **Use it or lose it** |
| `student_marks` | 290 | ✅ Active | Current marks storage | **Keep but needs FK constraint** |
| `student_marks_detailed` | 0 | ❌ Unused | Better-designed marks table | **DELETE or migrate** |
| `student_component_marks` | 45 | ✅ Active | Marking schemes system | **Keep - different purpose** |
| `component_marks` | 0 | ❌ Unused | Old marks system? | **DELETE** |
| `marks` | 5 | ⚠️ Minimal | Unknown purpose | **Investigate** |
| `subject_mark_distribution` | 0 | ❌ Unused | Unknown | **DELETE** |
| `subject_marking_schemes` | 0 | ❌ Unused | Unknown | **DELETE** |
| `marking_templates` | 0 | ❌ Unused | Templates system | **DELETE or implement** |

---

## 🔧 RECOMMENDED FIXES

### Fix 1: Clean Up Duplicate Tables (IMMEDIATE)
```sql
-- Delete unused tables
DROP TABLE IF EXISTS section_exam_types;
DROP TABLE IF EXISTS student_marks_detailed;
DROP TABLE IF EXISTS component_marks;
DROP TABLE IF EXISTS subject_mark_distribution;
DROP TABLE IF EXISTS subject_marking_schemes;
DROP TABLE IF EXISTS marking_templates;
```

### Fix 2: Fix Data Integrity for student_marks (CRITICAL)

**Option A: Add constraint to validate exam_type exists in exam_types**
```sql
-- This would require migrating existing mismatched data first
-- Then add a trigger or application-level validation

-- Clean up mismatched data first:
UPDATE student_marks sm
JOIN students s ON sm.student_id = s.id
SET sm.exam_type = 
  CASE 
    WHEN sm.exam_type = 'Midterm' THEN 'Mid Term 1'
    WHEN sm.exam_type = 'Final' THEN 'Final Exam'
    -- Add more mappings as needed
  END
WHERE s.section_id = 24;
```

**Option B: Change student_marks.exam_type to be a foreign key** (Better long-term)
```sql
-- 1. Add new column
ALTER TABLE student_marks ADD COLUMN exam_type_id INT AFTER subject_id;

-- 2. Migrate data by matching names
UPDATE student_marks sm
JOIN students s ON sm.student_id = s.id
JOIN exam_types et ON et.section_id = s.section_id 
  AND et.exam_name = sm.exam_type
SET sm.exam_type_id = et.id;

-- 3. Add foreign key constraint
ALTER TABLE student_marks 
  ADD CONSTRAINT fk_exam_type 
  FOREIGN KEY (exam_type_id) REFERENCES exam_types(id);

-- 4. Drop old varchar column
ALTER TABLE student_marks DROP COLUMN exam_type;
```

### Fix 3: Use subject_exam_types Properly (if keeping it)
```sql
-- Populate subject_exam_types based on actual marks data
INSERT INTO subject_exam_types (section_id, subject_id, exam_type_id)
SELECT DISTINCT 
  s.section_id,
  sm.subject_id,
  et.id
FROM student_marks sm
JOIN students s ON sm.student_id = s.id
JOIN exam_types et ON et.section_id = s.section_id 
  AND et.exam_name = sm.exam_type
WHERE NOT EXISTS (
  SELECT 1 FROM subject_exam_types set2
  WHERE set2.section_id = s.section_id
    AND set2.subject_id = sm.subject_id
    AND set2.exam_type_id = et.id
);

-- Then update the query in CreateSectionPanel to use this table
```

---

## 🎯 CURRENT SYSTEM ANALYSIS

### System 1: Simple Exam Types (Legacy/Current)
- Tables: `exam_types`, `student_marks`
- Design: Simple, exam types are section-wide
- Issues: No foreign key validation, allows data inconsistency

### System 2: Marking Schemes (Weightage System)
- Tables: `marking_schemes`, `component_groups`, `marking_components`, `student_component_marks`
- Design: Sophisticated, supports best-of-N, group weightages
- Status: ✅ Working well
- Usage: 45 marks recorded

### System 3: Unused/Half-Implemented
- Tables: `section_exam_types`, `student_marks_detailed`, `subject_exam_types`
- Status: ❌ Either never implemented or abandoned
- Action: Clean up

---

## 📝 NEXT STEPS

1. **Immediate**: Decide on Fix Option A or B for student_marks
2. **Short-term**: Delete unused tables to reduce confusion
3. **Long-term**: Standardize on ONE marks storage system
4. **Critical**: Add validation when entering marks to ensure exam types match section's exam_types

---

## 🚨 ROOT CAUSE OF CURRENT BUG

The bug you experienced where "all subjects showed same exam types" was caused by:

1. Section 24 was created with exam_types: "Mid Term 1", "Mid Term 2", "Assignment", "Quiz", "Final Exam"
2. When marks were entered, users typed: "Midterm", "Final", "Lab", "Project", "Quiz"
3. The JOIN query only matched "Quiz" (exact match)
4. Result: All subjects showed only "Quiz"

**Fix applied:** I manually added the missing exam type names to exam_types table so they match student_marks data.

**Permanent solution needed:** Prevent free-text entry in marks entry screen - force selection from exam_types dropdown.
