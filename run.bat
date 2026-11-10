@echo off
echo =================================================================
echo   Compiling and Starting Bill Splitter (Java REST API + Web UI)
echo =================================================================

if not exist "bin" mkdir bin

echo Compiling Java backend...
javac -d bin src\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please ensure JDK is installed and in PATH.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Starting Server on http://localhost:8080 ...
start http://localhost:8080
java -cp bin Server
pause

