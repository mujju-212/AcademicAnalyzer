# Academic Analyzer - Simple Distribution Package
# Creates a portable distribution without needing WiX Toolset

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Academic Analyzer - Build Package" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Step 1: Clean previous builds
Write-Host "[1/4] Cleaning previous builds..." -ForegroundColor Yellow
Remove-Item -Recurse -Force bin/* -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force AcademicAnalyzer-Portable -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path AcademicAnalyzer-Portable, AcademicAnalyzer-Portable/lib | Out-Null

# Step 2: Compile Java sources
Write-Host "[2/4] Compiling Java sources..." -ForegroundColor Yellow
javac -encoding UTF-8 -d bin -cp "lib/*" -sourcepath src src/Main.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nCompilation failed!" -ForegroundColor Red
    exit 1
}
Write-Host "    Compilation successful" -ForegroundColor Green

# Step 3: Prepare portable package
Write-Host "[3/4] Creating portable package..." -ForegroundColor Yellow

# Copy resources
Copy-Item -Recurse -Path "resources" -Destination "bin/resources" -Force
Copy-Item -Path ".env" -Destination "bin/.env" -Force -ErrorAction SilentlyContinue

# Create JAR with all resources
jar cf AcademicAnalyzer-Portable/AcademicAnalyzer.jar -C bin .

# Copy libraries
Copy-Item -Path "lib/*.jar" -Destination "AcademicAnalyzer-Portable/lib/" -Force

# Copy resources folder for external access
Copy-Item -Recurse -Path "resources" -Destination "AcademicAnalyzer-Portable/resources" -Force
Copy-Item -Path ".env" -Destination "AcademicAnalyzer-Portable/.env" -Force -ErrorAction SilentlyContinue

Write-Host "    Portable package created" -ForegroundColor Green

# Step 4: Create launcher scripts
Write-Host "[4/4] Creating launcher scripts..." -ForegroundColor Yellow

# Windows batch launcher
$batchContent = @'
@echo off
title Academic Analyzer
echo Starting Academic Analyzer...
java -cp "AcademicAnalyzer.jar;lib/*" Main
if errorlevel 1 (
    echo.
    echo ERROR: Failed to start Academic Analyzer
    echo Make sure Java 11+ is installed
    echo.
    pause
)
'@
$batchContent | Out-File -FilePath "AcademicAnalyzer-Portable/AcademicAnalyzer.bat" -Encoding ASCII

# PowerShell launcher (more robust)
$psContent = @'
Write-Host "Starting Academic Analyzer..." -ForegroundColor Cyan
try {
    java -cp "AcademicAnalyzer.jar;lib/*" Main
} catch {
    Write-Host "`nERROR: Failed to start Academic Analyzer" -ForegroundColor Red
    Write-Host "Make sure Java 11+ is installed" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
}
'@
$psContent | Out-File -FilePath "AcademicAnalyzer-Portable/AcademicAnalyzer.ps1" -Encoding UTF8

# README file
$readmeContent = @'
# Academic Analyzer - Portable Edition

## Requirements
- Java 11 or higher installed on your system
- MySQL database server running

## How to Run

### Option 1: Double-click
Simply double-click `AcademicAnalyzer.bat` to start the application

### Option 2: Command Line
Open Command Prompt in this folder and run:
```
AcademicAnalyzer.bat
```

### Option 3: PowerShell
Right-click `AcademicAnalyzer.ps1` and select "Run with PowerShell"

## Database Configuration
Edit the `.env` file to configure your database connection:
- DB_HOST=localhost
- DB_PORT=3306
- DB_NAME=your_database
- DB_USER=your_username
- DB_PASSWORD=your_password

## Troubleshooting

### "Java is not recognized"
Install Java 11+ from https://adoptium.net/ and add it to PATH

### Database connection failed
1. Make sure MySQL is running
2. Verify credentials in `.env` file
3. Check if database exists

## Distribution
This entire folder can be copied to any Windows computer with Java installed.

## Support
For issues, contact the Academic Analyzer development team.
'@
$readmeContent | Out-File -FilePath "AcademicAnalyzer-Portable/README.txt" -Encoding UTF8

Write-Host "    Launcher scripts created" -ForegroundColor Green

# Success message
Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host "`nPortable package created:" -ForegroundColor Cyan
Write-Host "  Folder: AcademicAnalyzer-Portable/" -ForegroundColor White

$totalSize = (Get-ChildItem -Path "AcademicAnalyzer-Portable" -Recurse | Measure-Object -Property Length -Sum).Sum
$sizeMB = [math]::Round($totalSize / 1MB, 2)
Write-Host "  Size: $sizeMB MB" -ForegroundColor White

Write-Host "`nContents:" -ForegroundColor Cyan
Write-Host "  - AcademicAnalyzer.bat (Windows launcher)" -ForegroundColor White
Write-Host "  - AcademicAnalyzer.ps1 (PowerShell launcher)" -ForegroundColor White
Write-Host "  - AcademicAnalyzer.jar (Application)" -ForegroundColor White
Write-Host "  - lib/ (Dependencies)" -ForegroundColor White
Write-Host "  - resources/ (Images, icons)" -ForegroundColor White
Write-Host "  - .env (Database configuration)" -ForegroundColor White
Write-Host "  - README.txt (Instructions)" -ForegroundColor White

Write-Host "`nTo distribute:" -ForegroundColor Cyan
Write-Host "  1. Zip the 'AcademicAnalyzer-Portable' folder" -ForegroundColor White
Write-Host "  2. Share the zip file" -ForegroundColor White
Write-Host "  3. Users extract and double-click AcademicAnalyzer.bat" -ForegroundColor White

Write-Host "`nNote: Users need Java 11+ installed" -ForegroundColor Yellow
Write-Host "`nReady for distribution!`n" -ForegroundColor Green
