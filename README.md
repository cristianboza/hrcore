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
- Backend API Docs: http://localhost:8080/swagger-ui.html
- Keycloak: http://localhost:9080

**Test Credentials:**
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | Super Admin |
| manager1 | manager123 | Manager (Engineering) |
| manager2 | manager123 | Manager (Sales) |
| employee1 | employee123 | Employee (Engineering) |
| employee2 | employee123 | Employee (Engineering) |
| employee3 | employee123 | Employee (Engineering) |
| employee4 | employee123 | Employee (Engineering) |
| employee5 | employee123 | Employee (Sales) |
| employee6 | employee123 | Employee (Sales) |
| employee7 | employee123 | Employee (Sales) |
| employee8 | employee123 | Employee (Sales) |

**Organizational Structure:**
- **Engineering Hierarchy**: manager1 → employee1, employee2, employee3, employee4
- **Sales Hierarchy**: manager2 → employee5, employee6, employee7, employee8

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
- **OpenAPI/Swagger documentation** - Interactive API documentation at `/swagger-ui.html`

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
- API Documentation: SpringDoc OpenAPI 2.6.0 (Swagger UI)
- Frontend: React 18, TypeScript, Zustand, Tailwind CSS
- Auth: Keycloak 26.0.0 (OIDC/OAuth2)
- Database: PostgreSQL 15 with Flyway migrations
- Deployment: Docker Compose, Nginx

## Features by Role

**Super Admin**: View all profiles, edit all data, manage active sessions, force logout

**Manager**: View all profiles, approve/deny absences, leave feedback

**Employee**: View own profile (R/W), view others' public data (R/O), request absences, leave feedback

## Feature Flags

The application uses a centralized feature flag system to enable/disable experimental or premium features. Feature flags are configured via environment variables and are consistent between backend and frontend.

### Architecture

- **Centralized Constants**: Feature keys are defined in `FeatureFlagConstants.java` (backend) and `featureFlags.ts` (frontend)
- **Environment-based**: All feature flags are configured through environment variables
- **No Runtime API Calls**: Frontend reads flags from environment variables directly (no backend calls)
- **Type-safe**: Strong typing ensures consistency across the application

### Available Feature Flags

**AI-Powered Feedback Polish** (`feedback.ai-polish`)
- Enables AI-powered feedback content polishing using HuggingFace API
- Allows managers to enhance feedback text before approval
- Requires HuggingFace API key to function
- Protected by `@RequireFeature` annotation on backend endpoint

### Configuration

**Backend** (`application.properties` or environment variables):
```bash
# Enable AI feedback polishing feature
features.feedback.ai-polish.enabled=true
# HuggingFace API key (required if feature is enabled)
features.feedback.ai-polish.huggingface-api-key=your_huggingface_api_key_here
```

**Frontend** (`.env.local`):
```bash
# Enable AI feedback polishing feature
VITE_FEATURE_FEEDBACK_AI_POLISH_ENABLED=true
```

See `.env.example` for all available feature flags.

### Usage

**Backend**: Use `@RequireFeature` annotation to protect endpoints:
```java
import static com.example.hrcore.config.FeatureFlagConstants.*;

@RequireManagerOrAbove
@RequireFeature(FEEDBACK_AI_POLISH)
@PostMapping("/{feedbackId}/polish")
public ResponseEntity<FeedbackDto> polishFeedback(@PathVariable Long feedbackId) {
    // This endpoint is only accessible when feature is enabled
}
```

**Backend**: Check feature programmatically:
```java
@RequiredArgsConstructor
public class MyService {
    private final FeatureFlags featureFlags;
    
    public void doSomething() {
        if (featureFlags.isFeatureEnabled(FEEDBACK_AI_POLISH)) {
            // Feature is enabled and configured
        }
    }
}
```

**Frontend**: Use the `useFeature` hook with centralized constants:
```typescript
import { useFeature, FEATURE_FLAGS } from '../hooks/useFeatureFlags';

function MyComponent() {
  const isAiPolishEnabled = useFeature(FEATURE_FLAGS.FEEDBACK_AI_POLISH);
  
  return (
    <>
      {isAiPolishEnabled && <PolishButton />}
    </>
  );
}
```

### Adding New Feature Flags

1. **Backend**: Add constant to `FeatureFlagConstants.java`:
   ```java
   public static final String MY_NEW_FEATURE = "my.new-feature";
   ```

