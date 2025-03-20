@echo off
REM Script to run the Mujde server on Windows

REM Default settings
set PORT=8080
set HOST=0.0.0.0

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :done_parsing
if "%~1"=="-p" (
    set PORT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--port" (
    set PORT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-h" (
    set HOST=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--host" (
    set HOST=%~2
    shift
    shift
    goto :parse_args
)

echo Unknown option: %~1
echo Usage: %0 [-p^|--port PORT] [-h^|--host HOST]
exit /b 1

:done_parsing

REM Check if Maven is installed
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Maven is not installed. Please install Maven first.
    exit /b 1
)

REM Check if the database and scripts directory exist
if not exist stored_scripts (
    mkdir stored_scripts
    echo Created directory: stored_scripts
)

if not exist logs (
    mkdir logs
    echo Created directory: logs
)

REM Run the application
echo Starting Mujde Server on %HOST%:%PORT%...
call mvn clean package -DskipTests
java -jar target\mujde-server-1.0-SNAPSHOT.jar -h "%HOST%" -p "%PORT%"