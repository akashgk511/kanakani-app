# Security notes and honest limits

## What is protected
- **Phone number:** never stored. We keep HMAC-SHA256(secret key, E.164 number). Without the key, numbers cannot be guessed back from the table.
- **One-time codes:** stored as a keyed MAC, five tries, five minutes.
- **Sessions:** random cookie (HttpOnly, Secure, SameSite Strict); only its SHA-256 is stored.
- **Posts:** AES-256-GCM, with the post id as associated data, so a copied row will not open under another post. Key ids allow rotation.
- **CSRF:** every form carries a token derived from the session. **CSP:** no inline script or style, no external origins.
- **Slip-guard:** warns on emails, numbers, links and employee IDs, and checks again on the server.
- **Cleanup:** chat messages, old codes and sessions are deleted on a timer. Panic wipe deletes the account.
- **No IP addresses stored** in the application tables. Make sure the web server and proxy logs do not keep them either.

## Limits we cannot code away
1. **The SMS provider sees your number** when sending the code.
2. **Key custody:** whoever holds the phone key and the database can test a known number. Keep the key in a cloud KMS or HSM in production, not an environment variable.
3. **Writing style and small groups:** a post only one team could know can identify the writer. Use thresholds, for example show a company page only when enough people have joined.
4. **Timing:** server logs showing when a number requested a code and when a post appeared could be correlated. Keep logs short and separate.
5. **Screenshots:** the web cannot block them. The phone app can (Android `FLAG_SECURE`) or detect and blur (iOS).
6. **The public feed cannot be end-to-end encrypted,** because everyone must read it. Private chats can be (see build plan).
7. **Law:** anonymous posts about real people raise defamation and takedown duties. Review India's IT Rules 2021 and the DPDP Act 2023 with a lawyer before launch. Add a report flow and a named grievance contact.

## Before launch
Real SMS gateway, KMS keys, HTTPS and HSTS, dependency scan, penetration test, rate limits behind the proxy, backups that are also encrypted.
