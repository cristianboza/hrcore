# HR Core - Employee Profile Management System

Full-stack HR application with OAuth2 authentication, role-based access control, AI-powered feedback, and admin session management.

## Quick Start

```bash
chmod +x build-and-deploy.sh
./build-and-deploy.sh
```

**Services:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- API Docs (Swagger): http://localhost:8080/swagger-ui.html
- Keycloak: http://localhost:9080

**Test Credentials:**
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | Super Admin |
| manager1 | manager123 | Manager (Engineering) |
| manager2 | manager123 | Manager (Sales) |
| employee1 | employee123 | Employee (Engineering) |
| employee5 | employee123 | Employee (Sales) |

---

## Tech Stack

**Backend:**
- Spring Boot 3.5.7 + Spring Security 6.5.6
- PostgreSQL 15 + Flyway migrations
- Keycloak 26.0.0 (OAuth2/OIDC)
- QueryDSL 5.1.0 (type-safe queries)
- MapStruct 1.6.3 (DTO mapping)
- SpringDoc OpenAPI 2.7.0 (Swagger)
- Bucket4j 8.10.1 (rate limiting)
- Caffeine (caching)
- Actuator + Prometheus (metrics)

**Frontend:**
- React 19.2.0 + TypeScript 5.9.3 (zero `any` types)
- Zustand 4.4.1 (state management)
- TanStack Query 5.28.0 (server state)
- React Hook Form 7.66.1 + Zod 4.1.12 (validation)
- Tailwind CSS 3.4.1 + Radix UI
- Vite (build tool)
- i18next (internationalization)

**Infrastructure:**
- Docker Compose orchestration
- Nginx reverse proxy
- Multi-stage Docker builds

---

## Core Features

### ✅ Employee Profiles
- Role-based view/edit permissions (owner, manager, coworker)
- Field-level access control (sensitive vs. public data)
- Search & filtering with pagination
- Organizational hierarchy (manager-employee relationships)
- Department-based organization

### ✅ Feedback System
- Submit feedback to any employee
- **AI-powered polishing** (HuggingFace API, feature-flagged)
- Manager approval workflow (PENDING → APPROVED/REJECTED)
- Advanced search with filters
- Only approved feedback visible to recipient

### ✅ Absence Requests
- Submit absence requests with date ranges
- Manager approval/rejection workflow
- Conflict detection
- Direct manager authorization only

### ✅ Admin Session Management
- View active sessions (Super Admin only)
- Force logout any user
- Real-time session tracking

### ✅ Security & Authentication
- OAuth2 Authorization Code Flow
- JWT bearer token validation
- Token persistence & invalidation in database
- Multi-layer authorization (annotations + service + context)
- Automatic logout on token expiry
- 3-tier RBAC (Super Admin, Manager, Employee)

### ✅ Feature Flags
- Centralized feature flag system (backend + frontend)
- AOP-based enforcement with `@RequireFeature` annotation
- Environment-driven configuration
- Type-safe constants across stack

---

## Architecture

```
hrcore/
├── src/main/java/com/example/hrcore/
│   ├── controller/          # REST endpoints with OpenAPI docs
│   ├── service/             # Business logic with transactions
│   ├── repository/          # JPA repositories with QueryDSL
│   ├── entity/              # Domain models with relationships
│   ├── dto/                 # Data transfer objects
│   ├── mapper/              # MapStruct DTO converters
│   ├── security/            # Custom security aspects & filters
│   ├── config/              # Feature flags, security, OpenAPI
│   └── specification/       # QueryDSL query specifications
├── src/main/resources/
│   └── db/migration/        # Flyway SQL migrations (11 files)
├── fe-hrcore/src/
│   ├── components/          # React components
│   ├── hooks/               # Custom hooks (useFeatureFlags, useProfile)
│   ├── services/            # API clients (TypeScript)
│   ├── types/               # TypeScript type definitions
│   ├── store/               # Zustand state stores
│   └── i18n/                # Internationalization config
└── docker-compose.yml       # Full stack orchestration
```

