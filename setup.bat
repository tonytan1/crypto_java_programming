@echo off
echo ========================================
echo Portfolio Application Setup
echo ========================================
echo.

echo Checking Java installation...
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17
    pause
    exit /b 1
)
echo.

echo Building the application...
call gradlew.bat build -x test
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)
echo.

echo Build successful! 
echo.
echo To run the application:
echo   1. Run: run.bat
echo   2. Or run: gradlew.bat run
echo.
echo The application will:
echo   - Load positions from sample-positions.csv
echo   - Initialize the H2 database with security definitions
echo   - Start real-time market data simulation
echo   - Display portfolio values every 5 seconds
echo.
pause
