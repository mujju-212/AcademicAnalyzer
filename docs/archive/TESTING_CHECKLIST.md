# TESTING CHECKLIST - Database Migration Verification
**Date:** January 10, 2026

---

## ✅ COMPLETED - Database Cleanup

- [x] Dropped 6 unused tables
- [x] Added exam_type_id FK column to student_marks
- [x] Migrated all 334 records to use FK
- [x] Added FK constraint to enforce integrity
- [x] Populated subject_exam_types with 23 records
- [x] Updated 3 Java files to use new FK structure
- [x] Compilation successful with no errors
- [x] Application starts without SQL errors

---

## 🧪 MANUAL TESTING REQUIRED

### Priority 1: Critical Functionality

#### Test 1.1: Edit Section - View Exam Types ⏳
**File:** CreateSectionPanel.java  
**Steps:**
1. Open application
2. Click on any section's "Edit" button
3. Go to "Exam Patterns" tab
4. Switch between different subjects in dropdown

**Expected Results:**
- ✅ Different subjects show DIFFERENT exam types
- ✅ Mathematics shows: Quiz, Midterm, Final (3 types)
- ✅ Physics shows: Quiz, Midterm, Final, Lab (4 types)
- ✅ Chemistry shows: Midterm, Final, Lab (3 types)
- ✅ Computer Science shows: Quiz, Midterm, Final, Project (4 types)
- ✅ No SQL errors in console

**Status:** 🔲 NOT TESTED YET

---

#### Test 1.2: Mark Entry - Load Existing Marks ⏳
**File:** MarkEntryDialog.java  
**Steps:**
1. Go to Mark Entry screen
2. Select section: "B (ise )"
3. Select subject: "Mathematics"
4. Click "Load Students" button

**Expected Results:**
- ✅ Grid displays with exam type columns
- ✅ Column headers show exam types from exam_types table
- ✅ Existing marks load into the grid
- ✅ No SQL errors in console

**Status:** 🔲 NOT TESTED YET

---

#### Test 1.3: Mark Entry - Save New Marks ⏳
**File:** MarkEntryDialog.java  
**Steps:**
1. After loading students (Test 1.2)
2. Enter marks in any cell (e.g., "85")
3. Tab or click away from cell
4. Check status bar for "Auto-saved" message

**Expected Results:**
- ✅ Status shows "✓ Auto-saved at HH:mm:ss" in green
- ✅ No errors in console
- ✅ Verify in database:
  ```sql
  SELECT * FROM student_marks WHERE student_id = [id] AND subject_id = [id];
  ```
- ✅ Record has exam_type_id (not NULL)

**Status:** 🔲 NOT TESTED YET

---

#### Test 1.4: Mark Entry - Delete Marks ⏳
**File:** MarkEntryDialog.java  
**Steps:**
1. After saving marks (Test 1.3)
2. Clear the cell (delete the value)
3. Tab or click away

**Expected Results:**
- ✅ Mark is removed from database
- ✅ Status shows auto-saved
- ✅ Cell shows empty
- ✅ Verify in database: Record deleted

**Status:** 🔲 NOT TESTED YET

---

### Priority 2: Dashboard Integration

#### Test 2.1: Dashboard Statistics ⏳
**File:** DashboardDataManager.java  
**Steps:**
1. Go to Dashboard home screen
2. Observe statistics panel

**Expected Results:**
- ✅ Statistics load without errors
- ✅ Shows correct student count
- ✅ Shows average scores
- ✅ No SQL errors about "exam_type" column

**Status:** 🔲 NOT TESTED YET

---

#### Test 2.2: Dashboard Section Details ⏳
**File:** DashboardDataManager.java  
**Steps:**
1. From Dashboard, select a section from dropdown
2. View student details panel

**Expected Results:**
- ✅ Student marks display correctly
- ✅ Exam type names appear (not NULL or empty)
- ✅ Subjects with marks visible
- ✅ No SQL errors

**Status:** 🔲 NOT TESTED YET

---

### Priority 3: Create New Data

#### Test 3.1: Create New Section ⏳
**File:** CreateSectionPanel.java  
**Steps:**
1. Click "Create Section" button
2. Enter section name: "Test Section"
3. Add 2-3 subjects
4. Go to Exam Patterns tab
5. Select a subject
6. Use template or add components manually
7. Save section

