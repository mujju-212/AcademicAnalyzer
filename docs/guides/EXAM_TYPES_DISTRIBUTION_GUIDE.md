# Exam Types Marks Distribution Guide

## Overview
The Academic Analyzer system uses a **Scaled Grading System (Option B)** where exam components can have different maximum marks but contribute specific percentages to the final subject total.

---

## 📋 Core Concepts

### Three Key Values for Each Exam Component:

1. **Max Marks** - The actual marks on the exam paper
   - Example: Internal exam out of 40, Final exam out of 100

2. **Weightage** - Percentage contribution to subject total (always adds to 100%)
   - Example: Internal contributes 10%, Final contributes 70%

3. **Passing Marks** - Minimum marks required to pass the component
   - Example: 18 for internals (45% of 40), 40 for finals (40% of 100)

---

## 🎯 Configuration in Create Section Panel

### Subject Creation Flow:

1. **Add Subject** → Enter subject details
   - Subject Name
   - Total Marks (always 100 for percentage-based)
   - Credit Hours
   - Pass Marks (subject level)

2. **Add Exam Components** → Define marking structure
   - Component Name (e.g., "Internal 1", "Final Exam")
   - Max Marks (paper max - e.g., 40, 100)
   - Weightage (contribution % - e.g., 10, 70)
   - Passing Marks (minimum required - e.g., 18, 40)

### Database Schema:
```sql
TABLE: exam_types
- id (PRIMARY KEY)
- exam_name (VARCHAR) - Component name
- max_marks (INT) - Exam paper maximum
- weightage (INT) - Contribution percentage
- passing_marks (INT) - Pass threshold
- subject_id (FOREIGN KEY)
```

---

## 📊 Real-World Examples

### Example 1: Cloud Computing (Total = 100%)
| Component | Max Marks | Weightage | Passing Marks | Description |
|-----------|-----------|-----------|---------------|-------------|
| Internal 1 | 40 | 10% | 18 | First internal exam |
| Internal 2 | 40 | 10% | 18 | Second internal exam |
| Internal 3 | 40 | 10% | 18 | Third internal exam |
| Final Exam | 100 | 70% | 40 | Final examination |
| **Total** | - | **100%** | - | - |

### Example 2: Gen AI (Total = 100%)
| Component | Max Marks | Weightage | Passing Marks | Description |
|-----------|-----------|-----------|---------------|-------------|
| Internal 1 | 40 | 25% | 18 | First internal exam |
| Internal 2 | 40 | 25% | 18 | Second internal exam |
| Final Exam | 100 | 50% | 40 | Final examination |
| **Total** | - | **100%** | - | - |

### Example 3: Computer Networks (Total = 100%)
| Component | Max Marks | Weightage | Passing Marks | Description |
|-----------|-----------|-----------|---------------|-------------|
| Theory Internal | 100 | 25% | 40 | Theory internal |
| Theory Final | 100 | 25% | 40 | Theory final |
| Lab Internal | 100 | 20% | 40 | Lab internal |
| Lab Final | 100 | 30% | 40 | Lab final |
| **Total** | - | **100%** | - | - |

---

## ✅ Configuration Rules

### 1. Weightage Must Sum to 100%
- ✓ VALID: 10% + 10% + 10% + 70% = 100%
- ✗ INVALID: 10% + 10% + 10% + 60% = 90%
- ✗ INVALID: 15% + 15% + 15% + 75% = 120%

### 2. Max Marks ≠ Weightage
- Max Marks = Exam paper maximum (40, 100, etc.)
- Weightage = Contribution to final % (10, 25, 70, etc.)
- **They are independent values!**

### 3. Passing Marks Logic
- Should be reasonable percentage of max marks
- Typically 40-45% of max marks
- Example: 40 max → 18 passing (45%)
- Example: 100 max → 40 passing (40%)

### 4. Component Flexibility
- Can mix different max marks in same subject
- Example: 40 mark internals + 100 mark final
- System auto-scales all to percentage contribution

---

## 🖥️ UI Components

### Create Section Panel Features:

#### Subject Table Columns:
- **Subject Name** (250px width)
- **Total Marks** (120px) - Always 100 for percentage
- **Credit** (80px) - Credit hours
- **Pass Marks** (120px) - Subject pass threshold
- **Actions** (200px) - Edit/Delete buttons

