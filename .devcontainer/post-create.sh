#!/bin/bash
# Post-create script: Run once after container is created

# Don't exit on error - make setup resilient
set +e

echo "=== Running post-create setup ==="

# Fix permissions for directories that might be created by features as root
sudo chown -R vscode:vscode /home/vscode/.vscode-server 2>/dev/null || true
sudo chown -R vscode:vscode /home/vscode/.m2 2>/dev/null || true
sudo chown -R vscode:vscode /home/vscode/.npm 2>/dev/null || true

# Navigate to workspace
cd /workspace

# Start X11 display (optional, for GUI apps)
echo "Starting X11 display (optional)..."
start-display.sh || echo "⚠ X11 not started (not needed for typical development)"

# Exit on error from here for critical setup
set -e

# Install OpenCode globally
echo "Installing OpenCode CLI..."
if command -v npm &> /dev/null; then
    npm install -g opencode-ai
    echo "✓ OpenCode CLI installed (run 'opencode' to start)"
else
    echo "⚠ npm not available, skipping OpenCode installation"
fi

# Install backend dependencies
echo "Installing backend dependencies..."
if [ -f "pom.xml" ]; then
    mvn dependency:resolve dependency:resolve-plugins -q
    echo "✓ Backend dependencies installed"
fi

# Install frontend dependencies
echo "Installing frontend dependencies..."
if [ -d "openfinance-ui" ] && [ -f "openfinance-ui/package.json" ]; then
    cd openfinance-ui
    npm install
    echo "✓ Frontend dependencies installed"
    cd ..
fi

# Create necessary directories
echo "Creating project directories..."
mkdir -p data logs target

# Set up Git hooks (if Husky is configured)
if [ -d ".git" ] && [ -f "openfinance-ui/package.json" ]; then
    cd openfinance-ui
    if command -v husky &> /dev/null; then
        npm run prepare 2>/dev/null || echo "Husky not configured"
    fi
    cd ..
fi

# Display environment info
echo ""
echo "=== Development Environment Ready ==="
echo "Java version:"
java -version 2>&1 | head -n 1
echo ""
echo "Maven version:"
mvn -version | head -n 1
echo ""
echo "Node.js version:"
node --version
echo ""
echo "npm version:"
npm --version
echo ""
echo "Chromium version:"
chromium-browser --version 2>/dev/null || echo "Not available (optional - only needed for manual browser testing)"
echo ""
echo "X11 Display: $DISPLAY"
echo ""
echo "=== Quick Start Commands ==="
echo "Backend:"
echo "  mvn clean install     - Build backend"
echo "  mvn spring-boot:run   - Run backend server"
echo "  mvn test              - Run backend tests"
echo ""
echo "Frontend:"
echo "  cd openfinance-ui"
echo "  npm run dev           - Start dev server"
echo "  npm test              - Run tests"
echo "  npm run build         - Build for production"
echo ""
echo "OpenCode:"
echo "  opencode              - Start OpenCode TUI"
echo "  opencode --help       - View OpenCode CLI options"
echo ""
echo "=== Post-create setup complete ==="
