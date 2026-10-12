# Build plan

## Phase 1: Web backend (this folder)
Run it, fix first-run issues, add the real SMS gateway, deploy behind HTTPS. Add reporting and a moderation page.

## Phase 2: JSON API
The phone app cannot use JSPs. Add `/api/v1/*` servlets that return JSON from the same services (`AuthService`, `FeedService`, `ReceiptService`, `RankService`). Use a bearer session token in secure storage and keep CSRF for the web only.

## Phase 3: Phone app (Flutter or React Native)
- Sign in with phone code, show secret name and streak.
- **Android:** `FLAG_SECURE` on all screens, so screenshots and screen recording are blocked.
- **iOS:** no true block exists. Detect `userDidTakeScreenshot` and `UIScreen.isCaptured`, blur the content, and tell the other person.
- Screenshot permission: the other side taps "allow" (table `screenshot_permissions`), then the app lifts the shield for that chat.
- Push notifications carry no message text.
- Tokens in Keychain / Keystore.

## Phase 4: Private chats and rooms
- End-to-end encryption with the Signal protocol (libsignal); the server stores ciphertext envelopes only (`chat_envelopes`).
- Vanishing: delete after reading unless both people tap keep.
- Rooms: invite link with a random secret, members list, optional saved memories.
- Streaks and day counts already exist in the schema.

## Phase 4b: Wishbox and remote updates
The Wishbox backend exists (`docs/WISHBOX.md`). In the phone app, add a Wishbox screen, read `/api/flags` at start-up, hide features whose flag is off, and plan an over-the-air update path (Shorebird or Expo EAS Update). Add a small admin screen so the team can move ideas to Building and Live.

## Phase 5: Growth
Real company data from employees, collusion detection for receipts, moderation tools, legal review.