**Expected Results:**
- ✅ Section saves successfully
- ✅ Verify in database:
  ```sql
  SELECT * FROM exam_types WHERE section_id = [new_section_id];
  ```
- ✅ Exam types created with proper IDs
- ✅ No duplicate entries

**Status:** 🔲 NOT TESTED YET

---

#### Test 3.2: Enter Marks for New Section ⏳
**Files:** CreateSectionPanel.java + MarkEntryDialog.java  
**Steps:**
1. After creating section (Test 3.1)
2. Add students to the section
3. Go to Mark Entry
4. Select the new section
5. Load students and enter marks

**Expected Results:**
- ✅ Exam types load from exam_types table
- ✅ Marks save with exam_type_id FK
- ✅ Verify: All new records have exam_type_id NOT NULL

**Status:** 🔲 NOT TESTED YET

---

### Priority 4: Edge Cases

#### Test 4.1: Subject Without Marks ⏳
**File:** CreateSectionPanel.java  
**Steps:**
1. Edit section "A" (ID 24)
2. Go to Exam Patterns tab
3. Select subject "maths" or "evs" (subjects with no marks)

**Expected Results:**
- ✅ Shows empty table (no components)
- ✅ Message or empty state displayed
- ✅ No errors in console
- ✅ Can still add new components via template

**Status:** 🔲 NOT TESTED YET

---

#### Test 4.2: Reload After Save ⏳
**File:** MarkEntryDialog.java  
**Steps:**
1. Enter marks and save
2. Close Mark Entry dialog
3. Re-open Mark Entry
4. Select same section/subject
5. Load students again

**Expected Results:**
- ✅ Previously saved marks appear
- ✅ All columns load correctly
- ✅ No data loss
- ✅ No duplicate entries

**Status:** 🔲 NOT TESTED YET

---

## 🐛 KNOWN ISSUES TO CHECK

### Other Files That May Need Updates:
These files still have references to old `exam_type` column:

1. **SectionAnalyzer.java** (lines 1228, 1234)
   - [ ] Check if actively used
   - [ ] Update if needed

2. **AnalyzerDAO.java** (multiple lines)
   - [ ] Check if actively used for reports/analysis
   - [ ] Update queries to use exam_type_id

**Priority:** Medium (only if features are used)

---

## 📝 TEST RESULTS LOG

### Test Session 1: [Date/Time]
Tester: ________________

| Test ID | Status | Notes |
|---------|--------|-------|
| 1.1 | ⏳ | |
| 1.2 | ⏳ | |
| 1.3 | ⏳ | |
| 1.4 | ⏳ | |
| 2.1 | ⏳ | |
| 2.2 | ⏳ | |
| 3.1 | ⏳ | |
| 3.2 | ⏳ | |
| 4.1 | ⏳ | |
| 4.2 | ⏳ | |

**Legend:**
- ⏳ Pending
- ✅ Passed
- ❌ Failed
- ⚠️ Issues Found

---

## 🔧 IF TESTS FAIL

### Rollback Plan:
1. Stop application
2. Restore database backup:
   ```powershell
   mysql -u root -p academic_analyzer < backup_before_cleanup_[timestamp].sql
   # You will be prompted for password
   ```
3. Revert code changes from git (if tracked)

**Note:** Database credentials are stored in `CREDENTIALS_REFERENCE.md` (not committed to version control)

### Debug Steps:
1. Check console for SQL errors
2. Verify column exists:
   ```sql
   DESC student_marks;
   ```
3. Check FK constraint:
   ```sql
   SHOW CREATE TABLE student_marks;
   ```
4. Verify exam_type_id values:
   ```sql
   SELECT exam_type_id, COUNT(*) FROM student_marks GROUP BY exam_type_id;
   ```

---

## ✅ SIGN-OFF

**Developer:** ________________ Date: __________

**Tester:** ________________ Date: __________

**Status:** 
- [ ] All tests passed
- [ ] Minor issues (documented above)
- [ ] Major issues (rollback required)

**Notes:**
_____________________________________________
_____________________________________________
_____________________________________________
