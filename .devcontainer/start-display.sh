#!/bin/bash
# Start X11 virtual display for Chromium browser

# Don't exit on error for optional X11 setup
set +e

# Check if display is already running
if [ -n "$DISPLAY" ] && xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
    echo "Display $DISPLAY is already running"
    exit 0
fi

# Create X11 directory if it doesn't exist (requires sudo)
if [ ! -d "/tmp/.X11-unix" ]; then
    echo "Creating /tmp/.X11-unix directory..."
    sudo mkdir -p /tmp/.X11-unix
    sudo chmod 1777 /tmp/.X11-unix
fi

# Start Xvfb on display :99
export DISPLAY=:99
Xvfb :99 -screen 0 1920x1080x24 -ac +extension GLX +render -noreset &
XVFB_PID=$!

# Wait for X server to start
echo "Starting X11 display :99..."
for i in {1..10}; do
    if xdpyinfo -display :99 >/dev/null 2>&1; then
        echo "✓ X11 display :99 started successfully"
        break
    fi
    if [ $i -eq 10 ]; then
        echo "⚠ Warning: Failed to start X11 display (not critical for most development tasks)"
        exit 0  # Don't fail the entire post-create script
    fi
    sleep 1
done

# Start window manager (optional, for GUI apps)
fluxbox -display :99 >/dev/null 2>&1 &

# Optionally start VNC server for remote viewing
x11vnc -display :99 -forever -nopw -quiet -bg -rfbport 5900 >/dev/null 2>&1 || echo "⚠ VNC server not started (optional)"

echo "X11 display ready. Set DISPLAY=:99 to use Chromium."
echo "VNC server available on port 5900 for remote viewing."
