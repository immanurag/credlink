# CredLink — Vyapar Bahi-Khata

Voice-first digital ledger for merchant-retailer credit (Udhaar/Jama) management. Merchants dictate
transactions in Hindi/Hinglish, the backend extracts a structured entry, the merchant reviews and
confirms, and the customer's running balance and WhatsApp/SMS notification are updated atomically.

## Stack

- **Frontend:** React + Vite + TypeScript + Tailwind CSS
- **Backend:** Java 17 + Spring Boot 3 + Spring Security + JWT + Maven
- **Database:** MySQL 8 (H2 in-memory by default for local dev — see below)
- **Voice:** Web Speech API (browser), extensible to Whisper server-side
- **NLP:** Rule-based Hindi/Hinglish extractor by default; OpenRouter/OpenAI-compatible LLM optional
- **Notifications:** Twilio SMS/WhatsApp (mock mode by default)
- **Deployment:** Docker + Docker Compose

## Quick start (no external accounts needed)

Everything runs in **mock mode** out of the box: no MySQL server, no LLM key, no Twilio account,
no SMTP required. This is the fastest way to see the whole voice → review → confirm → ledger flow.

```bash
# Backend (uses in-memory H2 by default — see application.yml)
cd backend
mvn spring-boot:run

# Frontend, in a second terminal
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. Log in with any email — in mock mode the OTP is printed to the
**backend console**, not emailed (look for a line starting `[DEV MODE] OTP for ...`).

To send a real OTP email instead, see **"Enable Real Email OTP"** in `docs/SETUP_GUIDE.md`.

## Full stack with Docker Compose (MySQL + real services)

```bash
cp .env.example .env
# edit .env: set JWT_SECRET, and flip OTP_MODE/LLM_MODE/NOTIFY_MODE to "live" with real
# credentials once you have them — everything works in "mock" mode without any of that.
docker compose up --build
```

- Frontend: http://localhost:8081
- Backend API: http://localhost:8080/api/v1

See `docs/SETUP_GUIDE.md` for a full walkthrough and `docs/API_DOCUMENTATION.md` for every endpoint.

## Project layout

```
CredLink/
├── backend/     Spring Boot API (module-per-domain: auth, customer, transaction, nlp, voice,
│                notification, report, dashboard)
├── frontend/    React app (pages match the CredLink UI design system in docs/ARCHITECTURE.md)
├── docs/        Architecture, DB design, API reference, setup & debugging guides
└── docker-compose.yml
```

## Design decisions worth knowing

- **Database default is H2, not MySQL.** `application.yml` defaults to an in-memory H2 instance in
  MySQL-compatibility mode so the backend runs immediately with zero setup. Docker Compose overrides
  this to real MySQL 8 via env vars. Nothing in the code is MySQL-specific beyond the JDBC URL.
- **Everything external has a mock mode** (`OTP_MODE`, `LLM_MODE`, `NOTIFY_MODE`, all default to
  `mock`). Mock mode is not a stub — OTP is generated and validated for real (just delivered via
  console log instead of email), the rule-based NLP extractor genuinely parses Hindi/Hinglish
  transactions, and Twilio calls are logged instead of sent. Nothing is faked as "working" when it
  isn't; see `docs/ARCHITECTURE.md` for exactly what each mode does.
- **`OTP_MODE=live` sends a real 6-digit OTP by SMTP** (`spring-boot-starter-mail`, STARTTLS on
  port 587 by default), never logs or returns the plaintext OTP anywhere, limits verification to 5
  attempts before requiring a fresh OTP, and never issues a JWT unless the OTP was actually sent
  and correctly verified. See "Enable Real Email OTP" in `docs/SETUP_GUIDE.md`.
- **A notification failure never rolls back a ledger transaction.** The ledger write commits first;
  the WhatsApp/SMS send happens asynchronously afterward via a `TransactionalEventListener` bound to
  `AFTER_COMMIT`. See `TransactionNotificationListener`.
- **The frontend never sees a secret.** LLM key, Twilio token, JWT secret, DB password all live in
  backend env vars only.
