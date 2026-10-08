# Debugging Guide

## "I logged in but never got an OTP"
You're almost certainly in `OTP_MODE=mock` (the default). The OTP is **printed to the backend
console**, not emailed — look for `[DEV MODE] OTP for <email> is: ######`. Switch to
`OTP_MODE=live` with real SMTP credentials to actually send email — see "Enable Real Email OTP" in
`docs/SETUP_GUIDE.md`.

## "OTP_MODE=live but I get 'Unable to send OTP right now'"
This is the `OTP_DELIVERY_FAILED` error, returned whenever the SMTP send genuinely fails — the app
never pretends an email went out when it didn't, and never issues a JWT for that attempt. Check,
in order:
1. `SMTP_USERNAME` and `SMTP_PASSWORD` are both set (and `SMTP_FROM` too, or it falls back to
   `SMTP_USERNAME`) — if neither `SMTP_FROM` nor `SMTP_USERNAME` is set, the backend fails fast
   with a log line `credlink.otp.mode=live but no SMTP_FROM or SMTP_USERNAME is configured.`
   before even attempting a send.
2. For Gmail: are you using a normal password instead of a **Google App Password**? Gmail rejects
   normal-password SMTP auth outright.
3. `SMTP_HOST`/`SMTP_PORT` reachable from where the backend is running — corporate networks and
   some cloud providers block outbound port 587/25.
4. Check the backend log for `Failed to send OTP email to <email>: <reason>` — the reason comes
   straight from the mail server/network layer and is never the OTP or the SMTP password.

## "OTP keeps failing even though I typed it correctly"
After 5 incorrect verification attempts against the same OTP, it's locked out
(`OTP_TOO_MANY_ATTEMPTS`) even if you eventually type the right code — this is intentional
brute-force protection. Request a new OTP.

## "Voice extraction keeps failing / returns NLP_EXTRACTION_FAILED"
- In `LLM_MODE=mock` (default), extraction uses `RuleBasedNlpService`, a pattern matcher — not a
  real language model. It needs: a recognizable amount (digits, or a supported Hindi number word
  like "do sau"), an explicit Udhaar/Jama keyword ("udhaar", "credit", "diya" vs "jama", "mila",
  "payment"), and a name token immediately before "se"/"ko"/"ka"/"ki"/"ne". Phrasing outside that
  shape will fail — that's expected behavior for the mock parser, not a bug.
- In `LLM_MODE=live`, check `LLM_API_KEY` is set and the backend log for
  `"LLM extraction call failed"` — the router automatically falls back to the rule-based parser on
  any live failure, so a fallback result with lower apparent accuracy usually means the live call
  errored.

## "Web Speech API doesn't start listening"
The Web Speech API is only implemented in Chromium-based browsers (Chrome, Edge) and Safari;
Firefox does not support it. `useSpeechRecognition`'s `supported` flag detects this and the UI
falls back to a manual transcript text box — this is intentional, not a bug.

## "Balance looks wrong after a transaction"
Check `transactions.balance_after` on the specific row via `GET /transactions/{id}` — it's a
point-in-time snapshot. If it doesn't match `customers.current_balance`, look for a transaction
stuck in `PENDING_REVIEW` (previewed but never confirmed) — those never touch the customer's
balance by design, only `confirm` does.

## "WhatsApp notification never arrives (NOTIFY_MODE=live)"
- Confirm the customer has a `phone` set — the listener silently no-ops if it's blank.
- Twilio WhatsApp requires **both** `TWILIO_WHATSAPP_FROM` and the destination number to carry the
  `whatsapp:` prefix; this is handled in `TwilioNotificationService.sendWhatsApp` already, but a
  sandbox Twilio WhatsApp number requires the recipient to have opted in via the Twilio join code
  first — check the Twilio console for delivery errors via the `notification_logs.error_message`
  column / `GET /notifications`.
- A failed notification never rolls back the ledger transaction — this is by design (see
  ARCHITECTURE.md), so "transaction confirmed but no WhatsApp" means check the notification log,
  not the transaction.

## "401 Unauthorized on every request after a while"
Access tokens expire after `JWT_ACCESS_TTL` minutes (default 60). The frontend's axios interceptor
auto-refreshes using the stored refresh token; if that's also expired (`JWT_REFRESH_TTL`, default 7
days), the user is redirected to `/auth/login` — this is expected, not a bug.

## Backend won't start: "Communications link failure" (Docker Compose)
The `backend` container depends on `mysql` being **healthy**, not just started — if this still
happens, `docker compose logs mysql` to confirm it finished initializing before checking backend
logs again.
