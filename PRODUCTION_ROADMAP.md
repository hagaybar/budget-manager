# Production Deployment Roadmap

**Goal**: Make the Budget Manager app publicly accessible on the existing Oracle Cloud Ampere A1 instance, with proper security, encryption, and user management.

**Current state**: Working FastAPI app with SQLite, no auth, no HTTPS, no reverse proxy, CORS wide open. Nginx is installed but not configured.

---

## Phase 1: Secure the Server & Enable HTTPS

### 1.1 Domain & DNS
- Register a domain (or use a free subdomain service such as DuckDNS or FreeDNS).
- Create an A record pointing to the Oracle A1 instance public IP.

### 1.2 Firewall (Oracle Cloud + OS)
- In the Oracle Cloud console, open **ingress rules** on the VCN security list for ports **80** and **443** (TCP).
- On the instance, allow the same ports via `iptables`:
  ```bash
  sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
  sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
  sudo netfilter-persistent save
  ```
- Close all other non-essential inbound ports (especially 8000 — uvicorn should only listen on localhost).

### 1.3 Nginx Reverse Proxy
- Configure Nginx to proxy requests to the uvicorn backend on `127.0.0.1:8000`.
- Serve static files (`/static`) directly from Nginx for better performance.
- Enable gzip compression for JSON and HTML responses.
- Example site config (`/etc/nginx/sites-available/budget-manager`):
  ```nginx
  server {
      listen 80;
      server_name your-domain.com;

      location /static/ {
          alias /home/ubuntu/projects/budget_manager/app/static/;
          expires 7d;
          add_header Cache-Control "public, immutable";
      }

      location / {
          proxy_pass http://127.0.0.1:8000;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
      }
  }
  ```

### 1.4 TLS with Let's Encrypt
- Install Certbot: `sudo apt install certbot python3-certbot-nginx`
- Obtain certificate: `sudo certbot --nginx -d your-domain.com`
- Certbot auto-configures Nginx for HTTPS and sets up auto-renewal via systemd timer.

---

## Phase 2: Process Management & Reliability

### 2.1 Systemd Service
Create `/etc/systemd/system/budget-manager.service`:
```ini
[Unit]
Description=Budget Manager FastAPI App
After=network.target

[Service]
User=ubuntu
Group=ubuntu
WorkingDirectory=/home/ubuntu/projects/budget_manager
Environment="PATH=/home/ubuntu/projects/budget_manager/venv/bin"
ExecStart=/home/ubuntu/projects/budget_manager/venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8000 --workers 2
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```
- `sudo systemctl enable --now budget-manager`
- Uvicorn with 2 workers is sufficient for the Ampere A1 (4 ARM cores, 24 GB RAM shared across workloads).

### 2.2 Logging
- Uvicorn logs go to journald by default (`journalctl -u budget-manager -f`).
- Add `--access-log` to the ExecStart command for request logging.
- Consider logrotate if writing to file.

### 2.3 Health Check Endpoint
- Add a `GET /api/health` endpoint that returns `{"status": "ok"}` and verifies the DB connection.
- Useful for uptime monitoring (e.g., UptimeRobot free tier, or a simple cron + curl).

---

## Phase 3: User Management & Authentication

### 3.1 Auth Strategy
Since there is no external identity provider, implement **local JWT authentication**:

| Component | Implementation |
|-----------|---------------|
| Password hashing | `bcrypt` via `passlib` |
| Token format | JWT (access + refresh tokens) via `python-jose` or `PyJWT` |
| Storage | `users` table in the same SQLite database |
| Middleware | FastAPI `Depends()` with a `get_current_user` dependency |

### 3.2 Database Changes
Add a `users` table:
```sql
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    email         TEXT UNIQUE,
    password_hash TEXT NOT NULL,
    is_active     INTEGER DEFAULT 1,
    created_at    TEXT DEFAULT (datetime('now'))
);
```
Add `user_id INTEGER NOT NULL REFERENCES users(id)` column to both `transactions` and `recurring_transactions` tables. All queries must filter by the authenticated user's ID.

