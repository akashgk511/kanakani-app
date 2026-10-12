#!/usr/bin/env bash
# Creates the tables, views and sample data. Usage: scripts/init-db.sh postgresql://user:pass@localhost:5432/kanakani
# 03_seed_sample_jobs.sql loads made-up companies only. Skip it in production.
set -euo pipefail
cd "$(dirname "$0")/.."
url="${1:?Pass a PostgreSQL connection URL}"
for f in db/01_schema.sql db/02_views_rankings.sql db/05_wishbox.sql db/03_seed_sample_jobs.sql; do
  echo "Running $f"
  psql "$url" -v ON_ERROR_STOP=1 -f "$f"
done
echo "Done. Schedule db/04_maintenance.sql (see the file header) to run every few minutes."
