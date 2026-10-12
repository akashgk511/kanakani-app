# Wishbox: members shape the app

A page where people suggest improvements, everyone votes yes or no (comment optional), and winning ideas become the public roadmap.

## Flow
1. **Suggest:** a title (5 to 80 characters) and optional details (up to 500). Account must be at least 3 days old; 3 open ideas at a time; 5 per day. Slip-guard blocks emails, numbers, links and employee IDs.
2. **Vote:** 14 days, yes or no, one vote per person (changeable), optional comment up to 200 characters. Authors cannot vote on their own idea. Voters need an account at least 3 days old.
3. **Close** (`db/04_maintenance.sql`, every 5 minutes): at least 10 voters and more yes than no gives **Coming soon**. At least 10 voters and not more yes gives **Not this time**. Fewer than 10 voters gives **Not enough votes**, and the idea can be suggested again.
4. **Build:** the team moves an approved idea to Building, then Live (SQL steps are at the bottom of `db/05_wishbox.sql`; there is no admin screen yet).
5. **Switch on:** the idea's feature flag turns on and everyone sees it.

"Approved" means the community wants it. The team still decides what is possible and when.

## How new features reach people without an app-store update

| Part | How it updates |
|---|---|
| Website (the JSP app in this folder) | Instantly for everyone, the moment the server is updated |
| Features already inside the installed app | Remote feature flags: the app calls `GET /api/flags?build=N` at start-up and shows what is switched on |
| Screens described by the server | The app draws them from JSON, so new screens and wording need no update |
| Interpreted code (Flutter via Shorebird, React Native via Expo EAS Update) | Over-the-air patches. Both stores allow this in general if the app's main purpose does not change; check current Apple and Google policy before relying on it |
| New permissions, new native code, SDK changes | Needs a normal store release. Users get it through the store's auto-update setting, which is how WhatsApp reaches most phones |

So "no Play Store update" works for most features if they are planned for ahead, but not for every change.

## Safety and fairness
- Ideas and comments show only the secret name and gender, like the rest of the app.
- Ideas are stored as plain text (they are public suggestions, not gossip), so do not put private details in them.
- Brigading risk: new accounts cannot vote, and one vote per person per idea. A coordinated group can still swing a vote of 10, so raise `MIN_VOTERS` as the user base grows.
- Moderators can hide an idea with `removed_at`.
