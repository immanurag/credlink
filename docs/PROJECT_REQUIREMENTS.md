# Project Requirements

## Functional Requirements

1. Merchant can log in via email + OTP and receive a JWT session.
2. Merchant can add, search, update, delete customers ("parties").
3. Merchant can record a transaction by voice (Hindi/Hinglish), have it transcribed and extracted
   into `{customerName, amount, type}`, review/edit it, and confirm before it's committed.
4. Merchant can record a transaction manually (no voice) with the same review/confirm step.
5. Every confirmed transaction atomically updates the customer's running balance.
6. Customer receives a WhatsApp/SMS notification after a transaction is confirmed.
7. Merchant can view a per-customer ledger register and export it as PDF or Excel.
8. Merchant can view a dashboard summarizing total receivable/payable, today's activity, and a
   7-day Udhaar-vs-Jama trend.
9. Merchant can send a manual or bulk WhatsApp payment reminder to customers with a due balance.

## Non-Functional Requirements

- Mobile-first, legible UI suited to a small retail counter environment.
- No merchant-supplied identity is ever trusted for authorization — always resolved from the JWT.
- Secrets (LLM key, Twilio token, JWT secret, DB password) never reach the frontend.
- A notification failure must never roll back a successfully committed ledger transaction.
- The system must be usable end-to-end (login → voice entry → ledger → statement) without any
  external service credentials, via the mock modes described in `ARCHITECTURE.md`.

## Out of scope for this delivery

- Real UPI payment gateway integration (the `UPI` transaction source is a manual tag only).
- Multi-user/staff roles per store (single merchant login per store for v1).
- Customer-facing login/portal (customers only receive outbound WhatsApp/SMS).
- Automated integration test suite (`@SpringBootTest`) and frontend component tests — documented
  as a next step in `TESTING_DOCUMENTATION.md`, not included in this delivery.
- The full academic "college project report" document (title page, literature review, UML set,
  etc.) — this delivery is the working software plus its technical documentation; the report can
  be generated as a follow-up using the architecture/DB docs here as source material.