**Design Principles:**
- **Clean Architecture:** Controller → Service → Repository pattern
- **Security:** Multi-layer authorization (annotations + service logic + context objects)
- **Type Safety:** MapStruct (compile-time) + QueryDSL (type-safe queries) + TypeScript (zero `any`)
- **Testability:** Dependency injection, context objects for operations
- **Database Versioning:** 11 Flyway migrations for schema evolution

### Why Monolith Over Microservices?

**Chosen:** Modular monolith with clean boundaries  
**Not Chosen:** Microservices architecture

**Reasoning:**
- **Domain Scope:** HR core features are highly cohesive (profiles, feedback, absences share user context)
- **Data Consistency:** Strong transactional boundaries needed (e.g., absence approval + notification in one transaction)
- **Team Size:** Single full-stack developer - microservices add operational overhead without benefit
- **Deployment Complexity:** No need for independent scaling of services at this stage
- **Development Velocity:** Monolith enables faster iteration, easier debugging, single deployment
- **Cost:** One database, one runtime, simpler infrastructure (vs. service mesh, API gateway, distributed tracing)

**Migration Path:** Modular design with clear package boundaries enables future extraction to microservices if needed (e.g., separate notification service, analytics service). Current structure supports vertical slicing by domain.

**When to Consider Microservices:**
- Team grows to 10+ engineers with specialized domain ownership
- Independent scaling requirements emerge (e.g., analytics service needs 10x resources)
- Polyglot persistence needed (different databases for different domains)
- Regulatory requirements demand physical separation

---

## AI Integration

**HuggingFace API** (feature-flagged):
- **Model:** `facebook/bart-large-cnn` for text summarization
- **Use Case:** Polish feedback content before manager approval
- **Graceful Degradation:** Falls back to original text on API failure
- **Configuration:** `HUGGINGFACE_API_KEY` environment variable

**Enable in `docker-compose.yml`:**
```yaml
environment:
  FEATURE_FEEDBACK_AI_POLISH_ENABLED: true
  HUGGINGFACE_API_KEY: your_api_key_here
```

---


## Why These Choices?

| Technology | Rationale |
|------------|-----------|
| **Keycloak** | Industry-standard IdP, saves months of auth development, enterprise-ready |
| **QueryDSL** | Type-safe queries prevent runtime SQL errors, better IDE support |
| **MapStruct** | Compile-time DTO mapping (faster than reflection-based alternatives) |
| **Zustand** | Simpler than Redux, no boilerplate, 1KB bundle size |
| **TanStack Query** | Server state caching eliminates custom cache logic, automatic refetching |
| **Feature Flags** | Enables gradual rollouts, A/B testing, kill switches for incidents |
| **Docker Compose** | Dev/prod parity, easy onboarding, reproducible environments |
| **PostgreSQL** | ACID compliance, jsonb support, production-grade reliability |
| **TypeScript (strict)** | Catch errors at compile time, better refactoring, self-documenting code |
| **Flyway** | Database versioning, reproducible migrations, rollback support |

---

## API Documentation

**Interactive Swagger UI:** http://localhost:8080/swagger-ui.html

**Key Endpoints:**

## Development Notes

**Running Tests:**
```bash
# Backend tests
./mvnw test

# Mutation testing (PITest)
./mvnw org.pitest:pitest-maven:mutationCoverage
```

**Database Migrations:**
```bash
# Migrations run automatically on startup via Flyway
# Located in: src/main/resources/db/migration/
```

**Feature Flag Example:**

Backend:
```java
@RequireFeature(FeatureFlagConstants.FEEDBACK_AI_POLISH)
@PostMapping("/{feedbackId}/polish")
public ResponseEntity<FeedbackDto> polishFeedback(@PathVariable Long feedbackId) {
    // Only accessible when feature is enabled
}
```

Frontend:
```typescript
import { useFeature, FEATURE_FLAGS } from '../hooks/useFeatureFlags';

const isAiPolishEnabled = useFeature(FEATURE_FLAGS.FEEDBACK_AI_POLISH);
{isAiPolishEnabled && <PolishButton />}
```

---
