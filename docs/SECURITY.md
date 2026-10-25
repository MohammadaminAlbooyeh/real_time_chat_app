# Security Overview

## Authentication

### JWT (JSON Web Tokens)

The application uses JWT for stateless authentication. Tokens are issued by `api-service` and independently verified by both `api-service` (REST) and `websocket-service` (WebSocket upgrade), using the [jjwt](https://github.com/jwtk/jjwt) library (`jjwt-api`/`jjwt-impl`/`jjwt-jackson` 0.12.6).

**Token types:**
- **Access token:** Short-lived (default 30 min, `JWT_ACCESS_TOKEN_EXPIRE_MINUTES`), used for API and WebSocket authentication
- **Refresh token:** Long-lived (default 7 days, `JWT_REFRESH_TOKEN_EXPIRE_DAYS`), used to obtain new access tokens

**JWT payload (`type` claim distinguishes access vs. refresh):**
```json
{
  "sub": "user-uuid",
  "username": "alice",
  "type": "access",
  "exp": 1700000000
}
```

**Algorithm:** HS256 (HMAC with SHA-256)

**Configuration** (`services/api-service/src/main/resources/application.yml`):
```yaml
app:
  jwt:
    secret-key: ${JWT_SECRET_KEY:dev-secret-key-change-in-production}  # NEVER hardcode in production
    access-token-expire-minutes: ${JWT_ACCESS_TOKEN_EXPIRE_MINUTES:30}
    refresh-token-expire-days: ${JWT_REFRESH_TOKEN_EXPIRE_DAYS:7}
```

`websocket-service` has its own `JWT_SECRET_KEY` env var (`services/websocket-service/.env`) — **it must be set to the exact same value as api-service's**, or every WebSocket connection will fail to authenticate (see TROUBLESHOOTING.md).

**Token verification** — `websocket-service`'s `JwtService` (`services/websocket-service/src/main/java/com/chatapp/websocket/security/JwtService.java`):
```java
public Claims verifyToken(String token) {
    try {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"access".equals(claims.get("type", String.class))) {
            return null;
        }
        return claims;
    } catch (JwtException | IllegalArgumentException e) {
        return null;
    }
}
```
`api-service`'s equivalent lives in `JwtService`/`JwtAuthFilter` and is wired as a Spring Security filter (see below).

### Authentication Flow

1. **Register/Login:** `POST /api/v1/auth/register` or `/api/v1/auth/login` → Server validates → Returns `accessToken` + `refreshToken`
2. **API requests:** Client sends `Authorization: Bearer <access_token>` header
3. **Token refresh:** `POST /api/v1/auth/refresh` with the refresh token → Returns new `accessToken` + `refreshToken`
4. **WebSocket:** Client connects with `ws://host/ws?token=<access_token>` query parameter; connection is closed with code `4001` if the token is missing, invalid, expired, or not an access token

### Password Hashing

Passwords are hashed using **BCrypt** via Spring Security's `BCryptPasswordEncoder` (`services/api-service/src/main/java/com/chatapp/api/config/SecurityConfig.java`):

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Password requirements (enforced in `UserCreate`/`PasswordChange` DTO validation):**
- Minimum 8 characters
- Maximum 128 characters
- Stored as a BCrypt hash, never in plain text
- Never logged or exposed in responses

## Authorization

### Spring Security Filter Chain

`SecurityConfig.java` configures:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/health", "/health/**").permitAll()
            .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

CSRF protection is disabled because the API is a stateless, token-authenticated JSON API with no browser session/cookie auth to protect; sessions are never created server-side (`SessionCreationPolicy.STATELESS`).

`JwtAuthFilter` (a custom `OncePerRequestFilter`, `services/api-service/src/main/java/com/chatapp/api/security/JwtAuthFilter.java`) runs before Spring Security's own authentication filter. For every request not on the permit-all list it:
1. Requires an `Authorization: Bearer <token>` header (401 if missing/malformed)
2. Validates the token via `JwtService` (401 if invalid/expired)
3. Loads the user and requires `user.isActive()` (401 if not found/inactive)
4. Populates `SecurityContextHolder` with the authenticated `User` so controllers can use `@AuthenticationPrincipal User user`

### Role/Ownership Checks (enforced in service classes, not Spring method security)

| Check | Enforced by |
|-------|-------------|
| Room creator/admin required to update or delete a room, add/remove members | `RoomService` |
| Only the message sender can edit/delete their own message | `MessageService` |
| Must be a room member to view/send messages or upload files | `MessageService` / `UploadController` (room membership check) |
| Only the addressee can accept/reject a friend request | `RelationshipController` |

## Data Protection

### Database

- Passwords hashed with BCrypt (never plain text)
- Spring Data JPA / Hibernate use parameterized queries (prevents SQL injection) for all repository and derived-query access
- Connection credentials (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`) come from environment variables / `.env` files, never hardcoded in source
- Database network isolated within the `chat-network` Docker bridge network (`docker-compose.yml`)

### File Upload Security

`UploadController.java` (`services/api-service/src/main/java/com/chatapp/api/controller/UploadController.java`) performs these checks, in order, on `POST /api/v1/upload`:

```java
private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
    "image/jpeg", "image/png", "image/gif", "image/webp",
    "application/pdf", "text/plain", "application/zip",
    "application/x-rar-compressed", "application/x-7z-compressed",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
);
```
1. `Content-Type` of the uploaded part must be in `ALLOWED_CONTENT_TYPES`, else `400`
2. File size must be ≤ 50 MB (52,428,800 bytes), else `400`
3. Caller must be a member of the target room, else `403`
4. If `messageId` is supplied, that message must exist in the room, else `404`
5. The file is saved under `uploads/yyyy/MM/dd/<random-uuid><original-extension>` and served back at `/uploads/**` via the static resource handler in `WebConfig.java`

**Security measures in place:**
- Content-Type allowlist (no arbitrary/executable file types)
- File size limit (50 MB)
- Room-membership verification before upload
- UUID-based stored filenames (no user-controlled or path-traversal-able names)
- Upload directory partitioned by date

**NOT implemented (verified against `UploadController.java` — still genuinely missing):**
- Virus/malware scanning of uploaded files
- Image sanitization (EXIF metadata is not stripped from uploaded images)
- File content (magic-byte) verification — the check only trusts the client-supplied `Content-Type` header and never inspects the actual file bytes, so a renamed/relabeled file can bypass the allowlist
- CDN integration — uploaded files are served directly by `api-service` from local disk via `/uploads/**`, not through a CDN or object storage

## Network Security

### CORS (Cross-Origin Resource Sharing)

`WebConfig.java` (`services/api-service/src/main/java/com/chatapp/api/config/WebConfig.java`):

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(apiProperties.getCors().getAllowedOrigins().toArray(new String[0]))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
}
```

Allowed origins come from `ALLOWED_ORIGINS` (comma-separated), defaulting to `http://localhost:3000,http://localhost:5173` in `application.yml` — i.e. the local web dev server ports. **In production, set `ALLOWED_ORIGINS` to your real domain(s)**, not the dev defaults and not `*` (wildcard origins are incompatible with `allowCredentials(true)` anyway).

### SSL/TLS

Nginx (`nginx/nginx.conf`, `nginx/Dockerfile`) fronts the services and is the place to terminate TLS. There is no TLS configuration committed in this repo beyond an `nginx/ssl/` directory for certificates — provisioning real certificates (e.g. via Let's Encrypt/certbot or a load balancer) is an operational step outside this codebase. If you do terminate TLS at Nginx, standard guidance applies:
- Redirect HTTP to HTTPS
- Restrict to TLS 1.2/1.3 with strong cipher suites
- Add HSTS (`add_header Strict-Transport-Security "max-age=31536000";`)

### Rate Limiting

`RateLimitingFilter.java` (`services/api-service/src/main/java/com/chatapp/api/middleware/RateLimitingFilter.java`) is a single, simple servlet `Filter` (`@Order(1)`) applied to **every** request on `api-service` — it does not distinguish auth endpoints from others and currently has no per-endpoint tiers:

```java
String clientIp = request.getRemoteAddr();
long windowMs = 60000;
int limit = apiProperties.getRateLimit().getRequestsPerMinute(); // default 120

// sliding window per client IP, in-memory (ConcurrentHashMap<String, Deque<Long>>)
// returns HTTP 429 with a JSON body once the limit is exceeded
```

- Limit: `120` requests/minute per client IP by default (`app.rate-limit.requests-per-minute`, env `RATE_LIMIT_REQUESTS_PER_MINUTE` if wired through `.env`)
- Keyed by `request.getRemoteAddr()` — note this means a shared NAT/proxy IP (e.g. behind Nginx without `X-Forwarded-For` handling) shares one limit across all its users
- State is in-memory per `api-service` instance, so running multiple replicas effectively multiplies the real limit (no shared Redis-backed counter)
- No `X-RateLimit-*` response headers are set — only a `429` JSON body on `{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded"}`
- `websocket-service` and `search-service` have no equivalent rate-limiting filter today

## Environment Variable Management

**Principles:**
1. No secrets in source code
2. All configuration via environment variables (`.env` per service, loaded by `docker-compose.yml`'s `env_file:`)
3. `.env` files excluded from version control (in `.gitignore`)

**Key production secrets:**
```
JWT_SECRET_KEY       - must be identical across api-service and websocket-service
DATABASE_PASSWORD    - Strong random password
SENTRY_DSN           - Sentry project DSN (see MONITORING.md for current wiring status)
```

**Secret generation:**
```bash
openssl rand -hex 32          # JWT secret
openssl rand -base64 24       # Database password
```

## Security Headers

Nginx does not currently set hardened security headers by default. Recommended additions to `nginx/nginx.conf`:

```nginx
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';" always;
```

## Input Validation

Request DTOs (e.g. `UserCreate`, `UserLogin`, `MessageSend`, `RoomCreate`) use `jakarta.validation` (Bean Validation) annotations, enforced via `@Valid @RequestBody` on controller methods and reported by `GlobalExceptionHandler.java`:

```java
public record UserCreate(
    @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_]+$") String username,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 128) String password
) {}
```
(Illustrative — see the actual DTO classes under `services/api-service/src/main/java/com/chatapp/api/dto/` for exact field constraints.)

**Validation rules enforced:**
- String length limits on text fields
- Username pattern restricted to alphanumeric + underscore
- Email format validation
- UUIDs are strongly typed path/query parameters (Spring rejects malformed UUIDs with a 400 automatically)

## Incident Response

**If a security issue is found:**
1. Rotate all secrets immediately (`JWT_SECRET_KEY` in both `api-service` and `websocket-service` `.env` files — keep them in sync)
2. Rotating `JWT_SECRET_KEY` immediately invalidates all existing access/refresh tokens
3. Review access logs for unauthorized access
4. Deploy the fix and notify affected users

## Security Checklist

- [ ] `JWT_SECRET_KEY` is a strong random value (not the `dev-secret-key-change-in-production` default) and identical across `api-service` and `websocket-service`
- [ ] Database password is strong and unique
- [ ] `ALLOWED_ORIGINS` restricted to your real production domain(s), not the localhost dev defaults
- [ ] TLS terminated and enforced (Nginx or load balancer) in production
- [ ] All user inputs validated server-side via Bean Validation
- [ ] File upload type/size restrictions enforced (and magic-byte/AV scanning added if you need them — not implemented today)
- [ ] Rate limiting tuned for your deployment (per-instance, per-IP, no shared counter today)
- [ ] Security headers set in Nginx
- [ ] `.env` files in `.gitignore` and never committed
- [ ] Database backups encrypted at rest
- [ ] Access logs retained for audit
