# Personal Tracker

A professional-grade productivity tracking application that helps users track daily tasks, measure productivity, and analyze effort distribution across applications/projects through interactive reports and visualizations.

## 🎯 Overview

Personal Tracker is an enterprise-style microservices architecture focused on clear service boundaries and independent deployments. It provides:

- **Task Management**: Create, update, and track tasks with complexity ratings, deadlines, and status tracking
- **Productivity Analytics**: Generate reports with time-based filters, complexity breakdowns, and productivity trends
- **Data Visualization**: Interactive pie charts and visual summaries for insights
- **User Authentication**: Secure JWT-based authentication with password reset functionality

## ⭐ Highlights

- Reactive microservices using Spring WebFlux + R2DBC
- JWT-based authentication with secure password reset
- Real-time productivity analytics with visual dashboards
- Clean separation of Auth, Task, and Reporting services


## 🏗️ Architecture

### Microservices Architecture

The application follows a microservices pattern with three main services:

```
┌─────────────────┐
│  React Frontend │
│   (Port 5173)   │
└────────┬────────┘
         │
    ┌────┴────┬──────────────┬──────────────┐
    │         │              │              │
┌───▼───┐ ┌──▼───┐    ┌─────▼─────┐  ┌─────▼─────┐
│ Auth  │ │ Task │    │ Reporting │  │  Common   │
│Service│ │Service│    │  Service  │  │  Module   │
│:8081  │ │:8082 │    │   :8083   │  │  (Shared) │
└───────┘ └──────┘    └───────────┘  └───────────┘
    │         │              │
    └─────────┴──────────────┘
              │
    ┌─────────▼─────────┐
    │   PostgreSQL      │
    │  (R2DBC Reactive) │
    └───────────────────┘
```

> **Note**: Reporting service currently shares the task database to keep the system simple and cost-effective. In a production-scale setup, this can be evolved to event-driven data replication.

### Services

- **auth-service** (Port 8081): User authentication, registration, password reset
- **task-service** (Port 8082): Task CRUD operations, notes, status management, archival
- **reporting-service** (Port 8083): Analytics, aggregations, productivity reports
- **common**: Shared code (JWT utilities, error handling, validation)

## 🛠️ Tech Stack

### Backend
- **Java 21**
- **Spring Boot 3.2.6**
- **Spring WebFlux** (Fully reactive, non-blocking)
- **R2DBC PostgreSQL** (Reactive database access)
- **Spring Security** (JWT-based authentication)
- **Maven** (Build tool)
- **Swagger/OpenAPI** (API documentation)

### Frontend
- **React 19.2.0**
- **TypeScript 5.9.3**
- **Vite 7.2.4**
- **Recharts 3.6.0** (Data visualization)

### Database
- **PostgreSQL** (with R2DBC for reactive access)

### Development
- **OpenSpec** (Specification-driven development)
- **JUnit 5** (Testing)

## 📋 Prerequisites

- **Java 21+**
- **Maven 3.9+**
- **Node.js 20+** and **npm**
- **PostgreSQL 12+**
- **Git**

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd personal-tracker
```

### 2. Database Setup

Create PostgreSQL databases for each service:

```sql
-- Auth Service Database
CREATE DATABASE personal_tracker_auth;

-- Task Service Database
CREATE DATABASE personal_tracker_task;

-- Reporting Service Database (uses same as task-service)
-- personal_tracker_task is shared


The services will automatically create tables on first startup using `schema.sql` files.

### 3. Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database Configuration
export R2DBC_URL=r2dbc:postgresql://localhost:5432
export R2DBC_USERNAME=tracker
export R2DBC_PASSWORD=tracker

# JWT Configuration
export JWT_SECRET=your-secret-key-at-least-32-characters-long

# Frontend URLs (optional)
export VITE_AUTH_URL=http://localhost:8081
export VITE_TASK_URL=http://localhost:8082
export VITE_REPORT_URL=http://localhost:8083

# Email Configuration (for password reset - optional)
export SMTP_HOST=smtp.example.com
export SMTP_PORT=587
export SMTP_USERNAME=your-email@example.com
export SMTP_PASSWORD=your-password
export SMTP_FROM=noreply@example.com
export MAIL_SINK_ENABLED=true  # For development (captures emails in logs)
```

### 4. Start Backend Services

From the repository root:

#### Auth Service (Port 8081)
```bash
cd backend/auth-service
mvn spring-boot:run
```

#### Task Service (Port 8082)
```bash
cd backend/task-service
mvn spring-boot:run
```

#### Reporting Service (Port 8083)
```bash
cd backend/reporting-service
mvn spring-boot:run
```

**Note**: Each service runs independently. Start them in separate terminal windows or use a process manager.

### 5. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at `http://localhost:5173`

