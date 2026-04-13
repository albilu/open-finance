# Open-Finance Development Container

This devcontainer provides a complete development environment for the Open-Finance project with all necessary tools pre-installed.

## What's Included

### Languages & Runtimes
- **Java 21 LTS** - Latest Java LTS version with OpenJDK
- **Node.js 20.18.1 LTS** - Via nvm with bundled npm 10.x
- **TypeScript** - Latest version with ts-node

### Build Tools
- **Maven 3.9.6** - Java build and dependency management
- **npm** - Node.js package manager
- **Vite** - Frontend build tool
- **Vitest** - Frontend testing framework

### Browsers
- **Chromium** - Full browser with Chrome DevTools
- **X11 Virtual Display** - Headless display server (Xvfb) on `:99`
- **VNC Server** - Remote browser viewing on port `5900`
- **ChromeDriver** - Selenium WebDriver for browser automation

### Databases
- **SQLite 3** - Embedded database for development
- **H2** - In-memory database for testing (via Maven)

### IDEs & Extensions
- **VSCode Extensions** - Pre-configured with Java, Spring Boot, React, TypeScript, ESLint, Prettier, and more
- **Language Servers** - Java Language Server, TypeScript Language Server
- **Debuggers** - Java Debug, Chrome Debug

### DevOps Tools
- **Docker-in-Docker** - Build and run containers from within the devcontainer
- **Git** - Version control with GitLens extension
- **GitHub CLI** - Manage pull requests and issues

## Getting Started

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop) (Windows/Mac) or Docker Engine (Linux)
- [Visual Studio Code](https://code.visualstudio.com/)
- [Dev Containers Extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

### Open in Container

1. Open the project folder in VSCode
2. Press `F1` and select `Dev Containers: Reopen in Container`
3. Wait for the container to build (first time takes ~10-15 minutes)
4. Container will automatically install all dependencies

### Quick Start Commands

#### Backend Development
```bash
# Build the project
mvn clean install

# Run backend server (http://localhost:8080)
mvn spring-boot:run

# Run tests
mvn test

# Run with coverage
mvn clean test jacoco:report

# Format code
mvn spotless:apply
```

#### Frontend Development
```bash
# Navigate to frontend
cd open-finance-frontend

# Install dependencies (already done in post-create)
npm install

# Start dev server (http://localhost:5173)
npm run dev

# Run tests
npm test

# Run tests with UI
npm run test -- --ui

# Build for production
npm run build

# Lint and fix
npm run lint -- --fix

# Format code
npm run format
```

#### Using Chromium Browser
```bash
# Launch Chromium (with X11 display)
chromium-browser

# Or use the alias
chrome

# Launch with specific URL
chromium-browser http://localhost:5173

# Headless mode for testing
chromium-browser --headless --disable-gpu --dump-dom http://localhost:8080
```

#### Database Management
```bash
# Open SQLite database
sqlite3 data/openfinance.db

# Run SQL query
sqlite3 data/openfinance.db "SELECT * FROM users;"

# Export schema
sqlite3 data/openfinance.db .schema > schema.sql
```

#### Docker Commands
```bash
# Build Docker images
docker build -t open-finance-backend .
docker build -f Dockerfile.frontend -t open-finance-frontend .

# Run with docker-compose
docker-compose up -d

# View logs
docker-compose logs -f
```

## Ports

The following ports are forwarded to your host machine:

| Port | Service | URL |
|------|---------|-----|
| 8080 | Backend API | http://localhost:8080 |
| 5173 | Frontend Dev Server | http://localhost:5173 |
| 3000 | Frontend Production | http://localhost:3000 |
| 5900 | VNC Server (Browser View) | vnc://localhost:5900 |

## X11 Display Setup

The container includes a virtual X11 display for running Chromium:

- **Display**: `:99`
- **Resolution**: 1920x1080x24
- **VNC Access**: Port 5900 (no password)

The display starts automatically. To manually restart:
```bash
start-display.sh
```

To view the browser remotely, connect with a VNC client to `localhost:5900`.

## VSCode Extensions

Pre-installed extensions include:

### Java Development
- Extension Pack for Java
- Spring Boot Extension Pack
- Lombok Annotations Support
- Maven for Java

### Frontend Development
- ESLint
- Prettier
- Tailwind CSS IntelliSense
- ES7+ React/Redux/React-Native snippets
- TypeScript

### Code Quality
- SonarLint
- Coverage Gutters
- TODO Tree

### Version Control
- GitLens
- GitHub Pull Requests

### Testing
- Vitest Explorer

### Utilities
- Path Intellisense
- IntelliCode
- Code Spell Checker
- REST Client

## Customization

### Add More Extensions
Edit `.devcontainer/devcontainer.json` and add extension IDs to the `extensions` array.

### Change Java Version
Edit `.devcontainer/Dockerfile` and modify the `sdk install java` line.

### Change Node Version
Edit `.devcontainer/Dockerfile` and modify the `NODE_VERSION` environment variable.

### Add System Packages
Edit `.devcontainer/Dockerfile` and add packages to the `apt-get install` command.

## Troubleshooting

### X11 Display Issues
If Chromium fails to launch:
```bash
# Check display
echo $DISPLAY  # Should be :99

# Restart display
pkill Xvfb
start-display.sh

# Test display
xdpyinfo -display :99
```

### Maven Dependencies
If Maven dependencies fail to download:
```bash
# Clear cache
rm -rf ~/.m2/repository

# Re-download
mvn dependency:resolve -U
```

### Node Modules
If npm packages are corrupted:
```bash
cd open-finance-frontend
rm -rf node_modules package-lock.json
npm install
```

### Container Rebuild
If the container is broken:
1. Press `F1` → `Dev Containers: Rebuild Container`
2. Or delete volumes and rebuild:
   ```bash
   docker-compose down -v
   docker-compose build --no-cache
   ```

### Port Already in Use
If ports are already bound:
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

## Performance Tips

### Speed Up Maven Builds
The container includes a persistent Maven cache at `~/.m2`. First builds will be slow, but subsequent builds are much faster.

### Speed Up npm Installs
The container includes a persistent npm cache at `~/.npm`.

### Increase Container Resources
In Docker Desktop:
- Settings → Resources
- Increase CPUs to 4+ and Memory to 8GB+ for better performance

### Use mvnd (Maven Daemon)
For even faster Maven builds:
```bash
# Install mvnd
sdk install mvnd

# Use instead of mvn
mvnd clean install
```

## Security

### Secrets Management
- Never commit `.env` files with real secrets
- Use `.env.example` as a template
- Set environment variables in the container or use a secrets manager

### Database Encryption
- Enable SQLCipher for production databases
- Use strong master keys (not the example values)

### Docker Socket
The container has access to the Docker socket for Docker-in-Docker. Be cautious when running untrusted code.

## Contributing

When making changes to the devcontainer:
1. Test your changes in a fresh container
2. Document any new tools or configuration
3. Update this README with new commands or troubleshooting steps
4. Commit both `.devcontainer/` and updated docs

## Resources

- [VS Code Dev Containers](https://code.visualstudio.com/docs/devcontainers/containers)
- [Open-Finance Documentation](../README.md)
- [Contributing Guidelines](../CONTRIBUTING.md)
- [Agent Guidelines](../AGENTS.md)
