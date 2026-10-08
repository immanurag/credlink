# Testing Documentation

## Backend unit tests (JUnit 5 + Mockito)

| Class | Covers |
|---|---|
| `RuleBasedNlpServiceTest` | Digit amount extraction, Hindi word-number amount extraction, UDHAAR/JAMA keyword classification, name extraction, rejection of amount-less/empty transcripts |
| `LlmResponseParserTest` | Parsing a full OpenAI-style completion envelope, normalizing CREDIT/DEBIT aliases to UDHAAR/JAMA, rejecting missing fields, non-positive amounts, and non-JSON content |
| `TransactionServiceTest` | UDHAAR increases balance, JAMA decreases balance, confirm atomically updates customer balance + fires `TransactionConfirmedEvent`, rejects re-confirming, rejects non-positive amount, rejects invalid type |
| `CustomerServiceTest` | New customers are scoped to the creating merchant; a customer owned by a different merchant returns `CUSTOMER_NOT_FOUND` (never leaks cross-merchant data) |
| `EmailOtpServiceTest` | Mock mode never touches `JavaMailSender`; resend cooldown enforced; live mode sends via `JavaMailSender`/`MimeMessageHelper` with `SMTP_FROM` preferred over `SMTP_USERNAME`; live mode with neither configured fails cleanly without calling the mail sender; SMTP send failure returns `OTP_DELIVERY_FAILED` without persisting an OTP row; expired/incorrect OTP rejected; 5 incorrect attempts locks out with `OTP_TOO_MANY_ATTEMPTS`; correct OTP marks the request consumed; a consumed OTP cannot be reused (`OTP_ALREADY_USED`) |
| `TwilioNotificationServiceTest` | Mock mode returns a `DELIVERED` result with a `mock-*` reference and never calls the Twilio SDK |

Run with `mvn test` from `backend/`.

## What integration/API-level tests should add (not included, documented as next step)

Given the sandbox this project was built in has no access to Maven Central, full
`@SpringBootTest` + `MockMvc` integration tests (API → Service → Repository → H2) were not added
to avoid shipping tests that were never actually executed. Before production use, add:

- Customer creation → 201 + persisted row
- Credit (UDHAAR) transaction → balance increases by exactly `amount`
- Debit (JAMA) transaction → balance decreases by exactly `amount`, and cannot go be requested with
  a negative/zero amount
- Unauthorized access (no/invalid JWT) → 401 on every protected route
- Cross-merchant access attempt (valid JWT for merchant A, customer id belonging to merchant B) →
  404 `CUSTOMER_NOT_FOUND`, never 403 (avoids confirming the id exists)
- Invalid transaction type / amount → 400 with the documented error code

## Frontend

- **Verified in this environment:** `npx tsc -b` (strict type-check) and `npm run build` (full
  Vite production build) both complete with zero errors against the exact source in this delivery.
- **Documented as needed, not included:** component/interaction tests (React Testing Library) for
  login → OTP, customer creation, search, the 6-state voice flow, transaction confirmation, and
  network-error states. The codebase is structured (isolated `services/*.ts`, a dedicated
  `useSpeechRecognition` hook) specifically so these are straightforward to add with Vitest +
  Testing Library.
