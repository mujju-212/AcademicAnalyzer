# Academic Analyzer - Build Windows Installer Script
# Creates a professional .msi installer using jpackage

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Academic Analyzer - Build Installer" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Step 1: Clean previous builds
Write-Host "[1/5] Cleaning previous builds..." -ForegroundColor Yellow
Remove-Item -Recurse -Force bin/* -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
Remove-Item *.msi -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path dist, dist/lib | Out-Null

# Step 2: Compile Java sources
Write-Host "[2/5] Compiling Java sources..." -ForegroundColor Yellow
javac -encoding UTF-8 -d bin -cp "lib/*" -sourcepath src src/Main.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nCompilation failed!" -ForegroundColor Red
    exit 1
}
Write-Host "    Compilation successful" -ForegroundColor Green

# Step 3: Prepare application bundle
Write-Host "[3/5] Preparing application bundle..." -ForegroundColor Yellow
# Copy compiled classes
jar cf dist/AcademicAnalyzer.jar -C bin .
# Copy libraries
Copy-Item -Path "lib/*.jar" -Destination "dist/lib/" -Force
# Copy resources
Copy-Item -Recurse -Path "resources" -Destination "bin/resources" -Force
Copy-Item -Path ".env" -Destination "bin/.env" -Force -ErrorAction SilentlyContinue
# Repackage with resources
jar cf dist/AcademicAnalyzer.jar -C bin .

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nJAR creation failed!" -ForegroundColor Red
    exit 1
}
Write-Host "    Application bundle ready" -ForegroundColor Green

# Step 4: Verify icon file
Write-Host "[4/5] Verifying application icon..." -ForegroundColor Yellow
$iconPath = "resources/AA LOGO.ico"
if (!(Test-Path $iconPath)) {
    Write-Host "    Warning: Icon file not found at $iconPath" -ForegroundColor Yellow
    $iconPath = $null
} else {
    Write-Host "    Using icon: $iconPath" -ForegroundColor Green
}

# Step 5: Create Windows installer with jpackage
Write-Host "[5/5] Creating Windows installer..." -ForegroundColor Yellow

if ($iconPath) {
    jpackage `
        --input dist `
        --name "AcademicAnalyzer" `
        --main-jar AcademicAnalyzer.jar `
        --main-class Main `
        --type exe `
        --icon $iconPath `
        --app-version "1.0.0" `
        --vendor "Academic Solutions" `
        --description "Academic Performance Management System" `
        --win-dir-chooser `
        --win-menu `
        --win-shortcut `
        --win-menu-group "Academic Analyzer"
} else {
    jpackage `
        --input dist `
        --name "AcademicAnalyzer" `
        --main-jar AcademicAnalyzer.jar `
        --main-class Main `
        --type exe `
        --app-version "1.0.0" `
        --vendor "Academic Solutions" `
        --description "Academic Performance Management System" `
        --win-dir-chooser `
        --win-menu `
        --win-shortcut `
        --win-menu-group "Academic Analyzer"
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    
    Write-Host "`nInstaller created:" -ForegroundColor Cyan
    Get-ChildItem *.exe, *.msi | ForEach-Object {
        $sizeMB = [math]::Round($_.Length / 1MB, 2)
        Write-Host "  Package: $($_.Name) - Size: $sizeMB MB" -ForegroundColor White
    }
    
    Write-Host "`nUsers can now:" -ForegroundColor Cyan
    Write-Host "  - Double-click the installer to install" -ForegroundColor White
    Write-Host "  - Find it in Start Menu > Academic Analyzer" -ForegroundColor White
    Write-Host "  - Run without Java installed (JRE bundled)" -ForegroundColor White
    Write-Host "  - Desktop shortcut created automatically" -ForegroundColor White
    
    Write-Host "`nReady for distribution!`n" -ForegroundColor Green
} else {
    Write-Host "`nInstaller creation failed!" -ForegroundColor Red
    Write-Host "Make sure you have JDK 14+ with jpackage tool" -ForegroundColor Yellow
    exit 1
}
