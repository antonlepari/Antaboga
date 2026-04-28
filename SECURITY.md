# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in Antaboga, please report it responsibly:

1. **DO NOT** open a public GitHub issue
2. Email security concerns to: **security@kamsib.id**
3. Include detailed reproduction steps
4. Allow up to 72 hours for initial response

## Security Measures

### Authentication & Sessions
- BCrypt password hashing (12 rounds)
- Secure session tokens (48-char random)
- HttpOnly, SameSite=Strict cookies
- Automatic session expiry
- Single active session per user

### Input Security
- Parameterized SQL queries (prevents SQL injection)
- Input validation and sanitization
- XSS output encoding
- CSRF token protection on state-changing requests

### Network Security
- Security response headers (X-Content-Type-Options, X-Frame-Options, X-XSS-Protection)
- Rate limiting per IP address
- Directory traversal prevention for static files

### Data Security
- H2 embedded database (no network exposure)
- Audit logging for all sensitive actions
- No hardcoded credentials in source code
- Environment variable support for sensitive configuration

## Best Practices for Deployment

1. **Change default credentials immediately** after first run
2. **Set a strong database password** via `ANTABOGA_DB_PASS` environment variable
3. **Bind to localhost** (`127.0.0.1`) unless external access is needed
4. **Use a reverse proxy** (nginx) with TLS for production
5. **Regularly backup** the `data/` directory
6. **Keep Java updated** to the latest Java SE 8 patch
