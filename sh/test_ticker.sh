#!/bin/bash

# Base URL for the API
BASE_URL="http://localhost:8080/api"

# Function to make GET requests and print response
call_endpoint() {
    echo "Calling $1..."
    curl -s "$BASE_URL$1" | jq '.'
    echo -e "\n"
}

# Function to make GET requests with parameters
call_endpoint_with_params() {
    echo "Calling $1 with params $2..."
    curl -s "$BASE_URL$1?$2" | jq '.'
    echo -e "\n"
}

# Test basic ticker operations
echo "=== Testing Basic Ticker Operations ==="
call_endpoint "ticker/next"
sleep 5
call_endpoint "/ticker/info"
call_endpoint_with_params "/ticker/load" "tickerId=1"
call_endpoint "/ticker/start"
sleep 2

# Test ticker status stream
echo "=== Testing Ticker Status Stream ==="
curl -N "$BASE_URL/tick" &
TICK_PID=$!
sleep 5
kill $TICK_PID

# Test song status stream
echo "=== Testing Song Status Stream ==="
curl -N "$BASE_URL/xox" &
XOX_PID=$!
sleep 5
kill $XOX_PID

# Test ticker controls
echo "=== Testing Ticker Controls ==="
call_endpoint "/ticker/pause"
sleep 1
call_endpoint "/ticker/start"
sleep 1
call_endpoint "/ticker/stop"

# Test ticker navigation
echo "=== Testing Ticker Navigation ==="
call_endpoint_with_params "/ticker/next" "currentTickerId=1"
call_endpoint_with_params "/ticker/prev" "currentTickerId=2"

# Test ticker updates
echo "=== Testing Ticker Updates ==="
# Update BPM to 140
call_endpoint_with_params "/ticker/update" "tickerId=1&updateType=0&updateValue=140"
sleep 1
# Update PPQ to 48
call_endpoint_with_params "/ticker/update" "tickerId=1&updateType=1&updateValue=48"

# Test ticker logging
echo "=== Testing Ticker Logging ==="
call_endpoint_with_params "/ticker/log" "requestType=status"
call_endpoint_with_params "/ticker/log" "requestType=info"

# Final status check
echo "=== Final Status Check ==="
call_endpoint "/ticker/status"

echo "Test complete!"
