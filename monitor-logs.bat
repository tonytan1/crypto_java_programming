@echo off
echo ========================================
echo Portfolio Application Log Monitor
echo ========================================
echo.

echo Creating logs directory...
if not exist logs mkdir logs

echo.
echo Choose what to monitor:
echo 1. Run application and see console output
echo 2. View application logs (portfolio-application.log)
echo 3. View portfolio summary logs (portfolio-summary.log)
echo 4. View all logs in real-time
echo.

set /p choice="Enter your choice (1-4): "

if "%choice%"=="1" (
    echo.
    echo Starting application...
    call gradlew.bat run
) else if "%choice%"=="2" (
    echo.
    echo Opening application logs...
    if exist logs\portfolio-application.log (
        type logs\portfolio-application.log
    ) else (
        echo No application logs found yet. Run the application first.
    )
) else if "%choice%"=="3" (
    echo.
    echo Opening portfolio summary logs...
    if exist logs\portfolio-summary.log (
        type logs\portfolio-summary.log
    ) else (
        echo No portfolio summary logs found yet. Run the application first.
    )
) else if "%choice%"=="4" (
    echo.
    echo Monitoring all logs in real-time (Press Ctrl+C to stop)...
    if exist logs\portfolio-application.log (
        powershell Get-Content logs\portfolio-application.log -Wait -Tail 10
    ) else (
        echo No logs found yet. Run the application first.
    )
) else (
    echo Invalid choice. Please run the script again.
)

echo.
pause
