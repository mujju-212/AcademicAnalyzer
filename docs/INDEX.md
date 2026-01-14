# Documentation Index

Welcome to the Academic Analyzer documentation. This folder contains all user guides, technical documentation, and historical records.

## 📂 Folder Structure

### `/guides/` - User Guides & References
Primary documentation for using and understanding the system.

**Configuration & Usage:**
- [EXAM_TYPES_DISTRIBUTION_GUIDE.md](guides/EXAM_TYPES_DISTRIBUTION_GUIDE.md) - Complete guide for configuring exam types in Create Section Panel
- [MARKS_CALCULATION_GUIDE.md](guides/MARKS_CALCULATION_GUIDE.md) - Detailed explanation of mark calculation, display, and validation
- [DOCUMENTATION_VALIDATION_SUMMARY.md](guides/DOCUMENTATION_VALIDATION_SUMMARY.md) - Feature verification and code references

### `/database/` - Database Documentation
Schema files and migration scripts.

**Current Schema:**
- [schema_current_2026-01-11.sql](database/schema_current_2026-01-11.sql) - Latest database structure (January 11, 2026)

**Migration Scripts:**
- [add_max_marks_column.sql](database/add_max_marks_column.sql) - Adds max_marks column for scaled grading
- [add_passing_marks_column.sql](database/add_passing_marks_column.sql) - Adds passing_marks column for validation

**Test Data:**
- [INSERT_REALISTIC_MARKS.sql](database/INSERT_REALISTIC_MARKS.sql) - 750 realistic mark entries for testing (50 students × 4 subjects)

**Old Schema:**
- [schema_old.sql](database/schema_old.sql) - Previous database structure (archived)

### `/archive/` - Historical Documents
Old summary files, audit reports, and historical documentation. These files are kept for reference but are no longer current.

**Development History:**
- CODE_FIX_SUMMARY.md
- DATABASE_FIX_SUMMARY.md
- DATABASE_CLEANUP_SUMMARY.md
- DATABASE_ANALYSIS.md
- DOCUMENTATION_AUDIT_SUMMARY.md
- EDIT_FUNCTIONALITY_UPDATE.md
- MARKS_ENTRY_COMPLETE_SUMMARY.md
- PROJECT_COMPLETE.md
- SECTION_CREATION_AUDIT.md
- TESTING_CHECKLIST.md
- UI_UPDATE_SUMMARY.md
- WEIGHTED_CALCULATION_SYSTEM.md
- WEIGHTED_IMPLEMENTATION_SUMMARY.md

---

## 🚀 Quick Start

### New Users
1. Read [SETUP.md](SETUP.md) for installation instructions
2. Review [REQUIREMENTS.md](REQUIREMENTS.md) for system requirements
3. Follow [EXAM_TYPES_DISTRIBUTION_GUIDE.md](guides/EXAM_TYPES_DISTRIBUTION_GUIDE.md) to configure subjects
4. Reference [MARKS_CALCULATION_GUIDE.md](guides/MARKS_CALCULATION_GUIDE.md) for mark entry

### Developers
1. Import schema: `mysql -u root -p academic_analyzer < database/schema_current_2026-01-11.sql`
2. (Optional) Load test data: `mysql -u root -p academic_analyzer < database/INSERT_REALISTIC_MARKS.sql`
3. Review [DOCUMENTATION_VALIDATION_SUMMARY.md](guides/DOCUMENTATION_VALIDATION_SUMMARY.md) for code locations
4. Check [archive/](archive/) for development history

---

## 📖 Documentation Guide

### Understanding the Scaled Grading System

The system uses **Option B: Scaled Grading** where:
- **max_marks** = Exam paper maximum (e.g., 40, 100)
- **weightage** = Contribution to subject total % (e.g., 10%, 70%)
- **passing_marks** = Minimum required to pass component (e.g., 18, 40)

**Formula:**
```
Subject Total = Σ [(marks_obtained / max_marks) × weightage]
```

**Example:**
```
Internal 1: (38/40) × 10% = 9.50%
Internal 2: (39/40) × 10% = 9.75%
Internal 3: (40/40) × 10% = 10.00%
Final Exam: (95/100) × 70% = 66.50%
Total: 95.75% (out of 100)
```

**Key Concepts:**
- Subject total is ALWAYS out of 100%
- Different exam types can have different max marks
- Weightage must sum to 100% per subject
- System auto-scales all contributions

---

## 🗄️ Database Schema Overview

### Core Tables

**users** - User authentication and profiles
- id, username, password, email, role, created_at

**sections** - Class/section organization
- id, name, year, semester, student_count, user_id, created_at

**students** - Student records
- id, roll_number, name, email, phone, section_id, created_at

**subjects** - Subject definitions
- id, subject_name, total_marks, credit, pass_marks, created_at

