# DevContainer Quick Start Guide

## 🚀 Get Started in 3 Steps

### 1. Prerequisites
Install these tools:
- ✅ [Docker Desktop](https://www.docker.com/products/docker-desktop)
- ✅ [Visual Studio Code](https://code.visualstudio.com/)
- ✅ [Dev Containers Extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

### 2. Open in Container
```bash
# Clone the repository
git clone <repository-url>
cd open-finance

# Open in VSCode
code .
```

In VSCode:
1. Press `F1` (or `Ctrl+Shift+P` / `Cmd+Shift+P`)
2. Type: `Dev Containers: Reopen in Container`
3. Wait 10-15 minutes for first build ☕

### 3. Start Developing
```bash
# Terminal 1: Start backend
mvn spring-boot:run

# Terminal 2: Start frontend (new terminal)
cd open-finance-frontend
npm run dev

# Open browser
# Backend: http://localhost:8080
# Frontend: http://localhost:5173
```

## 🎯 What You Get

✨ **Pre-installed Tools**
- Java 21 + Maven 3.9.6
- Node.js 20.20.0 + npm 10.x
- OpenCode CLI (AI coding assistant)
- SQLite 3
- Docker-in-Docker
- Git + GitHub CLI

📦 **VSCode Extensions**
- Java Extension Pack
- Spring Boot Tools
- ESLint + Prettier
- Tailwind CSS IntelliSense
- React/TypeScript Support
- GitLens
- And 20+ more!

🔧 **Pre-configured**
- Maven dependencies pre-downloaded
- npm packages pre-installed
- Git hooks configured
- Code formatting set up
- Testing frameworks ready
- OpenCode with custom agents

## 🤖 Using OpenCode

OpenCode is an AI coding assistant that's pre-installed in the container.

```bash
# Start OpenCode TUI
opencode

# Initialize OpenCode for the project (first time)
/init

# View help
opencode --help
```

**Custom Agents Available:**
- `/dev-expert` - General development assistance
- `/frontend-dev` - React/TypeScript frontend work
- `/test` - Write unit tests
- `/compile` - Build and fix compilation errors
- `/code-review` - Review code quality
- `/debug` - Debug Java issues

**Tips:**
- Use `@` to reference files in prompts
- Use `Tab` to switch between Plan and Build modes
- Use `/undo` to revert changes
- Use `/share` to share conversations

See [OpenCode docs](https://opencode.ai/docs) for more.

## 🌐 Using Chromium Browser

Chromium is not installed by default as frontend tests use jsdom. If you need a browser for manual testing, you can install it:

```bash
# Install Playwright with bundled browsers
npx playwright install chromium

# Launch browser
npx playwright open http://localhost:5173
```

**Note**: System `chromium-browser` package is not available as it requires snap (incompatible with containers).

## 📝 Common Commands

### OpenCode
```bash
opencode                   # Start OpenCode TUI
/init                      # Initialize project (first time)
/dev-expert                # General development help
/test                      # Write tests
/compile                   # Fix compilation errors
```

### Backend
```bash
mvn clean install          # Build project
mvn test                   # Run tests
mvn spring-boot:run        # Start server
mvn spotless:apply         # Format code
mvn clean test jacoco:report  # Coverage report
```

### Frontend
```bash
cd open-finance-frontend
npm run dev                # Dev server
npm test                   # Run tests
npm run build              # Production build
npm run lint -- --fix      # Fix lint issues
```

### Docker
```bash
docker-compose up -d       # Start all services
docker-compose logs -f     # View logs
docker-compose down        # Stop services
```

## 🔍 Troubleshooting

**Container won't start?**
```bash
# Rebuild from scratch
F1 → Dev Containers: Rebuild Container
```

**Browser won't launch?**
```bash
# Restart X11 display
start-display.sh
```

**Port already in use?**
```bash
# Find and kill process
lsof -i :8080
kill -9 <PID>
```

**Slow performance?**
- Increase Docker resources: Docker Desktop → Settings → Resources
- Recommended: 4+ CPUs, 8GB+ RAM

## 📚 Full Documentation

See [.devcontainer/README.md](.devcontainer/README.md) for:
- Detailed configuration
- Advanced usage
- Customization options
- Security considerations
- Complete troubleshooting guide

## 🤝 Contributing

When modifying the devcontainer:
1. Test in a fresh container build
2. Update documentation
3. Commit changes with descriptive message

Happy coding! 🎉
