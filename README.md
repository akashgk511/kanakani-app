# Kanakani (কানাকানি) — backend

Anonymous gossip for office people. Phone number in, silly secret name out, and posts that cannot be traced back.
Its unique feature is **Rumour Receipts**: a rumour can later be settled by community vote, and writers earn a public truth score.
The **Wishbox** lets members vote on new features; winners become the roadmap and switch on remotely.

## Folder map (one folder per kind of file)

| Folder | What lives there |
|---|---|
| `db/` | PostgreSQL: schema, ranking views, sample jobs data, cleanup job |
| `java/` | Java source: `security`, `model`, `service`, `dao`, `util`, `web` packages |
| `jsp/` | Pages (go to `WEB-INF/jsp` in the WAR) |
| `static/` | CSS and JavaScript served at `/static` |
| `i18n/` | Text for each language: `messages.properties` (English), `messages_bn` (Bengali), `messages_hi` (Hindi) |
| `config/` | `web.xml`, `context.xml` (database pool), `env.example` (secrets) |
| `tests/` | `SelfTest.java`, a plain-Java check with no libraries |
| `scripts/` | `selftest.sh`, `init-db.sh` |
| `prototype/` | Clickable single-file demo |
| `docs/` | `BUILD_PLAN.md`, `SECURITY.md`, `RUMOUR_RECEIPTS.md`, `WISHBOX.md`, `LANGUAGES.md` |

## Run it

For a fuller local setup walkthrough, see [`docs/LOCAL_SETUP.md`](docs/LOCAL_SETUP.md). For the verification boundary and remaining gaps, see [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md). The supplied logo is served from `static/images/kanakani-logo.png` and is used in the shared page header.


1. Need JDK 17+, Maven, PostgreSQL 15+, Tomcat 10.1.
2. `createdb kanakani`, then `scripts/init-db.sh postgresql://user:pass@localhost:5432/kanakani`
3. Make secrets: see `config/env.example` (`openssl rand -base64 32`). Export them, with `KANAKANI_SMS=console` for development.
4. `mvn package`, copy `target/kanakani.war` into Tomcat `webapps/`, start Tomcat with the `-Dkanakani.db.*` options from `config/context.xml`.
5. Open it over HTTPS (cookies are Secure, so plain http will not keep you signed in).
6. Schedule `db/04_maintenance.sql` every 5 minutes.

Check the logic and required assets without external services: `scripts/package-check.sh` (includes the self-test, expected `80 checks, 0 failed`).

## Honest status

- Verified: the Java compiles against the Servlet 6 API and the self test passes.
- **Not yet run:** the SQL files, the JSPs, `pom.xml` and the whole app together. Expect small fixes on first run.
- Sample companies are made up (`is_sample = true`). Do not present them as real.
- The website cannot block screenshots. That needs the phone app (see `docs/BUILD_PLAN.md`).
- Languages: the clickable prototype is fully switchable (English, Bengali, Hindi). In the Java backend only the top bar, sign-in page and error page are translated so far; the other pages still need their text moved into `i18n/` (see `docs/LANGUAGES.md`).
- The Wishbox needs the team to move ideas along by SQL for now; there is no admin screen.