### 3.3 New Endpoints
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/auth/register` | POST | Create account (can be disabled after initial setup) |
| `/api/auth/login` | POST | Returns access + refresh tokens |
| `/api/auth/refresh` | POST | Exchange refresh token for new access token |
| `/api/auth/me` | GET | Return current user profile |

### 3.4 Token Configuration
- Access token: short-lived (15–30 minutes), stored in memory/JS variable.
- Refresh token: longer-lived (7 days), stored in an `httpOnly` secure cookie.
- Secret key: loaded from environment variable (`JWT_SECRET`), not hardcoded.

### 3.5 Frontend Changes
- Add login/register page (or modal) to `index.html`.
- Store access token in a JS variable; attach as `Authorization: Bearer <token>` header on all API calls.
- On 401 responses, attempt silent refresh; if that fails, redirect to login.

---

## Phase 4: Data Encryption

### 4.1 Encryption at Rest — SQLite
SQLite does not support native encryption. Options:

| Option | Pros | Cons |
|--------|------|------|
| **SQLCipher** | Transparent AES-256 encryption of the entire DB file. Drop-in replacement for sqlite3. | Requires compiling from source on ARM64. Slight performance overhead. |
| **Application-level encryption** | Encrypt sensitive fields (amount, description, category) before storing. Use `cryptography.fernet` with a per-user or global key. | More complex queries (can't SUM encrypted amounts). Need to decrypt on read. |
| **Filesystem encryption (LUKS)** | Encrypt the partition/volume where the DB lives. Transparent to the app. | Requires root setup; protects against disk theft, not app-level access. |

**Recommended approach**: Use **SQLCipher** for full DB encryption. It is the standard approach for encrypted SQLite and requires minimal code changes.

To install SQLCipher on ARM64 Ubuntu:
```bash
sudo apt install sqlcipher libsqlcipher-dev
pip install sqlcipher3
```
Then replace `import sqlite3` with `import sqlcipher3 as sqlite3` in `database.py` and add `PRAGMA key = '<encryption-key>';` after connecting.

### 4.2 Encryption in Transit
- Already handled by HTTPS/TLS (Phase 1.4).
- Ensure `Strict-Transport-Security` header is set in Nginx.

### 4.3 Backup Encryption
- Encrypt backup JSON files using `cryptography.fernet` before writing to disk.
- Store the encryption key in an environment variable (`BACKUP_ENCRYPTION_KEY`).
- Decrypt on restore/export.

### 4.4 Secrets Management
Store all secrets in a `.env` file (not committed to git):
```bash
# /home/ubuntu/projects/budget_manager/.env
JWT_SECRET=<random-64-char-string>
DB_ENCRYPTION_KEY=<random-64-char-string>
BACKUP_ENCRYPTION_KEY=<random-64-char-string>
ALLOWED_ORIGINS=https://your-domain.com
```
Load with `python-dotenv` or via the systemd `EnvironmentFile=` directive.

---

## Phase 5: Harden the Application

### 5.1 Lock Down CORS
In `main.py`, replace `allow_origins=["*"]` with the actual domain:
```python
origins = os.getenv("ALLOWED_ORIGINS", "").split(",")
app.add_middleware(CORSMiddleware, allow_origins=origins, ...)
```

### 5.2 Rate Limiting
Install `slowapi` (built on `limits`, works with FastAPI):
```bash
pip install slowapi
```
Apply rate limits to auth endpoints to prevent brute-force attacks:
- `/api/auth/login`: 5 requests/minute per IP
- `/api/auth/register`: 3 requests/hour per IP
- All other endpoints: 60 requests/minute per user

### 5.3 Input Sanitization
- Already handled well by Pydantic validators.
- Add `max_length` constraints on all string fields if not already present.
- Reject excessively large request bodies via Nginx `client_max_body_size 1m;`.

### 5.4 Security Headers (Nginx)
```nginx
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
```

### 5.5 Disable Debug Endpoints in Production
- Disable `/docs` and `/redoc` by setting `docs_url=None, redoc_url=None` in the FastAPI constructor when `ENV=production`.

---

## Phase 6: Automated Backups & Recovery

### 6.1 Scheduled Backups
Create a cron job or systemd timer to call the backup endpoint daily:
```bash
# /etc/cron.d/budget-manager-backup
0 3 * * * ubuntu curl -s -X POST http://127.0.0.1:8000/api/backup/save > /dev/null
```

### 6.2 Backup Rotation
- Keep the last 30 daily backups.
- Add a cleanup cron or extend the backup router to prune old files:
  ```bash
  0 4 * * * ubuntu find /home/ubuntu/projects/budget_manager/backups -name "*.json" -mtime +30 -delete
  ```

### 6.3 Off-Site Backup
Since big cloud storage services are excluded, options:
- **rsync to a second machine** (if available).
- **Rclone to a free-tier S3-compatible store** (e.g., Backblaze B2 free 10 GB, Cloudflare R2 free 10 GB — these are not "big cloud" services).
- **Email backup** via a simple script using `msmtp` to send the encrypted JSON as an attachment.

---

## Phase 7: Database Migration Path

SQLite works well for a single-user or small multi-user budget app. If user count grows beyond ~10 concurrent users, consider migrating to PostgreSQL:

### When to Migrate
- Multiple concurrent write operations cause `SQLITE_BUSY` errors.
- Database file exceeds ~500 MB.
- Need for row-level locking or concurrent connections.

### Migration Strategy
1. Add `alembic` for schema migrations (works with both SQLite and PostgreSQL).
2. Replace `sqlite3` calls with `SQLAlchemy` Core (not ORM — keeps the current direct-SQL style).
3. Use `pg8000` (pure Python) or `psycopg` as the PostgreSQL driver.
4. PostgreSQL can run on the same Ampere A1 instance.

---

## Implementation Order & Effort Estimates

| Phase | Priority | Complexity | Dependencies |
|-------|----------|------------|-------------|
| **Phase 1**: HTTPS & Reverse Proxy | Critical | Low | Domain name |
| **Phase 2**: Systemd & Logging | Critical | Low | Phase 1 |
| **Phase 3**: User Management | High | Medium | Phase 1 |
| **Phase 4**: Data Encryption | High | Medium | Phase 3 (for per-user keys) |
| **Phase 5**: Hardening | High | Low | Phase 1, 3 |
| **Phase 6**: Automated Backups | Medium | Low | Phase 4 (for encrypted backups) |
| **Phase 7**: DB Migration | Low (future) | High | Only if needed |

---

## New Dependencies Summary

```
# Add to requirements.txt for production
python-jose[cryptography]   # JWT tokens
passlib[bcrypt]              # Password hashing
python-dotenv               # Environment variable loading
slowapi                     # Rate limiting
sqlcipher3                  # Encrypted SQLite (requires libsqlcipher-dev)
cryptography                # Backup encryption, general crypto
```
