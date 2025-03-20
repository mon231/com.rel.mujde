#!/bin/bash

# Script to run the Mujde server on Linux

# Default settings
PORT=8080
HOST="0.0.0.0"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    key="$1"

    case $key in
        -p|--port)
        PORT="$2"
        shift
        shift
        ;;
        -h|--host)
        HOST="$2"
        shift
        shift
        ;;
        *)
        echo "Unknown option: $key"
        echo "Usage: $0 [-p|--port PORT] [-h|--host HOST]"
        exit 1
        ;;
    esac
done

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if the database and scripts directory exist
if [ ! -d "stored_scripts" ]; then
    mkdir -p stored_scripts
    echo "Created directory: stored_scripts"
fi

if [ ! -d "logs" ]; then
    mkdir -p logs
    echo "Created directory: logs"
fi

# Run the application
echo "Starting Mujde Server on $HOST:$PORT..."
mvn clean package -DskipTests
java -jar target/mujde-server-1.0-SNAPSHOT.jar -h "$HOST" -p "$PORT"