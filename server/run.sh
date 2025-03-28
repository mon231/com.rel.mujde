#!/bin/bash

PORT=8080
HOST="0.0.0.0"

while [[ $# -gt 0 ]]; do
    key="$1"

    case $key in
        --port)
        PORT="$2"
        shift
        shift
        ;;
        --host)
        HOST="$2"
        shift
        shift
        ;;
        *)
        echo "Usage: [--port PORT] [--host HOST]"
        exit 1
        ;;
    esac
done

if [ ! -d "stored_scripts" ]; then
    mkdir -p stored_scripts
    echo "Created directory: stored_scripts"
fi

if [ ! -d "logs" ]; then
    mkdir -p logs
    echo "Created directory: logs"
fi

mvn clean package -DskipTests
java -jar target/mujde-server-1.0-SNAPSHOT.jar --host "$HOST" --port "$PORT"
