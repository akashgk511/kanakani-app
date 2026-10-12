# Local setup (Windows / macOS / Linux)

## Requirements

- JDK 17 or newer (JDK 21 is fine)
- Maven 3.9+
- PostgreSQL 15+ (or Docker Desktop / Docker Engine)
- Tomcat 10.1.x

## 1. Configure local database

From the project root:

1. Copy `.env.example` to `.env` and set `KANAKANI_DB_PASSWORD` to a strong local-only password.
2. Start PostgreSQL with `docker compose up -d db`.
3. Confirm it is healthy using `docker compose ps`.
4. Apply the schema and seed scripts in order. If using the bundled container, one approach is:

```bash
docker compose exec -T db psql -U kanakani -d kanakani < db/01_schema.sql
docker compose exec -T db psql -U kanakani -d kanakani < db/02_views_rankings.sql
docker compose exec -T db psql -U kanakani -d kanakani < db/03_seed_sample_jobs.sql
docker compose exec -T db psql -U kanakani -d kanakani < db/05_wishbox.sql
```

Review `db/04_maintenance.sql` and schedule it using an appropriate scheduler; it is not an automatically running SQL job just because the file exists.

## 2. Configure application secrets

Create fresh development-only secrets rather than reusing production values. `openssl rand -base64 32` is suitable for generating random key material. Fill the variables described in `config/env.example`. For the database pool, start Tomcat with JVM system properties similar to:

```text
-Dkanakani.db.url=jdbc:postgresql://localhost:5432/kanakani
-Dkanakani.db.user=kanakani
-Dkanakani.db.password=YOUR_LOCAL_DB_PASSWORD
```

The project currently includes a console SMS gateway for development; the OTP is expected in server logs. Never enable that gateway in production, and do not expose development logs.

## 3. Test and build

```bash
bash scripts/package-check.sh
mvn clean package
```

On Windows PowerShell, run the Java self-test using Git Bash or WSL if Bash is not installed. Maven itself can be run from PowerShell with `mvn clean package`.

## 4. Deploy

Copy `target/kanakani.war` to Tomcat 10.1's `webapps` directory, configure the database JVM properties in Tomcat's service/environment, and start Tomcat. Serve the site through HTTPS. The application uses secure session cookies and HSTS; do not disable those protections to make a plain-HTTP production deployment appear to work.

## 5. First-run checklist

- Check Tomcat logs for JNDI datasource errors and missing environment keys.
- Check PostgreSQL logs for schema/constraint errors.
- Test sign-in, CSRF-protected forms, logout, language switching and every route.
- Verify no phone number or OTP is written to application logs or persisted in raw form.
- Verify retention/cleanup with disposable test data.
- Run the app behind HTTPS before testing secure-cookie behaviour in a browser.
