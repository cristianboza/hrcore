# HR Core - Employee Profile Management System

Secure employee profile management system with Keycloak OAuth2 authentication, JWT token persistence, role-based access control, and admin session management.

## Quick Start

```bash
chmod +x build-and-deploy.sh
./build-and-deploy.sh
```

**Services:**
- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- Keycloak: http://localhost:9080

**Test Credentials:**
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | Super Admin |
| manager | manager123 | Manager |
| employee1 | employee123 | Employee |
| employee2 | employee123 | Employee |

## What's Implemented

✅ **Backend**
- Keycloak OAuth2/OIDC authentication
- JWT bearer token validation
- Token storage & persistence in PostgreSQL
- Role-based access control (SUPER_ADMIN, MANAGER, EMPLOYEE)
- Admin session management with force logout
- Employee profiles with field-level access control
- Absence request management
- Feedback system

✅ **Frontend**
- React 18 + TypeScript (no `any` types)
- Zustand state management
- OAuth2 code flow integration
- Admin dashboard for session management
- Responsive UI with Tailwind CSS

✅ **Security**
- Bearer token on all protected endpoints
- Token invalidation mechanism
- Automatic logout for invalid/expired tokens
- Database-validated token checking
- Role-based endpoint protection

## Tech Stack

- Backend: Spring Boot 3.5.7, Spring Security 6.5.6
- Frontend: React 18, TypeScript, Zustand, Tailwind CSS
- Auth: Keycloak 26.0.0 (OIDC/OAuth2)
- Database: PostgreSQL 15 with Flyway migrations
- Deployment: Docker Compose, Nginx

## Features by Role

**Super Admin**: View all profiles, edit all data, manage active sessions, force logout

**Manager**: View all profiles, approve/deny absences, leave feedback

**Employee**: View own profile (R/W), view others' public data (R/O), request absences, leave feedback

## Project Structure

```
hrcore/
├── src/main/java/              # Backend (Spring Boot)
├── fe-hrcore/                  # Frontend (React + TypeScript)
├── keycloak-realm.json         # Keycloak realm config
├── docker-compose.yml          # Service orchestration
└── build-and-deploy.sh         # Deploy script
```

## Future Improvements

### Backend Improvements

**Security & Authentication**
- [ ] Refresh token rotation for enhanced security
- [ ] OAuth2 password reset flows and email verification
- [ ] Rate limiting with Bucket4j on API endpoints
- [ ] API versioning (`/api/v1`, `/api/v2`)

**Data Validation & Error Handling**
- [ ] Input validation with `@Valid` on DTOs and request bodies
- [ ] Global exception handler with `@ControllerAdvice`
- [ ] Custom error response format with error codes

**Performance & Scalability**
- [ ] Redis caching layer with `@Cacheable` for profile lookups
- [ ] Pagination support with `Pageable` on list endpoints
- [ ] Database connection pooling optimization (HikariCP tuning)
- [ ] Query optimization with database indexes

**Data Management**
- [ ] Optimistic locking with `@Version` for concurrent profile updates
- [ ] Audit logging for admin actions (Spring Data Envers)
- [ ] Session activity logging and tracking
- [ ] Soft delete support for user profiles

**Features**
- [ ] File upload support for profile pictures and documents (multipart/form-data)
- [ ] Email notifications service integration (SMTP/SendGrid)
- [ ] Advanced RBAC with granular permissions
- [ ] Export profiles to CSV/PDF

### Frontend Improvements

**Form Validation & UX**
- [ ] Client-side form validation with Zod or React Hook Form
- [ ] Loading skeleton states for better perceived performance
- [ ] Confirmation dialogs for destructive actions

**Data Handling**
- [ ] Pagination UI components for profile and absence lists
- [ ] Optimistic UI updates in React Query mutations
- [ ] Error boundary components for graceful error handling
- [ ] Retry logic for failed API requests

**Features**
- [ ] File upload UI with drag-and-drop for profile pictures
- [ ] Real-time WebSocket notifications for absence approvals
- [ ] Search and filter functionality for profiles
- [ ] Dark mode support

**Accessibility & Polish**
- [ ] ARIA labels and keyboard navigation
- [ ] Responsive design improvements for mobile devices
- [ ] Loading states and progress indicators
- [ ] Form field autocomplete and validation feedback