**exam_types** - Exam component configuration
- id, exam_name, **max_marks**, **weightage**, **passing_marks**, subject_id, is_flexible

**student_marks** - Mark entries
- id, student_id, subject_id, exam_type_id, marks_obtained, entered_at

**section_subjects** - Section-subject mapping
- id, section_id, subject_id, created_at

---

## 🎯 Common Tasks

### Configure New Subject
1. Open Create Section Panel
2. Add subject with name, total marks (100), credit, pass marks
3. Add exam components:
   - Component name (e.g., "Internal 1")
   - Max marks (e.g., 40)
   - Weightage (e.g., 10%)
   - Passing marks (e.g., 18)
4. Verify weightage sum = 100%
5. Save configuration

**Reference:** [EXAM_TYPES_DISTRIBUTION_GUIDE.md](guides/EXAM_TYPES_DISTRIBUTION_GUIDE.md)

### Enter Marks
1. Open Mark Entry dialog
2. Select section and subject
3. Enter marks for each component (out of max_marks)
4. System auto-calculates scaled total
5. Status shows "Complete" when all components filled

**Reference:** [MARKS_CALCULATION_GUIDE.md](guides/MARKS_CALCULATION_GUIDE.md)

### Export/Import Marks
**Export:**
- Select format (Excel/PDF)
- Headers include max marks: "Internal 1 (40)"
- All data preserved with proper formatting

**Import:**
- Use exported Excel as template
- Headers must match exam type names
- System validates roll numbers and marks

**Reference:** [MARKS_CALCULATION_GUIDE.md](guides/MARKS_CALCULATION_GUIDE.md) - Import/Export section

---

## 🔍 Finding Information

### Configuration Questions
→ [EXAM_TYPES_DISTRIBUTION_GUIDE.md](guides/EXAM_TYPES_DISTRIBUTION_GUIDE.md)

### Calculation Questions
→ [MARKS_CALCULATION_GUIDE.md](guides/MARKS_CALCULATION_GUIDE.md)

### Code Location Questions
→ [DOCUMENTATION_VALIDATION_SUMMARY.md](guides/DOCUMENTATION_VALIDATION_SUMMARY.md)

### Database Schema Questions
→ [database/schema_current_2026-01-11.sql](database/schema_current_2026-01-11.sql)

### Historical Context
→ [archive/](archive/)

---

## 📊 System Features

### Mark Entry Dialog
- ✅ Scaled calculation: (marks/max_marks) × weightage
- ✅ Color coding per component (red/white/green)
- ✅ Incomplete validation: Shows "Incomplete (x/y)"
- ✅ Auto-save on cell change
- ✅ Auto-sized columns
- ✅ Export to Excel/PDF
- ✅ Import from Excel

### Create Section Panel
- ✅ Subject configuration
- ✅ Exam component setup
- ✅ Weightage validation (sum=100%)
- ✅ Max marks and passing marks configuration
- ✅ Edit/Delete actions
- ✅ Organized table display

### Dashboard
- ✅ Performance analytics
- ✅ Section statistics
- ✅ Student progress tracking
- ✅ Grade distribution charts

---

## 🛠️ Maintenance

### Updating Documentation
When updating docs:
1. Update relevant guide in `/guides/`
2. Update this INDEX.md if structure changes
3. Archive old versions to `/archive/` if major changes
4. Update version date at bottom of document

### Database Changes
When updating schema:
1. Create migration script in `/database/`
2. Update `schema_current_YYYY-MM-DD.sql`
3. Archive old schema as `schema_old_YYYY-MM-DD.sql`
4. Update this INDEX.md with new file names

---

## 📞 Support Resources

**Configuration Issues:**
- Check [EXAM_TYPES_DISTRIBUTION_GUIDE.md](guides/EXAM_TYPES_DISTRIBUTION_GUIDE.md) - Common mistakes section
- Verify weightage sum = 100%
- Ensure max_marks column exists in database

**Calculation Issues:**
- Review [MARKS_CALCULATION_GUIDE.md](guides/MARKS_CALCULATION_GUIDE.md) - Common issues section
- Verify ExamTypeInfo loads all 5 fields
- Check scaled formula implementation

**Database Issues:**
- Import latest schema: `database/schema_current_2026-01-11.sql`
- Check migration scripts in `database/` folder
- Verify .env configuration

---

## 📝 Version History

### Version 2.0 (January 11, 2026)
- Scaled grading system implemented
- Incomplete entry validation added
- Component-specific color coding
- Enhanced import/export functionality
- Comprehensive documentation created
- Documentation reorganized into structured folders

### Version 1.0 (Previous)
- Basic mark entry system
- Simple calculation (sum)
- Basic import/export
- Archived documentation in `/archive/`

---

**Documentation Version:** 2.0  
**Last Updated:** January 11, 2026  
**Status:** Current and Complete ✅
