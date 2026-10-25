# Storingator

Personal encrypted vault API (logins + notes). Started on Java 19, now on Java 25 + Spring Boot. Goal: learn API design and be secure enough for personal use.

Base URL: `https://<host>` when accessed through Cloudflare (Nginx terminates origin TLS and proxies to the API over the private Docker network). Locally, the API defaults to port `8888`. All bodies/answers are JSON. Authenticated calls send `Authorization: Bearer <loginkey>`.

## How it works

1. `POST /api/v1/User/New` → create account (`username`, `email`, `clearPassword`).
2. `POST /api/v1/User/Signin` → returns `{ "username", "loginkey" }`. Use `loginkey` as the Bearer token.
3. Call `/Data/*` (login vault) and `/Note/*` with that header.
4. `POST /api/v1/User/Logout` when done.

## Features

| Area | Endpoints |
|---|---|
| Health (public) | `GET /health` → `OK` |
| User | `GET /api/v1/User/` (info), `POST /New`, `POST /Signin`, `POST /Logout`, `GET /Me`, `POST /ChangePassword`, `POST /Delete` (deletes user + all vault/notes) |
| Login vault | `POST /api/v1/Data/New`, `POST /Update`, `DELETE /Delete/{seq}`, `POST /GetLoginData` (passwords masked as `*************` unless device-encrypted), `POST /GetClearPassword` (needs `passphrase` for server-encrypted entries) |
| Notes | `POST /api/v1/Note/New`, `POST /Update`, `DELETE /Delete/{seq}`, `POST /GetNotes` (listing, no decryption), `POST /GetNote` (single note, decrypts) |

Vault entries (`{ tmpLoginData, passphrase }` / `{ tmpNote, passphrase }`):
- `encryptOnDevice: true` → server stores bytes as-is, `passphrase` not needed, reads return raw value.
- `encryptOnDevice: false` → server encrypts with AES-256-GCM, key from `passphrase` via PBKDF2-HMAC-SHA256 (600k iterations) + per-user vault salt + random 12-byte nonce. Reads/decrypts require the same `passphrase`.
- `GetNotes` returns metadata (`title`, `seq`, `timestamp`, `encryptOnDevice`) for all of the caller's notes and needs no `passphrase`; for server-encrypted notes `content` is omitted, and `GetNote` is used to fetch and decrypt one.
- Every entry has a UUID `seq`; all read/update/delete calls check entry ownership (`401/403` otherwise). Unknown paths return `404 {"error":"Endpoint not found: ..."}`, wrong method returns `405`.

## Security in place

- **Passwords:** BCrypt-hashed, never stored/returned in clear. Dummy-hash check on unknown users to limit enumeration.
- **Sessions:** random 50-char token, only SHA-256 hash stored; 30 min TTL; one active session per user (re-signin evicts old); password change/logout/delete invalidate sessions. Stateless, no cookies.
- **Transport:** Cloudflare terminates public HTTPS and connects to Nginx over HTTPS using a Cloudflare Origin CA certificate; Nginx proxies plain HTTP to the API on the private Docker network. The API itself has no TLS listener. Set Cloudflare SSL/TLS mode to **Full (strict)**. Keep port 8080 private and publish only Nginx port 443. Store the origin certificate/key at `Storingator/secrets/cloudflare-origin.pem` and `Storingator/secrets/cloudflare-origin-key.pem`. HSTS-style headers: `frame-deny`, `default-src 'self'`, `referrer same-origin`.
- **Access control:** `/User/New`, `/ChangePassword`, `/Delete` restricted to `REGISTRATION_ALLOWED_IPS` (defaults to the Nginx container's fixed internal address); `/Signin`, `/User/`, `/health` public; everything else needs a valid Bearer token (`401 {"error":"Unauthorized"}`, `403 {"error":"Forbidden"}`).
- **Rate limits:** per-IP global filter on every request (`app.rate-limit.per-minute`, default 20/min → `429 {"error":"Too many requests"}`); signin capped at 100 *failed* attempts/15 min globally plus 5 *failed* attempts/15 min per username (successful logins never consume abuse budget); signup capped at 5 attempts/hour per username+email. Client IP comes from `request.getRemoteAddr()` unless `TRUSTED_PROXY_HEADER` is set.
- **CORS:** only `ALLOWED_ORIGINS` (default `http://localhost:*`, `http://127.0.0.1:*`), methods GET/POST/PUT/DELETE/OPTIONS, headers `Authorization, Content-Type`.
- **Input/data:** Jakarta validation on all bodies, Flyway-managed Postgres schema (`ddl-auto: validate`), per-user data isolation.

## Run

Needs `Storingator/.env` with `DB_PASSWORD` (plus optional `DB_NAME`, `REGISTRATION_ALLOWED_IPS`, `ALLOWED_ORIGINS`):

```bash
cd Storingator
docker compose up --build
```

DB and API are not exposed to the host; only Nginx publishes port 443 by default. In Cloudflare, proxy the DNS record and select **Full (strict)** SSL/TLS mode. Restrict the published port to Cloudflare IP ranges in the VPS firewall. `HTTPS_PORT` can change the host-side port to another Cloudflare-supported HTTPS origin port (such as `8443`). DB debug: `docker compose exec db psql -U storingator`.

The Compose setup caps memory at 768 MiB for the API (JVM heap ~65% of that), 512 MiB for PostgreSQL, and 128 MiB for Nginx—about 1.4 GiB total, leaving room for the OS on a 2 GB VPS. PostgreSQL is also tuned for a small instance. Check actual usage with `docker stats` after deployment and adjust if your workload needs more.
