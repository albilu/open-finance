#!/bin/bash
# Post-start script: Run every time the container starts

set -e

echo "=== Container started ==="

# Ensure X11 display is running
if ! xdpyinfo -display :99 >/dev/null 2>&1; then
    echo "Starting X11 display..."
    start-display.sh
fi

# Display current git branch
if [ -d ".git" ]; then
    BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")
    echo "Git branch: $BRANCH"
fi

# Check if backend is running
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "Backend server is running on port 8080"
fi

# Check if frontend is running
if lsof -Pi :5173 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "Frontend dev server is running on port 5173"
fi

echo "=== Ready for development ==="
