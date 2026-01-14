# Academic Analyzer

A comprehensive Student Management System for academic institutions.

## 📁 Project Structure

```
AcademicAnalyzer/
├── src/                    # Source code
├── lib/                    # External libraries
├── bin/                    # Compiled classes
├── docs/                   # Documentation
│   ├── guides/            # User guides
│   ├── database/          # Database schemas
│   └── archive/           # Historical documents
├── .env                    # Configuration
└── sources_win.txt         # Build file list
```

## 📚 Documentation

### Quick Start
- [Setup Guide](docs/SETUP.md) - Installation and configuration
- [Requirements](docs/REQUIREMENTS.md) - System requirements

### User Guides
- [Exam Types Distribution Guide](docs/guides/EXAM_TYPES_DISTRIBUTION_GUIDE.md) - Configure exam components
- [Marks Calculation Guide](docs/guides/MARKS_CALCULATION_GUIDE.md) - Understanding calculations
- [Documentation Validation](docs/guides/DOCUMENTATION_VALIDATION_SUMMARY.md) - Feature verification

### Database
- [Current Schema](docs/database/schema_current_2026-01-11.sql) - Latest database structure
- [Migration Scripts](docs/database/) - Database update scripts

## 🚀 Features

- **Student Management** - Comprehensive student records
- **Section Management** - Organize students by sections
- **Marks Entry** - Scaled grading system with validation
- **Result Calculation** - Automated weighted calculations
- **Report Generation** - Export to Excel/PDF
- **Dashboard Analytics** - Performance insights

## 🔧 Technology Stack

- **Language:** Java
- **Database:** MySQL
- **UI Framework:** Java Swing with FlatLaf
- **Build:** Manual compilation (javac)
- **Libraries:** Apache POI, iText, MySQL Connector

## 📊 Grading System

### Scaled Grading (Option B)
```
Subject Total = Σ [(marks_obtained / max_marks) × weightage]
```

**Example: Cloud Computing**
- Internal 1: 38/40 × 10% = 9.50%
- Internal 2: 39/40 × 10% = 9.75%
- Internal 3: 40/40 × 10% = 10.00%
- Final: 95/100 × 70% = 66.50%
- **Total: 95.75%**

## 🛠️ Build & Run

### Compile
```powershell
javac -encoding UTF-8 -d bin -cp "lib/*" $(Get-Content sources_win.txt)
```

### Run
```powershell
java -cp "bin;lib/*" Main
```

## 📝 Configuration

Create `.env` file (copy from `.env.example`):
```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=academic_analyzer
DB_USER=root
DB_PASSWORD=your_password
AUTO_LOGIN_ENABLED=true
AUTO_LOGIN_USER_ID=1
```

## 🗄️ Database Setup

1. Create database:
```sql
CREATE DATABASE academic_analyzer;
```

2. Import schema:
```powershell
mysql -u root -p academic_analyzer < docs/database/schema_current_2026-01-11.sql
```

3. (Optional) Import test data:
```powershell
mysql -u root -p academic_analyzer < docs/database/INSERT_REALISTIC_MARKS.sql
```

## 📈 Recent Updates (January 2026)

- ✅ Scaled grading system (max_marks ≠ weightage)
- ✅ Incomplete entry validation
- ✅ Component-specific color coding
- ✅ Auto-sized column widths
- ✅ Enhanced import/export with proper headers
- ✅ Comprehensive documentation

## 🐛 Known Issues

None currently. See [archive](docs/archive/) for historical issues.

## 📞 Support

For issues or questions, refer to:
- [Exam Types Guide](docs/guides/EXAM_TYPES_DISTRIBUTION_GUIDE.md) - Configuration help
- [Calculation Guide](docs/guides/MARKS_CALCULATION_GUIDE.md) - Calculation troubleshooting

## 📜 License

See [LICENSE](LICENSE) file for details.

---

**Version:** 2.0 - Scaled Grading System  
**Last Updated:** January 11, 2026  
**Status:** Production Ready ✅
