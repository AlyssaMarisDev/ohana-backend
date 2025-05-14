#!/bin/bash

PORT=4240
SERVER_LOG="ktor-server.log"
SERVER_PID=""

# Function to clean up the server
cleanup() {
  if [[ -n "$SERVER_PID" ]] && ps -p $SERVER_PID > /dev/null; then
    echo "Cleaning up server with PID $SERVER_PID..."
    kill $SERVER_PID
    wait $SERVER_PID 2>/dev/null
  fi
}

# Trap any exit, INT (Ctrl+C), or TERM signal and run cleanup
trap cleanup EXIT INT TERM

# Start the Ktor server in the background
echo "Starting Ktor server on port $PORT..."
nohup env PORT=$PORT ./gradlew run > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!
echo "Server started with PID $SERVER_PID"

# Wait for server to be ready with a timeout
echo "Waiting for server to be ready on port $PORT..."
timeout=10
elapsed=0
while ! netstat -an | grep -q ":$PORT"; do
  sleep 1
  elapsed=$((elapsed + 1))
  if [ $elapsed -ge $timeout ]; then
    echo "Server did not start within $timeout seconds. Please check the logs at $SERVER_LOG."
    exit 1
  fi
done
echo "Server is up!"

# Run tests
echo "Running tests..."
./gradlew test
TEST_EXIT_CODE=$?

# Cleanup will happen automatically because of the trap
exit $TEST_EXIT_CODE