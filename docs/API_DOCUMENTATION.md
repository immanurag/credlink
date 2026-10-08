# API Documentation

Base URL: `/api/v1`. All responses use the envelope:

```json
{ "success": true, "data": { ... } }
{ "success": false, "error": { "code": "...", "message": "...", "timestamp": "...", "path": "..." } }
```

All endpoints except `/auth/*` require `Authorization: Bearer <accessToken>`. The merchant identity
is always derived from the JWT server-side — it is never accepted from the request body or params.

## Auth

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/request-otp` | `{email, storeName?}` | Creates the merchant on first use; sends/logs a 6-digit OTP |
| POST | `/auth/verify-otp` | `{email, otp}` | Returns `{accessToken, refreshToken, merchantId, storeName, email}` |
| POST | `/auth/refresh` | `{refreshToken}` | Rotates both tokens |
| POST | `/auth/logout` | — | Stateless; frontend discards tokens |
| GET | `/auth/me` | — | Current merchant profile |

## Customers

| Method | Path | Notes |
|---|---|---|
| POST | `/customers` | `{name, phone?, address?, category?, creditLimit?}` |
| GET | `/customers?search=` | List, optional name search |
| GET | `/customers/{id}` | |
| PUT | `/customers/{id}` | |
| DELETE | `/customers/{id}` | |
| GET | `/customers/{id}/balance` | `{balance}` |
| GET | `/customers/{id}/transactions` | Confirmed transactions only, chronological |

## Transactions (ledger engine)

| Method | Path | Notes |
|---|---|---|
| POST | `/transactions` / `/transactions/preview` | `{customerId, type, amount, description?, source?, voiceTranscript?, confidenceScore?}` → creates a `PENDING_REVIEW` row with a computed (not yet committed) `balanceAfter` |
| GET | `/transactions/{id}` | |
| POST | `/transactions/{id}/confirm` | Optional body to edit type/amount/description before committing. Atomically updates `customer.currentBalance` and fires the notification event |
| POST | `/transactions/confirm` | Same as above, `{transactionId, edits}` in body |
| DELETE | `/transactions/{id}` | Discards a `PENDING_REVIEW` transaction (cannot discard a confirmed one) |

## Voice / NLP

| Method | Path | Notes |
|---|---|---|
| POST | `/voice/transcribe` | `{transcript}` — echoes back a client-transcribed string (STT happens in-browser via Web Speech API); extension point for server-side Whisper |
| POST | `/nlp/extract` | `{transcript}` → `{customerName, amount, type, confidence, valid}` |

## Reports

| Method | Path | Notes |
|---|---|---|
| GET | `/reports/customers/{id}/statement?from=&to=` | ISO-8601 instants, both optional |
| GET | `/reports/customers/{id}/statement/export?format=pdf\|excel` | Streams the file |

## Notifications

| Method | Path | Notes |
|---|---|---|
| GET | `/notifications` | All logs, newest first |
| GET | `/notifications/{id}` | |
| POST | `/notifications/customers/{id}/remind` | Manual single reminder |
| POST | `/notifications/customers/remind-overdue` | Bulk reminder to every customer with a positive balance |

## Dashboard

| Method | Path | Notes |
|---|---|---|
| GET | `/dashboard/summary` | Totals for the Merchant Command Centre cards |
| GET | `/dashboard/trend?days=7` | Daily Udhaar/Jama series for the trend chart |

## Error codes (non-exhaustive)

`OTP_COOLDOWN`, `OTP_NOT_FOUND`, `OTP_EXPIRED`, `OTP_INVALID`, `OTP_ALREADY_USED`,
`OTP_TOO_MANY_ATTEMPTS`, `OTP_DELIVERY_FAILED`, `MERCHANT_NOT_FOUND`, `REFRESH_TOKEN_INVALID`, `CUSTOMER_NOT_FOUND`,
`TRANSACTION_NOT_FOUND`, `TRANSACTION_ALREADY_CONFIRMED`, `TRANSACTION_DISCARDED`,
`INVALID_AMOUNT`, `INVALID_TRANSACTION_TYPE`, `NLP_EXTRACTION_FAILED`, `VOICE_EMPTY_TRANSCRIPT`,
`VALIDATION_ERROR`, `UNAUTHORIZED`, `INTERNAL_ERROR`.
