#!/bin/bash

# HR Core Application - Git Repository Setup Script
# This script initializes the GitHub repository and commits the initial setup

set -e

echo "🚀 Initializing HR Core Git Repository"
echo "========================================"

# Check if git is initialized
if [ ! -d .git ]; then
    echo "Initializing git repository..."
    git init
else
    echo "✓ Git repository already initialized"
fi

# Configure git user (if not already set)
if [ -z "$(git config user.name)" ]; then
    echo "Setting up git user configuration..."
    git config user.name "HR Core Developer"
    git config user.email "developer@hrcore.local"
fi

# Add all files
echo "Adding files to staging area..."
git add .

# Create initial commit
echo "Creating initial commit..."
git commit -m "Initial commit: Milestone 1 - Project Setup and Configuration

- Backend: Spring Boot 3.5.7 with Spring Data JPA, Security, and Flyway
- Frontend: React 19 with Vite, TypeScript, Tailwind CSS, and TanStack Query
- Database: PostgreSQL with Flyway migrations
- Configuration: CORS, Security, API client service
- Docker: docker-compose.yml for full stack deployment
- Documentation: Comprehensive setup and API documentation

Features:
✓ Backend API endpoints for user management
✓ Frontend with React Router and API client
✓ Authentication context for role-based access control
✓ Tailwind CSS styling framework
✓ Docker Compose setup for local development

Milestones:
✓ Milestone 1: Project Setup and Repository Publishing
🔄 Milestone 2: Employee Profile Management
🔄 Milestone 3: Feedback Functionality with AI Polishing
🔄 Milestone 4: Absence Request Functionality
🔄 Milestone 5: Finalization and Deployment
"

echo ""
echo "✓ Repository initialized successfully!"
echo ""
echo "Next steps:"
echo "1. Create a GitHub repository: https://github.com/new"
echo "2. Run: git remote add origin https://github.com/YOUR_USERNAME/hrcore.git"
echo "3. Run: git branch -M main"
echo "4. Run: git push -u origin main"
echo ""
echo "To start development:"
echo "  Backend:  mvn spring-boot:run"
echo "  Frontend: cd fe-hrcore && npm install && npm run dev"
echo "  Docker:   docker-compose up"