#### Exam Pattern Table Columns:
- **Component** (250px) - Exam name
- **Max Marks** (120px) - Paper maximum
- **Weightage** (120px) - Contribution %
- **Passing Marks** (120px) - Pass threshold
- **Actions** (200px) - Edit/Delete buttons

### Add/Edit Exam Component Dialog:
```
Component Name: [Text Field]
Max Marks: [Number Field] (1-200)
Weightage: [Number Field] (1-100)
Passing Marks: [Number Field] (0-max_marks)

[Save] [Cancel]
```

---

## 🔧 Backend Processing

### When Creating/Editing Component:
```java
// Validation in CreateSectionPanel
- Verify weightage sum = 100%
- Check max_marks > 0
- Validate passing_marks <= max_marks
- Ensure component name is unique per subject

// Database Insert
INSERT INTO exam_types (exam_name, max_marks, weightage, passing_marks, subject_id)
VALUES ('Internal 1', 40, 10, 18, 28);
```

### When Loading Components:
```java
// Query in MarkEntryDialog.loadExamTypes()
SELECT et.id, et.exam_name, et.max_marks, et.weightage, et.passing_marks
FROM exam_types et
WHERE et.subject_id = ?
ORDER BY et.id;

// Store in ExamTypeInfo class
class ExamTypeInfo {
    int id;
    String name;
    int maxMarks;      // Paper max (e.g., 40)
    int weightage;     // Contribution % (e.g., 10)
    int passingMarks;  // Pass threshold (e.g., 18)
}
```

---

## 📈 Calculation Formula (See MARKS_CALCULATION_GUIDE.md)

For each student, per subject:
```
Subject Total = Σ [(marks_obtained / max_marks) × weightage]
```

Example:
- Internal 1: (38/40) × 10 = 9.5%
- Internal 2: (39/40) × 10 = 9.75%
- Internal 3: (40/40) × 10 = 10%
- Final: (95/100) × 70 = 66.5%
- **Total = 95.75%** (out of 100)

---

## ⚠️ Common Mistakes

### Mistake 1: Setting weightage = max_marks
```
❌ WRONG:
Internal: max_marks=40, weightage=40 (40% too high!)

✓ CORRECT:
Internal: max_marks=40, weightage=10 (10% contribution)
```

### Mistake 2: Forgetting to sum to 100%
```
❌ WRONG:
Int1=10%, Int2=10%, Int3=10%, Final=60% = 90% total

✓ CORRECT:
Int1=10%, Int2=10%, Int3=10%, Final=70% = 100% total
```

### Mistake 3: Passing marks > max marks
```
❌ WRONG:
Max Marks=40, Passing Marks=50 (impossible!)

✓ CORRECT:
Max Marks=40, Passing Marks=18 (45% of max)
```

---

## 🔍 Testing Your Configuration

### Checklist Before Saving:
- [ ] All weightages sum to exactly 100%
- [ ] Max marks are realistic (40, 50, 100, etc.)
- [ ] Passing marks are reasonable (40-45% of max)
- [ ] Component names are clear and unique
- [ ] No duplicate component names in same subject

### Sample Test Case:
```
Subject: Data Structures
Total: 100 (percentage-based)

Components:
1. Quiz 1: max=20, weightage=5%, passing=9
2. Quiz 2: max=20, weightage=5%, passing=9
3. Mid Term: max=50, weightage=20%, passing=20
4. Final: max=100, weightage=70%, passing=40

Verification:
✓ Weightage sum = 5+5+20+70 = 100% ✓
✓ All passing marks < max marks ✓
✓ Realistic mark distributions ✓
```

---

## 📌 Key Takeaways

1. **Three separate values**: max_marks, weightage, passing_marks
2. **Weightage always sums to 100%** for the subject
3. **Max marks = paper marks**, weightage = contribution %
4. **Scaled calculation** handles different max marks automatically
5. **Backward compatible** - works without max_marks column (Option A)
6. **Column widths auto-sized** for better visibility in UI

---

## 📞 Support

For issues with exam configuration:
1. Verify database has `max_marks` column in `exam_types` table
2. Check weightage sum = 100% for all subjects
3. Ensure application is compiled with latest code
4. Review calculation in Mark Entry to verify scaled formula

**Documentation Version:** 2.0 - Scaled Grading System
**Last Updated:** January 11, 2026
