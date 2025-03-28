@echo off

set PORT=8080
set HOST=0.0.0.0

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :done_parsing
if "%~1"=="--port" (
    set PORT=%~2
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

echo Usage: [--port PORT] [--host HOST]
exit /b 1

:done_parsing

if not exist stored_scripts (
    mkdir stored_scripts
    echo Created directory: stored_scripts
)

if not exist logs (
    mkdir logs
    echo Created directory: logs
)

call mvn clean package
java -jar target\mujde-server-1.0-SNAPSHOT.jar --host "%HOST%" --port "%PORT%"
