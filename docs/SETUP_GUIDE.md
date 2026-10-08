# Setup Guide

## Option A — Run locally without Docker (fastest, fully mocked)

**Requirements:** Java 17, Maven, Node.js 20.

```bash
cd backend
mvn spring-boot:run
```
Backend starts on `http://localhost:8080` using an in-memory H2 database (no MySQL needed) and
mock OTP/NLP/Notification modes.

```bash
cd frontend
npm install
npm run dev
```
Frontend starts on `http://localhost:5173` and proxies `/api` to the backend.

**Login flow in mock mode:**
1. Open http://localhost:5173, enter a store name + any email, submit.
2. Check the **backend terminal** for a line like:
   `[DEV MODE] OTP for you@store.com is: 482913 (valid 5 min)`
3. Enter that 6-digit code on the Verify OTP screen.

**Try the voice flow:** on `/voice`, either use the mic (Chrome/Edge support the Web Speech API
best) or type a transcript manually, e.g. `Ramesh se 200 rupaye credit kiya`. You'll need a
customer named "Ramesh" to exist first (add one from `/customers`).

## Option B — Docker Compose (full stack incl. real MySQL)

```bash
cp .env.example .env
# Set JWT_SECRET at minimum: openssl rand -base64 48
docker compose up --build
```
- Frontend: http://localhost:8081
- Backend: http://localhost:8080/api/v1
- MySQL: localhost:3306 (credentials from `.env`)

All three external integrations still default to `mock` mode inside Docker too — flip `OTP_MODE`,
`LLM_MODE`, `NOTIFY_MODE` to `live` in `.env` and supply real credentials when you're ready.

## Enabling real services

| Service | Env vars | Where to get credentials |
|---|---|---|
| OTP email | `OTP_MODE=live`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | Your email provider's SMTP/app-password settings |
| LLM extraction | `LLM_MODE=live`, `LLM_API_KEY`, `LLM_API_URL`, `LLM_MODEL` | openrouter.ai or any OpenAI-compatible provider |
| Twilio SMS/WhatsApp | `NOTIFY_MODE=live`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_SMS_FROM`, `TWILIO_WHATSAPP_FROM` | twilio.com console |

## Enable Real Email OTP

By default (`OTP_MODE=mock`) no email is sent — the OTP is printed to the backend console only.
To make merchant login send a real 6-digit OTP to the merchant's actual email inbox:

1. **Copy the env file**: `cp .env.example .env` (if you haven't already).
2. **Set `OTP_MODE=live`** in `.env`.
3. **Configure SMTP credentials** in `.env`:
   ```
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_USERNAME=your-account@gmail.com
   SMTP_PASSWORD=your-app-password
   SMTP_FROM=your-account@gmail.com
   ```
4. **For Gmail specifically**: your normal Gmail password will NOT work over SMTP. Create a
   **Google App Password** instead:
   - Go to your Google Account → Security → 2-Step Verification (must be enabled first)
   - Scroll to "App passwords" → generate one for "Mail" / "Other (Custom name)"
   - Use the generated 16-character password as `SMTP_PASSWORD`
   Any other SMTP provider (SendGrid, Mailgun, your company mail server, etc.) works the same way
   — just point `SMTP_HOST`/`SMTP_PORT` at it and use its credentials.
5. **Start the backend** with these env vars loaded, e.g.:
   ```bash
   cd backend
   export $(grep -v '^#' ../.env | xargs)   # or use your IDE's env-file support
   mvn spring-boot:run
   ```
   Or with Docker Compose, just `docker compose up --build` — it reads `.env` automatically.
6. **Test merchant login**: on the login screen, enter a real email address you can check, and
   submit. The backend will attempt a real SMTP send instead of logging to console.
7. **Check the recipient inbox** (and spam/promotions folder) for an email titled
   "Your CredLink login code". Enter that 6-digit code on the Verify OTP screen.

If SMTP sending fails for any reason (bad credentials, network block, wrong port), the API returns
a clean `OTP_DELIVERY_FAILED` error ("Unable to send OTP right now. Please try again.") — no JWT is
ever issued for a login where the OTP wasn't actually delivered, and the app won't crash. See
`DEBUGGING_GUIDE.md` if this happens.

Mock mode remains fully available for local development — just leave `OTP_MODE=mock` (or unset).

## Running backend tests

```bash
cd backend
mvn test
```

> Note: this project was built in a sandboxed environment without access to Maven Central, so the
> backend test suite is written and reviewed but could not be executed here. Run `mvn test` in your
> own environment before deploying — see `docs/TESTING_DOCUMENTATION.md` for what's covered.

## Running frontend type-check / build

```bash
cd frontend
npm install
npx tsc -b      # type-check only
npm run build   # full production build (this was verified to succeed while building this project)
```
