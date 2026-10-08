# Database Design

```mermaid
erDiagram
    MERCHANTS ||--o{ CUSTOMERS : owns
    MERCHANTS ||--o{ TRANSACTIONS : owns
    MERCHANTS ||--o{ OTP_REQUESTS : "logs in via"
    CUSTOMERS ||--o{ TRANSACTIONS : has
    TRANSACTIONS ||--o{ NOTIFICATION_LOGS : triggers

    MERCHANTS {
        bigint id PK
        varchar email UK
        varchar store_name
        varchar owner_name
        varchar phone
        varchar address
        timestamp created_at
    }

    CUSTOMERS {
        bigint id PK
        bigint merchant_id FK
        varchar name
        varchar phone
        varchar address
        varchar category
        int trust_score
        decimal credit_limit
        decimal current_balance
        timestamp created_at
    }

    TRANSACTIONS {
        bigint id PK
        bigint merchant_id FK
        bigint customer_id FK
        varchar type "UDHAAR | JAMA"
        decimal amount
        decimal balance_after
        varchar description
        varchar source "VOICE | MANUAL | UPI"
        varchar voice_transcript
        double confidence_score
        varchar status "PENDING_REVIEW | CONFIRMED | DISCARDED"
        timestamp created_at
        timestamp confirmed_at
    }

    OTP_REQUESTS {
        bigint id PK
        varchar email
        varchar otp_hash
        timestamp expires_at
        boolean consumed
        int attempts
        timestamp created_at
    }

    NOTIFICATION_LOGS {
        bigint id PK
        bigint transaction_id FK
        varchar channel "SMS | WHATSAPP"
        varchar status "QUEUED | DELIVERED | FAILED"
        varchar provider_ref
        varchar error_message
        timestamp created_at
    }
```

## Notes

- `customers.current_balance` is a **denormalized running total**, updated atomically alongside
  each `transactions` insert in the same DB transaction (see `TransactionService.confirm`). This
  keeps dashboard/list reads O(1) instead of re-summing history every time.
- `transactions.balance_after` snapshots what the balance became at that point in time — this is
  what makes the ledger register (and the "opening balance" of any date-ranged statement)
  reconstructable without re-walking the entire history.
- `otp_requests.otp_hash` stores a BCrypt hash, never the plaintext OTP. The plaintext is never
  logged either, except in `OTP_MODE=mock`, where it is printed to the **server console only**
  (clearly marked as dev-mode) so the login flow is testable without real email credentials.
- `otp_requests.attempts` counts failed verification attempts against that OTP; verification is
  locked out (`OTP_TOO_MANY_ATTEMPTS`) after 5, requiring a fresh OTP.
- `trust_score` and `credit_limit` on `customers` are additions beyond the original PRD schema,
  added to match the new Stitch UI mockups. Both are advisory only — nothing in the ledger engine
  enforces the credit limit as a hard block.
- `notification_logs.transaction_id = 0` is used for bulk/manual reminder sends that aren't tied
  to a single transaction (the "Send Bulk WhatsApp Payment Reminders" dashboard action).
