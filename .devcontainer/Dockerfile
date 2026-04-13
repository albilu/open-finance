# Open-Finance Development Container
# Base image with essential tools - features handle Java/Node/Git/Docker

FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

# Avoid prompts from apt
ARG DEBIAN_FRONTEND=noninteractive

# Install system dependencies
# Note: Java, Node, Git, and Docker are installed via DevContainer features
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Build essentials
    build-essential \
    curl \
    wget \
    unzip \
    zip \
    ca-certificates \
    gnupg \
    lsb-release \
    software-properties-common \
    # Node.js build dependencies
    python3 \
    python3-pip \
    # X11 dependencies (optional, for GUI apps if needed)
    xvfb \
    x11-utils \
    # GUI libraries (for potential browser testing in future)
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libdbus-1-3 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2 \
    libatspi2.0-0 \
    # SQLite
    sqlite3 \
    libsqlite3-dev \
    # Additional utilities
    vim \
    nano \
    htop \
    procps \
    net-tools \
    iputils-ping \
    jq \
    tree \
    && rm -rf /var/lib/apt/lists/*

# Switch to vscode user for remaining setup
USER vscode
WORKDIR /home/vscode

# Create directories for persistence
RUN mkdir -p /home/vscode/.m2 \
    && mkdir -p /home/vscode/.npm \
    && mkdir -p /home/vscode/.cache \
    && mkdir -p /home/vscode/.vscode-server

# Configure bash aliases
RUN echo 'alias ll="ls -lah"' >> /home/vscode/.bashrc \
    && echo 'alias mvn-test="mvn clean test"' >> /home/vscode/.bashrc \
    && echo 'alias mvn-build="mvn clean install"' >> /home/vscode/.bashrc

# Set up command history persistence (as root, then change ownership)
USER root
RUN mkdir -p /commandhistory \
    && touch /commandhistory/.bash_history \
    && chown -R vscode:vscode /commandhistory

USER vscode
RUN echo 'export PROMPT_COMMAND="history -a"' >> /home/vscode/.bashrc \
    && echo 'export HISTFILE=/commandhistory/.bash_history' >> /home/vscode/.bashrc

# Create workspace directory
USER root
RUN mkdir -p /workspace && chown vscode:vscode /workspace

# Create X11 display script for Chromium
RUN mkdir -p /usr/local/bin
COPY start-display.sh /usr/local/bin/start-display.sh
RUN chmod +x /usr/local/bin/start-display.sh && chown vscode:vscode /usr/local/bin/start-display.sh

# Create X11 socket directory with proper permissions
RUN mkdir -p /tmp/.X11-unix && chmod 1777 /tmp/.X11-unix

# Configure Git safe directory
USER vscode
RUN git config --global --add safe.directory /workspace

WORKDIR /workspace

# Default command (will be overridden by docker-compose)
CMD ["/bin/bash"]
