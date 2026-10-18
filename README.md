# Storingator

Personal encrypted vault API (logins + notes). Started on Java 19, now on Java 25 + Spring Boot. Goal: learn API design and be secure enough for personal use.

Base URL: `https://<host>:8443` (default port `8443` in Docker, `8888` locally). All bodies/answers are JSON. Authenticated calls send `Authorization: Bearer <loginkey>`.

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
- **Transport:** HTTPS enforced with HTTP→HTTPS redirect (disable locally via `REQUIRE_HTTPS=false`); HSTS-style headers: `frame-deny`, `default-src 'self'`, `referrer same-origin`.
- **Access control:** `/User/New`, `/ChangePassword`, `/Delete` restricted to `REGISTRATION_ALLOWED_IPS` (default loopback only); `/Signin`, `/User/`, `/health` public; everything else needs a valid Bearer token (`401 {"error":"Unauthorized"}`, `403 {"error":"Forbidden"}`).
- **Rate limits:** per-IP global filter on every request (`app.rate-limit.per-minute`, default 20/min → `429 {"error":"Too many requests"}`); signin capped at 100 *failed* attempts/15 min globally plus 5 *failed* attempts/15 min per username (successful logins never consume abuse budget); signup capped at 5 attempts/hour per username+email. Client IP comes from `request.getRemoteAddr()` unless `TRUSTED_PROXY_HEADER` is set.
- **CORS:** only `ALLOWED_ORIGINS` (default `http://localhost:*`, `http://127.0.0.1:*`), methods GET/POST/PUT/DELETE/OPTIONS, headers `Authorization, Content-Type`.
- **Input/data:** Jakarta validation on all bodies, Flyway-managed Postgres schema (`ddl-auto: validate`), per-user data isolation.

## Run

Needs `Storingator/.env` (`DB_PASSWORD`, `SSL_KEY_STORE_PASSWORD`, plus optional `DB_NAME`, `SSL_KEY_ALIAS`, `REQUIRE_HTTPS`, `REGISTRATION_ALLOWED_IPS`, `ALLOWED_ORIGINS`) and keystore at `Storingator/secrets/storingator.pkcs12`:

```bash
cd Storingator
docker compose up --build
```

DB is not exposed to the host; debug with `docker compose exec db psql -U storingator`.