2. **Backend**: Add property fields to `FeatureFlags.java`:
   ```java
   @Value("${features." + MY_NEW_FEATURE + ".enabled:false}")
   private boolean myNewFeatureEnabled;
   ```

3. **Backend**: Update `isFeatureEnabled()` method in `FeatureFlags.java`:
   ```java
   case MY_NEW_FEATURE:
       return myNewFeatureEnabled;
   ```

4. **Backend**: Add to `application.properties`:
   ```properties
   features.my.new-feature.enabled=${FEATURE_MY_NEW_FEATURE_ENABLED:false}
   ```

5. **Frontend**: Add to `featureFlags.ts`:
   ```typescript
   export const FEATURE_FLAGS = {
     MY_NEW_FEATURE: 'my.new-feature',
   } as const;
   ```

6. **Frontend**: Add to `.env.example`:
   ```bash
   VITE_FEATURE_MY_NEW_FEATURE_ENABLED=false
   ```

7. **Use the feature**: Use `@RequireFeature(MY_NEW_FEATURE)` on backend endpoints and `useFeature(FEATURE_FLAGS.MY_NEW_FEATURE)` in frontend components

**Important**: Always ensure the feature key matches exactly between backend `FeatureFlagConstants.java` and frontend `featureFlags.ts`.

## API Documentation

The backend provides comprehensive API documentation via **OpenAPI/Swagger UI**.

### Access API Documentation

**Interactive Swagger UI**: http://localhost:8080/swagger-ui.html

The Swagger UI provides:
- Complete list of all available API endpoints
- Request/response schemas with examples
- Try-it-out functionality to test endpoints directly
- Authentication support (click "Authorize" and paste your JWT token)
- Detailed parameter descriptions and validation rules

### API Endpoints Overview

**Authentication** (`/api/auth`)
- `GET /api/auth/login-redirect` - Get Keycloak login URL
- `POST /api/auth/callback` - Exchange OAuth code for token
- `POST /api/auth/logout` - Logout and invalidate session
- `GET /api/auth/me` - Get current user info

**Profiles** (`/api/profiles`)
- `GET /api/profiles` - List profiles with filtering and pagination
- `POST /api/profiles` - Create new profile (Manager+)
- `GET /api/profiles/{userId}` - Get profile by ID
- `PUT /api/profiles/{userId}` - Update profile
- `DELETE /api/profiles/{userId}` - Delete profile (Manager+)
- `GET /api/profiles/me` - Get current user profile
- `GET /api/profiles/{userId}/permissions` - Check profile permissions
- `GET /api/profiles/{userId}/direct-reports` - Get direct reports

**Absence Requests** (`/api/absence-requests`)
- `GET /api/absence-requests/search` - Search requests with filters
- `POST /api/absence-requests` - Submit new absence request
- `GET /api/absence-requests` - Get user's absence requests
- `GET /api/absence-requests/pending` - Get pending requests (Manager+)
- `PUT /api/absence-requests/{requestId}/approve` - Approve request (Manager+)
- `PUT /api/absence-requests/{requestId}/reject` - Reject request (Manager+)

**Feedback** (`/api/feedback`)
- `POST /api/feedback` - Submit feedback
- `GET /api/feedback/search` - Search feedback with filters
- `GET /api/feedback` - Get user's feedback
- `POST /api/feedback/{feedbackId}/polish` - AI-polish feedback (requires feature flag)

### Using Swagger UI

1. **Navigate** to http://localhost:8080/swagger-ui.html
2. **Authenticate**: Click "Authorize" button, enter `Bearer <your-jwt-token>`
3. **Explore**: Browse endpoints by controller/tag
4. **Test**: Click "Try it out" on any endpoint to test with real data
5. **View Schemas**: Check request/response models at the bottom

### API Documentation in Code

All endpoints are documented with OpenAPI annotations:
- `@Operation` - Endpoint description
- `@Parameter` - Parameter details
- `@ApiResponse` - Response codes and descriptions
- `@Tag` - Controller grouping
- `@SecurityRequirement` - Authentication requirements

Example:
```java
@Operation(
    summary = "Get all profiles",
    description = "Retrieve paginated list of user profiles with optional filtering"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved profiles"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden")
})
@GetMapping
public ResponseEntity<PageResponse<UserDto>> getAllProfiles(...) {
    // implementation
}
```

**Important**: Always ensure the feature key matches exactly between backend `FeatureFlagConstants.java` and frontend `featureFlags.ts`.

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


