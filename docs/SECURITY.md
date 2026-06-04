# Security Overview

## Authentication

### JWT (JSON Web Tokens)

The application uses JWT for stateless authentication.

**Token types:**
- **Access token:** Short-lived (30 min), used for API and WebSocket authentication
- **Refresh token:** Long-lived (7 days), used to obtain new access tokens

**JWT payload:**
```json
{
  "sub": "user-uuid",
  "exp": 1700000000,
  "type": "access"  // or "refresh"
}
```

**Algorithm:** HS256 (HMAC with SHA-256)

**Configuration:**
```python
# config.py
jwt_secret_key: str = "<64-char-random-hex>"  # NEVER hardcode in source
jwt_algorithm: str = "HS256"
jwt_access_token_expire_minutes: int = 30
jwt_refresh_token_expire_days: int = 7
```

**Token verification:**
```python
# In auth.py
def decode_token(token: str) -> Optional[dict]:
    try:
        payload = jwt.decode(
            token, settings.jwt_secret_key, algorithms=[settings.jwt_algorithm]
        )
        return payload
    except JWTError:
        return None
```

### Authentication Flow

1. **Register/Login:** Client sends credentials → Server validates → Returns `access_token` + `refresh_token`
2. **API requests:** Client sends `Authorization: Bearer <access_token>` header
3. **Token refresh:** Client sends `refresh_token` → Server validates → Returns new `access_token` + `refresh_token`
4. **WebSocket:** Client connects with `?token=<access_token>` query parameter

### Password Hashing

Passwords are hashed using **bcrypt** via the `passlib` library:

```python
from passlib.context import CryptContext

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)
```

**Password requirements:**
- Minimum 8 characters
- Maximum 128 characters
- Stored as bcrypt hash (60 characters)
- Never stored in plain text
- Never logged or exposed in responses

## Authorization

### Role-Based Access Control

**Room roles:**
| Role | Permissions |
|------|-------------|
| `admin` | Update room, delete room, add/remove members, send messages |
| `member` | View room, send messages, edit own messages, leave room |

**Permission checks in code:**
- Room creator check: `room.created_by_id != user.id` (delete room)
- Admin check: `member.role != MemberRole.ADMIN` (update room, manage members)
- Ownership check: `msg.sender_id != user.id` (edit/delete message)
- Membership check: `RoomMember` existence (view room messages)

### API Security

**Endpoint protection:**
```python
# Dependency injection ensures authentication
async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: AsyncSession = Depends(get_db),
) -> User:
    payload = decode_token(credentials.credentials)
    # Validates: token exists, is not expired, is "access" type
    # Validates: user exists and is active
    ...
```

**Key protections:**
- All data-mutating endpoints require authentication
- Users can only access their own data or data from rooms they belong to
- Message editing/deletion restricted to message owner
- Room update/deletion restricted to admin/creator
- Friend request response restricted to addressee

## Data Protection

### Database

- Passwords hashed with bcrypt (never plain text)
- SQLAlchemy ORM prevents SQL injection (parameterized queries)
- Connection strings include credentials (never in source code)
- Database network isolated within Docker network

### File Upload Security

**Validation checks:**
```python
ALLOWED_CONTENT_TYPES = {
    "image/jpeg": "image",
    "image/png": "image",
    "image/gif": "image",
    "image/webp": "image",
    "application/pdf": "document",
    "text/plain": "document",
    "application/zip": "archive",
    # ...
}
MAX_FILE_SIZE = 50 * 1024 * 1024  # 50 MB
```

**Security measures:**
- Content type whitelist (no executable files)
- File size limit (50 MB)
- Files stored outside web root (served via static mount)
- UUID-based filenames (no user-controlled paths)
- Upload directory organized by date (YYYY/MM/DD)
- Room membership verification before upload

**NOT implemented (future improvements):**
- Virus scanning
- Image sanitization (strip EXIF data)
- File content type verification (magic bytes)
- CDN integration for file serving

## Network Security

### CORS (Cross-Origin Resource Sharing)

```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,  # Explicit allowlist
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

**Production:**
```python
allowed_origins: list[str] = ["https://yourdomain.com"]  # NOT wildcard
```

### SSL/TLS

Nginx terminates SSL/TLS in production:

```nginx
server {
    listen 443 ssl;
    server_name yourdomain.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
}
```

- HTTP to HTTPS redirect configured
- TLS 1.2 and 1.3 only
- Strong cipher suites enforced
- HSTS recommended (add `add_header Strict-Transport-Security "max-age=31536000";`)

### Rate Limiting

Rate limiting is implemented to prevent abuse:

| Endpoint | Limit |
|----------|-------|
| Authenticated API | 100 req/min per IP |
| Auth (register/login) | 20 req/min per IP |
| WebSocket connections | 60/min per IP |

Headers returned: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

## Environment Variable Management

**Principles:**
1. No secrets in source code
2. All configuration via environment variables
3. Different `.env` files per environment (dev, staging, production)
4. `.env` files excluded from version control (in `.gitignore`)

**Production secrets:**
```
JWT_SECRET_KEY       - 64 char hex string (generate with openssl rand -hex 32)
DB_PASSWORD          - Strong random password
SENTRY_DSN           - Sentry project DSN
```

**Secret generation:**
```bash
openssl rand -hex 32          # JWT secret (64 hex chars)
openssl rand -base64 24       # Database password
```

## Security Headers

Recommended Nginx security headers (add to `nginx/nginx.conf`):

```nginx
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';" always;
```

## Input Validation

**Backend (Pydantic schemas):**
```python
# Field constraints on all user inputs
username: str = Field(..., min_length=3, max_length=50, pattern=r"^[a-zA-Z0-9_]+$")
email: EmailStr  # Validated email format
password: str = Field(..., min_length=8, max_length=128)
content: str = Field(..., min_length=1, max_length=5000)
```

**Validation rules enforced:**
- String length limits on all text fields
- Regex pattern on usernames (alphanumeric + underscore only)
- Email format validation
- UUID validation for all ID parameters
- Enum validation for message_type, role, relationship status

## Security Scanning

The CI pipeline includes automated security scanning:

**`.github/workflows/security.yml`:**
- Weekly scheduled scans (Mondays 6 AM)
- On push to main branch

**Scans performed:**
1. **Safety** - Python dependency vulnerability check
2. **npm audit** - JavaScript dependency audit
3. **Trivy** - Filesystem vulnerability scan (Docker images, code)

## Incident Response

**If a security issue is found:**
1. Rotate all secrets immediately
2. Revoke all JWT tokens (change `JWT_SECRET_KEY`)
3. Review access logs for unauthorized access
4. Deploy fix and notify affected users

## Security Checklist

- [ ] `JWT_SECRET_KEY` is a strong random value (not "change-me")
- [ ] Database passwords are strong and unique
- [ ] `DEBUG=false` in production
- [ ] CORS origins restricted to production domain
- [ ] SSL certificates valid and auto-renewal configured
- [ ] All user inputs validated server-side
- [ ] File upload type/size restrictions enforced
- [ ] Rate limiting configured
- [ ] Security headers set in Nginx
- [ ] Containers run as non-root user
- [ ] `.env` files in `.gitignore`
- [ ] Dependencies regularly scanned for vulnerabilities
- [ ] Database backups encrypted
- [ ] Access logs retained for audit