### 6. Access the Application

1. Open `http://localhost:5173` in your browser
2. Create an account using the Create account flow
3. Log in and start using the application


## 📚 API Documentation

Each service provides Swagger/OpenAPI documentation:

- **Auth Service**: http://localhost:8081/swagger-ui.html
- **Task Service**: http://localhost:8082/swagger-ui.html
- **Reporting Service**: http://localhost:8083/swagger-ui.html

## 🧪 Testing

### Backend Tests

Run tests for all services:

```bash
# From backend directory
mvn test

# For specific service
cd backend/task-service
mvn test
```

### Frontend Tests

```bash
cd frontend
npm test
```

## 📁 Project Structure

```
personal-tracker/
├── backend/
│   ├── auth-service/          # Authentication microservice
│   │   ├── src/main/java/     # Java source code
│   │   ├── src/main/resources/ # Configuration & schema
│   │   └── src/test/          # Test code
│   ├── task-service/          # Task management microservice
│   ├── reporting-service/     # Analytics microservice
│   ├── common/                # Shared code module
│   └── pom.xml               # Parent POM
├── frontend/                  # React frontend application
│   ├── src/                  # React source code
│   ├── public/               # Static assets
│   └── package.json          # Node dependencies
├── openspec/                 # Specification-driven development
│   ├── specs/                # Current specifications
│   ├── changes/              # Proposed changes
│   └── AGENTS.md             # OpenSpec guidelines
└── README.md                 # This file
```

## 🔐 Security Features

- **JWT Authentication**: Secure token-based authentication
- **Password Validation**: 
  - Minimum 8 characters
  - At least 1 uppercase, 1 lowercase, 1 number, 1 special character
- **Login Attempt Tracking**: Maximum 5 attempts before account lockout
- **Password Reset**: Email-based reset with token/OTP support
- **Per-User Data Isolation**: Users can only access their own data

## 🎨 Features

### Task Management
- Create tasks with title, description, application/project, complexity, deadline
- Update task status (OPEN → IN_PROGRESS → CLOSED)
- Add multiple notes to tasks (append-only with timestamps)
- Automatic archival when tasks are closed (read-only)
- Filter tasks by status (include/exclude archived)

### Reporting & Analytics
- **Time-based Filters**: Weekly, Monthly, Quarterly, Half-Yearly, Yearly
- **Attribute Filters**: By application/project, complexity
- **Sorting**: By completion date or complexity
- **Visualizations**: 
  - Status distribution (pie chart)
  - Application mix (pie chart)
  - Complexity breakdown (pie chart)
  - Productivity trends (bar chart)

### User Management
- User registration with email
- Password reset via email link or OTP
- Session management with JWT tokens

## 🔧 Development

### Code Style
- Follows SonarQube standards
- Clean code principles
- TDD (Test-Driven Development) approach
- Reactive programming patterns (non-blocking)

### Building

```bash
# Build all backend services
cd backend
mvn clean install

# Build frontend
cd frontend
npm run build
```

### Running in Development

Backend services support hot-reload with Spring Boot DevTools (if configured).

Frontend uses Vite's HMR (Hot Module Replacement) for instant updates.

## 📝 OpenSpec

This project uses [OpenSpec](https://github.com/your-openspec-repo) for specification-driven development:

- **Specs** (`openspec/specs/`): Current truth - what IS built
- **Changes** (`openspec/changes/`): Proposals - what SHOULD change
- **Archive** (`openspec/changes/archive/`): Completed changes

See `openspec/AGENTS.md` for guidelines on creating and applying changes.

## 🐛 Troubleshooting

### Database Connection Issues
- Ensure PostgreSQL is running
- Check database credentials in `application.yml`
- Verify databases are created

### Port Conflicts
- Auth Service: 8081
- Task Service: 8082
- Reporting Service: 8083
- Frontend: 5173

Change ports in respective `application.yml` files if needed.

### JWT Token Issues
- Ensure `JWT_SECRET` is set (minimum 32 characters)
- Tokens expire after 1 hour (configurable)

### Email Not Working
- Set `MAIL_SINK_ENABLED=true` for development (emails logged to console)
- Configure SMTP settings for production


---

**Built with ❤️ using Spring Boot, React, and OpenSpec**
